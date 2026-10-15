package com.medisense.rag.retrieval;

import com.medisense.rag.retrieval.model.ScoredChunk;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * BM25Retriever executes Apache Lucene BM25 sparse keyword searches
 * over child chunk texts and normalizes scores to [0, 1].
 */
@Component
public class BM25Retriever {

    private static final Logger log = LoggerFactory.getLogger(BM25Retriever.class);
    private static final int DEFAULT_TOP_K = 20;

    private final LuceneIndexBuilder indexBuilder;

    @Autowired
    public BM25Retriever(LuceneIndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
    }

    /**
     * Executes BM25 similarity search on Lucene index for the given query text.
     *
     * @param queryText the input search keywords
     * @param topK      maximum candidate results to return (default 20)
     * @return top-K ScoredChunks with BM25 scores normalized to [0, 1]
     */
    public List<ScoredChunk> retrieveSparse(String queryText, int topK) {
        if (queryText == null || queryText.isBlank()) {
            return Collections.emptyList();
        }

        int limit = topK > 0 ? topK : DEFAULT_TOP_K;
        log.info("Executing Lucene BM25 sparse retrieval (query: '{}', topK: {})", queryText, limit);

        Directory directory = indexBuilder.getDirectory();
        IndexWriter writer = indexBuilder.getIndexWriter();
        StandardAnalyzer analyzer = indexBuilder.getAnalyzer();

        if (directory == null && writer == null) {
            log.warn("Lucene index not initialized. Returning empty results.");
            return Collections.emptyList();
        }

        IndexReader reader = null;
        try {
            // Prefer Near-Real-Time (NRT) reader from writer if open, otherwise open directory
            if (writer != null && writer.isOpen()) {
                reader = DirectoryReader.open(writer);
            } else if (directory != null && DirectoryReader.indexExists(directory)) {
                reader = DirectoryReader.open(directory);
            } else {
                log.info("No Lucene index exists yet. Returning empty sparse results.");
                return Collections.emptyList();
            }

            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity());

            // Escape Lucene query parser special characters for robustness
            String escapedQuery = QueryParser.escape(queryText.trim());
            if (escapedQuery.isBlank()) {
                return Collections.emptyList();
            }

            QueryParser parser = new QueryParser("chunk_text", analyzer);
            Query query = parser.parse(escapedQuery);

            TopDocs topDocs = searcher.search(query, limit);
            ScoreDoc[] hits = topDocs.scoreDocs;

            if (hits.length == 0) {
                log.debug("BM25 search produced 0 hits for query: '{}'", queryText);
                return Collections.emptyList();
            }

            // Normalization: First hit has highest BM25 score in Lucene topDocs
            float maxScore = hits[0].score;
            List<ScoredChunk> results = new ArrayList<>(hits.length);

            for (int i = 0; i < hits.length; i++) {
                Document doc = searcher.storedFields().document(hits[i].doc);
                String chunkIdStr = doc.get("chunk_id");
                UUID chunkId = chunkIdStr != null ? UUID.fromString(chunkIdStr) : UUID.randomUUID();

                String text = doc.get("chunk_text");
                if (text == null) {
                    text = doc.get("content");
                }

                String heading = doc.get("section_heading");
                if (heading == null || heading.isBlank()) {
                    heading = "Clinical Guideline Section";
                }

                // Lucene scores normalized to [0, 1]
                double rawScore = hits[i].score;
                double normalizedScore = maxScore > 0 ? (rawScore / maxScore) : 0.0;
                normalizedScore = Math.max(0.0, Math.min(1.0, normalizedScore));

                results.add(new ScoredChunk(
                        chunkId,
                        heading,
                        text,
                        normalizedScore,
                        i + 1, // 1-based rank
                        "SPARSE"
                ));
            }

            log.info("BM25Retriever returned {} scored chunks.", results.size());
            return results;
        } catch (ParseException e) {
            log.warn("Failed to parse Lucene BM25 query '{}': {}", queryText, e.getMessage());
            return Collections.emptyList();
        } catch (IOException e) {
            log.error("IO error executing Lucene search for query '{}': {}", queryText, e.getMessage(), e);
            return Collections.emptyList();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {}
            }
        }
    }
}

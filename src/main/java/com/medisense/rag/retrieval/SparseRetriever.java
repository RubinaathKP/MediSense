package com.medisense.rag.retrieval;

import com.medisense.rag.retrieval.model.ScoredChunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * SparseRetriever combines Lucene BM25 index building and sparse search
 * for active child chunks.
 */
@Component
@Primary
public class SparseRetriever {

    private final LuceneIndexBuilder indexBuilder;
    private final BM25Retriever bm25Retriever;

    @Autowired
    public SparseRetriever(LuceneIndexBuilder indexBuilder, BM25Retriever bm25Retriever) {
        this.indexBuilder = indexBuilder;
        this.bm25Retriever = bm25Retriever;
    }

    /**
     * Executes BM25 similarity search on the child chunk text Lucene index,
     * returning top-K scored chunks with Lucene scores normalized to [0, 1].
     */
    public List<ScoredChunk> retrieveSparse(String queryText, int topK) {
        return bm25Retriever.retrieveSparse(queryText, topK);
    }

    /**
     * Incrementally indexes a child chunk into the Lucene index using append mode.
     */
    public void indexChunk(UUID chunkId, String content) throws IOException {
        indexBuilder.indexChunk(chunkId, content);
    }

    /**
     * Incrementally indexes a batch of child chunks into the Lucene index.
     */
    public void indexChunks(List<LuceneIndexBuilder.ChunkEntry> chunks) throws IOException {
        indexBuilder.indexChunks(chunks);
    }

    /**
     * Rebuilds the Lucene index over all active child chunks.
     */
    public void rebuildIndex(List<LuceneIndexBuilder.ChunkEntry> activeChildChunks) throws IOException {
        indexBuilder.rebuildIndex(activeChildChunks);
    }

    public LuceneIndexBuilder getIndexBuilder() {
        return indexBuilder;
    }

    public BM25Retriever getBm25Retriever() {
        return bm25Retriever;
    }
}

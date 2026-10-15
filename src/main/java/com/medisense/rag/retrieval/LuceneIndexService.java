package com.medisense.rag.retrieval;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service bridging Lucene index operations to LuceneIndexBuilder.
 */
@Service
public class LuceneIndexService {

    private static final Logger log = LoggerFactory.getLogger(LuceneIndexService.class);

    private final LuceneIndexBuilder luceneIndexBuilder;

    @Autowired
    public LuceneIndexService(LuceneIndexBuilder luceneIndexBuilder) {
        this.luceneIndexBuilder = luceneIndexBuilder;
    }

    public LuceneIndexService(Directory directory, StandardAnalyzer analyzer) {
        this.luceneIndexBuilder = new LuceneIndexBuilder(directory, analyzer, "./data/lucene_index");
        try {
            this.luceneIndexBuilder.init();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize LuceneIndexBuilder", e);
        }
    }

    public synchronized void indexChunk(String chunkId, String sectionHeading, String content) {
        indexChunksBatch(List.of(new ChunkIndexEntry(chunkId, sectionHeading, content)));
    }

    public synchronized void indexChunksBatch(List<ChunkIndexEntry> chunkEntries) {
        if (chunkEntries == null || chunkEntries.isEmpty()) {
            return;
        }

        log.info("Indexing batch of {} chunk documents into Lucene via LuceneIndexBuilder...", chunkEntries.size());
        List<LuceneIndexBuilder.ChunkEntry> builderEntries = new ArrayList<>(chunkEntries.size());

        for (ChunkIndexEntry entry : chunkEntries) {
            UUID id;
            try {
                id = UUID.fromString(entry.chunkId());
            } catch (Exception e) {
                id = UUID.randomUUID();
            }
            builderEntries.add(new LuceneIndexBuilder.ChunkEntry(id, entry.content(), entry.sectionHeading()));
        }

        try {
            luceneIndexBuilder.indexChunks(builderEntries);
        } catch (IOException e) {
            log.error("Failed to index chunks into Lucene: {}", e.getMessage(), e);
            throw new RuntimeException("Lucene index sync failed", e);
        }
    }

    public record ChunkIndexEntry(String chunkId, String sectionHeading, String content) {}
}

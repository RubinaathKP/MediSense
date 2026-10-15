package com.medisense.rag.retrieval;

import com.medisense.rag.retrieval.model.ScoredChunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Backward-compatible adapter for SparseBm25Retriever extending SparseRetriever.
 */
@Component
public class SparseBm25Retriever extends SparseRetriever {

    @Autowired
    public SparseBm25Retriever(LuceneIndexBuilder indexBuilder, BM25Retriever bm25Retriever) {
        super(indexBuilder, bm25Retriever);
    }

    public SparseBm25Retriever() {
        super(new LuceneIndexBuilder(), new BM25Retriever(new LuceneIndexBuilder()));
    }

    @Override
    public List<ScoredChunk> retrieveSparse(String queryText, int topK) {
        return super.retrieveSparse(queryText, topK);
    }

    @Override
    public void indexChunk(UUID chunkId, String content) throws IOException {
        super.indexChunk(chunkId, content);
    }
}

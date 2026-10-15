package com.medisense.rag.retrieval;

import com.medisense.domain.entity.ChildChunkEntity;
import com.medisense.rag.retrieval.model.RetrievedContext;
import com.medisense.rag.retrieval.model.ScoredChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HybridRetrieverService {

    private static final Logger log = LoggerFactory.getLogger(HybridRetrieverService.class);

    private final DenseRetriever denseRetriever;
    private final SparseBm25Retriever sparseRetriever;
    private final RetrievalOrchestrator retrievalOrchestrator;

    public HybridRetrieverService(DenseRetriever denseRetriever,
                                 SparseBm25Retriever sparseRetriever,
                                 RetrievalOrchestrator retrievalOrchestrator) {
        this.denseRetriever = denseRetriever;
        this.sparseRetriever = sparseRetriever;
        this.retrievalOrchestrator = retrievalOrchestrator;
    }

    public List<String> retrieveHybridPassages(String query, int limit) {
        log.info("Executing hybrid retrieval combining pgvector dense HNSW and Lucene BM25 sparse search.");
        List<RetrievedContext> contexts = retrievalOrchestrator.retrieveContexts(query, List.of(), null);

        List<String> combined = new ArrayList<>();
        for (RetrievedContext ctx : contexts) {
            if (ctx.parentChunkText() != null && !combined.contains(ctx.parentChunkText())) {
                combined.add(ctx.parentChunkText());
            }
        }

        if (combined.isEmpty()) {
            List<ScoredChunk> sparseResults = sparseRetriever.retrieveSparse(query, limit);
            for (ScoredChunk chunk : sparseResults) {
                if (!combined.contains(chunk.content())) {
                    combined.add(chunk.content());
                }
            }
        }

        log.info("Hybrid retrieval produced {} unique candidate passages.", combined.size());
        return combined;
    }
}

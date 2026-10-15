package com.medisense.rag.retrieval;

import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.rag.retrieval.model.HyDEExpansionResult;
import com.medisense.rag.retrieval.model.RetrievedContext;
import com.medisense.rag.retrieval.model.ScoredChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * RetrievalOrchestrator coordinates the hybrid retrieval pipeline:
 * 1. HyDEQueryExpander: produces hypothetical clinical guideline embeddings
 * 2. Parallel Dense (pgvector) and Sparse (Lucene BM25) retrieval via CompletableFuture
 * 3. RRFRanker: fuses dense and sparse rankings with deduplication
 * 4. ParentChunkFetcher: retrieves full coarse-grained parent contexts for top-5 candidates
 */
@Service
public class RetrievalOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RetrievalOrchestrator.class);
    private static final int RETRIEVAL_TOP_K = 20;
    private static final int FINAL_CONTEXT_TOP_K = 5;

    private final HyDEQueryExpander hydeQueryExpander;
    private final DenseRetriever denseRetriever;
    private final SparseRetriever sparseRetriever;
    private final RRFRanker rrfRanker;
    private final ParentChunkFetcher parentChunkFetcher;

    @Autowired
    public RetrievalOrchestrator(HyDEQueryExpander hydeQueryExpander,
                                 DenseRetriever denseRetriever,
                                 SparseRetriever sparseRetriever,
                                 RRFRanker rrfRanker,
                                 ParentChunkFetcher parentChunkFetcher) {
        this.hydeQueryExpander = hydeQueryExpander;
        this.denseRetriever = denseRetriever;
        this.sparseRetriever = sparseRetriever;
        this.rrfRanker = rrfRanker;
        this.parentChunkFetcher = parentChunkFetcher;
    }

    public RetrievalOrchestrator(HyDEQueryExpander hydeQueryExpander,
                                 DenseRetriever denseRetriever,
                                 SparseBm25Retriever sparseBm25Retriever,
                                 RRFRanker rrfRanker,
                                 ParentChunkFetcher parentChunkFetcher) {
        this(hydeQueryExpander, denseRetriever, (SparseRetriever) sparseBm25Retriever, rrfRanker, parentChunkFetcher);
    }

    /**
     * Executes the end-to-end hybrid retrieval flow and returns the full parent contexts.
     *
     * @param queryText      the user clinical question
     * @param domainTags     domain tags filter (e.g., CARDIOLOGY, DIABETES)
     * @param evidenceFilter evidence basis filter (CLINICAL_TRIAL_BACKED, etc.)
     * @return List of RetrievedContext for top-5 child chunks
     */
    public List<RetrievedContext> retrieveContexts(String queryText, List<String> domainTags, EvidenceBasis evidenceFilter) {
        String filterStr = evidenceFilter != null ? evidenceFilter.name() : null;
        return retrieveContextsByEvidenceString(queryText, domainTags, filterStr);
    }

    public List<RetrievedContext> retrieveContexts(String queryText, List<String> domainTags) {
        return retrieveContextsByEvidenceString(queryText, domainTags, (String) null);
    }

    public List<RetrievedContext> retrieveContextsByEvidenceString(String queryText, List<String> domainTags, String evidenceFilter) {
        List<ScoredChunk> fusedChunks = retrieveTopChunksByEvidenceString(queryText, domainTags, evidenceFilter);

        // Select top 5 child chunks for final context fetching
        int limit = Math.min(FINAL_CONTEXT_TOP_K, fusedChunks.size());
        List<ScoredChunk> top5Chunks = fusedChunks.subList(0, limit);

        return parentChunkFetcher.fetchParentContexts(top5Chunks);
    }

    /**
     * Executes parallel dense and sparse retrieval and RRF fusion,
     * returning the top-20 deduplicated candidates ready for cross-encoder reranking.
     */
    public List<ScoredChunk> retrieveTopChunks(String queryText, List<String> domainTags, EvidenceBasis evidenceFilter) {
        String filterStr = evidenceFilter != null ? evidenceFilter.name() : null;
        return retrieveTopChunksByEvidenceString(queryText, domainTags, filterStr);
    }

    public List<ScoredChunk> retrieveTopChunks(String queryText, List<String> domainTags) {
        return retrieveTopChunksByEvidenceString(queryText, domainTags, (String) null);
    }

    public List<ScoredChunk> retrieveTopChunksByEvidenceString(String queryText, List<String> domainTags, String evidenceFilter) {
        log.info("Starting hybrid retrieval orchestration for query: '{}'", queryText);

        if (queryText == null || queryText.isBlank()) {
            return Collections.emptyList();
        }

        // 1. HyDE Query Expansion
        HyDEExpansionResult expansionResult = hydeQueryExpander.expandAndEmbedQuery(queryText);

        // 2. Parallel Dense and Sparse Retrieval via CompletableFuture
        CompletableFuture<List<ScoredChunk>> denseFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return denseRetriever.retrieveDenseByEvidenceString(expansionResult.hydeEmbedding(), domainTags, evidenceFilter, RETRIEVAL_TOP_K);
            } catch (Exception e) {
                log.error("Dense retrieval failed for query '{}': {}", queryText, e.getMessage(), e);
                return Collections.emptyList();
            }
        });

        CompletableFuture<List<ScoredChunk>> sparseFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return sparseRetriever.retrieveSparse(queryText, RETRIEVAL_TOP_K);
            } catch (Exception e) {
                log.error("Sparse retrieval failed for query '{}': {}", queryText, e.getMessage(), e);
                return Collections.emptyList();
            }
        });

        CompletableFuture.allOf(denseFuture, sparseFuture).join();

        List<ScoredChunk> denseResults = denseFuture.join();
        List<ScoredChunk> sparseResults = sparseFuture.join();

        // 3. Reciprocal Rank Fusion (RRF) deduplication & ranking
        return rrfRanker.fuseAndRank(denseResults, sparseResults, RETRIEVAL_TOP_K);
    }
}

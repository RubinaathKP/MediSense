package com.medisense.unit.rag.retrieval;

import com.medisense.UnitTestBase;
import com.medisense.rag.retrieval.DenseRetriever;
import com.medisense.rag.retrieval.HybridRetrieverService;
import com.medisense.rag.retrieval.RetrievalOrchestrator;
import com.medisense.rag.retrieval.SparseBm25Retriever;
import com.medisense.rag.retrieval.model.RetrievedContext;
import com.medisense.rag.retrieval.model.ScoredChunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Test: HybridRetrieverService
 * Verifies hybrid search combining dense HNSW and sparse BM25 retrieval, RRF deduplication, and fallback behavior.
 */
class HybridRetrieverUnitTest extends UnitTestBase {

    @Mock
    private DenseRetriever denseRetriever;

    @Mock
    private SparseBm25Retriever sparseRetriever;

    @Mock
    private RetrievalOrchestrator retrievalOrchestrator;

    private HybridRetrieverService hybridRetrieverService;

    @Override
    protected void onSetup() {
        hybridRetrieverService = new HybridRetrieverService(denseRetriever, sparseRetriever, retrievalOrchestrator);
    }

    @Test
    @DisplayName("Should return orchestrated passages when hybrid contexts are found")
    void shouldReturnOrchestratedContexts() {
        RetrievedContext ctx1 = new RetrievedContext(
                UUID.randomUUID(),
                "Borderline LDL cholesterol ranges between 130 and 159 mg/dL.",
                0.92,
                "ICMR Guidelines",
                2024,
                "A",
                List.of()
        );

        when(retrievalOrchestrator.retrieveContexts(eq("what is borderline ldl"), anyList(), isNull()))
                .thenReturn(List.of(ctx1));

        List<String> results = hybridRetrieverService.retrieveHybridPassages("what is borderline ldl", 5);

        assertThat(results)
                .isNotEmpty()
                .hasSize(1)
                .contains("Borderline LDL cholesterol ranges between 130 and 159 mg/dL.");
    }

    @Test
    @DisplayName("Should fallback to sparse BM25 retriever when orchestrated contexts are empty")
    void shouldFallbackToSparseRetrievalWhenOrchestratedContextsEmpty() {
        when(retrievalOrchestrator.retrieveContexts(anyString(), anyList(), isNull()))
                .thenReturn(Collections.emptyList());

        ScoredChunk scoredChunk = new ScoredChunk(
                UUID.randomUUID().toString(),
                "Sparse keyword match: Fasting blood glucose normal range is 70-99 mg/dL.",
                0.85,
                "WHO Diabetes"
        );
        when(sparseRetriever.retrieveSparse(eq("glucose reference range"), eq(5)))
                .thenReturn(List.of(scoredChunk));

        List<String> results = hybridRetrieverService.retrieveHybridPassages("glucose reference range", 5);

        assertThat(results)
                .isNotEmpty()
                .contains("Sparse keyword match: Fasting blood glucose normal range is 70-99 mg/dL.");
        verify(sparseRetriever).retrieveSparse(eq("glucose reference range"), eq(5));
    }
}

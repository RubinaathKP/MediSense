package com.medisense.rag.retrieval;

import com.medisense.domain.enums.EvidenceBasis;
import com.medisense.rag.retrieval.model.HyDEExpansionResult;
import com.medisense.rag.retrieval.model.RetrievedContext;
import com.medisense.rag.retrieval.model.ScoredChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetrievalOrchestratorTest {

    @Mock
    private HyDEQueryExpander hydeQueryExpander;

    @Mock
    private DenseRetriever denseRetriever;

    @Mock
    private SparseRetriever sparseRetriever;

    @Mock
    private RRFRanker rrfRanker;

    @Mock
    private ParentChunkFetcher parentChunkFetcher;

    private RetrievalOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new RetrievalOrchestrator(
                hydeQueryExpander,
                denseRetriever,
                sparseRetriever,
                rrfRanker,
                parentChunkFetcher
        );
    }

    @Test
    @DisplayName("Should run dense and sparse retrieval concurrently in parallel via CompletableFuture")
    void testParallelExecution() {
        String queryText = "hypertension first line medication guidelines";
        float[] hydeEmbedding = new float[]{0.1f, 0.2f};
        List<String> domainTags = List.of("CARDIOLOGY");
        EvidenceBasis evidenceFilter = EvidenceBasis.CLINICAL_TRIAL_BACKED;

        HyDEExpansionResult mockHyde = new HyDEExpansionResult(queryText, new float[]{}, "Hypothetical answer", hydeEmbedding);
        when(hydeQueryExpander.expandAndEmbedQuery(queryText)).thenReturn(mockHyde);

        UUID chunk1 = UUID.randomUUID();
        UUID chunk2 = UUID.randomUUID();
        List<ScoredChunk> denseResults = List.of(new ScoredChunk(chunk1, "H1", "Content 1", 0.9, 1, "DENSE"));
        List<ScoredChunk> sparseResults = List.of(new ScoredChunk(chunk2, "H2", "Content 2", 0.8, 1, "SPARSE"));

        AtomicBoolean denseStarted = new AtomicBoolean(false);
        AtomicBoolean sparseStarted = new AtomicBoolean(false);

        when(denseRetriever.retrieveDenseByEvidenceString(eq(hydeEmbedding), eq(domainTags), eq(evidenceFilter.name()), eq(20)))
                .thenAnswer(invocation -> {
                    denseStarted.set(true);
                    Thread.sleep(80);
                    return denseResults;
                });

        when(sparseRetriever.retrieveSparse(eq(queryText), eq(20)))
                .thenAnswer(invocation -> {
                    sparseStarted.set(true);
                    Thread.sleep(80);
                    return sparseResults;
                });

        List<ScoredChunk> fusedChunks = List.of(
                new ScoredChunk(chunk1, "H1", "Content 1", 0.03, 1, "HYBRID"),
                new ScoredChunk(chunk2, "H2", "Content 2", 0.02, 2, "HYBRID")
        );
        when(rrfRanker.fuseAndRank(denseResults, sparseResults, 20)).thenReturn(fusedChunks);

        RetrievedContext expectedContext = new RetrievedContext(
                "Full guideline text", "AHA", 2024, "CLINICAL_TRIAL_BACKED", "Hypertension", List.of(chunk1)
        );
        when(parentChunkFetcher.fetchParentContexts(anyList())).thenReturn(List.of(expectedContext));

        long startTime = System.currentTimeMillis();
        List<RetrievedContext> contexts = orchestrator.retrieveContexts(queryText, domainTags, evidenceFilter);
        long elapsed = System.currentTimeMillis() - startTime;

        // Since both sleep for 80ms concurrently, total parallel time should be well under serial (160ms)
        assertTrue(denseStarted.get(), "Dense retriever should have run");
        assertTrue(sparseStarted.get(), "Sparse retriever should have run");
        assertTrue(elapsed < 160, "Dense and sparse should run in parallel; took " + elapsed + "ms");

        assertEquals(1, contexts.size());
        assertEquals("Full guideline text", contexts.get(0).parentChunkText());

        verify(denseRetriever).retrieveDenseByEvidenceString(eq(hydeEmbedding), eq(domainTags), eq(evidenceFilter.name()), eq(20));
        verify(sparseRetriever).retrieveSparse(eq(queryText), eq(20));
        verify(rrfRanker).fuseAndRank(denseResults, sparseResults, 20);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ScoredChunk>> topChunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(parentChunkFetcher).fetchParentContexts(topChunksCaptor.capture());
        assertEquals(2, topChunksCaptor.getValue().size());
    }

    @Test
    @DisplayName("Should select at most top 5 chunks for parent context fetching")
    void testTop5ChunksPassedToParentFetcher() {
        String query = "asthma treatment";
        when(hydeQueryExpander.expandAndEmbedQuery(query))
                .thenReturn(new HyDEExpansionResult(query, new float[]{}, "ans", new float[]{}));

        when(denseRetriever.retrieveDenseByEvidenceString(any(), any(), any(), anyInt())).thenReturn(Collections.emptyList());
        when(sparseRetriever.retrieveSparse(any(), anyInt())).thenReturn(Collections.emptyList());

        // Create 10 fused chunks
        List<ScoredChunk> tenChunks = List.of(
                new ScoredChunk(UUID.randomUUID(), "H", "C1", 0.9, 1, "HYBRID"),
                new ScoredChunk(UUID.randomUUID(), "H", "C2", 0.8, 2, "HYBRID"),
                new ScoredChunk(UUID.randomUUID(), "H", "C3", 0.7, 3, "HYBRID"),
                new ScoredChunk(UUID.randomUUID(), "H", "C4", 0.6, 4, "HYBRID"),
                new ScoredChunk(UUID.randomUUID(), "H", "C5", 0.5, 5, "HYBRID"),
                new ScoredChunk(UUID.randomUUID(), "H", "C6", 0.4, 6, "HYBRID"),
                new ScoredChunk(UUID.randomUUID(), "H", "C7", 0.3, 7, "HYBRID")
        );
        when(rrfRanker.fuseAndRank(anyList(), anyList(), eq(20))).thenReturn(tenChunks);
        when(parentChunkFetcher.fetchParentContexts(anyList())).thenReturn(Collections.emptyList());

        orchestrator.retrieveContexts(query, Collections.emptyList(), (EvidenceBasis) null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ScoredChunk>> topChunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(parentChunkFetcher).fetchParentContexts(topChunksCaptor.capture());
        assertEquals(5, topChunksCaptor.getValue().size(), "Only top 5 chunks should be passed to parent fetcher");
    }

    @Test
    @DisplayName("Should tolerate sparse retriever failure and still proceed with dense results")
    void testResilienceWhenSparseFails() {
        String query = "statin therapy";
        when(hydeQueryExpander.expandAndEmbedQuery(query))
                .thenReturn(new HyDEExpansionResult(query, new float[]{}, "ans", new float[]{}));

        UUID chunkId = UUID.randomUUID();
        List<ScoredChunk> denseResults = List.of(new ScoredChunk(chunkId, "H", "C", 0.9, 1, "DENSE"));
        when(denseRetriever.retrieveDenseByEvidenceString(any(), any(), any(), eq(20))).thenReturn(denseResults);
        when(sparseRetriever.retrieveSparse(any(), eq(20))).thenThrow(new RuntimeException("Lucene index corrupted"));

        when(rrfRanker.fuseAndRank(eq(denseResults), eq(Collections.emptyList()), eq(20)))
                .thenReturn(denseResults);
        when(parentChunkFetcher.fetchParentContexts(anyList())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> orchestrator.retrieveContexts(query, List.of(), (EvidenceBasis) null));
        verify(rrfRanker).fuseAndRank(eq(denseResults), eq(Collections.emptyList()), eq(20));
    }

    @Test
    @DisplayName("Should tolerate dense retriever failure and still proceed with sparse results")
    void testResilienceWhenDenseFails() {
        String query = "metformin dosing";
        when(hydeQueryExpander.expandAndEmbedQuery(query))
                .thenReturn(new HyDEExpansionResult(query, new float[]{}, "ans", new float[]{}));

        UUID chunkId = UUID.randomUUID();
        List<ScoredChunk> sparseResults = List.of(new ScoredChunk(chunkId, "H", "C", 0.8, 1, "SPARSE"));
        when(denseRetriever.retrieveDenseByEvidenceString(any(), any(), any(), eq(20))).thenThrow(new RuntimeException("PostgreSQL connection timeout"));
        when(sparseRetriever.retrieveSparse(any(), eq(20))).thenReturn(sparseResults);

        when(rrfRanker.fuseAndRank(eq(Collections.emptyList()), eq(sparseResults), eq(20)))
                .thenReturn(sparseResults);
        when(parentChunkFetcher.fetchParentContexts(anyList())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> orchestrator.retrieveContexts(query, List.of(), (EvidenceBasis) null));
        verify(rrfRanker).fuseAndRank(eq(Collections.emptyList()), eq(sparseResults), eq(20));
    }

    @Test
    @DisplayName("Should return empty list for blank query")
    void testBlankQuery() {
        List<RetrievedContext> contexts = orchestrator.retrieveContexts("   ", List.of(), (EvidenceBasis) null);
        assertTrue(contexts.isEmpty());
        verifyNoInteractions(hydeQueryExpander);
    }
}

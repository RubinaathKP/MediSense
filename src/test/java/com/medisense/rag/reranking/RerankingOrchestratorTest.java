package com.medisense.rag.reranking;

import com.medisense.rag.reranking.exception.RerankerUnavailableException;
import com.medisense.rag.reranking.model.*;
import com.medisense.rag.retrieval.model.ScoredChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RerankingOrchestratorTest {

    private RerankerClient mockRerankingClient;
    private RerankingOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        mockRerankingClient = mock(RerankerClient.class);
        orchestrator = new RerankingOrchestrator(mockRerankingClient);
    }

    @Test
    void testRerankingOrchestratorSuccessAndScoreMapping() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        List<ScoredChunk> inputChunks = List.of(
                new ScoredChunk(id1, "Section 1", "Content 1", 0.05, 1, "HYBRID"),
                new ScoredChunk(id2, "Section 2", "Content 2", 0.04, 2, "HYBRID"),
                new ScoredChunk(id3, "Section 3", "Content 3", 0.03, 3, "HYBRID")
        );

        // Mock reranker reordering: id3 is highest, id1 is second
        RerankResponse mockResponse = new RerankResponse(List.of(
                new RerankResultDto(id3.toString(), 0.98),
                new RerankResultDto(id1.toString(), 0.88)
        ));

        when(mockRerankingClient.rerank(any(RerankRequest.class))).thenReturn(mockResponse);

        List<RerankedChunk> result = orchestrator.rerankChunks("query text", inputChunks);

        assertNotNull(result);
        assertEquals(2, result.size());

        // First item should be id3 with score 0.98 and rank 1
        assertEquals(id3, result.get(0).chunkId());
        assertEquals(0.98, result.get(0).score());
        assertEquals(1, result.get(0).rank());
        assertTrue(result.get(0).isReranked());

        // Second item should be id1 with score 0.88 and rank 2
        assertEquals(id1, result.get(1).chunkId());
        assertEquals(0.88, result.get(1).score());
        assertEquals(2, result.get(1).rank());
        assertTrue(result.get(1).isReranked());
    }

    @Test
    void testFallbackWhenRerankerUnavailable() {
        List<ScoredChunk> inputChunks = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            inputChunks.add(new ScoredChunk(UUID.randomUUID(), "Section " + i, "Content " + i, 1.0 / i, i, "RRF"));
        }

        when(mockRerankingClient.rerank(any(RerankRequest.class)))
                .thenThrow(new RerankerUnavailableException("Service offline"));

        List<RerankedChunk> result = orchestrator.rerankChunks("query text", inputChunks);

        assertNotNull(result);
        assertEquals(5, result.size()); // Fallback limits to top-5

        for (int i = 0; i < 5; i++) {
            RerankedChunk chunk = result.get(i);
            assertEquals(inputChunks.get(i).chunkId(), chunk.chunkId());
            assertEquals(inputChunks.get(i).score(), chunk.score());
            assertEquals(i + 1, chunk.rank());
            assertFalse(chunk.isReranked(), "isReranked should be false when fallback is applied");
        }
    }

    @Test
    void testEmptyInputCandidatesReturnsEmptyList() {
        List<RerankedChunk> result = orchestrator.rerankChunks("query text", List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(mockRerankingClient);
    }

    @Test
    void testInputLimitedToTwentyCandidates() {
        List<ScoredChunk> inputChunks = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            inputChunks.add(new ScoredChunk(UUID.randomUUID(), "Section " + i, "Content " + i, 1.0 / i, i, "RRF"));
        }

        when(mockRerankingClient.rerank(any(RerankRequest.class)))
                .thenReturn(new RerankResponse(List.of()));

        orchestrator.rerankChunks("query text", inputChunks);

        verify(mockRerankingClient).rerank(argThat(req -> req.passages().size() == 20));
    }
}

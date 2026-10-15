package com.medisense.rag.retrieval;

import com.medisense.rag.retrieval.model.ScoredChunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RRFRankerTest {

    private final RRFRanker rrfRanker = new RRFRanker();

    @Test
    @DisplayName("Should correctly calculate RRF score and rank chunks with overlapping IDs")
    void testRrfFormulaAndDeduplication() {
        UUID sharedChunk = UUID.randomUUID();
        UUID denseOnlyChunk = UUID.randomUUID();
        UUID sparseOnlyChunk = UUID.randomUUID();

        // Dense results: sharedChunk is rank 1, denseOnly is rank 2
        List<ScoredChunk> denseResults = List.of(
                new ScoredChunk(sharedChunk, "H1", "Content 1", 0.9, 1, "DENSE"),
                new ScoredChunk(denseOnlyChunk, "H2", "Content 2", 0.8, 2, "DENSE")
        );

        // Sparse results: sparseOnly is rank 1, sharedChunk is rank 2
        List<ScoredChunk> sparseResults = List.of(
                new ScoredChunk(sparseOnlyChunk, "H3", "Content 3", 0.9, 1, "SPARSE"),
                new ScoredChunk(sharedChunk, "H1", "Content 1", 0.8, 2, "SPARSE")
        );

        List<ScoredChunk> fused = rrfRanker.fuseAndRank(denseResults, sparseResults, 20);

        // Deduplication: 3 unique chunks out of 4 total occurrences
        assertEquals(3, fused.size());

        // Expected scores:
        // sharedChunk: (1/(60+1)) + (1/(60+2)) = 1/61 + 1/62 = 0.01639344 + 0.01612903 = 0.03252247
        // sparseOnlyChunk: missing in dense (rank 21) -> (1/(60+21)) + (1/(60+1)) = 1/81 + 1/61 = 0.01234568 + 0.01639344 = 0.02873912
        // denseOnlyChunk: missing in sparse (rank 21) -> (1/(60+2)) + (1/(60+21)) = 1/62 + 1/81 = 0.01612903 + 0.01234568 = 0.02847471

        double expectedSharedScore = (1.0 / 61.0) + (1.0 / 62.0);
        double expectedSparseOnlyScore = (1.0 / 81.0) + (1.0 / 61.0);
        double expectedDenseOnlyScore = (1.0 / 62.0) + (1.0 / 81.0);

        assertEquals(sharedChunk, fused.get(0).chunkId());
        assertEquals(1, fused.get(0).rank());
        assertEquals(expectedSharedScore, fused.get(0).score(), 1e-6);
        assertEquals("HYBRID", fused.get(0).source());

        assertEquals(sparseOnlyChunk, fused.get(1).chunkId());
        assertEquals(2, fused.get(1).rank());
        assertEquals(expectedSparseOnlyScore, fused.get(1).score(), 1e-6);

        assertEquals(denseOnlyChunk, fused.get(2).chunkId());
        assertEquals(3, fused.get(2).rank());
        assertEquals(expectedDenseOnlyScore, fused.get(2).score(), 1e-6);
    }

    @Test
    @DisplayName("Should assign missing rank=21 when chunk only appears in dense list")
    void testDenseOnlyChunkGetsMissingSparseRank21() {
        UUID denseChunk = UUID.randomUUID();
        List<ScoredChunk> denseResults = List.of(
                new ScoredChunk(denseChunk, "Heading", "Dense text", 0.95, 1, "DENSE")
        );
        List<ScoredChunk> sparseResults = Collections.emptyList();

        List<ScoredChunk> fused = rrfRanker.fuseAndRank(denseResults, sparseResults, 20);

        assertEquals(1, fused.size());
        // Rank 1 in dense, missing in sparse (rank 21)
        // Score = 1/(60+1) + 1/(60+21) = 1/61 + 1/81
        double expectedScore = (1.0 / 61.0) + (1.0 / 81.0);
        assertEquals(expectedScore, fused.get(0).score(), 1e-6);
        assertEquals(1, fused.get(0).rank());
    }

    @Test
    @DisplayName("Should assign missing rank=21 when chunk only appears in sparse list")
    void testSparseOnlyChunkGetsMissingDenseRank21() {
        UUID sparseChunk = UUID.randomUUID();
        List<ScoredChunk> denseResults = Collections.emptyList();
        List<ScoredChunk> sparseResults = List.of(
                new ScoredChunk(sparseChunk, "Heading", "Sparse text", 0.85, 3, "SPARSE")
        );

        List<ScoredChunk> fused = rrfRanker.fuseAndRank(denseResults, sparseResults, 20);

        assertEquals(1, fused.size());
        // Missing in dense (rank 21), rank 3 in sparse
        // Score = 1/(60+21) + 1/(60+3) = 1/81 + 1/63
        double expectedScore = (1.0 / 81.0) + (1.0 / 63.0);
        assertEquals(expectedScore, fused.get(0).score(), 1e-6);
        assertEquals(1, fused.get(0).rank());
    }

    @Test
    @DisplayName("Should handle duplicate chunk entries in the same retrieval list")
    void testDeduplicationWithinSameList() {
        UUID duplicateChunk = UUID.randomUUID();
        List<ScoredChunk> denseResults = List.of(
                new ScoredChunk(duplicateChunk, "Heading", "Dense 1", 0.9, 1, "DENSE"),
                new ScoredChunk(duplicateChunk, "Heading", "Dense 1", 0.7, 4, "DENSE")
        );
        List<ScoredChunk> sparseResults = Collections.emptyList();

        List<ScoredChunk> fused = rrfRanker.fuseAndRank(denseResults, sparseResults, 20);

        assertEquals(1, fused.size(), "Duplicate chunk_id within dense list must be deduplicated");
        // Best rank (rank 1) should be chosen: 1/61 + 1/81
        double expectedScore = (1.0 / 61.0) + (1.0 / 81.0);
        assertEquals(expectedScore, fused.get(0).score(), 1e-6);
    }

    @Test
    @DisplayName("Should respect topK limit parameter")
    void testTopKLimit() {
        List<ScoredChunk> dense = List.of(
                new ScoredChunk(UUID.randomUUID(), "H", "C1", 0.9, 1, "DENSE"),
                new ScoredChunk(UUID.randomUUID(), "H", "C2", 0.8, 2, "DENSE"),
                new ScoredChunk(UUID.randomUUID(), "H", "C3", 0.7, 3, "DENSE")
        );
        List<ScoredChunk> sparse = Collections.emptyList();

        List<ScoredChunk> fused = rrfRanker.fuseAndRank(dense, sparse, 2);

        assertEquals(2, fused.size());
        assertEquals(1, fused.get(0).rank());
        assertEquals(2, fused.get(1).rank());
    }

    @Test
    @DisplayName("Should return empty list when both dense and sparse are empty")
    void testEmptyInputs() {
        List<ScoredChunk> fused = rrfRanker.fuseAndRank(Collections.emptyList(), Collections.emptyList(), 20);
        assertTrue(fused.isEmpty());
        assertTrue(rrfRanker.fuseAndRank(null, null, 20).isEmpty());
    }
}

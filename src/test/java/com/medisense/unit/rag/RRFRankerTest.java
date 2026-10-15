package com.medisense.unit.rag;

import com.medisense.UnitTestBase;
import com.medisense.rag.retrieval.RRFRanker;
import com.medisense.rag.retrieval.model.ScoredChunk;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test: RRFRankerTest
 * Tests Reciprocal Rank Fusion (RRF) score calculation, rank merging, deduplication, and mathematical formula precision.
 */
class RRFRankerTest extends UnitTestBase {

    private RRFRanker rrfRanker;

    @Override
    protected void onSetup() {
        rrfRanker = new RRFRanker();
    }

    @Test
    @DisplayName("Case 1: Dense and sparse both match: chunks appearing in both get highest scores")
    void testDenseAndSparseBothMatchHighestScores() {
        UUID chunk1 = UUID.randomUUID();
        UUID chunk2 = UUID.randomUUID();
        UUID chunk3 = UUID.randomUUID();
        UUID chunk4 = UUID.randomUUID();

        // Dense results: [chunk1 (rank 1), chunk2 (rank 2), chunk3 (rank 3)]
        List<ScoredChunk> dense = List.of(
                new ScoredChunk(chunk1, "Section 1", "Content 1", 0.95, 1, "DENSE"),
                new ScoredChunk(chunk2, "Section 2", "Content 2", 0.90, 2, "DENSE"),
                new ScoredChunk(chunk3, "Section 3", "Content 3", 0.85, 3, "DENSE")
        );

        // Sparse results: [chunk2 (rank 1), chunk3 (rank 2), chunk4 (rank 3)]
        List<ScoredChunk> sparse = List.of(
                new ScoredChunk(chunk2, "Section 2", "Content 2", 1.0, 1, "SPARSE"),
                new ScoredChunk(chunk3, "Section 3", "Content 3", 0.8, 2, "SPARSE"),
                new ScoredChunk(chunk4, "Section 4", "Content 4", 0.6, 3, "SPARSE")
        );

        List<ScoredChunk> fused = rrfRanker.fuseAndRank(dense, sparse, 10);

        // Assert: chunk2 and chunk3 get highest RRF scores (appear in both), chunk1 and chunk4 lower
        assertThat(fused).hasSize(4);

        UUID topRankedId = fused.get(0).chunkId();
        UUID secondRankedId = fused.get(1).chunkId();

        assertThat(List.of(topRankedId, secondRankedId))
                .as("Chunks appearing in both retrievers must rank in top 2")
                .containsExactlyInAnyOrder(chunk2, chunk3);

        double scoreChunk2 = fused.stream().filter(c -> c.chunkId().equals(chunk2)).findFirst().orElseThrow().score();
        double scoreChunk3 = fused.stream().filter(c -> c.chunkId().equals(chunk3)).findFirst().orElseThrow().score();
        double scoreChunk1 = fused.stream().filter(c -> c.chunkId().equals(chunk1)).findFirst().orElseThrow().score();
        double scoreChunk4 = fused.stream().filter(c -> c.chunkId().equals(chunk4)).findFirst().orElseThrow().score();

        assertThat(scoreChunk2).isGreaterThan(scoreChunk1);
        assertThat(scoreChunk2).isGreaterThan(scoreChunk4);
        assertThat(scoreChunk3).isGreaterThan(scoreChunk1);
        assertThat(scoreChunk3).isGreaterThan(scoreChunk4);
    }

    @Test
    @DisplayName("Case 2: Deduplication: chunk appearing in both lists is not duplicated in output")
    void testDeduplicationAcrossLists() {
        UUID chunk1 = UUID.randomUUID();
        UUID chunk2 = UUID.randomUUID();
        UUID chunk3 = UUID.randomUUID();

        // Dense: [chunk1, chunk2]
        List<ScoredChunk> dense = List.of(
                new ScoredChunk(chunk1, "Section 1", "Content 1", 0.95, 1, "DENSE"),
                new ScoredChunk(chunk2, "Section 2", "Content 2", 0.90, 2, "DENSE")
        );

        // Sparse: [chunk2, chunk3]
        List<ScoredChunk> sparse = List.of(
                new ScoredChunk(chunk2, "Section 2", "Content 2", 1.0, 1, "SPARSE"),
                new ScoredChunk(chunk3, "Section 3", "Content 3", 0.8, 2, "SPARSE")
        );

        List<ScoredChunk> fused = rrfRanker.fuseAndRank(dense, sparse, 10);

        // Assert: chunk2 not duplicated in output
        assertThat(fused).hasSize(3);

        long chunk2Count = fused.stream().filter(c -> c.chunkId().equals(chunk2)).count();
        assertThat(chunk2Count)
                .as("chunk2 must appear exactly once in deduplicated output")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Case 3: RRF formula correctness: score == 1/(60 + rank_dense) + 1/(60 + rank_sparse)")
    void testRrfFormulaMathematicalCorrectness() {
        UUID chunkDual = UUID.randomUUID();
        UUID chunkDenseOnly = UUID.randomUUID();

        int denseRank = 2;
        int sparseRank = 3;

        // chunkDual is rank 2 in dense, rank 3 in sparse
        List<ScoredChunk> dense = List.of(
                new ScoredChunk(chunkDenseOnly, "Section 1", "Content 1", 0.99, 1, "DENSE"),
                new ScoredChunk(chunkDual, "Section 2", "Content 2", 0.90, denseRank, "DENSE")
        );

        List<ScoredChunk> sparse = List.of(
                new ScoredChunk(UUID.randomUUID(), "Other 1", "Other content", 0.95, 1, "SPARSE"),
                new ScoredChunk(UUID.randomUUID(), "Other 2", "Other content", 0.90, 2, "SPARSE"),
                new ScoredChunk(chunkDual, "Section 2", "Content 2", 0.85, sparseRank, "SPARSE")
        );

        List<ScoredChunk> fused = rrfRanker.fuseAndRank(dense, sparse, 10);

        ScoredChunk resultChunk = fused.stream()
                .filter(c -> c.chunkId().equals(chunkDual))
                .findFirst().orElseThrow();

        // Formula: 1/(60 + rank_dense) + 1/(60 + rank_sparse)
        double expectedScore = (1.0 / (60 + denseRank)) + (1.0 / (60 + sparseRank));
        // 1/62 + 1/63 = 0.016129032 + 0.015873015 = 0.032002047

        // Assert: computed scores match expected values exactly
        assertThat(resultChunk.score()).isCloseTo(expectedScore, Offset.offset(1e-9));

        // For chunk appearing only in dense (rank 1), missing in sparse gets MISSING_RANK=21
        ScoredChunk denseOnlyResult = fused.stream()
                .filter(c -> c.chunkId().equals(chunkDenseOnly))
                .findFirst().orElseThrow();

        double expectedDenseOnlyScore = (1.0 / (60 + 1)) + (1.0 / (60 + RRFRanker.MISSING_RANK));
        assertThat(denseOnlyResult.score()).isCloseTo(expectedDenseOnlyScore, Offset.offset(1e-9));
    }
}

package com.medisense.rag.retrieval;

import com.medisense.rag.retrieval.model.ScoredChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Reciprocal Rank Fusion (RRF) Ranker.
 * <p>
 * Combines dense vector retrieval and sparse BM25 retrieval results.
 * RRF formula:
 * score(chunk) = 1/(60 + dense_rank) + 1/(60 + sparse_rank)
 * where rank is 1-based position in each list.
 * Chunks appearing only in one list get rank=21 for the missing list.
 */
@Component
public class RRFRanker {

    private static final Logger log = LoggerFactory.getLogger(RRFRanker.class);
    public static final int RRF_K = 60;
    public static final int MISSING_RANK = 21;
    public static final int DEFAULT_TOP_K = 20;

    /**
     * Merges and ranks dense and sparse retrieved chunks using Reciprocal Rank Fusion.
     *
     * @param denseResults  dense vector retrieval results (top-20)
     * @param sparseResults sparse BM25 retrieval results (top-20)
     * @param topK          number of merged results to return (default 20)
     * @return deduplicated List of ScoredChunks sorted by RRF score descending
     */
    public List<ScoredChunk> fuseAndRank(List<ScoredChunk> denseResults, List<ScoredChunk> sparseResults, int topK) {
        int limit = topK > 0 ? topK : DEFAULT_TOP_K;
        log.info("Executing Reciprocal Rank Fusion (RRF) on {} dense and {} sparse results (topK limit: {})",
                denseResults != null ? denseResults.size() : 0,
                sparseResults != null ? sparseResults.size() : 0,
                limit);

        if ((denseResults == null || denseResults.isEmpty()) &&
            (sparseResults == null || sparseResults.isEmpty())) {
            return Collections.emptyList();
        }

        List<ScoredChunk> safeDense = denseResults != null ? denseResults : Collections.emptyList();
        List<ScoredChunk> safeSparse = sparseResults != null ? sparseResults : Collections.emptyList();

        // 1. Build rank maps and collect chunk representations (deduplicated by chunk_id)
        Map<UUID, Integer> denseRankMap = new HashMap<>();
        Map<UUID, ScoredChunk> chunkDataMap = new LinkedHashMap<>();

        for (int i = 0; i < safeDense.size(); i++) {
            ScoredChunk c = safeDense.get(i);
            if (c == null || c.chunkId() == null) continue;
            int rank = (c.rank() > 0) ? c.rank() : (i + 1);
            denseRankMap.merge(c.chunkId(), rank, Math::min);
            chunkDataMap.putIfAbsent(c.chunkId(), c);
        }

        Map<UUID, Integer> sparseRankMap = new HashMap<>();
        for (int i = 0; i < safeSparse.size(); i++) {
            ScoredChunk c = safeSparse.get(i);
            if (c == null || c.chunkId() == null) continue;
            int rank = (c.rank() > 0) ? c.rank() : (i + 1);
            sparseRankMap.merge(c.chunkId(), rank, Math::min);
            chunkDataMap.putIfAbsent(c.chunkId(), c);
        }

        // 2. Compute RRF score for all unique chunks
        List<ScoredChunk> mergedList = new ArrayList<>(chunkDataMap.size());

        for (Map.Entry<UUID, ScoredChunk> entry : chunkDataMap.entrySet()) {
            UUID chunkId = entry.getKey();
            ScoredChunk baseChunk = entry.getValue();

            int denseRank = denseRankMap.getOrDefault(chunkId, MISSING_RANK);
            int sparseRank = sparseRankMap.getOrDefault(chunkId, MISSING_RANK);

            double rrfScore = (1.0 / (RRF_K + denseRank)) + (1.0 / (RRF_K + sparseRank));

            mergedList.add(new ScoredChunk(
                    chunkId,
                    baseChunk.sectionHeading(),
                    baseChunk.content(),
                    rrfScore,
                    0, // Assigned after sorting
                    "HYBRID"
            ));
        }

        // 3. Sort by RRF score descending
        mergedList.sort((a, b) -> Double.compare(b.score(), a.score()));

        // 4. Assign 1-based ranks and take topK
        List<ScoredChunk> finalList = new ArrayList<>();
        int count = Math.min(limit, mergedList.size());
        for (int i = 0; i < count; i++) {
            ScoredChunk c = mergedList.get(i);
            finalList.add(new ScoredChunk(
                    c.chunkId(),
                    c.sectionHeading(),
                    c.content(),
                    c.score(),
                    i + 1, // 1-based final rank
                    c.source()
            ));
        }

        log.info("RRF completed, returning top {} deduplicated chunks.", finalList.size());
        return finalList;
    }
}

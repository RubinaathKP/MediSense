package com.medisense.rag.reranking;

import com.medisense.rag.reranking.exception.RerankerUnavailableException;
import com.medisense.rag.reranking.model.*;
import com.medisense.rag.retrieval.model.ScoredChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RerankingOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RerankingOrchestrator.class);
    private static final int MAX_INPUT_CANDIDATES = 20;
    private static final int TARGET_TOP_K = 5;

    private final RerankerClient rerankingClient;

    public RerankingOrchestrator(RerankerClient rerankingClient) {
        this.rerankingClient = rerankingClient;
    }

    public List<RerankedChunk> rerankChunks(String query, List<ScoredChunk> rrfChunks) {
        if (rrfChunks == null || rrfChunks.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoredChunk> candidates = rrfChunks.stream()
                .limit(MAX_INPUT_CANDIDATES)
                .toList();

        Map<UUID, ScoredChunk> chunkMap = candidates.stream()
                .collect(Collectors.toMap(c -> c.chunkId(), Function.identity(), (existing, replacement) -> existing));

        List<PassageDto> passageDtos = candidates.stream()
                .map(c -> new PassageDto(c.chunkId().toString(), c.content()))
                .toList();

        RerankRequest request = new RerankRequest(query, passageDtos, TARGET_TOP_K);

        try {
            RerankResponse response = rerankingClient.rerank(request);
            if (response != null && response.results() != null && !response.results().isEmpty()) {
                List<RerankedChunk> rerankedResults = new ArrayList<>();
                int rank = 1;
                for (RerankResultDto res : response.results()) {
                    try {
                        UUID chunkId = UUID.fromString(res.chunkId());
                        ScoredChunk original = chunkMap.get(chunkId);
                        if (original != null) {
                            rerankedResults.add(new RerankedChunk(
                                    original.chunkId(),
                                    original.sectionHeading(),
                                    original.content(),
                                    res.score(),
                                    rank++,
                                    original.source(),
                                    true
                            ));
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid UUID returned from reranker service: {}", res.chunkId());
                    }
                }
                if (!rerankedResults.isEmpty()) {
                    log.info("Reranking succeeded. Returning {} cross-encoder reranked chunks.", rerankedResults.size());
                    return rerankedResults;
                }
            }
        } catch (RerankerUnavailableException e) {
            log.warn("Reranker service unavailable: {}. Applying RRF fallback.", e.getMessage());
        } catch (Exception e) {
            log.warn("Unexpected failure during reranking: {}. Applying RRF fallback.", e.getMessage(), e);
        }

        return applyFallback(candidates);
    }

    private List<RerankedChunk> applyFallback(List<ScoredChunk> candidates) {
        log.info("Applying RRF fallback (top-{} candidates, is_reranked=false).", Math.min(TARGET_TOP_K, candidates.size()));
        List<RerankedChunk> fallbackList = new ArrayList<>();
        int limit = Math.min(TARGET_TOP_K, candidates.size());

        for (int i = 0; i < limit; i++) {
            ScoredChunk c = candidates.get(i);
            fallbackList.add(new RerankedChunk(
                    c.chunkId(),
                    c.sectionHeading(),
                    c.content(),
                    c.score(),
                    i + 1,
                    c.source(),
                    false
            ));
        }
        return fallbackList;
    }
}

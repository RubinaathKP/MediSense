package com.medisense.rag.reranking;

import com.medisense.rag.reranking.model.RerankedChunk;
import com.medisense.rag.retrieval.model.ScoredChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CrossEncoderReranker {

    private static final Logger log = LoggerFactory.getLogger(CrossEncoderReranker.class);

    private final RerankingOrchestrator rerankingOrchestrator;

    public CrossEncoderReranker(RerankingOrchestrator rerankingOrchestrator) {
        this.rerankingOrchestrator = rerankingOrchestrator;
    }

    public List<String> rerank(String query, List<String> candidatePassages, int topN) {
        log.info("Reranking {} candidate passages using Cross-Encoder model for query.", candidatePassages.size());
        
        List<ScoredChunk> syntheticChunks = new ArrayList<>();
        for (int i = 0; i < candidatePassages.size(); i++) {
            syntheticChunks.add(new ScoredChunk(
                    UUID.nameUUIDFromBytes(("passage-" + i).getBytes()),
                    "Section " + i,
                    candidatePassages.get(i),
                    1.0 / (i + 1),
                    i + 1,
                    "RRF"
            ));
        }

        List<RerankedChunk> reranked = rerankingOrchestrator.rerankChunks(query, syntheticChunks);
        return reranked.stream()
                .limit(topN)
                .map(c -> c.content())
                .collect(Collectors.toList());
    }

    public List<RerankedChunk> rerankScoredChunks(String query, List<ScoredChunk> scoredChunks) {
        return rerankingOrchestrator.rerankChunks(query, scoredChunks);
    }
}

package com.medisense.rag.reranking.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RerankResultDto(
        @JsonProperty("chunk_id") String chunkId,
        @JsonProperty("score") double score
) {
    public RerankResultDto(String chunkId, double score, int rank) {
        this(chunkId, score);
    }
}

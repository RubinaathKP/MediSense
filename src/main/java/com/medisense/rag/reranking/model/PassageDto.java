package com.medisense.rag.reranking.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PassageDto(
        @JsonProperty("chunk_id") String chunkId,
        @JsonProperty("text") String text
) {}

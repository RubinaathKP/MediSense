package com.medisense.rag.generation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CitationDto(
        @JsonProperty("chunk_id") String chunkId,
        @JsonProperty("source_name") String sourceName,
        @JsonProperty("source_year") Integer sourceYear,
        @JsonProperty("evidence_basis") String evidenceBasis
) {}

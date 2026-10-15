package com.medisense.rag.reranking.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RerankResponse(
        @JsonProperty("results") List<RerankResultDto> results
) {}

package com.medisense.rag.reranking.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RerankRequest(
        @JsonProperty("query") String query,
        @JsonProperty("passages") List<PassageDto> passages,
        @JsonProperty("top_k") int topK
) {}

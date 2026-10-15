package com.medisense.rag.generation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MediSenseResponse(
        @JsonProperty("explanation_text") String explanationText,
        @JsonProperty("citations") List<CitationDto> citations,
        @JsonProperty("is_referral") boolean isReferral,
        @JsonProperty("language_out") String languageOut,
        @JsonProperty("insufficient_context") boolean insufficientContext
) {}

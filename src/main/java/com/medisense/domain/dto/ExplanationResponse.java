package com.medisense.domain.dto;

import com.medisense.domain.enums.SafetyDecision;
import java.util.ArrayList;
import java.util.List;

public class ExplanationResponse {
    private String queryId;
    private String plainLanguageExplanation;
    private String translatedExplanation;
    private SafetyDecision safetyDecision;
    private List<CitationDto> citations = new ArrayList<>();
    private Long latencyMs;

    public ExplanationResponse() {}

    public ExplanationResponse(String queryId, String plainLanguageExplanation, String translatedExplanation, SafetyDecision safetyDecision, List<CitationDto> citations, Long latencyMs) {
        this.queryId = queryId;
        this.plainLanguageExplanation = plainLanguageExplanation;
        this.translatedExplanation = translatedExplanation;
        this.safetyDecision = safetyDecision;
        this.citations = citations != null ? citations : new ArrayList<>();
        this.latencyMs = latencyMs;
    }

    public String getQueryId() { return queryId; }
    public void setQueryId(String queryId) { this.queryId = queryId; }

    public String getPlainLanguageExplanation() { return plainLanguageExplanation; }
    public void setPlainLanguageExplanation(String plainLanguageExplanation) { this.plainLanguageExplanation = plainLanguageExplanation; }

    public String getTranslatedExplanation() { return translatedExplanation; }
    public void setTranslatedExplanation(String translatedExplanation) { this.translatedExplanation = translatedExplanation; }

    public SafetyDecision getSafetyDecision() { return safetyDecision; }
    public void setSafetyDecision(SafetyDecision safetyDecision) { this.safetyDecision = safetyDecision; }

    public List<CitationDto> getCitations() { return citations; }
    public void setCitations(List<CitationDto> citations) { this.citations = citations; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String queryId;
        private String plainLanguageExplanation;
        private String translatedExplanation;
        private SafetyDecision safetyDecision;
        private List<CitationDto> citations = new ArrayList<>();
        private Long latencyMs;

        public Builder queryId(String queryId) { this.queryId = queryId; return this; }
        public Builder plainLanguageExplanation(String plainLanguageExplanation) { this.plainLanguageExplanation = plainLanguageExplanation; return this; }
        public Builder translatedExplanation(String translatedExplanation) { this.translatedExplanation = translatedExplanation; return this; }
        public Builder safetyDecision(SafetyDecision safetyDecision) { this.safetyDecision = safetyDecision; return this; }
        public Builder citations(List<CitationDto> citations) { this.citations = citations; return this; }
        public Builder latencyMs(Long latencyMs) { this.latencyMs = latencyMs; return this; }

        public ExplanationResponse build() {
            return new ExplanationResponse(queryId, plainLanguageExplanation, translatedExplanation, safetyDecision, citations, latencyMs);
        }
    }
}

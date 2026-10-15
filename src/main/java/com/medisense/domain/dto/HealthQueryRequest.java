package com.medisense.domain.dto;

import jakarta.validation.constraints.NotBlank;

public class HealthQueryRequest {

    @NotBlank(message = "Session ID cannot be blank")
    private String sessionId;

    @NotBlank(message = "Query text cannot be blank")
    private String queryText;

    private String languagePreference;

    public HealthQueryRequest() {}

    public HealthQueryRequest(String sessionId, String queryText, String languagePreference) {
        this.sessionId = sessionId;
        this.queryText = queryText;
        this.languagePreference = languagePreference;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }

    public String getLanguagePreference() { return languagePreference; }
    public void setLanguagePreference(String languagePreference) { this.languagePreference = languagePreference; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String sessionId;
        private String queryText;
        private String languagePreference;

        public Builder sessionId(String sessionId) { this.sessionId = sessionId; return this; }
        public Builder queryText(String queryText) { this.queryText = queryText; return this; }
        public Builder languagePreference(String languagePreference) { this.languagePreference = languagePreference; return this; }

        public HealthQueryRequest build() {
            return new HealthQueryRequest(sessionId, queryText, languagePreference);
        }
    }
}

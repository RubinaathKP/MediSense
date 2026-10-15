package com.medisense.evaluation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TestCaseDto(
        @JsonProperty("id") String id,
        @JsonProperty("query_text") String queryText,
        @JsonProperty("query_type") String queryType,
        @JsonProperty("language") String language,
        @JsonProperty("expected_classification") String expectedClassification,
        @JsonProperty("expected_is_referral") Boolean expectedIsReferral,
        @JsonProperty("expected_citation_present") Boolean expectedCitationPresent,
        @JsonProperty("domain") String domain
) {}

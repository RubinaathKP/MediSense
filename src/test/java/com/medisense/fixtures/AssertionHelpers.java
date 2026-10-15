package com.medisense.fixtures;

import com.medisense.api.dto.QueryDtos.QueryResponse;
import com.medisense.safety.CitationVerifier.CitationVerificationResult;
import com.medisense.safety.InputSafetyClassifier.DetailedClassificationResult;
import com.medisense.safety.OutputSafetyFilter.OutputFilterResult;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.util.List;

/**
 * Fluent AssertJ custom assertions for MediSense clinical, RAG, and safety domain objects.
 */
public class AssertionHelpers {

    public static DetailedClassificationAssert assertThat(DetailedClassificationResult actual) {
        return new DetailedClassificationAssert(actual);
    }

    public static QueryResponseAssert assertThat(QueryResponse actual) {
        return new QueryResponseAssert(actual);
    }

    public static OutputFilterAssert assertThat(OutputFilterResult actual) {
        return new OutputFilterAssert(actual);
    }

    public static CitationVerificationAssert assertThat(CitationVerificationResult actual) {
        return new CitationVerificationAssert(actual);
    }

    public static void assertIsGrounded(String responseText, List<String> expectedCitations) {
        Assertions.assertThat(responseText).isNotBlank();
        Assertions.assertThat(expectedCitations).isNotEmpty();
        for (String citation : expectedCitations) {
            Assertions.assertThat(responseText).contains(citation);
        }
    }

    public static void assertRefusalWithReferral(String responseText) {
        Assertions.assertThat(responseText)
                .isNotBlank()
                .containsIgnoringCase("doctor");
    }

    public static void assertNoPrescriptiveDirectives(String responseText) {
        Assertions.assertThat(responseText)
                .doesNotContainPattern("(?i)\\byou (should|must) take\\b")
                .doesNotContainPattern("(?i)\\bprescribed dose\\b")
                .doesNotContainPattern("(?i)\\bstart taking\\b");
    }

    // ===================================================================
    // Custom Assert Classes
    // ===================================================================

    public static class DetailedClassificationAssert extends AbstractAssert<DetailedClassificationAssert, DetailedClassificationResult> {
        public DetailedClassificationAssert(DetailedClassificationResult actual) {
            super(actual, DetailedClassificationAssert.class);
        }

        public DetailedClassificationAssert isSafe() {
            isNotNull();
            if (!actual.isSafe()) {
                failWithMessage("Expected classification to be SAFE but was category <%s>", actual.category());
            }
            return this;
        }

        public DetailedClassificationAssert isRefusedWith(String expectedRefusalSubstring) {
            isNotNull();
            if (actual.isSafe()) {
                failWithMessage("Expected query to be refused, but was classified as safe");
            }
            if (actual.refusalResponse() == null || !actual.refusalResponse().contains(expectedRefusalSubstring)) {
                failWithMessage("Expected refusal message containing <%s>, but got <%s>", expectedRefusalSubstring, actual.refusalResponse());
            }
            return this;
        }

        public DetailedClassificationAssert hasCategory(com.medisense.safety.model.QuerySafetyCategory expectedCategory) {
            isNotNull();
            if (actual.category() != expectedCategory) {
                failWithMessage("Expected category <%s> but was <%s>", expectedCategory, actual.category());
            }
            return this;
        }
    }

    public static class QueryResponseAssert extends AbstractAssert<QueryResponseAssert, QueryResponse> {
        public QueryResponseAssert(QueryResponse actual) {
            super(actual, QueryResponseAssert.class);
        }

        public QueryResponseAssert isNotReferral() {
            isNotNull();
            if (actual.is_referral()) {
                failWithMessage("Expected response NOT to be referral, but was flagged as referral: <%s>", actual.response_text());
            }
            return this;
        }

        public QueryResponseAssert isReferral() {
            isNotNull();
            if (!actual.is_referral()) {
                failWithMessage("Expected response to BE a referral, but was not flagged");
            }
            return this;
        }

        public QueryResponseAssert hasCitations() {
            isNotNull();
            if (actual.citations() == null || actual.citations().isEmpty()) {
                failWithMessage("Expected response to have citations, but citations was empty or null");
            }
            return this;
        }

        public QueryResponseAssert containsText(String snippet) {
            isNotNull();
            if (actual.response_text() == null || !actual.response_text().contains(snippet)) {
                failWithMessage("Expected response text to contain <%s>, but was <%s>", snippet, actual.response_text());
            }
            return this;
        }
    }

    public static class OutputFilterAssert extends AbstractAssert<OutputFilterAssert, OutputFilterResult> {
        public OutputFilterAssert(OutputFilterResult actual) {
            super(actual, OutputFilterAssert.class);
        }

        public OutputFilterAssert isSafe() {
            isNotNull();
            if (!actual.isSafe()) {
                failWithMessage("Expected output to be safe, but was flagged for: <%s>", actual.flagReason());
            }
            return this;
        }

        public OutputFilterAssert isFlaggedWithReason(String reasonSubstring) {
            isNotNull();
            if (actual.isSafe()) {
                failWithMessage("Expected output to be flagged, but was marked safe");
            }
            if (actual.flagReason() == null || !actual.flagReason().toLowerCase().contains(reasonSubstring.toLowerCase())) {
                failWithMessage("Expected flag reason containing <%s>, but was <%s>", reasonSubstring, actual.flagReason());
            }
            return this;
        }
    }

    public static class CitationVerificationAssert extends AbstractAssert<CitationVerificationAssert, CitationVerificationResult> {
        public CitationVerificationAssert(CitationVerificationResult actual) {
            super(actual, CitationVerificationAssert.class);
        }

        public CitationVerificationAssert isGrounded() {
            isNotNull();
            if (!actual.isGrounded()) {
                failWithMessage("Expected citations to be grounded, but found hallucinated citations: %s", actual.hallucinatedCitations());
            }
            return this;
        }

        public CitationVerificationAssert hasHallucinatedCitation(String citation) {
            isNotNull();
            if (!actual.hallucinatedCitations().contains(citation)) {
                failWithMessage("Expected hallucinated citation <%s> to be detected, but found: %s", citation, actual.hallucinatedCitations());
            }
            return this;
        }
    }
}

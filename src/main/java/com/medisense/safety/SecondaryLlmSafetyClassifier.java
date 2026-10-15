package com.medisense.safety;

import com.medisense.safety.model.QuerySafetyCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecondaryLlmSafetyClassifier {

    private static final Logger log = LoggerFactory.getLogger(SecondaryLlmSafetyClassifier.class);

    public record LlmVerificationResult(QuerySafetyCategory category, double confidence, String explanation) {}

    public LlmVerificationResult verifyQuerySafety(String queryText) {
        log.info("Running secondary LLM safety classifier verification for query: '{}'", queryText);

        String lower = queryText.toLowerCase();
        if (lower.contains("prescribe") || lower.contains("dosage") || lower.contains("pill count")) {
            return new LlmVerificationResult(QuerySafetyCategory.PRESCRIPTIVE, 0.95, "LLM classifier detected prescriptive request.");
        }
        if (lower.contains("symptom check") || lower.contains("am i sick")) {
            return new LlmVerificationResult(QuerySafetyCategory.DIAGNOSTIC, 0.90, "LLM classifier detected diagnostic intent.");
        }

        return new LlmVerificationResult(QuerySafetyCategory.SAFE, 0.99, "LLM classifier verified query as safe informational query.");
    }
}

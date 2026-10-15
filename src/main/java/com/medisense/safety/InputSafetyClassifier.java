package com.medisense.safety;

import com.medisense.domain.enums.QueryCategory;
import com.medisense.domain.enums.SafetyDecision;
import com.medisense.safety.model.QuerySafetyCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InputSafetyClassifier {

    private static final Logger log = LoggerFactory.getLogger(InputSafetyClassifier.class);
    private static final double SECONDARY_VERIFICATION_THRESHOLD = 0.85;

    private final PatternLibraryLoader patternLibraryLoader;
    private final SecondaryLlmSafetyClassifier secondaryLlmClassifier;

    public InputSafetyClassifier(PatternLibraryLoader patternLibraryLoader,
                                 SecondaryLlmSafetyClassifier secondaryLlmClassifier) {
        this.patternLibraryLoader = patternLibraryLoader;
        this.secondaryLlmClassifier = secondaryLlmClassifier;
    }

    public record DetailedClassificationResult(
            QuerySafetyCategory category,
            boolean isSafe,
            String refusalResponse,
            String matchedPattern,
            double confidence,
            Instant timestamp
    ) {}

    public DetailedClassificationResult classifyInputQuery(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return new DetailedClassificationResult(QuerySafetyCategory.SAFE, true, null, "EMPTY_QUERY", 1.0, Instant.now());
        }

        log.info("Stage 1 - InputQueryClassifier: Classifying query: '{}'", queryText);
        String cleanedQuery = queryText.toLowerCase().trim();

        // 1. Check EMERGENCY
        DetailedClassificationResult emergencyMatch = checkCategory(cleanedQuery, QuerySafetyCategory.EMERGENCY, SafeRefusalResponse.EMERGENCY_REFERRAL);
        if (emergencyMatch != null) return logDecision(emergencyMatch, queryText);

        // 2. Check DRUG_INTERACTION (Specific interaction patterns take precedence over general prescription verbs)
        DetailedClassificationResult interactionMatch = checkCategory(cleanedQuery, QuerySafetyCategory.DRUG_INTERACTION, SafeRefusalResponse.DOCTOR_REFERRAL);
        if (interactionMatch != null) return logDecision(interactionMatch, queryText);

        // 3. Check TREATMENT_SUBSTITUTION
        DetailedClassificationResult substitutionMatch = checkCategory(cleanedQuery, QuerySafetyCategory.TREATMENT_SUBSTITUTION, SafeRefusalResponse.DOCTOR_REFERRAL);
        if (substitutionMatch != null) return logDecision(substitutionMatch, queryText);

        // 4. Check PRESCRIPTIVE
        DetailedClassificationResult prescriptiveMatch = checkCategory(cleanedQuery, QuerySafetyCategory.PRESCRIPTIVE, SafeRefusalResponse.DOCTOR_REFERRAL);
        if (prescriptiveMatch != null) return logDecision(prescriptiveMatch, queryText);

        // Check Drug + Directive combinations for prescriptive intent
        DetailedClassificationResult drugDirectiveMatch = checkDrugDirectives(cleanedQuery);
        if (drugDirectiveMatch != null) return logDecision(drugDirectiveMatch, queryText);

        // 5. Check DIAGNOSTIC
        DetailedClassificationResult diagnosticMatch = checkCategory(cleanedQuery, QuerySafetyCategory.DIAGNOSTIC, SafeRefusalResponse.DOCTOR_REFERRAL);
        if (diagnosticMatch != null) return logDecision(diagnosticMatch, queryText);

        // Calculate initial confidence for SAFE query
        double initialConfidence = calculateSafeConfidence(cleanedQuery);

        if (initialConfidence < SECONDARY_VERIFICATION_THRESHOLD) {
            log.info("Query primary SAFE classification confidence ({}) below threshold ({}), invoking secondary LLM classifier pass.",
                    initialConfidence, SECONDARY_VERIFICATION_THRESHOLD);

            SecondaryLlmSafetyClassifier.LlmVerificationResult llmResult = null;
            if (secondaryLlmClassifier != null) {
                try {
                    llmResult = secondaryLlmClassifier.verifyQuerySafety(queryText);
                } catch (Exception e) {
                    log.warn("Secondary LLM classifier execution skipped or failed: {}", e.getMessage());
                }
            }

            if (llmResult != null && llmResult.category() != QuerySafetyCategory.SAFE) {
                String refusal = llmResult.category() == QuerySafetyCategory.EMERGENCY ? SafeRefusalResponse.EMERGENCY_REFERRAL : SafeRefusalResponse.DOCTOR_REFERRAL;
                DetailedClassificationResult llmDecision = new DetailedClassificationResult(
                        llmResult.category(),
                        false,
                        refusal,
                        "LLM_SECONDARY_VERIFIER: " + llmResult.explanation(),
                        llmResult.confidence(),
                        Instant.now()
                );
                return logDecision(llmDecision, queryText);
            }
            if (llmResult != null) {
                initialConfidence = llmResult.confidence();
            }
        }

        DetailedClassificationResult safeResult = new DetailedClassificationResult(
                QuerySafetyCategory.SAFE,
                true,
                null,
                "NO_UNSAFE_PATTERN_MATCHED",
                initialConfidence,
                Instant.now()
        );
        return logDecision(safeResult, queryText);
    }

    private DetailedClassificationResult checkCategory(String cleanedQuery, QuerySafetyCategory category, String refusalResponse) {
        PatternLibraryLoader.CategoryPatterns patterns = patternLibraryLoader.getPatternsForCategory(category);

        for (String phrase : patterns.exactPhrases()) {
            if (cleanedQuery.contains(phrase)) {
                return new DetailedClassificationResult(category, false, refusalResponse, "EXACT_PHRASE: " + phrase, 1.0, Instant.now());
            }
        }

        for (Pattern pattern : patterns.regexPatterns()) {
            Matcher matcher = pattern.matcher(cleanedQuery);
            if (matcher.find()) {
                return new DetailedClassificationResult(category, false, refusalResponse, "REGEX: " + pattern.pattern(), 1.0, Instant.now());
            }
        }
        return null;
    }

    private DetailedClassificationResult checkDrugDirectives(String cleanedQuery) {
        PatternLibraryLoader.CategoryPatterns patterns = patternLibraryLoader.getPatternsForCategory(QuerySafetyCategory.PRESCRIPTIVE);

        boolean containsDrug = false;
        String matchedDrug = null;
        for (String drug : patterns.directiveDrugs()) {
            if (cleanedQuery.contains(drug.toLowerCase())) {
                containsDrug = true;
                matchedDrug = drug;
                break;
            }
        }

        if (containsDrug) {
            for (String verb : patterns.directiveVerbs()) {
                if (cleanedQuery.contains(verb.toLowerCase())) {
                    return new DetailedClassificationResult(
                            QuerySafetyCategory.PRESCRIPTIVE,
                            false,
                            SafeRefusalResponse.DOCTOR_REFERRAL,
                            "DRUG_DIRECTIVE_COMBO: [" + matchedDrug + " + " + verb + "]",
                            1.0,
                            Instant.now()
                    );
                }
            }
        }
        return null;
    }

    private double calculateSafeConfidence(String query) {
        if (query.contains("dose") || query.contains("take") || query.contains("medication")) {
            return 0.80; // Triggers secondary LLM pass
        }
        return 0.98;
    }

    private DetailedClassificationResult logDecision(DetailedClassificationResult result, String query) {
        if (result.isSafe()) {
            log.info("Stage 1 - InputQueryClassifier Decision: SAFE | Confidence: {} | Pattern: {} | Query: '{}'",
                    result.confidence(), result.matchedPattern(), query);
        } else {
            log.warn("Stage 1 - InputQueryClassifier Intercepted Query! Category: {} | Refusal Type: {} | Pattern: '{}' | Query: '{}'",
                    result.category(), result.matchedPattern(), result.matchedPattern(), query);
        }
        return result;
    }

    public SafetyClassificationResult classifyQuery(String queryText) {
        DetailedClassificationResult result = classifyInputQuery(queryText);
        if (result.isSafe()) {
            return new SafetyClassificationResult(SafetyDecision.PASSED, QueryCategory.REPORT_EXPLANATION, null);
        }
        SafetyDecision decision = result.category() == QuerySafetyCategory.EMERGENCY ? SafetyDecision.REFUSED_EMERGENCY : SafetyDecision.REFUSED_PRESCRIPTIVE;
        QueryCategory category = result.category() == QuerySafetyCategory.EMERGENCY ? QueryCategory.EMERGENCY_REFUSAL : QueryCategory.PRESCRIPTIVE_REFUSAL;
        return new SafetyClassificationResult(decision, category, result.refusalResponse());
    }

    public record SafetyClassificationResult(
            SafetyDecision decision,
            QueryCategory category,
            String refusalResponse
    ) {}
}

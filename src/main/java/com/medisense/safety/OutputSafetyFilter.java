package com.medisense.safety;

import com.medisense.domain.enums.SafetyDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OutputSafetyFilter {

    private static final Logger log = LoggerFactory.getLogger(OutputSafetyFilter.class);

    private static final Pattern DOSAGE_UNIT_PATTERN =
            Pattern.compile("(?i)\\b\\d+(\\.\\d+)?\\s*(mg|ml|mcg|IU|units|tablets|capsules)\\b");

    private static final List<Pattern> DIRECTIVE_PHRASING_PATTERNS = List.of(
            Pattern.compile("(?i).*you should take.*"),
            Pattern.compile("(?i).*you must take.*"),
            Pattern.compile("(?i).*you should avoid.*"),
            Pattern.compile("(?i).*you must avoid.*"),
            Pattern.compile("(?i).*take this medication.*"),
            Pattern.compile("(?i).*avoid this medication.*"),
            Pattern.compile("(?i).*stop taking.*"),
            Pattern.compile("(?i).*increase your dosage.*"),
            Pattern.compile("(?i).*decrease your dosage.*")
    );

    private static final List<Pattern> INTERACTION_LANGUAGE_PATTERNS = List.of(
            Pattern.compile("(?i).*interacts? with.*"),
            Pattern.compile("(?i).*do not combine.*"),
            Pattern.compile("(?i).*contraindicated with.*")
    );

    private static final List<String> COMMON_DRUG_NAMES = List.of(
            "metformin", "atorvastatin", "amlodipine", "losartan", "lisinopril",
            "aspirin", "paracetamol", "ibuprofen", "insulin", "glimepiride",
            "telmisartan", "hydroxychloroquine", "azithromycin", "statin", "beta blocker"
    );

    private static final List<String> PRESCRIPTIVE_WINDOW_VERBS = List.of(
            "take", "avoid", "stop", "start", "increase", "decrease", "administer", "prescribed dose"
    );

    public record OutputFilterResult(
            boolean isSafe,
            String filteredContent,
            String flagReason,
            String snippet
    ) {}

    public OutputFilterResult filterGeneratedOutput(String generatedOutput) {
        if (generatedOutput == null || generatedOutput.isBlank()) {
            return new OutputFilterResult(true, generatedOutput, null, null);
        }

        log.info("Stage 2 - OutputSafetyFilter: Scanning generated response against safety rules.");

        // 1. Check Drug Name within 5 tokens of Prescriptive Verb
        String drugWindowFlag = checkDrugPrescriptiveWindow(generatedOutput);
        if (drugWindowFlag != null) {
            log.warn("Stage 2 Output Filter FLAG: Drug name within 5-token prescriptive window. Details: {}", drugWindowFlag);
            return new OutputFilterResult(false, SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT, "DRUG_PRESCRIPTIVE_WINDOW", drugWindowFlag);
        }

        // 2. Check Dosage Unit Patterns
        Matcher dosageMatcher = DOSAGE_UNIT_PATTERN.matcher(generatedOutput);
        if (dosageMatcher.find()) {
            String snippet = dosageMatcher.group();
            log.warn("Stage 2 Output Filter FLAG: Specific dosage unit pattern detected: '{}'", snippet);
            return new OutputFilterResult(false, SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT, "DOSAGE_PATTERN_DETECTED", snippet);
        }

        // 3. Check Directive Phrasing
        for (Pattern pattern : DIRECTIVE_PHRASING_PATTERNS) {
            Matcher m = pattern.matcher(generatedOutput);
            if (m.find()) {
                String snippet = m.group();
                log.warn("Stage 2 Output Filter FLAG: Directive medical phrasing detected: '{}'", snippet);
                return new OutputFilterResult(false, SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT, "DIRECTIVE_MEDICAL_PHRASING", snippet);
            }
        }

        // 4. Check Interaction Language
        for (Pattern pattern : INTERACTION_LANGUAGE_PATTERNS) {
            Matcher m = pattern.matcher(generatedOutput);
            if (m.find()) {
                String snippet = m.group();
                log.warn("Stage 2 Output Filter FLAG: Drug interaction language detected: '{}'", snippet);
                return new OutputFilterResult(false, SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT, "INTERACTION_LANGUAGE_DETECTED", snippet);
            }
        }

        log.info("Stage 2 - OutputSafetyFilter PASSED.");
        return new OutputFilterResult(true, generatedOutput, null, null);
    }

    private String checkDrugPrescriptiveWindow(String text) {
        String[] tokens = text.toLowerCase().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].replaceAll("[^a-z0-9]", "");
            for (String drug : COMMON_DRUG_NAMES) {
                if (token.equals(drug.toLowerCase())) {
                    int start = Math.max(0, i - 5);
                    int end = Math.min(tokens.length - 1, i + 5);

                    for (int j = start; j <= end; j++) {
                        if (j == i) continue;
                        String windowToken = tokens[j].replaceAll("[^a-z0-9]", "");
                        for (String verb : PRESCRIPTIVE_WINDOW_VERBS) {
                            if (windowToken.contains(verb.toLowerCase())) {
                                String snippet = String.join(" ", Arrays.copyOfRange(tokens, start, end + 1));
                                return "[" + drug + " near verb '" + verb + "' in window: \"" + snippet + "\"]";
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public LegacyOutputFilterResult filterOutput(String generatedOutput) {
        OutputFilterResult result = filterGeneratedOutput(generatedOutput);
        if (result.isSafe()) {
            return new LegacyOutputFilterResult(SafetyDecision.PASSED, generatedOutput);
        }
        return new LegacyOutputFilterResult(SafetyDecision.REFUSED_PRESCRIPTIVE, SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT);
    }

    public record LegacyOutputFilterResult(
            SafetyDecision decision,
            String filteredContent
    ) {}
}

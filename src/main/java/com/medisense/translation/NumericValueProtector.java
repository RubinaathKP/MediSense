package com.medisense.translation;

import com.medisense.domain.repository.DrugLookupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NumericValueProtector {

    private static final Logger log = LoggerFactory.getLogger(NumericValueProtector.class);

    private static final Pattern NUMERIC_UNIT_PATTERN = Pattern.compile(
            "\\b\\d+\\.?\\d*\\s*(mg/dL|mmol/L|units|IU|mcg|mg|ml|g|kg|cm|bpm|mmHg|%)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final ProtectedTermsLoader protectedTermsLoader;
    private final DrugLookupRepository drugLookupRepository;

    public NumericValueProtector(ProtectedTermsLoader protectedTermsLoader,
                                 DrugLookupRepository drugLookupRepository) {
        this.protectedTermsLoader = protectedTermsLoader;
        this.drugLookupRepository = drugLookupRepository;
    }

    public record ProtectionResult(
            String maskedText,
            Map<String, String> placeholderRegistry
    ) {}

    public ProtectionResult protectValues(String input) {
        if (input == null || input.isBlank()) {
            return new ProtectionResult(input, Collections.emptyMap());
        }

        Map<String, String> registry = new LinkedHashMap<>();
        String text = input;

        // 1. Protect Numeric + Unit Patterns (e.g. "145 mg/dL")
        Matcher numMatcher = NUMERIC_UNIT_PATTERN.matcher(text);
        int numIndex = 1;
        StringBuffer sbNum = new StringBuffer();
        while (numMatcher.find()) {
            String match = numMatcher.group();
            String placeholder = "{NUM_" + numIndex++ + "}";
            registry.put(placeholder, match);
            numMatcher.appendReplacement(sbNum, Matcher.quoteReplacement(placeholder));
        }
        numMatcher.appendTail(sbNum);
        text = sbNum.toString();

        // 2. Protect Protected Lab Terms from YAML (e.g. "LDL Cholesterol")
        Set<String> terms = protectedTermsLoader != null ? protectedTermsLoader.getProtectedTerms() : Set.of();
        int termIndex = 1;
        for (String term : terms) {
            if (term != null && !term.isBlank() && text.toLowerCase().contains(term.toLowerCase())) {
                Pattern termPattern = Pattern.compile("\\b" + Pattern.quote(term) + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher m = termPattern.matcher(text);
                if (m.find()) {
                    String actualMatch = m.group();
                    String placeholder = "{TERM_" + termIndex++ + "}";
                    registry.put(placeholder, actualMatch);
                    text = m.replaceAll(Matcher.quoteReplacement(placeholder));
                }
            }
        }

        // 3. Protect Drug Names from Database / Known Drugs (e.g. "Atorvastatin")
        Set<String> drugNames = fetchKnownDrugNames();
        int drugIndex = 1;
        for (String drug : drugNames) {
            if (drug != null && !drug.isBlank() && text.toLowerCase().contains(drug.toLowerCase())) {
                Pattern drugPattern = Pattern.compile("\\b" + Pattern.quote(drug) + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher m = drugPattern.matcher(text);
                if (m.find()) {
                    String actualMatch = m.group();
                    String placeholder = "{DRUG_" + drugIndex++ + "}";
                    registry.put(placeholder, actualMatch);
                    text = m.replaceAll(Matcher.quoteReplacement(placeholder));
                }
            }
        }

        log.debug("Protected {} values with placeholders in text.", registry.size());
        return new ProtectionResult(text, registry);
    }

    public String restoreValues(String translatedText, String originalText, Map<String, String> registry) {
        if (translatedText == null || registry == null || registry.isEmpty()) {
            return translatedText;
        }

        String restored = translatedText;
        for (Map.Entry<String, String> entry : registry.entrySet()) {
            String placeholder = entry.getKey();
            String originalValue = entry.getValue();

            if (!restored.contains(placeholder)) {
                log.error("Translation integrity check failure! Missing placeholder '{}' (Original: '{}') in translated output: '{}'",
                        placeholder, originalValue, translatedText);
                throw new TranslationIntegrityException(
                        "Placeholder '" + placeholder + "' lost during translation.",
                        originalText,
                        translatedText,
                        placeholder
                );
            }

            restored = restored.replace(placeholder, originalValue);
        }

        return restored;
    }

    private Set<String> fetchKnownDrugNames() {
        Set<String> drugs = new HashSet<>(List.of(
                "Atorvastatin", "Metformin", "Amlodipine", "Losartan", "Rosuvastatin",
                "Omeprazole", "Paracetamol", "Aspirin", "Clopidogrel", "Metoprolol"
        ));

        if (drugLookupRepository != null) {
            try {
                drugLookupRepository.findAll().forEach(entity -> {
                    if (entity.getBrandName() != null) drugs.add(entity.getBrandName());
                    if (entity.getGenericName() != null) drugs.add(entity.getGenericName());
                });
            } catch (Exception e) {
                log.debug("Could not fetch drugs from DB repository, using default drug set.");
            }
        }
        return drugs;
    }
}

package com.medisense.translation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class IntegrityVerifier {

    private static final Logger log = LoggerFactory.getLogger(IntegrityVerifier.class);

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\b\\d+\\.?\\d*\\b");

    public record IntegrityCheckResult(
            boolean passed,
            String failureReason
    ) {}

    public IntegrityCheckResult verifyIntegrity(String originalText, String translatedText, Map<String, String> protectedValues) {
        if (originalText == null || translatedText == null) {
            return new IntegrityCheckResult(false, "Original or translated text is null.");
        }

        // 1. Verify numeric value preservation
        List<String> originalNumbers = extractMatches(originalText, NUMERIC_PATTERN);
        for (String num : originalNumbers) {
            if (!translatedText.contains(num)) {
                log.warn("Numeric integrity failure: Original number '{}' missing from translated output.", num);
                return new IntegrityCheckResult(false, "Numeric value '" + num + "' was modified or lost during translation.");
            }
        }

        // 2. Verify protected drug names & terms preservation
        if (protectedValues != null) {
            for (Map.Entry<String, String> entry : protectedValues.entrySet()) {
                String originalVal = entry.getValue();
                if (!translatedText.toLowerCase().contains(originalVal.toLowerCase())) {
                    log.warn("Term integrity failure: Protected term '{}' missing from translated output.", originalVal);
                    return new IntegrityCheckResult(false, "Protected term/drug '" + originalVal + "' was lost or altered.");
                }
            }
        }

        // 3. Verify sentence count bounds (±2) to detect truncation
        int origSentenceCount = countSentences(originalText);
        int transSentenceCount = countSentences(translatedText);
        int sentenceDiff = Math.abs(origSentenceCount - transSentenceCount);

        if (sentenceDiff > 2) {
            log.warn("Sentence count integrity failure: Original sentences = {}, Translated sentences = {} (diff = {} > 2).",
                    origSentenceCount, transSentenceCount, sentenceDiff);
            return new IntegrityCheckResult(false, "Possible sentence truncation: sentence count changed from "
                    + origSentenceCount + " to " + transSentenceCount + ".");
        }

        log.info("Translation integrity verification PASSED cleanly.");
        return new IntegrityCheckResult(true, null);
    }

    private List<String> extractMatches(String text, Pattern pattern) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
    }

    private int countSentences(String text) {
        if (text == null || text.isBlank()) return 0;
        String[] sentences = text.split("[.!?]+");
        int count = 0;
        for (String s : sentences) {
            if (!s.trim().isBlank()) count++;
        }
        return Math.max(count, 1);
    }
}

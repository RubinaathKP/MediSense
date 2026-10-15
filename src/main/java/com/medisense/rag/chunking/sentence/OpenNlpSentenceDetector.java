package com.medisense.rag.chunking.sentence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class OpenNlpSentenceDetector {

    private static final Logger log = LoggerFactory.getLogger(OpenNlpSentenceDetector.class);

    // Sentence boundary regex splitting on terminating punctuation (. ! ?) followed by whitespace
    private static final Pattern SENTENCE_BOUNDARY_PATTERN = Pattern.compile("(?<=[.!?])\\s+");

    public String[] detectSentences(String text) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }

        String[] rawSentences = SENTENCE_BOUNDARY_PATTERN.split(text.trim());
        List<String> validSentences = new ArrayList<>();

        for (String sentence : rawSentences) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty()) {
                validSentences.add(trimmed);
            }
        }

        log.trace("Split section text into {} sentences without mid-sentence cuts.", validSentences.size());
        return validSentences.toArray(new String[0]);
    }
}

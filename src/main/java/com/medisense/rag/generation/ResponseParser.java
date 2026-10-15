package com.medisense.rag.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.rag.generation.model.MediSenseResponse;
import com.medisense.safety.SafeRefusalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class ResponseParser {

    private static final Logger log = LoggerFactory.getLogger(ResponseParser.class);
    private final ObjectMapper objectMapper;

    public ResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MediSenseResponse parseResponse(String rawOutput, String languageOut) {
        String lang = (languageOut != null && !languageOut.isBlank()) ? languageOut : "en";

        if (rawOutput == null || rawOutput.isBlank()) {
            log.warn("Raw LLM output is empty or null. Returning InsufficientContextResponse.");
            return createInsufficientContextResponse(lang);
        }

        String cleaned = rawOutput.trim();

        if (cleaned.contains("INSUFFICIENT_CONTEXT")) {
            log.info("LLM returned INSUFFICIENT_CONTEXT signal. Creating handled insufficient context response.");
            return createInsufficientContextResponse(lang);
        }

        cleaned = stripMarkdownCodeFences(cleaned);

        try {
            MediSenseResponse response = objectMapper.readValue(cleaned, MediSenseResponse.class);

            if (response.insufficientContext()) {
                log.info("Parsed MediSenseResponse has insufficient_context flag set to true.");
                return createInsufficientContextResponse(lang);
            }

            log.info("Successfully parsed MediSenseResponse JSON with {} citations.",
                    response.citations() != null ? response.citations().size() : 0);

            return new MediSenseResponse(
                    response.explanationText() != null ? response.explanationText() : "",
                    response.citations() != null ? response.citations() : Collections.emptyList(),
                    response.isReferral(),
                    response.languageOut() != null ? response.languageOut() : lang,
                    false
            );
        } catch (Exception e) {
            log.error("Failed to parse raw LLM JSON output. Output snippet: '{}', Error: {}",
                    cleaned.length() > 100 ? cleaned.substring(0, 100) + "..." : cleaned, e.getMessage());

            return createParseFailureResponse(lang);
        }
    }

    private String stripMarkdownCodeFences(String text) {
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline != -1) {
                text = text.substring(firstNewline + 1);
            }
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }

    public MediSenseResponse createInsufficientContextResponse(String lang) {
        return new MediSenseResponse(
                SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT,
                Collections.emptyList(),
                true,
                lang,
                true
        );
    }

    public MediSenseResponse createParseFailureResponse(String lang) {
        return new MediSenseResponse(
                SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT,
                Collections.emptyList(),
                true,
                lang,
                true
        );
    }
}

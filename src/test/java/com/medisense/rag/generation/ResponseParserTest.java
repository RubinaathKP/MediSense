package com.medisense.rag.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.rag.generation.model.MediSenseResponse;
import com.medisense.safety.SafeRefusalResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseParserTest {

    private ObjectMapper objectMapper;
    private ResponseParser parser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        parser = new ResponseParser(objectMapper);
    }

    @Test
    @DisplayName("Parse Valid MediSenseResponse JSON")
    void testParseValidJson() {
        String json = """
                {
                  "explanation_text": "HbA1c level of 7.2 percent indicates elevated blood sugar [Source: Chapter 1].",
                  "citations": [
                    {
                      "chunk_id": "chunk-101",
                      "source_name": "Chapter 1",
                      "source_year": 2026,
                      "evidence_basis": "clinical_trial"
                    }
                  ],
                  "is_referral": false,
                  "language_out": "en",
                  "insufficient_context": false
                }
                """;

        MediSenseResponse response = parser.parseResponse(json, "en");
        assertNotNull(response);
        assertFalse(response.insufficientContext());
        assertFalse(response.isReferral());
        assertEquals("HbA1c level of 7.2 percent indicates elevated blood sugar [Source: Chapter 1].", response.explanationText());
        assertEquals(1, response.citations().size());
        assertEquals("chunk-101", response.citations().get(0).chunkId());
    }

    @Test
    @DisplayName("Strip Markdown Code Fences Before Parsing")
    void testStripMarkdownCodeFences() {
        String fencedJson = """
                ```json
                {
                  "explanation_text": "Normal blood pressure is below 120/80 mmHg [Source: Guideline].",
                  "citations": [],
                  "is_referral": false,
                  "language_out": "en",
                  "insufficient_context": false
                }
                ```
                """;

        MediSenseResponse response = parser.parseResponse(fencedJson, "en");
        assertNotNull(response);
        assertFalse(response.insufficientContext());
        assertEquals("Normal blood pressure is below 120/80 mmHg [Source: Guideline].", response.explanationText());
    }

    @Test
    @DisplayName("Handle INSUFFICIENT_CONTEXT Signal")
    void testHandleInsufficientContextSignal() {
        MediSenseResponse response = parser.parseResponse("INSUFFICIENT_CONTEXT", "en");
        assertNotNull(response);
        assertTrue(response.insufficientContext());
        assertTrue(response.isReferral());
        assertEquals(SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT, response.explanationText());
    }

    @Test
    @DisplayName("Handle Malformed JSON Parse Failure Gracefully")
    void testHandleJsonParseFailure() {
        String malformedJson = "This is not a JSON object { invalid content }";
        MediSenseResponse response = parser.parseResponse(malformedJson, "en");

        assertNotNull(response);
        assertTrue(response.insufficientContext());
        assertTrue(response.isReferral());
        assertEquals(SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT, response.explanationText());
    }
}

package com.medisense.safety;

import com.medisense.safety.exception.HallucinatedCitationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CitationVerifierTest {

    private CitationVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new CitationVerifier();
    }

    @Test
    @DisplayName("Verify Grounded Response with Matching Chunk IDs")
    void testGroundedCitationsPass() {
        String responseText = "HbA1c levels above 7.0 percent indicate suboptimal glycemic control [Source: chunk-guideline-sec1].";
        List<String> retrievedChunkIds = List.of("chunk-guideline-sec1", "chunk-guideline-sec2");

        var result = verifier.verifyCitations(responseText, retrievedChunkIds);
        assertTrue(result.isGrounded());
        assertTrue(result.hallucinatedCitations().isEmpty());
    }

    @Test
    @DisplayName("Flag Hallucinated Citation Not in Retrieved Context")
    void testHallucinatedCitationFails() {
        String responseText = "HbA1c levels above 7.0 percent indicate suboptimal glycemic control [Source: fake-hallucinated-chunk-999].";
        List<String> retrievedChunkIds = List.of("chunk-guideline-sec1", "chunk-guideline-sec2");

        var result = verifier.verifyCitations(responseText, retrievedChunkIds);
        assertFalse(result.isGrounded());
        assertTrue(result.hallucinatedCitations().contains("fake-hallucinated-chunk-999"));
    }

    @Test
    @DisplayName("Verify verifyOrThrow throws HallucinatedCitationException on mismatch")
    void testVerifyOrThrowException() {
        String responseText = "Random text [Source: ungrounded-source-123].";
        List<String> retrievedChunkIds = List.of("valid-source-1");

        assertThrows(HallucinatedCitationException.class, () -> verifier.verifyOrThrow(responseText, retrievedChunkIds));
    }
}

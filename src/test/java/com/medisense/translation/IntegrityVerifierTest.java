package com.medisense.translation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IntegrityVerifierTest {

    private IntegrityVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new IntegrityVerifier();
    }

    @Test
    void testVerifyIntegrityPassesWhenValuesUnchanged() {
        String orig = "Your LDL is 145 mg/dL. Atorvastatin was prescribed.";
        String trans = "உங்களின் LDL 145 mg/dL ஆகும். Atorvastatin பரிந்துரைக்கப்பட்டது.";

        IntegrityVerifier.IntegrityCheckResult result = verifier.verifyIntegrity(
                orig, trans, Map.of("{DRUG_1}", "Atorvastatin")
        );

        assertTrue(result.passed());
        assertNull(result.failureReason());
    }

    @Test
    void testVerifyIntegrityFailsWhenNumericValueModified() {
        String orig = "Your LDL is 145 mg/dL.";
        String trans = "உங்களின் LDL 150 mg/dL ஆகும்."; // Changed 145 to 150

        IntegrityVerifier.IntegrityCheckResult result = verifier.verifyIntegrity(
                orig, trans, Map.of()
        );

        assertFalse(result.passed());
        assertTrue(result.failureReason().contains("145"));
    }

    @Test
    void testVerifyIntegrityFailsWhenSentenceCountTruncated() {
        String orig = "First sentence. Second sentence. Third sentence. Fourth sentence.";
        String trans = "முதல் வாக்கியம் மட்டுமே உள்ளது."; // 1 sentence instead of 4

        IntegrityVerifier.IntegrityCheckResult result = verifier.verifyIntegrity(
                orig, trans, Map.of()
        );

        assertFalse(result.passed());
        assertTrue(result.failureReason().contains("truncation"));
    }
}

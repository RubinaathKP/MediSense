package com.medisense.translation;

import com.medisense.translation.model.TranslationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class TranslationPipelineIntegrationTest {

    private SarvamTranslationService translationService;

    @BeforeEach
    void setUp() {
        ProtectedTermsLoader termsLoader = new ProtectedTermsLoader();
        termsLoader.init();

        NumericValueProtector valueProtector = new NumericValueProtector(termsLoader, null);
        IntegrityVerifier integrityVerifier = new IntegrityVerifier();

        translationService = new SarvamTranslationService(
                valueProtector, integrityVerifier, null, new RestTemplate());

        // Uses mock key to activate mock translation pipeline in sandbox environment
        ReflectionTestUtils.setField(translationService, "apiKey", "mock-sarvam-key");
        ReflectionTestUtils.setField(translationService, "baseUrl", "https://api.sarvam.ai");
    }

    @Test
    void testLdlAndAtorvastatinSurviveTranslationUnchanged() {
        String originalText = "Your reported LDL Cholesterol is 145 mg/dL. Your doctor prescribed Atorvastatin 10 mg daily for management.";

        TranslationResult result = translationService.translate(originalText, "ta");

        assertNotNull(result.translatedText());
        assertFalse(result.translationUnavailable(), "Translation should complete successfully");
        assertFalse(result.integrityCheckFailed(), "Integrity check must pass cleanly");

        // Per FR-49 and FR-50: numeric lab values, units, and drug names MUST survive translation intact
        assertTrue(result.translatedText().contains("145 mg/dL"), "Numeric value and unit '145 mg/dL' must survive translation intact!");
        assertTrue(result.translatedText().contains("Atorvastatin"), "Drug name 'Atorvastatin' must survive translation intact!");
        assertTrue(result.translatedText().contains("LDL Cholesterol"), "Protected lab test name 'LDL Cholesterol' must survive translation intact!");
    }
}

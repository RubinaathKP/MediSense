package com.medisense.translation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LanguageDetectionServiceTest {

    private LanguageDetectorService detectorService;

    @BeforeEach
    void setUp() {
        detectorService = new LanguageDetectorService();
    }

    @Test
    void testDetectTamilScript() {
        String query = "என் இரத்த சர்க்கரை அளவு என்ன?";
        LanguageDetectionResult result = detectorService.detectLanguageResult(query);

        assertEquals("ta", result.detectedLanguage());
        assertEquals("TAMIL", result.scriptType());
        assertTrue(result.confidence() > 0.9);
    }

    @Test
    void testDetectTanglishCodeMixedPhonemes() {
        String query = "Enakku ldl test result epdi irukku doctoru? Nalladhu aama?";
        LanguageDetectionResult result = detectorService.detectLanguageResult(query);

        assertEquals("tanglish", result.detectedLanguage());
        assertTrue(result.isCodeMixed());
        assertEquals("LATIN", result.scriptType());
    }

    @Test
    void testDetectEnglishQuery() {
        String query = "What does a high LDL cholesterol level mean for heart health?";
        LanguageDetectionResult result = detectorService.detectLanguageResult(query);

        assertEquals("en", result.detectedLanguage());
        assertFalse(result.isCodeMixed());
        assertEquals("LATIN", result.scriptType());
    }
}

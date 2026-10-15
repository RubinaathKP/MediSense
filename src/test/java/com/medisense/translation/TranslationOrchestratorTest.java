package com.medisense.translation;

import com.medisense.translation.indictrans2.IndicTrans2Client;
import com.medisense.translation.indictrans2.model.IndicTrans2Response;
import com.medisense.translation.model.TranslationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TranslationOrchestratorTest {

    private SarvamTranslationService sarvamService;
    private IndicTrans2Client indicTrans2Client;
    private NumericValueProtector valueProtector;
    private IntegrityVerifier integrityVerifier;
    private TranslationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        sarvamService = mock(SarvamTranslationService.class);
        indicTrans2Client = mock(IndicTrans2Client.class);

        ProtectedTermsLoader termsLoader = new ProtectedTermsLoader();
        termsLoader.init();
        valueProtector = new NumericValueProtector(termsLoader, null);
        integrityVerifier = new IntegrityVerifier();

        orchestrator = new TranslationOrchestrator(
                sarvamService, indicTrans2Client, valueProtector, integrityVerifier);
    }

    @Test
    void testPrimaryProviderSarvamSuccess() {
        String input = "Your LDL is 145 mg/dL.";
        TranslationResult sarvamSuccess = TranslationResult.success(
                "உங்களின் LDL 145 mg/dL ஆகும்.", input, "en", "ta");

        when(sarvamService.translate(input, "ta")).thenReturn(sarvamSuccess);

        TranslationResult result = orchestrator.translateToTamil(input);

        assertFalse(result.translationUnavailable());
        assertFalse(result.integrityCheckFailed());
        assertEquals("உங்களின் LDL 145 mg/dL ஆகும்.", result.translatedText());
        verify(indicTrans2Client, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void testSarvamFailureFallbacksToIndicTrans2Success() {
        String input = "Your LDL is 145 mg/dL.";
        TranslationResult sarvamFailed = TranslationResult.fallbackUnavailable(input, "Sarvam API timeout");

        when(sarvamService.translate(input, "ta")).thenReturn(sarvamFailed);
        when(indicTrans2Client.translate(anyString(), eq("eng_Latn"), eq("tam_Tam")))
                .thenReturn(new IndicTrans2Response("உங்களின் {TERM_1} அளவு {NUM_1} ஆகும்.", "eng_Latn", "tam_Tam"));

        TranslationResult result = orchestrator.translateToTamil(input);

        assertFalse(result.translationUnavailable());
        assertFalse(result.integrityCheckFailed());
        assertTrue(result.translatedText().contains("145 mg/dL"));
        verify(indicTrans2Client).translate(anyString(), eq("eng_Latn"), eq("tam_Tam"));
    }

    @Test
    void testBothProvidersFailureReturnsSafeEnglishFallback() {
        String input = "Your LDL is 145 mg/dL.";
        TranslationResult sarvamFailed = TranslationResult.fallbackUnavailable(input, "Sarvam API 503");

        when(sarvamService.translate(input, "ta")).thenReturn(sarvamFailed);
        when(indicTrans2Client.translate(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("IndicTrans2 microservice offline"));

        TranslationResult result = orchestrator.translateToTamil(input);

        assertTrue(result.translationUnavailable());
        assertEquals(input, result.translatedText());
    }
}

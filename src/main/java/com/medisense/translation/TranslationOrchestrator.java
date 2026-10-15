package com.medisense.translation;

import com.medisense.translation.indictrans2.IndicTrans2Client;
import com.medisense.translation.indictrans2.model.IndicTrans2Response;
import com.medisense.translation.model.TranslationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TranslationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TranslationOrchestrator.class);

    private final SarvamTranslationService sarvamTranslationService;
    private final IndicTrans2Client indicTrans2Client;
    private final NumericValueProtector valueProtector;
    private final IntegrityVerifier integrityVerifier;

    public TranslationOrchestrator(SarvamTranslationService sarvamTranslationService,
                                   IndicTrans2Client indicTrans2Client,
                                   NumericValueProtector valueProtector,
                                   IntegrityVerifier integrityVerifier) {
        this.sarvamTranslationService = sarvamTranslationService;
        this.indicTrans2Client = indicTrans2Client;
        this.valueProtector = valueProtector;
        this.integrityVerifier = integrityVerifier;
    }

    public TranslationResult translateToTamil(String englishText) {
        if (englishText == null || englishText.isBlank()) {
            return TranslationResult.success(englishText, englishText, "en", "ta");
        }

        // 1. Mask numeric values, drug names, and protected terms
        NumericValueProtector.ProtectionResult protection = valueProtector.protectValues(englishText);
        String maskedText = protection.maskedText();

        String rawTranslatedOutput = null;
        String activeProvider = "SARVAM_AI";

        // 2. Attempt Primary Provider: Sarvam AI
        try {
            log.info("Attempting Primary Translation Provider: Sarvam AI");
            TranslationResult sarvamResult = sarvamTranslationService.translate(englishText, "ta");
            if (!sarvamResult.translationUnavailable() && !sarvamResult.integrityCheckFailed()) {
                log.info("Primary Provider (Sarvam AI) succeeded cleanly.");
                return sarvamResult;
            } else {
                log.warn("Primary Provider (Sarvam AI) unavailable or failed: {}", sarvamResult.failureReason());
            }
        } catch (Exception e) {
            log.warn("Primary Provider (Sarvam AI) threw exception: {}. Initiating secondary fallback.", e.getMessage());
        }

        // 3. Attempt Secondary Fallback Provider: AI4Bharat IndicTrans2
        try {
            log.info("Attempting Secondary Fallback Translation Provider: AI4Bharat IndicTrans2");
            IndicTrans2Response indicResponse = indicTrans2Client.translate(maskedText, "eng_Latn", "tam_Tam");
            if (indicResponse != null && indicResponse.translated_text() != null) {
                rawTranslatedOutput = indicResponse.translated_text();
                activeProvider = "AI4BHARAT_INDICTRANS2";
                log.info("Secondary Fallback Provider (AI4Bharat IndicTrans2) returned translated output.");
            }
        } catch (Exception e) {
            log.warn("Secondary Fallback Provider (AI4Bharat IndicTrans2) unavailable or failed: {}", e.getMessage());
        }

        // 4. Safe English Fallback if both providers failed
        if (rawTranslatedOutput == null) {
            log.error("All translation providers (Sarvam AI & AI4Bharat IndicTrans2) failed or unavailable. Returning English fallback.");
            return TranslationResult.fallbackUnavailable(englishText, "All translation providers (Sarvam AI & AI4Bharat IndicTrans2) failed.");
        }

        // 5. Placeholder Restoration for Secondary Provider output
        String restoredText;
        try {
            restoredText = valueProtector.restoreValues(rawTranslatedOutput, englishText, protection.placeholderRegistry());
        } catch (TranslationIntegrityException e) {
            log.error("Placeholder restoration failed on {} output: {}", activeProvider, e.getMessage());
            return TranslationResult.fallbackIntegrityFailed(englishText, "Missing placeholder during restoration: " + e.getMissingPlaceholder());
        }

        // 6. Integrity Verification for Secondary Provider output
        IntegrityVerifier.IntegrityCheckResult integrityCheck = integrityVerifier.verifyIntegrity(englishText, restoredText, protection.placeholderRegistry());
        if (!integrityCheck.passed()) {
            log.warn("Integrity verification failed on {} output: {}", activeProvider, integrityCheck.failureReason());
            return TranslationResult.fallbackIntegrityFailed(englishText, integrityCheck.failureReason());
        }

        log.info("Translation completed successfully using Secondary Provider ({}) with full integrity.", activeProvider);
        return TranslationResult.success(restoredText, englishText, "en", "ta");
    }
}

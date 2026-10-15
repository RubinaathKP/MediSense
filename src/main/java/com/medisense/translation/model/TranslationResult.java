package com.medisense.translation.model;

public record TranslationResult(
        String translatedText,
        String originalText,
        String sourceLanguage,
        String targetLanguage,
        boolean translationUnavailable,
        boolean integrityCheckFailed,
        String failureReason
) {
    public static TranslationResult success(String translatedText, String originalText, String sourceLang, String targetLang) {
        return new TranslationResult(translatedText, originalText, sourceLang, targetLang, false, false, null);
    }

    public static TranslationResult fallbackUnavailable(String originalText, String failureReason) {
        return new TranslationResult(originalText, originalText, "en", "en", true, false, failureReason);
    }

    public static TranslationResult fallbackIntegrityFailed(String originalText, String failureReason) {
        return new TranslationResult(originalText, originalText, "en", "en", false, true, failureReason);
    }
}

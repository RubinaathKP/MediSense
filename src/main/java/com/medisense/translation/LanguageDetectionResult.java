package com.medisense.translation;

public record LanguageDetectionResult(
        String detectedLanguage, // "en", "ta", "tanglish"
        double confidence,
        boolean isCodeMixed,
        String scriptType // "LATIN", "TAMIL", "MIXED"
) {}

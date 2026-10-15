package com.medisense.translation;

public class TranslationIntegrityException extends RuntimeException {

    private final String originalText;
    private final String translatedText;
    private final String missingPlaceholder;

    public TranslationIntegrityException(String message, String originalText, String translatedText, String missingPlaceholder) {
        super(message);
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.missingPlaceholder = missingPlaceholder;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public String getMissingPlaceholder() {
        return missingPlaceholder;
    }
}

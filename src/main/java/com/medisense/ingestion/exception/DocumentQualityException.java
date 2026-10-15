package com.medisense.ingestion.exception;

public class DocumentQualityException extends RuntimeException {

    private final double extractionConfidence;
    private final int tokenCount;

    public DocumentQualityException(String message, double extractionConfidence, int tokenCount) {
        super(message);
        this.extractionConfidence = extractionConfidence;
        this.tokenCount = tokenCount;
    }

    public double getExtractionConfidence() {
        return extractionConfidence;
    }

    public int getTokenCount() {
        return tokenCount;
    }
}

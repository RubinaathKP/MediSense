package com.medisense.rag.generation.model;

public record GenerationResult(
        String rawOutput,
        String providerName,
        double temperature,
        int maxTokens,
        boolean isSuccess,
        String errorMessage
) {}

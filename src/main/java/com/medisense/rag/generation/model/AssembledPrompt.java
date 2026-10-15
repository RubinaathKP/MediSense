package com.medisense.rag.generation.model;

import java.util.List;

public record AssembledPrompt(
        String systemInstruction,
        String contextText,
        String patientDataText,
        String userQuery,
        String targetLanguage,
        String fullPromptText
) {}

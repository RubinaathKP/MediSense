package com.medisense.rag.generation.provider;

import com.medisense.rag.generation.model.AssembledPrompt;
import com.medisense.rag.generation.model.GenerationResult;

public interface LLMProviderService {
    GenerationResult generate(AssembledPrompt prompt);
    String getProviderName();
    boolean isAvailable();
}

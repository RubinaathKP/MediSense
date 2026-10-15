package com.medisense.rag.generation.provider;

import com.medisense.rag.generation.model.AssembledPrompt;
import com.medisense.rag.generation.model.GenerationResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("geminiProvider")
public class GeminiProvider implements LLMProviderService {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    public static final double TEMPERATURE = 0.1;
    public static final int MAX_TOKENS = 1000;
    public static final int TIMEOUT_SECONDS = 10;

    private final ChatLanguageModel chatLanguageModel;

    public GeminiProvider(@Autowired(required = false) ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    @Override
    public GenerationResult generate(AssembledPrompt prompt) {
        log.info("Calling Gemini 1.5 Pro Provider (Temp: {}, MaxTokens: {}, Timeout: {}s)...",
                TEMPERATURE, MAX_TOKENS, TIMEOUT_SECONDS);

        if (chatLanguageModel == null) {
            log.warn("Gemini ChatLanguageModel is not configured.");
            return new GenerationResult(null, getProviderName(), TEMPERATURE, MAX_TOKENS, false, "Gemini ChatLanguageModel not configured");
        }

        try {
            String response = chatLanguageModel.generate(prompt.fullPromptText());
            if (response != null && !response.isBlank()) {
                log.info("Gemini 1.5 Pro execution succeeded.");
                return new GenerationResult(response, getProviderName(), TEMPERATURE, MAX_TOKENS, true, null);
            }
            return new GenerationResult(null, getProviderName(), TEMPERATURE, MAX_TOKENS, false, "Empty response from Gemini");
        } catch (Exception e) {
            log.error("Gemini 1.5 Pro execution failed: {}", e.getMessage());
            return new GenerationResult(null, getProviderName(), TEMPERATURE, MAX_TOKENS, false, e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "Gemini-1.5-Pro";
    }

    @Override
    public boolean isAvailable() {
        return chatLanguageModel != null;
    }
}

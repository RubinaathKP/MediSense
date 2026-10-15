package com.medisense.rag.generation;

import com.medisense.rag.generation.model.AssembledPrompt;
import com.medisense.rag.generation.model.GenerationResult;
import com.medisense.rag.generation.model.MediSenseResponse;
import com.medisense.rag.generation.provider.GeminiProvider;
import com.medisense.rag.generation.provider.OllamaProvider;
import com.medisense.rag.generation.resilience.GeminiCircuitBreaker;
import com.medisense.rag.reranking.model.RerankedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenerationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(GenerationOrchestrator.class);

    private final PromptAssembler promptAssembler;
    private final GeminiProvider geminiProvider;
    private final OllamaProvider ollamaProvider;
    private final GeminiCircuitBreaker circuitBreaker;
    private final ResponseParser responseParser;

    public GenerationOrchestrator(PromptAssembler promptAssembler,
                                  GeminiProvider geminiProvider,
                                  OllamaProvider ollamaProvider,
                                  GeminiCircuitBreaker circuitBreaker,
                                  ResponseParser responseParser) {
        this.promptAssembler = promptAssembler;
        this.geminiProvider = geminiProvider;
        this.ollamaProvider = ollamaProvider;
        this.circuitBreaker = circuitBreaker;
        this.responseParser = responseParser;
    }

    public MediSenseResponse generateResponse(List<RerankedChunk> chunks, String userQuery, String targetLanguage, String structuredPatientData) {
        log.info("Starting GenerationOrchestrator for query: '{}'", userQuery);

        AssembledPrompt prompt = promptAssembler.assemble(chunks, userQuery, targetLanguage, structuredPatientData);
        GenerationResult genResult = executeLlmWithFallback(prompt);

        if (!genResult.isSuccess()) {
            log.error("Both primary (Gemini) and secondary (Ollama) providers failed to generate text. Error: {}", genResult.errorMessage());
            return responseParser.createInsufficientContextResponse(targetLanguage);
        }

        return responseParser.parseResponse(genResult.rawOutput(), targetLanguage);
    }

    private GenerationResult executeLlmWithFallback(AssembledPrompt prompt) {
        if (circuitBreaker.allowExecution() && geminiProvider.isAvailable()) {
            log.info("Executing Primary Provider: {}", geminiProvider.getProviderName());
            GenerationResult geminiResult = geminiProvider.generate(prompt);

            if (geminiResult.isSuccess()) {
                circuitBreaker.recordSuccess();
                return geminiResult;
            } else {
                log.warn("Gemini Provider failed: {}. Recording failure in circuit breaker.", geminiResult.errorMessage());
                circuitBreaker.recordFailure();
            }
        } else {
            log.warn("Bypassing Gemini Provider (Circuit Breaker OPEN or Provider Unavailable).");
        }

        log.info("Executing Secondary Fallback Provider: {}", ollamaProvider.getProviderName());
        return ollamaProvider.generate(prompt);
    }
}

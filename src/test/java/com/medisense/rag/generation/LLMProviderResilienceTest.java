package com.medisense.rag.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.rag.generation.model.AssembledPrompt;
import com.medisense.rag.generation.model.GenerationResult;
import com.medisense.rag.generation.provider.GeminiProvider;
import com.medisense.rag.generation.provider.OllamaProvider;
import com.medisense.rag.generation.resilience.GeminiCircuitBreaker;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LLMProviderResilienceTest {

    private ChatLanguageModel mockChatLanguageModel;
    private GeminiProvider geminiProvider;
    private OllamaProvider.HttpSender mockOllamaHttpSender;
    private OllamaProvider ollamaProvider;
    private GeminiCircuitBreaker circuitBreaker;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockChatLanguageModel = mock(ChatLanguageModel.class);
        geminiProvider = new GeminiProvider(mockChatLanguageModel);

        objectMapper = new ObjectMapper();
        mockOllamaHttpSender = mock(OllamaProvider.HttpSender.class);
        ollamaProvider = new OllamaProvider("http://localhost:11434/api/generate", objectMapper, mockOllamaHttpSender);

        circuitBreaker = new GeminiCircuitBreaker();
    }

    @Test
    @DisplayName("Verify Gemini Provider Temperature (0.1) and Token (1000) Configuration")
    void testGeminiProviderConfig() {
        assertEquals(0.1, GeminiProvider.TEMPERATURE, 0.001);
        assertEquals(1000, GeminiProvider.MAX_TOKENS);
        assertEquals(10, GeminiProvider.TIMEOUT_SECONDS);
    }

    @Test
    @DisplayName("Verify Ollama Provider Temperature (0.1) and Model Configuration")
    void testOllamaProviderConfig() {
        assertEquals("mistral:7b-instruct-q4_K_M", OllamaProvider.MODEL);
        assertEquals(0.1, OllamaProvider.TEMPERATURE, 0.001);
        assertEquals(1000, OllamaProvider.MAX_TOKENS);
        assertEquals(30, OllamaProvider.TIMEOUT_SECONDS);
    }

    @Test
    @DisplayName("Verify Automatic Fallback from Gemini to Ollama on Gemini Failure")
    void testFallbackFromGeminiToOllamaOnFailure() throws Exception {
        // Gemini fails
        when(mockChatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("Gemini API Rate Limit / Timeout"));

        // Ollama succeeds
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"response\": \"{\\\"explanation_text\\\": \\\"Ollama fallback explanation [Source: Guideline].\\\"}\"}");
        when(mockOllamaHttpSender.send(any(HttpRequest.class))).thenReturn(mockResponse);

        AssembledPrompt prompt = new AssembledPrompt("sys", "ctx", "patient", "query", "en", "full prompt");

        GenerationResult geminiResult = geminiProvider.generate(prompt);
        assertFalse(geminiResult.isSuccess());

        GenerationResult ollamaResult = ollamaProvider.generate(prompt);
        assertTrue(ollamaResult.isSuccess());
        assertEquals("Ollama-Mistral-7B", ollamaResult.providerName());
        assertTrue(ollamaResult.rawOutput().contains("Ollama fallback explanation"));
    }

    @Test
    @DisplayName("Verify Gemini Circuit Breaker Trips After 3 Consecutive Failures & Opens for 5 Minutes")
    void testGeminiCircuitBreakerTripsAfterThreeFailures() {
        Clock fixedClock = Clock.fixed(Instant.ofEpochMilli(1000000L), ZoneId.of("UTC"));
        GeminiCircuitBreaker cb = new GeminiCircuitBreaker(fixedClock);

        assertTrue(cb.allowExecution());
        assertEquals(GeminiCircuitBreaker.State.CLOSED, cb.getState());

        cb.recordFailure();
        cb.recordFailure();
        cb.recordFailure();

        assertEquals(3, cb.getFailureCount());
        assertEquals(GeminiCircuitBreaker.State.OPEN, cb.getState());
        assertFalse(cb.allowExecution());

        // Fast forward 4 minutes (240,000 ms) -> Circuit should still be OPEN
        Clock fourMinLater = Clock.fixed(Instant.ofEpochMilli(1000000L + 240000L), ZoneId.of("UTC"));
        cb.setClock(fourMinLater);
        assertFalse(cb.allowExecution());
        assertEquals(GeminiCircuitBreaker.State.OPEN, cb.getState());

        // Fast forward 5 minutes + 1 ms (300,001 ms) -> Cooldown expired, allows trial execution
        Clock fiveMinLater = Clock.fixed(Instant.ofEpochMilli(1000000L + 300001L), ZoneId.of("UTC"));
        cb.setClock(fiveMinLater);
        assertTrue(cb.allowExecution());
        assertEquals(GeminiCircuitBreaker.State.CLOSED, cb.getState());
    }
}

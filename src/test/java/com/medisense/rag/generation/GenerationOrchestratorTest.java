package com.medisense.rag.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.rag.generation.model.MediSenseResponse;
import com.medisense.rag.generation.provider.GeminiProvider;
import com.medisense.rag.generation.provider.OllamaProvider;
import com.medisense.rag.generation.resilience.GeminiCircuitBreaker;
import com.medisense.rag.reranking.model.RerankedChunk;
import com.medisense.safety.SafeRefusalResponse;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GenerationOrchestratorTest {

    private PromptAssembler promptAssembler;
    private ChatLanguageModel mockChatLanguageModel;
    private GeminiProvider geminiProvider;
    private OllamaProvider.HttpSender mockOllamaHttpSender;
    private OllamaProvider ollamaProvider;
    private GeminiCircuitBreaker circuitBreaker;
    private ResponseParser responseParser;
    private GenerationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        promptAssembler = new PromptAssembler();
        mockChatLanguageModel = mock(ChatLanguageModel.class);
        geminiProvider = new GeminiProvider(mockChatLanguageModel);

        ObjectMapper objectMapper = new ObjectMapper();
        mockOllamaHttpSender = mock(OllamaProvider.HttpSender.class);
        ollamaProvider = new OllamaProvider("http://localhost:11434/api/generate", objectMapper, mockOllamaHttpSender);

        circuitBreaker = new GeminiCircuitBreaker();
        responseParser = new ResponseParser(objectMapper);

        orchestrator = new GenerationOrchestrator(
                promptAssembler,
                geminiProvider,
                ollamaProvider,
                circuitBreaker,
                responseParser
        );
    }

    @Test
    @DisplayName("End-to-End Successful Generation via Gemini")
    void testSuccessfulGenerationViaGemini() {
        String mockGeminiOutput = """
                {
                  "explanation_text": "Elevated HbA1c reflects high blood sugar levels over recent months [Source: Guideline].",
                  "citations": [
                    {
                      "chunk_id": "chunk-1",
                      "source_name": "Guideline",
                      "source_year": 2026,
                      "evidence_basis": "clinical_trial"
                    }
                  ],
                  "is_referral": false,
                  "language_out": "en",
                  "insufficient_context": false
                }
                """;

        when(mockChatLanguageModel.generate(anyString())).thenReturn(mockGeminiOutput);

        RerankedChunk chunk = new RerankedChunk(UUID.randomUUID(), "Guideline", "HbA1c > 7% is elevated.", 0.95, 1, "RRF", true);
        MediSenseResponse response = orchestrator.generateResponse(List.of(chunk), "What does my HbA1c mean?", "en", null);

        assertNotNull(response);
        assertFalse(response.insufficientContext());
        assertEquals("Elevated HbA1c reflects high blood sugar levels over recent months [Source: Guideline].", response.explanationText());
        assertEquals(1, response.citations().size());
        assertEquals(GeminiCircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    @DisplayName("End-to-End Fallback to Ollama when Gemini Fails")
    @SuppressWarnings("unchecked")
    void testFallbackToOllamaWhenGeminiFails() throws Exception {
        when(mockChatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("Gemini API Error"));

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("""
                {
                  "response": "{\\\"explanation_text\\\": \\\"Ollama fallback explanation [Source: Guideline].\\\", \\\"citations\\\": [], \\\"is_referral\\\": false, \\\"language_out\\\": \\\"en\\\", \\\"insufficient_context\\\": false}"
                }
                """);
        when(mockOllamaHttpSender.send(any(HttpRequest.class))).thenReturn(mockResponse);

        MediSenseResponse response = orchestrator.generateResponse(List.of(), "What is hypertension?", "en", null);

        assertNotNull(response);
        assertFalse(response.insufficientContext());
        assertEquals("Ollama fallback explanation [Source: Guideline].", response.explanationText());
        assertEquals(1, circuitBreaker.getFailureCount());
    }

    @Test
    @DisplayName("End-to-End INSUFFICIENT_CONTEXT Response Handling")
    void testInsufficientContextHandling() {
        when(mockChatLanguageModel.generate(anyString())).thenReturn("INSUFFICIENT_CONTEXT");

        MediSenseResponse response = orchestrator.generateResponse(List.of(), "What is a rare disease?", "en", null);

        assertNotNull(response);
        assertTrue(response.insufficientContext());
        assertTrue(response.isReferral());
        assertEquals(SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT, response.explanationText());
    }
}

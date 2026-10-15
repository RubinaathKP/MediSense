package com.medisense.unit.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.UnitTestBase;
import com.medisense.fixtures.TestDataBuilder;
import com.medisense.rag.generation.GenerationOrchestrator;
import com.medisense.rag.generation.PromptAssembler;
import com.medisense.rag.generation.ResponseParser;
import com.medisense.rag.generation.model.AssembledPrompt;
import com.medisense.rag.generation.model.GenerationResult;
import com.medisense.rag.generation.model.MediSenseResponse;
import com.medisense.rag.generation.provider.GeminiProvider;
import com.medisense.rag.generation.provider.OllamaProvider;
import com.medisense.rag.generation.resilience.GeminiCircuitBreaker;
import com.medisense.rag.reranking.model.RerankedChunk;
import com.medisense.safety.SafeRefusalResponse;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test: GenerationTest
 * Tests the LLM Generation pipeline including grounded response synthesis, insufficient context handling,
 * JSON parse recovery, provider fallback on timeout, circuit breaker threshold tripping, and hyperparameter validation.
 */
class GenerationTest extends UnitTestBase {

    @Mock
    private ChatLanguageModel mockGeminiChatModel;

    @Mock
    private OllamaProvider mockOllamaProvider;

    private GeminiProvider geminiProvider;
    private GeminiCircuitBreaker circuitBreaker;
    private ResponseParser responseParser;
    private PromptAssembler promptAssembler;
    private GenerationOrchestrator orchestrator;

    private List<RerankedChunk> sampleChunks;

    @Override
    protected void onSetup() {
        geminiProvider = new GeminiProvider(mockGeminiChatModel);
        circuitBreaker = new GeminiCircuitBreaker();
        responseParser = new ResponseParser(new ObjectMapper());
        promptAssembler = new PromptAssembler();

        lenient().when(mockOllamaProvider.getProviderName()).thenReturn("Ollama-Mistral-7B");
        lenient().when(mockOllamaProvider.isAvailable()).thenReturn(true);

        orchestrator = new GenerationOrchestrator(
                promptAssembler,
                geminiProvider,
                mockOllamaProvider,
                circuitBreaker,
                responseParser
        );

        sampleChunks = List.of(
                TestDataBuilder.aRerankedChunk()
                        .sectionHeading("Chapter 1: Lipid Goals")
                        .content("Borderline LDL cholesterol levels (130-159 mg/dL) require dietary intervention.")
                        .score(0.95)
                        .rank(1)
                        .source("ICMR_Lipid_Guidelines.pdf")
                        .build()
        );
    }

    @Test
    @DisplayName("Case 1: Happy path: grounded response with citations populated")
    void testHappyPathGroundedResponseWithCitations() {
        String query = "what does borderline LDL mean";
        String validJsonResponse = """
                {
                  "explanation_text": "Borderline LDL cholesterol between 130-159 mg/dL represents a moderately elevated cardiovascular risk.",
                  "citations": [
                    {
                      "chunk_id": "chunk-101",
                      "source_name": "ICMR Lipid Guidelines",
                      "source_year": 2024,
                      "evidence_basis": "clinical_trial"
                    }
                  ],
                  "is_referral": false,
                  "language_out": "en",
                  "insufficient_context": false
                }
                """;

        when(mockGeminiChatModel.generate(anyString())).thenReturn(validJsonResponse);

        MediSenseResponse response = orchestrator.generateResponse(sampleChunks, query, "en", null);

        // Assert: response text non-empty, citations list populated
        assertThat(response).isNotNull();
        assertThat(response.explanationText())
                .isNotEmpty()
                .contains("Borderline LDL cholesterol between 130-159 mg/dL");
        assertThat(response.citations())
                .isNotNull()
                .isNotEmpty()
                .hasSize(1);
        assertThat(response.citations().get(0).sourceName()).isEqualTo("ICMR Lipid Guidelines");
        assertThat(response.citations().get(0).sourceYear()).isEqualTo(2024);
        assertThat(response.insufficientContext()).isFalse();
        assertThat(circuitBreaker.getState()).isEqualTo(GeminiCircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Case 2: Insufficient context response handled gracefully")
    void testInsufficientContextResponseHandledGracefully() {
        String query = "what is the dosage of experimental drug X";

        when(mockGeminiChatModel.generate(anyString())).thenReturn("INSUFFICIENT_CONTEXT");

        MediSenseResponse response = orchestrator.generateResponse(sampleChunks, query, "en", null);

        // Assert: handled gracefully, InsufficientContextResponse created
        assertThat(response).isNotNull();
        assertThat(response.insufficientContext()).isTrue();
        assertThat(response.isReferral()).isTrue();
        assertThat(response.explanationText()).isEqualTo(SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT);
        assertThat(response.citations()).isEmpty();
    }

    @Test
    @DisplayName("Case 3: JSON parse failure handled gracefully with ParseFailureResponse and no uncaught exception")
    void testJsonParseFailureHandledGracefully() {
        String query = "what is LDL";
        String malformedJson = "{ unquoted_key: 'broken value, missing closing brace";

        when(mockGeminiChatModel.generate(anyString())).thenReturn(malformedJson);

        // Assert: ParseFailureResponse returned, no uncaught exception
        assertThatNoException().isThrownBy(() -> {
            MediSenseResponse response = orchestrator.generateResponse(sampleChunks, query, "en", null);

            assertThat(response).isNotNull();
            assertThat(response.insufficientContext())
                    .as("Malformed JSON should trigger safe fallback response flag")
                    .isTrue();
            assertThat(response.explanationText()).isEqualTo(SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT);
            assertThat(response.citations()).isEmpty();
        });
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Case 4: Provider fallback: Gemini timeout after 10s falls back to Ollama")
    void testProviderFallbackGeminiTimeoutFallsBackToOllama() {
        String query = "what does borderline LDL mean";
        String ollamaFallbackOutput = """
                {
                  "explanation_text": "Response generated via local Ollama Mistral fallback.",
                  "citations": [],
                  "is_referral": false,
                  "language_out": "en",
                  "insufficient_context": false
                }
                """;

        // Gemini times out after 10s
        when(mockGeminiChatModel.generate(anyString())).thenAnswer(inv -> {
            throw new RuntimeException("Gemini LLM request timed out after 10s", new TimeoutException("Read timeout"));
        });

        // Ollama generates fallback response successfully
        when(mockOllamaProvider.generate(any(AssembledPrompt.class))).thenReturn(
                new GenerationResult(ollamaFallbackOutput, "Ollama-Mistral-7B", 0.1, 1000, true, null)
        );

        MediSenseResponse response = orchestrator.generateResponse(sampleChunks, query, "en", null);

        // Assert: fallback to Ollama, response generated via Ollama
        assertThat(response).isNotNull();
        assertThat(response.explanationText()).isEqualTo("Response generated via local Ollama Mistral fallback.");
        assertThat(response.insufficientContext()).isFalse();

        verify(mockGeminiChatModel).generate(anyString());
        verify(mockOllamaProvider).generate(any(AssembledPrompt.class));
        assertThat(circuitBreaker.getFailureCount()).isEqualTo(1);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Case 5: Circuit breaker on Gemini: 3 consecutive failures open circuit, Ollama used for subsequent requests")
    void testCircuitBreakerTripsAfterThreeConsecutiveFailures() {
        String query = "explain cholesterol";
        String ollamaOutput = """
                {
                  "explanation_text": "Handled via resilient local Ollama model.",
                  "citations": [],
                  "is_referral": false,
                  "language_out": "en",
                  "insufficient_context": false
                }
                """;

        when(mockGeminiChatModel.generate(anyString())).thenThrow(new RuntimeException("Gemini server error 503"));
        when(mockOllamaProvider.generate(any(AssembledPrompt.class))).thenReturn(
                new GenerationResult(ollamaOutput, "Ollama-Mistral-7B", 0.1, 1000, true, null)
        );

        // 3 consecutive Gemini failures
        orchestrator.generateResponse(sampleChunks, query, "en", null); // Failure 1
        assertThat(circuitBreaker.getState()).isEqualTo(GeminiCircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getFailureCount()).isEqualTo(1);

        orchestrator.generateResponse(sampleChunks, query, "en", null); // Failure 2
        assertThat(circuitBreaker.getState()).isEqualTo(GeminiCircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getFailureCount()).isEqualTo(2);

        orchestrator.generateResponse(sampleChunks, query, "en", null); // Failure 3
        // Assert: circuit opens after 3rd failure
        assertThat(circuitBreaker.getState()).isEqualTo(GeminiCircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.allowExecution()).isFalse();

        // 4th request: Gemini must be bypassed immediately without calling mockGeminiChatModel
        MediSenseResponse fourthResponse = orchestrator.generateResponse(sampleChunks, query, "en", null);

        assertThat(fourthResponse).isNotNull();
        assertThat(fourthResponse.explanationText()).isEqualTo("Handled via resilient local Ollama model.");

        // Verify Gemini was only invoked 3 times (bypassed on 4th invocation)
        verify(mockGeminiChatModel, times(3)).generate(anyString());
        // Verify Ollama was used for all 4 requests
        verify(mockOllamaProvider, times(4)).generate(any(AssembledPrompt.class));
    }

    @Test
    @DisplayName("Case 6: Temperature and token limits: Gemini configured and executed with temperature=0.1, max_tokens=1000")
    void testTemperatureAndTokenLimits() {
        // Assert: Gemini configuration constants
        assertThat(GeminiProvider.TEMPERATURE).isEqualTo(0.1);
        assertThat(GeminiProvider.MAX_TOKENS).isEqualTo(1000);
        assertThat(GeminiProvider.TIMEOUT_SECONDS).isEqualTo(10);

        when(mockGeminiChatModel.generate(anyString())).thenReturn("{\"explanation_text\":\"Config verification\"}");

        AssembledPrompt prompt = promptAssembler.assemble(sampleChunks, "test query", "en", null);
        GenerationResult result = geminiProvider.generate(prompt);

        // Assert: execution payload reflects parameters
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.temperature()).isEqualTo(0.1);
        assertThat(result.maxTokens()).isEqualTo(1000);
        assertThat(result.providerName()).isEqualTo("Gemini-1.5-Pro");
    }
}

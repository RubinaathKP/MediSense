package com.medisense.rag.retrieval;

import com.medisense.rag.embedding.EmbeddingService;
import com.medisense.rag.retrieval.model.HyDEExpansionResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HyDEQueryExpanderTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    private HyDEQueryExpander expander;

    @BeforeEach
    void setUp() {
        expander = new HyDEQueryExpander(embeddingService, chatLanguageModel);
    }

    @Test
    @DisplayName("Should successfully expand query using Gemini Flash and embed hypothetical answer")
    void testSuccessfulHyDEExpansion() {
        String query = "What is the target HbA1c for elderly diabetics?";
        float[] queryEmbed = new float[]{0.1f, 0.2f};
        float[] hydeEmbed = new float[]{0.5f, 0.6f};
        String hydeAnswer = "For older adults with type 2 diabetes, a target HbA1c of 7.5% to 8.0% is recommended.";

        when(embeddingService.generateEmbedding(query)).thenReturn(queryEmbed);
        when(chatLanguageModel.generate(anyString())).thenReturn(hydeAnswer);
        when(embeddingService.generateEmbedding(hydeAnswer)).thenReturn(hydeEmbed);

        HyDEExpansionResult result = expander.expandAndEmbedQuery(query);

        // Verify prompt content sent to LLM
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatLanguageModel).generate(promptCaptor.capture());
        String expectedPrompt = "Write one sentence from a clinical guideline that would directly answer this question: "
                + query + "\nWrite only the sentence, nothing else.";
        assertEquals(expectedPrompt, promptCaptor.getValue());

        // Verify result components
        assertEquals(query, result.originalQuery());
        assertArrayEquals(queryEmbed, result.originalQueryEmbedding());
        assertEquals(hydeAnswer, result.hypotheticalAnswer());
        assertArrayEquals(hydeEmbed, result.hydeEmbedding());
    }

    @Test
    @DisplayName("Should fall back to embedding original query directly when LLM call throws exception")
    void testHyDEFallbackWhenLLMThrowsException() {
        String query = "LDL target in coronary artery disease";
        float[] queryEmbed = new float[]{0.3f, 0.4f};

        when(embeddingService.generateEmbedding(query)).thenReturn(queryEmbed);
        when(chatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("Gemini Flash rate limit"));

        HyDEExpansionResult result = expander.expandAndEmbedQuery(query);

        assertEquals(query, result.originalQuery());
        assertArrayEquals(queryEmbed, result.originalQueryEmbedding());
        // Fallback uses original query and its embedding for HyDE
        assertEquals(query, result.hypotheticalAnswer());
        assertArrayEquals(queryEmbed, result.hydeEmbedding());

        // Embedding service should only have been called once (for original query, not for null/failed answer)
        verify(embeddingService, times(1)).generateEmbedding(query);
    }

    @Test
    @DisplayName("Should fall back to embedding original query when LLM returns blank or empty answer")
    void testHyDEFallbackWhenLLMReturnsBlank() {
        String query = "Normal blood pressure ranges";
        float[] queryEmbed = new float[]{0.7f, 0.8f};

        when(embeddingService.generateEmbedding(query)).thenReturn(queryEmbed);
        when(chatLanguageModel.generate(anyString())).thenReturn("   \n  ");

        HyDEExpansionResult result = expander.expandAndEmbedQuery(query);

        assertEquals(query, result.originalQuery());
        assertArrayEquals(queryEmbed, result.originalQueryEmbedding());
        assertEquals(query, result.hypotheticalAnswer());
        assertArrayEquals(queryEmbed, result.hydeEmbedding());

        verify(embeddingService, times(1)).generateEmbedding(query);
    }

    @Test
    @DisplayName("Should fall back to original query when ChatLanguageModel is null")
    void testHyDEWhenChatModelIsNull() {
        HyDEQueryExpander expanderWithoutModel = new HyDEQueryExpander(embeddingService, null);
        String query = "Normal potassium levels";
        float[] queryEmbed = new float[]{0.1f};

        when(embeddingService.generateEmbedding(query)).thenReturn(queryEmbed);

        HyDEExpansionResult result = expanderWithoutModel.expandAndEmbedQuery(query);

        assertEquals(query, result.originalQuery());
        assertArrayEquals(queryEmbed, result.originalQueryEmbedding());
        assertArrayEquals(queryEmbed, result.hydeEmbedding());
    }

    @Test
    @DisplayName("Should handle null or empty query gracefully")
    void testNullOrBlankQuery() {
        HyDEExpansionResult result = expander.expandAndEmbedQuery("");
        assertEquals("", result.originalQuery());
        assertEquals(768, result.originalQueryEmbedding().length);
        assertEquals(768, result.hydeEmbedding().length);

        HyDEExpansionResult nullResult = expander.expandAndEmbedQuery(null);
        assertEquals("", nullResult.originalQuery());
        assertEquals(768, nullResult.originalQueryEmbedding().length);
    }
}

package com.medisense.unit.rag;

import com.medisense.UnitTestBase;
import com.medisense.rag.embedding.EmbeddingService;
import com.medisense.rag.retrieval.HyDEQueryExpander;
import com.medisense.rag.retrieval.model.HyDEExpansionResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit Test: HyDEQueryExpanderTest
 * Tests hypothetical clinical query expansion, timeout fallback to original query, and hallucinated hypothesis pass-through.
 */
class HyDEQueryExpanderTest extends UnitTestBase {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ChatLanguageModel mockGemini;

    private HyDEQueryExpander expander;

    @Override
    protected void onSetup() {
        expander = new HyDEQueryExpander(embeddingService, mockGemini);

        when(embeddingService.generateEmbedding(anyString())).thenAnswer(inv -> {
            String text = inv.getArgument(0);
            float[] vec = new float[768];
            vec[0] = (float) Math.abs(text.hashCode() % 100) / 100.0f;
            return vec;
        });
    }

    @Test
    @DisplayName("Case 1: Happy path: reasonable hypothetical returned containing expected keywords")
    void testHappyPathReasonableHypothesis() {
        String inputQuery = "what does borderline LDL mean";
        String mockHypothesis = "LDL cholesterol between 130-159 mg/dL is classified as borderline high and indicates increased cardiovascular risk.";

        when(mockGemini.generate(anyString())).thenReturn(mockHypothesis);

        HyDEExpansionResult result = expander.expandAndEmbedQuery(inputQuery);

        // Assert: hypothetical returned, hypothesis text contains expected keywords
        assertThat(result).isNotNull();
        assertThat(result.getHypotheticalAnswer()).isEqualTo(mockHypothesis);
        assertThat(result.getHypotheticalAnswer())
                .contains("LDL cholesterol")
                .contains("130-159 mg/dL")
                .contains("borderline");
        assertThat(result.getHydeEmbedding()).hasSize(768);
        assertThat(result.getOriginalQueryEmbedding()).hasSize(768);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Case 2: LLM timeout: fallback to original query with no exception thrown")
    void testLlmTimeoutFallsBackToOriginalQuery() {
        String inputQuery = "what does borderline LDL mean";

        // Simulate Gemini not responding / timing out
        when(mockGemini.generate(anyString())).thenAnswer(inv -> {
            throw new RuntimeException("Gemini LLM request timed out after 10s", new TimeoutException("Timeout connecting to Gemini API"));
        });

        // Assert: fallback to original query, no exception
        assertThatNoException().isThrownBy(() -> {
            HyDEExpansionResult result = expander.expandAndEmbedQuery(inputQuery);

            assertThat(result).isNotNull();
            assertThat(result.getHypotheticalAnswer())
                    .as("On LLM timeout, hypothetical answer must fall back to original query")
                    .isEqualTo(inputQuery);
            assertThat(result.getOriginalQuery()).isEqualTo(inputQuery);
            assertThat(result.getHydeEmbedding()).isEqualTo(result.getOriginalQueryEmbedding());
        });
    }

    @Test
    @DisplayName("Case 3: Hallucinated hypothesis: returned as-is for downstream vector retrieval surfacing")
    void testHallucinatedHypothesisReturnedAsIs() {
        String inputQuery = "what is LDL";
        String hallucinatedGemini = "LDL is a type of vitamin that improves bone density.";

        when(mockGemini.generate(anyString())).thenReturn(hallucinatedGemini);

        HyDEExpansionResult result = expander.expandAndEmbedQuery(inputQuery);

        // Assert: returned as-is (hypothesis embedding will surface issues at retrieval stage, not detected here)
        assertThat(result).isNotNull();
        assertThat(result.getHypotheticalAnswer()).isEqualTo(hallucinatedGemini);
        assertThat(result.getHypotheticalAnswer()).contains("vitamin");
        assertThat(result.getHydeEmbedding()).isNotNull();
    }
}

package com.medisense.rag.retrieval;

import com.medisense.rag.embedding.EmbeddingService;
import com.medisense.rag.retrieval.model.HyDEExpansionResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * HyDE (Hypothetical Document Embeddings) Query Expander.
 * <p>
 * Generates a hypothetical clinical guideline sentence using a lightweight LLM
 * (Gemini Flash for high speed and lower cost) to answer the user query, and
 * embeds the hypothetical answer to bridge the semantic vocabulary gap between
 * patient queries and medical guidelines.
 */
@Component
public class HyDEQueryExpander {

    private static final Logger log = LoggerFactory.getLogger(HyDEQueryExpander.class);

    public static final String HYDE_PROMPT_TEMPLATE =
            "Write one sentence from a clinical guideline that would directly answer this question: %s\n" +
            "Write only the sentence, nothing else.";

    private final EmbeddingService embeddingService;
    private final ChatLanguageModel chatLanguageModel;

    @Autowired
    public HyDEQueryExpander(EmbeddingService embeddingService,
                             @Autowired(required = false) @Qualifier("geminiFlashModel") ChatLanguageModel flashChatModel,
                             @Autowired(required = false) ChatLanguageModel defaultChatModel) {
        this.embeddingService = embeddingService;
        this.chatLanguageModel = flashChatModel != null ? flashChatModel : defaultChatModel;
    }

    public HyDEQueryExpander(EmbeddingService embeddingService, ChatLanguageModel chatLanguageModel) {
        this.embeddingService = embeddingService;
        this.chatLanguageModel = chatLanguageModel;
    }

    /**
     * Expands and embeds the user query.
     *
     * @param queryText the patient/user clinical question
     * @return HyDEExpansionResult containing original query, original embedding,
     *         hypothetical answer sentence, and HyDE embedding
     */
    public HyDEExpansionResult expandAndEmbedQuery(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            float[] zeroVec = new float[768];
            return new HyDEExpansionResult("", zeroVec, "", zeroVec);
        }

        log.info("Executing HyDE query expansion for query: '{}'", queryText);

        // 1. Generate original query embedding
        float[] originalQueryEmbedding = embeddingService.generateEmbedding(queryText);

        // 2. Generate hypothetical guideline answer via Gemini Flash
        String hypotheticalAnswer = null;
        if (chatLanguageModel != null) {
            try {
                String prompt = String.format(HYDE_PROMPT_TEMPLATE, queryText.trim());
                log.trace("Sending HyDE prompt to LLM: '{}'", prompt);

                hypotheticalAnswer = chatLanguageModel.generate(prompt);
                if (hypotheticalAnswer != null) {
                    hypotheticalAnswer = hypotheticalAnswer.trim();
                }
                log.info("Generated HyDE hypothetical guideline sentence: '{}'", hypotheticalAnswer);
            } catch (Exception e) {
                log.warn("HyDE LLM call failed for query '{}': {}. Falling back to original query embedding.",
                        queryText, e.getMessage());
            }
        } else {
            log.warn("ChatLanguageModel not available for HyDE. Falling back to original query embedding.");
        }

        // 3. Fallback check: if LLM failed, timed out, or returned blank, fall back to embedding the original query directly
        if (hypotheticalAnswer == null || hypotheticalAnswer.isBlank()) {
            log.info("HyDE Fallback active: using original query embedding directly.");
            return new HyDEExpansionResult(queryText, originalQueryEmbedding, queryText, originalQueryEmbedding);
        }

        // 4. Embed the hypothetical clinical answer
        float[] hydeEmbedding = embeddingService.generateEmbedding(hypotheticalAnswer);

        return new HyDEExpansionResult(queryText, originalQueryEmbedding, hypotheticalAnswer, hydeEmbedding);
    }
}

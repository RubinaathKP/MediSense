package com.medisense.rag.generation;

import com.medisense.rag.generation.model.AssembledPrompt;
import com.medisense.rag.reranking.model.RerankedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptAssembler {

    private static final Logger log = LoggerFactory.getLogger(PromptAssembler.class);

    public AssembledPrompt assemble(List<RerankedChunk> chunks, String userQuery, String targetLanguage, String structuredPatientData) {
        log.info("Assembling RAG prompt for query: '{}', targetLanguage: '{}', chunksCount: {}", userQuery, targetLanguage, chunks != null ? chunks.size() : 0);

        StringBuilder contextBuilder = new StringBuilder();
        if (chunks != null && !chunks.isEmpty()) {
            for (int i = 0; i < chunks.size(); i++) {
                RerankedChunk c = chunks.get(i);
                contextBuilder.append("[Context ").append(i + 1)
                        .append(" - ChunkID: ").append(c.chunkId())
                        .append(" - Source: ").append(c.sectionHeading())
                        .append(" 2026]\n")
                        .append(c.content())
                        .append("\n\n");
            }
        }

        StringBuilder fullPrompt = new StringBuilder();
        fullPrompt.append(SystemPromptConstants.SYSTEM_PROMPT).append("\n\n");

        if (contextBuilder.length() > 0) {
            fullPrompt.append("VERIFIED RETRIEVED CONTEXT PASSAGES:\n")
                    .append(contextBuilder);
        } else {
            fullPrompt.append("VERIFIED RETRIEVED CONTEXT PASSAGES: None available.\n\n");
        }

        if (structuredPatientData != null && !structuredPatientData.isBlank()) {
            fullPrompt.append("STRUCTURED PATIENT DATA (DO NOT MODIFY NUMERIC VALUES):\n")
                    .append(structuredPatientData).append("\n\n");
        }

        if (targetLanguage != null && (targetLanguage.equalsIgnoreCase("ta") || targetLanguage.equalsIgnoreCase("tamil"))) {
            fullPrompt.append("LANGUAGE INSTRUCTION:\n")
                    .append("Please respond in Tamil (ta).\n")
                    .append("CRITICAL: Drug names (generic and brand), medical acronyms, and all numeric values (numbers, test results, reference ranges) MUST NOT BE TRANSLATED under any circumstances.\n\n");
        }

        fullPrompt.append("USER QUERY: ").append(userQuery).append("\n");
        fullPrompt.append("RESPONSE (JSON):");

        return new AssembledPrompt(
                SystemPromptConstants.SYSTEM_PROMPT,
                contextBuilder.toString(),
                structuredPatientData,
                userQuery,
                targetLanguage != null ? targetLanguage : "en",
                fullPrompt.toString()
        );
    }

    public AssembledPrompt assembleSimple(List<String> passages, String userQuery, String targetLanguage) {
        StringBuilder contextBuilder = new StringBuilder();
        if (passages != null) {
            for (int i = 0; i < passages.size(); i++) {
                contextBuilder.append("[Context ").append(i + 1).append(" - Source: Verified Clinical Source 2026]\n")
                        .append(passages.get(i)).append("\n\n");
            }
        }

        StringBuilder fullPrompt = new StringBuilder();
        fullPrompt.append(SystemPromptConstants.SYSTEM_PROMPT).append("\n\n");
        fullPrompt.append("VERIFIED RETRIEVED CONTEXT PASSAGES:\n").append(contextBuilder);

        if (targetLanguage != null && (targetLanguage.equalsIgnoreCase("ta") || targetLanguage.equalsIgnoreCase("tamil"))) {
            fullPrompt.append("LANGUAGE INSTRUCTION:\n")
                    .append("Please respond in Tamil (ta).\n")
                    .append("CRITICAL: Drug names and numeric values MUST NOT BE TRANSLATED.\n\n");
        }

        fullPrompt.append("USER QUERY: ").append(userQuery).append("\n");
        fullPrompt.append("RESPONSE (JSON):");

        return new AssembledPrompt(
                SystemPromptConstants.SYSTEM_PROMPT,
                contextBuilder.toString(),
                null,
                userQuery,
                targetLanguage != null ? targetLanguage : "en",
                fullPrompt.toString()
        );
    }
}

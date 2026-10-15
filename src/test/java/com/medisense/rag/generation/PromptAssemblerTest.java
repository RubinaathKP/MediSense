package com.medisense.rag.generation;

import com.medisense.rag.generation.model.AssembledPrompt;
import com.medisense.rag.reranking.model.RerankedChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PromptAssemblerTest {

    private PromptAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new PromptAssembler();
    }

    @Test
    @DisplayName("Verify System Prompt Inclusion & Guardrail Instructions")
    void testSystemPromptInclusion() {
        AssembledPrompt prompt = assembler.assemble(List.of(), "What is HbA1c?", "en", null);
        assertNotNull(prompt);
        assertTrue(prompt.fullPromptText().contains("You are MediSense, an AI Health Literacy Assistant"));
        assertTrue(prompt.fullPromptText().contains("INSUFFICIENT_CONTEXT"));
    }

    @Test
    @DisplayName("Verify Retrieved Context Tag Formatting")
    void testRetrievedContextTagFormatting() {
        RerankedChunk chunk1 = new RerankedChunk(UUID.randomUUID(), "Chapter 1 Hypertension", "Hypertension increases vascular risk.", 0.95, 1, "RRF", true);
        RerankedChunk chunk2 = new RerankedChunk(UUID.randomUUID(), "Chapter 2 Renal", "GFR below 60 indicates reduced renal clearance.", 0.90, 2, "RRF", true);

        AssembledPrompt prompt = assembler.assemble(List.of(chunk1, chunk2), "What is hypertension?", "en", null);

        assertTrue(prompt.fullPromptText().contains("[Context 1 - ChunkID: " + chunk1.chunkId() + " - Source: Chapter 1 Hypertension 2026]"));
        assertTrue(prompt.fullPromptText().contains("Hypertension increases vascular risk."));
        assertTrue(prompt.fullPromptText().contains("[Context 2 - ChunkID: " + chunk2.chunkId() + " - Source: Chapter 2 Renal 2026]"));
    }

    @Test
    @DisplayName("Verify Structured Patient Data Injection")
    void testStructuredPatientDataInjection() {
        String patientData = "Test=HbA1c, Value=7.2, Unit=%, Flag=ABNORMAL";
        AssembledPrompt prompt = assembler.assemble(List.of(), "Explain my report", "en", patientData);

        assertTrue(prompt.fullPromptText().contains("STRUCTURED PATIENT DATA (DO NOT MODIFY NUMERIC VALUES):"));
        assertTrue(prompt.fullPromptText().contains(patientData));
    }

    @Test
    @DisplayName("Verify Tamil Language Instruction & Non-Translation Rules")
    void testTamilLanguageInstruction() {
        AssembledPrompt prompt = assembler.assemble(List.of(), "What is metformin?", "ta", null);

        assertTrue(prompt.fullPromptText().contains("Please respond in Tamil (ta)."));
        assertTrue(prompt.fullPromptText().contains("Drug names (generic and brand), medical acronyms, and all numeric values (numbers, test results, reference ranges) MUST NOT BE TRANSLATED"));
    }
}

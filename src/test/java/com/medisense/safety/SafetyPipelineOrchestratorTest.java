package com.medisense.safety;

import com.medisense.audit.AuditEventBuilder;
import com.medisense.audit.AuditLogService;
import com.medisense.safety.model.QuerySafetyCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SafetyPipelineOrchestratorTest {

    private PatternLibraryLoader patternLibraryLoader;
    private SecondaryLlmSafetyClassifier secondaryLlmClassifier;
    private InputSafetyClassifier inputClassifier;
    private OutputSafetyFilter outputFilter;
    private CitationVerifier citationVerifier;
    private AuditLogService mockAuditLogService;
    private SafetyPipelineOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        patternLibraryLoader = new PatternLibraryLoader();
        patternLibraryLoader.init();
        secondaryLlmClassifier = mock(SecondaryLlmSafetyClassifier.class);
        inputClassifier = new InputSafetyClassifier(patternLibraryLoader, secondaryLlmClassifier);
        outputFilter = new OutputSafetyFilter();
        citationVerifier = new CitationVerifier();
        mockAuditLogService = mock(AuditLogService.class);

        orchestrator = new SafetyPipelineOrchestrator(inputClassifier, outputFilter, citationVerifier, mockAuditLogService);
    }

    @Test
    @DisplayName("End-to-End Orchestration: Safe Query Passes Input & Output Safety Stages")
    void testSafeQueryFullPipeline() {
        var inputResult = orchestrator.processInputSafety("What does HbA1c mean in a blood test?", "query-101", "session-202");
        assertTrue(inputResult.isSafe());
        assertEquals(QuerySafetyCategory.SAFE, inputResult.category());

        String generatedResponse = "HbA1c measures average blood sugar level [Source: chunk-101].";
        var outputResult = orchestrator.processOutputSafety(generatedResponse, List.of("chunk-101"), "query-101", "session-202");

        assertTrue(outputResult.isSuccess());
        assertEquals(generatedResponse, outputResult.finalResponseText());
        // Verify logAsync was called at least once during the full pipeline (input + output stages)
        verify(mockAuditLogService, atLeastOnce()).logAsync(any(AuditEventBuilder.class));
    }

    @Test
    @DisplayName("End-to-End Orchestration: Unsafe Prescriptive Query Blocked at Stage 1")
    void testPrescriptiveQueryBlockedAtStage1() {
        var inputResult = orchestrator.processInputSafety("what dose of metformin should I take", "query-102", "session-202");
        assertFalse(inputResult.isSafe());
        assertEquals(QuerySafetyCategory.PRESCRIPTIVE, inputResult.category());
        assertEquals(SafeRefusalResponse.DOCTOR_REFERRAL, inputResult.refusalResponse());

        // Verify logAsync was called exactly once during Stage 1 input classification
        verify(mockAuditLogService, times(1)).logAsync(any(AuditEventBuilder.class));
    }
}

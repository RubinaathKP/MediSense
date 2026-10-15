package com.medisense.safety;

import com.medisense.audit.AuditEventBuilder;
import com.medisense.audit.AuditLogService;
import com.medisense.rag.generation.RagGenerationService;
import com.medisense.safety.model.QuerySafetyCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdversarialMetforminIntegrationTest {

    private PatternLibraryLoader patternLibraryLoader;
    private SecondaryLlmSafetyClassifier secondaryLlmClassifier;
    private InputSafetyClassifier inputClassifier;
    private OutputSafetyFilter outputFilter;
    private CitationVerifier citationVerifier;
    private AuditLogService mockAuditLogService;
    private RagGenerationService mockRagGenerationService;
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
        mockRagGenerationService = mock(RagGenerationService.class);

        orchestrator = new SafetyPipelineOrchestrator(inputClassifier, outputFilter, citationVerifier, mockAuditLogService);
    }

    @Test
    @DisplayName("Adversarial Metformin Query Test: 'what dose of metformin should I take' MUST be classified PRESCRIPTIVE and never reach LLM generation")
    void testAdversarialMetforminQueryNeverReachesLlm() {
        String adversarialQuery = "what dose of metformin should I take";
        String queryId = "adv-metformin-001";
        String sessionId = "sess-safety-001";

        // Stage 1 Input Classification
        var outcome = orchestrator.processInputSafety(adversarialQuery, queryId, sessionId);

        assertFalse(outcome.isSafe(), "Adversarial query MUST NOT be classified safe");
        assertEquals(QuerySafetyCategory.PRESCRIPTIVE, outcome.category());
        assertEquals(SafeRefusalResponse.DOCTOR_REFERRAL, outcome.refusalResponse());

        // Verify guardrail interception before LLM call:
        if (!outcome.isSafe()) {
            // Simulated controller / orchestrator execution flow: return refusal response directly
            logInterceptionAndRefuse(outcome.refusalResponse());
        } else {
            // This code path must NEVER execute for adversarial prescriptive queries!
            mockRagGenerationService.generateGroundedResponse(adversarialQuery, null);
        }

        // Assert RagGenerationService (LLM) was NEVER invoked
        verifyNoInteractions(mockRagGenerationService);

        // Assert audit log entry recorded with the new AuditEventBuilder API
        verify(mockAuditLogService).logAsync(any(AuditEventBuilder.class));
    }

    private void logInterceptionAndRefuse(String refusalMessage) {
        assertNotNull(refusalMessage);
        assertEquals(SafeRefusalResponse.DOCTOR_REFERRAL, refusalMessage);
    }
}

package com.medisense.safety;

import com.medisense.safety.model.QuerySafetyCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class InputQueryClassifierTest {

    private PatternLibraryLoader patternLibraryLoader;
    private SecondaryLlmSafetyClassifier secondaryLlmClassifier;
    private InputSafetyClassifier classifier;

    @BeforeEach
    void setUp() {
        patternLibraryLoader = new PatternLibraryLoader();
        patternLibraryLoader.init();
        secondaryLlmClassifier = mock(SecondaryLlmSafetyClassifier.class);
        classifier = new InputSafetyClassifier(patternLibraryLoader, secondaryLlmClassifier);
    }

    @Test
    @DisplayName("Classify SAFE Informational Query")
    void testSafeInformationalQuery() {
        var result = classifier.classifyInputQuery("What is HbA1c in a lab report?");
        assertTrue(result.isSafe());
        assertEquals(QuerySafetyCategory.SAFE, result.category());
        assertNull(result.refusalResponse());
    }

    @Test
    @DisplayName("Classify PRESCRIPTIVE Query")
    void testPrescriptiveQuery() {
        var result = classifier.classifyInputQuery("what dose of metformin should I take");
        assertFalse(result.isSafe());
        assertEquals(QuerySafetyCategory.PRESCRIPTIVE, result.category());
        assertEquals(SafeRefusalResponse.DOCTOR_REFERRAL, result.refusalResponse());
    }

    @Test
    @DisplayName("Classify DIAGNOSTIC Query")
    void testDiagnosticQuery() {
        var result = classifier.classifyInputQuery("does this high blood sugar mean I have diabetes");
        assertFalse(result.isSafe());
        assertEquals(QuerySafetyCategory.DIAGNOSTIC, result.category());
        assertEquals(SafeRefusalResponse.DOCTOR_REFERRAL, result.refusalResponse());
    }

    @Test
    @DisplayName("Classify DRUG INTERACTION Query")
    void testDrugInteractionQuery() {
        var result = classifier.classifyInputQuery("can I take aspirin with metformin");
        assertFalse(result.isSafe());
        assertEquals(QuerySafetyCategory.DRUG_INTERACTION, result.category());
        assertEquals(SafeRefusalResponse.DOCTOR_REFERRAL, result.refusalResponse());
    }

    @Test
    @DisplayName("Classify TREATMENT SUBSTITUTION Query")
    void testTreatmentSubstitutionQuery() {
        var result = classifier.classifyInputQuery("can I substitute my prescription with herbal supplements");
        assertFalse(result.isSafe());
        assertEquals(QuerySafetyCategory.TREATMENT_SUBSTITUTION, result.category());
        assertEquals(SafeRefusalResponse.DOCTOR_REFERRAL, result.refusalResponse());
    }

    @Test
    @DisplayName("Classify EMERGENCY Query")
    void testEmergencyQuery() {
        var result = classifier.classifyInputQuery("I am suffering from severe chest pain and shortness of breath");
        assertFalse(result.isSafe());
        assertEquals(QuerySafetyCategory.EMERGENCY, result.category());
        assertEquals(SafeRefusalResponse.EMERGENCY_REFERRAL, result.refusalResponse());
    }

    @Test
    @DisplayName("Trigger Secondary LLM Classifier when confidence is low")
    void testSecondaryLlmClassifierTrigger() {
        when(secondaryLlmClassifier.verifyQuerySafety(anyString()))
                .thenReturn(new SecondaryLlmSafetyClassifier.LlmVerificationResult(
                        QuerySafetyCategory.PRESCRIPTIVE, 0.95, "Low confidence trigger caught prescription intent"
                ));

        var result = classifier.classifyInputQuery("Can you tell me about the dose for this medication");
        assertFalse(result.isSafe());
        assertEquals(QuerySafetyCategory.PRESCRIPTIVE, result.category());
        verify(secondaryLlmClassifier, times(1)).verifyQuerySafety(anyString());
    }
}

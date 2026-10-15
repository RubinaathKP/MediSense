package com.medisense.safety;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OutputSafetyFilterTest {

    private OutputSafetyFilter filter;

    @BeforeEach
    void setUp() {
        filter = new OutputSafetyFilter();
    }

    @Test
    @DisplayName("Clean Informational Response Passes Filter")
    void testCleanOutputPasses() {
        String cleanText = "Glycated hemoglobin (HbA1c) reflects average blood glucose levels over the past 2 to 3 months.";
        var result = filter.filterGeneratedOutput(cleanText);
        assertTrue(result.isSafe());
        assertEquals(cleanText, result.filteredContent());
    }

    @Test
    @DisplayName("Flag Drug Name Near Prescriptive Verb (5-token window)")
    void testFlagDrugNameNearPrescriptiveVerb() {
        String unsafeText = "For high blood pressure, you should take metformin daily.";
        var result = filter.filterGeneratedOutput(unsafeText);
        assertFalse(result.isSafe());
        assertEquals(SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT, result.filteredContent());
        assertEquals("DRUG_PRESCRIPTIVE_WINDOW", result.flagReason());
    }

    @Test
    @DisplayName("Flag Dosage Unit Pattern")
    void testFlagDosageUnitPattern() {
        String unsafeText = "The recommended dosage mentioned in literature is 500 mg daily.";
        var result = filter.filterGeneratedOutput(unsafeText);
        assertFalse(result.isSafe());
        assertEquals(SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT, result.filteredContent());
        assertEquals("DOSAGE_PATTERN_DETECTED", result.flagReason());
    }

    @Test
    @DisplayName("Flag Directive Medical Phrasing")
    void testFlagDirectiveMedicalPhrasing() {
        String unsafeText = "You should avoid this medication if your levels are low.";
        var result = filter.filterGeneratedOutput(unsafeText);
        assertFalse(result.isSafe());
        assertEquals(SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT, result.filteredContent());
        assertEquals("DIRECTIVE_MEDICAL_PHRASING", result.flagReason());
    }

    @Test
    @DisplayName("Flag Drug Interaction Phrasing")
    void testFlagInteractionPhrasing() {
        String unsafeText = "Note that this drug interacts with blood thinners.";
        var result = filter.filterGeneratedOutput(unsafeText);
        assertFalse(result.isSafe());
        assertEquals(SafeRefusalResponse.UNVERIFIED_OR_FLAGGED_OUTPUT, result.filteredContent());
        assertEquals("INTERACTION_LANGUAGE_DETECTED", result.flagReason());
    }
}

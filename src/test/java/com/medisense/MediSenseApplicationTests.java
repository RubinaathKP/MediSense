package com.medisense;

import com.medisense.domain.enums.SafetyDecision;
import com.medisense.safety.InputSafetyClassifier;
import com.medisense.safety.PatternLibraryLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediSenseApplicationTests {

    private InputSafetyClassifier safetyClassifier;

    @BeforeEach
    void setUp() {
        PatternLibraryLoader patternLibraryLoader = new PatternLibraryLoader();
        patternLibraryLoader.init();
        safetyClassifier = new InputSafetyClassifier(patternLibraryLoader, null);
    }

    @Test
    void testStandardHealthQueryPassesSafetyFilter() {
        var result = safetyClassifier.classifyQuery("What does a high LDL value in a lipid profile mean?");
        assertEquals(SafetyDecision.PASSED, result.decision());
        assertNull(result.refusalResponse());
    }

    @Test
    void testPrescriptiveQueryIsIntercepted() {
        var result = safetyClassifier.classifyQuery("Should I take 10mg of Atorvastatin daily for my high cholesterol?");
        assertEquals(SafetyDecision.REFUSED_PRESCRIPTIVE, result.decision());
        assertNotNull(result.refusalResponse());
        assertTrue(result.refusalResponse().contains("doctor's judgment"));
    }

    @Test
    void testEmergencyQueryIsIntercepted() {
        var result = safetyClassifier.classifyQuery("I am having severe chest pain right now, what medicine should I take?");
        assertEquals(SafetyDecision.REFUSED_EMERGENCY, result.decision());
        assertTrue(result.refusalResponse().contains("immediate medical attention"));
    }
}

package com.medisense.translation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NumericValueProtectorTest {

    private NumericValueProtector protector;

    @BeforeEach
    void setUp() {
        ProtectedTermsLoader loader = new ProtectedTermsLoader();
        loader.init();
        protector = new NumericValueProtector(loader, null);
    }

    @Test
    void testProtectValuesExtractsNumericAndDrugPlaceholders() {
        String input = "Your LDL Cholesterol value is 145 mg/dL. You were prescribed Atorvastatin 10 mg daily.";

        NumericValueProtector.ProtectionResult result = protector.protectValues(input);

        assertNotNull(result.maskedText());
        assertFalse(result.placeholderRegistry().isEmpty());
        assertTrue(result.maskedText().contains("{NUM_1}"));
        assertTrue(result.maskedText().contains("{DRUG_1}"));
        assertTrue(result.placeholderRegistry().containsValue("145 mg/dL"));
        assertTrue(result.placeholderRegistry().containsValue("Atorvastatin"));
    }

    @Test
    void testRestoreValuesReplacesPlaceholdersCorrectly() {
        String input = "Your LDL Cholesterol value is 145 mg/dL. Prescribed Atorvastatin 10 mg.";

        NumericValueProtector.ProtectionResult result = protector.protectValues(input);
        String translatedMasked = result.maskedText() + " (தமிழில் விளக்கம்)";

        String restored = protector.restoreValues(translatedMasked, input, result.placeholderRegistry());

        assertTrue(restored.contains("145 mg/dL"));
        assertTrue(restored.contains("Atorvastatin"));
        assertFalse(restored.contains("{NUM_1}"));
        assertFalse(restored.contains("{DRUG_1}"));
    }

    @Test
    void testRestoreValuesThrowsExceptionWhenPlaceholderMissing() {
        String input = "Your LDL Cholesterol value is 145 mg/dL.";
        NumericValueProtector.ProtectionResult result = protector.protectValues(input);

        // Simulate translation dropping the placeholder
        String corruptedTranslation = "உங்களின் இரத்தப் பரிசோதனை முடிவுகள் வந்துவிட்டன.";

        TranslationIntegrityException ex = assertThrows(
                TranslationIntegrityException.class,
                () -> protector.restoreValues(corruptedTranslation, input, result.placeholderRegistry())
        );

        assertNotNull(ex.getMissingPlaceholder());
        assertEquals(input, ex.getOriginalText());
    }
}

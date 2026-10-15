package com.medisense.unit.safety.classifier;

import com.medisense.UnitTestBase;
import com.medisense.fixtures.AssertionHelpers;
import com.medisense.safety.InputSafetyClassifier;
import com.medisense.safety.PatternLibraryLoader;
import com.medisense.safety.SecondaryLlmSafetyClassifier;
import com.medisense.safety.model.QuerySafetyCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test: InputSafetyClassifier
 * Verifies guardrail classification for Emergency, Prescriptive, Drug Interaction,
 * and Treatment Substitution queries.
 */
class InputClassifierUnitTest extends UnitTestBase {

    @Mock
    private SecondaryLlmSafetyClassifier secondaryLlmClassifier;

    private InputSafetyClassifier classifier;

    @Override
    protected void onSetup() {
        PatternLibraryLoader loader = new PatternLibraryLoader();
        loader.init();
        classifier = new InputSafetyClassifier(loader, secondaryLlmClassifier);
    }

    @Test
    @DisplayName("Should immediately intercept emergency medical symptoms with urgent referral")
    void shouldInterceptEmergencyQueries() {
        String emergencyQuery = "I am having severe crushing chest pain radiating to left arm right now";
        var result = classifier.classifyInputQuery(emergencyQuery);

        AssertionHelpers.assertThat(result)
                .hasCategory(QuerySafetyCategory.EMERGENCY)
                .isRefusedWith("immediate medical attention");
    }

    @Test
    @DisplayName("Should intercept prescriptive dosage requests before RAG retrieval")
    void shouldInterceptPrescriptiveQueries() {
        String prescriptiveQuery = "What dose of metformin 500mg should I take daily for my sugar?";
        var result = classifier.classifyInputQuery(prescriptiveQuery);

        AssertionHelpers.assertThat(result)
                .hasCategory(QuerySafetyCategory.PRESCRIPTIVE)
                .isRefusedWith("doctor's judgment");
    }

    @Test
    @DisplayName("Should intercept drug-drug interaction inquiries for clinical referral")
    void shouldInterceptDrugInteractionQueries() {
        String interactionQuery = "Can I combine atorvastatin and amlodipine together safely?";
        var result = classifier.classifyInputQuery(interactionQuery);

        AssertionHelpers.assertThat(result)
                .hasCategory(QuerySafetyCategory.DRUG_INTERACTION)
                .isRefusedWith("doctor's judgment");
    }

    @Test
    @DisplayName("Should allow factual health literacy inquiries to pass safely")
    void shouldPassFactualHealthLiteracyQueries() {
        String factualQuery = "What is considered a normal fasting blood sugar level in mg/dL?";
        var result = classifier.classifyInputQuery(factualQuery);

        AssertionHelpers.assertThat(result)
                .isSafe()
                .hasCategory(QuerySafetyCategory.SAFE);
        assertThat(result.refusalResponse()).isNull();
    }
}

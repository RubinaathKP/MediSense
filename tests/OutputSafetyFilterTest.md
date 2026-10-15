# Test Specification: OutputSafetyFilterTest

## 1. Test Suite Overview
- **Component Under Test:** `com.medisense.safety.OutputSafetyFilter`
- **Category:** Unit Test (Post-Generation Guardrail Engine)
- **Framework:** JUnit 5, AssertJ
- **File Location:** `src/test/java/com/medisense/unit/safety/filter/SafetyFilterUnitTest.java`

---

## 2. Source Code Implementation
```java
package com.medisense.unit.safety.filter;

import com.medisense.UnitTestBase;
import com.medisense.fixtures.AssertionHelpers;
import com.medisense.safety.OutputSafetyFilter;
import com.medisense.safety.OutputSafetyFilter.OutputFilterResult;
import com.medisense.safety.SafeRefusalResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SafetyFilterUnitTest extends UnitTestBase {

    private OutputSafetyFilter outputSafetyFilter;

    @Override
    protected void onSetup() {
        outputSafetyFilter = new OutputSafetyFilter();
    }

    @Test
    @DisplayName("Should allow factual educational responses without prescriptive language")
    void shouldPassCleanEducationalResponse() {
        String safeText = "Borderline LDL cholesterol (130-159 mg/dL) reflects mild elevation. Dietary fiber and lifestyle activity help maintain normal metabolic function.";
        OutputFilterResult result = outputSafetyFilter.filterGeneratedOutput(safeText);

        AssertionHelpers.assertThat(result).isSafe();
        assertThat(result.filteredContent()).isEqualTo(safeText);
    }

    @Test
    @DisplayName("Should flag and redact directive phrasing commanding the user to take a dose")
    void shouldFlagDirectiveDosagePhrasing() {
        String dangerousText = "Based on your test results, you should take 20mg of atorvastatin every evening.";
        OutputFilterResult result = outputSafetyFilter.filterGeneratedOutput(dangerousText);

        AssertionHelpers.assertThat(result)
                .isFlaggedWithReason("Directive phrasing detected");
        assertThat(result.filteredContent()).isEqualTo(SafeRefusalResponse.DOCTOR_REFERRAL);
    }

    @Test
    @DisplayName("Should flag contraindication or interaction claims in generated output")
    void shouldFlagInteractionPhrasing() {
        String interactionText = "Do not combine metformin with this medicine as it interacts with blood pressure tablets.";
        OutputFilterResult result = outputSafetyFilter.filterGeneratedOutput(interactionText);

        AssertionHelpers.assertThat(result)
                .isFlaggedWithReason("Contraindication or interaction language detected");
        assertThat(result.filteredContent()).isEqualTo(SafeRefusalResponse.DOCTOR_REFERRAL);
    }
}
```

# Test Specification: CitationVerifierTest

## 1. Test Suite Overview
- **Component Under Test:** `com.medisense.safety.CitationVerifier`
- **Category:** Unit Test (Citation Grounding & Anti-Hallucination)
- **Framework:** JUnit 5, AssertJ
- **File Location:** `src/test/java/com/medisense/unit/safety/verifier/CitationVerifierUnitTest.java`

---

## 2. Source Code Implementation
```java
package com.medisense.unit.safety.verifier;

import com.medisense.UnitTestBase;
import com.medisense.fixtures.AssertionHelpers;
import com.medisense.safety.CitationVerifier;
import com.medisense.safety.CitationVerifier.CitationVerificationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CitationVerifierUnitTest extends UnitTestBase {

    private CitationVerifier citationVerifier;

    @Override
    protected void onSetup() {
        citationVerifier = new CitationVerifier();
    }

    @Test
    @DisplayName("Should verify citations when all referenced tags match retrieved chunk identifiers")
    void shouldVerifyValidGroundedCitations() {
        String response = "Normal fasting blood glucose is 70 to 99 mg/dL [Source: ICMR-DIABETES-01].";
        List<String> retrievedChunks = List.of("ICMR-DIABETES-01", "WHO-NCD-2023");

        CitationVerificationResult result = citationVerifier.verifyCitations(response, retrievedChunks);

        AssertionHelpers.assertThat(result).isGrounded();
        assertThat(result.extractedCitations()).contains("ICMR-DIABETES-01");
        assertThat(result.hallucinatedCitations()).isEmpty();
    }

    @Test
    @DisplayName("Should detect hallucinated citations not present in retrieved context")
    void shouldDetectHallucinatedCitations() {
        String response = "Creatinine above 1.2 mg/dL indicates reduced filtration [Source: FAKE-SOURCE-999].";
        List<String> retrievedChunks = List.of("ICMR-RENAL-01");

        CitationVerificationResult result = citationVerifier.verifyCitations(response, retrievedChunks);

        AssertionHelpers.assertThat(result)
                .hasHallucinatedCitation("FAKE-SOURCE-999");
        assertThat(result.isGrounded()).isFalse();
        assertThat(result.refusalResponse()).isNotNull();
    }
}
```

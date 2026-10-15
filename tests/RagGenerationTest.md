# Test Specification: RagGenerationTest

## 1. Test Suite Overview
- **Component Under Test:** `com.medisense.rag.generation.RagGenerationService`
- **Category:** Unit Test (LLM Response Generation & Grounding)
- **Framework:** JUnit 5, Mockito, AssertJ
- **File Location:** `src/test/java/com/medisense/unit/rag/generation/RagGenerationUnitTest.java`

---

## 2. Source Code Implementation
```java
package com.medisense.unit.rag.generation;

import com.medisense.UnitTestBase;
import com.medisense.rag.generation.GenerationOrchestrator;
import com.medisense.rag.generation.PromptAssembler;
import com.medisense.rag.generation.RagGenerationService;
import com.medisense.rag.generation.model.CitationDto;
import com.medisense.rag.generation.model.MediSenseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagGenerationUnitTest extends UnitTestBase {

    @Mock
    private GenerationOrchestrator generationOrchestrator;

    @Mock
    private PromptAssembler promptAssembler;

    private RagGenerationService ragGenerationService;

    @Override
    protected void onSetup() {
        ragGenerationService = new RagGenerationService(generationOrchestrator, promptAssembler);
    }

    @Test
    @DisplayName("Should generate grounded clinical explanation from retrieved passages")
    void shouldGenerateGroundedResponse() {
        String query = "What does a high LDL level mean?";
        List<String> passages = List.of("LDL cholesterol above 160 mg/dL is classified as high and elevates cardiovascular risk.");

        MediSenseResponse mockResponse = new MediSenseResponse(
                "A high LDL level indicates elevated cholesterol that can build up in arterial walls. Consult your physician.",
                List.of(new CitationDto("ICMR_Lipid_Guidelines", "Chapter 1", 15, "high_confidence")),
                "en",
                false,
                null
        );

        when(generationOrchestrator.generateResponse(isNull(), eq(query), eq("en"), isNull()))
                .thenReturn(mockResponse);

        String result = ragGenerationService.generateGroundedResponse(query, passages);

        assertThat(result)
                .isNotBlank()
                .contains("elevated cholesterol")
                .contains("Consult your physician");
        verify(generationOrchestrator).generateResponse(isNull(), eq(query), eq("en"), isNull());
    }
}
```

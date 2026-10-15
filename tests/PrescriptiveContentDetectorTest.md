# Test Specification: PrescriptiveContentDetectorTest

## 1. Test Suite Overview
- **Component Under Test:** `com.medisense.ingestion.PrescriptiveContentDetectorService`
- **Category:** Unit Test (Safety Guardrails / Ingestion Pipeline)
- **Framework:** JUnit 5, AssertJ
- **File Location:** `src/test/java/com/medisense/unit/ingestion/detector/PrescriptiveDetectorUnitTest.java`

---

## 2. Source Code Implementation
```java
package com.medisense.unit.ingestion.detector;

import com.medisense.UnitTestBase;
import com.medisense.ingestion.PrescriptiveContentDetectorService;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.IngestionResult;
import com.medisense.ingestion.model.NodeLevel;
import com.medisense.ingestion.model.StructuredNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptiveDetectorUnitTest extends UnitTestBase {

    private PrescriptiveContentDetectorService detector;

    @Override
    protected void onSetup() {
        detector = new PrescriptiveContentDetectorService();
    }

    @Test
    @DisplayName("Should detect and exclude dosage directive language from corpus structure")
    void shouldExcludeDosageDirectives() {
        StructuredNode prescriptiveNode = StructuredNode.builder()
                .level(NodeLevel.PARAGRAPH)
                .heading("Dosage Instruction")
                .textContent("For adult onset type 2 diabetes, administer 500 mg daily with food.")
                .positionPath("1.1.1")
                .children(new ArrayList<>())
                .build();

        StructuredNode safeNode = StructuredNode.builder()
                .level(NodeLevel.PARAGRAPH)
                .heading("Disease Overview")
                .textContent("Type 2 diabetes is characterized by progressive insulin resistance and beta cell dysfunction.")
                .positionPath("1.1.2")
                .children(new ArrayList<>())
                .build();

        DocumentStructure structure = DocumentStructure.builder()
                .rootNodes(List.of(prescriptiveNode, safeNode))
                .totalNodeCount(2)
                .build();

        IngestionResult result = detector.detectAndFilter(structure, "Diabetes_Guideline.pdf");

        assertThat(result).isNotNull();
        assertThat(result.getExcludedNodeCount()).isEqualTo(1);
        assertThat(result.getSafeNodes()).hasSize(1);
        assertThat(result.getSafeNodes().get(0).getHeading()).isEqualTo("Disease Overview");
        assertThat(result.getExcludedNodesLog()).anyMatch(log -> log.contains("Dosage Instruction"));
    }

    @Test
    @DisplayName("Should filter out formulary drug tables containing dosage and route")
    void shouldFilterDrugFormularyTables() {
        StructuredNode tableNode = StructuredNode.builder()
                .level(NodeLevel.TABLE)
                .heading("Drug Table")
                .textContent("Generic medicine brand name dose frequency route: Metformin oral 500mg BID.")
                .positionPath("2.1")
                .children(new ArrayList<>())
                .build();

        DocumentStructure structure = DocumentStructure.builder()
                .rootNodes(List.of(tableNode))
                .totalNodeCount(1)
                .build();

        IngestionResult result = detector.detectAndFilter(structure, "Formulary.pdf");

        assertThat(result.getExcludedNodeCount()).isEqualTo(1);
        assertThat(result.getSafeNodes()).isEmpty();
    }

    @Test
    @DisplayName("Should preserve safe clinical explanation nodes without directives")
    void shouldPreserveSafeClinicalInformation() {
        StructuredNode infoNode = StructuredNode.builder()
                .level(NodeLevel.SECTION)
                .heading("Normal Fasting Glucose Reference")
                .textContent("Fasting plasma glucose below 100 mg/dL is considered within normal physiological parameters.")
                .positionPath("3.1")
                .children(new ArrayList<>())
                .build();

        DocumentStructure structure = DocumentStructure.builder()
                .rootNodes(List.of(infoNode))
                .totalNodeCount(1)
                .build();

        IngestionResult result = detector.detectAndFilter(structure, "Glucose_Standards.pdf");

        assertThat(result.getExcludedNodeCount()).isEqualTo(0);
        assertThat(result.getSafeNodes()).hasSize(1);
    }
}
```

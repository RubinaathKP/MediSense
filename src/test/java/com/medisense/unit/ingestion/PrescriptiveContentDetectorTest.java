package com.medisense.unit.ingestion;

import com.medisense.UnitTestBase;
import com.medisense.fixtures.TestDataBuilder;
import com.medisense.ingestion.PrescriptiveContentDetectorService;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.IngestionResult;
import com.medisense.ingestion.model.NodeLevel;
import com.medisense.ingestion.model.StructuredNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test: PrescriptiveContentDetectorTest
 * Tests prescriptive safety filters: dosage directives, drug formulary tables, multi-pattern accounting, and false positive prevention.
 */
class PrescriptiveContentDetectorTest extends UnitTestBase {

    private PrescriptiveContentDetectorService detectorService;

    @Override
    protected void onSetup() {
        detectorService = new PrescriptiveContentDetectorService();
    }

    @Test
    @DisplayName("Case 1: Clean text (no prescriptive content) is not flagged")
    void testCleanTextNotFlagged() {
        String input = "LDL cholesterol is associated with cardiovascular risk";

        // Assert: not flagged
        assertThat(detectorService.isPrescriptive(input)).isFalse();
        assertThat(detectorService.detectPattern(input)).isNull();

        StructuredNode safeNode = TestDataBuilder.aStructuredNode()
                .textContent(input)
                .build();
        DocumentStructure structure = TestDataBuilder.aDocumentStructure()
                .rootNodes(List.of(safeNode))
                .build();

        IngestionResult result = detectorService.detectAndFilter(structure, "clean_guideline.pdf");
        assertThat(result.getSafeNodes()).hasSize(1);
        assertThat(result.getExcludedNodeCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Case 2: Dosage pattern detected and flagged as dosage_directive")
    void testDosagePatternDetected() {
        String input = "Take 10mg once daily";

        // Assert: flagged as prescriptive, pattern matched: "dosage_directive"
        assertThat(detectorService.isPrescriptive(input)).isTrue();
        assertThat(detectorService.detectPattern(input)).isEqualTo("dosage_directive");

        StructuredNode prescriptiveNode = TestDataBuilder.aStructuredNode()
                .textContent(input)
                .build();
        DocumentStructure structure = TestDataBuilder.aDocumentStructure()
                .rootNodes(List.of(prescriptiveNode))
                .build();

        IngestionResult result = detectorService.detectAndFilter(structure, "rx_directive.pdf");
        assertThat(result.getSafeNodes()).isEmpty();
        assertThat(result.getExcludedNodeCount()).isEqualTo(1);
        assertThat(result.getExcludedNodesLog().get(0)).contains("dosage_directive");
    }

    @Test
    @DisplayName("Case 3: Drug formulary table pattern detected and flagged as drug_table")
    void testDrugFormularyTablePatternDetected() {
        String input = "Drug | Dosage | Frequency\nMetformin | 500mg | BID";

        // Assert: flagged, pattern: "drug_table"
        assertThat(detectorService.isPrescriptive(input)).isTrue();
        assertThat(detectorService.detectPattern(input)).isEqualTo("drug_table");

        StructuredNode formularyNode = TestDataBuilder.aStructuredNode()
                .level(NodeLevel.TABLE)
                .textContent(input)
                .build();
        DocumentStructure structure = TestDataBuilder.aDocumentStructure()
                .rootNodes(List.of(formularyNode))
                .build();

        IngestionResult result = detectorService.detectAndFilter(structure, "formulary.pdf");
        assertThat(result.getSafeNodes()).isEmpty();
        assertThat(result.getExcludedNodeCount()).isEqualTo(1);
        assertThat(result.getExcludedNodesLog().get(0)).contains("drug_table");
    }

    @Test
    @DisplayName("Case 4: Multiple prescriptive patterns all flagged with accurate counts")
    void testMultiplePrescriptivePatternsFlaggedWithCounts() {
        StructuredNode node1 = TestDataBuilder.aStructuredNode()
                .positionPath("1.1")
                .textContent("Take 10mg once daily after meals")
                .build();
        StructuredNode node2 = TestDataBuilder.aStructuredNode()
                .positionPath("1.2")
                .textContent("Cardiovascular disease risk factors include smoking and elevated LDL.")
                .build();
        StructuredNode node3 = TestDataBuilder.aStructuredNode()
                .positionPath("1.3")
                .textContent("Drug | Dosage | Frequency\nAtorvastatin | 20mg | OD")
                .build();
        StructuredNode node4 = TestDataBuilder.aStructuredNode()
                .positionPath("1.4")
                .textContent("First-line therapy for dyslipidemia involves statin titrate up to recommended dosage")
                .build();

        DocumentStructure structure = TestDataBuilder.aDocumentStructure()
                .rootNodes(List.of(node1, node2, node3, node4))
                .build();

        IngestionResult result = detectorService.detectAndFilter(structure, "multi_pattern_guideline.pdf");

        // Assert: all sections flagged with counts
        assertThat(result.getTotalNodesScanned()).isEqualTo(4);
        assertThat(result.getExcludedNodeCount()).isEqualTo(3);
        assertThat(result.getSafeNodes()).hasSize(1);
        assertThat(result.getSafeNodes().get(0).getTextContent())
                .contains("Cardiovascular disease risk factors include smoking and elevated LDL.");
        assertThat(result.getExcludedNodesLog()).hasSize(3);
    }

    @Test
    @DisplayName("Case 5: Edge case: 'dose' in non-medical context is not flagged")
    void testDoseInNonMedicalContextNotFlagged() {
        String input = "a dose of reality";

        // Assert: not flagged (context check prevents false positive)
        assertThat(detectorService.isPrescriptive(input)).isFalse();
        assertThat(detectorService.detectPattern(input)).isNull();

        StructuredNode metaphorNode = TestDataBuilder.aStructuredNode()
                .textContent(input)
                .build();
        DocumentStructure structure = TestDataBuilder.aDocumentStructure()
                .rootNodes(List.of(metaphorNode))
                .build();

        IngestionResult result = detectorService.detectAndFilter(structure, "metaphor.pdf");
        assertThat(result.getSafeNodes()).hasSize(1);
        assertThat(result.getExcludedNodeCount()).isEqualTo(0);
    }
}

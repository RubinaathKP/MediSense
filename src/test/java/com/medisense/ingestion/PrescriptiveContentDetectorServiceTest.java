package com.medisense.ingestion;

import com.medisense.ingestion.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptiveContentDetectorServiceTest {

    private PrescriptiveContentDetectorService detectorService;

    @BeforeEach
    void setUp() {
        detectorService = new PrescriptiveContentDetectorService();
    }

    @Test
    void testPrescriptiveContentDetectionAndExclusion() {
        StructuredNode safeNode = StructuredNode.builder()
                .level(NodeLevel.PARAGRAPH)
                .textContent("High cholesterol increases the risk of cardiovascular disease.")
                .positionPath("1.1.p1")
                .children(new ArrayList<>())
                .build();

        StructuredNode dosageDirectiveNode = StructuredNode.builder()
                .level(NodeLevel.PARAGRAPH)
                .textContent("Take 20mg of Atorvastatin daily at bedtime.")
                .positionPath("1.1.p2")
                .children(new ArrayList<>())
                .build();

        StructuredNode protocolNode = StructuredNode.builder()
                .level(NodeLevel.PARAGRAPH)
                .textContent("First-line therapy for stage 2 hypertension involves combination therapy.")
                .positionPath("1.1.p3")
                .children(new ArrayList<>())
                .build();

        DocumentStructure structure = DocumentStructure.builder()
                .rootNodes(List.of(safeNode, dosageDirectiveNode, protocolNode))
                .totalNodeCount(3)
                .build();

        IngestionResult result = detectorService.detectAndFilter(structure, "prescriptive_test.pdf");

        assertNotNull(result);
        assertEquals(3, result.getTotalNodesScanned());
        assertEquals(2, result.getExcludedNodeCount());
        assertEquals(1, result.getSafeNodes().size());
        assertEquals("High cholesterol increases the risk of cardiovascular disease.", result.getSafeNodes().get(0).getTextContent());
        assertEquals(2, result.getExcludedNodesLog().size());
    }
}

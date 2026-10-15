package com.medisense.unit.ingestion;

import com.medisense.UnitTestBase;
import com.medisense.fixtures.TestDataBuilder;
import com.medisense.ingestion.StructureExtractorService;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.NodeLevel;
import com.medisense.ingestion.model.ParsedDocument;
import com.medisense.ingestion.model.StructuredNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test: StructureExtractorTest
 * Tests hierarchical tree extraction and default section fallback when headings are absent.
 */
class StructureExtractorTest extends UnitTestBase {

    private StructureExtractorService structureExtractorService;

    @Override
    protected void onSetup() {
        structureExtractorService = new StructureExtractorService();
    }

    @Test
    @DisplayName("Case 1: Simple document structure creates StructuredNode tree with correct level depths")
    void testSimpleDocumentStructureHierarchy() {
        String structuredContent = """
                Chapter 1 Cardiovascular Guidelines
                
                1.1 Lipid Profile Target Levels
                
                1.1.1 Borderline Risk Evaluation
                
                Adult patients presenting with borderline LDL levels should undergo evaluation.
                """;

        ParsedDocument parsed = TestDataBuilder.aParsedDocument()
                .documentName("structured_guideline.pdf")
                .rawText(structuredContent)
                .build();

        DocumentStructure structure = structureExtractorService.extractStructure(parsed);

        // Assert: StructuredNode tree created, level depths correct
        assertThat(structure).isNotNull();
        assertThat(structure.getRootNodes()).isNotEmpty();
        assertThat(structure.getTotalNodeCount()).isGreaterThanOrEqualTo(4);

        // Verify Root: Chapter level
        StructuredNode chapterNode = structure.getRootNodes().get(0);
        assertThat(chapterNode.getLevel()).isEqualTo(NodeLevel.CHAPTER);
        assertThat(chapterNode.getHeading()).contains("Chapter 1");
        assertThat(chapterNode.getPositionPath()).isEqualTo("1");

        // Verify Level 2: Section level
        assertThat(chapterNode.getChildren()).isNotEmpty();
        StructuredNode sectionNode = chapterNode.getChildren().get(0);
        assertThat(sectionNode.getLevel()).isEqualTo(NodeLevel.SECTION);
        assertThat(sectionNode.getHeading()).contains("1.1");
        assertThat(sectionNode.getPositionPath()).isEqualTo("1.1");

        // Verify Level 3: Subsection level
        assertThat(sectionNode.getChildren()).isNotEmpty();
        StructuredNode subsectionNode = sectionNode.getChildren().get(0);
        assertThat(subsectionNode.getLevel()).isEqualTo(NodeLevel.SUBSECTION);
        assertThat(subsectionNode.getHeading()).contains("1.1.1");
        assertThat(subsectionNode.getPositionPath()).isEqualTo("1.1.1");

        // Verify Level 4: Paragraph level
        assertThat(subsectionNode.getChildren()).isNotEmpty();
        StructuredNode paragraphNode = subsectionNode.getChildren().get(0);
        assertThat(paragraphNode.getLevel()).isEqualTo(NodeLevel.PARAGRAPH);
        assertThat(paragraphNode.getTextContent()).contains("Adult patients presenting with borderline LDL levels");
    }

    @Test
    @DisplayName("Case 2: Missing headings (mostly paragraph text) creates default section and includes all text")
    void testMissingHeadingsCreatesDefaultSection() {
        String unstructuredText = """
                This document contains clinical observations without any chapter or section headings.
                
                The patient demonstrates normal fasting blood glucose and creatinine parameters.
                
                Follow-up assessment is scheduled in six months for routine maintenance.
                """;

        ParsedDocument parsed = TestDataBuilder.aParsedDocument()
                .documentName("unstructured_notes.pdf")
                .rawText(unstructuredText)
                .build();

        DocumentStructure structure = structureExtractorService.extractStructure(parsed);

        // Assert: default section created, all text included
        assertThat(structure).isNotNull();
        assertThat(structure.getRootNodes()).hasSize(1);

        StructuredNode defaultSection = structure.getRootNodes().get(0);
        assertThat(defaultSection.getLevel()).isEqualTo(NodeLevel.SECTION);
        assertThat(defaultSection.getHeading()).isEqualTo("General");

        // Assert: all text paragraphs are included within the default section
        assertThat(defaultSection.getChildren()).hasSize(3);
        assertThat(defaultSection.getChildren().get(0).getTextContent())
                .contains("clinical observations without any chapter");
        assertThat(defaultSection.getChildren().get(1).getTextContent())
                .contains("fasting blood glucose");
        assertThat(defaultSection.getChildren().get(2).getTextContent())
                .contains("Follow-up assessment");
    }
}

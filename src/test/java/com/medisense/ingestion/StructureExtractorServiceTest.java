package com.medisense.ingestion;

import com.medisense.ingestion.model.CleanedDocument;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.NodeLevel;
import com.medisense.ingestion.model.StructuredNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StructureExtractorServiceTest {

    private StructureExtractorService structureExtractorService;

    @BeforeEach
    void setUp() {
        structureExtractorService = new StructureExtractorService();
    }

    @Test
    void testHierarchicalStructureExtraction() {
        List<String> paragraphs = List.of(
                "Chapter 1 Introduction to Health Literacy",
                "1.1 Overview of Guidelines",
                "1.1.1 Scope and Purpose",
                "This document outlines plain language health literacy standards."
        );

        CleanedDocument cleanedDoc = CleanedDocument.builder()
                .documentName("structure_test.pdf")
                .cleanedParagraphs(paragraphs)
                .build();

        DocumentStructure structure = structureExtractorService.extractStructure(cleanedDoc);

        assertNotNull(structure);
        assertEquals(1, structure.getRootNodes().size());

        StructuredNode chapterNode = structure.getRootNodes().get(0);
        assertEquals(NodeLevel.CHAPTER, chapterNode.getLevel());
        assertEquals("1", chapterNode.getPositionPath());

        assertFalse(chapterNode.getChildren().isEmpty());
        StructuredNode sectionNode = chapterNode.getChildren().get(0);
        assertEquals(NodeLevel.SECTION, sectionNode.getLevel());
        assertEquals("1.1", sectionNode.getPositionPath());

        StructuredNode subsectionNode = sectionNode.getChildren().get(0);
        assertEquals(NodeLevel.SUBSECTION, subsectionNode.getLevel());
        assertEquals("1.1.1", subsectionNode.getPositionPath());

        StructuredNode paragraphNode = subsectionNode.getChildren().get(0);
        assertEquals(NodeLevel.PARAGRAPH, paragraphNode.getLevel());
        assertEquals("1.1.1.p1", paragraphNode.getPositionPath());
    }
}

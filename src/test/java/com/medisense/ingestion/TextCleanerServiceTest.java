package com.medisense.ingestion;

import com.medisense.ingestion.model.CleanedDocument;
import com.medisense.ingestion.model.ParsedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextCleanerServiceTest {

    private TextCleanerService textCleanerService;

    @BeforeEach
    void setUp() {
        textCleanerService = new TextCleanerService();
    }

    @Test
    void testTextCleaningAndAbbreviationExpansion() {
        String rawText = "ICMR CLINICAL PRACTICE GUIDELINES\n" +
                "Chapter 1 Overview\n" +
                "Patients with NCD and HTN require BP monitoring.\n" +
                "Line-break hyphen-\n" +
                "ation should be fixed.\n" +
                "Page 1 of 10\n";

        ParsedDocument doc = ParsedDocument.builder()
                .documentName("guideline.pdf")
                .rawText(rawText)
                .build();

        CleanedDocument cleaned = textCleanerService.cleanText(doc);

        assertNotNull(cleaned);
        assertFalse(cleaned.getRawCleanedText().contains("Page 1 of 10"));
        assertTrue(cleaned.getRawCleanedText().contains("Non-Communicable Disease"));
        assertTrue(cleaned.getRawCleanedText().contains("Hypertension"));
        assertTrue(cleaned.getRawCleanedText().contains("Blood Pressure"));
        assertTrue(cleaned.getRawCleanedText().contains("hyphenation"));
    }
}

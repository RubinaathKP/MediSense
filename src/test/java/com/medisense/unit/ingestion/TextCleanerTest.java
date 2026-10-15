package com.medisense.unit.ingestion;

import com.medisense.UnitTestBase;
import com.medisense.fixtures.TestDataBuilder;
import com.medisense.ingestion.TextCleanerService;
import com.medisense.ingestion.model.CleanedDocument;
import com.medisense.ingestion.model.ParsedDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Test: TextCleanerTest
 * Tests noise reduction, hyphenation normalization, whitespace cleanup, and metadata abbreviation mapping.
 */
class TextCleanerTest extends UnitTestBase {

    private TextCleanerService textCleanerService;

    @Override
    protected void onSetup() {
        textCleanerService = new TextCleanerService();
    }

    @Test
    @DisplayName("Case 1: Text with headers/footers removed while preserving actual content")
    void testHeadersAndFootersRemoved() {
        String input = "Header text\n\n[actual content]\n\nFooter text";

        ParsedDocument parsed = TestDataBuilder.aParsedDocument()
                .documentName("header_footer_doc.pdf")
                .rawText(input)
                .build();

        CleanedDocument cleaned = textCleanerService.cleanText(parsed);

        // Assert: headers/footers removed, actual content preserved
        assertThat(cleaned).isNotNull();
        assertThat(cleaned.getRawCleanedText())
                .doesNotContain("Header text")
                .doesNotContain("Footer text")
                .contains("[actual content]");
        assertThat(cleaned.getCleanedParagraphs())
                .containsExactly("[actual content]");
    }

    @Test
    @DisplayName("Case 2: Hyphenation normalization repairs split words while preserving non-hyphen newlines")
    void testHyphenationNormalization() {
        String input = "anti-\nbiotic resistance\nregular newline without hyphen";

        ParsedDocument parsed = TestDataBuilder.aParsedDocument()
                .documentName("hyphenation_doc.pdf")
                .rawText(input)
                .build();

        CleanedDocument cleaned = textCleanerService.cleanText(parsed);

        // Assert: "antibiotic resistance" (newline preserved if not hyphen)
        assertThat(cleaned.getRawCleanedText())
                .contains("antibiotic resistance")
                .doesNotContain("anti-\nbiotic");
        // Verify regular newline is preserved when there is no hyphen
        assertThat(cleaned.getRawCleanedText())
                .contains("regular newline without hyphen");
    }

    @Test
    @DisplayName("Case 3: Multiple consecutive spaces collapsed into a single space")
    void testMultipleConsecutiveSpacesCollapsed() {
        String input = "LDL    cholesterol";

        ParsedDocument parsed = TestDataBuilder.aParsedDocument()
                .documentName("whitespace_doc.pdf")
                .rawText(input)
                .build();

        CleanedDocument cleaned = textCleanerService.cleanText(parsed);

        // Assert: "LDL cholesterol" (single space)
        assertThat(cleaned.getRawCleanedText()).isEqualTo("LDL cholesterol");
    }

    @Test
    @DisplayName("Case 4: Medical abbreviations mapped as metadata while text remains unchanged")
    void testMedicalAbbreviationsMappedAsMetadata() {
        String input = "HDL levels increased";

        ParsedDocument parsed = TestDataBuilder.aParsedDocument()
                .documentName("abbreviations_doc.pdf")
                .rawText(input)
                .build();

        CleanedDocument cleaned = textCleanerService.cleanText(parsed);

        // Assert: abbreviation mapped but text unchanged (mapping is metadata, not text replacement)
        assertThat(cleaned.getRawCleanedText())
                .isEqualTo("HDL levels increased")
                .doesNotContain("High-Density Lipoprotein levels increased");

        assertThat(cleaned.getMappedAbbreviations())
                .isNotNull()
                .containsKey("HDL")
                .containsEntry("HDL", "High-Density Lipoprotein");
    }
}

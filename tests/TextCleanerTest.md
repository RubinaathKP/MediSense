# Test Specification: TextCleanerTest

## 1. Test Suite Overview
- **Component Under Test:** `com.medisense.ingestion.TextCleanerService`
- **Category:** Unit Test (Document Ingestion Engine)
- **Framework:** JUnit 5, AssertJ
- **File Location:** `src/test/java/com/medisense/unit/ingestion/cleaner/TextCleanerUnitTest.java`

---

## 2. Source Code Implementation
```java
package com.medisense.unit.ingestion.cleaner;

import com.medisense.UnitTestBase;
import com.medisense.ingestion.TextCleanerService;
import com.medisense.ingestion.model.CleanedDocument;
import com.medisense.ingestion.model.ParsedDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class TextCleanerUnitTest extends UnitTestBase {

    private TextCleanerService textCleanerService;

    @Override
    protected void onSetup() {
        textCleanerService = new TextCleanerService();
    }

    @Test
    @DisplayName("Should strip headers, footers, and page numbers from document text")
    void shouldStripHeadersFootersAndPageNumbers() {
        String dirtyText = """
                ICMR Clinical Practice Guidelines
                Chapter 1 Clinical Protocols
                Page 1 of 12
                Serum cholesterol levels should be tested fasting.
                - 2 -
                CONFIDENTIAL
                """;

        ParsedDocument parsed = ParsedDocument.builder()
                .documentName("guidelines.pdf")
                .rawText(dirtyText)
                .metadata(Collections.emptyMap())
                .extractionConfidence(0.95)
                .tokenCount(25)
                .build();

        CleanedDocument cleaned = textCleanerService.cleanText(parsed);

        assertThat(cleaned).isNotNull();
        assertThat(cleaned.getCleanedText())
                .doesNotContain("Page 1 of 12")
                .doesNotContain("- 2 -")
                .doesNotContain("CONFIDENTIAL")
                .contains("Serum cholesterol levels should be tested fasting.");
    }

    @Test
    @DisplayName("Should repair line-break hyphenation in medical terms")
    void shouldRepairLineBreakHyphenation() {
        String rawText = "Patients with cardio-\\nvascular conditions should monitor athero-\\nsclerosis risk.";

        ParsedDocument parsed = ParsedDocument.builder()
                .documentName("cardio.pdf")
                .rawText(rawText)
                .metadata(Collections.emptyMap())
                .extractionConfidence(0.92)
                .tokenCount(15)
                .build();

        CleanedDocument cleaned = textCleanerService.cleanText(parsed);

        assertThat(cleaned.getCleanedText())
                .contains("cardiovascular")
                .contains("atherosclerosis")
                .doesNotContain("cardio-\\nvascular");
    }

    @Test
    @DisplayName("Should preserve section headings and normalize whitespace")
    void shouldPreserveSectionHeadingsAndNormalizeWhitespace() {
        String rawText = """
                Chapter 1 Lipid Management
                
                
                Elevated   LDL     cholesterol   warrants   dietary   review.
                """;

        ParsedDocument parsed = ParsedDocument.builder()
                .documentName("headings.pdf")
                .rawText(rawText)
                .metadata(Collections.emptyMap())
                .extractionConfidence(0.90)
                .tokenCount(12)
                .build();

        CleanedDocument cleaned = textCleanerService.cleanText(parsed);

        assertThat(cleaned.getCleanedText()).contains("Elevated LDL cholesterol warrants dietary review.");
        assertThat(cleaned.getSectionHeadings()).contains("Chapter 1 Lipid Management");
    }
}
```

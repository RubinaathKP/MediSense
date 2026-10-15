# Test Specification: FormatParserTest

## 1. Test Suite Overview
- **Component Under Test:** `com.medisense.ingestion.FormatParserService`
- **Category:** Unit Test (Document Ingestion Engine)
- **Framework:** JUnit 5, AssertJ, Apache PDFBox 2.0.31, Apache Tika 2.9.2
- **File Location:** `src/test/java/com/medisense/unit/ingestion/parser/FormatParserUnitTest.java`

---

## 2. Source Code Implementation
```java
package com.medisense.unit.ingestion.parser;

import com.medisense.UnitTestBase;
import com.medisense.ingestion.FormatParserService;
import com.medisense.ingestion.exception.DocumentParsingException;
import com.medisense.ingestion.model.ParsedDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormatParserUnitTest extends UnitTestBase {

    private FormatParserService formatParserService;

    @Override
    protected void onSetup() {
        formatParserService = new FormatParserService();
    }

    @Test
    @DisplayName("Should extract structured text from valid clinical lab report PDF")
    void shouldExtractTextFromValidLabReport() throws Exception {
        File file = new File("src/test/resources/fixtures/pdfs/valid_lab_report.pdf");
        assertThat(file).exists();

        try (InputStream is = new FileInputStream(file)) {
            ParsedDocument doc = formatParserService.parseFormat(is, "valid_lab_report.pdf");

            assertThat(doc).isNotNull();
            assertThat(doc.getDocumentName()).isEqualTo("valid_lab_report.pdf");
            assertThat(doc.getRawText())
                    .contains("MEDISENSE CLINICAL LABORATORY REPORT")
                    .contains("LDL Cholesterol")
                    .contains("145")
                    .contains("mg/dL");
            assertThat(doc.getExtractionConfidence()).isGreaterThanOrEqualTo(0.85);
            assertThat(doc.getTokenCount()).isGreaterThan(20);
        }
    }

    @Test
    @DisplayName("Should throw DocumentParsingException when parsing image-only PDF with zero text")
    void shouldRejectImageOnlyPdfWithoutText() throws Exception {
        File file = new File("src/test/resources/fixtures/pdfs/invalid_lab_report_image_only.pdf");
        assertThat(file).exists();

        try (InputStream is = new FileInputStream(file)) {
            assertThatThrownBy(() -> formatParserService.parseFormat(is, "invalid_lab_report_image_only.pdf"))
                    .isInstanceOf(DocumentParsingException.class);
        }
    }

    @Test
    @DisplayName("Should throw DocumentParsingException on corrupted PDF byte stream")
    void shouldThrowExceptionOnCorruptedPdf() throws Exception {
        File file = new File("src/test/resources/fixtures/pdfs/corrupted.pdf");
        assertThat(file).exists();

        try (InputStream is = new FileInputStream(file)) {
            assertThatThrownBy(() -> formatParserService.parseFormat(is, "corrupted.pdf"))
                    .isInstanceOf(DocumentParsingException.class);
        }
    }

    @Test
    @DisplayName("Should extract tabular formulary text preserving column markers")
    void shouldExtractFormularyTableText() throws Exception {
        File file = new File("src/test/resources/fixtures/pdfs/formulary_table.pdf");
        assertThat(file).exists();

        try (InputStream is = new FileInputStream(file)) {
            ParsedDocument doc = formatParserService.parseFormat(is, "formulary_table.pdf");

            assertThat(doc).isNotNull();
            assertThat(doc.getRawText())
                    .contains("Metformin")
                    .contains("Atorvastatin")
                    .contains("Biguanide");
        }
    }
}
```

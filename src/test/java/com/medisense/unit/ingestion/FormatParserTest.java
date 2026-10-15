package com.medisense.unit.ingestion;

import com.medisense.UnitTestBase;
import com.medisense.fixtures.PdfFixtureGenerator;
import com.medisense.ingestion.FormatParserService;
import com.medisense.ingestion.exception.DocumentQualityException;
import com.medisense.ingestion.exception.ParseException;
import com.medisense.ingestion.model.ParsedDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit Test: FormatParserTest
 * Tests the 6 specified PDF ingestion scenarios using AssertJ fluent assertions.
 */
class FormatParserTest extends UnitTestBase {

    private FormatParserService formatParserService;

    private static final File FIXTURES_DIR = new File("src/test/resources/fixtures/pdfs");
    private static final File VALID_LAB_REPORT = new File(FIXTURES_DIR, "valid_lab_report.pdf");
    private static final File MULTI_PAGE_LAB_REPORT = new File(FIXTURES_DIR, "lab_report_5_pages.pdf");
    private static final File IMAGE_ONLY_PDF = new File(FIXTURES_DIR, "invalid_lab_report_image_only.pdf");
    private static final File CORRUPTED_PDF = new File(FIXTURES_DIR, "corrupted.pdf");
    private static final File LARGE_100_PAGE_PDF = new File(FIXTURES_DIR, "lab_report_100_pages.pdf");

    @BeforeAll
    static void generateRequiredFixtures() throws Exception {
        if (!FIXTURES_DIR.exists()) {
            FIXTURES_DIR.mkdirs();
        }
        if (!VALID_LAB_REPORT.exists()) {
            PdfFixtureGenerator.generateValidLabReport(VALID_LAB_REPORT);
        }
        if (!MULTI_PAGE_LAB_REPORT.exists()) {
            PdfFixtureGenerator.generateMultiPageLabReport(MULTI_PAGE_LAB_REPORT, 5);
        }
        if (!IMAGE_ONLY_PDF.exists()) {
            PdfFixtureGenerator.generateInvalidImageOnlyPdf(IMAGE_ONLY_PDF);
        }
        if (!CORRUPTED_PDF.exists()) {
            PdfFixtureGenerator.generateCorruptedPdf(CORRUPTED_PDF);
        }
        if (!LARGE_100_PAGE_PDF.exists()) {
            PdfFixtureGenerator.generate100PagePdf(LARGE_100_PAGE_PDF);
        }
    }

    @Override
    protected void onSetup() {
        formatParserService = new FormatParserService();
    }

    @Test
    @DisplayName("Case 1: Happy path — valid single-page lab report PDF")
    void testHappyPathSinglePageLabReport() throws Exception {
        try (InputStream is = new FileInputStream(VALID_LAB_REPORT)) {
            ParsedDocument doc = formatParserService.parseFormat(is, "valid_lab_report.pdf");

            assertThat(doc).isNotNull();
            // Assert: extracted text non-empty
            assertThat(doc.getRawText()).isNotBlank();
            // Assert: confidence score > 0.8
            assertThat(doc.getExtractionConfidence()).isGreaterThan(0.8);
            // Assert: no extraction errors, contains expected markers
            assertThat(doc.getRawText())
                    .contains("MEDISENSE CLINICAL LABORATORY REPORT")
                    .contains("LDL Cholesterol")
                    .contains("145")
                    .contains("mg/dL");
            assertThat(doc.getTokenCount()).isGreaterThan(20);
        }
    }

    @Test
    @DisplayName("Case 2: Multi-page lab report (5 pages) extracts all pages and sections")
    void testMultiPageLabReport() throws Exception {
        try (InputStream is = new FileInputStream(MULTI_PAGE_LAB_REPORT)) {
            ParsedDocument doc = formatParserService.parseFormat(is, "lab_report_5_pages.pdf");

            assertThat(doc).isNotNull();
            assertThat(doc.getRawText()).isNotBlank();
            // Assert: all pages extracted, text contains expected sections
            assertThat(doc.getRawText())
                    .contains("SECTION 1: COMPLETE BLOOD COUNT & HEMATOLOGY")
                    .contains("SECTION 2: COMPREHENSIVE METABOLIC & LIPID PROFILE")
                    .contains("SECTION 3: HEPATIC FUNCTION & ENZYME PANEL")
                    .contains("SECTION 4: RENAL FUNCTION & ELECTROLYTE EVALUATION")
                    .contains("SECTION 5: ENDOCRINE, THYROID & HbA1c BIOMARKERS");
            assertThat(doc.getExtractionConfidence()).isGreaterThan(0.8);
        }
    }

    @Test
    @DisplayName("Case 3: Image-only PDF (scanned, no text layer) throws DocumentQualityException")
    void testImageOnlyPdfThrowsQualityException() {
        // Assert: extracted text empty or very short, confidence < 0.5, throws DocumentQualityException
        assertThatThrownBy(() -> {
            try (InputStream is = new FileInputStream(IMAGE_ONLY_PDF)) {
                formatParserService.parseFormat(is, "invalid_lab_report_image_only.pdf");
            }
        }).isInstanceOf(DocumentQualityException.class)
          .satisfies(ex -> {
              DocumentQualityException dqe = (DocumentQualityException) ex;
              assertThat(dqe.getExtractionConfidence()).isLessThan(0.5);
              assertThat(dqe.getTokenCount()).isLessThan(10);
              assertThat(dqe.getMessage()).containsIgnoringCase("Image-only");
          });
    }

    @Test
    @DisplayName("Case 4: Corrupted PDF file throws ParseException with clean error message")
    void testCorruptedPdfThrowsParseException() {
        // Assert: throws ParseException, clean error message
        assertThatThrownBy(() -> {
            try (InputStream is = new FileInputStream(CORRUPTED_PDF)) {
                formatParserService.parseFormat(is, "corrupted.pdf");
            }
        }).isInstanceOf(ParseException.class)
          .hasMessageContaining("Corrupted PDF document");
    }

    @Test
    @DisplayName("Case 5: Non-PDF file (text file with .pdf extension) throws ParseException with magic byte details")
    void testNonPdfFileThrowsParseExceptionWithMagicByteDetails() {
        String fakePdfContent = "Plain text document masquerading as a PDF file with invalid magic bytes.";
        InputStream is = new ByteArrayInputStream(fakePdfContent.getBytes(StandardCharsets.UTF_8));

        // Assert: throws ParseException with magic byte details
        assertThatThrownBy(() -> formatParserService.parseFormat(is, "masquerading_text.pdf"))
                .isInstanceOf(ParseException.class)
                .hasMessageContaining("Invalid PDF magic bytes")
                .hasMessageContaining("%PDF");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Case 6: Large PDF (100 pages) extraction completes without memory error within timeout")
    void testLarge100PagePdfCompletesWithinTimeout() throws Exception {
        try (InputStream is = new FileInputStream(LARGE_100_PAGE_PDF)) {
            ParsedDocument doc = formatParserService.parseFormat(is, "lab_report_100_pages.pdf");

            // Assert: extraction completes, no memory errors, timeout not exceeded
            assertThat(doc).isNotNull();
            assertThat(doc.getRawText()).isNotBlank();
            assertThat(doc.getRawText()).contains("Page 1 of 100");
            assertThat(doc.getRawText()).contains("Page 100 of 100");
            assertThat(doc.getTokenCount()).isGreaterThan(500);
            assertThat(doc.getExtractionConfidence()).isGreaterThan(0.8);
        }
    }
}

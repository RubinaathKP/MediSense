package com.medisense.ingestion;

import com.medisense.ingestion.exception.DocumentParsingException;
import com.medisense.ingestion.model.ParsedDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class FormatParserServiceTest {

    private FormatParserService formatParserService;

    @BeforeEach
    void setUp() {
        formatParserService = new FormatParserService();
    }

    @Test
    void testHappyPathTextExtraction() throws Exception {
        byte[] pdfBytes = createSamplePdf("Chapter 1 Medical Guidelines\nThis is a clinical guidelines document for health literacy.");
        ParsedDocument doc = formatParserService.parseFormat(new ByteArrayInputStream(pdfBytes), "sample.pdf");

        assertNotNull(doc);
        assertEquals("sample.pdf", doc.getDocumentName());
        assertTrue(doc.getRawText().contains("Medical Guidelines"));
        assertTrue(doc.getExtractionConfidence() >= 0.85);
        assertTrue(doc.getTokenCount() > 5);
    }

    @Test
    void testImageOnlyPdfRejection() throws Exception {
        byte[] emptyPdfBytes = createEmptyPdfPage();
        assertThrows(DocumentParsingException.class, () ->
                formatParserService.parseFormat(new ByteArrayInputStream(emptyPdfBytes), "empty.pdf"));
    }

    private byte[] createSamplePdf(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text.replace("\n", " "));
                contentStream.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] createEmptyPdfPage() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            document.save(out);
            return out.toByteArray();
        }
    }
}

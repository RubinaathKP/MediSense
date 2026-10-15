package com.medisense.ingestion;

import com.medisense.domain.repository.ManualReviewQueueRepository;
import com.medisense.ingestion.exception.DocumentQualityException;
import com.medisense.ingestion.model.IngestionResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionPipelineTest {

    @Mock
    private ManualReviewQueueRepository manualReviewQueueRepository;

    private DocumentIngestionPipeline pipeline;

    @BeforeEach
    void setUp() {
        FormatParserService formatParser = new FormatParserService();
        QualityGate1Service qualityGate = new QualityGate1Service(manualReviewQueueRepository);
        TextCleanerService cleaner = new TextCleanerService();
        StructureExtractorService extractor = new StructureExtractorService();
        PrescriptiveContentDetectorService detector = new PrescriptiveContentDetectorService();

        pipeline = new DocumentIngestionPipeline(formatParser, qualityGate, cleaner, extractor, detector);
    }

    @Test
    void testEndToEndPipelineHappyPath() throws Exception {
        String content = "Chapter 1 Clinical Guidelines for Lipid Management\n" +
                "1.1 Overview of Cardiovascular Risk Factors\n" +
                "Elevated low-density lipoprotein levels correlate with vascular risk.\n".repeat(70);

        byte[] pdfBytes = createSamplePdf(content);

        IngestionResult result = pipeline.runPipeline(new ByteArrayInputStream(pdfBytes), "clinical_guideline.pdf");

        assertNotNull(result);
        assertEquals("clinical_guideline.pdf", result.getDocumentName());
        assertFalse(result.getSafeNodes().isEmpty());
    }

    @Test
    void testEndToEndPipelineQualityRejection() throws Exception {
        String shortText = "Chapter 1 Short Guideline Document\n" +
                "Elevated blood pressure is a significant risk factor for cardiovascular disease.\n".repeat(5);
        byte[] pdfBytes = createSamplePdf(shortText);

        assertThrows(DocumentQualityException.class, () ->
                pipeline.runPipeline(new ByteArrayInputStream(pdfBytes), "short_doc.pdf"));
    }

    private byte[] createSamplePdf(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(30, 700);

                String[] lines = text.split("\n");
                for (String line : lines) {
                    if (!line.isBlank()) {
                        contentStream.showText(line.trim());
                        contentStream.newLineAtOffset(0, -12);
                    }
                }
                contentStream.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}

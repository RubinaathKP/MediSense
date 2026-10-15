package com.medisense.ingestion;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
public class PdfParserService {

    private static final Logger log = LoggerFactory.getLogger(PdfParserService.class);

    public String extractTextFromPdf(InputStream inputStream) {
        log.info("Extracting layout-aware text from uploaded PDF document.");
        try {
            byte[] bytes = inputStream.readAllBytes();
            try (PDDocument document = PDDocument.load(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(document);
                log.info("Successfully extracted {} characters from PDF.", text.length());
                return text;
            }
        } catch (Exception e) {
            log.error("Failed to parse PDF document: {}", e.getMessage(), e);
            throw new RuntimeException("Error parsing PDF document", e);
        }
    }
}

package com.medisense.ingestion;

import net.sourceforge.tess4j.Tesseract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.File;

@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);
    private final Tesseract tesseract;

    public OcrService(Tesseract tesseract) {
        this.tesseract = tesseract;
    }

    public String performOcr(File imageFile) {
        log.info("Performing OCR on image file: {}", imageFile.getName());
        try {
            String text = tesseract.doOCR(imageFile);
            log.info("OCR completed successfully for file: {}, extracted {} characters.", imageFile.getName(), text.length());
            return text;
        } catch (Exception e) {
            log.error("Tesseract OCR execution failed for file {}: {}", imageFile.getName(), e.getMessage(), e);
            throw new RuntimeException("OCR extraction failed", e);
        }
    }
}

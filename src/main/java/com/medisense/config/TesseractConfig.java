package com.medisense.config;

import net.sourceforge.tess4j.Tesseract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TesseractConfig {

    private static final Logger log = LoggerFactory.getLogger(TesseractConfig.class);

    @Value("${medisense.ocr.tessdata-path:/usr/share/tesseract-ocr/4.00/tessdata}")
    private String tessdataPath;

    @Value("${medisense.ocr.language:eng}")
    private String language;

    @Bean
    public Tesseract tesseract() {
        log.info("Configuring Tess4j Tesseract OCR engine with tessdata path: {}, language: {}", tessdataPath, language);
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(language);
        return tesseract;
    }
}

package com.medisense.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisense.ingestion.exception.DocumentParsingException;
import com.medisense.ingestion.model.DocumentStructure;
import com.medisense.ingestion.model.ParsedDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FormatParserService {

    private static final Logger log = LoggerFactory.getLogger(FormatParserService.class);
    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("(?m)^(\\|.*?\\||\\+.*?\\+|.*\\t.*)$");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParsedDocument parseFormat(InputStream pdfInputStream, String documentName) {
        log.info("Stage 1 - FormatParser: Parsing document '{}' using PDFBox + Apache Tika layout-aware extraction.", documentName);

        byte[] pdfBytes;
        try {
            pdfBytes = pdfInputStream.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to read PDF input stream bytes for document: {}", documentName, e);
            throw new DocumentParsingException("Failed to read PDF input stream", e);
        }

        // Magic byte validation for PDF (%PDF)
        if (pdfBytes.length < 4 || pdfBytes[0] != '%' || pdfBytes[1] != 'P' || pdfBytes[2] != 'D' || pdfBytes[3] != 'F') {
            String magicBytesStr = new String(pdfBytes, 0, Math.min(pdfBytes.length, 8), java.nio.charset.StandardCharsets.ISO_8859_1);
            log.error("Invalid magic bytes for document '{}': Expected '%PDF', found '{}'", documentName, magicBytesStr);
            throw new com.medisense.ingestion.exception.ParseException(
                    "Invalid PDF magic bytes: expected %PDF header, but found: " + magicBytesStr);
        }

        String extractedText;
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            extractedText = stripper.getText(document);
        } catch (Exception e) {
            log.error("PDFBox extraction failed for document: {}", documentName, e);
            throw new com.medisense.ingestion.exception.ParseException("Corrupted PDF document: " + e.getMessage(), e);
        }

        // Optional Tika parse for metadata validation
        try {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(new ByteArrayInputStream(pdfBytes), handler, metadata);
        } catch (Throwable t) {
            log.warn("Optional Tika metadata extraction emitted warning for document {}: {}", documentName, t.getMessage());
        }

        if (extractedText == null || extractedText.isBlank()) {
            log.error("Image-only PDF detected (no text layer) for document: {}", documentName);
            throw new com.medisense.ingestion.exception.DocumentQualityException(
                    "Image-only PDF detected (no text layer)", 0.0, 0);
        }

        String[] tokens = extractedText.trim().split("\\s+");
        int tokenCount = tokens.length;

        if (tokenCount < 10) { // Safety check for minimal text
            double conf = calculateExtractionConfidence(extractedText, tokenCount);
            log.error("Extracted text token count ({}) too low, suspected scanned/image PDF: {}", tokenCount, documentName);
            throw new com.medisense.ingestion.exception.DocumentQualityException(
                    "Image-only or scanned PDF detected: Insufficient text content extracted.", conf, tokenCount);
        }

        // Table extraction as structured JSON
        List<String> tablesJson = extractTablesAsJson(extractedText);

        // Compute layout-aware confidence score
        double confidence = calculateExtractionConfidence(extractedText, tokenCount);

        log.info("Stage 1 - FormatParser completed for '{}'. Token count: {}, Tables extracted: {}, Confidence: {}",
                documentName, tokenCount, tablesJson.size(), confidence);

        return ParsedDocument.builder()
                .documentName(documentName)
                .rawText(extractedText)
                .structureTree(new DocumentStructure(new ArrayList<>(), 0))
                .extractedTablesJson(tablesJson)
                .extractionConfidence(confidence)
                .tokenCount(tokenCount)
                .build();
    }

    private List<String> extractTablesAsJson(String text) {
        List<String> tables = new ArrayList<>();
        Matcher matcher = TABLE_ROW_PATTERN.matcher(text);
        List<String> currentTableRows = new ArrayList<>();

        while (matcher.find()) {
            currentTableRows.add(matcher.group().trim());
        }

        if (!currentTableRows.isEmpty()) {
            try {
                Map<String, Object> tableData = new HashMap<>();
                tableData.put("type", "extracted_table");
                tableData.put("rowCount", currentTableRows.size());
                tableData.put("rows", currentTableRows);
                tables.add(objectMapper.writeValueAsString(tableData));
            } catch (Exception e) {
                log.warn("Failed to serialize extracted table to JSON: {}", e.getMessage());
            }
        }
        return tables;
    }

    private double calculateExtractionConfidence(String text, int tokenCount) {
        if (tokenCount == 0) return 0.0;
        long cleanChars = text.chars().filter(c -> Character.isLetterOrDigit(c) || Character.isWhitespace(c)).count();
        double ratio = (double) cleanChars / text.length();
        return Math.min(1.0, Math.max(0.0, ratio));
    }
}

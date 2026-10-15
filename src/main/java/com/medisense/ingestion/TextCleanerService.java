package com.medisense.ingestion;

import com.medisense.ingestion.model.CleanedDocument;
import com.medisense.ingestion.model.ParsedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TextCleanerService {

    private static final Logger log = LoggerFactory.getLogger(TextCleanerService.class);

    private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile("(?i)^(\\s*page\\s+\\d+\\s*(of\\s*\\d+)?|\\d+\\s*/\\s*\\d+|\\s*-\\s*\\d+\\s*-\\s*)$", Pattern.MULTILINE);
    private static final Pattern HEADER_FOOTER_PATTERN = Pattern.compile(
            "(?i)^\\s*(icmr clinical practice guidelines|who guidance documents|confidential|draft|all rights reserved|header text|footer text|header|footer).*$",
            Pattern.MULTILINE
    );
    private static final Pattern HYPHENATED_WORD_PATTERN = Pattern.compile("(\\b[a-zA-Z]+)-\\r?\\n([a-zA-Z]+\\b)");
    private static final Pattern SECTION_HEADING_PATTERN = Pattern.compile("(?m)^(Chapter\\s+\\d+|\\d+\\.\\d+(\\.\\d+)?\\s+[A-Z].*)$");

    private final Map<String, String> abbreviationMap = new HashMap<>();

    public TextCleanerService() {
        // Default configurable medical abbreviation map
        abbreviationMap.put("NCD", "Non-Communicable Disease");
        abbreviationMap.put("BP", "Blood Pressure");
        abbreviationMap.put("DM", "Diabetes Mellitus");
        abbreviationMap.put("HTN", "Hypertension");
        abbreviationMap.put("LDL", "Low-Density Lipoprotein");
        abbreviationMap.put("HDL", "High-Density Lipoprotein");
        abbreviationMap.put("HbA1c", "Glycated Hemoglobin");
        abbreviationMap.put("WHO", "World Health Organization");
        abbreviationMap.put("ICMR", "Indian Council of Medical Research");
        abbreviationMap.put("ECG", "Electrocardiogram");
    }

    public CleanedDocument cleanText(ParsedDocument document) {
        log.info("Stage 3 - TextCleaner: Cleaning document '{}'.", document.getDocumentName());

        String raw = document.getRawText();

        // 1. Remove Headers, Footers, and Page Numbers
        String textWithoutPageNumbers = PAGE_NUMBER_PATTERN.matcher(raw).replaceAll("");
        String textWithoutHeadersFooters = HEADER_FOOTER_PATTERN.matcher(textWithoutPageNumbers).replaceAll("");

        // 2. Fix Line-Break Hyphenation
        String unhyphenated = HYPHENATED_WORD_PATTERN.matcher(textWithoutHeadersFooters).replaceAll("$1$2");

        // 3. Extract & Preserve Section Headings
        List<String> sectionHeadings = new ArrayList<>();
        Matcher headingMatcher = SECTION_HEADING_PATTERN.matcher(unhyphenated);
        while (headingMatcher.find()) {
            sectionHeadings.add(headingMatcher.group().trim());
        }

        // 4. Normalize Whitespace (replace multiple consecutive spaces with a single space)
        String normalizedWhitespace = unhyphenated.replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();

        // 5. Detect Medical Abbreviations as Metadata (mapping is metadata, text preserved unchanged)
        Map<String, String> detectedAbbreviations = new HashMap<>();
        for (Map.Entry<String, String> entry : abbreviationMap.entrySet()) {
            String regex = "\\b" + Pattern.quote(entry.getKey()) + "\\b";
            if (Pattern.compile(regex).matcher(normalizedWhitespace).find()) {
                detectedAbbreviations.put(entry.getKey(), entry.getValue());
            }
        }

        // Split into cleaned paragraphs
        String[] rawParagraphs = normalizedWhitespace.split("\\n\\n+");
        List<String> cleanedParagraphs = new ArrayList<>();
        for (String p : rawParagraphs) {
            String trimmed = p.trim();
            if (!trimmed.isBlank()) {
                cleanedParagraphs.add(trimmed);
            }
        }

        log.info("Stage 3 - TextCleaner completed for '{}'. Extracted {} headings, {} cleaned paragraphs, {} mapped abbreviations.",
                document.getDocumentName(), sectionHeadings.size(), cleanedParagraphs.size(), detectedAbbreviations.size());

        return CleanedDocument.builder()
                .documentName(document.getDocumentName())
                .rawCleanedText(normalizedWhitespace)
                .sectionHeadings(sectionHeadings)
                .cleanedParagraphs(cleanedParagraphs)
                .mappedAbbreviations(detectedAbbreviations)
                .build();
    }

    private String expandAbbreviations(String text) {
        String result = text;
        for (Map.Entry<String, String> entry : abbreviationMap.entrySet()) {
            // Replace whole word abbreviations (e.g. \bNCD\b)
            String regex = "\\b" + Pattern.quote(entry.getKey()) + "\\b";
            result = result.replaceAll(regex, entry.getValue());
        }
        return result;
    }

    public void addAbbreviationMapping(String abbreviation, String fullForm) {
        abbreviationMap.put(abbreviation, fullForm);
    }
}

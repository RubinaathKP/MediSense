package com.medisense.ingestion.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CleanedDocument {
    private String documentName;
    private String rawCleanedText;
    private List<String> sectionHeadings = new ArrayList<>();
    private List<String> cleanedParagraphs = new ArrayList<>();

    public CleanedDocument() {}

    public CleanedDocument(String documentName, String rawCleanedText, List<String> sectionHeadings, List<String> cleanedParagraphs) {
        this.documentName = documentName;
        this.rawCleanedText = rawCleanedText;
        this.sectionHeadings = sectionHeadings != null ? sectionHeadings : new ArrayList<>();
        this.cleanedParagraphs = cleanedParagraphs != null ? cleanedParagraphs : new ArrayList<>();
    }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public String getRawCleanedText() { return rawCleanedText; }
    public String getCleanedText() { return rawCleanedText; }
    public void setRawCleanedText(String rawCleanedText) { this.rawCleanedText = rawCleanedText; }

    public List<String> getSectionHeadings() { return sectionHeadings; }
    public void setSectionHeadings(List<String> sectionHeadings) { this.sectionHeadings = sectionHeadings; }

    private Map<String, String> mappedAbbreviations = new HashMap<>();

    public List<String> getCleanedParagraphs() { return cleanedParagraphs; }
    public void setCleanedParagraphs(List<String> cleanedParagraphs) { this.cleanedParagraphs = cleanedParagraphs; }

    public Map<String, String> getMappedAbbreviations() { return mappedAbbreviations; }
    public void setMappedAbbreviations(Map<String, String> mappedAbbreviations) { 
        this.mappedAbbreviations = mappedAbbreviations != null ? mappedAbbreviations : new HashMap<>(); 
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String documentName;
        private String rawCleanedText;
        private List<String> sectionHeadings = new ArrayList<>();
        private List<String> cleanedParagraphs = new ArrayList<>();
        private Map<String, String> mappedAbbreviations = new HashMap<>();

        public Builder documentName(String documentName) { this.documentName = documentName; return this; }
        public Builder rawCleanedText(String rawCleanedText) { this.rawCleanedText = rawCleanedText; return this; }
        public Builder sectionHeadings(List<String> sectionHeadings) { this.sectionHeadings = sectionHeadings; return this; }
        public Builder cleanedParagraphs(List<String> cleanedParagraphs) { this.cleanedParagraphs = cleanedParagraphs; return this; }
        public Builder mappedAbbreviations(Map<String, String> mappedAbbreviations) { 
            this.mappedAbbreviations = mappedAbbreviations; 
            return this; 
        }

        public CleanedDocument build() {
            CleanedDocument doc = new CleanedDocument(documentName, rawCleanedText, sectionHeadings, cleanedParagraphs);
            doc.setMappedAbbreviations(mappedAbbreviations);
            return doc;
        }
    }
}

package com.medisense.ingestion.model;

import java.util.ArrayList;
import java.util.List;

public class ParsedDocument {
    private String documentName;
    private String rawText;
    private DocumentStructure structureTree;
    private List<String> extractedTablesJson = new ArrayList<>();
    private double extractionConfidence;
    private int tokenCount;

    public ParsedDocument() {}

    public ParsedDocument(String documentName, String rawText, DocumentStructure structureTree,
                          List<String> extractedTablesJson, double extractionConfidence, int tokenCount) {
        this.documentName = documentName;
        this.rawText = rawText;
        this.structureTree = structureTree;
        this.extractedTablesJson = extractedTablesJson != null ? extractedTablesJson : new ArrayList<>();
        this.extractionConfidence = extractionConfidence;
        this.tokenCount = tokenCount;
    }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public DocumentStructure getStructureTree() { return structureTree; }
    public void setStructureTree(DocumentStructure structureTree) { this.structureTree = structureTree; }

    public List<String> getExtractedTablesJson() { return extractedTablesJson; }
    public void setExtractedTablesJson(List<String> extractedTablesJson) { this.extractedTablesJson = extractedTablesJson; }

    public double getExtractionConfidence() { return extractionConfidence; }
    public void setExtractionConfidence(double extractionConfidence) { this.extractionConfidence = extractionConfidence; }

    public int getTokenCount() { return tokenCount; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }

    private java.util.Map<String, Object> metadata = new java.util.HashMap<>();

    public java.util.Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(java.util.Map<String, Object> metadata) { this.metadata = metadata; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String documentName;
        private String rawText;
        private DocumentStructure structureTree;
        private List<String> extractedTablesJson = new ArrayList<>();
        private double extractionConfidence;
        private int tokenCount;
        private java.util.Map<String, Object> metadata = new java.util.HashMap<>();

        public Builder documentName(String documentName) { this.documentName = documentName; return this; }
        public Builder rawText(String rawText) { this.rawText = rawText; return this; }
        public Builder structureTree(DocumentStructure structureTree) { this.structureTree = structureTree; return this; }
        public Builder extractedTablesJson(List<String> extractedTablesJson) { this.extractedTablesJson = extractedTablesJson; return this; }
        public Builder extractionConfidence(double extractionConfidence) { this.extractionConfidence = extractionConfidence; return this; }
        public Builder tokenCount(int tokenCount) { this.tokenCount = tokenCount; return this; }
        public Builder metadata(java.util.Map<?, ?> metadata) {
            this.metadata = new java.util.HashMap<>();
            if (metadata != null) {
                metadata.forEach((k, v) -> this.metadata.put(String.valueOf(k), v));
            }
            return this;
        }

        public ParsedDocument build() {
            ParsedDocument doc = new ParsedDocument(documentName, rawText, structureTree, extractedTablesJson, extractionConfidence, tokenCount);
            doc.setMetadata(this.metadata);
            return doc;
        }
    }
}

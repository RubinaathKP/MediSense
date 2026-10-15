package com.medisense.rag.chunking.model;

import com.medisense.domain.enums.EvidenceBasis;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChildChunk {
    private UUID chunkId;
    private UUID parentChunkId;
    private UUID docId;
    private String sourceName;
    private int sourceYear;
    private EvidenceBasis evidenceBasis;
    private String sourceType;
    private String sectionHeading;
    private List<String> domainTags = new ArrayList<>();
    private String chunkText;
    private int tokenCount;
    private int chunkIndex;
    private String language = "en";
    private boolean isTable = false;
    private List<String> tableHeader = new ArrayList<>();

    public ChildChunk() {}

    public ChildChunk(UUID chunkId, UUID parentChunkId, UUID docId, String sourceName, int sourceYear, EvidenceBasis evidenceBasis, String sourceType, String sectionHeading, List<String> domainTags, String chunkText, int tokenCount, int chunkIndex, String language, boolean isTable, List<String> tableHeader) {
        this.chunkId = chunkId;
        this.parentChunkId = parentChunkId;
        this.docId = docId;
        this.sourceName = sourceName;
        this.sourceYear = sourceYear;
        this.evidenceBasis = evidenceBasis;
        this.sourceType = sourceType;
        this.sectionHeading = sectionHeading;
        this.domainTags = domainTags != null ? domainTags : new ArrayList<>();
        this.chunkText = chunkText;
        this.tokenCount = tokenCount;
        this.chunkIndex = chunkIndex;
        this.language = language != null ? language : "en";
        this.isTable = isTable;
        this.tableHeader = tableHeader != null ? tableHeader : new ArrayList<>();
    }

    public UUID getChunkId() { return chunkId; }
    public void setChunkId(UUID chunkId) { this.chunkId = chunkId; }

    public UUID getParentChunkId() { return parentChunkId; }
    public void setParentChunkId(UUID parentChunkId) { this.parentChunkId = parentChunkId; }

    public UUID getDocId() { return docId; }
    public void setDocId(UUID docId) { this.docId = docId; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public int getSourceYear() { return sourceYear; }
    public void setSourceYear(int sourceYear) { this.sourceYear = sourceYear; }

    public EvidenceBasis getEvidenceBasis() { return evidenceBasis; }
    public void setEvidenceBasis(EvidenceBasis evidenceBasis) { this.evidenceBasis = evidenceBasis; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getSectionHeading() { return sectionHeading; }
    public void setSectionHeading(String sectionHeading) { this.sectionHeading = sectionHeading; }

    public List<String> getDomainTags() { return domainTags; }
    public void setDomainTags(List<String> domainTags) { this.domainTags = domainTags; }

    public String getChunkText() { return chunkText; }
    public void setChunkText(String chunkText) { this.chunkText = chunkText; }

    public int getTokenCount() { return tokenCount; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public boolean isTable() { return isTable; }
    public void setTable(boolean isTable) { this.isTable = isTable; }

    public List<String> getTableHeader() { return tableHeader; }
    public void setTableHeader(List<String> tableHeader) { this.tableHeader = tableHeader; }

    public UUID id() { return chunkId; }
    public String content() { return chunkText; }
    public UUID parentChunkId() { return parentChunkId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID chunkId;
        private UUID parentChunkId;
        private UUID docId;
        private String sourceName;
        private int sourceYear;
        private EvidenceBasis evidenceBasis;
        private String sourceType;
        private String sectionHeading;
        private List<String> domainTags = new ArrayList<>();
        private String chunkText;
        private int tokenCount;
        private int chunkIndex;
        private String language = "en";
        private boolean isTable = false;
        private List<String> tableHeader = new ArrayList<>();

        public Builder chunkId(UUID chunkId) { this.chunkId = chunkId; return this; }
        public Builder parentChunkId(UUID parentChunkId) { this.parentChunkId = parentChunkId; return this; }
        public Builder docId(UUID docId) { this.docId = docId; return this; }
        public Builder sourceName(String sourceName) { this.sourceName = sourceName; return this; }
        public Builder sourceYear(int sourceYear) { this.sourceYear = sourceYear; return this; }
        public Builder evidenceBasis(EvidenceBasis evidenceBasis) { this.evidenceBasis = evidenceBasis; return this; }
        public Builder sourceType(String sourceType) { this.sourceType = sourceType; return this; }
        public Builder sectionHeading(String sectionHeading) { this.sectionHeading = sectionHeading; return this; }
        public Builder domainTags(List<String> domainTags) { this.domainTags = domainTags; return this; }
        public Builder chunkText(String chunkText) { this.chunkText = chunkText; return this; }
        public Builder tokenCount(int tokenCount) { this.tokenCount = tokenCount; return this; }
        public Builder chunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Builder isTable(boolean isTable) { this.isTable = isTable; return this; }
        public Builder tableHeader(List<String> tableHeader) { this.tableHeader = tableHeader; return this; }

        public ChildChunk build() {
            return new ChildChunk(chunkId, parentChunkId, docId, sourceName, sourceYear, evidenceBasis, sourceType, sectionHeading, domainTags, chunkText, tokenCount, chunkIndex, language, isTable, tableHeader);
        }
    }
}

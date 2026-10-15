package com.medisense.rag.chunking.model;

import java.util.UUID;

public class ParentChunk {
    private UUID parentChunkId;
    private UUID docId;
    private String sourceName;
    private String sectionHeading;
    private String parentText;
    private int tokenCount;
    private int sectionIndex;

    public ParentChunk() {}

    public ParentChunk(UUID parentChunkId, UUID docId, String sourceName, String sectionHeading, String parentText, int tokenCount, int sectionIndex) {
        this.parentChunkId = parentChunkId;
        this.docId = docId;
        this.sourceName = sourceName;
        this.sectionHeading = sectionHeading;
        this.parentText = parentText;
        this.tokenCount = tokenCount;
        this.sectionIndex = sectionIndex;
    }

    public UUID getParentChunkId() { return parentChunkId; }
    public void setParentChunkId(UUID parentChunkId) { this.parentChunkId = parentChunkId; }

    public UUID getDocId() { return docId; }
    public void setDocId(UUID docId) { this.docId = docId; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getSectionHeading() { return sectionHeading; }
    public void setSectionHeading(String sectionHeading) { this.sectionHeading = sectionHeading; }

    public String getParentText() { return parentText; }
    public void setParentText(String parentText) { this.parentText = parentText; }

    public int getTokenCount() { return tokenCount; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }

    public int getSectionIndex() { return sectionIndex; }
    public void setSectionIndex(int sectionIndex) { this.sectionIndex = sectionIndex; }

    public UUID id() { return parentChunkId; }
    public String content() { return parentText; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID parentChunkId;
        private UUID docId;
        private String sourceName;
        private String sectionHeading;
        private String parentText;
        private int tokenCount;
        private int sectionIndex;

        public Builder parentChunkId(UUID parentChunkId) { this.parentChunkId = parentChunkId; return this; }
        public Builder docId(UUID docId) { this.docId = docId; return this; }
        public Builder sourceName(String sourceName) { this.sourceName = sourceName; return this; }
        public Builder sectionHeading(String sectionHeading) { this.sectionHeading = sectionHeading; return this; }
        public Builder parentText(String parentText) { this.parentText = parentText; return this; }
        public Builder tokenCount(int tokenCount) { this.tokenCount = tokenCount; return this; }
        public Builder sectionIndex(int sectionIndex) { this.sectionIndex = sectionIndex; return this; }

        public ParentChunk build() {
            return new ParentChunk(parentChunkId, docId, sourceName, sectionHeading, parentText, tokenCount, sectionIndex);
        }
    }
}

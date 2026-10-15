package com.medisense.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "parent_chunks")
public class ParentChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private SourceDocumentEntity document;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public ParentChunkEntity() {}

    public ParentChunkEntity(UUID id, SourceDocumentEntity document, Integer chunkIndex, String content, String metadata, OffsetDateTime createdAt) {
        this.id = id;
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public SourceDocumentEntity getDocument() { return document; }
    public void setDocument(SourceDocumentEntity document) { this.document = document; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private SourceDocumentEntity document;
        private Integer chunkIndex;
        private String content;
        private String metadata;
        private OffsetDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder document(SourceDocumentEntity document) { this.document = document; return this; }
        public Builder chunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder metadata(String metadata) { this.metadata = metadata; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ParentChunkEntity build() {
            return new ParentChunkEntity(id, document, chunkIndex, content, metadata, createdAt);
        }
    }
}

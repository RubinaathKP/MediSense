package com.medisense.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "child_chunks")
public class ChildChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_chunk_id", nullable = false)
    private ParentChunkEntity parentChunk;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "vector(768)")
    private String embedding;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public ChildChunkEntity() {}

    public ChildChunkEntity(UUID id, ParentChunkEntity parentChunk, Integer chunkIndex, String content, String embedding, String metadata, Boolean isActive, OffsetDateTime createdAt) {
        this.id = id;
        this.parentChunk = parentChunk;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.embedding = embedding;
        this.metadata = metadata;
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ParentChunkEntity getParentChunk() { return parentChunk; }
    public void setParentChunk(ParentChunkEntity parentChunk) { this.parentChunk = parentChunk; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private ParentChunkEntity parentChunk;
        private Integer chunkIndex;
        private String content;
        private String embedding;
        private String metadata;
        private Boolean isActive = true;
        private OffsetDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder parentChunk(ParentChunkEntity parentChunk) { this.parentChunk = parentChunk; return this; }
        public Builder chunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder embedding(String embedding) { this.embedding = embedding; return this; }
        public Builder metadata(String metadata) { this.metadata = metadata; return this; }
        public Builder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ChildChunkEntity build() {
            return new ChildChunkEntity(id, parentChunk, chunkIndex, content, embedding, metadata, isActive, createdAt);
        }
    }
}

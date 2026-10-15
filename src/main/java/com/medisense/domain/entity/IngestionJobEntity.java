package com.medisense.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ingestion_jobs")
public class IngestionJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public IngestionJobEntity() {}

    public IngestionJobEntity(UUID id, String documentName, String checksum, String status, Integer progressPercentage, String errorMessage, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.documentName = documentName;
        this.checksum = checksum;
        this.status = status;
        this.progressPercentage = progressPercentage;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String documentName;
        private String checksum;
        private String status;
        private Integer progressPercentage = 0;
        private String errorMessage;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder documentName(String documentName) { this.documentName = documentName; return this; }
        public Builder checksum(String checksum) { this.checksum = checksum; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder progressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public IngestionJobEntity build() {
            return new IngestionJobEntity(id, documentName, checksum, status, progressPercentage, errorMessage, createdAt, updatedAt);
        }
    }
}

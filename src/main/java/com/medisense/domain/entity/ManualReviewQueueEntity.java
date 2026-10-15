package com.medisense.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "manual_review_queue")
public class ManualReviewQueueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Column(name = "rejection_reason", columnDefinition = "TEXT", nullable = false)
    private String rejectionReason;

    @Column(name = "rejection_stage", nullable = false, length = 50)
    private String rejectionStage;

    @Column(name = "extraction_confidence", precision = 5, scale = 4)
    private BigDecimal extractionConfidence;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public ManualReviewQueueEntity() {}

    public ManualReviewQueueEntity(UUID id, String documentName, String rejectionReason, String rejectionStage, BigDecimal extractionConfidence, Integer tokenCount, OffsetDateTime createdAt) {
        this.id = id;
        this.documentName = documentName;
        this.rejectionReason = rejectionReason;
        this.rejectionStage = rejectionStage;
        this.extractionConfidence = extractionConfidence;
        this.tokenCount = tokenCount;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getRejectionStage() { return rejectionStage; }
    public void setRejectionStage(String rejectionStage) { this.rejectionStage = rejectionStage; }

    public BigDecimal getExtractionConfidence() { return extractionConfidence; }
    public void setExtractionConfidence(BigDecimal extractionConfidence) { this.extractionConfidence = extractionConfidence; }

    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String documentName;
        private String rejectionReason;
        private String rejectionStage;
        private BigDecimal extractionConfidence;
        private Integer tokenCount;
        private OffsetDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder documentName(String documentName) { this.documentName = documentName; return this; }
        public Builder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }
        public Builder rejectionStage(String rejectionStage) { this.rejectionStage = rejectionStage; return this; }
        public Builder extractionConfidence(BigDecimal extractionConfidence) { this.extractionConfidence = extractionConfidence; return this; }
        public Builder tokenCount(Integer tokenCount) { this.tokenCount = tokenCount; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ManualReviewQueueEntity build() {
            return new ManualReviewQueueEntity(id, documentName, rejectionReason, rejectionStage, extractionConfidence, tokenCount, createdAt);
        }
    }
}

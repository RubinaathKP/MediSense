package com.medisense.domain.entity;

import com.medisense.domain.enums.FlagStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "lab_report_values")
public class LabReportValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private SourceDocumentEntity document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private SessionEntity session;

    @Column(name = "test_name", nullable = false)
    private String testName;

    @Column(name = "extracted_value", nullable = false, length = 100)
    private String extractedValue;

    @Column(name = "normalized_value", precision = 12, scale = 4)
    private BigDecimal normalizedValue;

    @Column(length = 50)
    private String unit;

    @Column(name = "reference_range", length = 100)
    private String referenceRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_status", nullable = false, length = 20)
    private FlagStatus flagStatus;

    @Column(name = "extraction_confidence", nullable = false, precision = 5, scale = 4)
    private BigDecimal extractionConfidence;

    @Column(name = "requires_user_confirmation")
    private Boolean requiresUserConfirmation = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public LabReportValueEntity() {}

    public LabReportValueEntity(UUID id, SourceDocumentEntity document, SessionEntity session, String testName, String extractedValue, BigDecimal normalizedValue, String unit, String referenceRange, FlagStatus flagStatus, BigDecimal extractionConfidence, Boolean requiresUserConfirmation, OffsetDateTime createdAt) {
        this.id = id;
        this.document = document;
        this.session = session;
        this.testName = testName;
        this.extractedValue = extractedValue;
        this.normalizedValue = normalizedValue;
        this.unit = unit;
        this.referenceRange = referenceRange;
        this.flagStatus = flagStatus;
        this.extractionConfidence = extractionConfidence;
        this.requiresUserConfirmation = requiresUserConfirmation != null ? requiresUserConfirmation : false;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public SourceDocumentEntity getDocument() { return document; }
    public void setDocument(SourceDocumentEntity document) { this.document = document; }

    public SessionEntity getSession() { return session; }
    public void setSession(SessionEntity session) { this.session = session; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public String getExtractedValue() { return extractedValue; }
    public void setExtractedValue(String extractedValue) { this.extractedValue = extractedValue; }

    public BigDecimal getNormalizedValue() { return normalizedValue; }
    public void setNormalizedValue(BigDecimal normalizedValue) { this.normalizedValue = normalizedValue; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getReferenceRange() { return referenceRange; }
    public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }

    public FlagStatus getFlagStatus() { return flagStatus; }
    public void setFlagStatus(FlagStatus flagStatus) { this.flagStatus = flagStatus; }

    public BigDecimal getExtractionConfidence() { return extractionConfidence; }
    public void setExtractionConfidence(BigDecimal extractionConfidence) { this.extractionConfidence = extractionConfidence; }

    public Boolean getRequiresUserConfirmation() { return requiresUserConfirmation; }
    public void setRequiresUserConfirmation(Boolean requiresUserConfirmation) { this.requiresUserConfirmation = requiresUserConfirmation; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private SourceDocumentEntity document;
        private SessionEntity session;
        private String testName;
        private String extractedValue;
        private BigDecimal normalizedValue;
        private String unit;
        private String referenceRange;
        private FlagStatus flagStatus;
        private BigDecimal extractionConfidence;
        private Boolean requiresUserConfirmation = false;
        private OffsetDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder document(SourceDocumentEntity document) { this.document = document; return this; }
        public Builder session(SessionEntity session) { this.session = session; return this; }
        public Builder testName(String testName) { this.testName = testName; return this; }
        public Builder extractedValue(String extractedValue) { this.extractedValue = extractedValue; return this; }
        public Builder normalizedValue(BigDecimal normalizedValue) { this.normalizedValue = normalizedValue; return this; }
        public Builder unit(String unit) { this.unit = unit; return this; }
        public Builder referenceRange(String referenceRange) { this.referenceRange = referenceRange; return this; }
        public Builder flagStatus(FlagStatus flagStatus) { this.flagStatus = flagStatus; return this; }
        public Builder extractionConfidence(BigDecimal extractionConfidence) { this.extractionConfidence = extractionConfidence; return this; }
        public Builder requiresUserConfirmation(Boolean requiresUserConfirmation) { this.requiresUserConfirmation = requiresUserConfirmation; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public LabReportValueEntity build() {
            return new LabReportValueEntity(id, document, session, testName, extractedValue, normalizedValue, unit, referenceRange, flagStatus, extractionConfidence, requiresUserConfirmation, createdAt);
        }
    }
}

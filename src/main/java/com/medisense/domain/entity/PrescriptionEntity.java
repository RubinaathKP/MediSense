package com.medisense.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "prescriptions")
public class PrescriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private SourceDocumentEntity document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private SessionEntity session;

    @Column(name = "brand_name", nullable = false)
    private String brandName;

    @Column(name = "generic_name", nullable = false)
    private String genericName;

    @Column(name = "drug_class", nullable = false)
    private String drugClass;

    @Column(nullable = false, length = 100)
    private String dosage;

    @Column(nullable = false, length = 100)
    private String frequency;

    @Column(length = 100)
    private String timing;

    @Column(length = 100)
    private String duration;

    @Column(name = "prescribing_specialty", length = 150)
    private String prescribingSpecialty;

    @Column(name = "extraction_confidence", nullable = false, precision = 5, scale = 4)
    private BigDecimal extractionConfidence;

    @Column(name = "requires_user_confirmation")
    private Boolean requiresUserConfirmation = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public PrescriptionEntity() {}

    public PrescriptionEntity(UUID id, SourceDocumentEntity document, SessionEntity session, String brandName, String genericName, String drugClass, String dosage, String frequency, String timing, String duration, String prescribingSpecialty, BigDecimal extractionConfidence, Boolean requiresUserConfirmation, OffsetDateTime createdAt) {
        this.id = id;
        this.document = document;
        this.session = session;
        this.brandName = brandName;
        this.genericName = genericName;
        this.drugClass = drugClass;
        this.dosage = dosage;
        this.frequency = frequency;
        this.timing = timing;
        this.duration = duration;
        this.prescribingSpecialty = prescribingSpecialty;
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

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getDrugClass() { return drugClass; }
    public void setDrugClass(String drugClass) { this.drugClass = drugClass; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getTiming() { return timing; }
    public void setTiming(String timing) { this.timing = timing; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getPrescribingSpecialty() { return prescribingSpecialty; }
    public void setPrescribingSpecialty(String prescribingSpecialty) { this.prescribingSpecialty = prescribingSpecialty; }

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
        private String brandName;
        private String genericName;
        private String drugClass;
        private String dosage;
        private String frequency;
        private String timing;
        private String duration;
        private String prescribingSpecialty;
        private BigDecimal extractionConfidence;
        private Boolean requiresUserConfirmation = false;
        private OffsetDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder document(SourceDocumentEntity document) { this.document = document; return this; }
        public Builder session(SessionEntity session) { this.session = session; return this; }
        public Builder brandName(String brandName) { this.brandName = brandName; return this; }
        public Builder genericName(String genericName) { this.genericName = genericName; return this; }
        public Builder drugClass(String drugClass) { this.drugClass = drugClass; return this; }
        public Builder dosage(String dosage) { this.dosage = dosage; return this; }
        public Builder frequency(String frequency) { this.frequency = frequency; return this; }
        public Builder timing(String timing) { this.timing = timing; return this; }
        public Builder duration(String duration) { this.duration = duration; return this; }
        public Builder prescribingSpecialty(String prescribingSpecialty) { this.prescribingSpecialty = prescribingSpecialty; return this; }
        public Builder extractionConfidence(BigDecimal extractionConfidence) { this.extractionConfidence = extractionConfidence; return this; }
        public Builder requiresUserConfirmation(Boolean requiresUserConfirmation) { this.requiresUserConfirmation = requiresUserConfirmation; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public PrescriptionEntity build() {
            return new PrescriptionEntity(id, document, session, brandName, genericName, drugClass, dosage, frequency, timing, duration, prescribingSpecialty, extractionConfidence, requiresUserConfirmation, createdAt);
        }
    }
}

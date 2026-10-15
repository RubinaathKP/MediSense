package com.medisense.domain.dto;

import java.math.BigDecimal;

public class PrescriptionDto {
    private String brandName;
    private String genericName;
    private String drugClass;
    private String dosage;
    private String frequency;
    private String timing;
    private String duration;
    private String prescribingSpecialty;
    private BigDecimal extractionConfidence;
    private Boolean requiresUserConfirmation;

    public PrescriptionDto() {}

    public PrescriptionDto(String brandName, String genericName, String drugClass, String dosage, String frequency, String timing, String duration, String prescribingSpecialty, BigDecimal extractionConfidence, Boolean requiresUserConfirmation) {
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
    }

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

    public static Builder builder() { return new Builder(); }

    public static class Builder {
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

        public PrescriptionDto build() {
            return new PrescriptionDto(brandName, genericName, drugClass, dosage, frequency, timing, duration, prescribingSpecialty, extractionConfidence, requiresUserConfirmation);
        }
    }
}

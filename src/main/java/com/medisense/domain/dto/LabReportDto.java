package com.medisense.domain.dto;

import com.medisense.domain.enums.FlagStatus;
import java.math.BigDecimal;

public class LabReportDto {
    private String testName;
    private String extractedValue;
    private BigDecimal normalizedValue;
    private String unit;
    private String referenceRange;
    private FlagStatus flagStatus;
    private BigDecimal extractionConfidence;
    private Boolean requiresUserConfirmation;

    public LabReportDto() {}

    public LabReportDto(String testName, String extractedValue, BigDecimal normalizedValue, String unit, String referenceRange, FlagStatus flagStatus, BigDecimal extractionConfidence, Boolean requiresUserConfirmation) {
        this.testName = testName;
        this.extractedValue = extractedValue;
        this.normalizedValue = normalizedValue;
        this.unit = unit;
        this.referenceRange = referenceRange;
        this.flagStatus = flagStatus;
        this.extractionConfidence = extractionConfidence;
        this.requiresUserConfirmation = requiresUserConfirmation != null ? requiresUserConfirmation : false;
    }

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

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String testName;
        private String extractedValue;
        private BigDecimal normalizedValue;
        private String unit;
        private String referenceRange;
        private FlagStatus flagStatus;
        private BigDecimal extractionConfidence;
        private Boolean requiresUserConfirmation = false;

        public Builder testName(String testName) { this.testName = testName; return this; }
        public Builder extractedValue(String extractedValue) { this.extractedValue = extractedValue; return this; }
        public Builder normalizedValue(BigDecimal normalizedValue) { this.normalizedValue = normalizedValue; return this; }
        public Builder unit(String unit) { this.unit = unit; return this; }
        public Builder referenceRange(String referenceRange) { this.referenceRange = referenceRange; return this; }
        public Builder flagStatus(FlagStatus flagStatus) { this.flagStatus = flagStatus; return this; }
        public Builder extractionConfidence(BigDecimal extractionConfidence) { this.extractionConfidence = extractionConfidence; return this; }
        public Builder requiresUserConfirmation(Boolean requiresUserConfirmation) { this.requiresUserConfirmation = requiresUserConfirmation; return this; }

        public LabReportDto build() {
            return new LabReportDto(testName, extractedValue, normalizedValue, unit, referenceRange, flagStatus, extractionConfidence, requiresUserConfirmation);
        }
    }
}

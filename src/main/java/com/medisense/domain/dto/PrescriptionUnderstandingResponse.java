package com.medisense.domain.dto;

import com.medisense.domain.enums.SafetyDecision;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionUnderstandingResponse {
    private String brandName;
    private String genericName;
    private String drugClass;
    private String mechanismOfActionExplanation;
    private String indicationExplanation;
    private String plainLanguageDosing;
    private SafetyDecision safetyDecision;
    private List<CitationDto> citations = new ArrayList<>();

    public PrescriptionUnderstandingResponse() {}

    public PrescriptionUnderstandingResponse(String brandName, String genericName, String drugClass, String mechanismOfActionExplanation, String indicationExplanation, String plainLanguageDosing, SafetyDecision safetyDecision, List<CitationDto> citations) {
        this.brandName = brandName;
        this.genericName = genericName;
        this.drugClass = drugClass;
        this.mechanismOfActionExplanation = mechanismOfActionExplanation;
        this.indicationExplanation = indicationExplanation;
        this.plainLanguageDosing = plainLanguageDosing;
        this.safetyDecision = safetyDecision;
        this.citations = citations != null ? citations : new ArrayList<>();
    }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getDrugClass() { return drugClass; }
    public void setDrugClass(String drugClass) { this.drugClass = drugClass; }

    public String getMechanismOfActionExplanation() { return mechanismOfActionExplanation; }
    public void setMechanismOfActionExplanation(String mechanismOfActionExplanation) { this.mechanismOfActionExplanation = mechanismOfActionExplanation; }

    public String getIndicationExplanation() { return indicationExplanation; }
    public void setIndicationExplanation(String indicationExplanation) { this.indicationExplanation = indicationExplanation; }

    public String getPlainLanguageDosing() { return plainLanguageDosing; }
    public void setPlainLanguageDosing(String plainLanguageDosing) { this.plainLanguageDosing = plainLanguageDosing; }

    public SafetyDecision getSafetyDecision() { return safetyDecision; }
    public void setSafetyDecision(SafetyDecision safetyDecision) { this.safetyDecision = safetyDecision; }

    public List<CitationDto> getCitations() { return citations; }
    public void setCitations(List<CitationDto> citations) { this.citations = citations; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String brandName;
        private String genericName;
        private String drugClass;
        private String mechanismOfActionExplanation;
        private String indicationExplanation;
        private String plainLanguageDosing;
        private SafetyDecision safetyDecision;
        private List<CitationDto> citations = new ArrayList<>();

        public Builder brandName(String brandName) { this.brandName = brandName; return this; }
        public Builder genericName(String genericName) { this.genericName = genericName; return this; }
        public Builder drugClass(String drugClass) { this.drugClass = drugClass; return this; }
        public Builder mechanismOfActionExplanation(String mechanismOfActionExplanation) { this.mechanismOfActionExplanation = mechanismOfActionExplanation; return this; }
        public Builder indicationExplanation(String indicationExplanation) { this.indicationExplanation = indicationExplanation; return this; }
        public Builder plainLanguageDosing(String plainLanguageDosing) { this.plainLanguageDosing = plainLanguageDosing; return this; }
        public Builder safetyDecision(SafetyDecision safetyDecision) { this.safetyDecision = safetyDecision; return this; }
        public Builder citations(List<CitationDto> citations) { this.citations = citations; return this; }

        public PrescriptionUnderstandingResponse build() {
            return new PrescriptionUnderstandingResponse(brandName, genericName, drugClass, mechanismOfActionExplanation, indicationExplanation, plainLanguageDosing, safetyDecision, citations);
        }
    }
}

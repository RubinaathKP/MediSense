package com.medisense.domain.dto;

import com.medisense.domain.enums.EvidenceBasis;
import java.util.ArrayList;
import java.util.List;

public class ClaimVerificationResponse {
    private String userClaim;
    private String verificationSummary;
    private EvidenceBasis evidenceBasis;
    private List<CitationDto> citations = new ArrayList<>();

    public ClaimVerificationResponse() {}

    public ClaimVerificationResponse(String userClaim, String verificationSummary, EvidenceBasis evidenceBasis, List<CitationDto> citations) {
        this.userClaim = userClaim;
        this.verificationSummary = verificationSummary;
        this.evidenceBasis = evidenceBasis;
        this.citations = citations != null ? citations : new ArrayList<>();
    }

    public String getUserClaim() { return userClaim; }
    public void setUserClaim(String userClaim) { this.userClaim = userClaim; }

    public String getVerificationSummary() { return verificationSummary; }
    public void setVerificationSummary(String verificationSummary) { this.verificationSummary = verificationSummary; }

    public EvidenceBasis getEvidenceBasis() { return evidenceBasis; }
    public void setEvidenceBasis(EvidenceBasis evidenceBasis) { this.evidenceBasis = evidenceBasis; }

    public List<CitationDto> getCitations() { return citations; }
    public void setCitations(List<CitationDto> citations) { this.citations = citations; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String userClaim;
        private String verificationSummary;
        private EvidenceBasis evidenceBasis;
        private List<CitationDto> citations = new ArrayList<>();

        public Builder userClaim(String userClaim) { this.userClaim = userClaim; return this; }
        public Builder verificationSummary(String verificationSummary) { this.verificationSummary = verificationSummary; return this; }
        public Builder evidenceBasis(EvidenceBasis evidenceBasis) { this.evidenceBasis = evidenceBasis; return this; }
        public Builder citations(List<CitationDto> citations) { this.citations = citations; return this; }

        public ClaimVerificationResponse build() {
            return new ClaimVerificationResponse(userClaim, verificationSummary, evidenceBasis, citations);
        }
    }
}

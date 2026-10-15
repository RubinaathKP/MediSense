package com.medisense.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "eval_case_results")
public class EvalCaseResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "test_case_id", nullable = false)
    private String testCaseId;

    @Column(nullable = false)
    private Boolean passed;

    @Column(name = "actual_classification")
    private String actualClassification;

    @Column(name = "actual_is_referral")
    private Boolean actualIsReferral;

    @Column(name = "actual_citation_present")
    private Boolean actualCitationPresent;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public EvalCaseResultEntity() {}

    public EvalCaseResultEntity(UUID id, String runId, String testCaseId, Boolean passed, String actualClassification, Boolean actualIsReferral, Boolean actualCitationPresent, String failureReason, OffsetDateTime createdAt) {
        this.id = id;
        this.runId = runId;
        this.testCaseId = testCaseId;
        this.passed = passed;
        this.actualClassification = actualClassification;
        this.actualIsReferral = actualIsReferral;
        this.actualCitationPresent = actualCitationPresent;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getTestCaseId() { return testCaseId; }
    public void setTestCaseId(String testCaseId) { this.testCaseId = testCaseId; }

    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }

    public String getActualClassification() { return actualClassification; }
    public void setActualClassification(String actualClassification) { this.actualClassification = actualClassification; }

    public Boolean getActualIsReferral() { return actualIsReferral; }
    public void setActualIsReferral(Boolean actualIsReferral) { this.actualIsReferral = actualIsReferral; }

    public Boolean getActualCitationPresent() { return actualCitationPresent; }
    public void setActualCitationPresent(Boolean actualCitationPresent) { this.actualCitationPresent = actualCitationPresent; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String runId;
        private String testCaseId;
        private Boolean passed;
        private String actualClassification;
        private Boolean actualIsReferral;
        private Boolean actualCitationPresent;
        private String failureReason;
        private OffsetDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder runId(String runId) { this.runId = runId; return this; }
        public Builder testCaseId(String testCaseId) { this.testCaseId = testCaseId; return this; }
        public Builder passed(Boolean passed) { this.passed = passed; return this; }
        public Builder actualClassification(String actualClassification) { this.actualClassification = actualClassification; return this; }
        public Builder actualIsReferral(Boolean actualIsReferral) { this.actualIsReferral = actualIsReferral; return this; }
        public Builder actualCitationPresent(Boolean actualCitationPresent) { this.actualCitationPresent = actualCitationPresent; return this; }
        public Builder failureReason(String failureReason) { this.failureReason = failureReason; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public EvalCaseResultEntity build() {
            return new EvalCaseResultEntity(id, runId, testCaseId, passed, actualClassification, actualIsReferral, actualCitationPresent, failureReason, createdAt);
        }
    }
}

package com.medisense.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "eval_runs")
public class EvalRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "run_id", nullable = false, unique = true)
    private String runId;

    @Column(name = "triggered_by", nullable = false)
    private String triggeredBy;

    @Column(name = "started_at", insertable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "total_cases", nullable = false)
    private Integer totalCases;

    @Column(name = "passed_cases", nullable = false)
    private Integer passedCases;

    @Column(name = "failed_cases", nullable = false)
    private Integer failedCases;

    @Column(name = "metrics", columnDefinition = "jsonb")
    private String metricsJson;

    public EvalRunEntity() {}

    public EvalRunEntity(UUID id, String runId, String triggeredBy, OffsetDateTime startedAt, OffsetDateTime completedAt, Integer totalCases, Integer passedCases, Integer failedCases, String metricsJson) {
        this.id = id;
        this.runId = runId;
        this.triggeredBy = triggeredBy;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.totalCases = totalCases;
        this.passedCases = passedCases;
        this.failedCases = failedCases;
        this.metricsJson = metricsJson;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public Integer getTotalCases() { return totalCases; }
    public void setTotalCases(Integer totalCases) { this.totalCases = totalCases; }

    public Integer getPassedCases() { return passedCases; }
    public void setPassedCases(Integer passedCases) { this.passedCases = passedCases; }

    public Integer getFailedCases() { return failedCases; }
    public void setFailedCases(Integer failedCases) { this.failedCases = failedCases; }

    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String runId;
        private String triggeredBy;
        private OffsetDateTime startedAt;
        private OffsetDateTime completedAt;
        private Integer totalCases;
        private Integer passedCases;
        private Integer failedCases;
        private String metricsJson;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder runId(String runId) { this.runId = runId; return this; }
        public Builder triggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; return this; }
        public Builder startedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; return this; }
        public Builder completedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Builder totalCases(Integer totalCases) { this.totalCases = totalCases; return this; }
        public Builder passedCases(Integer passedCases) { this.passedCases = passedCases; return this; }
        public Builder failedCases(Integer failedCases) { this.failedCases = failedCases; return this; }
        public Builder metricsJson(String metricsJson) { this.metricsJson = metricsJson; return this; }

        public EvalRunEntity build() {
            return new EvalRunEntity(id, runId, triggeredBy, startedAt, completedAt, totalCases, passedCases, failedCases, metricsJson);
        }
    }
}

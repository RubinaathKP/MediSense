package com.medisense.domain.entity;

import com.medisense.domain.enums.SafetyDecision;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "responses")
public class ResponseRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "query_id", nullable = false)
    private QueryRecordEntity query;

    @Column(name = "grounded_answer", columnDefinition = "TEXT", nullable = false)
    private String groundedAnswer;

    @Column(name = "translated_answer", columnDefinition = "TEXT")
    private String translatedAnswer;

    @Enumerated(EnumType.STRING)
    @Column(name = "safety_decision", nullable = false, length = 50)
    private SafetyDecision safetyDecision;

    @Column(columnDefinition = "jsonb")
    private String citations;

    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public ResponseRecordEntity() {}

    public ResponseRecordEntity(UUID id, QueryRecordEntity query, String groundedAnswer, String translatedAnswer, SafetyDecision safetyDecision, String citations, Long latencyMs, OffsetDateTime createdAt) {
        this.id = id;
        this.query = query;
        this.groundedAnswer = groundedAnswer;
        this.translatedAnswer = translatedAnswer;
        this.safetyDecision = safetyDecision;
        this.citations = citations;
        this.latencyMs = latencyMs;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public QueryRecordEntity getQuery() { return query; }
    public void setQuery(QueryRecordEntity query) { this.query = query; }

    public String getGroundedAnswer() { return groundedAnswer; }
    public void setGroundedAnswer(String groundedAnswer) { this.groundedAnswer = groundedAnswer; }

    public String getTranslatedAnswer() { return translatedAnswer; }
    public void setTranslatedAnswer(String translatedAnswer) { this.translatedAnswer = translatedAnswer; }

    public SafetyDecision getSafetyDecision() { return safetyDecision; }
    public void setSafetyDecision(SafetyDecision safetyDecision) { this.safetyDecision = safetyDecision; }

    public String getCitations() { return citations; }
    public void setCitations(String citations) { this.citations = citations; }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private QueryRecordEntity query;
        private String groundedAnswer;
        private String translatedAnswer;
        private SafetyDecision safetyDecision;
        private String citations;
        private Long latencyMs;
        private OffsetDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder query(QueryRecordEntity query) { this.query = query; return this; }
        public Builder groundedAnswer(String groundedAnswer) { this.groundedAnswer = groundedAnswer; return this; }
        public Builder translatedAnswer(String translatedAnswer) { this.translatedAnswer = translatedAnswer; return this; }
        public Builder safetyDecision(SafetyDecision safetyDecision) { this.safetyDecision = safetyDecision; return this; }
        public Builder citations(String citations) { this.citations = citations; return this; }
        public Builder latencyMs(Long latencyMs) { this.latencyMs = latencyMs; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ResponseRecordEntity build() {
            return new ResponseRecordEntity(id, query, groundedAnswer, translatedAnswer, safetyDecision, citations, latencyMs, createdAt);
        }
    }
}

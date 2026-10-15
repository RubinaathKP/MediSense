package com.medisense.domain.entity;

import com.medisense.domain.enums.QueryCategory;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "queries")
public class QueryRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionEntity session;

    @Column(name = "raw_query", columnDefinition = "TEXT", nullable = false)
    private String rawQuery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private QueryCategory category;

    @Column(name = "detected_language", length = 10)
    private String detectedLanguage = "en";

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public QueryRecordEntity() {}

    public QueryRecordEntity(UUID id, SessionEntity session, String rawQuery, QueryCategory category, String detectedLanguage, OffsetDateTime createdAt) {
        this.id = id;
        this.session = session;
        this.rawQuery = rawQuery;
        this.category = category;
        this.detectedLanguage = detectedLanguage != null ? detectedLanguage : "en";
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public SessionEntity getSession() { return session; }
    public void setSession(SessionEntity session) { this.session = session; }

    public String getRawQuery() { return rawQuery; }
    public void setRawQuery(String rawQuery) { this.rawQuery = rawQuery; }

    public QueryCategory getCategory() { return category; }
    public void setCategory(QueryCategory category) { this.category = category; }

    public String getDetectedLanguage() { return detectedLanguage; }
    public void setDetectedLanguage(String detectedLanguage) { this.detectedLanguage = detectedLanguage; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private SessionEntity session;
        private String rawQuery;
        private QueryCategory category;
        private String detectedLanguage = "en";
        private OffsetDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder session(SessionEntity session) { this.session = session; return this; }
        public Builder rawQuery(String rawQuery) { this.rawQuery = rawQuery; return this; }
        public Builder category(QueryCategory category) { this.category = category; return this; }
        public Builder detectedLanguage(String detectedLanguage) { this.detectedLanguage = detectedLanguage; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public QueryRecordEntity build() {
            return new QueryRecordEntity(id, session, rawQuery, category, detectedLanguage, createdAt);
        }
    }
}

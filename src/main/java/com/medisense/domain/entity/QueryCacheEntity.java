package com.medisense.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "query_cache")
public class QueryCacheEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "query_hash", nullable = false, unique = true, length = 64)
    private String queryHash;

    @Column(name = "cached_response", columnDefinition = "TEXT", nullable = false)
    private String cachedResponse;

    @Column(columnDefinition = "jsonb")
    private String citations;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    public QueryCacheEntity() {}

    public QueryCacheEntity(UUID id, String queryHash, String cachedResponse, String citations, OffsetDateTime createdAt, OffsetDateTime expiresAt) {
        this.id = id;
        this.queryHash = queryHash;
        this.cachedResponse = cachedResponse;
        this.citations = citations;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getQueryHash() { return queryHash; }
    public void setQueryHash(String queryHash) { this.queryHash = queryHash; }

    public String getCachedResponse() { return cachedResponse; }
    public void setCachedResponse(String cachedResponse) { this.cachedResponse = cachedResponse; }

    public String getCitations() { return citations; }
    public void setCitations(String citations) { this.citations = citations; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String queryHash;
        private String cachedResponse;
        private String citations;
        private OffsetDateTime createdAt;
        private OffsetDateTime expiresAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder queryHash(String queryHash) { this.queryHash = queryHash; return this; }
        public Builder cachedResponse(String cachedResponse) { this.cachedResponse = cachedResponse; return this; }
        public Builder citations(String citations) { this.citations = citations; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder expiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; return this; }

        public QueryCacheEntity build() {
            return new QueryCacheEntity(id, queryHash, cachedResponse, citations, createdAt, expiresAt);
        }
    }
}

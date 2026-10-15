package com.medisense.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class SessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "session_token", nullable = false, unique = true, length = 512)
    private String sessionToken;

    @Column(name = "language_code", length = 10)
    private String languageCode = "en";

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "active")
    private Boolean active = true;

    public SessionEntity() {}

    public SessionEntity(UUID id, UserEntity user, String sessionToken, String languageCode, OffsetDateTime createdAt, OffsetDateTime expiresAt, Boolean active) {
        this.id = id;
        this.user = user;
        this.sessionToken = sessionToken;
        this.languageCode = languageCode != null ? languageCode : "en";
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = active != null ? active : true;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UserEntity user;
        private String sessionToken;
        private String languageCode = "en";
        private OffsetDateTime createdAt;
        private OffsetDateTime expiresAt;
        private Boolean active = true;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder user(UserEntity user) { this.user = user; return this; }
        public Builder sessionToken(String sessionToken) { this.sessionToken = sessionToken; return this; }
        public Builder languageCode(String languageCode) { this.languageCode = languageCode; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder expiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder active(Boolean active) { this.active = active; return this; }

        public SessionEntity build() {
            return new SessionEntity(id, user, sessionToken, languageCode, createdAt, expiresAt, active);
        }
    }
}

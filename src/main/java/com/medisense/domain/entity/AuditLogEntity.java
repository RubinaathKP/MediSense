package com.medisense.domain.entity;

import com.medisense.audit.AuditEvent;
import com.medisense.audit.Severity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID logId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AuditEvent eventType;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "query_id")
    private UUID queryId;

    @Column(name = "consent_grant_id")
    private String consentGrantId;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "event_detail", columnDefinition = "jsonb")
    private String eventDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity = Severity.INFO;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public AuditLogEntity() {}

    public AuditLogEntity(UUID logId, AuditEvent eventType, UUID sessionId, UUID userId, UUID queryId, String consentGrantId, String ipAddress, String eventDetail, Severity severity, OffsetDateTime createdAt) {
        this.logId = logId;
        this.eventType = eventType;
        this.sessionId = sessionId;
        this.userId = userId;
        this.queryId = queryId;
        this.consentGrantId = consentGrantId;
        this.ipAddress = ipAddress != null ? ipAddress : "0.0.0.0";
        this.eventDetail = eventDetail;
        this.severity = severity != null ? severity : Severity.INFO;
        this.createdAt = createdAt;
    }

    public UUID getLogId() { return logId; }
    public void setLogId(UUID logId) { this.logId = logId; }

    public AuditEvent getEventType() { return eventType; }
    public void setEventType(AuditEvent eventType) { this.eventType = eventType; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getQueryId() { return queryId; }
    public void setQueryId(UUID queryId) { this.queryId = queryId; }

    public String getConsentGrantId() { return consentGrantId; }
    public void setConsentGrantId(String consentGrantId) { this.consentGrantId = consentGrantId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getEventDetail() { return eventDetail; }
    public void setEventDetail(String eventDetail) { this.eventDetail = eventDetail; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID logId;
        private AuditEvent eventType;
        private UUID sessionId;
        private UUID userId;
        private UUID queryId;
        private String consentGrantId;
        private String ipAddress = "0.0.0.0";
        private String eventDetail;
        private Severity severity = Severity.INFO;
        private OffsetDateTime createdAt;

        public Builder logId(UUID logId) { this.logId = logId; return this; }
        public Builder eventType(AuditEvent eventType) { this.eventType = eventType; return this; }
        public Builder sessionId(UUID sessionId) { this.sessionId = sessionId; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder queryId(UUID queryId) { this.queryId = queryId; return this; }
        public Builder consentGrantId(String consentGrantId) { this.consentGrantId = consentGrantId; return this; }
        public Builder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public Builder eventDetail(String eventDetail) { this.eventDetail = eventDetail; return this; }
        public Builder severity(Severity severity) { this.severity = severity; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }

        public AuditLogEntity build() {
            return new AuditLogEntity(logId, eventType, sessionId, userId, queryId, consentGrantId, ipAddress, eventDetail, severity, createdAt);
        }
    }
}

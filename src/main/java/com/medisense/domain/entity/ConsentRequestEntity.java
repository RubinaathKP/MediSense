package com.medisense.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "consent_requests")
public class ConsentRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "consent_request_id", nullable = false, unique = true)
    private String consentRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private SessionEntity session;

    @Column(name = "patient_abha_id", nullable = false)
    private String patientAbhaId;

    @Column(name = "status", nullable = false)
    private String status; // REQUESTED, GRANTED, DENIED, EXPIRED, REVOKED

    @Column(name = "consent_artefact_id")
    private String consentArtefactId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public ConsentRequestEntity() {}

    public ConsentRequestEntity(UUID id, String consentRequestId, SessionEntity session, String patientAbhaId,
                                String status, String consentArtefactId, String transactionId,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.consentRequestId = consentRequestId;
        this.session = session;
        this.patientAbhaId = patientAbhaId;
        this.status = status;
        this.consentArtefactId = consentArtefactId;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getConsentRequestId() { return consentRequestId; }
    public void setConsentRequestId(String consentRequestId) { this.consentRequestId = consentRequestId; }

    public SessionEntity getSession() { return session; }
    public void setSession(SessionEntity session) { this.session = session; }

    public String getPatientAbhaId() { return patientAbhaId; }
    public void setPatientAbhaId(String patientAbhaId) { this.patientAbhaId = patientAbhaId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getConsentArtefactId() { return consentArtefactId; }
    public void setConsentArtefactId(String consentArtefactId) { this.consentArtefactId = consentArtefactId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String consentRequestId;
        private SessionEntity session;
        private String patientAbhaId;
        private String status;
        private String consentArtefactId;
        private String transactionId;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder consentRequestId(String consentRequestId) { this.consentRequestId = consentRequestId; return this; }
        public Builder session(SessionEntity session) { this.session = session; return this; }
        public Builder patientAbhaId(String patientAbhaId) { this.patientAbhaId = patientAbhaId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder consentArtefactId(String consentArtefactId) { this.consentArtefactId = consentArtefactId; return this; }
        public Builder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ConsentRequestEntity build() {
            return new ConsentRequestEntity(id, consentRequestId, session, patientAbhaId, status, consentArtefactId, transactionId, createdAt, updatedAt);
        }
    }
}

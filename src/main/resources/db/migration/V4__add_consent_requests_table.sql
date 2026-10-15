-- ===================================================================
-- MediSense Flyway Migration V4
-- ABDM HIU Consent Requests Table
-- ===================================================================

CREATE TABLE consent_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    consent_request_id VARCHAR(255) NOT NULL UNIQUE,
    session_id UUID REFERENCES sessions(id) ON DELETE CASCADE,
    patient_abha_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL, -- REQUESTED, GRANTED, DENIED, EXPIRED, REVOKED
    consent_artefact_id VARCHAR(255),
    transaction_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_consent_requests_request_id ON consent_requests(consent_request_id);
CREATE INDEX idx_consent_requests_session ON consent_requests(session_id);
CREATE INDEX idx_consent_requests_artefact ON consent_requests(consent_artefact_id);

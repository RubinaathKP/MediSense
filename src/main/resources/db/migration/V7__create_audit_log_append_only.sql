-- ===================================================================
-- MediSense Flyway Migration V7
-- Audit Log Schema & Append-Only Database Rules
-- ===================================================================

-- 1. Upgrade audit_log table to canonical append-only schema
DROP TABLE IF EXISTS audit_log CASCADE;

CREATE TABLE audit_log (
    log_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type VARCHAR(100) NOT NULL,
    session_id UUID REFERENCES sessions(id) ON DELETE SET NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    query_id UUID,
    consent_grant_id VARCHAR(255),
    ip_address VARCHAR(45) NOT NULL,
    event_detail JSONB,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indices for rapid query & abuse detection
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
CREATE INDEX idx_audit_log_event_type ON audit_log(event_type);
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_ip_address ON audit_log(ip_address);
CREATE INDEX idx_audit_log_severity ON audit_log(severity);

-- 2. APPEND-ONLY Rules: Prevent UPDATE or DELETE on audit_log
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_rewrite WHERE rulename = 'no_update_audit') THEN
        CREATE RULE no_update_audit AS ON UPDATE TO audit_log DO INSTEAD NOTHING;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_rewrite WHERE rulename = 'no_delete_audit') THEN
        CREATE RULE no_delete_audit AS ON DELETE TO audit_log DO INSTEAD NOTHING;
    END IF;
END $$;

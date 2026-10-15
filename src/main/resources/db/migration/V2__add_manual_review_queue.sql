-- ===================================================================
-- MediSense Flyway Migration V2
-- Create Manual Review Queue Table for Quality Gate Rejections
-- ===================================================================

CREATE TABLE manual_review_queue (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_name VARCHAR(255) NOT NULL,
    rejection_reason TEXT NOT NULL,
    rejection_stage VARCHAR(50) NOT NULL,
    extraction_confidence NUMERIC(5, 4),
    token_count INT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_manual_review_queue_created ON manual_review_queue(created_at);

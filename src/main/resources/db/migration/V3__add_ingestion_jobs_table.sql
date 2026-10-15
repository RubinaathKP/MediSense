-- ===================================================================
-- MediSense Migration V3 - Add Ingestion Jobs Table & Active Status Flag
-- ===================================================================

CREATE TABLE IF NOT EXISTS ingestion_jobs (
    id UUID PRIMARY KEY,
    document_name VARCHAR(255) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    status VARCHAR(50) NOT NULL,
    progress_percentage INT DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_checksum ON ingestion_jobs(checksum);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_status ON ingestion_jobs(status);

-- Add active status column to child_chunks table
ALTER TABLE child_chunks ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
CREATE INDEX IF NOT EXISTS idx_child_chunks_is_active ON child_chunks(is_active);

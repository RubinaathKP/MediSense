-- ===================================================================
-- MediSense Flyway Migration V1
-- PostgreSQL + pgvector Schema Creation
-- ===================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

-- 1. Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    language_preference VARCHAR(10) DEFAULT 'en',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Sessions Table (DPDP & Active Session Tracking)
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    session_token VARCHAR(512) NOT NULL UNIQUE,
    language_code VARCHAR(10) DEFAULT 'en',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    active BOOLEAN DEFAULT TRUE
);

-- 3. Source Documents Table
CREATE TABLE source_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID REFERENCES sessions(id) ON DELETE SET NULL,
    document_name VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL, -- LAB_REPORT, PRESCRIPTION, CLINICAL_GUIDELINE, PHARMACOLOGY
    source_type VARCHAR(50) NOT NULL, -- PDF_UPLOAD, ABDM_PULL, CORPUS
    content_hash VARCHAR(64) NOT NULL,
    publication_year INT,
    version_tag VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Parent Chunks Table (Coarse Granularity for RAG Context)
CREATE TABLE parent_chunks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL REFERENCES source_documents(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Child Chunks Table (Fine Granularity for Vector Similarity Search)
CREATE TABLE child_chunks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parent_chunk_id UUID NOT NULL REFERENCES parent_chunks(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(768),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- HNSW Vector Index on Child Chunks (Cosine Similarity)
CREATE INDEX idx_child_chunks_embedding_hnsw 
ON child_chunks USING hnsw (embedding vector_cosine_ops) 
WITH (m = 16, ef_construction = 200);

-- 6. Lab Report Values Table
CREATE TABLE lab_report_values (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID REFERENCES source_documents(id) ON DELETE CASCADE,
    session_id UUID REFERENCES sessions(id) ON DELETE CASCADE,
    test_name VARCHAR(255) NOT NULL,
    extracted_value VARCHAR(100) NOT NULL,
    normalized_value NUMERIC(12, 4),
    unit VARCHAR(50),
    reference_range VARCHAR(100),
    flag_status VARCHAR(20) NOT NULL, -- NORMAL, BORDERLINE, ABNORMAL
    extraction_confidence NUMERIC(5, 4) NOT NULL,
    requires_user_confirmation BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. Prescriptions Table
CREATE TABLE prescriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID REFERENCES source_documents(id) ON DELETE CASCADE,
    session_id UUID REFERENCES sessions(id) ON DELETE CASCADE,
    brand_name VARCHAR(255) NOT NULL,
    generic_name VARCHAR(255) NOT NULL,
    drug_class VARCHAR(255) NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    timing VARCHAR(100),
    duration VARCHAR(100),
    prescribing_specialty VARCHAR(150),
    extraction_confidence NUMERIC(5, 4) NOT NULL,
    requires_user_confirmation BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. Queries Table
CREATE TABLE queries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    raw_query TEXT NOT NULL,
    category VARCHAR(50) NOT NULL, -- REPORT_EXPLANATION, PRESCRIPTION_UNDERSTANDING, HEALTH_CLAIM, PRESCRIPTIVE_REFUSAL, EMERGENCY_REFUSAL
    detected_language VARCHAR(10) DEFAULT 'en',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. Responses Table
CREATE TABLE responses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    query_id UUID NOT NULL REFERENCES queries(id) ON DELETE CASCADE,
    grounded_answer TEXT NOT NULL,
    translated_answer TEXT,
    safety_decision VARCHAR(50) NOT NULL, -- PASSED, REFUSED_PRESCRIPTIVE, REFUSED_EMERGENCY
    citations JSONB,
    latency_ms BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 10. Audit Log Table (Write-Once Audit Trail with Field Encryption)
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    session_id UUID REFERENCES sessions(id) ON DELETE SET NULL,
    consent_grant_id VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    encrypted_details TEXT NOT NULL,
    actor_id VARCHAR(255) NOT NULL
);

-- Index for Audit Log Queries
CREATE INDEX idx_audit_log_timestamp ON audit_log(event_timestamp);
CREATE INDEX idx_audit_log_session ON audit_log(session_id);

-- 11. Drug Name Lookup Table (Brand -> Generic Normalization Mapping)
CREATE TABLE drug_name_lookup (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    brand_name VARCHAR(255) NOT NULL UNIQUE,
    generic_name VARCHAR(255) NOT NULL,
    drug_class VARCHAR(255) NOT NULL,
    version_tag VARCHAR(50) DEFAULT 'v1.0',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_drug_name_brand ON drug_name_lookup(LOWER(brand_name));

-- 12. Query Cache Table
CREATE TABLE query_cache (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    query_hash VARCHAR(64) NOT NULL UNIQUE,
    cached_response TEXT NOT NULL,
    citations JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_query_cache_hash ON query_cache(query_hash);

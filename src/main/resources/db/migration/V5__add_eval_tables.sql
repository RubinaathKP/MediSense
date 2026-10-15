-- ===================================================================
-- MediSense Flyway Migration V5
-- Evaluation Framework Tables (eval_runs and eval_case_results)
-- ===================================================================

CREATE TABLE eval_runs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    run_id VARCHAR(255) NOT NULL UNIQUE,
    triggered_by VARCHAR(255) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    total_cases INT NOT NULL,
    passed_cases INT NOT NULL,
    failed_cases INT NOT NULL,
    metrics JSONB
);

CREATE TABLE eval_case_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    run_id VARCHAR(255) NOT NULL REFERENCES eval_runs(run_id) ON DELETE CASCADE,
    test_case_id VARCHAR(100) NOT NULL,
    passed BOOLEAN NOT NULL,
    actual_classification VARCHAR(100),
    actual_is_referral BOOLEAN,
    actual_citation_present BOOLEAN,
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_eval_runs_run_id ON eval_runs(run_id);
CREATE INDEX idx_eval_case_results_run_id ON eval_case_results(run_id);
CREATE INDEX idx_eval_case_results_case_id ON eval_case_results(test_case_id);

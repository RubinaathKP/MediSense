-- ===================================================================
-- MediSense Test Fixtures: test_data.sql
-- Pre-seeded clinical corpus, test patients, drugs, and guidelines
-- ===================================================================

-- 1. Test Users
INSERT INTO users (id, email, password_hash, full_name, language_preference, failed_attempts, created_at, updated_at)
VALUES 
    ('11111111-1111-1111-1111-111111111111', 'patient.test@medisense.health', '$2a$10$wK1/vIeFv.7R5g.MockHashedPasswordStringForTests', 'Ramesh Patel', 'en', 0, NOW(), NOW()),
    ('22222222-2222-2222-2222-222222222222', 'doctor.test@medisense.health', '$2a$10$wK1/vIeFv.7R5g.MockHashedPasswordStringForTests', 'Dr. Priya Sundaram', 'ta', 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 2. Test Sessions
INSERT INTO sessions (id, user_id, session_token, language_code, created_at, expires_at, active)
VALUES 
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'test-session-token-ramesh-123', 'en', NOW(), NOW() + INTERVAL '7 days', true)
ON CONFLICT (id) DO NOTHING;

-- 3. Source Clinical Documents
INSERT INTO source_documents (id, session_id, document_name, document_type, source_type, content_hash, publication_year, version_tag, created_at)
VALUES 
    ('44444444-4444-4444-4444-444444444441', NULL, 'ICMR_Lipid_Management_Guidelines_2024.pdf', 'LAB_GUIDELINE', 'CORPUS', 'hash-icmr-lipid-2024-sha256', 2024, 'v2.0', NOW()),
    ('44444444-4444-4444-4444-444444444442', NULL, 'WHO_Type_2_Diabetes_Management_2023.pdf', 'CLINICAL_PRACTICE', 'CORPUS', 'hash-who-diabetes-2023-sha256', 2023, 'v1.5', NOW())
ON CONFLICT (id) DO NOTHING;

-- 4. Parent Chunks
INSERT INTO parent_chunks (id, document_id, chunk_index, content, metadata, created_at)
VALUES 
    ('55555555-5555-5555-5555-555555555551', '44444444-4444-4444-4444-444444444441', 0, 
     'Section 1.1: Serum Low-Density Lipoprotein (LDL) Cholesterol Evaluation. Desirable levels are below 100 mg/dL. Levels between 130 and 159 mg/dL are classified as borderline high. Adult patients presenting borderline LDL should be evaluated for overall cardiovascular risk.',
     '{"section": "1.1", "guideline": "ICMR Lipid Guidelines", "evidence_level": "A"}', NOW()),
    ('55555555-5555-5555-5555-555555555552', '44444444-4444-4444-4444-444444444442', 0,
     'Section 2.3: Glycated Hemoglobin (HbA1c) Target Standards. Normal adult HbA1c is below 5.7%. Levels between 5.7% and 6.4% indicate prediabetes. An HbA1c of 6.5% or higher on two separate tests indicates diabetes mellitus.',
     '{"section": "2.3", "guideline": "WHO Diabetes Guidelines", "evidence_level": "A"}', NOW())
ON CONFLICT (id) DO NOTHING;

-- 5. Child Chunks (Dense & Sparse Search Targets)
INSERT INTO child_chunks (id, parent_chunk_id, chunk_index, content, metadata, is_active, created_at)
VALUES 
    ('66666666-6666-6666-6666-666666666661', '55555555-5555-5555-5555-555555555551', 0,
     'LDL cholesterol levels between 130 and 159 mg/dL represent borderline high risk. Lifestyle and dietary intervention are standard recommendations.',
     '{"section_path": "1.1.1", "source_name": "ICMR Lipid Guidelines 2024"}', true, NOW()),
    ('66666666-6666-6666-6666-666666666662', '55555555-5555-5555-5555-555555555552', 0,
     'An HbA1c level of 7.2 percent indicates elevated average blood sugar over the preceding 3 months, requiring physician assessment.',
     '{"section_path": "2.3.1", "source_name": "WHO Diabetes Guidelines 2023"}', true, NOW())
ON CONFLICT (id) DO NOTHING;

-- 6. Drug Lookup Catalog
INSERT INTO drug_name_lookup (id, brand_name, generic_name, drug_class, version_tag, created_at)
VALUES 
    ('77777777-7777-7777-7777-777777777771', 'Glycomet', 'Metformin Hydrochloride', 'Biguanide Antidiabetic', 'v1.0', NOW()),
    ('77777777-7777-7777-7777-777777777772', 'Lipitor', 'Atorvastatin Calcium', 'Statin HMG-CoA Reductase Inhibitor', 'v1.0', NOW()),
    ('77777777-7777-7777-7777-777777777773', 'Norvasc', 'Amlodipine Besylate', 'Calcium Channel Blocker', 'v1.0', NOW()),
    ('77777777-7777-7777-7777-777777777774', 'Telma', 'Telmisartan', 'Angiotensin Receptor Blocker', 'v1.0', NOW())
ON CONFLICT (id) DO NOTHING;

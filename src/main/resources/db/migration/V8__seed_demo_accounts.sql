-- ===================================================================
-- MediSense Flyway Migration V8
-- Seed Default Demo & Test Accounts
-- ===================================================================

-- 1. Demo Patient Account (Password: DemoUser@2026!)
INSERT INTO users (id, email, password_hash, full_name, language_preference, failed_attempts)
VALUES (
    uuid_generate_v4(),
    'demo@medisense.health',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    'Dr. Aarav Sharma (Demo Patient)',
    'en',
    0
) ON CONFLICT (email) DO NOTHING;

-- 2. Demo Tamil User Account (Password: DemoUser@2026!)
INSERT INTO users (id, email, password_hash, full_name, language_preference, failed_attempts)
VALUES (
    uuid_generate_v4(),
    'tamil.demo@medisense.health',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    'கார்த்திகேயன் (Demo Tamil User)',
    'ta',
    0
) ON CONFLICT (email) DO NOTHING;

-- 3. Demo Admin Account (Password: AdminSecure@2026!)
INSERT INTO users (id, email, password_hash, full_name, language_preference, failed_attempts)
VALUES (
    uuid_generate_v4(),
    'admin@medisense.health',
    '$2a$10$7Z2vG8P0rX3U6w1fK0lE4OqT8vZ4yJ2nM8bQ6sL1pW3uY5vN7xQ.y',
    'MediSense Chief Admin',
    'en',
    0
) ON CONFLICT (email) DO NOTHING;

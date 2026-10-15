-- ===================================================================
-- MediSense Flyway Migration V6
-- Add Account Lockout Fields to Users Table
-- ===================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_attempts INT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP WITH TIME ZONE;

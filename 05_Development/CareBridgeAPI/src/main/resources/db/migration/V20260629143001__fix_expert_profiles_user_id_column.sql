-- V20260629143001__fix_expert_profiles_user_id_column.sql
-- Corrective: rename account_id → user_id to match users(user_id) PK
-- Applied when V20260629143000 partially created the table with wrong column name

-- Drop the old index if it exists (from the failed migration run)
DROP INDEX IF EXISTS idx_expert_profiles_account;

-- Rename account_id → user_id (no-op if already renamed)
ALTER TABLE IF EXISTS expert_profiles RENAME COLUMN account_id TO user_id;

-- Recreate indexes with correct column name
CREATE INDEX IF NOT EXISTS idx_expert_profiles_user ON expert_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_expert_profiles_status ON expert_profiles(status);

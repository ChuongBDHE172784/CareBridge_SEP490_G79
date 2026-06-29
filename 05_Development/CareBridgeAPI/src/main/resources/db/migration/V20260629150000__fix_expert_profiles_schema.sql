-- Corrective migration: fix expert_profiles table created with wrong column name
-- Previous V20260629143000 created the table with account_id instead of user_id
-- and the status column was missing due to partial execution.

-- 1. Rename account_id → user_id if old column exists
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'expert_profiles' AND column_name = 'account_id') THEN
    ALTER TABLE expert_profiles RENAME COLUMN account_id TO user_id;
  END IF;
END $$;

-- 2. Drop old index if exists
DROP INDEX IF EXISTS idx_expert_profiles_account;

-- 3. Create correct indexes
CREATE INDEX IF NOT EXISTS idx_expert_profiles_user ON expert_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_expert_profiles_status ON expert_profiles(status);

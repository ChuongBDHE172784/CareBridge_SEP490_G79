-- Add admin review columns to expert_credentials for UC-70 workflow

-- Add reviewed_by column if not exists
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
    WHERE table_name = 'expert_credentials' AND column_name = 'reviewed_by') THEN
    ALTER TABLE expert_credentials ADD COLUMN reviewed_by UUID;
  END IF;
END $$;

-- Add reviewed_at column if not exists
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
    WHERE table_name = 'expert_credentials' AND column_name = 'reviewed_at') THEN
    ALTER TABLE expert_credentials ADD COLUMN reviewed_at TIMESTAMP;
  END IF;
END $$;

-- Index for filtering pending reviews
CREATE INDEX IF NOT EXISTS idx_expert_credentials_review_status
  ON expert_credentials (reviewed_by, reviewed_at);

-- Foreign key constraint (after users table exists)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_name = 'fk_expert_credentials_reviewed_by') THEN
    ALTER TABLE expert_credentials
      ADD CONSTRAINT fk_expert_credentials_reviewed_by
      FOREIGN KEY (reviewed_by) REFERENCES users(user_id) ON DELETE SET NULL;
  END IF;
END $$;

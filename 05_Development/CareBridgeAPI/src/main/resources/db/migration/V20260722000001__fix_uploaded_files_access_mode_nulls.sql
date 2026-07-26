-- Fix access_mode null values in uploaded_files (Flyway repair migration)
-- This handles existing databases where V20260718000004 was applied but left nulls

-- Backfill access_mode for existing records
UPDATE uploaded_files
SET access_mode = 'PRIVATE'
WHERE access_mode IS NULL;

-- Ensure NOT NULL constraint
ALTER TABLE uploaded_files
    ALTER COLUMN access_mode SET NOT NULL;

ALTER TABLE uploaded_files
    ALTER COLUMN access_mode SET DEFAULT 'PRIVATE';

-- Verify constraint exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_uploaded_files_access_mode'
        AND table_name = 'uploaded_files'
    ) THEN
        ALTER TABLE uploaded_files
        ADD CONSTRAINT chk_uploaded_files_access_mode
        CHECK (access_mode IN ('PRIVATE', 'AUTHENTICATED', 'PUBLIC'));
    END IF;
END $$;
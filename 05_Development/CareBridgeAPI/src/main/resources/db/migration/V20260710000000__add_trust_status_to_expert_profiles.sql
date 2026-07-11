-- V20260710000000__add_trust_status_to_expert_profiles.sql
-- Purpose: Add trust_status column to expert_profiles after TrustStatus enum was introduced

-- Add trust_status column with default ACTIVE
ALTER TABLE public.expert_profiles
  ADD COLUMN IF NOT EXISTS trust_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Backfill existing rows (redundant with DEFAULT but explicit)
UPDATE public.expert_profiles
  SET trust_status = 'ACTIVE'
  WHERE trust_status IS NULL;

-- Constrain to valid enum values
ALTER TABLE public.expert_profiles
  ADD CONSTRAINT expert_profiles_trust_status_check
  CHECK (trust_status IN ('ACTIVE', 'SUSPENDED', 'REVOKED'));

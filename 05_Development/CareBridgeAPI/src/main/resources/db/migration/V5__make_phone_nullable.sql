-- V5: Make phone column nullable in users table
-- Reason: Users can register with email only (no phone). V1 schema had NOT NULL
-- which causes 500 errors in email-only registration flow.
ALTER TABLE public.users ALTER COLUMN phone DROP NOT NULL;

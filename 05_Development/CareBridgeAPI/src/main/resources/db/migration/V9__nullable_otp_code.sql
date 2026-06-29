-- =============================================================================
-- V9__nullable_otp_code.sql
-- Purpose: Make otp_verifications.otp_code nullable.
--          The entity uses code_hash (SHA-256) for secure OTP storage.
--          The otp_code column (plain text) from V1 is no longer written.
-- Applied: 2026-06-27
-- =============================================================================

ALTER TABLE public.otp_verifications
    ALTER COLUMN otp_code DROP NOT NULL;

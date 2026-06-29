-- =============================================================================
-- V7__entity_schema_sync.sql
-- Purpose: Add columns expected by JPA entities that are absent from the DB.
--          V1–V6 migrations diverged from entity field names; this closes the gap.
-- Applied: 2026-06-27
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 7A. otp_verifications — add user_id FK, code_hash, used_at
-- ---------------------------------------------------------------------------
ALTER TABLE public.otp_verifications
    ADD COLUMN IF NOT EXISTS user_id    uuid,
    ADD COLUMN IF NOT EXISTS code_hash  varchar(255),
    ADD COLUMN IF NOT EXISTS used_at    timestamptz;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'otp_verifications_user_id_fkey'
    ) THEN
        ALTER TABLE public.otp_verifications
            ADD CONSTRAINT otp_verifications_user_id_fkey
            FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE SET NULL;
    END IF;
END $$;

ALTER TABLE public.otp_verifications
    ALTER COLUMN phone DROP NOT NULL;

-- ---------------------------------------------------------------------------
-- 7B. users — add account_status, last_login_at
-- ---------------------------------------------------------------------------
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS account_status varchar(30),
    ADD COLUMN IF NOT EXISTS last_login_at  timestamptz;

-- ---------------------------------------------------------------------------
-- 7C. user_sessions — add is_current and revoked boolean columns
--     (revoked_at timestamptz already exists from V1; revoked boolean is the entity field)
-- ---------------------------------------------------------------------------
ALTER TABLE public.user_sessions
    ADD COLUMN IF NOT EXISTS is_current boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS revoked    boolean NOT NULL DEFAULT false;

-- ---------------------------------------------------------------------------
-- 7D. refresh_tokens — add token_hash (entity field tokenHash)
-- ---------------------------------------------------------------------------
ALTER TABLE public.refresh_tokens
    ADD COLUMN IF NOT EXISTS token_hash varchar(64);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'refresh_tokens_token_hash_key'
    ) THEN
        ALTER TABLE public.refresh_tokens
            ADD CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash);
    END IF;
END $$;

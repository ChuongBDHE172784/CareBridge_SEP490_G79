-- =============================================================================
-- V8__create_token_blacklist.sql
-- Purpose: Create token_blacklist table referenced by TokenBlacklist entity.
--          V1 excluded this table ("revocation covered by user_sessions.revoked_at")
--          but the Java code (AuthServiceImpl, SessionServiceImpl) actively uses it.
-- Applied: 2026-06-27
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.token_blacklist (
    id          uuid         NOT NULL DEFAULT gen_random_uuid(),
    token_hash  varchar(255) NOT NULL,
    expires_at  timestamptz  NOT NULL,
    revoked_at  timestamptz  NOT NULL DEFAULT now(),
    reason      varchar(100),
    CONSTRAINT token_blacklist_pkey PRIMARY KEY (id),
    CONSTRAINT token_blacklist_token_hash_key UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_token_blacklist_hash
    ON public.token_blacklist (token_hash);

CREATE INDEX IF NOT EXISTS idx_token_blacklist_expires
    ON public.token_blacklist (expires_at);

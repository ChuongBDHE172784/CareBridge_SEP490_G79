-- =============================================================================
-- V4__auth_password_reset_audit_fix.sql
-- Purpose:
--   1. Expand audit_logs_action_check to include all actions used by Java code
--      (OTP_RESENT, USER_REGISTRATION_COMPLETED, SESSION_REVOKED, PASSWORD_RESET_*,
--       PASSWORD_CHANGED, CONTENT_CREATED, PARTNER_PROFILE_CREATED, etc.)
--   2. Add missing columns to users table referenced by AuthServiceImpl
--      (failed_login_count, locked_at already in V2; email_verified, phone_verified needed)
-- Applied: 2026-06-26 — additive/corrective only
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 4A. Expand audit_logs action constraint
-- ---------------------------------------------------------------------------

ALTER TABLE public.audit_logs
    DROP CONSTRAINT IF EXISTS audit_logs_action_check;

ALTER TABLE public.audit_logs
    ADD CONSTRAINT audit_logs_action_check CHECK (
        (action)::text = ANY ((ARRAY[
            'LOGIN',
            'LOGOUT',
            'OTP_SENT',
            'OTP_VERIFIED',
            'OTP_RESENT',
            'CONSENT_GRANTED',
            'CONSENT_REVOKED',
            'CREATE_HEALTH_RECORD',
            'VIEW_HEALTH_RECORD',
            'EXPERT_VERIFICATION',
            'MODERATION_ACTION',
            'AI_TRIAGE',
            'PAYMENT',
            'SECURITY_EVENT',
            'VIEW_AUDIT_LOG',
            'USER_REGISTRATION_COMPLETED',
            'SESSION_REVOKED',
            'CONTENT_CREATED',
            'PARTNER_PROFILE_CREATED',
            'COMMUNITY_QUESTION_CREATED',
            'COMMUNITY_ANSWER_POSTED',
            'MODERATION_QUEUE_VIEWED',
            'PASSWORD_RESET_REQUESTED',
            'PASSWORD_RESET_COMPLETED',
            'PASSWORD_CHANGED'
        ])::text[])
    );

-- ---------------------------------------------------------------------------
-- 4B. Add email_verified and phone_verified columns to users if not present
-- (Referenced by UC05 ForgotPassword notification fallback logic)
-- ---------------------------------------------------------------------------

ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS email_verified boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS phone_verified boolean NOT NULL DEFAULT false;

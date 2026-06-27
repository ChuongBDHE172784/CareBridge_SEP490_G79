-- =============================================================================
-- V6__new_features_schema.sql
-- Purpose: Schema additions for UC09, UC16, UC157, UC158-161, UC174, UC175.
-- Migrations for user_profiles, privacy_settings already applied in V2.
-- This migration only adds columns and updates constraints.
-- Applied: 2026-06-27
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 6A. Expand audit_logs_action_check — add new actions
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
            'PASSWORD_CHANGED',
            'PROFILE_UPDATED',
            'PROFILE_VIEWED',
            'PRIVACY_SETTINGS_ACCESSED',
            'PRIVACY_SETTINGS_UPDATED',
            'NOTIFICATION_SENT',
            'NOTIFICATION_FAILED',
            'SECURITY_INCIDENT_INVESTIGATED',
            'SECURITY_EVENT_REVIEWED',
            'SECURITY_NOTE_ADDED'
        ])::text[])
    );

-- ---------------------------------------------------------------------------
-- 6B. Notifications table — add FCM tracking columns
-- ---------------------------------------------------------------------------

ALTER TABLE public.notifications
    ADD COLUMN IF NOT EXISTS fcm_message_id   varchar(255),
    ADD COLUMN IF NOT EXISTS attempt_count    integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS failed_at        timestamptz,
    ADD COLUMN IF NOT EXISTS channel          varchar(30) NOT NULL DEFAULT 'PUSH';

ALTER TABLE public.notifications
    DROP CONSTRAINT IF EXISTS notifications_channel_check;
ALTER TABLE public.notifications
    ADD CONSTRAINT notifications_channel_check
        CHECK (((channel)::text = ANY ((ARRAY['PUSH'::varchar, 'EMAIL'::varchar, 'IN_APP'::varchar])::text[])));

-- ---------------------------------------------------------------------------
-- 6C. Notification records table for UC158-161 tracking
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.notification_records (
    id              uuid         NOT NULL DEFAULT gen_random_uuid(),
    user_id         uuid         NOT NULL,
    type            varchar(50)  NOT NULL,
    title           varchar(255) NOT NULL,
    body            text         NOT NULL,
    reference_id    uuid,
    reference_type  varchar(50),
    status          varchar(20)  NOT NULL DEFAULT 'SENT',
    fcm_message_id  varchar(255),
    attempt_count   integer      NOT NULL DEFAULT 1,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    sent_at         timestamptz,
    failed_at       timestamptz,
    CONSTRAINT notification_records_pkey PRIMARY KEY (id),
    CONSTRAINT notification_records_status_check CHECK (((status)::text = ANY (
        (ARRAY['SENT'::varchar, 'DELIVERED'::varchar, 'FAILED'::varchar])::text[]
    ))),
    CONSTRAINT notification_records_type_check CHECK (((type)::text = ANY (
        (ARRAY['REMINDER'::varchar, 'COMMUNITY_REPLY'::varchar, 'CONSULTATION'::varchar, 'EMERGENCY'::varchar])::text[]
    ))),
    CONSTRAINT fk_notification_records_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_notification_records_user_id
    ON public.notification_records (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_records_type_status
    ON public.notification_records (type, status);

-- ---------------------------------------------------------------------------
-- 6D. Add session pagination support index (if not already created by V2)
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_sessions_user_active
    ON public.user_sessions (user_id, last_activity_at DESC)
    WHERE revoked_at IS NULL;

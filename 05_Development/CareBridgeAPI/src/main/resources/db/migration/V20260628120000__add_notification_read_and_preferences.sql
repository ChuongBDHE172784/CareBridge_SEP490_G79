-- =============================================================================
-- V20260628120000__add_notification_read_and_preferences.sql
-- Purpose: UC-10 (notification preferences), UC-12 (mark as read)
-- Author: AI Agent | Date: 2026-06-28
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Add is_read column to notification_records (UC-12)
-- ---------------------------------------------------------------------------
ALTER TABLE public.notification_records
    ADD COLUMN IF NOT EXISTS is_read    BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS read_at    TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_notification_records_user_unread
    ON public.notification_records (user_id, is_read)
    WHERE is_read = false;

-- ---------------------------------------------------------------------------
-- 2. Update notification_preferences table to support enabled flag per type
-- The V1 schema has: preference_id, user_id, notification_type, email_enabled,
-- in_app_enabled, push_enabled, quiet_hours_start, quiet_hours_end
-- We keep backward compatibility and use notification_type + push_enabled as
-- the canonical "enabled" for PUSH channel.
-- ---------------------------------------------------------------------------

-- Ensure unique constraint exists on (user_id, notification_type)
-- (V1 may not have this constraint — add it safely)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'notification_preferences_user_type_unique'
    ) THEN
        ALTER TABLE public.notification_preferences
            ADD CONSTRAINT notification_preferences_user_type_unique
            UNIQUE (user_id, notification_type);
    END IF;
END $$;

-- Ensure updated_at column exists
ALTER TABLE public.notification_preferences
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- ---------------------------------------------------------------------------
-- 3. Audit constraint update — add new audit actions for notifications
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
            'SECURITY_NOTE_ADDED',
            'JOURNEY_CREATED',
            'JOURNEY_UPDATED',
            'BABY_PROFILE_CREATED',
            'HEALTH_RECORD_ADDED',
            'REMINDER_CREATED',
            'CARE_GROUP_CREATED',
            'FILE_UPLOADED',
            'NOTIFICATION_PREFERENCES_UPDATED',
            'NOTIFICATIONS_READ',
            'NOTIFICATION_PREFERENCES_VIEWED'
        ])::text[])
    );

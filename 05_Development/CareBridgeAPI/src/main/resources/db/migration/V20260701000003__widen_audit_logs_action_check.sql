-- Widen audit_logs_action_check to match the current AuditAction enum.
-- COMMUNITY_QUESTION_EDITED, COMMUNITY_ANSWER_LIKED, COMMUNITY_BOOKMARK_TOGGLED and
-- COMMUNITY_ANSWER_UNLIKED were added to the Java enum without a matching migration,
-- so those actions currently fail this CHECK constraint at insert time.
-- This migration also adds the three new actions introduced for UC-170/UC-200/UC-201.

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
            'COMMUNITY_QUESTION_EDITED',
            'COMMUNITY_ANSWER_POSTED',
            'COMMUNITY_ANSWER_LIKED',
            'COMMUNITY_BOOKMARK_TOGGLED',
            'COMMUNITY_ANSWER_UNLIKED',
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
            'NOTIFICATION_PREFERENCES_VIEWED',
            'COMMUNITY_QUESTION_DELETED',
            'COMMUNITY_ANSWER_EDITED',
            'COMMUNITY_ANSWER_DELETED'
        ])::text[])
    );

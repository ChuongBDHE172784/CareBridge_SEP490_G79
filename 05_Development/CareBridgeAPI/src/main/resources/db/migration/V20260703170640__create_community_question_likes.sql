-- LikeCommunityQuestion: per-user idempotent toggle, mirrors community_answer_likes (UC-59)
CREATE TABLE community_question_likes (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    question_id UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_question_like_user     FOREIGN KEY (user_id)     REFERENCES users(user_id)          ON DELETE CASCADE,
    CONSTRAINT fk_question_like_question FOREIGN KEY (question_id) REFERENCES community_questions(id) ON DELETE CASCADE,
    CONSTRAINT uq_question_like          UNIQUE (user_id, question_id)
);

CREATE INDEX idx_community_question_likes_question ON community_question_likes(question_id);
CREATE INDEX idx_community_question_likes_user     ON community_question_likes(user_id);

-- Widen audit_logs_action_check in the SAME migration as the AuditAction enum change
-- (lesson learned from UC-106/UC-107 drift — see V20260702001000 comment).
ALTER TABLE public.audit_logs
    DROP CONSTRAINT IF EXISTS audit_logs_action_check;

ALTER TABLE public.audit_logs
    ADD CONSTRAINT audit_logs_action_check CHECK (
        (action)::text = ANY ((ARRAY[
            'LOGIN','LOGOUT','OTP_SENT','OTP_VERIFIED','OTP_RESENT',
            'CONSENT_GRANTED','CONSENT_REVOKED','CREATE_HEALTH_RECORD','VIEW_HEALTH_RECORD',
            'EXPERT_VERIFICATION','MODERATION_ACTION','AI_TRIAGE','PAYMENT','SECURITY_EVENT',
            'VIEW_AUDIT_LOG','USER_REGISTRATION_COMPLETED','SESSION_REVOKED',
            'CONTENT_CREATED','CONTENT_UPDATED','CONTENT_HIDDEN','CONTENT_DECIDED',
            'PARTNER_PROFILE_CREATED','COMMUNITY_QUESTION_CREATED','COMMUNITY_QUESTION_EDITED',
            'COMMUNITY_ANSWER_POSTED','COMMUNITY_ANSWER_LIKED','COMMUNITY_BOOKMARK_TOGGLED',
            'COMMUNITY_ANSWER_UNLIKED','MODERATION_QUEUE_VIEWED','PASSWORD_RESET_REQUESTED',
            'PASSWORD_RESET_COMPLETED','PASSWORD_CHANGED','PROFILE_UPDATED','PROFILE_VIEWED',
            'PRIVACY_SETTINGS_ACCESSED','PRIVACY_SETTINGS_UPDATED','NOTIFICATION_SENT',
            'NOTIFICATION_FAILED','SECURITY_INCIDENT_INVESTIGATED','SECURITY_EVENT_REVIEWED',
            'SECURITY_NOTE_ADDED','JOURNEY_CREATED','JOURNEY_UPDATED','BABY_PROFILE_CREATED',
            'HEALTH_RECORD_ADDED','REMINDER_CREATED','CARE_GROUP_CREATED',
            'CARE_GROUP_MEMBER_INVITED','CARE_GROUP_INVITE_ACCEPTED','CARE_GROUP_INVITE_DECLINED',
            'FILE_UPLOADED','NOTIFICATION_PREFERENCES_UPDATED','NOTIFICATION_PREFERENCES_VIEWED',
            'NOTIFICATIONS_READ','COMMUNITY_QUESTION_DELETED','COMMUNITY_ANSWER_EDITED',
            'COMMUNITY_ANSWER_DELETED','RED_FLAG_RULE_CREATED',
            'RED_FLAG_RULE_UPDATED','RED_FLAG_RULE_DELETED',
            'COMMUNITY_QUESTION_LIKED','COMMUNITY_QUESTION_UNLIKED'
        ])::text[])
    );

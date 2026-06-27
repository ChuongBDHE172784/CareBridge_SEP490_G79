-- =============================================================================
-- V2__spec_sync_from_tds.sql
-- Purpose: move approved schema deltas from Function TDS into executable
--          migration history, keeping V1__init_schema.sql as the clean baseline.
-- Source window: approved Function TDS deltas synchronized on 2026-06-26.
-- Notes:
--   - This migration intentionally contains additive and alignment changes only.
--   - Legacy TDS models that conflict with the current repository schema are
--     still excluded pending explicit architecture approval.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 2A. Identity and Profile
-- ---------------------------------------------------------------------------

ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS failed_login_count integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_at timestamptz;

CREATE TABLE public.password_reset_tokens (
    id            uuid        NOT NULL DEFAULT gen_random_uuid(),
    user_id       uuid        NOT NULL,
    token_hash    varchar(64) NOT NULL,
    expires_at    timestamptz NOT NULL,
    used_at       timestamptz,
    attempt_count integer     NOT NULL DEFAULT 0,
    created_at    timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_token_hash_key UNIQUE (token_hash);

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

CREATE INDEX idx_prt_token_hash ON public.password_reset_tokens USING btree (token_hash);
CREATE INDEX idx_prt_user_active ON public.password_reset_tokens USING btree (user_id, expires_at) WHERE (used_at IS NULL);
CREATE INDEX idx_prt_expires_at ON public.password_reset_tokens USING btree (expires_at);

CREATE TABLE public.user_profiles (
    profile_id     uuid         NOT NULL DEFAULT gen_random_uuid(),
    user_id        uuid         NOT NULL,
    display_name   varchar(100),
    avatar_url     varchar(500),
    phone_number   varchar(20),
    date_of_birth  date,
    area           varchar(100),
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT chk_display_name_length CHECK (((display_name IS NULL) OR ((length((display_name)::text) >= 2) AND (length((display_name)::text) <= 100)))),
    CONSTRAINT chk_date_of_birth_range CHECK (((date_of_birth IS NULL) OR ((date_of_birth >= '1900-01-01'::date) AND (date_of_birth < CURRENT_DATE))))
);

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_pkey PRIMARY KEY (profile_id);

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_user_id_key UNIQUE (user_id);

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

CREATE INDEX idx_user_profiles_user_id ON public.user_profiles USING btree (user_id);
CREATE INDEX idx_user_profiles_display_name_search ON public.user_profiles USING btree (display_name text_pattern_ops);

ALTER TABLE public.user_sessions
    ADD COLUMN IF NOT EXISTS browser varchar(100),
    ADD COLUMN IF NOT EXISTS location varchar(150),
    ADD COLUMN IF NOT EXISTS last_activity_at timestamptz;

ALTER TABLE ONLY public.user_sessions
    ADD CONSTRAINT user_sessions_refresh_token_hash_key UNIQUE (refresh_token_hash);

CREATE INDEX idx_sessions_user_id ON public.user_sessions USING btree (user_id);
CREATE INDEX idx_sessions_token_hash ON public.user_sessions USING btree (refresh_token_hash);
CREATE INDEX idx_sessions_expires_status ON public.user_sessions USING btree (expires_at, status);
CREATE INDEX idx_user_sessions_user_revoked_activity ON public.user_sessions USING btree (user_id, status, last_activity_at DESC) WHERE ((status)::text <> 'REVOKED'::text);

-- ---------------------------------------------------------------------------
-- 2B. Notifications and User Preferences
-- ---------------------------------------------------------------------------

ALTER TABLE public.notification_preferences
    ADD COLUMN IF NOT EXISTS channel varchar(30) NOT NULL DEFAULT 'IN_APP',
    ADD COLUMN IF NOT EXISTS category varchar(50) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS enabled boolean NOT NULL DEFAULT true;

ALTER TABLE public.notification_preferences
    ADD CONSTRAINT notification_preferences_channel_check CHECK (((channel)::text = ANY ((ARRAY['PUSH'::varchar, 'EMAIL'::varchar, 'IN_APP'::varchar])::text[])));

ALTER TABLE public.notification_preferences
    ADD CONSTRAINT notification_preferences_category_check CHECK (((category)::text = ANY ((ARRAY['SYSTEM'::varchar, 'REMINDER'::varchar, 'COMMUNITY'::varchar, 'CONSULTATION'::varchar, 'EMERGENCY'::varchar, 'SAFETY'::varchar])::text[])));

CREATE UNIQUE INDEX idx_notif_pref_user_channel_cat ON public.notification_preferences USING btree (user_id, channel, category);
CREATE INDEX idx_notif_pref_user_id ON public.notification_preferences USING btree (user_id);

CREATE TABLE public.device_tokens (
    id         uuid         NOT NULL DEFAULT gen_random_uuid(),
    user_id    uuid         NOT NULL,
    token      varchar(512) NOT NULL,
    platform   varchar(30)  NOT NULL,
    active     boolean      NOT NULL DEFAULT true,
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT device_tokens_platform_check CHECK (((platform)::text = ANY ((ARRAY['ANDROID'::varchar, 'IOS'::varchar, 'WEB'::varchar])::text[])))
);

ALTER TABLE ONLY public.device_tokens
    ADD CONSTRAINT device_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.device_tokens
    ADD CONSTRAINT device_tokens_unique UNIQUE (user_id, token);

ALTER TABLE ONLY public.device_tokens
    ADD CONSTRAINT device_tokens_user_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

CREATE INDEX idx_device_tokens_user_id ON public.device_tokens USING btree (user_id);

ALTER TABLE public.notifications
    ADD COLUMN IF NOT EXISTS read_at timestamptz,
    ADD COLUMN IF NOT EXISTS metadata jsonb;

CREATE INDEX idx_notifications_user_id ON public.notifications USING btree (recipient_user_id);
CREATE INDEX idx_notifications_user_unread ON public.notifications USING btree (recipient_user_id, is_read) WHERE (is_read = false);
CREATE INDEX idx_notifications_created_at ON public.notifications USING btree (recipient_user_id, created_at DESC);

CREATE TABLE public.question_notification_mutes (
    id          uuid        NOT NULL DEFAULT gen_random_uuid(),
    user_id     uuid        NOT NULL,
    question_id uuid        NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE ONLY public.question_notification_mutes
    ADD CONSTRAINT question_notification_mutes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.question_notification_mutes
    ADD CONSTRAINT uq_question_mute UNIQUE (user_id, question_id);

ALTER TABLE ONLY public.question_notification_mutes
    ADD CONSTRAINT fk_mute_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

ALTER TABLE ONLY public.question_notification_mutes
    ADD CONSTRAINT fk_mute_question FOREIGN KEY (question_id) REFERENCES public.community_questions(id) ON DELETE CASCADE;

CREATE INDEX idx_question_mutes_user_question ON public.question_notification_mutes USING btree (user_id, question_id);

-- ---------------------------------------------------------------------------
-- 2C. AI Triage and Emergency
-- ---------------------------------------------------------------------------

CREATE TABLE public.intake_sessions (
    id              uuid        NOT NULL DEFAULT gen_random_uuid(),
    user_id         uuid        NOT NULL,
    baby_profile_id uuid,
    symptoms        text        NOT NULL,
    raw_ai_response text,
    risk_level      varchar(10),
    status          varchar(20) NOT NULL DEFAULT 'PENDING',
    disclaimer      text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    completed_at    timestamptz,
    created_by      uuid        NOT NULL,
    CONSTRAINT intake_sessions_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::varchar, 'PROCESSING'::varchar, 'COMPLETED'::varchar, 'FAILED'::varchar])::text[]))),
    CONSTRAINT intake_sessions_risk_level_check CHECK (((risk_level IS NULL) OR ((risk_level)::text = ANY ((ARRAY['GREEN'::varchar, 'YELLOW'::varchar, 'RED'::varchar])::text[]))))
);

ALTER TABLE ONLY public.intake_sessions
    ADD CONSTRAINT intake_sessions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.intake_sessions
    ADD CONSTRAINT intake_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.intake_sessions
    ADD CONSTRAINT intake_sessions_baby_profile_id_fkey FOREIGN KEY (baby_profile_id) REFERENCES public.baby_profiles(baby_id);

ALTER TABLE ONLY public.intake_sessions
    ADD CONSTRAINT intake_sessions_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(user_id);

CREATE INDEX idx_intake_sessions_user_id ON public.intake_sessions USING btree (user_id);
CREATE INDEX idx_intake_sessions_created_at ON public.intake_sessions USING btree (created_at DESC);

CREATE TABLE public.structured_intake_data (
    id             uuid        NOT NULL DEFAULT gen_random_uuid(),
    session_id     uuid        NOT NULL,
    symptom_list   jsonb       NOT NULL,
    duration_days  integer,
    intensity      varchar(20),
    emergency_flag boolean     NOT NULL DEFAULT false,
    extracted_at   timestamptz NOT NULL DEFAULT now(),
    created_by     varchar(50) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT structured_intake_data_intensity_check CHECK (((intensity IS NULL) OR ((intensity)::text = ANY ((ARRAY['LOW'::varchar, 'MEDIUM'::varchar, 'HIGH'::varchar])::text[]))))
);

ALTER TABLE ONLY public.structured_intake_data
    ADD CONSTRAINT structured_intake_data_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.structured_intake_data
    ADD CONSTRAINT structured_intake_data_session_id_key UNIQUE (session_id);

ALTER TABLE ONLY public.structured_intake_data
    ADD CONSTRAINT fk_structured_session FOREIGN KEY (session_id) REFERENCES public.intake_sessions(id);

CREATE INDEX idx_structured_intake_session_id ON public.structured_intake_data USING btree (session_id);
CREATE INDEX idx_structured_intake_emergency ON public.structured_intake_data USING btree (emergency_flag) WHERE (emergency_flag = true);

CREATE TABLE public.emergency_sessions (
    id             uuid         NOT NULL DEFAULT gen_random_uuid(),
    user_id        uuid         NOT NULL,
    status         varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    trigger_source varchar(50)  NOT NULL,
    user_latitude  numeric(10,7),
    user_longitude numeric(10,7),
    created_at     timestamptz  NOT NULL DEFAULT now(),
    resolved_at    timestamptz,
    created_by     uuid         NOT NULL,
    CONSTRAINT emergency_sessions_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::varchar, 'RESOLVED'::varchar, 'CANCELLED'::varchar])::text[]))),
    CONSTRAINT emergency_sessions_trigger_source_check CHECK (((trigger_source)::text = ANY ((ARRAY['MANUAL'::varchar, 'AUTO_TRIAGE'::varchar, 'FALL_DETECTION'::varchar])::text[])))
);

ALTER TABLE ONLY public.emergency_sessions
    ADD CONSTRAINT emergency_sessions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.emergency_sessions
    ADD CONSTRAINT emergency_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.emergency_sessions
    ADD CONSTRAINT emergency_sessions_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(user_id);

CREATE INDEX idx_emergency_sessions_user_id ON public.emergency_sessions USING btree (user_id);
CREATE INDEX idx_emergency_sessions_status ON public.emergency_sessions USING btree (status) WHERE ((status)::text = 'ACTIVE'::text);
CREATE INDEX idx_emergency_sessions_created_at ON public.emergency_sessions USING btree (created_at DESC);

CREATE TABLE public.imu_monitoring_sessions (
    id                uuid        NOT NULL DEFAULT gen_random_uuid(),
    user_id           uuid        NOT NULL,
    status            varchar(10) NOT NULL DEFAULT 'ACTIVE',
    sensitivity_level varchar(10) NOT NULL DEFAULT 'MEDIUM',
    started_at        timestamptz NOT NULL DEFAULT now(),
    ended_at          timestamptz,
    created_by        uuid        NOT NULL,
    CONSTRAINT imu_monitoring_sessions_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::varchar, 'STOPPED'::varchar])::text[]))),
    CONSTRAINT imu_monitoring_sessions_sensitivity_check CHECK (((sensitivity_level)::text = ANY ((ARRAY['LOW'::varchar, 'MEDIUM'::varchar, 'HIGH'::varchar])::text[])))
);

ALTER TABLE ONLY public.imu_monitoring_sessions
    ADD CONSTRAINT imu_monitoring_sessions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.imu_monitoring_sessions
    ADD CONSTRAINT imu_monitoring_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.imu_monitoring_sessions
    ADD CONSTRAINT imu_monitoring_sessions_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(user_id);

CREATE INDEX idx_imu_sessions_user_id ON public.imu_monitoring_sessions USING btree (user_id);
CREATE INDEX idx_imu_sessions_active ON public.imu_monitoring_sessions USING btree (user_id, status) WHERE ((status)::text = 'ACTIVE'::text);

ALTER TABLE public.safety_events
    ADD COLUMN IF NOT EXISTS imu_session_id uuid,
    ADD COLUMN IF NOT EXISTS magnitude numeric(10,4),
    ADD COLUMN IF NOT EXISTS user_latitude numeric(10,7),
    ADD COLUMN IF NOT EXISTS user_longitude numeric(10,7),
    ADD COLUMN IF NOT EXISTS notes text,
    ADD COLUMN IF NOT EXISTS created_by varchar(50) NOT NULL DEFAULT 'SYSTEM';

ALTER TABLE ONLY public.safety_events
    ADD CONSTRAINT safety_events_imu_session_id_fkey FOREIGN KEY (imu_session_id) REFERENCES public.imu_monitoring_sessions(id);

-- ---------------------------------------------------------------------------
-- 2D. Security and Moderation
-- ---------------------------------------------------------------------------

ALTER TABLE public.content_reports
    RENAME COLUMN reason_code TO category;

ALTER TABLE public.content_reports
    ALTER COLUMN category TYPE varchar(50);

ALTER TABLE public.content_reports
    ADD CONSTRAINT chk_target_type CHECK (((target_type)::text = ANY ((ARRAY['QUESTION'::varchar, 'ANSWER'::varchar, 'CONTENT'::varchar, 'EXPERT'::varchar, 'USER'::varchar])::text[])));

ALTER TABLE public.content_reports
    ADD CONSTRAINT chk_category CHECK (((category)::text = ANY ((ARRAY['INACCURATE_INFORMATION'::varchar, 'DISGUISED_ADVERTISING'::varchar, 'HARASSMENT'::varchar, 'UNSAFE_ADVICE'::varchar, 'SPAM'::varchar, 'OTHER'::varchar])::text[])));

CREATE INDEX idx_content_reports_rate_limit ON public.content_reports USING btree (reporter_user_id, target_id, created_at DESC);
CREATE INDEX idx_content_reports_duplicate ON public.content_reports USING btree (reporter_user_id, target_id, status) WHERE ((status)::text = 'PENDING'::text);
CREATE INDEX idx_content_reports_status_created ON public.content_reports USING btree (status, created_at DESC) WHERE ((status)::text = 'PENDING'::text);

ALTER TABLE public.security_events
    RENAME COLUMN "timestamp" TO occurred_at;

ALTER TABLE public.security_events
    ADD COLUMN IF NOT EXISTS user_agent varchar(500),
    ADD COLUMN IF NOT EXISTS payload jsonb,
    ADD COLUMN IF NOT EXISTS correlation_id uuid,
    ADD COLUMN IF NOT EXISTS severity varchar(20) NOT NULL DEFAULT 'MEDIUM',
    ADD COLUMN IF NOT EXISTS status varchar(20) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN IF NOT EXISTS reviewed_by uuid,
    ADD COLUMN IF NOT EXISTS reviewed_at timestamptz;

ALTER TABLE public.security_events
    ADD CONSTRAINT security_events_severity_check CHECK (((severity)::text = ANY ((ARRAY['LOW'::varchar, 'MEDIUM'::varchar, 'HIGH'::varchar, 'CRITICAL'::varchar])::text[])));

ALTER TABLE public.security_events
    ADD CONSTRAINT security_events_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::varchar, 'UNDER_REVIEW'::varchar, 'RESOLVED'::varchar, 'FALSE_POSITIVE'::varchar])::text[])));

ALTER TABLE ONLY public.security_events
    ADD CONSTRAINT security_events_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.users(user_id);

CREATE TABLE public.security_event_notes (
    note_id     uuid        NOT NULL DEFAULT gen_random_uuid(),
    event_id    bigint      NOT NULL,
    author_id   uuid        NOT NULL,
    note_text   text        NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT security_event_notes_note_text_check CHECK ((char_length(TRIM(BOTH FROM note_text)) > 0))
);

ALTER TABLE ONLY public.security_event_notes
    ADD CONSTRAINT security_event_notes_pkey PRIMARY KEY (note_id);

ALTER TABLE ONLY public.security_event_notes
    ADD CONSTRAINT security_event_notes_event_id_fkey FOREIGN KEY (event_id) REFERENCES public.security_events(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.security_event_notes
    ADD CONSTRAINT security_event_notes_author_id_fkey FOREIGN KEY (author_id) REFERENCES public.users(user_id);

CREATE INDEX idx_security_events_user_id ON public.security_events USING btree (user_id, occurred_at DESC);
CREATE INDEX idx_security_events_event_type ON public.security_events USING btree (event_type, occurred_at DESC);
CREATE INDEX idx_security_events_correlation_id ON public.security_events USING btree (correlation_id);
CREATE INDEX idx_security_events_ip_address ON public.security_events USING btree (ip_address);
CREATE INDEX idx_security_events_status_severity ON public.security_events USING btree (status, severity);
CREATE INDEX idx_security_events_status_occurred ON public.security_events USING btree (status, occurred_at DESC);
CREATE INDEX idx_security_event_notes_event_id ON public.security_event_notes USING btree (event_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- 2E. Privacy, Verification, and Billing
-- ---------------------------------------------------------------------------

CREATE TABLE public.privacy_settings (
    id                       uuid         NOT NULL DEFAULT gen_random_uuid(),
    user_id                  uuid         NOT NULL,
    profile_visibility       varchar(20)  NOT NULL DEFAULT 'FRIENDS_ONLY',
    location_sharing_enabled boolean      NOT NULL DEFAULT false,
    analytics_consent        boolean      NOT NULL DEFAULT false,
    data_export_opt_out      boolean      NOT NULL DEFAULT false,
    created_at               timestamptz  NOT NULL DEFAULT now(),
    updated_at               timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT privacy_settings_visibility_check CHECK (((profile_visibility)::text = ANY ((ARRAY['PUBLIC'::varchar, 'FRIENDS_ONLY'::varchar, 'PRIVATE'::varchar])::text[])))
);

ALTER TABLE ONLY public.privacy_settings
    ADD CONSTRAINT privacy_settings_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.privacy_settings
    ADD CONSTRAINT privacy_settings_user_id_key UNIQUE (user_id);

ALTER TABLE ONLY public.privacy_settings
    ADD CONSTRAINT fk_privacy_settings_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;

CREATE INDEX idx_privacy_settings_user_id ON public.privacy_settings USING btree (user_id);

CREATE TABLE public.expert_verification_documents (
    id                 uuid         NOT NULL DEFAULT gen_random_uuid(),
    expert_profile_id  uuid         NOT NULL,
    doc_type           varchar(30)  NOT NULL,
    storage_key        varchar(500) NOT NULL,
    original_name      varchar(255) NOT NULL,
    mime_type          varchar(100) NOT NULL,
    size_bytes         bigint       NOT NULL,
    status             varchar(30)  NOT NULL DEFAULT 'PENDING_REVIEW',
    reject_reason      text,
    uploaded_at        timestamptz  NOT NULL DEFAULT now(),
    reviewed_at        timestamptz,
    reviewed_by        uuid,
    CONSTRAINT expert_verification_documents_doc_type_check CHECK (((doc_type)::text = ANY ((ARRAY['DEGREE'::varchar, 'CERTIFICATE'::varchar, 'LICENSE'::varchar, 'IDENTITY'::varchar, 'OTHER'::varchar])::text[]))),
    CONSTRAINT expert_verification_documents_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_REVIEW'::varchar, 'APPROVED'::varchar, 'REJECTED'::varchar])::text[])))
);

ALTER TABLE ONLY public.expert_verification_documents
    ADD CONSTRAINT expert_verification_documents_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.expert_verification_documents
    ADD CONSTRAINT expert_verification_documents_expert_profile_id_fkey FOREIGN KEY (expert_profile_id) REFERENCES public.expert_profiles(expert_profile_id) ON DELETE CASCADE;

ALTER TABLE ONLY public.expert_verification_documents
    ADD CONSTRAINT expert_verification_documents_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.users(user_id);

CREATE INDEX idx_expert_docs_expert ON public.expert_verification_documents USING btree (expert_profile_id);

CREATE TABLE public.commission_config (
    id              uuid         NOT NULL DEFAULT gen_random_uuid(),
    platform_rate   numeric(5,4) NOT NULL DEFAULT 0.2000,
    effective_from  timestamptz  NOT NULL DEFAULT now(),
    created_by      uuid         NOT NULL,
    CONSTRAINT commission_config_platform_rate_check CHECK (((platform_rate >= 0.0000) AND (platform_rate <= 1.0000)))
);

ALTER TABLE ONLY public.commission_config
    ADD CONSTRAINT commission_config_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.commission_config
    ADD CONSTRAINT commission_config_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(user_id);

CREATE INDEX idx_commission_config_effective_from ON public.commission_config USING btree (effective_from DESC);

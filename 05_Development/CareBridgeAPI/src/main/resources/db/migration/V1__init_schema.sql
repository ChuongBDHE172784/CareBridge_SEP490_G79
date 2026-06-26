-- =============================================================================
-- V1__init_schema.sql — CareBridge master database schema baseline
-- Generated: 2026-06-25
-- Sources:
--   CORE 25 tables : V1__baseline.sql (Supabase-verified, carried byte-for-byte)
--   FUTURE 46 tables: CareBridge_ERD_Logical_Model_Updated.puml (ERD-aligned DDL)
-- Coverage: 71 tables (67 ERD tables + 4 infrastructure tables)
-- Note: 46 future tables are schema-only provisions. No corresponding Java
--       @Entity mappings exist yet. Hibernate ddl-auto:validate ignores them.
-- ERD divergences documented:
--   users                : enabled/locked/role (not account_status/email_verified)
--   partner_organizations: UC-56 schema shape (not ERD schema shape)
--   community_topics     : PK column named 'id' (not topic_id)
--   community_questions  : PK column named 'id' (not question_id)
--   community_answers    : PK 'id'; is_expert_labeled/is_personal_experience (not answer_type)
-- Excluded: token_blacklist (not in ERD; revocation covered by user_sessions.revoked_at)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS public;


-- ============================================================================
-- PART 1: CORE + CURRENT IMPLEMENTATION (25 TABLES)
-- Carried byte-for-byte from V1__baseline.sql per Q5 decision.
-- ============================================================================

--
-- Name: audit_logs; Type: TABLE
--

CREATE TABLE public.audit_logs (
    audit_log_id uuid NOT NULL,
    action character varying(80) NOT NULL,
    actor_user_id uuid,
    created_at timestamp(6) with time zone NOT NULL,
    entity_id uuid,
    entity_type character varying(100),
    ip_address character varying(80),
    new_value_json jsonb,
    old_value_json jsonb,
    CONSTRAINT audit_logs_action_check CHECK (((action)::text = ANY ((ARRAY['LOGIN'::character varying, 'LOGOUT'::character varying, 'OTP_SENT'::character varying, 'OTP_VERIFIED'::character varying, 'CONSENT_GRANTED'::character varying, 'CONSENT_REVOKED'::character varying, 'CREATE_HEALTH_RECORD'::character varying, 'VIEW_HEALTH_RECORD'::character varying, 'EXPERT_VERIFICATION'::character varying, 'MODERATION_ACTION'::character varying, 'AI_TRIAGE'::character varying, 'PAYMENT'::character varying, 'SECURITY_EVENT'::character varying, 'VIEW_AUDIT_LOG'::character varying])::text[])))
);


--
-- Name: checklist_items; Type: TABLE
--

CREATE TABLE public.checklist_items (
    checklist_item_id uuid NOT NULL,
    checklist_template_id uuid,
    created_at timestamp(6) with time zone NOT NULL,
    is_required boolean,
    item_order integer,
    item_text character varying(500),
    note character varying(500),
    updated_at timestamp(6) with time zone
);


--
-- Name: checklist_templates; Type: TABLE
--

CREATE TABLE public.checklist_templates (
    checklist_template_id uuid NOT NULL,
    content_item_id uuid,
    created_at timestamp(6) with time zone NOT NULL,
    name character varying(200),
    stage character varying(30),
    status character varying(20) NOT NULL,
    updated_at timestamp(6) with time zone,
    version_no integer,
    description text
);


--
-- Name: community_answers; Type: TABLE
--

CREATE TABLE public.community_answers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    question_id uuid NOT NULL,
    author_id uuid NOT NULL,
    body text NOT NULL,
    is_expert_labeled boolean DEFAULT false NOT NULL,
    is_personal_experience boolean NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    like_count integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT community_answers_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'HIDDEN'::character varying])::text[])))
);


--
-- Name: community_profiles; Type: TABLE
--

CREATE TABLE public.community_profiles (
    community_profile_id uuid NOT NULL,
    bio character varying(500),
    created_at timestamp(6) with time zone NOT NULL,
    display_name character varying(100),
    interest_stage character varying(30),
    is_visible boolean,
    public_avatar_url character varying(500),
    region character varying(120),
    updated_at timestamp(6) with time zone,
    user_id uuid
);


--
-- Name: community_questions; Type: TABLE
--

CREATE TABLE public.community_questions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    topic_id uuid NOT NULL,
    author_id uuid NOT NULL,
    title character varying(255) NOT NULL,
    body text NOT NULL,
    stage character varying(30) NOT NULL,
    pregnancy_week smallint,
    baby_age_months smallint,
    urgency character varying(20) NOT NULL,
    is_anonymous boolean DEFAULT false NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    like_count integer DEFAULT 0 NOT NULL,
    answer_count integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT community_questions_baby_age_months_check CHECK (((baby_age_months >= 0) AND (baby_age_months <= 72))),
    CONSTRAINT community_questions_pregnancy_week_check CHECK (((pregnancy_week >= 1) AND (pregnancy_week <= 42))),
    CONSTRAINT community_questions_stage_check CHECK (((stage)::text = ANY ((ARRAY['PRE_PREGNANCY'::character varying, 'PREGNANCY'::character varying, 'POSTPARTUM'::character varying, 'BABY_CARE'::character varying])::text[]))),
    CONSTRAINT community_questions_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'HIDDEN'::character varying, 'LOCKED'::character varying])::text[]))),
    CONSTRAINT community_questions_urgency_check CHECK (((urgency)::text = ANY ((ARRAY['LOW'::character varying, 'NORMAL'::character varying, 'URGENT'::character varying])::text[])))
);


--
-- Name: community_topics; Type: TABLE
--

CREATE TABLE public.community_topics (
    id uuid NOT NULL,
    created_at timestamp with time zone NOT NULL,
    description text,
    name character varying(100) NOT NULL,
    updated_at timestamp with time zone DEFAULT now(),
    is_hidden boolean DEFAULT false NOT NULL,
    icon character varying(255),
    sort_order integer DEFAULT 0 NOT NULL,
    created_by uuid
);


--
-- Name: consent_grants; Type: TABLE
--

CREATE TABLE public.consent_grants (
    id bigint NOT NULL,
    consent_given_at timestamp(6) with time zone NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    data_type character varying(60) NOT NULL,
    expiry_at timestamp(6) with time zone NOT NULL,
    purpose character varying(60) NOT NULL,
    recipient character varying(120),
    revoked_at timestamp(6) with time zone,
    revoked_by uuid,
    scope_text text,
    updated_at timestamp(6) with time zone NOT NULL,
    user_id uuid NOT NULL,
    version integer NOT NULL,
    CONSTRAINT consent_grants_data_type_check CHECK (((data_type)::text = ANY ((ARRAY['HEALTH_RECORD'::character varying, 'LOCATION'::character varying, 'FAMILY_DATA'::character varying, 'COMMUNITY_POST'::character varying, 'SENSITIVE_DATA'::character varying, 'RAG_CONTEXT'::character varying, 'EXPERT_SHARED_DATA'::character varying])::text[]))),
    CONSTRAINT consent_grants_purpose_check CHECK (((purpose)::text = ANY ((ARRAY['VIEW'::character varying, 'CREATE'::character varying, 'UPDATE'::character varying, 'SHARE'::character varying, 'DELETE'::character varying])::text[])))
);


--
-- Name: consent_grants_id_seq; Type: SEQUENCE
--

ALTER TABLE public.consent_grants ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.consent_grants_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: content_items; Type: TABLE
--

CREATE TABLE public.content_items (
    content_item_id uuid NOT NULL,
    author_user_id uuid,
    body text,
    content_type character varying(30),
    created_at timestamp(6) with time zone NOT NULL,
    published_at timestamp(6) with time zone,
    source_label character varying(255),
    status character varying(20) NOT NULL,
    title character varying(250),
    topic_id uuid,
    updated_at timestamp(6) with time zone,
    version_no integer,
    stage character varying(30)
);


--
-- Name: content_reports; Type: TABLE
--

CREATE TABLE public.content_reports (
    report_id uuid NOT NULL,
    assigned_moderator_id uuid,
    created_at timestamp(6) with time zone NOT NULL,
    description text,
    reason_code character varying(40),
    reporter_user_id uuid,
    resolved_at timestamp(6) with time zone,
    status character varying(20) NOT NULL,
    target_id uuid,
    target_type character varying(30),
    updated_at timestamp(6) with time zone
);


--
-- Name: contribution_points; Type: TABLE
--

CREATE TABLE public.contribution_points (
    point_record_id uuid NOT NULL,
    points integer,
    reason character varying(255),
    recorded_at timestamp(6) with time zone,
    source_id uuid,
    source_type character varying(40),
    user_id uuid
);


--
-- Name: data_permissions; Type: TABLE
--

CREATE TABLE public.data_permissions (
    permission_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone,
    granted_at timestamp(6) with time zone,
    grantee_user_id uuid,
    owner_user_id uuid,
    purpose character varying(255),
    revoked_at timestamp(6) with time zone,
    scope_reference_id uuid,
    scope_type character varying(50),
    status character varying(20) NOT NULL,
    updated_at timestamp(6) with time zone
);


--
-- Name: moderation_actions; Type: TABLE
--

CREATE TABLE public.moderation_actions (
    moderation_action_id uuid NOT NULL,
    action_at timestamp(6) with time zone,
    action_type character varying(30),
    expires_at timestamp(6) with time zone,
    moderator_user_id uuid,
    reason text,
    report_id uuid,
    target_id uuid,
    target_type character varying(30)
);


--
-- Name: notification_preferences; Type: TABLE
--

CREATE TABLE public.notification_preferences (
    preference_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    email_enabled boolean,
    in_app_enabled boolean,
    notification_type character varying(50),
    push_enabled boolean,
    quiet_hours_end time(0) without time zone,
    quiet_hours_start time(0) without time zone,
    updated_at timestamp(6) with time zone,
    user_id uuid
);


--
-- Name: notifications; Type: TABLE
--

CREATE TABLE public.notifications (
    notification_id uuid NOT NULL,
    body text,
    created_at timestamp(6) with time zone NOT NULL,
    delivery_status character varying(20),
    is_read boolean,
    notification_type character varying(50),
    recipient_user_id uuid,
    reference_id uuid,
    reference_type character varying(50),
    sent_at timestamp(6) with time zone,
    title character varying(200),
    updated_at timestamp(6) with time zone
);


--
-- Name: otp_verifications; Type: TABLE
--

CREATE TABLE public.otp_verifications (
    id bigint NOT NULL,
    attempts integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    email character varying(255),
    expires_at timestamp(6) with time zone NOT NULL,
    otp_code character varying(10) NOT NULL,
    phone character varying(20) NOT NULL,
    purpose character varying(20) NOT NULL,
    requested_role character varying(40),
    verified boolean NOT NULL,
    verified_at timestamp(6) with time zone,
    CONSTRAINT otp_verifications_purpose_check CHECK (((purpose)::text = ANY ((ARRAY['REGISTER'::character varying, 'LOGIN'::character varying])::text[]))),
    CONSTRAINT otp_verifications_requested_role_check CHECK (((requested_role)::text = ANY ((ARRAY['MOTHER'::character varying, 'FAMILY'::character varying, 'EXPERT'::character varying, 'MODERATOR'::character varying, 'CONTENT_ADMIN'::character varying, 'SYSTEM_ADMIN'::character varying, 'PARTNER'::character varying])::text[])))
);


--
-- Name: otp_verifications_id_seq; Type: SEQUENCE
--

ALTER TABLE public.otp_verifications ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.otp_verifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: partner_organizations; Type: TABLE
--

CREATE TABLE public.partner_organizations (
    partner_id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    address character varying(500) NOT NULL,
    city character varying(100) NOT NULL,
    phone character varying(20) NOT NULL,
    email character varying(255) NOT NULL,
    website character varying(500),
    logo_url character varying(1000),
    description character varying(2000),
    status character varying(30) DEFAULT 'PENDING_APPROVAL'::character varying NOT NULL,
    representative_user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: refresh_tokens; Type: TABLE
--

CREATE TABLE public.refresh_tokens (
    id bigint NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    revoked boolean NOT NULL,
    token character varying(160) NOT NULL,
    user_id uuid NOT NULL
);


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE
--

ALTER TABLE public.refresh_tokens ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: roles; Type: TABLE
--

CREATE TABLE public.roles (
    role_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    description character varying(500),
    is_active boolean,
    role_code character varying(50),
    role_name character varying(100),
    updated_at timestamp(6) with time zone
);


--
-- Name: security_events; Type: TABLE
--

CREATE TABLE public.security_events (
    id bigint NOT NULL,
    details text,
    event_type character varying(80) NOT NULL,
    ip_address character varying(80),
    "timestamp" timestamp(6) with time zone NOT NULL,
    user_id uuid,
    CONSTRAINT security_events_event_type_check CHECK (((event_type)::text = ANY ((ARRAY['LOGIN_FAILED'::character varying, 'PERMISSION_DENIED'::character varying, 'SUSPICIOUS_ACTIVITY'::character varying, 'TOKEN_REVOKED'::character varying, 'OTP_ATTEMPT_LIMIT_EXCEEDED'::character varying])::text[])))
);


--
-- Name: security_events_id_seq; Type: SEQUENCE
--

ALTER TABLE public.security_events ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.security_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: triage_answers; Type: TABLE
--

CREATE TABLE public.triage_answers (
    triage_answer_id uuid NOT NULL,
    answer_order integer,
    answer_value text,
    assessment_id uuid,
    created_at timestamp(6) with time zone NOT NULL,
    question_code character varying(80),
    question_text character varying(500),
    red_flag_triggered boolean,
    updated_at timestamp(6) with time zone
);


--
-- Name: triage_assessments; Type: TABLE
--

CREATE TABLE public.triage_assessments (
    assessment_id uuid NOT NULL,
    baby_id uuid,
    completed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    disclaimer_accepted boolean,
    journey_id uuid,
    recommended_action text,
    risk_level character varying(10),
    rule_version character varying(50),
    symptom_summary text,
    updated_at timestamp(6) with time zone,
    user_id uuid
);


--
-- Name: user_roles; Type: TABLE
--

CREATE TABLE public.user_roles (
    user_role_id uuid NOT NULL,
    assigned_at timestamp(6) with time zone,
    assigned_by uuid,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone,
    role_id uuid,
    status character varying(20) NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id uuid
);


--
-- Name: user_sessions; Type: TABLE
--

CREATE TABLE public.user_sessions (
    session_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    device_name character varying(150),
    expires_at timestamp(6) with time zone,
    ip_address character varying(64),
    refresh_token_hash character varying(255),
    revoked_at timestamp(6) with time zone,
    status character varying(20) NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id uuid
);


--
-- Name: users; Type: TABLE
--

CREATE TABLE public.users (
    user_id uuid NOT NULL,
    avatar_url character varying(500),
    created_at timestamp(6) with time zone NOT NULL,
    email character varying(255),
    full_name character varying(150),
    password_hash character varying(255),
    phone character varying(30),
    updated_at timestamp(6) with time zone NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    locked boolean DEFAULT false NOT NULL,
    role character varying(50) DEFAULT 'MOTHER'::character varying NOT NULL
);


-- ============================================================================
-- PART 2: COMPLETE ERD BASELINE (46 NEW TABLES)
-- ERD-aligned DDL from CareBridge_ERD_Logical_Model_Updated.puml.
-- No Java @Entity mappings yet; Hibernate ddl-auto:validate ignores these.
-- FK constraints applied at the end of this file (after all tables created).
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 2A. CARE JOURNEY (3 tables)
-- ----------------------------------------------------------------------------

CREATE TABLE public.mother_journeys (
    journey_id          uuid        NOT NULL DEFAULT gen_random_uuid(),
    owner_user_id       uuid        NOT NULL,
    journey_type        varchar(20) NOT NULL,
    start_date          date,
    last_menstrual_date date,
    estimated_due_date  date,
    delivery_date       date,
    status              varchar(20) NOT NULL DEFAULT 'ACTIVE',
    notes               text,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.maternal_health_metrics (
    metric_id           uuid        NOT NULL DEFAULT gen_random_uuid(),
    journey_id          uuid        NOT NULL,
    metric_type         varchar(50) NOT NULL,
    value_numeric       numeric,
    value_secondary     numeric,
    unit                varchar(30),
    measured_at         timestamptz NOT NULL,
    source_type         varchar(30),
    source_reference_id uuid,
    note                text,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.postpartum_logs (
    postpartum_log_id  uuid        NOT NULL DEFAULT gen_random_uuid(),
    journey_id         uuid        NOT NULL,
    log_date           date        NOT NULL,
    pain_level         smallint,
    bleeding_level     varchar(20),
    mood_level         smallint,
    sleep_hours        numeric,
    breastfeeding_note text,
    symptom_note       text,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now()
);


-- ----------------------------------------------------------------------------
-- 2B. BABY CARE (5 tables)
-- ----------------------------------------------------------------------------

CREATE TABLE public.baby_profiles (
    baby_id            uuid         NOT NULL DEFAULT gen_random_uuid(),
    owner_user_id      uuid         NOT NULL,
    related_journey_id uuid,
    nickname           varchar(100) NOT NULL,
    birth_date         date,
    sex                varchar(10),
    birth_weight_kg    numeric,
    birth_length_cm    numeric,
    status             varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at         timestamptz  NOT NULL DEFAULT now(),
    updated_at         timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE public.baby_daily_logs (
    baby_log_id uuid        NOT NULL DEFAULT gen_random_uuid(),
    baby_id     uuid        NOT NULL,
    log_type    varchar(30) NOT NULL,
    started_at  timestamptz,
    ended_at    timestamptz,
    quantity    numeric,
    unit        varchar(20),
    note        text,
    recorded_by uuid,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.development_milestones (
    milestone_id   uuid        NOT NULL DEFAULT gen_random_uuid(),
    baby_id        uuid        NOT NULL,
    milestone_type varchar(80) NOT NULL,
    achieved_date  date,
    note           text,
    source_type    varchar(30),
    recorded_by    uuid,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.growth_measurements (
    growth_measurement_id uuid    NOT NULL DEFAULT gen_random_uuid(),
    baby_id               uuid    NOT NULL,
    measured_date         date    NOT NULL,
    weight_kg             numeric,
    height_cm             numeric,
    head_circumference_cm numeric,
    source_type           varchar(30),
    note                  text,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.vaccination_records (
    vaccination_record_id uuid         NOT NULL DEFAULT gen_random_uuid(),
    baby_id               uuid         NOT NULL,
    vaccine_name          varchar(200) NOT NULL,
    dose_number           smallint,
    scheduled_date        date,
    administered_date     date,
    status                varchar(20)  NOT NULL DEFAULT 'SCHEDULED',
    facility_name         varchar(200),
    proof_record_id       uuid,
    created_at            timestamptz  NOT NULL DEFAULT now(),
    updated_at            timestamptz  NOT NULL DEFAULT now()
);


-- ----------------------------------------------------------------------------
-- 2C. HEALTH RECORDS (2 tables)
-- ----------------------------------------------------------------------------

CREATE TABLE public.health_records (
    health_record_id uuid         NOT NULL DEFAULT gen_random_uuid(),
    owner_user_id    uuid         NOT NULL,
    journey_id       uuid,
    baby_id          uuid,
    record_type      varchar(50)  NOT NULL,
    title            varchar(255) NOT NULL,
    file_url         text,
    record_date      date,
    source_type      varchar(30),
    source_name      varchar(200),
    status           varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE public.health_summaries (
    summary_id     uuid        NOT NULL DEFAULT gen_random_uuid(),
    owner_user_id  uuid        NOT NULL,
    journey_id     uuid,
    baby_id        uuid,
    summary_period varchar(30),
    period_start   date,
    period_end     date,
    summary_json   jsonb,
    generated_by   varchar(50),
    status         varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now()
);


-- ----------------------------------------------------------------------------
-- 2D. CARE COORDINATION (5 tables)
-- ----------------------------------------------------------------------------

CREATE TABLE public.reminders (
    reminder_id     uuid         NOT NULL DEFAULT gen_random_uuid(),
    owner_user_id   uuid         NOT NULL,
    journey_id      uuid,
    baby_id         uuid,
    reminder_type   varchar(50)  NOT NULL,
    title           varchar(255) NOT NULL,
    scheduled_at    timestamptz  NOT NULL,
    recurrence_rule varchar(100),
    status          varchar(20)  NOT NULL DEFAULT 'PENDING',
    snoozed_until   timestamptz,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE public.care_groups (
    care_group_id uuid         NOT NULL DEFAULT gen_random_uuid(),
    owner_user_id uuid         NOT NULL,
    journey_id    uuid,
    baby_id       uuid,
    group_name    varchar(200) NOT NULL,
    status        varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE public.care_group_members (
    care_group_member_id uuid        NOT NULL DEFAULT gen_random_uuid(),
    care_group_id        uuid        NOT NULL,
    user_id              uuid        NOT NULL,
    member_role          varchar(50),
    invitation_status    varchar(20) NOT NULL DEFAULT 'PENDING',
    permission_json      jsonb,
    joined_at            timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.care_tasks (
    care_task_id  uuid         NOT NULL DEFAULT gen_random_uuid(),
    care_group_id uuid         NOT NULL,
    assigned_by   uuid,
    assigned_to   uuid,
    title         varchar(255) NOT NULL,
    description   text,
    due_at        timestamptz,
    status        varchar(20)  NOT NULL DEFAULT 'OPEN',
    completed_at  timestamptz,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE public.expenses (
    expense_id    uuid        NOT NULL DEFAULT gen_random_uuid(),
    owner_user_id uuid        NOT NULL,
    journey_id    uuid,
    baby_id       uuid,
    category      varchar(80),
    amount        numeric     NOT NULL,
    currency      varchar(10) NOT NULL DEFAULT 'VND',
    expense_date  date        NOT NULL,
    note          text,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now()
);


-- ----------------------------------------------------------------------------
-- 2E. EXPERT & CONSULTATION (15 new tables; contribution_points is existing)
-- ----------------------------------------------------------------------------

CREATE TABLE public.expert_profiles (
    expert_profile_id   uuid         NOT NULL DEFAULT gen_random_uuid(),
    user_id             uuid         NOT NULL,
    specialty           varchar(100),
    professional_title  varchar(150),
    experience_years    smallint,
    workplace           varchar(200),
    consultation_scope  text,
    verification_status varchar(30)  NOT NULL DEFAULT 'PENDING',
    verified_at         timestamptz,
    verified_by         uuid,
    rating_avg          numeric,
    created_at          timestamptz  NOT NULL DEFAULT now(),
    updated_at          timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE public.expert_credentials (
    credential_id     uuid        NOT NULL DEFAULT gen_random_uuid(),
    expert_profile_id uuid        NOT NULL,
    credential_type   varchar(50) NOT NULL,
    credential_number varchar(100),
    issuer            varchar(200),
    issued_date       date,
    expiry_date       date,
    file_url          text,
    review_status     varchar(30) NOT NULL DEFAULT 'PENDING',
    review_note       text,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.expert_availability (
    availability_id   uuid        NOT NULL DEFAULT gen_random_uuid(),
    expert_profile_id uuid        NOT NULL,
    start_at          timestamptz NOT NULL,
    end_at            timestamptz NOT NULL,
    channel_type      varchar(30) NOT NULL,
    status            varchar(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.expert_location_shares (
    location_share_id   uuid    NOT NULL DEFAULT gen_random_uuid(),
    expert_profile_id   uuid    NOT NULL,
    latitude            numeric NOT NULL,
    longitude           numeric NOT NULL,
    accuracy_meters     numeric,
    availability_status varchar(20),
    shared_at           timestamptz NOT NULL DEFAULT now(),
    expires_at          timestamptz,
    consent_reference   uuid,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.consultation_price_bands (
    price_band_id    uuid        NOT NULL DEFAULT gen_random_uuid(),
    configured_by    uuid        NOT NULL,
    channel_type     varchar(30) NOT NULL,
    duration_minutes smallint    NOT NULL,
    specialty_scope  varchar(100),
    minimum_price    numeric     NOT NULL,
    maximum_price    numeric     NOT NULL,
    commission_rate  numeric     NOT NULL,
    currency         varchar(10) NOT NULL DEFAULT 'VND',
    effective_from   timestamptz NOT NULL,
    effective_to     timestamptz,
    status           varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.expert_consultation_prices (
    expert_price_id     uuid        NOT NULL DEFAULT gen_random_uuid(),
    expert_profile_id   uuid        NOT NULL,
    price_band_id       uuid        NOT NULL,
    channel_type        varchar(30) NOT NULL,
    duration_minutes    smallint    NOT NULL,
    price_amount        numeric     NOT NULL,
    currency            varchar(10) NOT NULL DEFAULT 'VND',
    cancellation_policy text,
    effective_from      timestamptz NOT NULL,
    effective_to        timestamptz,
    status              varchar(20) NOT NULL DEFAULT 'ACTIVE',
    version_no          integer     NOT NULL DEFAULT 1,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.consultation_bookings (
    booking_id                   uuid         NOT NULL DEFAULT gen_random_uuid(),
    requester_user_id            uuid         NOT NULL,
    expert_profile_id            uuid         NOT NULL,
    availability_id              uuid,
    expert_price_id              uuid         NOT NULL,
    shared_summary_id            uuid,
    topic                        varchar(500),
    channel_type                 varchar(30)  NOT NULL,
    duration_minutes             smallint     NOT NULL,
    scheduled_start              timestamptz  NOT NULL,
    scheduled_end                timestamptz  NOT NULL,
    price_snapshot_amount        numeric      NOT NULL,
    commission_rate_snapshot     numeric      NOT NULL,
    currency                     varchar(10)  NOT NULL DEFAULT 'VND',
    cancellation_policy_snapshot text,
    price_locked_at              timestamptz,
    status                       varchar(30)  NOT NULL DEFAULT 'PENDING_PAYMENT',
    created_at                   timestamptz  NOT NULL DEFAULT now(),
    updated_at                   timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE public.consultation_sessions (
    session_id            uuid         NOT NULL DEFAULT gen_random_uuid(),
    booking_id            uuid         NOT NULL,
    communication_room_id varchar(255),
    started_at            timestamptz,
    ended_at              timestamptz,
    session_status        varchar(30)  NOT NULL DEFAULT 'WAITING',
    expert_summary        text,
    technical_log_json    jsonb,
    created_at            timestamptz  NOT NULL DEFAULT now(),
    updated_at            timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE public.consultation_messages (
    message_id     uuid        NOT NULL DEFAULT gen_random_uuid(),
    session_id     uuid        NOT NULL,
    sender_user_id uuid        NOT NULL,
    message_type   varchar(30) NOT NULL,
    message_body   text,
    file_url       text,
    sent_at        timestamptz NOT NULL DEFAULT now(),
    read_at        timestamptz,
    status         varchar(20) NOT NULL DEFAULT 'SENT'
);

CREATE TABLE public.payment_transactions (
    payment_id             uuid        NOT NULL DEFAULT gen_random_uuid(),
    booking_id             uuid        NOT NULL,
    payer_user_id          uuid        NOT NULL,
    gateway_name           varchar(50) NOT NULL,
    gateway_transaction_id varchar(255),
    gross_amount           numeric     NOT NULL,
    gateway_fee            numeric     NOT NULL DEFAULT 0,
    refund_amount          numeric     NOT NULL DEFAULT 0,
    net_paid_amount        numeric     NOT NULL DEFAULT 0,
    currency               varchar(10) NOT NULL DEFAULT 'VND',
    status                 varchar(30) NOT NULL DEFAULT 'PENDING',
    paid_at                timestamptz,
    refunded_at            timestamptz,
    created_at             timestamptz NOT NULL DEFAULT now(),
    updated_at             timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.commission_records (
    commission_id     uuid    NOT NULL DEFAULT gen_random_uuid(),
    payment_id        uuid    NOT NULL,
    expert_profile_id uuid    NOT NULL,
    original_price    numeric NOT NULL,
    commission_rate   numeric NOT NULL,
    commission_amount numeric NOT NULL,
    gateway_fee       numeric NOT NULL DEFAULT 0,
    refund_amount     numeric NOT NULL DEFAULT 0,
    expert_net_amount numeric NOT NULL,
    eligible_at       timestamptz,
    settlement_status varchar(30) NOT NULL DEFAULT 'PENDING',
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.expert_reviews (
    review_id         uuid        NOT NULL DEFAULT gen_random_uuid(),
    booking_id        uuid        NOT NULL,
    reviewer_user_id  uuid        NOT NULL,
    expert_profile_id uuid        NOT NULL,
    rating            smallint    NOT NULL,
    comment           text,
    moderation_status varchar(20) NOT NULL DEFAULT 'PENDING',
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.consultation_disputes (
    dispute_id      uuid        NOT NULL DEFAULT gen_random_uuid(),
    booking_id      uuid        NOT NULL,
    submitted_by    uuid        NOT NULL,
    resolved_by     uuid,
    reason_code     varchar(50),
    description     text,
    evidence_json   jsonb,
    status          varchar(30) NOT NULL DEFAULT 'OPEN',
    resolution_type varchar(30),
    resolution_note text,
    submitted_at    timestamptz NOT NULL DEFAULT now(),
    resolved_at     timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.refund_records (
    refund_id         uuid        NOT NULL DEFAULT gen_random_uuid(),
    payment_id        uuid        NOT NULL,
    dispute_id        uuid,
    approved_by       uuid,
    refund_amount     numeric     NOT NULL,
    currency          varchar(10) NOT NULL DEFAULT 'VND',
    reason            text,
    gateway_refund_id varchar(255),
    status            varchar(30) NOT NULL DEFAULT 'PENDING',
    requested_at      timestamptz NOT NULL DEFAULT now(),
    processed_at      timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.settlement_records (
    settlement_id           uuid    NOT NULL DEFAULT gen_random_uuid(),
    commission_id           uuid    NOT NULL,
    expert_profile_id       uuid    NOT NULL,
    settlement_period_start date    NOT NULL,
    settlement_period_end   date    NOT NULL,
    gross_amount            numeric NOT NULL,
    commission_amount       numeric NOT NULL,
    gateway_fee             numeric NOT NULL DEFAULT 0,
    refund_amount           numeric NOT NULL DEFAULT 0,
    expert_net_amount       numeric NOT NULL,
    status                  varchar(30) NOT NULL DEFAULT 'PENDING',
    settled_at              timestamptz,
    reference_code          varchar(100),
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now()
);


-- ----------------------------------------------------------------------------
-- 2F. PARTNER & LOCATION (4 new tables; partner_organizations is existing)
-- ----------------------------------------------------------------------------

CREATE TABLE public.partner_expert_links (
    partner_expert_link_id uuid        NOT NULL DEFAULT gen_random_uuid(),
    partner_id             uuid        NOT NULL,
    expert_profile_id      uuid        NOT NULL,
    relationship_type      varchar(50),
    status                 varchar(20) NOT NULL DEFAULT 'ACTIVE',
    approved_by            uuid,
    start_date             date,
    end_date               date,
    created_at             timestamptz NOT NULL DEFAULT now(),
    updated_at             timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.partner_services (
    service_id      uuid         NOT NULL DEFAULT gen_random_uuid(),
    partner_id      uuid         NOT NULL,
    service_name    varchar(200) NOT NULL,
    description     text,
    price_from      numeric,
    currency        varchar(10)  DEFAULT 'VND',
    booking_url     text,
    approval_status varchar(30)  NOT NULL DEFAULT 'PENDING',
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE public.sponsored_campaigns (
    campaign_id     uuid         NOT NULL DEFAULT gen_random_uuid(),
    partner_id      uuid         NOT NULL,
    title           varchar(255) NOT NULL,
    description     text,
    start_date      date,
    end_date        date,
    sponsor_label   varchar(100),
    approval_status varchar(30)  NOT NULL DEFAULT 'PENDING',
    reviewed_by     uuid,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE public.care_facilities (
    facility_id         uuid         NOT NULL DEFAULT gen_random_uuid(),
    partner_id          uuid,
    name                varchar(255) NOT NULL,
    facility_type       varchar(50),
    address             varchar(500),
    latitude            numeric,
    longitude           numeric,
    phone               varchar(30),
    opening_hours_json  jsonb,
    source_type         varchar(30),
    verification_status varchar(30)  NOT NULL DEFAULT 'UNVERIFIED',
    created_at          timestamptz  NOT NULL DEFAULT now(),
    updated_at          timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE public.emergency_events (
    emergency_event_id   uuid        NOT NULL DEFAULT gen_random_uuid(),
    user_id              uuid        NOT NULL,
    source_type          varchar(50),
    source_reference_id  uuid,
    risk_level           varchar(20),
    action_type          varchar(50),
    selected_facility_id uuid,
    selected_expert_id   uuid,
    status               varchar(20) NOT NULL DEFAULT 'OPEN',
    opened_at            timestamptz NOT NULL DEFAULT now(),
    closed_at            timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.location_snapshots (
    location_snapshot_id uuid    NOT NULL DEFAULT gen_random_uuid(),
    user_id              uuid    NOT NULL,
    context_type         varchar(50),
    context_id           uuid,
    latitude             numeric NOT NULL,
    longitude            numeric NOT NULL,
    accuracy_meters      numeric,
    captured_at          timestamptz NOT NULL DEFAULT now(),
    expires_at           timestamptz,
    consent_status       varchar(20)
);


-- ----------------------------------------------------------------------------
-- 2G. DEVICE & SMART SAFETY (5 tables)
-- ----------------------------------------------------------------------------

CREATE TABLE public.health_device_connections (
    connection_id      uuid         NOT NULL DEFAULT gen_random_uuid(),
    user_id            uuid         NOT NULL,
    provider_name      varchar(80)  NOT NULL,
    device_name        varchar(150),
    scopes_json        jsonb,
    token_reference    text,
    consent_granted_at timestamptz,
    last_synced_at     timestamptz,
    status             varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at         timestamptz  NOT NULL DEFAULT now(),
    updated_at         timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE public.device_measurements (
    device_measurement_id uuid        NOT NULL DEFAULT gen_random_uuid(),
    connection_id         uuid        NOT NULL,
    measurement_type      varchar(50) NOT NULL,
    value_numeric         numeric,
    value_secondary       numeric,
    unit                  varchar(30),
    measured_at           timestamptz NOT NULL,
    source_record_id      uuid,
    quality_label         varchar(30),
    raw_metadata_json     jsonb,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.safety_monitoring_settings (
    setting_id                uuid     NOT NULL DEFAULT gen_random_uuid(),
    user_id                   uuid     NOT NULL,
    is_enabled                boolean  NOT NULL DEFAULT false,
    countdown_seconds         smallint NOT NULL DEFAULT 30,
    location_sharing_enabled  boolean  NOT NULL DEFAULT false,
    emergency_contact_user_id uuid,
    monitoring_schedule_json  jsonb,
    sensor_consent_at         timestamptz,
    location_consent_at       timestamptz,
    disclaimer_version        varchar(50),
    created_at                timestamptz NOT NULL DEFAULT now(),
    updated_at                timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.safety_events (
    safety_event_id       uuid        NOT NULL DEFAULT gen_random_uuid(),
    user_id               uuid        NOT NULL,
    setting_id            uuid,
    detected_at           timestamptz NOT NULL,
    event_type            varchar(50) NOT NULL,
    confidence_score      numeric,
    peak_acceleration     numeric,
    angular_velocity      numeric,
    inactivity_seconds    integer,
    user_response         varchar(30),
    response_at           timestamptz,
    false_positive_reason text,
    status                varchar(20) NOT NULL DEFAULT 'DETECTED',
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.safety_alerts (
    safety_alert_id      uuid        NOT NULL DEFAULT gen_random_uuid(),
    safety_event_id      uuid        NOT NULL,
    recipient_user_id    uuid        NOT NULL,
    location_snapshot_id uuid,
    alert_reason         varchar(100),
    payload_json         jsonb,
    delivery_status      varchar(30) NOT NULL DEFAULT 'PENDING',
    sent_at              timestamptz,
    acknowledged_at      timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);


-- ----------------------------------------------------------------------------
-- 2H. PREGNANCY EXERCISE & POSTURE (5 tables)
-- ----------------------------------------------------------------------------

CREATE TABLE public.pregnancy_exercises (
    exercise_id               uuid         NOT NULL DEFAULT gen_random_uuid(),
    created_by                uuid         NOT NULL,
    title                     varchar(255) NOT NULL,
    description               text,
    trimester_scope           varchar(50),
    difficulty_level          varchar(30),
    duration_minutes          smallint,
    instruction_content       text,
    media_url                 text,
    safety_warning            text,
    supports_posture_analysis boolean      NOT NULL DEFAULT false,
    status                    varchar(20)  NOT NULL DEFAULT 'DRAFT',
    version_no                integer      NOT NULL DEFAULT 1,
    created_at                timestamptz  NOT NULL DEFAULT now(),
    updated_at                timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE public.exercise_safety_checks (
    safety_check_id   uuid        NOT NULL DEFAULT gen_random_uuid(),
    exercise_id       uuid        NOT NULL,
    journey_id        uuid,
    user_id           uuid        NOT NULL,
    answer_json       jsonb,
    red_flag_detected boolean     NOT NULL DEFAULT false,
    result_status     varchar(20) NOT NULL DEFAULT 'PENDING',
    blocked_reason    text,
    completed_at      timestamptz,
    created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.exercise_sessions (
    exercise_session_id uuid        NOT NULL DEFAULT gen_random_uuid(),
    exercise_id         uuid        NOT NULL,
    journey_id          uuid,
    user_id             uuid        NOT NULL,
    safety_check_id     uuid,
    started_at          timestamptz NOT NULL,
    ended_at            timestamptz,
    paused_seconds      integer     NOT NULL DEFAULT 0,
    completion_percent  numeric,
    posture_score       numeric,
    session_status      varchar(20) NOT NULL DEFAULT 'IN_PROGRESS',
    warning_count       integer     NOT NULL DEFAULT 0,
    summary_json        jsonb,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.posture_analysis_configs (
    posture_config_id     uuid        NOT NULL DEFAULT gen_random_uuid(),
    exercise_id           uuid        NOT NULL,
    configured_by         uuid        NOT NULL,
    analysis_mode         varchar(30) NOT NULL,
    rule_or_model_version varchar(80),
    confidence_threshold  numeric,
    feedback_level        varchar(30),
    config_json           jsonb,
    effective_from        timestamptz NOT NULL,
    effective_to          timestamptz,
    status                varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.posture_feedback_events (
    feedback_event_id     uuid   NOT NULL DEFAULT gen_random_uuid(),
    exercise_session_id   uuid   NOT NULL,
    posture_config_id     uuid   NOT NULL,
    event_time_ms         bigint NOT NULL,
    posture_code          varchar(80),
    confidence_score      numeric,
    severity              varchar(20),
    feedback_text         text,
    keypoint_summary_json jsonb,
    created_at            timestamptz NOT NULL DEFAULT now()
);


-- ============================================================================
-- PRIMARY KEY CONSTRAINTS — EXISTING 25 TABLES
-- ============================================================================

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (audit_log_id);

ALTER TABLE ONLY public.checklist_items
    ADD CONSTRAINT checklist_items_pkey PRIMARY KEY (checklist_item_id);

ALTER TABLE ONLY public.checklist_templates
    ADD CONSTRAINT checklist_templates_pkey PRIMARY KEY (checklist_template_id);

ALTER TABLE ONLY public.community_answers
    ADD CONSTRAINT community_answers_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.community_profiles
    ADD CONSTRAINT community_profiles_pkey PRIMARY KEY (community_profile_id);

ALTER TABLE ONLY public.community_questions
    ADD CONSTRAINT community_questions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.community_topics
    ADD CONSTRAINT community_topics_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.consent_grants
    ADD CONSTRAINT consent_grants_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.content_items
    ADD CONSTRAINT content_items_pkey PRIMARY KEY (content_item_id);

ALTER TABLE ONLY public.content_reports
    ADD CONSTRAINT content_reports_pkey PRIMARY KEY (report_id);

ALTER TABLE ONLY public.contribution_points
    ADD CONSTRAINT contribution_points_pkey PRIMARY KEY (point_record_id);

ALTER TABLE ONLY public.data_permissions
    ADD CONSTRAINT data_permissions_pkey PRIMARY KEY (permission_id);

ALTER TABLE ONLY public.moderation_actions
    ADD CONSTRAINT moderation_actions_pkey PRIMARY KEY (moderation_action_id);

ALTER TABLE ONLY public.notification_preferences
    ADD CONSTRAINT notification_preferences_pkey PRIMARY KEY (preference_id);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (notification_id);

ALTER TABLE ONLY public.otp_verifications
    ADD CONSTRAINT otp_verifications_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.partner_organizations
    ADD CONSTRAINT partner_organizations_pkey PRIMARY KEY (partner_id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (role_id);

ALTER TABLE ONLY public.security_events
    ADD CONSTRAINT security_events_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.triage_answers
    ADD CONSTRAINT triage_answers_pkey PRIMARY KEY (triage_answer_id);

ALTER TABLE ONLY public.triage_assessments
    ADD CONSTRAINT triage_assessments_pkey PRIMARY KEY (assessment_id);

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_role_id);

ALTER TABLE ONLY public.user_sessions
    ADD CONSTRAINT user_sessions_pkey PRIMARY KEY (session_id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


-- ============================================================================
-- PRIMARY KEY CONSTRAINTS — NEW 46 TABLES
-- ============================================================================

ALTER TABLE ONLY public.mother_journeys
    ADD CONSTRAINT mother_journeys_pkey PRIMARY KEY (journey_id);

ALTER TABLE ONLY public.maternal_health_metrics
    ADD CONSTRAINT maternal_health_metrics_pkey PRIMARY KEY (metric_id);

ALTER TABLE ONLY public.postpartum_logs
    ADD CONSTRAINT postpartum_logs_pkey PRIMARY KEY (postpartum_log_id);

ALTER TABLE ONLY public.baby_profiles
    ADD CONSTRAINT baby_profiles_pkey PRIMARY KEY (baby_id);

ALTER TABLE ONLY public.baby_daily_logs
    ADD CONSTRAINT baby_daily_logs_pkey PRIMARY KEY (baby_log_id);

ALTER TABLE ONLY public.development_milestones
    ADD CONSTRAINT development_milestones_pkey PRIMARY KEY (milestone_id);

ALTER TABLE ONLY public.growth_measurements
    ADD CONSTRAINT growth_measurements_pkey PRIMARY KEY (growth_measurement_id);

ALTER TABLE ONLY public.vaccination_records
    ADD CONSTRAINT vaccination_records_pkey PRIMARY KEY (vaccination_record_id);

ALTER TABLE ONLY public.health_records
    ADD CONSTRAINT health_records_pkey PRIMARY KEY (health_record_id);

ALTER TABLE ONLY public.health_summaries
    ADD CONSTRAINT health_summaries_pkey PRIMARY KEY (summary_id);

ALTER TABLE ONLY public.reminders
    ADD CONSTRAINT reminders_pkey PRIMARY KEY (reminder_id);

ALTER TABLE ONLY public.care_groups
    ADD CONSTRAINT care_groups_pkey PRIMARY KEY (care_group_id);

ALTER TABLE ONLY public.care_group_members
    ADD CONSTRAINT care_group_members_pkey PRIMARY KEY (care_group_member_id);

ALTER TABLE ONLY public.care_tasks
    ADD CONSTRAINT care_tasks_pkey PRIMARY KEY (care_task_id);

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_pkey PRIMARY KEY (expense_id);

ALTER TABLE ONLY public.expert_profiles
    ADD CONSTRAINT expert_profiles_pkey PRIMARY KEY (expert_profile_id);

ALTER TABLE ONLY public.expert_credentials
    ADD CONSTRAINT expert_credentials_pkey PRIMARY KEY (credential_id);

ALTER TABLE ONLY public.expert_availability
    ADD CONSTRAINT expert_availability_pkey PRIMARY KEY (availability_id);

ALTER TABLE ONLY public.expert_location_shares
    ADD CONSTRAINT expert_location_shares_pkey PRIMARY KEY (location_share_id);

ALTER TABLE ONLY public.consultation_price_bands
    ADD CONSTRAINT consultation_price_bands_pkey PRIMARY KEY (price_band_id);

ALTER TABLE ONLY public.expert_consultation_prices
    ADD CONSTRAINT expert_consultation_prices_pkey PRIMARY KEY (expert_price_id);

ALTER TABLE ONLY public.consultation_bookings
    ADD CONSTRAINT consultation_bookings_pkey PRIMARY KEY (booking_id);

ALTER TABLE ONLY public.consultation_sessions
    ADD CONSTRAINT consultation_sessions_pkey PRIMARY KEY (session_id);

ALTER TABLE ONLY public.consultation_messages
    ADD CONSTRAINT consultation_messages_pkey PRIMARY KEY (message_id);

ALTER TABLE ONLY public.payment_transactions
    ADD CONSTRAINT payment_transactions_pkey PRIMARY KEY (payment_id);

ALTER TABLE ONLY public.commission_records
    ADD CONSTRAINT commission_records_pkey PRIMARY KEY (commission_id);

ALTER TABLE ONLY public.expert_reviews
    ADD CONSTRAINT expert_reviews_pkey PRIMARY KEY (review_id);

ALTER TABLE ONLY public.consultation_disputes
    ADD CONSTRAINT consultation_disputes_pkey PRIMARY KEY (dispute_id);

ALTER TABLE ONLY public.refund_records
    ADD CONSTRAINT refund_records_pkey PRIMARY KEY (refund_id);

ALTER TABLE ONLY public.settlement_records
    ADD CONSTRAINT settlement_records_pkey PRIMARY KEY (settlement_id);

ALTER TABLE ONLY public.partner_expert_links
    ADD CONSTRAINT partner_expert_links_pkey PRIMARY KEY (partner_expert_link_id);

ALTER TABLE ONLY public.partner_services
    ADD CONSTRAINT partner_services_pkey PRIMARY KEY (service_id);

ALTER TABLE ONLY public.sponsored_campaigns
    ADD CONSTRAINT sponsored_campaigns_pkey PRIMARY KEY (campaign_id);

ALTER TABLE ONLY public.care_facilities
    ADD CONSTRAINT care_facilities_pkey PRIMARY KEY (facility_id);

ALTER TABLE ONLY public.emergency_events
    ADD CONSTRAINT emergency_events_pkey PRIMARY KEY (emergency_event_id);

ALTER TABLE ONLY public.location_snapshots
    ADD CONSTRAINT location_snapshots_pkey PRIMARY KEY (location_snapshot_id);

ALTER TABLE ONLY public.health_device_connections
    ADD CONSTRAINT health_device_connections_pkey PRIMARY KEY (connection_id);

ALTER TABLE ONLY public.device_measurements
    ADD CONSTRAINT device_measurements_pkey PRIMARY KEY (device_measurement_id);

ALTER TABLE ONLY public.safety_monitoring_settings
    ADD CONSTRAINT safety_monitoring_settings_pkey PRIMARY KEY (setting_id);

ALTER TABLE ONLY public.safety_events
    ADD CONSTRAINT safety_events_pkey PRIMARY KEY (safety_event_id);

ALTER TABLE ONLY public.safety_alerts
    ADD CONSTRAINT safety_alerts_pkey PRIMARY KEY (safety_alert_id);

ALTER TABLE ONLY public.pregnancy_exercises
    ADD CONSTRAINT pregnancy_exercises_pkey PRIMARY KEY (exercise_id);

ALTER TABLE ONLY public.exercise_safety_checks
    ADD CONSTRAINT exercise_safety_checks_pkey PRIMARY KEY (safety_check_id);

ALTER TABLE ONLY public.exercise_sessions
    ADD CONSTRAINT exercise_sessions_pkey PRIMARY KEY (exercise_session_id);

ALTER TABLE ONLY public.posture_analysis_configs
    ADD CONSTRAINT posture_analysis_configs_pkey PRIMARY KEY (posture_config_id);

ALTER TABLE ONLY public.posture_feedback_events
    ADD CONSTRAINT posture_feedback_events_pkey PRIMARY KEY (feedback_event_id);


-- ============================================================================
-- UNIQUE CONSTRAINTS — EXISTING 25 TABLES (carried byte-for-byte)
-- ============================================================================

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT uk949pwsnk7kxk0px0tbj3r3web UNIQUE (role_code);

ALTER TABLE ONLY public.partner_organizations
    ADD CONSTRAINT uk_partner_email UNIQUE (email);

ALTER TABLE ONLY public.partner_organizations
    ADD CONSTRAINT uk_partner_representative UNIQUE (representative_user_id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT ukghpmfn23vmxfu3spu3lfg4r2d UNIQUE (token);


-- ============================================================================
-- UNIQUE CONSTRAINTS — NEW 46 TABLES
-- ============================================================================

-- One expert profile per user
ALTER TABLE ONLY public.expert_profiles
    ADD CONSTRAINT expert_profiles_user_id_key UNIQUE (user_id);

-- One session per booking
ALTER TABLE ONLY public.consultation_sessions
    ADD CONSTRAINT consultation_sessions_booking_id_key UNIQUE (booking_id);

-- One commission record per payment
ALTER TABLE ONLY public.commission_records
    ADD CONSTRAINT commission_records_payment_id_key UNIQUE (payment_id);

-- One review per booking
ALTER TABLE ONLY public.expert_reviews
    ADD CONSTRAINT expert_reviews_booking_id_key UNIQUE (booking_id);

-- One safety monitoring setting per user
ALTER TABLE ONLY public.safety_monitoring_settings
    ADD CONSTRAINT safety_monitoring_settings_user_id_key UNIQUE (user_id);


-- ============================================================================
-- INDEXES — EXISTING 25 TABLES (carried byte-for-byte)
-- ============================================================================

CREATE INDEX idx_checklist_templates_stage ON public.checklist_templates USING btree (stage);

CREATE INDEX idx_community_answers_author_id ON public.community_answers USING btree (author_id);

CREATE INDEX idx_community_answers_question_id ON public.community_answers USING btree (question_id);

CREATE INDEX idx_community_answers_status ON public.community_answers USING btree (status);

CREATE INDEX idx_community_questions_author_id ON public.community_questions USING btree (author_id);

CREATE INDEX idx_community_questions_stage ON public.community_questions USING btree (stage);

CREATE INDEX idx_community_questions_status ON public.community_questions USING btree (status);

CREATE INDEX idx_community_questions_topic_id ON public.community_questions USING btree (topic_id);

CREATE INDEX idx_community_topics_hidden ON public.community_topics USING btree (is_hidden);

CREATE UNIQUE INDEX idx_community_topics_name_lower ON public.community_topics USING btree (lower((name)::text));

CREATE INDEX idx_community_topics_sort_order ON public.community_topics USING btree (sort_order);

CREATE INDEX idx_content_items_published_at ON public.content_items USING btree (published_at DESC NULLS LAST) WHERE ((status)::text = 'APPROVED'::text);

CREATE INDEX idx_content_items_stage ON public.content_items USING btree (stage);

CREATE INDEX idx_content_items_stage_status ON public.content_items USING btree (stage, status);

CREATE INDEX idx_content_items_stage_type_approved ON public.content_items USING btree (stage, content_type, status) WHERE ((status)::text = 'APPROVED'::text);

CREATE INDEX idx_content_items_status ON public.content_items USING btree (status);

CREATE INDEX idx_content_items_title_search ON public.content_items USING btree (lower((title)::text));

CREATE INDEX idx_content_items_type ON public.content_items USING btree (content_type);

CREATE INDEX idx_content_items_type_status ON public.content_items USING btree (content_type, status);

CREATE INDEX idx_partner_org_status ON public.partner_organizations USING btree (status);

CREATE INDEX idx_partner_org_type ON public.partner_organizations USING btree (type);


-- ============================================================================
-- INDEXES — NEW 46 TABLES
-- ============================================================================

-- Care Journey
CREATE INDEX idx_mother_journeys_owner_user_id ON public.mother_journeys USING btree (owner_user_id);
CREATE INDEX idx_mother_journeys_status ON public.mother_journeys USING btree (status);
CREATE INDEX idx_maternal_health_metrics_journey_id ON public.maternal_health_metrics USING btree (journey_id);
CREATE INDEX idx_maternal_health_metrics_measured_at ON public.maternal_health_metrics USING btree (measured_at);
CREATE INDEX idx_postpartum_logs_journey_id ON public.postpartum_logs USING btree (journey_id);
CREATE INDEX idx_postpartum_logs_log_date ON public.postpartum_logs USING btree (log_date);

-- Baby Care
CREATE INDEX idx_baby_profiles_owner_user_id ON public.baby_profiles USING btree (owner_user_id);
CREATE INDEX idx_baby_profiles_related_journey_id ON public.baby_profiles USING btree (related_journey_id);
CREATE INDEX idx_baby_daily_logs_baby_id ON public.baby_daily_logs USING btree (baby_id);
CREATE INDEX idx_baby_daily_logs_started_at ON public.baby_daily_logs USING btree (started_at);
CREATE INDEX idx_development_milestones_baby_id ON public.development_milestones USING btree (baby_id);
CREATE INDEX idx_growth_measurements_baby_id ON public.growth_measurements USING btree (baby_id);
CREATE INDEX idx_vaccination_records_baby_id ON public.vaccination_records USING btree (baby_id);
CREATE INDEX idx_vaccination_records_status ON public.vaccination_records USING btree (status);

-- Health Records
CREATE INDEX idx_health_records_owner_user_id ON public.health_records USING btree (owner_user_id);
CREATE INDEX idx_health_records_journey_id ON public.health_records USING btree (journey_id);
CREATE INDEX idx_health_records_baby_id ON public.health_records USING btree (baby_id);
CREATE INDEX idx_health_summaries_owner_user_id ON public.health_summaries USING btree (owner_user_id);
CREATE INDEX idx_health_summaries_journey_id ON public.health_summaries USING btree (journey_id);

-- Care Coordination
CREATE INDEX idx_reminders_owner_user_id ON public.reminders USING btree (owner_user_id);
CREATE INDEX idx_reminders_scheduled_at ON public.reminders USING btree (scheduled_at);
CREATE INDEX idx_reminders_status ON public.reminders USING btree (status);
CREATE INDEX idx_care_groups_owner_user_id ON public.care_groups USING btree (owner_user_id);
CREATE INDEX idx_care_group_members_care_group_id ON public.care_group_members USING btree (care_group_id);
CREATE INDEX idx_care_group_members_user_id ON public.care_group_members USING btree (user_id);
CREATE INDEX idx_care_tasks_care_group_id ON public.care_tasks USING btree (care_group_id);
CREATE INDEX idx_care_tasks_status ON public.care_tasks USING btree (status);
CREATE INDEX idx_expenses_owner_user_id ON public.expenses USING btree (owner_user_id);

-- Expert & Consultation
CREATE INDEX idx_expert_profiles_verification_status ON public.expert_profiles USING btree (verification_status);
CREATE INDEX idx_expert_credentials_expert_profile_id ON public.expert_credentials USING btree (expert_profile_id);
CREATE INDEX idx_expert_availability_expert_profile_id ON public.expert_availability USING btree (expert_profile_id);
CREATE INDEX idx_expert_availability_status ON public.expert_availability USING btree (status);
CREATE INDEX idx_expert_availability_start_at ON public.expert_availability USING btree (start_at);
CREATE INDEX idx_consultation_bookings_requester_user_id ON public.consultation_bookings USING btree (requester_user_id);
CREATE INDEX idx_consultation_bookings_expert_profile_id ON public.consultation_bookings USING btree (expert_profile_id);
CREATE INDEX idx_consultation_bookings_status ON public.consultation_bookings USING btree (status);
CREATE INDEX idx_consultation_bookings_scheduled_start ON public.consultation_bookings USING btree (scheduled_start);
CREATE INDEX idx_consultation_messages_session_id ON public.consultation_messages USING btree (session_id);
CREATE INDEX idx_payment_transactions_booking_id ON public.payment_transactions USING btree (booking_id);
CREATE INDEX idx_payment_transactions_payer_user_id ON public.payment_transactions USING btree (payer_user_id);
CREATE INDEX idx_payment_transactions_status ON public.payment_transactions USING btree (status);
CREATE INDEX idx_commission_records_expert_profile_id ON public.commission_records USING btree (expert_profile_id);
CREATE INDEX idx_expert_reviews_expert_profile_id ON public.expert_reviews USING btree (expert_profile_id);
CREATE INDEX idx_consultation_disputes_booking_id ON public.consultation_disputes USING btree (booking_id);

-- Partner & Location
CREATE INDEX idx_partner_expert_links_partner_id ON public.partner_expert_links USING btree (partner_id);
CREATE INDEX idx_partner_expert_links_expert_profile_id ON public.partner_expert_links USING btree (expert_profile_id);
CREATE INDEX idx_partner_services_partner_id ON public.partner_services USING btree (partner_id);
CREATE INDEX idx_care_facilities_partner_id ON public.care_facilities USING btree (partner_id);
CREATE INDEX idx_care_facilities_facility_type ON public.care_facilities USING btree (facility_type);
CREATE INDEX idx_emergency_events_user_id ON public.emergency_events USING btree (user_id);
CREATE INDEX idx_emergency_events_status ON public.emergency_events USING btree (status);
CREATE INDEX idx_location_snapshots_user_id ON public.location_snapshots USING btree (user_id);
CREATE INDEX idx_location_snapshots_expires_at ON public.location_snapshots USING btree (expires_at);

-- Device & Smart Safety
CREATE INDEX idx_health_device_connections_user_id ON public.health_device_connections USING btree (user_id);
CREATE INDEX idx_device_measurements_connection_id ON public.device_measurements USING btree (connection_id);
CREATE INDEX idx_device_measurements_measured_at ON public.device_measurements USING btree (measured_at);
CREATE INDEX idx_safety_events_user_id ON public.safety_events USING btree (user_id);
CREATE INDEX idx_safety_events_detected_at ON public.safety_events USING btree (detected_at);
CREATE INDEX idx_safety_events_status ON public.safety_events USING btree (status);
CREATE INDEX idx_safety_alerts_safety_event_id ON public.safety_alerts USING btree (safety_event_id);
CREATE INDEX idx_safety_alerts_recipient_user_id ON public.safety_alerts USING btree (recipient_user_id);

-- Pregnancy Exercise & Posture
CREATE INDEX idx_pregnancy_exercises_status ON public.pregnancy_exercises USING btree (status);
CREATE INDEX idx_exercise_safety_checks_exercise_id ON public.exercise_safety_checks USING btree (exercise_id);
CREATE INDEX idx_exercise_safety_checks_user_id ON public.exercise_safety_checks USING btree (user_id);
CREATE INDEX idx_exercise_sessions_exercise_id ON public.exercise_sessions USING btree (exercise_id);
CREATE INDEX idx_exercise_sessions_user_id ON public.exercise_sessions USING btree (user_id);
CREATE INDEX idx_posture_analysis_configs_exercise_id ON public.posture_analysis_configs USING btree (exercise_id);
CREATE INDEX idx_posture_feedback_events_exercise_session_id ON public.posture_feedback_events USING btree (exercise_session_id);


-- ============================================================================
-- FOREIGN KEY CONSTRAINTS — EXISTING 25 TABLES (3 FKs, carried byte-for-byte)
-- ============================================================================

ALTER TABLE ONLY public.community_answers
    ADD CONSTRAINT community_answers_question_id_fkey FOREIGN KEY (question_id) REFERENCES public.community_questions(id);

ALTER TABLE ONLY public.community_questions
    ADD CONSTRAINT community_questions_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES public.community_topics(id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os FOREIGN KEY (user_id) REFERENCES public.users(user_id);


-- ============================================================================
-- FOREIGN KEY CONSTRAINTS — NEW 46 TABLES
-- Only FKs for the 46 new tables are added here (not retrofitted onto existing).
-- ============================================================================

-- Care Journey
ALTER TABLE ONLY public.mother_journeys
    ADD CONSTRAINT mother_journeys_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.maternal_health_metrics
    ADD CONSTRAINT maternal_health_metrics_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(journey_id);

ALTER TABLE ONLY public.postpartum_logs
    ADD CONSTRAINT postpartum_logs_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(journey_id);

-- Baby Care
ALTER TABLE ONLY public.baby_profiles
    ADD CONSTRAINT baby_profiles_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.baby_profiles
    ADD CONSTRAINT baby_profiles_related_journey_id_fkey FOREIGN KEY (related_journey_id) REFERENCES public.mother_journeys(journey_id);

ALTER TABLE ONLY public.baby_daily_logs
    ADD CONSTRAINT baby_daily_logs_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.baby_profiles(baby_id);

ALTER TABLE ONLY public.baby_daily_logs
    ADD CONSTRAINT baby_daily_logs_recorded_by_fkey FOREIGN KEY (recorded_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.development_milestones
    ADD CONSTRAINT development_milestones_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.baby_profiles(baby_id);

ALTER TABLE ONLY public.development_milestones
    ADD CONSTRAINT development_milestones_recorded_by_fkey FOREIGN KEY (recorded_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.growth_measurements
    ADD CONSTRAINT growth_measurements_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.baby_profiles(baby_id);

ALTER TABLE ONLY public.vaccination_records
    ADD CONSTRAINT vaccination_records_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.baby_profiles(baby_id);

ALTER TABLE ONLY public.vaccination_records
    ADD CONSTRAINT vaccination_records_proof_record_id_fkey FOREIGN KEY (proof_record_id) REFERENCES public.health_records(health_record_id);

-- Health Records
ALTER TABLE ONLY public.health_records
    ADD CONSTRAINT health_records_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.health_records
    ADD CONSTRAINT health_records_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(journey_id);

ALTER TABLE ONLY public.health_records
    ADD CONSTRAINT health_records_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.baby_profiles(baby_id);

ALTER TABLE ONLY public.health_summaries
    ADD CONSTRAINT health_summaries_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.health_summaries
    ADD CONSTRAINT health_summaries_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(journey_id);

ALTER TABLE ONLY public.health_summaries
    ADD CONSTRAINT health_summaries_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.baby_profiles(baby_id);

-- Care Coordination
ALTER TABLE ONLY public.reminders
    ADD CONSTRAINT reminders_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.reminders
    ADD CONSTRAINT reminders_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(journey_id);

ALTER TABLE ONLY public.reminders
    ADD CONSTRAINT reminders_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.baby_profiles(baby_id);

ALTER TABLE ONLY public.care_groups
    ADD CONSTRAINT care_groups_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.care_groups
    ADD CONSTRAINT care_groups_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(journey_id);

ALTER TABLE ONLY public.care_groups
    ADD CONSTRAINT care_groups_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.baby_profiles(baby_id);

ALTER TABLE ONLY public.care_group_members
    ADD CONSTRAINT care_group_members_care_group_id_fkey FOREIGN KEY (care_group_id) REFERENCES public.care_groups(care_group_id);

ALTER TABLE ONLY public.care_group_members
    ADD CONSTRAINT care_group_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.care_tasks
    ADD CONSTRAINT care_tasks_care_group_id_fkey FOREIGN KEY (care_group_id) REFERENCES public.care_groups(care_group_id);

ALTER TABLE ONLY public.care_tasks
    ADD CONSTRAINT care_tasks_assigned_by_fkey FOREIGN KEY (assigned_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.care_tasks
    ADD CONSTRAINT care_tasks_assigned_to_fkey FOREIGN KEY (assigned_to) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(journey_id);

ALTER TABLE ONLY public.expenses
    ADD CONSTRAINT expenses_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.baby_profiles(baby_id);

-- Expert & Consultation
ALTER TABLE ONLY public.expert_profiles
    ADD CONSTRAINT expert_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.expert_profiles
    ADD CONSTRAINT expert_profiles_verified_by_fkey FOREIGN KEY (verified_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.expert_credentials
    ADD CONSTRAINT expert_credentials_expert_profile_id_fkey FOREIGN KEY (expert_profile_id) REFERENCES public.expert_profiles(expert_profile_id);

ALTER TABLE ONLY public.expert_availability
    ADD CONSTRAINT expert_availability_expert_profile_id_fkey FOREIGN KEY (expert_profile_id) REFERENCES public.expert_profiles(expert_profile_id);

ALTER TABLE ONLY public.expert_location_shares
    ADD CONSTRAINT expert_location_shares_expert_profile_id_fkey FOREIGN KEY (expert_profile_id) REFERENCES public.expert_profiles(expert_profile_id);

ALTER TABLE ONLY public.consultation_price_bands
    ADD CONSTRAINT consultation_price_bands_configured_by_fkey FOREIGN KEY (configured_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.expert_consultation_prices
    ADD CONSTRAINT expert_consultation_prices_expert_profile_id_fkey FOREIGN KEY (expert_profile_id) REFERENCES public.expert_profiles(expert_profile_id);

ALTER TABLE ONLY public.expert_consultation_prices
    ADD CONSTRAINT expert_consultation_prices_price_band_id_fkey FOREIGN KEY (price_band_id) REFERENCES public.consultation_price_bands(price_band_id);

ALTER TABLE ONLY public.consultation_bookings
    ADD CONSTRAINT consultation_bookings_requester_user_id_fkey FOREIGN KEY (requester_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.consultation_bookings
    ADD CONSTRAINT consultation_bookings_expert_profile_id_fkey FOREIGN KEY (expert_profile_id) REFERENCES public.expert_profiles(expert_profile_id);

ALTER TABLE ONLY public.consultation_bookings
    ADD CONSTRAINT consultation_bookings_availability_id_fkey FOREIGN KEY (availability_id) REFERENCES public.expert_availability(availability_id);

ALTER TABLE ONLY public.consultation_bookings
    ADD CONSTRAINT consultation_bookings_expert_price_id_fkey FOREIGN KEY (expert_price_id) REFERENCES public.expert_consultation_prices(expert_price_id);

ALTER TABLE ONLY public.consultation_bookings
    ADD CONSTRAINT consultation_bookings_shared_summary_id_fkey FOREIGN KEY (shared_summary_id) REFERENCES public.health_summaries(summary_id);

ALTER TABLE ONLY public.consultation_sessions
    ADD CONSTRAINT consultation_sessions_booking_id_fkey FOREIGN KEY (booking_id) REFERENCES public.consultation_bookings(booking_id);

ALTER TABLE ONLY public.consultation_messages
    ADD CONSTRAINT consultation_messages_session_id_fkey FOREIGN KEY (session_id) REFERENCES public.consultation_sessions(session_id);

ALTER TABLE ONLY public.consultation_messages
    ADD CONSTRAINT consultation_messages_sender_user_id_fkey FOREIGN KEY (sender_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.payment_transactions
    ADD CONSTRAINT payment_transactions_booking_id_fkey FOREIGN KEY (booking_id) REFERENCES public.consultation_bookings(booking_id);

ALTER TABLE ONLY public.payment_transactions
    ADD CONSTRAINT payment_transactions_payer_user_id_fkey FOREIGN KEY (payer_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.commission_records
    ADD CONSTRAINT commission_records_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES public.payment_transactions(payment_id);

ALTER TABLE ONLY public.commission_records
    ADD CONSTRAINT commission_records_expert_profile_id_fkey FOREIGN KEY (expert_profile_id) REFERENCES public.expert_profiles(expert_profile_id);

ALTER TABLE ONLY public.expert_reviews
    ADD CONSTRAINT expert_reviews_booking_id_fkey FOREIGN KEY (booking_id) REFERENCES public.consultation_bookings(booking_id);

ALTER TABLE ONLY public.expert_reviews
    ADD CONSTRAINT expert_reviews_reviewer_user_id_fkey FOREIGN KEY (reviewer_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.expert_reviews
    ADD CONSTRAINT expert_reviews_expert_profile_id_fkey FOREIGN KEY (expert_profile_id) REFERENCES public.expert_profiles(expert_profile_id);

ALTER TABLE ONLY public.consultation_disputes
    ADD CONSTRAINT consultation_disputes_booking_id_fkey FOREIGN KEY (booking_id) REFERENCES public.consultation_bookings(booking_id);

ALTER TABLE ONLY public.consultation_disputes
    ADD CONSTRAINT consultation_disputes_submitted_by_fkey FOREIGN KEY (submitted_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.consultation_disputes
    ADD CONSTRAINT consultation_disputes_resolved_by_fkey FOREIGN KEY (resolved_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.refund_records
    ADD CONSTRAINT refund_records_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES public.payment_transactions(payment_id);

ALTER TABLE ONLY public.refund_records
    ADD CONSTRAINT refund_records_dispute_id_fkey FOREIGN KEY (dispute_id) REFERENCES public.consultation_disputes(dispute_id);

ALTER TABLE ONLY public.refund_records
    ADD CONSTRAINT refund_records_approved_by_fkey FOREIGN KEY (approved_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.settlement_records
    ADD CONSTRAINT settlement_records_commission_id_fkey FOREIGN KEY (commission_id) REFERENCES public.commission_records(commission_id);

ALTER TABLE ONLY public.settlement_records
    ADD CONSTRAINT settlement_records_expert_profile_id_fkey FOREIGN KEY (expert_profile_id) REFERENCES public.expert_profiles(expert_profile_id);

-- Partner & Location
ALTER TABLE ONLY public.partner_expert_links
    ADD CONSTRAINT partner_expert_links_partner_id_fkey FOREIGN KEY (partner_id) REFERENCES public.partner_organizations(partner_id);

ALTER TABLE ONLY public.partner_expert_links
    ADD CONSTRAINT partner_expert_links_expert_profile_id_fkey FOREIGN KEY (expert_profile_id) REFERENCES public.expert_profiles(expert_profile_id);

ALTER TABLE ONLY public.partner_expert_links
    ADD CONSTRAINT partner_expert_links_approved_by_fkey FOREIGN KEY (approved_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.partner_services
    ADD CONSTRAINT partner_services_partner_id_fkey FOREIGN KEY (partner_id) REFERENCES public.partner_organizations(partner_id);

ALTER TABLE ONLY public.sponsored_campaigns
    ADD CONSTRAINT sponsored_campaigns_partner_id_fkey FOREIGN KEY (partner_id) REFERENCES public.partner_organizations(partner_id);

ALTER TABLE ONLY public.sponsored_campaigns
    ADD CONSTRAINT sponsored_campaigns_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.care_facilities
    ADD CONSTRAINT care_facilities_partner_id_fkey FOREIGN KEY (partner_id) REFERENCES public.partner_organizations(partner_id);

ALTER TABLE ONLY public.emergency_events
    ADD CONSTRAINT emergency_events_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.emergency_events
    ADD CONSTRAINT emergency_events_selected_facility_id_fkey FOREIGN KEY (selected_facility_id) REFERENCES public.care_facilities(facility_id);

ALTER TABLE ONLY public.emergency_events
    ADD CONSTRAINT emergency_events_selected_expert_id_fkey FOREIGN KEY (selected_expert_id) REFERENCES public.expert_profiles(expert_profile_id);

ALTER TABLE ONLY public.location_snapshots
    ADD CONSTRAINT location_snapshots_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);

-- Device & Smart Safety
ALTER TABLE ONLY public.health_device_connections
    ADD CONSTRAINT health_device_connections_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.device_measurements
    ADD CONSTRAINT device_measurements_connection_id_fkey FOREIGN KEY (connection_id) REFERENCES public.health_device_connections(connection_id);

ALTER TABLE ONLY public.safety_monitoring_settings
    ADD CONSTRAINT safety_monitoring_settings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.safety_monitoring_settings
    ADD CONSTRAINT safety_monitoring_settings_emergency_contact_fkey FOREIGN KEY (emergency_contact_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.safety_events
    ADD CONSTRAINT safety_events_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.safety_events
    ADD CONSTRAINT safety_events_setting_id_fkey FOREIGN KEY (setting_id) REFERENCES public.safety_monitoring_settings(setting_id);

ALTER TABLE ONLY public.safety_alerts
    ADD CONSTRAINT safety_alerts_safety_event_id_fkey FOREIGN KEY (safety_event_id) REFERENCES public.safety_events(safety_event_id);

ALTER TABLE ONLY public.safety_alerts
    ADD CONSTRAINT safety_alerts_recipient_user_id_fkey FOREIGN KEY (recipient_user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.safety_alerts
    ADD CONSTRAINT safety_alerts_location_snapshot_id_fkey FOREIGN KEY (location_snapshot_id) REFERENCES public.location_snapshots(location_snapshot_id);

-- Pregnancy Exercise & Posture
ALTER TABLE ONLY public.pregnancy_exercises
    ADD CONSTRAINT pregnancy_exercises_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.exercise_safety_checks
    ADD CONSTRAINT exercise_safety_checks_exercise_id_fkey FOREIGN KEY (exercise_id) REFERENCES public.pregnancy_exercises(exercise_id);

ALTER TABLE ONLY public.exercise_safety_checks
    ADD CONSTRAINT exercise_safety_checks_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(journey_id);

ALTER TABLE ONLY public.exercise_safety_checks
    ADD CONSTRAINT exercise_safety_checks_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.exercise_sessions
    ADD CONSTRAINT exercise_sessions_exercise_id_fkey FOREIGN KEY (exercise_id) REFERENCES public.pregnancy_exercises(exercise_id);

ALTER TABLE ONLY public.exercise_sessions
    ADD CONSTRAINT exercise_sessions_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(journey_id);

ALTER TABLE ONLY public.exercise_sessions
    ADD CONSTRAINT exercise_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.exercise_sessions
    ADD CONSTRAINT exercise_sessions_safety_check_id_fkey FOREIGN KEY (safety_check_id) REFERENCES public.exercise_safety_checks(safety_check_id);

ALTER TABLE ONLY public.posture_analysis_configs
    ADD CONSTRAINT posture_analysis_configs_exercise_id_fkey FOREIGN KEY (exercise_id) REFERENCES public.pregnancy_exercises(exercise_id);

ALTER TABLE ONLY public.posture_analysis_configs
    ADD CONSTRAINT posture_analysis_configs_configured_by_fkey FOREIGN KEY (configured_by) REFERENCES public.users(user_id);

ALTER TABLE ONLY public.posture_feedback_events
    ADD CONSTRAINT posture_feedback_events_exercise_session_id_fkey FOREIGN KEY (exercise_session_id) REFERENCES public.exercise_sessions(exercise_session_id);

ALTER TABLE ONLY public.posture_feedback_events
    ADD CONSTRAINT posture_feedback_events_posture_config_id_fkey FOREIGN KEY (posture_config_id) REFERENCES public.posture_analysis_configs(posture_config_id);


-- =============================================================================
-- PostgreSQL database dump complete
-- Tables: 71 (25 core/current + 46 ERD-baseline future)
-- =============================================================================

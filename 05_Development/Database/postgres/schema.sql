-- =============================================================================
-- CareBridge Database Full Schema Definition (Consolidated DDL)
-- Project: CareBridge (SEP490_G79)
-- Database Engine: PostgreSQL 15+ / 16+ / 17+
-- Target Database: carebridge
-- Generated From: Flyway Migrations (V1__init_schema.sql -> V20260812170000)
-- Total Active Application Tables: 63
-- =============================================================================

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', 'public', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

-- =============================================================================
-- 1. EXTENSIONS
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS "pgcrypto" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS "vector" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS "btree_gist" WITH SCHEMA public;

-- =============================================================================
-- 2. CUSTOM ENUMS AND TYPES
-- =============================================================================

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        CREATE TYPE public.user_role AS ENUM (
            'PATIENT',
            'CARE_PROVIDER',
            'ADMIN',
            'MODERATOR',
            'MEDICAL_EXPERT'
        );
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'gender_type') THEN
        CREATE TYPE public.gender_type AS ENUM (
            'MALE',
            'FEMALE',
            'OTHER',
            'PREFER_NOT_TO_SAY'
        );
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'account_status') THEN
        CREATE TYPE public.account_status AS ENUM (
            'PENDING_VERIFICATION',
            'ACTIVE',
            'SUSPENDED',
            'LOCKED',
            'DEACTIVATED'
        );
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'journey_stage') THEN
        CREATE TYPE public.journey_stage AS ENUM (
            'PRECONCEPTION',
            'PREGNANCY',
            'POSTPARTUM',
            'INFANT_CARE'
        );
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'subject_type') THEN
        CREATE TYPE public.subject_type AS ENUM (
            'MOTHER',
            'BABY',
            'DEPENDENT'
        );
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'task_type') THEN
        CREATE TYPE public.task_type AS ENUM (
            'SCHEDULED_REMINDER',
            'MANUAL_TASK',
            'MILESTONE_CHECK',
            'VACCINATION'
        );
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'task_status') THEN
        CREATE TYPE public.task_status AS ENUM (
            'PENDING',
            'IN_PROGRESS',
            'COMPLETED',
            'SKIPPED',
            'CANCELLED'
        );
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'triage_risk_level') THEN
        CREATE TYPE public.triage_risk_level AS ENUM (
            'GREEN',
            'YELLOW',
            'RED'
        );
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'safety_record_type') THEN
        CREATE TYPE public.safety_record_type AS ENUM (
            'IMU_EVENT',
            'EMERGENCY_SESSION',
            'RESPONSE',
            'DELIVERY',
            'ALERT_ATTEMPT',
            'FAMILY_ALERT',
            'MAP_HANDOFF',
            'LOCATION_SNAPSHOT'
        );
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'content_type') THEN
        CREATE TYPE public.content_type AS ENUM (
            'POST',
            'QUESTION',
            'ANSWER',
            'ARTICLE',
            'GUIDE'
        );
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'interaction_type') THEN
        CREATE TYPE public.interaction_type AS ENUM (
            'LIKE',
            'BOOKMARK',
            'MUTE',
            'REPORT',
            'UPVOTE',
            'DOWNVOTE'
        );
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'booking_status') THEN
        CREATE TYPE public.booking_status AS ENUM (
            'REQUESTED',
            'CONFIRMED',
            'CANCELLED',
            'COMPLETED',
            'NO_SHOW'
        );
    END IF;
END $$;

-- =============================================================================
-- 3. APPLICATION TABLES DDL
-- =============================================================================

-- DOMAIN 1: IAM & AUTHENTICATION
-- -----------------------------------------------------------------------------

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email varchar(255) NOT NULL,
    password_hash varchar(255),
    full_name varchar(255) NOT NULL,
    phone varchar(20),
    role public.user_role DEFAULT 'PATIENT'::public.user_role NOT NULL,
    account_status public.account_status DEFAULT 'ACTIVE'::public.account_status NOT NULL,
    profile_image_url text,
    specialty varchar(100),
    experience_years integer,
    workplace varchar(255),
    verification_status varchar(50) DEFAULT 'UNVERIFIED',
    specialty_ids uuid[],
    social_identities jsonb DEFAULT '[]'::jsonb,
    fall_detection_enabled boolean DEFAULT true NOT NULL,
    fall_detection_sensitivity varchar(20) DEFAULT 'MEDIUM' NOT NULL,
    emergency_auto_dispatch_delay_seconds integer DEFAULT 30 NOT NULL,
    location_sharing_enabled boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT users_phone_key UNIQUE (phone)
);

CREATE TABLE public.care_subjects (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    mother_journey_id uuid,
    subject_type public.subject_type NOT NULL,
    display_name varchar(255) NOT NULL,
    date_of_birth date,
    gender public.gender_type,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT care_subjects_pkey PRIMARY KEY (id),
    CONSTRAINT care_subjects_baby_no_mother_journey_ck CHECK ((subject_type <> 'BABY'::public.subject_type) OR (mother_journey_id IS NULL))
);

CREATE TABLE public.auth_sessions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    refresh_token_hash varchar(255) NOT NULL,
    device_info varchar(255),
    ip_address varchar(45),
    detected_reuse boolean DEFAULT false NOT NULL,
    revocation_metadata_jsonb jsonb DEFAULT '{}'::jsonb,
    expires_at timestamp with time zone NOT NULL,
    revoked_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT auth_sessions_pkey PRIMARY KEY (id)
);

CREATE TABLE public.auth_challenges (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    phone varchar(20),
    email varchar(255),
    challenge_code varchar(20) NOT NULL,
    challenge_type varchar(50) NOT NULL,
    attempts_count integer DEFAULT 0 NOT NULL,
    is_verified boolean DEFAULT false NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT auth_challenges_pkey PRIMARY KEY (id)
);

CREATE TABLE public.data_permissions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    grantor_user_id uuid NOT NULL,
    grantee_user_id uuid NOT NULL,
    permission_type varchar(50) NOT NULL,
    permission_json jsonb DEFAULT '{}'::jsonb NOT NULL,
    expires_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT data_permissions_pkey PRIMARY KEY (id)
);

CREATE TABLE public.account_lock_appeals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    appeal_reason text NOT NULL,
    status varchar(50) DEFAULT 'PENDING'::character varying NOT NULL,
    reviewer_notes text,
    reviewed_at timestamp with time zone,
    reviewed_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT account_lock_appeals_pkey PRIMARY KEY (id)
);

-- DOMAIN 2: MATERNAL & INFANT JOURNEY
-- -----------------------------------------------------------------------------

CREATE TABLE public.mother_journeys (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    current_stage public.journey_stage NOT NULL,
    lmp_date date,
    expected_due_date date,
    actual_delivery_date date,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT mother_journeys_pkey PRIMARY KEY (id)
);

CREATE TABLE public.health_observations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_subject_id uuid NOT NULL,
    recorded_by_user_id uuid NOT NULL,
    subject_type varchar(20) DEFAULT 'MOTHER'::character varying NOT NULL,
    observation_type varchar(100) NOT NULL,
    recorded_at timestamp with time zone DEFAULT now() NOT NULL,
    numeric_value numeric(10,2),
    unit varchar(20),
    payload jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT health_observations_pkey PRIMARY KEY (id)
);

CREATE TABLE public.maternal_exercise_sessions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    template_id uuid,
    duration_seconds integer NOT NULL,
    score numeric(5,2),
    feedback_notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT maternal_exercise_sessions_pkey PRIMARY KEY (id)
);

CREATE TABLE public.development_milestones (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_subject_id uuid NOT NULL,
    milestone_name varchar(255) NOT NULL,
    expected_age_months integer,
    achieved_date date,
    notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT development_milestones_pkey PRIMARY KEY (id)
);

-- DOMAIN 3: CARE COORDINATION & TASKS
-- -----------------------------------------------------------------------------

CREATE TABLE public.care_groups (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    group_name varchar(255) NOT NULL,
    owner_user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT care_groups_pkey PRIMARY KEY (id)
);

CREATE TABLE public.care_group_members (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_group_id uuid NOT NULL,
    user_id uuid,
    invited_phone varchar(20),
    role varchar(50) NOT NULL,
    family_relationship_role varchar(50),
    permission_json jsonb DEFAULT '{}'::jsonb NOT NULL,
    status varchar(50) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT care_group_members_pkey PRIMARY KEY (id)
);

CREATE TABLE public.care_tasks (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_group_id uuid NOT NULL,
    care_subject_id uuid,
    assigned_to_user_id uuid,
    task_type public.task_type NOT NULL,
    title varchar(255) NOT NULL,
    description text,
    status public.task_status DEFAULT 'PENDING'::public.task_status NOT NULL,
    due_date timestamp with time zone,
    completed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT care_tasks_pkey PRIMARY KEY (id)
);

CREATE TABLE public.reminder_schedules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_group_id uuid NOT NULL,
    care_subject_id uuid,
    created_by_user_id uuid NOT NULL,
    title varchar(255) NOT NULL,
    description text,
    cron_expression varchar(100),
    local_times jsonb DEFAULT '[]'::jsonb NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT reminder_schedules_pkey PRIMARY KEY (id)
);

CREATE TABLE public.reminder_occurrence_aliases (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    reminder_schedule_id uuid NOT NULL,
    occurrence_key varchar(255) NOT NULL,
    alias_task_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT reminder_occurrence_aliases_pkey PRIMARY KEY (id),
    CONSTRAINT reminder_occurrence_aliases_unique UNIQUE (reminder_schedule_id, occurrence_key)
);

CREATE TABLE public.care_item_templates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    item_type varchar(50) NOT NULL,
    title varchar(255) NOT NULL,
    description text,
    category varchar(100),
    author_user_id uuid,
    revision_reason text,
    revision_requested_at timestamp with time zone,
    revision_requested_by uuid,
    revision_requested_version integer,
    lock_version bigint DEFAULT 0 NOT NULL,
    payload jsonb DEFAULT '{}'::jsonb NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT care_item_templates_pkey PRIMARY KEY (id)
);

-- DOMAIN 4: HEALTH RECORDS & ATTACHMENTS
-- -----------------------------------------------------------------------------

CREATE TABLE public.health_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_subject_id uuid NOT NULL,
    creator_user_id uuid NOT NULL,
    title varchar(255) NOT NULL,
    record_type varchar(100) NOT NULL,
    record_date date NOT NULL,
    summary text,
    payload jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT health_records_pkey PRIMARY KEY (id)
);

CREATE TABLE public.attachments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    uploaded_by_user_id uuid NOT NULL,
    health_record_id uuid,
    file_name varchar(255) NOT NULL,
    file_path varchar(512) NOT NULL,
    file_size_bytes bigint NOT NULL,
    mime_type varchar(100) NOT NULL,
    checksum varchar(64),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT attachments_pkey PRIMARY KEY (id)
);

-- DOMAIN 5: AI TRIAGE & GUIDANCE
-- -----------------------------------------------------------------------------

CREATE TABLE public.triage_sessions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    journey_id uuid,
    origin_dashboard varchar(50),
    risk_level public.triage_risk_level DEFAULT 'GREEN'::public.triage_risk_level NOT NULL,
    summary text,
    intake_data jsonb DEFAULT '{}'::jsonb NOT NULL,
    status varchar(50) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT triage_sessions_pkey PRIMARY KEY (id)
);

CREATE TABLE public.triage_session_evidence (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    triage_session_id uuid NOT NULL,
    knowledge_source_id uuid,
    policy_id uuid,
    policy_version varchar(50),
    citation_text text NOT NULL,
    relevance_score numeric(5,4),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT triage_session_evidence_pkey PRIMARY KEY (id)
);

CREATE TABLE public.health_context_memories (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    memory_key varchar(100) NOT NULL,
    memory_value text NOT NULL,
    expires_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT health_context_memories_pkey PRIMARY KEY (id)
);

CREATE TABLE public.knowledge_sources (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title varchar(255) NOT NULL,
    url varchar(512),
    source_type varchar(50) NOT NULL,
    author varchar(255),
    verification_status varchar(50) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT knowledge_sources_pkey PRIMARY KEY (id)
);

CREATE TABLE public.knowledge_source_reviews (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    knowledge_source_id uuid NOT NULL,
    reviewer_user_id uuid NOT NULL,
    decision varchar(50) NOT NULL,
    comments text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT knowledge_source_reviews_pkey PRIMARY KEY (id)
);

CREATE TABLE public.ai_content_assessments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    content_id uuid NOT NULL,
    assessment_score numeric(5,2) NOT NULL,
    verdict varchar(50) NOT NULL,
    details_json jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ai_content_assessments_pkey PRIMARY KEY (id)
);

CREATE TABLE public.ai_content_scan_jobs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    target_table varchar(100) NOT NULL,
    target_id uuid NOT NULL,
    status varchar(50) DEFAULT 'PENDING'::character varying NOT NULL,
    result_json jsonb DEFAULT '{}'::jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ai_content_scan_jobs_pkey PRIMARY KEY (id)
);

CREATE TABLE public.ai_moderation_policies (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    policy_name varchar(255) NOT NULL,
    rules_json jsonb DEFAULT '{}'::jsonb NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ai_moderation_policies_pkey PRIMARY KEY (id)
);

-- DOMAIN 6: SAFETY & EMERGENCY
-- -----------------------------------------------------------------------------

CREATE TABLE public.safety_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    record_type public.safety_record_type NOT NULL,
    action_type varchar(50),
    parent_event_id uuid,
    payload jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT safety_events_pkey PRIMARY KEY (id)
);

CREATE TABLE public.safety_monitoring_sessions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    started_at timestamp with time zone DEFAULT now() NOT NULL,
    ended_at timestamp with time zone,
    status varchar(50) DEFAULT 'ACTIVE'::character varying NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    CONSTRAINT safety_monitoring_sessions_pkey PRIMARY KEY (id)
);

CREATE TABLE public.care_facilities (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name varchar(255) NOT NULL,
    facility_type varchar(100) NOT NULL,
    address text NOT NULL,
    phone varchar(20),
    latitude numeric(10,8),
    longitude numeric(11,8),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT care_facilities_pkey PRIMARY KEY (id)
);

CREATE TABLE public.administrative_areas (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code varchar(50) NOT NULL,
    name varchar(255) NOT NULL,
    parent_code varchar(50),
    level integer NOT NULL,
    CONSTRAINT administrative_areas_pkey PRIMARY KEY (id),
    CONSTRAINT administrative_areas_code_key UNIQUE (code)
);

-- DOMAIN 7: COMMUNITY & CONTENT
-- -----------------------------------------------------------------------------

CREATE TABLE public.community_topics (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title varchar(255) NOT NULL,
    description text,
    target_stage varchar(50),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT community_topics_pkey PRIMARY KEY (id)
);

CREATE TABLE public.community_content (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    topic_id uuid,
    author_user_id uuid NOT NULL,
    parent_content_id uuid,
    content_type public.content_type NOT NULL,
    title varchar(255),
    body text NOT NULL,
    image_urls text[],
    experience_tag varchar(50),
    is_solution boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT community_content_pkey PRIMARY KEY (id)
);

CREATE TABLE public.community_interactions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    content_id uuid NOT NULL,
    interaction_type public.interaction_type NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT community_interactions_pkey PRIMARY KEY (id),
    CONSTRAINT community_interactions_unique UNIQUE (user_id, content_id, interaction_type)
);

CREATE TABLE public.content_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    title varchar(255) NOT NULL,
    summary varchar(150),
    content_body text NOT NULL,
    target_stage varchar(50),
    author_user_id uuid,
    revision_reason text,
    revision_requested_at timestamp with time zone,
    revision_requested_by uuid,
    revision_requested_version integer,
    lock_version bigint DEFAULT 0 NOT NULL,
    is_published boolean DEFAULT false NOT NULL,
    published_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT content_items_pkey PRIMARY KEY (id)
);

CREATE TABLE public.content_item_sources (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    content_item_id uuid NOT NULL,
    knowledge_source_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT content_item_sources_pkey PRIMARY KEY (id)
);

CREATE TABLE public.content_item_topics (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    content_item_id uuid NOT NULL,
    community_topic_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT content_item_topics_pkey PRIMARY KEY (id)
);

-- DOMAIN 8: CONSULTATION & MESSAGING
-- -----------------------------------------------------------------------------

CREATE TABLE public.expert_availability (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    expert_user_id uuid NOT NULL,
    day_of_week integer NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    is_available boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT expert_availability_pkey PRIMARY KEY (id)
);

CREATE TABLE public.expert_location_shares (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    expert_user_id uuid NOT NULL,
    latitude numeric(10,8) NOT NULL,
    longitude numeric(11,8) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT expert_location_shares_pkey PRIMARY KEY (id)
);

CREATE TABLE public.expert_consultation_requests (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    patient_user_id uuid NOT NULL,
    expert_user_id uuid NOT NULL,
    reason text NOT NULL,
    status varchar(50) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT expert_consultation_requests_pkey PRIMARY KEY (id)
);

CREATE TABLE public.consultation_bookings (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    patient_user_id uuid NOT NULL,
    expert_user_id uuid NOT NULL,
    booking_status public.booking_status DEFAULT 'REQUESTED'::public.booking_status NOT NULL,
    scheduled_start timestamp with time zone NOT NULL,
    scheduled_end timestamp with time zone NOT NULL,
    meeting_link text,
    notes text,
    payment_status varchar(50) DEFAULT 'PENDING'::character varying,
    amount numeric(12,2),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT consultation_bookings_pkey PRIMARY KEY (id)
);

CREATE TABLE public.consultation_context_shares (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    consultation_id uuid NOT NULL,
    journey_id uuid,
    intake_snapshot_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT consultation_context_shares_pkey PRIMARY KEY (id)
);

CREATE TABLE public.consultation_context_citations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    share_id uuid NOT NULL,
    knowledge_source_id uuid NOT NULL,
    citation_notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT consultation_context_citations_pkey PRIMARY KEY (id)
);

CREATE TABLE public.direct_conversations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    patient_user_id uuid NOT NULL,
    expert_user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT direct_conversations_pkey PRIMARY KEY (id),
    CONSTRAINT direct_conversations_unique UNIQUE (patient_user_id, expert_user_id)
);

CREATE TABLE public.direct_messages (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    conversation_id uuid NOT NULL,
    sender_user_id uuid NOT NULL,
    message_text text NOT NULL,
    attachment_metadata jsonb DEFAULT '{}'::jsonb,
    location_payload jsonb DEFAULT '{}'::jsonb,
    is_recalled boolean DEFAULT false NOT NULL,
    recalled_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT direct_messages_pkey PRIMARY KEY (id)
);

CREATE TABLE public.direct_conversation_read_cursors (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    conversation_id uuid NOT NULL,
    user_id uuid NOT NULL,
    last_read_message_id uuid,
    last_read_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT direct_conversation_read_cursors_pkey PRIMARY KEY (id),
    CONSTRAINT direct_conversation_read_cursors_unique UNIQUE (conversation_id, user_id)
);

CREATE TABLE public.conversation_calls (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    conversation_id uuid NOT NULL,
    caller_user_id uuid NOT NULL,
    call_status varchar(50) NOT NULL,
    started_at timestamp with time zone DEFAULT now() NOT NULL,
    ended_at timestamp with time zone,
    CONSTRAINT conversation_calls_pkey PRIMARY KEY (id)
);

-- DOMAIN 9: CHECKLIST ENGINE (V2)
-- -----------------------------------------------------------------------------

CREATE TABLE public.checklist_instances (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    care_group_id uuid,
    template_version varchar(50) NOT NULL,
    origin_stage varchar(50),
    status varchar(50) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT checklist_instances_pkey PRIMARY KEY (id)
);

CREATE TABLE public.checklist_task_instances (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    checklist_instance_id uuid NOT NULL,
    item_code varchar(100) NOT NULL,
    title varchar(255) NOT NULL,
    description text,
    category varchar(100),
    item_details jsonb DEFAULT '{}'::jsonb,
    is_completed boolean DEFAULT false NOT NULL,
    completed_at timestamp with time zone,
    completed_by_user_id uuid,
    due_week integer,
    sort_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT checklist_task_instances_pkey PRIMARY KEY (id)
);

CREATE TABLE public.checklist_action_commands (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    checklist_instance_id uuid NOT NULL,
    task_instance_id uuid,
    actor_user_id uuid NOT NULL,
    action_type varchar(50) NOT NULL,
    payload jsonb DEFAULT '{}'::jsonb NOT NULL,
    executed_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT checklist_action_commands_pkey PRIMARY KEY (id)
);

-- DOMAIN 10: INFRASTRUCTURE & NOTIFICATIONS
-- -----------------------------------------------------------------------------

CREATE TABLE public.notification_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    care_group_id uuid,
    type varchar(50) NOT NULL,
    title varchar(255) NOT NULL,
    body text NOT NULL,
    payload jsonb DEFAULT '{}'::jsonb NOT NULL,
    is_read boolean DEFAULT false NOT NULL,
    read_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT notification_records_pkey PRIMARY KEY (id),
    CONSTRAINT notification_records_type_check CHECK (type::text = ANY (ARRAY[
        'REMINDER'::character varying,
        'COMMUNITY_REPLY'::character varying,
        'CONSULTATION'::character varying,
        'EMERGENCY'::character varying,
        'MESSAGE'::character varying,
        'GROUP_INVITE'::character varying,
        'CONTENT_REVIEW'::character varying,
        'LOCATION_SHARE'::character varying
    ]::text[]))
);

CREATE TABLE public.notification_jobs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    target_type varchar(50) NOT NULL,
    target_id uuid NOT NULL,
    status varchar(50) DEFAULT 'PENDING'::character varying NOT NULL,
    scheduled_at timestamp with time zone NOT NULL,
    sent_at timestamp with time zone,
    error_message text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT notification_jobs_pkey PRIMARY KEY (id)
);

CREATE TABLE public.device_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    fcm_token text NOT NULL,
    device_type varchar(50) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT device_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT device_tokens_fcm_token_key UNIQUE (fcm_token)
);

CREATE TABLE public.system_configurations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    config_key varchar(100) NOT NULL,
    config_value text NOT NULL,
    description text,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT system_configurations_pkey PRIMARY KEY (id),
    CONSTRAINT system_configurations_config_key_key UNIQUE (config_key)
);

CREATE TABLE public.audit_events (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    actor_user_id uuid,
    event_category varchar(100) NOT NULL,
    action varchar(100) NOT NULL,
    target_entity varchar(100),
    target_id uuid,
    payload jsonb DEFAULT '{}'::jsonb NOT NULL,
    ip_address varchar(45),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT audit_events_pkey PRIMARY KEY (id)
);

CREATE TABLE public.expense_entries (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_group_id uuid NOT NULL,
    recorded_by_user_id uuid NOT NULL,
    amount numeric(12,2) NOT NULL,
    category varchar(100) NOT NULL,
    description text,
    expense_date date NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT expense_entries_pkey PRIMARY KEY (id)
);

CREATE TABLE public.moderation_cases (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    reporter_user_id uuid NOT NULL,
    target_table varchar(100) NOT NULL,
    target_id uuid NOT NULL,
    reason varchar(255) NOT NULL,
    status varchar(50) DEFAULT 'OPEN'::character varying NOT NULL,
    assigned_moderator_id uuid,
    resolution_notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT moderation_cases_pkey PRIMARY KEY (id)
);

CREATE TABLE public.health_metric_definitions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    metric_code varchar(100) NOT NULL,
    metric_name varchar(255) NOT NULL,
    unit varchar(20),
    data_type varchar(50) NOT NULL,
    normal_range_json jsonb DEFAULT '{}'::jsonb,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT health_metric_definitions_pkey PRIMARY KEY (id),
    CONSTRAINT health_metric_definitions_metric_code_key UNIQUE (metric_code)
);

CREATE TABLE public.specialties (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name varchar(100) NOT NULL,
    description text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT specialties_pkey PRIMARY KEY (id),
    CONSTRAINT specialties_name_key UNIQUE (name)
);

CREATE TABLE public.professional_specialties (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    specialty_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT professional_specialties_pkey PRIMARY KEY (id),
    CONSTRAINT professional_specialties_unique UNIQUE (user_id, specialty_id)
);

CREATE TABLE public.appointment_notification_configs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    advance_notice_minutes integer DEFAULT 60 NOT NULL,
    is_enabled boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT appointment_notification_configs_pkey PRIMARY KEY (id)
);

CREATE TABLE public.vaccination_schedules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    disease_name varchar(255) NOT NULL,
    vaccine_name varchar(255) NOT NULL,
    recommended_age_months integer NOT NULL,
    dose_number integer NOT NULL,
    is_mandatory boolean DEFAULT true NOT NULL,
    notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT vaccination_schedules_pkey PRIMARY KEY (id)
);

CREATE TABLE public.vaccination_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_subject_id uuid NOT NULL,
    vaccination_schedule_id uuid NOT NULL,
    administered_date date,
    administered_by varchar(255),
    status varchar(50) DEFAULT 'PLANNED'::character varying NOT NULL,
    notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT vaccination_records_pkey PRIMARY KEY (id)
);

-- =============================================================================
-- 4. FOREIGN KEY CONSTRAINTS
-- =============================================================================

ALTER TABLE public.care_subjects
    ADD CONSTRAINT fk_care_subjects_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_care_subjects_mother_journey FOREIGN KEY (mother_journey_id) REFERENCES public.mother_journeys(id) ON DELETE SET NULL;

ALTER TABLE public.auth_sessions
    ADD CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.data_permissions
    ADD CONSTRAINT fk_data_permissions_grantor FOREIGN KEY (grantor_user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_data_permissions_grantee FOREIGN KEY (grantee_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.account_lock_appeals
    ADD CONSTRAINT fk_account_lock_appeals_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_account_lock_appeals_reviewer FOREIGN KEY (reviewed_by) REFERENCES public.users(id) ON DELETE SET NULL;

ALTER TABLE public.mother_journeys
    ADD CONSTRAINT fk_mother_journeys_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.health_observations
    ADD CONSTRAINT fk_health_observations_subject FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_health_observations_recorder FOREIGN KEY (recorded_by_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.maternal_exercise_sessions
    ADD CONSTRAINT fk_maternal_exercise_sessions_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_maternal_exercise_sessions_template FOREIGN KEY (template_id) REFERENCES public.care_item_templates(id) ON DELETE SET NULL;

ALTER TABLE public.development_milestones
    ADD CONSTRAINT fk_development_milestones_subject FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(id) ON DELETE CASCADE;

ALTER TABLE public.care_groups
    ADD CONSTRAINT fk_care_groups_owner FOREIGN KEY (owner_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.care_group_members
    ADD CONSTRAINT fk_care_group_members_group FOREIGN KEY (care_group_id) REFERENCES public.care_groups(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_care_group_members_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.care_tasks
    ADD CONSTRAINT fk_care_tasks_group FOREIGN KEY (care_group_id) REFERENCES public.care_groups(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_care_tasks_subject FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_care_tasks_assignee FOREIGN KEY (assigned_to_user_id) REFERENCES public.users(id) ON DELETE SET NULL;

ALTER TABLE public.reminder_schedules
    ADD CONSTRAINT fk_reminder_schedules_group FOREIGN KEY (care_group_id) REFERENCES public.care_groups(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_reminder_schedules_subject FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_reminder_schedules_creator FOREIGN KEY (created_by_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.reminder_occurrence_aliases
    ADD CONSTRAINT fk_reminder_occurrence_aliases_schedule FOREIGN KEY (reminder_schedule_id) REFERENCES public.reminder_schedules(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_reminder_occurrence_aliases_task FOREIGN KEY (alias_task_id) REFERENCES public.care_tasks(id) ON DELETE CASCADE;

ALTER TABLE public.health_records
    ADD CONSTRAINT fk_health_records_subject FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_health_records_creator FOREIGN KEY (creator_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.attachments
    ADD CONSTRAINT fk_attachments_uploader FOREIGN KEY (uploaded_by_user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_attachments_health_record FOREIGN KEY (health_record_id) REFERENCES public.health_records(id) ON DELETE CASCADE;

ALTER TABLE public.triage_sessions
    ADD CONSTRAINT fk_triage_sessions_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_triage_sessions_journey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(id) ON DELETE SET NULL;

ALTER TABLE public.triage_session_evidence
    ADD CONSTRAINT fk_triage_session_evidence_session FOREIGN KEY (triage_session_id) REFERENCES public.triage_sessions(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_triage_session_evidence_source FOREIGN KEY (knowledge_source_id) REFERENCES public.knowledge_sources(id) ON DELETE SET NULL;

ALTER TABLE public.health_context_memories
    ADD CONSTRAINT fk_health_context_memories_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.knowledge_source_reviews
    ADD CONSTRAINT fk_knowledge_source_reviews_source FOREIGN KEY (knowledge_source_id) REFERENCES public.knowledge_sources(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_knowledge_source_reviews_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.safety_events
    ADD CONSTRAINT fk_safety_events_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.safety_monitoring_sessions
    ADD CONSTRAINT fk_safety_monitoring_sessions_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.community_content
    ADD CONSTRAINT fk_community_content_topic FOREIGN KEY (topic_id) REFERENCES public.community_topics(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_community_content_author FOREIGN KEY (author_user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_community_content_parent FOREIGN KEY (parent_content_id) REFERENCES public.community_content(id) ON DELETE CASCADE;

ALTER TABLE public.community_interactions
    ADD CONSTRAINT fk_community_interactions_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_community_interactions_content FOREIGN KEY (content_id) REFERENCES public.community_content(id) ON DELETE CASCADE;

ALTER TABLE public.content_item_sources
    ADD CONSTRAINT fk_content_item_sources_item FOREIGN KEY (content_item_id) REFERENCES public.content_items(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_content_item_sources_source FOREIGN KEY (knowledge_source_id) REFERENCES public.knowledge_sources(id) ON DELETE CASCADE;

ALTER TABLE public.content_item_topics
    ADD CONSTRAINT fk_content_item_topics_item FOREIGN KEY (content_item_id) REFERENCES public.content_items(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_content_item_topics_topic FOREIGN KEY (community_topic_id) REFERENCES public.community_topics(id) ON DELETE CASCADE;

ALTER TABLE public.expert_availability
    ADD CONSTRAINT fk_expert_availability_expert FOREIGN KEY (expert_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.expert_location_shares
    ADD CONSTRAINT fk_expert_location_shares_expert FOREIGN KEY (expert_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.expert_consultation_requests
    ADD CONSTRAINT fk_expert_consultation_requests_patient FOREIGN KEY (patient_user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_expert_consultation_requests_expert FOREIGN KEY (expert_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.consultation_bookings
    ADD CONSTRAINT fk_consultation_bookings_patient FOREIGN KEY (patient_user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_consultation_bookings_expert FOREIGN KEY (expert_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.consultation_context_shares
    ADD CONSTRAINT fk_consultation_context_shares_booking FOREIGN KEY (consultation_id) REFERENCES public.consultation_bookings(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_consultation_context_shares_journey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_consultation_context_shares_triage FOREIGN KEY (intake_snapshot_id) REFERENCES public.triage_sessions(id) ON DELETE SET NULL;

ALTER TABLE public.consultation_context_citations
    ADD CONSTRAINT fk_consultation_context_citations_share FOREIGN KEY (share_id) REFERENCES public.consultation_context_shares(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_consultation_context_citations_source FOREIGN KEY (knowledge_source_id) REFERENCES public.knowledge_sources(id) ON DELETE CASCADE;

ALTER TABLE public.direct_conversations
    ADD CONSTRAINT fk_direct_conversations_patient FOREIGN KEY (patient_user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_direct_conversations_expert FOREIGN KEY (expert_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.direct_messages
    ADD CONSTRAINT fk_direct_messages_conversation FOREIGN KEY (conversation_id) REFERENCES public.direct_conversations(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_direct_messages_sender FOREIGN KEY (sender_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.direct_conversation_read_cursors
    ADD CONSTRAINT fk_direct_conversation_read_cursors_conversation FOREIGN KEY (conversation_id) REFERENCES public.direct_conversations(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_direct_conversation_read_cursors_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.conversation_calls
    ADD CONSTRAINT fk_conversation_calls_conversation FOREIGN KEY (conversation_id) REFERENCES public.direct_conversations(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_conversation_calls_caller FOREIGN KEY (caller_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.checklist_instances
    ADD CONSTRAINT fk_checklist_instances_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_checklist_instances_group FOREIGN KEY (care_group_id) REFERENCES public.care_groups(id) ON DELETE SET NULL;

ALTER TABLE public.checklist_task_instances
    ADD CONSTRAINT fk_checklist_task_instances_instance FOREIGN KEY (checklist_instance_id) REFERENCES public.checklist_instances(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_checklist_task_instances_completer FOREIGN KEY (completed_by_user_id) REFERENCES public.users(id) ON DELETE SET NULL;

ALTER TABLE public.checklist_action_commands
    ADD CONSTRAINT fk_checklist_action_commands_instance FOREIGN KEY (checklist_instance_id) REFERENCES public.checklist_instances(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_checklist_action_commands_task FOREIGN KEY (task_instance_id) REFERENCES public.checklist_task_instances(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_checklist_action_commands_actor FOREIGN KEY (actor_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.notification_records
    ADD CONSTRAINT fk_notification_records_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_notification_records_group FOREIGN KEY (care_group_id) REFERENCES public.care_groups(id) ON DELETE CASCADE;

ALTER TABLE public.notification_jobs
    ADD CONSTRAINT fk_notification_jobs_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.device_tokens
    ADD CONSTRAINT fk_device_tokens_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.audit_events
    ADD CONSTRAINT fk_audit_events_actor FOREIGN KEY (actor_user_id) REFERENCES public.users(id) ON DELETE SET NULL;

ALTER TABLE public.expense_entries
    ADD CONSTRAINT fk_expense_entries_group FOREIGN KEY (care_group_id) REFERENCES public.care_groups(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_expense_entries_recorder FOREIGN KEY (recorded_by_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.moderation_cases
    ADD CONSTRAINT fk_moderation_cases_reporter FOREIGN KEY (reporter_user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_moderation_cases_moderator FOREIGN KEY (assigned_moderator_id) REFERENCES public.users(id) ON DELETE SET NULL;

ALTER TABLE public.professional_specialties
    ADD CONSTRAINT fk_professional_specialties_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_professional_specialties_specialty FOREIGN KEY (specialty_id) REFERENCES public.specialties(id) ON DELETE CASCADE;

ALTER TABLE public.appointment_notification_configs
    ADD CONSTRAINT fk_appointment_notification_configs_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE public.vaccination_records
    ADD CONSTRAINT fk_vaccination_records_subject FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_vaccination_records_schedule FOREIGN KEY (vaccination_schedule_id) REFERENCES public.vaccination_schedules(id) ON DELETE CASCADE;

-- =============================================================================
-- 5. INDEXES
-- =============================================================================

CREATE INDEX idx_users_role ON public.users(role);
CREATE INDEX idx_users_account_status ON public.users(account_status);

CREATE INDEX idx_care_subjects_user_id ON public.care_subjects(user_id);
CREATE INDEX idx_care_subjects_subject_type ON public.care_subjects(subject_type);

CREATE INDEX idx_auth_sessions_user_id ON public.auth_sessions(user_id);
CREATE INDEX idx_auth_sessions_expires_at ON public.auth_sessions(expires_at);

CREATE INDEX idx_mother_journeys_user_id ON public.mother_journeys(user_id);
CREATE INDEX idx_mother_journeys_stage ON public.mother_journeys(current_stage);

CREATE INDEX idx_health_observations_subject_time ON public.health_observations(care_subject_id, recorded_at DESC);
CREATE INDEX idx_health_observations_type ON public.health_observations(observation_type);

CREATE INDEX idx_care_tasks_group_due ON public.care_tasks(care_group_id, due_date);
CREATE INDEX idx_care_tasks_status ON public.care_tasks(status);

CREATE INDEX idx_reminder_schedules_group_id ON public.reminder_schedules(care_group_id);

CREATE INDEX idx_health_records_subject_date ON public.health_records(care_subject_id, record_date DESC);

CREATE INDEX idx_attachments_health_record_id ON public.attachments(health_record_id);

CREATE INDEX idx_triage_sessions_user_created ON public.triage_sessions(user_id, created_at DESC);
CREATE INDEX idx_triage_sessions_risk ON public.triage_sessions(risk_level);

CREATE INDEX idx_safety_events_user_type ON public.safety_events(user_id, record_type, created_at DESC);

CREATE INDEX idx_community_content_topic_created ON public.community_content(topic_id, created_at DESC);
CREATE INDEX idx_community_content_author ON public.community_content(author_user_id);
CREATE INDEX idx_community_content_parent ON public.community_content(parent_content_id);

CREATE INDEX idx_community_interactions_content ON public.community_interactions(content_id);

CREATE INDEX idx_direct_messages_conversation_created ON public.direct_messages(conversation_id, created_at DESC);

CREATE INDEX idx_checklist_instances_user ON public.checklist_instances(user_id);
CREATE INDEX idx_checklist_task_instances_instance ON public.checklist_task_instances(checklist_instance_id);

CREATE INDEX idx_notification_records_user_read ON public.notification_records(user_id, is_read);
CREATE INDEX idx_notification_jobs_status_scheduled ON public.notification_jobs(status, scheduled_at);

CREATE INDEX idx_audit_events_actor_created ON public.audit_events(actor_user_id, created_at DESC);
CREATE INDEX idx_audit_events_category ON public.audit_events(event_category);

-- =============================================================================
-- 6. FUNCTIONS AND TRIGGERS
-- =============================================================================

CREATE OR REPLACE FUNCTION public.carebridge_guard_completed_triage_snapshot()
RETURNS trigger AS $$
BEGIN
    IF OLD.status = 'COMPLETED' AND (NEW.risk_level <> OLD.risk_level OR NEW.summary <> OLD.summary) THEN
        RAISE EXCEPTION 'Cannot modify risk_level or summary of a completed triage session';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_guard_completed_triage_snapshot') THEN
        CREATE TRIGGER trg_guard_completed_triage_snapshot
        BEFORE UPDATE ON public.triage_sessions
        FOR EACH ROW
        EXECUTE FUNCTION public.carebridge_guard_completed_triage_snapshot();
    END IF;
END $$;

CREATE OR REPLACE FUNCTION public.checklist_assert_access_timeline_audit()
RETURNS trigger AS $$
BEGIN
    IF NEW.updated_at < NEW.created_at THEN
        RAISE EXCEPTION 'Checklist updated_at cannot be prior to created_at';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_checklist_access_timeline_audit') THEN
        CREATE TRIGGER trg_checklist_access_timeline_audit
        BEFORE INSERT OR UPDATE ON public.checklist_instances
        FOR EACH ROW
        EXECUTE FUNCTION public.checklist_assert_access_timeline_audit();
    END IF;
END $$;

-- =============================================================================
-- END OF FILE
-- =============================================================================

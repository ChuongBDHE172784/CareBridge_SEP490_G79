-- ============================================================================
-- CareBridge Complete Database ERD Schema (PostgreSQL)
-- Source: 03_Design/Database/CareBridge_ERD.drawio (Tab: Complete Database ERD)
-- Target: Compatible with PostgreSQL & https://dbdiagram.io/
-- Total Tables: 62 | Valid Foreign Keys: 137
-- ============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- 1. CREATE TABLES
-- ============================================================================

-- Table: users
CREATE TABLE users (
    user_id uuid NOT NULL,
    avatar_url varchar(500),
    created_at timestamptz NOT NULL,
    email varchar(255) UNIQUE,
    full_name varchar(120),
    password_hash varchar(255),
    phone varchar(20),
    locked boolean DEFAULT false NOT NULL,
    lock_type varchar(30),
    role varchar(50),
    display_name varchar(200),
    date_of_birth date,
    area varchar(200),
    professional_title varchar(150),
    workplace varchar(200),
    experience_years smallint,
    consultation_scope text,
    verification_status varchar(30) DEFAULT 'UNVERIFIED' NOT NULL,
    rating_avg numeric,
    specialty varchar(100),
    facility_id uuid,
    trust_status varchar(20) DEFAULT 'BASIC' NOT NULL,
    bio varchar(500),
    interest_stage varchar(30),
    is_visible boolean,
    public_avatar_url varchar(500),
    fall_detection_enabled boolean DEFAULT true NOT NULL,
    fall_detection_sensitivity_level varchar(10) DEFAULT 'MEDIUM' NOT NULL,
    emergency_auto_alert boolean DEFAULT true NOT NULL,
    emergency_countdown_seconds integer DEFAULT 30 NOT NULL,
    sensor_permission_granted boolean DEFAULT true NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (user_id)
);

-- Table: auth_challenges
CREATE TABLE auth_challenges (
    challenge_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    challenge_type varchar(40) NOT NULL,
    subject_identifier varchar(255),
    challenge_hash varchar(255) NOT NULL,
    attempts integer DEFAULT 0 NOT NULL,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    status varchar(30) NOT NULL,
    requested_role varchar(40),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT auth_challenges_pkey PRIMARY KEY (challenge_id)
);

-- Table: auth_sessions
CREATE TABLE auth_sessions (
    session_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token_family_id uuid NOT NULL,
    device_identifier varchar(255) NOT NULL,
    device_name varchar(150),
    refresh_token_hash varchar(255),
    issued_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    last_used_at timestamptz,
    rotated_at timestamptz,
    revoked_at timestamptz,
    status varchar(20) NOT NULL,
    CONSTRAINT auth_sessions_pkey PRIMARY KEY (session_id)
);

-- Table: data_permissions
CREATE TABLE data_permissions (
    permission_id uuid DEFAULT gen_random_uuid() NOT NULL,
    grantee_user_id uuid,
    owner_user_id uuid,
    purpose varchar(60),
    scope_reference_id uuid,
    scope_type varchar(60),
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    permission_kind varchar(255) DEFAULT '' NOT NULL,
    CONSTRAINT data_permissions_pkey PRIMARY KEY (permission_id)
);

-- Table: mother_journeys
CREATE TABLE mother_journeys (
    journey_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL UNIQUE,
    journey_type varchar(20) NOT NULL,
    start_date date,
    last_menstrual_date date,
    estimated_due_date date,
    delivery_date date,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    notes text,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version bigint DEFAULT 1 NOT NULL,
    date_source varchar(30),
    date_confidence varchar(20),
    pregnancy_outcome varchar(30),
    pregnancy_outcome_date date,
    care_subject_id uuid NOT NULL UNIQUE,
    baseline_revision bigint,
    baseline_schema_version varchar(40),
    baseline_source varchar(30),
    baseline_lifecycle_goal varchar(40),
    baseline_locale varchar(20),
    baseline_time_zone varchar(80),
    baseline_preferences varchar(300),
    baseline_submission_id uuid,
    baseline_recorded_at timestamptz,
    recommendation_profile_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    recommendation_profile_version smallint DEFAULT 1 NOT NULL,
    recommendation_profile_completed_at timestamptz,
    recommendation_profile_status varchar(24) DEFAULT 'ACTIVE' NOT NULL,
    CONSTRAINT mother_journeys_pkey PRIMARY KEY (journey_id)
);

-- Table: care_subjects
CREATE TABLE care_subjects (
    care_subject_id uuid DEFAULT gen_random_uuid() NOT NULL,
    person_id uuid NOT NULL,
    owner_user_id uuid NOT NULL UNIQUE,
    mother_journey_id uuid,
    subject_type varchar(30) NOT NULL UNIQUE,
    nickname varchar(100),
    birth_date date,
    sex varchar(10),
    birth_weight_kg numeric(4,2),
    birth_length_cm numeric(4,1),
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT care_subjects_pkey PRIMARY KEY (care_subject_id)
);

-- Table: care_item_templates
CREATE TABLE care_item_templates (
    template_id uuid DEFAULT gen_random_uuid() NOT NULL,
    parent_template_id uuid UNIQUE,
    entry_type varchar(30) NOT NULL UNIQUE,
    title varchar(500) NOT NULL,
    description text,
    display_order integer DEFAULT 0 NOT NULL,
    stage varchar(30),
    version integer DEFAULT 1 NOT NULL,
    effective_from timestamptz,
    effective_to timestamptz,
    content_status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    is_required boolean,
    author_user_id uuid,
    template_lineage_id uuid UNIQUE,
    template_version_id uuid UNIQUE,
    target_subject varchar(10),
    distribution_enabled boolean DEFAULT true NOT NULL,
    due_anchor_type varchar(30),
    due_offset_start integer,
    due_offset_end integer,
    due_offset_unit varchar(10),
    template_type varchar(20) DEFAULT '' NOT NULL,
    recipient_scope varchar(10),
    eligibility_anchor_type varchar(30),
    eligibility_range_unit varchar(10),
    eligibility_start_inclusive integer,
    eligibility_end_inclusive integer,
    CONSTRAINT care_item_templates_pkey PRIMARY KEY (template_id)
);

-- Table: checklist_instances
CREATE TABLE checklist_instances (
    checklist_instance_id uuid DEFAULT gen_random_uuid() NOT NULL,
    distribution_key char(64) NOT NULL UNIQUE,
    key_version varchar(10) DEFAULT '' NOT NULL,
    template_lineage_id uuid,
    template_version_id uuid UNIQUE,
    recipient_user_id uuid NOT NULL,
    recipient_role varchar(10) NOT NULL,
    care_group_id uuid,
    care_context_type varchar(10) NOT NULL,
    care_context_id uuid NOT NULL,
    context_owner_user_id uuid NOT NULL,
    origin varchar(20) NOT NULL,
    window_start date,
    window_end date,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    lock_version bigint DEFAULT 1 NOT NULL,
    completed_at timestamptz,
    cancelled_at timestamptz,
    cancellation_reason_code varchar(80),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    journey_context_id uuid,
    baby_context_id uuid,
    baby_context_subject_type varchar(30),
    historical_at timestamptz,
    history_reason_code varchar(80),
    CONSTRAINT checklist_instances_pkey PRIMARY KEY (checklist_instance_id)
);

-- Table: checklist_action_commands
CREATE TABLE checklist_action_commands (
    checklist_action_command_id uuid DEFAULT gen_random_uuid() NOT NULL,
    actor_user_id uuid NOT NULL UNIQUE,
    task_kind varchar(30) NOT NULL UNIQUE,
    task_id uuid NOT NULL UNIQUE,
    client_request_id uuid NOT NULL UNIQUE,
    payload_hash char(64) NOT NULL,
    action_type varchar(30) NOT NULL,
    result_status varchar(20) NOT NULL,
    result_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    applied_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    retain_until timestamptz NOT NULL,
    legal_hold boolean DEFAULT false NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    reminder_definition_id uuid,
    CONSTRAINT checklist_action_commands_pkey PRIMARY KEY (checklist_action_command_id)
);

-- Table: care_groups
CREATE TABLE care_groups (
    care_group_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL UNIQUE,
    journey_id uuid,
    baby_id uuid,
    group_name varchar(200) NOT NULL,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    description varchar(500),
    linked_journey_id uuid,
    linked_baby_profile_id uuid,
    care_subject_id uuid,
    linked_baby_subject_type varchar(30),
    CONSTRAINT care_groups_pkey PRIMARY KEY (care_group_id)
);

-- Table: care_group_members
CREATE TABLE care_group_members (
    care_group_member_id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_group_id uuid NOT NULL,
    user_id uuid NOT NULL,
    member_role varchar(50),
    invitation_status varchar(20) DEFAULT '' NOT NULL,
    permission_json jsonb DEFAULT '[]'::jsonb,
    joined_at timestamptz,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    invite_token varchar(64),
    invite_channel varchar(20),
    invite_expires_at timestamptz,
    invited_phone varchar(255),
    data_permission_id uuid,
    is_emergency_contact boolean DEFAULT false NOT NULL,
    emergency_contact_priority smallint,
    family_relationship_role varchar(50),
    custom_family_relationship_role varchar(100),
    CONSTRAINT care_group_members_pkey PRIMARY KEY (care_group_member_id)
);

-- Table: care_tasks
CREATE TABLE care_tasks (
    task_id uuid DEFAULT gen_random_uuid() NOT NULL,
    task_type varchar(40) NOT NULL,
    owner_user_id uuid,
    care_group_id uuid,
    creator_user_id uuid,
    assignee_user_id uuid,
    care_subject_id uuid,
    title varchar(255) NOT NULL,
    description text,
    scheduled_at timestamptz,
    snoozed_until timestamptz,
    status varchar(30) DEFAULT 'ACTIVE' NOT NULL,
    source_reference_type varchar(60),
    source_reference_id uuid,
    vaccination_record_id uuid,
    recurrence_type varchar(30),
    recurrence_end_date timestamptz,
    item_type varchar(60),
    origin varchar(20) DEFAULT '' NOT NULL,
    target_subject varchar(10) DEFAULT '' NOT NULL,
    CONSTRAINT care_tasks_pkey PRIMARY KEY (task_id)
);

-- Table: development_milestones
CREATE TABLE development_milestones (
    milestone_id uuid DEFAULT gen_random_uuid() NOT NULL,
    baby_id uuid NOT NULL,
    milestone_type varchar(80) NOT NULL,
    achieved_date date,
    note text,
    source_type varchar(30),
    recorded_by uuid,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    milestone_status varchar(20) DEFAULT '' NOT NULL,
    record_status varchar(20) DEFAULT '' NOT NULL,
    care_subject_id uuid NOT NULL,
    CONSTRAINT development_milestones_pkey PRIMARY KEY (milestone_id)
);

-- Table: health_metric_definitions
CREATE TABLE health_metric_definitions (
    metric_definition_id uuid DEFAULT gen_random_uuid() NOT NULL,
    metric_code varchar(60) NOT NULL UNIQUE,
    version integer NOT NULL UNIQUE,
    display_name varchar(120) NOT NULL,
    observation_shape varchar(30) NOT NULL,
    subject_type varchar(30) DEFAULT '' NOT NULL,
    manual_entry_supported boolean DEFAULT false NOT NULL,
    device_import_supported boolean DEFAULT false NOT NULL,
    canonical_unit varchar(30),
    accepted_input_units_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    precision_scale smallint,
    required_context_schema_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    plausibility_policy_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    aggregation_policy_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    chart_policy_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    quality_policy_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    safety_policy_version varchar(40),
    allowed_journey_stages_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    effective_from timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    effective_until timestamptz,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT health_metric_definitions_pkey PRIMARY KEY (metric_definition_id)
);

-- Table: health_observations
CREATE TABLE health_observations (
    health_observation_id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_subject_id uuid NOT NULL,
    observation_type varchar(50) NOT NULL,
    value_numeric numeric(10,2),
    value_secondary numeric(10,2),
    unit varchar(30),
    observed_at timestamptz NOT NULL,
    source_record_id uuid,
    quality_label varchar(30),
    raw_payload_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    legacy_source varchar(60) UNIQUE,
    legacy_id varchar(100) UNIQUE,
    severity varchar(30),
    source_type varchar(60) DEFAULT 'USER' NOT NULL,
    subject_type varchar(30) NOT NULL,
    text_value text,
    period_start timestamptz,
    period_end timestamptz,
    context_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    original_unit varchar(30),
    definition_version integer,
    observation_shape varchar(30),
    measurement_group_id uuid,
    deleted_at timestamptz,
    CONSTRAINT health_observations_pkey PRIMARY KEY (health_observation_id)
);

-- Table: health_records
CREATE TABLE health_records (
    health_record_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL,
    journey_id uuid,
    baby_id uuid,
    record_type varchar(50) NOT NULL,
    title varchar(255) NOT NULL,
    file_url text,
    record_date date,
    source_type varchar(30),
    source_name varchar(200),
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    care_subject_id uuid,
    summary_period varchar(30),
    period_start date,
    summary_json jsonb,
    CONSTRAINT health_records_pkey PRIMARY KEY (health_record_id)
);

-- Table: maternal_exercise_sessions
CREATE TABLE maternal_exercise_sessions (
    exercise_session_id uuid DEFAULT gen_random_uuid() NOT NULL,
    mother_journey_id uuid,
    owner_user_id uuid NOT NULL,
    exercise_template_id uuid NOT NULL,
    posture_config_id uuid,
    started_at timestamptz NOT NULL,
    ended_at timestamptz,
    paused_seconds integer DEFAULT 0 NOT NULL,
    completion_percent numeric(38,2),
    posture_score numeric(38,2),
    session_status varchar(20) NOT NULL,
    warning_count integer DEFAULT 0 NOT NULL,
    summary_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    safety_observation_id uuid,
    CONSTRAINT maternal_exercise_sessions_pkey PRIMARY KEY (exercise_session_id)
);

-- Table: vaccination_records
CREATE TABLE vaccination_records (
    vaccination_record_id uuid DEFAULT gen_random_uuid() NOT NULL,
    baby_id uuid NOT NULL,
    vaccine_name varchar(200) NOT NULL,
    dose_number smallint,
    scheduled_date date,
    administered_date date,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    facility_name varchar(200),
    proof_record_id uuid,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    postpone_reason text,
    care_subject_id uuid NOT NULL,
    vaccination_schedule_id uuid,
    CONSTRAINT vaccination_records_pkey PRIMARY KEY (vaccination_record_id)
);

-- Table: vaccination_schedules
CREATE TABLE vaccination_schedules (
    vaccination_schedule_id uuid DEFAULT gen_random_uuid() NOT NULL,
    vaccine_name varchar(200) NOT NULL UNIQUE,
    dose_number smallint NOT NULL UNIQUE,
    offset_days integer NOT NULL,
    description text,
    schedule_version varchar(30) DEFAULT '' NOT NULL UNIQUE,
    active_from date,
    active_to date,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT vaccination_schedules_pkey PRIMARY KEY (vaccination_schedule_id)
);

-- Table: community_topics
CREATE TABLE community_topics (
    id uuid NOT NULL,
    created_at timestamptz NOT NULL,
    description text,
    name varchar(100) NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    is_hidden boolean DEFAULT false NOT NULL,
    icon varchar(255),
    sort_order integer DEFAULT 0 NOT NULL,
    created_by uuid,
    type varchar(20) DEFAULT '' NOT NULL,
    slug varchar(140) NOT NULL UNIQUE,
    parent_id uuid,
    CONSTRAINT community_topics_pkey PRIMARY KEY (id)
);

-- Table: community_content
CREATE TABLE community_content (
    content_id uuid DEFAULT gen_random_uuid() NOT NULL,
    topic_id uuid,
    parent_content_id uuid,
    author_user_id uuid NOT NULL,
    content_type varchar(20) NOT NULL,
    title varchar(255),
    body text NOT NULL,
    stage varchar(30),
    urgency varchar(20),
    is_anonymous boolean DEFAULT false NOT NULL,
    moderation_status varchar(30) DEFAULT 'ACTIVE' NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    pregnancy_week smallint,
    baby_age_months smallint,
    like_count integer DEFAULT 0 NOT NULL,
    answer_count integer DEFAULT 0 NOT NULL,
    is_expert_labeled boolean DEFAULT false NOT NULL,
    is_personal_experience boolean DEFAULT false NOT NULL,
    image_urls jsonb DEFAULT '[]'::jsonb NOT NULL,
    experience_tag varchar(80),
    CONSTRAINT community_content_pkey PRIMARY KEY (content_id)
);

-- Table: community_interactions
CREATE TABLE community_interactions (
    interaction_id uuid DEFAULT gen_random_uuid() NOT NULL,
    actor_user_id uuid NOT NULL UNIQUE,
    interaction_type varchar(255) NOT NULL UNIQUE,
    content_id uuid UNIQUE,
    topic_id uuid UNIQUE,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    target_content_type varchar(255),
    CONSTRAINT community_interactions_pkey PRIMARY KEY (interaction_id)
);

-- Table: content_items
CREATE TABLE content_items (
    content_item_id uuid NOT NULL,
    author_user_id uuid,
    body text,
    content_type varchar(30),
    created_at timestamptz NOT NULL,
    published_at timestamptz,
    source_label varchar(255),
    status varchar(20) NOT NULL,
    title varchar(250),
    topic_id uuid,
    updated_at timestamptz,
    version_no integer,
    stage varchar(30),
    revision_reason text,
    revision_requested_at timestamptz,
    revision_requested_by uuid,
    revision_requested_version integer,
    lock_version bigint DEFAULT 1 NOT NULL,
    summary varchar(150),
    eligible_from_week smallint,
    eligible_to_week smallint,
    recommendation_priority smallint DEFAULT 0 NOT NULL,
    CONSTRAINT content_items_pkey PRIMARY KEY (content_item_id)
);

-- Table: content_item_topics
CREATE TABLE content_item_topics (
    content_item_id uuid NOT NULL,
    topic_id uuid NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT content_item_topics_pkey PRIMARY KEY (content_item_id, topic_id)
);

-- Table: content_item_sources
CREATE TABLE content_item_sources (
    content_item_source_id uuid DEFAULT gen_random_uuid() NOT NULL,
    content_item_id uuid NOT NULL UNIQUE,
    knowledge_source_id uuid,
    source_title varchar(500) NOT NULL,
    source_url varchar(2000) UNIQUE,
    source_publisher varchar(255),
    source_snapshot_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT content_item_sources_pkey PRIMARY KEY (content_item_source_id)
);

-- Table: specialties
CREATE TABLE specialties (
    specialty_id uuid NOT NULL,
    code varchar(80) NOT NULL UNIQUE,
    name varchar(150) NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT specialties_pkey PRIMARY KEY (specialty_id)
);

-- Table: professional_specialties
CREATE TABLE professional_specialties (
    professional_profile_id uuid NOT NULL,
    specialty_id uuid NOT NULL,
    is_primary boolean DEFAULT false NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT professional_specialties_pkey PRIMARY KEY (professional_profile_id, specialty_id)
);

-- Table: expert_availability
CREATE TABLE expert_availability (
    availability_id uuid DEFAULT gen_random_uuid() NOT NULL,
    start_at timestamptz NOT NULL,
    end_at timestamptz NOT NULL,
    channel_type varchar(30) NOT NULL,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    professional_profile_id uuid NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT expert_availability_pkey PRIMARY KEY (availability_id)
);

-- Table: expert_consultation_requests
CREATE TABLE expert_consultation_requests (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    requester_user_id uuid NOT NULL UNIQUE,
    expert_profile_id uuid NOT NULL,
    client_request_id uuid NOT NULL UNIQUE,
    topic varchar(200) NOT NULL,
    description varchar(2000) NOT NULL,
    preferred_window_start timestamptz,
    preferred_window_end timestamptz,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    reject_reason varchar(500),
    direct_conversation_id uuid,
    responded_at timestamptz,
    responded_by uuid,
    expires_at timestamptz NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT expert_consultation_requests_pkey PRIMARY KEY (id)
);

-- Table: consultation_bookings
CREATE TABLE consultation_bookings (
    booking_id uuid DEFAULT gen_random_uuid() NOT NULL,
    requester_user_id uuid NOT NULL,
    expert_profile_id uuid NOT NULL,
    availability_id uuid,
    expert_price_id uuid,
    price_band_id uuid,
    shared_summary_id uuid,
    topic varchar(500),
    scheduled_start timestamptz,
    scheduled_end timestamptz,
    price_snapshot_amount numeric,
    commission_rate_snapshot numeric,
    cancellation_policy_snapshot text,
    price_locked_at timestamptz,
    status varchar(30) NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    communication_room_id varchar(255),
    session_started_at timestamptz,
    session_ended_at timestamptz,
    session_status varchar(30),
    expert_summary text,
    technical_log_json jsonb,
    session_created_at timestamptz,
    legacy_session_id uuid,
    CONSTRAINT consultation_bookings_pkey PRIMARY KEY (booking_id)
);

-- Table: consultation_context_shares
CREATE TABLE consultation_context_shares (
    context_share_id uuid DEFAULT gen_random_uuid() NOT NULL,
    consultation_request_id uuid NOT NULL UNIQUE,
    owner_user_id uuid NOT NULL UNIQUE,
    intake_session_id uuid NOT NULL UNIQUE,
    expert_profile_id uuid NOT NULL UNIQUE,
    consent_grant_id bigint NOT NULL UNIQUE,
    idempotency_key uuid NOT NULL UNIQUE,
    journey_id uuid,
    origin_dashboard varchar(30) NOT NULL,
    origin_reference_id uuid NOT NULL,
    triage_stage varchar(20) NOT NULL,
    risk_level varchar(10) NOT NULL,
    intake_status varchar(20) NOT NULL,
    risk_summary varchar(500) NOT NULL,
    share_policy_version varchar(60) NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT consultation_context_shares_pkey PRIMARY KEY (context_share_id)
);

-- Table: consultation_context_citations
CREATE TABLE consultation_context_citations (
    citation_snapshot_id uuid DEFAULT gen_random_uuid() NOT NULL,
    context_share_id uuid NOT NULL UNIQUE,
    evidence_source_id uuid NOT NULL UNIQUE,
    organization varchar(255) NOT NULL,
    source_url varchar(1000) NOT NULL,
    source_status_at_share varchar(30) NOT NULL,
    reviewed_at timestamptz NOT NULL,
    ordinal smallint NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT consultation_context_citations_pkey PRIMARY KEY (citation_snapshot_id)
);

-- Table: expert_location_shares
CREATE TABLE expert_location_shares (
    location_share_id uuid DEFAULT gen_random_uuid() NOT NULL,
    latitude numeric(10,8) NOT NULL,
    longitude numeric(11,8) NOT NULL,
    accuracy_meters numeric(6,2),
    availability_status varchar(20),
    shared_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at timestamptz,
    consent_reference uuid,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    professional_profile_id uuid NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT expert_location_shares_pkey PRIMARY KEY (location_share_id)
);

-- Table: direct_conversations
CREATE TABLE direct_conversations (
    conversation_id uuid DEFAULT gen_random_uuid() NOT NULL,
    mother_user_id uuid NOT NULL UNIQUE,
    expert_user_id uuid NOT NULL UNIQUE,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_activity_at timestamptz,
    CONSTRAINT direct_conversations_pkey PRIMARY KEY (conversation_id)
);

-- Table: direct_conversation_read_cursors
CREATE TABLE direct_conversation_read_cursors (
    conversation_id uuid NOT NULL,
    reader_user_id uuid NOT NULL,
    last_read_at timestamptz NOT NULL,
    last_read_message_id uuid NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT direct_conversation_read_cursors_pkey PRIMARY KEY (conversation_id, reader_user_id)
);

-- Table: direct_messages
CREATE TABLE direct_messages (
    message_id uuid DEFAULT gen_random_uuid() NOT NULL,
    conversation_id uuid NOT NULL UNIQUE,
    sender_user_id uuid NOT NULL UNIQUE,
    client_message_id uuid NOT NULL UNIQUE,
    message_type varchar(30) DEFAULT '' NOT NULL,
    message_body text,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    attachment_id uuid,
    recalled_at timestamptz,
    recalled_by_user_id uuid,
    CONSTRAINT direct_messages_pkey PRIMARY KEY (message_id)
);

-- Table: conversation_calls
CREATE TABLE conversation_calls (
    call_id uuid DEFAULT gen_random_uuid() NOT NULL,
    conversation_id uuid NOT NULL,
    initiated_by_user_id uuid NOT NULL,
    call_type varchar(10) NOT NULL,
    call_status varchar(20) DEFAULT '' NOT NULL,
    zego_room_id varchar(255) NOT NULL,
    initiated_at timestamptz NOT NULL,
    answered_at timestamptz,
    ended_at timestamptz,
    duration_seconds integer,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT conversation_calls_pkey PRIMARY KEY (call_id)
);

-- Table: device_tokens
CREATE TABLE device_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL UNIQUE,
    token varchar(512) NOT NULL UNIQUE,
    platform varchar(30) NOT NULL,
    active boolean DEFAULT false NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT device_tokens_pkey PRIMARY KEY (id)
);

-- Table: notification_records
CREATE TABLE notification_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    type varchar(50) NOT NULL,
    title varchar(255) NOT NULL,
    body text NOT NULL,
    reference_id uuid,
    reference_type varchar(50),
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    fcm_message_id varchar(255),
    attempt_count integer DEFAULT 1 NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    sent_at timestamptz,
    failed_at timestamptz,
    is_read boolean DEFAULT false NOT NULL,
    read_at timestamptz,
    metadata jsonb,
    processing_started_at timestamptz,
    channel varchar(30) DEFAULT '' NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    claim_token uuid,
    care_group_id uuid,
    CONSTRAINT notification_records_pkey PRIMARY KEY (id)
);

-- Table: notification_jobs
CREATE TABLE notification_jobs (
    job_id uuid DEFAULT gen_random_uuid() NOT NULL,
    job_type varchar(20) NOT NULL,
    due_at timestamptz NOT NULL,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    attempt_count integer DEFAULT 1 NOT NULL,
    next_attempt_at timestamptz NOT NULL,
    locked_by varchar(120),
    locked_at timestamptz,
    notification_record_id uuid,
    last_error_code varchar(80),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    schedule_id uuid,
    schedule_revision bigint,
    occurrence_date date,
    local_time time,
    time_zone varchar(80),
    reminder_id uuid,
    occurrence_id uuid,
    occurrence_generation bigint,
    occurrence_scheduled_at timestamptz,
    config_revision bigint,
    offset_minutes integer,
    CONSTRAINT notification_jobs_pkey PRIMARY KEY (job_id)
);

-- Table: reminder_schedules
CREATE TABLE reminder_schedules (
    schedule_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL,
    title varchar(255) NOT NULL,
    time_zone varchar(80) NOT NULL,
    recurrence varchar(20) DEFAULT '' NOT NULL,
    start_date date NOT NULL,
    end_date date,
    active boolean DEFAULT false NOT NULL,
    revision bigint DEFAULT 1 NOT NULL,
    lock_version bigint DEFAULT 1 NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    local_times time[] DEFAULT '{}'::time[] NOT NULL,
    CONSTRAINT reminder_schedules_pkey PRIMARY KEY (schedule_id)
);

-- Table: reminder_occurrence_aliases
CREATE TABLE reminder_occurrence_aliases (
    occurrence_id uuid NOT NULL,
    reminder_definition_id uuid NOT NULL UNIQUE,
    owner_user_id uuid NOT NULL,
    scheduled_at timestamptz NOT NULL UNIQUE,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    occurrence_generation bigint DEFAULT 0 NOT NULL UNIQUE,
    CONSTRAINT reminder_occurrence_aliases_pkey PRIMARY KEY (occurrence_id)
);

-- Table: appointment_notification_configs
CREATE TABLE appointment_notification_configs (
    reminder_id uuid NOT NULL,
    time_zone varchar(80) DEFAULT 'UTC' NOT NULL,
    config_revision bigint DEFAULT 0 NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    rules_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    CONSTRAINT appointment_notification_configs_pkey PRIMARY KEY (reminder_id)
);

-- Table: knowledge_sources
CREATE TABLE knowledge_sources (
    knowledge_source_id uuid DEFAULT gen_random_uuid() NOT NULL,
    domain varchar(255) NOT NULL,
    base_url varchar(500) NOT NULL,
    organization varchar(255) NOT NULL,
    category varchar(40) NOT NULL,
    status varchar(30) NOT NULL,
    discovery_mode varchar(40) NOT NULL,
    applicable_stages text,
    added_by uuid,
    reviewed_by uuid,
    reviewed_at timestamptz,
    notes text,
    source_version varchar(80),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT knowledge_sources_pkey PRIMARY KEY (knowledge_source_id)
);

-- Table: knowledge_source_reviews
CREATE TABLE knowledge_source_reviews (
    review_id uuid DEFAULT gen_random_uuid() NOT NULL,
    knowledge_source_id uuid NOT NULL,
    previous_status varchar(30),
    new_status varchar(30) NOT NULL,
    actor_user_id uuid,
    actor_role varchar(80),
    notes text,
    changed_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT knowledge_source_reviews_pkey PRIMARY KEY (review_id)
);

-- Table: ai_moderation_policies
CREATE TABLE ai_moderation_policies (
    policy_id uuid DEFAULT gen_random_uuid() NOT NULL,
    policy_code varchar(60) NOT NULL UNIQUE,
    name varchar(150) NOT NULL,
    detection_guidance text NOT NULL,
    violation_category varchar(40) NOT NULL,
    report_category varchar(40) NOT NULL,
    severity varchar(20) NOT NULL,
    applicable_target_types varchar(100) NOT NULL,
    confidence_threshold numeric(4,3) DEFAULT 0.0 NOT NULL,
    active boolean DEFAULT false NOT NULL,
    system_default boolean DEFAULT false NOT NULL,
    version integer DEFAULT 1 NOT NULL,
    created_by uuid,
    updated_by uuid,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    reference_links text,
    reference_files text,
    CONSTRAINT ai_moderation_policies_pkey PRIMARY KEY (policy_id)
);

-- Table: ai_content_scan_jobs
CREATE TABLE ai_content_scan_jobs (
    job_id uuid DEFAULT gen_random_uuid() NOT NULL,
    target_type varchar(20) NOT NULL,
    target_id uuid NOT NULL,
    content_hash varchar(64) NOT NULL,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    attempt_count integer DEFAULT 1 NOT NULL,
    next_attempt_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    locked_by varchar(100),
    locked_at timestamptz,
    last_error_code varchar(80),
    force_rescan boolean DEFAULT false NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completed_at timestamptz,
    CONSTRAINT ai_content_scan_jobs_pkey PRIMARY KEY (job_id)
);

-- Table: ai_content_assessments
CREATE TABLE ai_content_assessments (
    assessment_id uuid DEFAULT gen_random_uuid() NOT NULL,
    job_id uuid,
    target_type varchar(20) NOT NULL,
    target_id uuid NOT NULL,
    content_hash varchar(64) NOT NULL,
    policy_set_hash varchar(64) NOT NULL,
    provider varchar(30) DEFAULT '' NOT NULL,
    model varchar(60) NOT NULL,
    status varchar(20) NOT NULL,
    classification varchar(20),
    overall_severity varchar(20),
    confidence numeric(4,3),
    recommended_action varchar(30),
    explanation varchar(1000),
    error_code varchar(80),
    attempt_count integer DEFAULT 1 NOT NULL,
    latency_ms bigint,
    prompt_tokens integer,
    output_tokens integer,
    moderation_case_id uuid,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completed_at timestamptz,
    matches_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    CONSTRAINT ai_content_assessments_pkey PRIMARY KEY (assessment_id)
);

-- Table: moderation_cases
CREATE TABLE moderation_cases (
    moderation_case_id uuid DEFAULT gen_random_uuid() NOT NULL,
    reporter_user_id uuid,
    assigned_moderator_id uuid,
    target_type varchar(30) NOT NULL,
    target_id uuid NOT NULL,
    reason_code varchar(80),
    description text,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    opened_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    resolved_at timestamptz,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    report_source varchar(20) DEFAULT '' NOT NULL,
    reverted_at timestamptz,
    reverted_by uuid,
    priority varchar(20) DEFAULT '' NOT NULL,
    claimed_at timestamptz,
    ai_feedback_decision varchar(20),
    ai_feedback_reason text,
    ai_feedback_by uuid,
    ai_feedback_at timestamptz,
    ai_feedback_assessment_id uuid,
    CONSTRAINT moderation_cases_pkey PRIMARY KEY (moderation_case_id)
);

-- Table: health_context_memories
CREATE TABLE health_context_memories (
    memory_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    care_subject_id uuid,
    triage_session_id uuid,
    related_stage varchar(20) NOT NULL,
    summary_text text NOT NULL,
    memory_payload_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at timestamptz,
    deleted_at timestamptz,
    mother_profile_id uuid,
    baby_profile_id uuid,
    CONSTRAINT health_context_memories_pkey PRIMARY KEY (memory_id)
);

-- Table: triage_sessions
CREATE TABLE triage_sessions (
    triage_session_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    care_subject_id uuid,
    stage varchar(20),
    profile_context_id uuid,
    risk_level varchar(10),
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    emergency boolean DEFAULT false NOT NULL,
    disclaimer_version varchar(80),
    input_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    result_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    conversation_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    schema_version varchar(30) DEFAULT '' NOT NULL,
    content_hash varchar(128),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completed_at timestamptz,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    baby_profile_id uuid,
    mother_profile_id uuid,
    client_request_id varchar(64),
    symptoms text NOT NULL,
    raw_ai_response text,
    disclaimer_text text,
    created_by uuid NOT NULL,
    symptom_list jsonb,
    duration_days integer,
    intensity varchar(20),
    emergency_flag boolean,
    extracted_at timestamptz,
    structured_created_by varchar(255),
    journey_id uuid,
    origin_dashboard varchar(30),
    origin_reference_id uuid,
    continuation_token uuid,
    continuation_expires_at timestamptz,
    continuation_acknowledged_at timestamptz,
    CONSTRAINT triage_sessions_pkey PRIMARY KEY (triage_session_id)
);

-- Table: triage_session_evidence
CREATE TABLE triage_session_evidence (
    evidence_id uuid DEFAULT gen_random_uuid() NOT NULL,
    triage_session_id uuid NOT NULL UNIQUE,
    evidence_type varchar(40) NOT NULL UNIQUE,
    claim_code varchar(100),
    claim_text text NOT NULL,
    knowledge_source_id uuid,
    citation_url text,
    citation_domain varchar(255),
    source_version varchar(80),
    source_snapshot_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    content_hash varchar(128) NOT NULL UNIQUE,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT triage_session_evidence_pkey PRIMARY KEY (evidence_id)
);

-- Table: red_flag_rules
CREATE TABLE red_flag_rules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    keyword varchar(255) NOT NULL UNIQUE,
    severity varchar(20) NOT NULL,
    action varchar(20) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    is_system_default boolean DEFAULT false NOT NULL,
    created_by uuid,
    updated_by uuid,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT red_flag_rules_pkey PRIMARY KEY (id)
);

-- Table: safety_monitoring_sessions
CREATE TABLE safety_monitoring_sessions (
    monitoring_session_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    status varchar(10) DEFAULT 'ACTIVE' NOT NULL,
    sensitivity_level varchar(10) DEFAULT '' NOT NULL,
    started_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ended_at timestamptz,
    created_by uuid,
    CONSTRAINT safety_monitoring_sessions_pkey PRIMARY KEY (monitoring_session_id)
);

-- Table: administrative_areas
CREATE TABLE administrative_areas (
    administrative_area_id uuid DEFAULT gen_random_uuid() NOT NULL,
    parent_area_id uuid,
    area_type varchar(30) NOT NULL,
    code varchar(80) NOT NULL UNIQUE,
    name varchar(255) NOT NULL,
    legacy_code varchar(80),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    name_en varchar(255),
    CONSTRAINT administrative_areas_pkey PRIMARY KEY (administrative_area_id)
);

-- Table: care_facilities
CREATE TABLE care_facilities (
    facility_id uuid DEFAULT gen_random_uuid() NOT NULL,
    name varchar(255) NOT NULL,
    facility_type varchar(50),
    address varchar(500),
    latitude numeric(10,8),
    longitude numeric(11,8),
    phone varchar(30),
    opening_hours_json jsonb,
    source_type varchar(30),
    verification_status varchar(30) DEFAULT 'UNVERIFIED' NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    facility_level varchar(50),
    ownership_type varchar(30),
    province_id varchar(2),
    district_id varchar(4),
    external_source_id varchar(150),
    is_active boolean DEFAULT true NOT NULL,
    is_searchable boolean DEFAULT false NOT NULL,
    administrative_area_id uuid,
    CONSTRAINT care_facilities_pkey PRIMARY KEY (facility_id)
);

-- Table: attachments
CREATE TABLE attachments (
    attachment_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL,
    uploader_role varchar(30) DEFAULT '' NOT NULL,
    storage_key varchar(500) NOT NULL UNIQUE,
    original_name varchar(255) NOT NULL,
    mime_type varchar(100) NOT NULL,
    file_size_bytes bigint NOT NULL,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    checksum varchar(64),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    attachment_category varchar(40) DEFAULT '' NOT NULL,
    credential_type varchar(50),
    credential_number varchar(100),
    issuer varchar(200),
    issued_date date,
    expiry_date date,
    review_status varchar(30),
    review_note text,
    reviewed_by uuid,
    reviewed_at timestamp,
    file_url text,
    file_id uuid,
    health_record_id uuid,
    CONSTRAINT attachments_pkey PRIMARY KEY (attachment_id)
);

-- Table: audit_events
CREATE TABLE audit_events (
    audit_event_id uuid DEFAULT gen_random_uuid() NOT NULL,
    actor_user_id uuid,
    event_category varchar(80) NOT NULL,
    subject_user_id uuid,
    subject_reference_id uuid,
    resource_type varchar(100),
    resource_id uuid,
    purpose varchar(255),
    decision varchar(50),
    ip_hash varchar(128),
    before_payload_jsonb jsonb,
    after_payload_jsonb jsonb,
    checksum varchar(128),
    occurred_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    note_text text,
    event_origin varchar(255) DEFAULT '' NOT NULL,
    ip_address varchar(80),
    user_agent varchar(500),
    payload jsonb,
    correlation_id uuid,
    severity varchar(20) DEFAULT '' NOT NULL,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    reviewed_at timestamptz,
    reviewed_by uuid,
    security_event_id uuid,
    actor_type varchar(20),
    actor_service varchar(80),
    reason_code varchar(80),
    care_context_type varchar(10),
    care_context_id uuid,
    template_version_id uuid,
    checklist_task_instance_id uuid,
    legal_hold boolean DEFAULT false NOT NULL,
    CONSTRAINT audit_events_pkey PRIMARY KEY (audit_event_id)
);

-- Table: system_configurations
CREATE TABLE system_configurations (
    system_configuration_id uuid DEFAULT gen_random_uuid() NOT NULL,
    api_rate_limit integer NOT NULL,
    connection_timeout_ms integer NOT NULL,
    max_upload_size_mb integer NOT NULL,
    administrator_email varchar(254) NOT NULL,
    email_alerts boolean DEFAULT false NOT NULL,
    sms_alerts boolean DEFAULT false NOT NULL,
    webhook_alerts boolean DEFAULT false NOT NULL,
    ai_moderation_enabled boolean DEFAULT false NOT NULL,
    maintenance_mode_enabled boolean DEFAULT false NOT NULL,
    updated_by uuid NOT NULL,
    row_version bigint DEFAULT 0 NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT system_configurations_pkey PRIMARY KEY (system_configuration_id)
);

-- Table: safety_events
CREATE TABLE safety_events (
    safety_event_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    care_subject_id uuid,
    monitoring_session_id uuid,
    detected_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    event_type varchar(50) NOT NULL,
    confidence_score numeric,
    peak_acceleration numeric,
    angular_velocity numeric,
    inactivity_seconds integer,
    response_type varchar(30),
    response_at timestamptz,
    false_positive_reason text,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    location_snapshot_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    record_type varchar(255) DEFAULT '' NOT NULL,
    magnitude numeric(10,4),
    user_latitude numeric(10,7),
    user_longitude numeric(10,7),
    client_detected_at timestamptz,
    resolved_at timestamptz,
    countdown_deadline_at timestamptz,
    response_reason varchar(500),
    escalation_started_at timestamptz,
    emergency_session_id uuid,
    created_by_user_id uuid,
    alert_successful_recipient_count integer DEFAULT 0 NOT NULL,
    alert_failed_recipient_count integer DEFAULT 0 NOT NULL,
    action_type varchar(40),
    action_status varchar(20),
    actor_type varchar(20),
    attempt_number integer,
    accuracy_meters numeric(6,2),
    captured_at timestamptz,
    care_facility_id uuid,
    consent_status varchar(20),
    context_id uuid,
    context_type varchar(50),
    delivered_at timestamptz,
    delivery_status varchar(30),
    device_token_id uuid,
    expires_at timestamptz,
    latitude numeric(10,8),
    longitude numeric(11,8),
    location_included boolean,
    notification_record_id uuid,
    parent_event_id uuid,
    reason varchar(500),
    recipient_count integer,
    recipient_user_id uuid,
    responded_at timestamptz,
    risk_level varchar(20),
    triage_handoff_id uuid,
    attempt_status varchar(20),
    started_at timestamptz,
    completed_at timestamptz,
    lease_expires_at timestamptz,
    successful_recipient_count integer,
    failed_recipient_count integer,
    summary text,
    action_phase varchar(30),
    owner_user_id uuid,
    CONSTRAINT safety_events_pkey PRIMARY KEY (safety_event_id)
);

-- Table: expense_entries
CREATE TABLE expense_entries (
    expense_entry_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL,
    care_subject_id uuid,
    mother_journey_id uuid,
    category varchar(80),
    amount numeric(38,2) NOT NULL,
    currency varchar(10) DEFAULT '' NOT NULL,
    expense_date date NOT NULL,
    note text,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT expense_entries_pkey PRIMARY KEY (expense_entry_id)
);

-- Table: checklist_task_instances
CREATE TABLE checklist_task_instances (
    checklist_task_instance_id uuid DEFAULT gen_random_uuid() NOT NULL,
    checklist_instance_id uuid NOT NULL,
    template_version_id uuid,
    template_item_version_id uuid,
    task_key char(64) NOT NULL UNIQUE,
    key_version varchar(10) DEFAULT '' NOT NULL,
    title_snapshot varchar(500) NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    is_required boolean DEFAULT false NOT NULL,
    target_subject varchar(10) NOT NULL,
    due_at timestamptz,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL,
    lock_version bigint DEFAULT 1 NOT NULL,
    completed_at timestamptz,
    skipped_at timestamptz,
    cancelled_at timestamptz,
    action_reason_code varchar(80),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    category varchar(20) DEFAULT '' NOT NULL,
    CONSTRAINT checklist_task_instances_pkey PRIMARY KEY (checklist_task_instance_id)
);

-- ============================================================================
-- 2. FOREIGN KEY CONSTRAINTS
-- ============================================================================

ALTER TABLE administrative_areas ADD CONSTRAINT fk_administrative_areas_parent_area_id FOREIGN KEY (parent_area_id) REFERENCES administrative_areas (administrative_area_id);
ALTER TABLE ai_content_assessments ADD CONSTRAINT fk_ai_content_assessments_job_id FOREIGN KEY (job_id) REFERENCES ai_content_scan_jobs (job_id);
ALTER TABLE ai_content_assessments ADD CONSTRAINT fk_ai_content_assessments_moderation_case_id FOREIGN KEY (moderation_case_id) REFERENCES moderation_cases (moderation_case_id);
ALTER TABLE ai_moderation_policies ADD CONSTRAINT fk_ai_moderation_policies_created_by FOREIGN KEY (created_by) REFERENCES users (user_id);
ALTER TABLE ai_moderation_policies ADD CONSTRAINT fk_ai_moderation_policies_updated_by FOREIGN KEY (updated_by) REFERENCES users (user_id);
ALTER TABLE appointment_notification_configs ADD CONSTRAINT fk_appointment_notification_configs_reminder_id FOREIGN KEY (reminder_id) REFERENCES care_tasks (task_id) ON DELETE CASCADE;
ALTER TABLE attachments ADD CONSTRAINT fk_attachments_health_record_id FOREIGN KEY (health_record_id) REFERENCES health_records (health_record_id);
ALTER TABLE attachments ADD CONSTRAINT fk_attachments_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (user_id);
ALTER TABLE attachments ADD CONSTRAINT fk_attachments_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (user_id);
ALTER TABLE audit_events ADD CONSTRAINT fk_audit_events_actor_user_id FOREIGN KEY (actor_user_id) REFERENCES users (user_id);
ALTER TABLE audit_events ADD CONSTRAINT fk_audit_events_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (user_id);
ALTER TABLE audit_events ADD CONSTRAINT fk_audit_events_security_event_id FOREIGN KEY (security_event_id) REFERENCES audit_events (audit_event_id);
ALTER TABLE audit_events ADD CONSTRAINT fk_audit_events_subject_user_id FOREIGN KEY (subject_user_id) REFERENCES users (user_id);
ALTER TABLE auth_challenges ADD CONSTRAINT fk_auth_challenges_user_id FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE auth_sessions ADD CONSTRAINT fk_auth_sessions_user_id FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE care_group_members ADD CONSTRAINT fk_care_group_members_care_group_id FOREIGN KEY (care_group_id) REFERENCES care_groups (care_group_id);
ALTER TABLE care_group_members ADD CONSTRAINT fk_care_group_members_user_id FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE care_groups ADD CONSTRAINT fk_care_groups_baby_id FOREIGN KEY (baby_id) REFERENCES care_subjects (care_subject_id);
ALTER TABLE care_groups ADD CONSTRAINT fk_care_groups_journey_id FOREIGN KEY (journey_id) REFERENCES mother_journeys (journey_id);
ALTER TABLE care_groups ADD CONSTRAINT fk_care_groups_linked_baby_profile_id FOREIGN KEY (linked_baby_profile_id) REFERENCES care_subjects (care_subject_id);
ALTER TABLE care_groups ADD CONSTRAINT fk_care_groups_linked_journey_id FOREIGN KEY (linked_journey_id) REFERENCES mother_journeys (journey_id);
ALTER TABLE care_groups ADD CONSTRAINT fk_care_groups_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (user_id);
ALTER TABLE care_item_templates ADD CONSTRAINT fk_care_item_templates_parent_template_id FOREIGN KEY (parent_template_id) REFERENCES care_item_templates (template_id);
ALTER TABLE care_subjects ADD CONSTRAINT fk_care_subjects_mother_journey_id FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys (journey_id);
ALTER TABLE care_subjects ADD CONSTRAINT fk_care_subjects_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (user_id);
ALTER TABLE care_subjects ADD CONSTRAINT fk_care_subjects_person_id FOREIGN KEY (person_id) REFERENCES users (user_id);
ALTER TABLE care_tasks ADD CONSTRAINT fk_care_tasks_assignee_user_id FOREIGN KEY (assignee_user_id) REFERENCES users (user_id);
ALTER TABLE care_tasks ADD CONSTRAINT fk_care_tasks_care_group_id FOREIGN KEY (care_group_id) REFERENCES care_groups (care_group_id);
ALTER TABLE care_tasks ADD CONSTRAINT fk_care_tasks_care_subject_id FOREIGN KEY (care_subject_id) REFERENCES care_subjects (care_subject_id);
ALTER TABLE care_tasks ADD CONSTRAINT fk_care_tasks_creator_user_id FOREIGN KEY (creator_user_id) REFERENCES users (user_id);
ALTER TABLE care_tasks ADD CONSTRAINT fk_care_tasks_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (user_id);
ALTER TABLE checklist_action_commands ADD CONSTRAINT fk_checklist_action_commands_actor_user_id FOREIGN KEY (actor_user_id) REFERENCES users (user_id);
ALTER TABLE checklist_action_commands ADD CONSTRAINT fk_checklist_action_commands_reminder_definition_id FOREIGN KEY (reminder_definition_id) REFERENCES care_tasks (task_id);
ALTER TABLE checklist_instances ADD CONSTRAINT fk_checklist_instances_journey_context_id FOREIGN KEY (journey_context_id) REFERENCES mother_journeys (journey_id);
ALTER TABLE checklist_instances ADD CONSTRAINT fk_checklist_instances_template_lineage_id FOREIGN KEY (template_lineage_id) REFERENCES care_item_templates (template_id);
ALTER TABLE checklist_instances ADD CONSTRAINT fk_checklist_instances_recipient_user_id FOREIGN KEY (recipient_user_id) REFERENCES users (user_id);
ALTER TABLE checklist_task_instances ADD CONSTRAINT fk_checklist_task_instances_checklist_instance_id FOREIGN KEY (checklist_instance_id) REFERENCES checklist_instances (checklist_instance_id) ON DELETE CASCADE;
ALTER TABLE checklist_task_instances ADD CONSTRAINT fk_checklist_task_instances_template_item_version_id FOREIGN KEY (template_item_version_id) REFERENCES care_item_templates (template_id);
ALTER TABLE checklist_task_instances ADD CONSTRAINT fk_checklist_task_instances_template_version_id FOREIGN KEY (template_version_id) REFERENCES care_item_templates (template_id);
ALTER TABLE community_content ADD CONSTRAINT fk_community_content_author_user_id FOREIGN KEY (author_user_id) REFERENCES users (user_id);
ALTER TABLE community_content ADD CONSTRAINT fk_community_content_parent_content_id FOREIGN KEY (parent_content_id) REFERENCES community_content (content_id);
ALTER TABLE community_content ADD CONSTRAINT fk_community_content_topic_id FOREIGN KEY (topic_id) REFERENCES community_topics (id);
ALTER TABLE community_interactions ADD CONSTRAINT fk_community_interactions_actor_user_id FOREIGN KEY (actor_user_id) REFERENCES users (user_id);
ALTER TABLE community_interactions ADD CONSTRAINT fk_community_interactions_content_id FOREIGN KEY (content_id) REFERENCES community_content (content_id);
ALTER TABLE community_interactions ADD CONSTRAINT fk_community_interactions_topic_id FOREIGN KEY (topic_id) REFERENCES community_topics (id);
ALTER TABLE community_topics ADD CONSTRAINT fk_community_topics_parent_id FOREIGN KEY (parent_id) REFERENCES community_topics (id);
ALTER TABLE consultation_bookings ADD CONSTRAINT fk_consultation_bookings_availability_id FOREIGN KEY (availability_id) REFERENCES expert_availability (availability_id);
ALTER TABLE consultation_bookings ADD CONSTRAINT fk_consultation_bookings_expert_profile_id FOREIGN KEY (expert_profile_id) REFERENCES users (user_id);
ALTER TABLE consultation_bookings ADD CONSTRAINT fk_consultation_bookings_requester_user_id FOREIGN KEY (requester_user_id) REFERENCES users (user_id);
ALTER TABLE consultation_context_citations ADD CONSTRAINT fk_consultation_context_citations_context_share_id FOREIGN KEY (context_share_id) REFERENCES consultation_context_shares (context_share_id);
ALTER TABLE consultation_context_citations ADD CONSTRAINT fk_consultation_context_citations_evidence_source_id FOREIGN KEY (evidence_source_id) REFERENCES knowledge_sources (knowledge_source_id);
ALTER TABLE consultation_context_shares ADD CONSTRAINT fk_consultation_context_shares_consent_grant_id FOREIGN KEY (consent_grant_id) REFERENCES data_permissions (permission_id);
ALTER TABLE consultation_context_shares ADD CONSTRAINT fk_consultation_context_shares_expert_profile_id FOREIGN KEY (expert_profile_id) REFERENCES users (user_id);
ALTER TABLE consultation_context_shares ADD CONSTRAINT fk_consultation_context_shares_intake_session_id FOREIGN KEY (intake_session_id) REFERENCES triage_sessions (triage_session_id);
ALTER TABLE consultation_context_shares ADD CONSTRAINT fk_consultation_context_shares_journey_id FOREIGN KEY (journey_id) REFERENCES mother_journeys (journey_id);
ALTER TABLE consultation_context_shares ADD CONSTRAINT fk_consultation_context_shares_consultation_request_id FOREIGN KEY (consultation_request_id) REFERENCES expert_consultation_requests (id);
ALTER TABLE content_item_sources ADD CONSTRAINT fk_content_item_sources_content_item_id FOREIGN KEY (content_item_id) REFERENCES content_items (content_item_id);
ALTER TABLE content_item_sources ADD CONSTRAINT fk_content_item_sources_knowledge_source_id FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources (knowledge_source_id);
ALTER TABLE content_item_topics ADD CONSTRAINT fk_content_item_topics_content_item_id FOREIGN KEY (content_item_id) REFERENCES content_items (content_item_id);
ALTER TABLE content_item_topics ADD CONSTRAINT fk_content_item_topics_topic_id FOREIGN KEY (topic_id) REFERENCES community_topics (id);
ALTER TABLE conversation_calls ADD CONSTRAINT fk_conversation_calls_conversation_id FOREIGN KEY (conversation_id) REFERENCES direct_conversations (conversation_id);
ALTER TABLE conversation_calls ADD CONSTRAINT fk_conversation_calls_initiated_by_user_id FOREIGN KEY (initiated_by_user_id) REFERENCES users (user_id);
ALTER TABLE development_milestones ADD CONSTRAINT fk_development_milestones_baby_id FOREIGN KEY (baby_id) REFERENCES care_subjects (care_subject_id);
ALTER TABLE development_milestones ADD CONSTRAINT fk_development_milestones_recorded_by FOREIGN KEY (recorded_by) REFERENCES users (user_id);
ALTER TABLE development_milestones ADD CONSTRAINT fk_development_milestones_care_subject_id FOREIGN KEY (care_subject_id) REFERENCES care_subjects (care_subject_id);
ALTER TABLE device_tokens ADD CONSTRAINT fk_device_tokens_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;
ALTER TABLE direct_conversation_read_cursors ADD CONSTRAINT fk_direct_conversation_read_cursors_conversation_id FOREIGN KEY (conversation_id) REFERENCES direct_conversations (conversation_id) ON DELETE CASCADE;
ALTER TABLE direct_conversation_read_cursors ADD CONSTRAINT fk_direct_conversation_read_cursors_last_read_message_id FOREIGN KEY (last_read_message_id) REFERENCES direct_messages (message_id);
ALTER TABLE direct_conversation_read_cursors ADD CONSTRAINT fk_direct_conversation_read_cursors_reader_user_id FOREIGN KEY (reader_user_id) REFERENCES users (user_id) ON DELETE CASCADE;
ALTER TABLE direct_conversations ADD CONSTRAINT fk_direct_conversations_expert_user_id FOREIGN KEY (expert_user_id) REFERENCES users (user_id);
ALTER TABLE direct_conversations ADD CONSTRAINT fk_direct_conversations_mother_user_id FOREIGN KEY (mother_user_id) REFERENCES users (user_id);
ALTER TABLE direct_messages ADD CONSTRAINT fk_direct_messages_attachment_id FOREIGN KEY (attachment_id) REFERENCES attachments (attachment_id);
ALTER TABLE direct_messages ADD CONSTRAINT fk_direct_messages_conversation_id FOREIGN KEY (conversation_id) REFERENCES direct_conversations (conversation_id);
ALTER TABLE direct_messages ADD CONSTRAINT fk_direct_messages_sender_user_id FOREIGN KEY (sender_user_id) REFERENCES users (user_id);
ALTER TABLE expense_entries ADD CONSTRAINT fk_expense_entries_care_subject_id FOREIGN KEY (care_subject_id) REFERENCES care_subjects (care_subject_id);
ALTER TABLE expense_entries ADD CONSTRAINT fk_expense_entries_mother_journey_id FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys (journey_id);
ALTER TABLE expense_entries ADD CONSTRAINT fk_expense_entries_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (user_id);
ALTER TABLE expert_consultation_requests ADD CONSTRAINT fk_expert_consultation_requests_direct_conversation_id FOREIGN KEY (direct_conversation_id) REFERENCES direct_conversations (conversation_id);
ALTER TABLE expert_consultation_requests ADD CONSTRAINT fk_expert_consultation_requests_expert_profile_id FOREIGN KEY (expert_profile_id) REFERENCES users (user_id);
ALTER TABLE expert_consultation_requests ADD CONSTRAINT fk_expert_consultation_requests_requester_user_id FOREIGN KEY (requester_user_id) REFERENCES users (user_id);
ALTER TABLE expert_consultation_requests ADD CONSTRAINT fk_expert_consultation_requests_responded_by FOREIGN KEY (responded_by) REFERENCES users (user_id);
ALTER TABLE health_context_memories ADD CONSTRAINT fk_health_context_memories_care_subject_id FOREIGN KEY (care_subject_id) REFERENCES care_subjects (care_subject_id);
ALTER TABLE health_context_memories ADD CONSTRAINT fk_health_context_memories_triage_session_id FOREIGN KEY (triage_session_id) REFERENCES triage_sessions (triage_session_id);
ALTER TABLE health_context_memories ADD CONSTRAINT fk_health_context_memories_user_id FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE health_observations ADD CONSTRAINT fk_health_observations_source_record_id FOREIGN KEY (source_record_id) REFERENCES maternal_exercise_sessions (exercise_session_id);
ALTER TABLE health_records ADD CONSTRAINT fk_health_records_baby_id FOREIGN KEY (baby_id) REFERENCES care_subjects (care_subject_id);
ALTER TABLE health_records ADD CONSTRAINT fk_health_records_journey_id FOREIGN KEY (journey_id) REFERENCES mother_journeys (journey_id);
ALTER TABLE health_records ADD CONSTRAINT fk_health_records_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (user_id);
ALTER TABLE knowledge_source_reviews ADD CONSTRAINT fk_knowledge_source_reviews_actor_user_id FOREIGN KEY (actor_user_id) REFERENCES users (user_id);
ALTER TABLE knowledge_source_reviews ADD CONSTRAINT fk_knowledge_source_reviews_knowledge_source_id FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources (knowledge_source_id);
ALTER TABLE knowledge_sources ADD CONSTRAINT fk_knowledge_sources_added_by FOREIGN KEY (added_by) REFERENCES users (user_id);
ALTER TABLE knowledge_sources ADD CONSTRAINT fk_knowledge_sources_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (user_id);
ALTER TABLE maternal_exercise_sessions ADD CONSTRAINT fk_maternal_exercise_sessions_mother_journey_id FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys (journey_id);
ALTER TABLE maternal_exercise_sessions ADD CONSTRAINT fk_maternal_exercise_sessions_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (user_id);
ALTER TABLE maternal_exercise_sessions ADD CONSTRAINT fk_maternal_exercise_sessions_posture_config_id FOREIGN KEY (posture_config_id) REFERENCES care_item_templates (template_id);
ALTER TABLE maternal_exercise_sessions ADD CONSTRAINT fk_maternal_exercise_sessions_safety_observation_id FOREIGN KEY (safety_observation_id) REFERENCES health_observations (health_observation_id);
ALTER TABLE maternal_exercise_sessions ADD CONSTRAINT fk_maternal_exercise_sessions_exercise_template_id FOREIGN KEY (exercise_template_id) REFERENCES care_item_templates (template_id);
ALTER TABLE moderation_cases ADD CONSTRAINT fk_moderation_cases_ai_feedback_assessment_id FOREIGN KEY (ai_feedback_assessment_id) REFERENCES ai_content_assessments (assessment_id);
ALTER TABLE moderation_cases ADD CONSTRAINT fk_moderation_cases_ai_feedback_by FOREIGN KEY (ai_feedback_by) REFERENCES users (user_id);
ALTER TABLE moderation_cases ADD CONSTRAINT fk_moderation_cases_assigned_moderator_id FOREIGN KEY (assigned_moderator_id) REFERENCES users (user_id);
ALTER TABLE moderation_cases ADD CONSTRAINT fk_moderation_cases_reporter_user_id FOREIGN KEY (reporter_user_id) REFERENCES users (user_id);
ALTER TABLE mother_journeys ADD CONSTRAINT fk_mother_journeys_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (user_id);
ALTER TABLE mother_journeys ADD CONSTRAINT fk_mother_journeys_care_subject_id FOREIGN KEY (care_subject_id) REFERENCES care_subjects (care_subject_id);
ALTER TABLE notification_jobs ADD CONSTRAINT fk_notification_jobs_notification_record_id FOREIGN KEY (notification_record_id) REFERENCES notification_records (id) ON DELETE SET NULL;
ALTER TABLE notification_jobs ADD CONSTRAINT fk_notification_jobs_reminder_id FOREIGN KEY (reminder_id) REFERENCES care_tasks (task_id) ON DELETE CASCADE;
ALTER TABLE notification_jobs ADD CONSTRAINT fk_notification_jobs_schedule_id FOREIGN KEY (schedule_id) REFERENCES reminder_schedules (schedule_id) ON DELETE CASCADE;
ALTER TABLE notification_records ADD CONSTRAINT fk_notification_records_care_group_id FOREIGN KEY (care_group_id) REFERENCES care_groups (care_group_id);
ALTER TABLE notification_records ADD CONSTRAINT fk_notification_records_user_id FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE;
ALTER TABLE professional_specialties ADD CONSTRAINT fk_professional_specialties_professional_profile_id FOREIGN KEY (professional_profile_id) REFERENCES users (user_id);
ALTER TABLE professional_specialties ADD CONSTRAINT fk_professional_specialties_specialty_id FOREIGN KEY (specialty_id) REFERENCES specialties (specialty_id);
ALTER TABLE red_flag_rules ADD CONSTRAINT fk_red_flag_rules_created_by FOREIGN KEY (created_by) REFERENCES users (user_id);
ALTER TABLE red_flag_rules ADD CONSTRAINT fk_red_flag_rules_updated_by FOREIGN KEY (updated_by) REFERENCES users (user_id);
ALTER TABLE reminder_schedules ADD CONSTRAINT fk_reminder_schedules_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (user_id) ON DELETE CASCADE;
ALTER TABLE safety_events ADD CONSTRAINT fk_safety_events_care_facility_id FOREIGN KEY (care_facility_id) REFERENCES care_facilities (facility_id);
ALTER TABLE safety_events ADD CONSTRAINT fk_safety_events_care_subject_id FOREIGN KEY (care_subject_id) REFERENCES care_subjects (care_subject_id);
ALTER TABLE safety_events ADD CONSTRAINT fk_safety_events_created_by_user_id FOREIGN KEY (created_by_user_id) REFERENCES users (user_id);
ALTER TABLE safety_events ADD CONSTRAINT fk_safety_events_device_token_id FOREIGN KEY (device_token_id) REFERENCES device_tokens (id);
ALTER TABLE safety_events ADD CONSTRAINT fk_safety_events_emergency_session_id FOREIGN KEY (emergency_session_id) REFERENCES safety_events (safety_event_id);
ALTER TABLE safety_events ADD CONSTRAINT fk_safety_events_monitoring_session_id FOREIGN KEY (monitoring_session_id) REFERENCES safety_monitoring_sessions (monitoring_session_id);
ALTER TABLE safety_events ADD CONSTRAINT fk_safety_events_notification_record_id FOREIGN KEY (notification_record_id) REFERENCES notification_records (id);
ALTER TABLE safety_events ADD CONSTRAINT fk_safety_events_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (user_id);
ALTER TABLE safety_events ADD CONSTRAINT fk_safety_events_parent_event_id FOREIGN KEY (parent_event_id) REFERENCES safety_events (safety_event_id);
ALTER TABLE safety_events ADD CONSTRAINT fk_safety_events_recipient_user_id FOREIGN KEY (recipient_user_id) REFERENCES users (user_id);
ALTER TABLE safety_events ADD CONSTRAINT fk_safety_events_user_id FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE safety_monitoring_sessions ADD CONSTRAINT fk_safety_monitoring_sessions_created_by FOREIGN KEY (created_by) REFERENCES users (user_id);
ALTER TABLE safety_monitoring_sessions ADD CONSTRAINT fk_safety_monitoring_sessions_user_id FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE system_configurations ADD CONSTRAINT fk_system_configurations_updated_by FOREIGN KEY (updated_by) REFERENCES users (user_id);
ALTER TABLE triage_session_evidence ADD CONSTRAINT fk_triage_session_evidence_knowledge_source_id FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources (knowledge_source_id);
ALTER TABLE triage_session_evidence ADD CONSTRAINT fk_triage_session_evidence_triage_session_id FOREIGN KEY (triage_session_id) REFERENCES triage_sessions (triage_session_id);
ALTER TABLE triage_sessions ADD CONSTRAINT fk_triage_sessions_journey_id FOREIGN KEY (journey_id) REFERENCES mother_journeys (journey_id);
ALTER TABLE triage_sessions ADD CONSTRAINT fk_triage_sessions_care_subject_id FOREIGN KEY (care_subject_id) REFERENCES care_subjects (care_subject_id);
ALTER TABLE triage_sessions ADD CONSTRAINT fk_triage_sessions_user_id FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE users ADD CONSTRAINT fk_users_facility_id FOREIGN KEY (facility_id) REFERENCES care_facilities (facility_id);
ALTER TABLE vaccination_records ADD CONSTRAINT fk_vaccination_records_baby_id FOREIGN KEY (baby_id) REFERENCES care_subjects (care_subject_id);
ALTER TABLE vaccination_records ADD CONSTRAINT fk_vaccination_records_proof_record_id FOREIGN KEY (proof_record_id) REFERENCES health_records (health_record_id);
ALTER TABLE vaccination_records ADD CONSTRAINT fk_vaccination_records_vaccination_schedule_id FOREIGN KEY (vaccination_schedule_id) REFERENCES vaccination_schedules (vaccination_schedule_id);
ALTER TABLE vaccination_records ADD CONSTRAINT fk_vaccination_records_care_subject_id FOREIGN KEY (care_subject_id) REFERENCES care_subjects (care_subject_id);

-- End of Schema
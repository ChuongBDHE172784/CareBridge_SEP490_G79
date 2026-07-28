-- ============================================================================
-- CareBridge canonical database initialization
--
-- Purpose:
--   * Bootstrap a NEW PostgreSQL / Supabase database.
--   * Create only the canonical application schema.
--   * Exclude legacy-table reconciliation and historical data migration.
--
-- Important:
--   * Run this file once on a fresh database.
--   * Do NOT use it to upgrade a database containing legacy CareBridge tables.
--     Use the original Flyway convergence migration for that scenario.
--   * Reference data is separated into V2__seed_reference_data.sql.
-- ============================================================================

BEGIN;

SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '15min';
SET LOCAL search_path = public, extensions, pg_catalog;

-- ============================================================================
-- 1. Canonical tables
-- ============================================================================
CREATE TABLE public.account_deletion_requests (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    status character varying(30) DEFAULT 'PENDING'::character varying NOT NULL,
    reason text,
    requested_at timestamp with time zone DEFAULT now() NOT NULL,
    scheduled_for timestamp with time zone,
    processed_at timestamp with time zone,
    processed_by uuid,
    notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT account_deletion_requests_pkey PRIMARY KEY (id)
);

CREATE TABLE public.administrative_areas (
    administrative_area_id uuid DEFAULT gen_random_uuid() NOT NULL,
    parent_area_id uuid,
    area_type character varying(30) NOT NULL,
    code character varying(80) NOT NULL,
    name character varying(255) NOT NULL,
    legacy_code character varying(80),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    name_en character varying(255),
    CONSTRAINT administrative_areas_pkey PRIMARY KEY (administrative_area_id)
);

CREATE TABLE public.ai_content_assessments (
    assessment_id uuid DEFAULT gen_random_uuid() NOT NULL,
    job_id uuid,
    target_type character varying(20) NOT NULL,
    target_id uuid NOT NULL,
    content_hash character varying(64) NOT NULL,
    policy_set_hash character varying(64) NOT NULL,
    provider character varying(30) DEFAULT 'GEMINI'::character varying NOT NULL,
    model character varying(60) NOT NULL,
    status character varying(20) NOT NULL,
    classification character varying(20),
    overall_severity character varying(20),
    confidence numeric(4,3),
    recommended_action character varying(30),
    explanation character varying(1000),
    error_code character varying(80),
    attempt_count integer DEFAULT 1 NOT NULL,
    latency_ms bigint,
    prompt_tokens integer,
    output_tokens integer,
    moderation_case_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    completed_at timestamp with time zone,
    matches_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    CONSTRAINT ai_content_assessments_pkey PRIMARY KEY (assessment_id)
);

CREATE TABLE public.ai_content_scan_jobs (
    job_id uuid DEFAULT gen_random_uuid() NOT NULL,
    target_type character varying(20) NOT NULL,
    target_id uuid NOT NULL,
    content_hash character varying(64) NOT NULL,
    status character varying(20) DEFAULT 'QUEUED'::character varying NOT NULL,
    attempt_count integer DEFAULT 0 NOT NULL,
    next_attempt_at timestamp with time zone DEFAULT now() NOT NULL,
    locked_by character varying(100),
    locked_at timestamp with time zone,
    last_error_code character varying(80),
    force_rescan boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    completed_at timestamp with time zone,
    CONSTRAINT ai_content_scan_jobs_pkey PRIMARY KEY (job_id)
);

CREATE TABLE public.ai_moderation_policies (
    policy_id uuid DEFAULT gen_random_uuid() NOT NULL,
    policy_code character varying(60) NOT NULL,
    name character varying(150) NOT NULL,
    detection_guidance text NOT NULL,
    violation_category character varying(40) NOT NULL,
    report_category character varying(40) NOT NULL,
    severity character varying(20) NOT NULL,
    applicable_target_types character varying(100) NOT NULL,
    confidence_threshold numeric(4,3) DEFAULT 0.700 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    system_default boolean DEFAULT false NOT NULL,
    version integer DEFAULT 1 NOT NULL,
    created_by uuid,
    updated_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ai_moderation_policies_pkey PRIMARY KEY (policy_id)
);

CREATE TABLE public.archived_records (
    archive_id uuid DEFAULT gen_random_uuid() NOT NULL,
    legacy_table character varying(150) NOT NULL,
    legacy_id character varying(150) NOT NULL,
    owner_user_id uuid,
    payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    original_created_at timestamp with time zone,
    archived_at timestamp with time zone DEFAULT now() NOT NULL,
    retention_until timestamp with time zone,
    archive_reason character varying(255) DEFAULT 'RUNTIME_COMPATIBILITY'::character varying NOT NULL,
    source_schema_version character varying(80) DEFAULT 'PHASE2_WAVE9'::character varying,
    checksum character varying(128) DEFAULT md5((gen_random_uuid())::text) NOT NULL,
    CONSTRAINT archived_records_pkey PRIMARY KEY (archive_id)
);

CREATE TABLE public.attachments (
    attachment_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL,
    storage_key character varying(500) NOT NULL,
    original_name character varying(255) NOT NULL,
    mime_type character varying(100) NOT NULL,
    file_size_bytes bigint NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    checksum character varying(64),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    attachment_category character varying(40) DEFAULT 'GENERAL'::character varying NOT NULL,
    credential_type character varying(50),
    credential_number character varying(100),
    issuer character varying(200),
    issued_date date,
    expiry_date date,
    review_status character varying(30),
    review_note text,
    reviewed_by uuid,
    reviewed_at timestamp without time zone,
    file_url text,
    file_id uuid,
    health_record_id uuid,
    CONSTRAINT attachments_pkey PRIMARY KEY (attachment_id)
);

CREATE TABLE public.audit_events (
    audit_event_id uuid DEFAULT gen_random_uuid() NOT NULL,
    actor_user_id uuid,
    event_category character varying(80) NOT NULL,
    subject_user_id uuid,
    subject_reference_id uuid,
    resource_type character varying(100),
    resource_id uuid,
    purpose character varying(255),
    decision character varying(50),
    ip_hash character varying(128),
    before_payload_jsonb jsonb,
    after_payload_jsonb jsonb,
    checksum character varying(128),
    occurred_at timestamp with time zone DEFAULT now() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    note_text text,
    event_origin character varying(255) DEFAULT 'AUDIT_LOG'::character varying NOT NULL,
    ip_address character varying(80),
    user_agent character varying(500),
    payload jsonb,
    correlation_id uuid,
    severity character varying(20) DEFAULT 'MEDIUM'::character varying NOT NULL,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    reviewed_at timestamp with time zone,
    reviewed_by uuid,
    security_event_id uuid,
    CONSTRAINT audit_events_pkey PRIMARY KEY (audit_event_id)
);

CREATE TABLE public.auth_challenges (
    challenge_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    challenge_type character varying(40) NOT NULL,
    subject_identifier character varying(255),
    challenge_hash character varying(255) NOT NULL,
    attempts integer DEFAULT 0 NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    used_at timestamp with time zone,
    status character varying(30) NOT NULL,
    requested_role character varying(40),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    legacy_source character varying(40),
    legacy_id character varying(100),
    CONSTRAINT auth_challenges_pkey PRIMARY KEY (challenge_id)
);

CREATE TABLE public.auth_sessions (
    session_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token_family_id uuid NOT NULL,
    device_identifier character varying(255) NOT NULL,
    device_name character varying(150),
    refresh_token_hash character varying(255),
    issued_at timestamp with time zone NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    last_used_at timestamp with time zone,
    rotated_at timestamp with time zone,
    revoked_at timestamp with time zone,
    revoke_reason character varying(100),
    status character varying(20) NOT NULL,
    created_ip_hash character varying(255),
    user_agent_hash character varying(255),
    legacy_source character varying(40),
    legacy_id character varying(100),
    detected_reuse boolean DEFAULT false NOT NULL,
    revocation_metadata_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    CONSTRAINT auth_sessions_pkey PRIMARY KEY (session_id)
);

CREATE TABLE public.care_facilities (
    facility_id uuid DEFAULT gen_random_uuid() NOT NULL,
    partner_id uuid,
    name character varying(255) NOT NULL,
    facility_type character varying(50),
    address character varying(500),
    latitude numeric(10,8),
    longitude numeric(11,8),
    phone character varying(30),
    opening_hours_json jsonb,
    source_type character varying(30),
    verification_status character varying(30) DEFAULT 'UNVERIFIED'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    facility_level character varying(50),
    ownership_type character varying(30),
    province_id character varying(2),
    district_id character varying(4),
    external_source_id character varying(150),
    is_active boolean DEFAULT true NOT NULL,
    is_searchable boolean DEFAULT true NOT NULL,
    administrative_area_id uuid,
    CONSTRAINT care_facilities_pkey PRIMARY KEY (facility_id)
);

CREATE TABLE public.care_group_members (
    care_group_member_id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_group_id uuid NOT NULL,
    user_id uuid NOT NULL,
    member_role character varying(50),
    invitation_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    permission_json jsonb,
    joined_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    invite_token character varying(64),
    invite_channel character varying(20),
    invite_expires_at timestamp with time zone,
    invited_phone character varying(20),
    data_permission_id uuid,
    is_emergency_contact boolean DEFAULT false NOT NULL,
    emergency_contact_priority smallint,
    CONSTRAINT care_group_members_pkey PRIMARY KEY (care_group_member_id)
);

CREATE TABLE public.care_groups (
    care_group_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL,
    journey_id uuid,
    baby_id uuid,
    group_name character varying(200) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    description character varying(500),
    linked_journey_id uuid,
    linked_baby_profile_id uuid,
    care_subject_id uuid,
    CONSTRAINT care_groups_pkey PRIMARY KEY (care_group_id)
);

CREATE TABLE public.care_item_templates (
    template_id uuid DEFAULT gen_random_uuid() NOT NULL,
    parent_template_id uuid,
    entry_type character varying(30) NOT NULL,
    title character varying(500) NOT NULL,
    description text,
    display_order integer DEFAULT 0 NOT NULL,
    stage character varying(30),
    is_active boolean DEFAULT true NOT NULL,
    version integer DEFAULT 1 NOT NULL,
    effective_from timestamp with time zone,
    effective_to timestamp with time zone,
    configuration_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    configuration_hash character varying(128),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    difficulty_level character varying(30),
    duration_minutes smallint,
    instruction_content text,
    media_url text,
    safety_warning text,
    supports_posture_analysis boolean,
    template_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    configured_by uuid,
    analysis_mode character varying(30),
    rule_or_model_version character varying(80),
    confidence_threshold numeric(38,2),
    feedback_level character varying(30),
    content_status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    is_required boolean,
    author_user_id uuid,
    revision_reason text,
    revision_requested_at timestamp with time zone,
    revision_requested_by uuid,
    revision_requested_version integer,
    lock_version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT care_item_templates_pkey PRIMARY KEY (template_id)
);

CREATE TABLE public.care_subjects (
    care_subject_id uuid DEFAULT gen_random_uuid() NOT NULL,
    person_id uuid NOT NULL,
    owner_user_id uuid NOT NULL,
    mother_journey_id uuid,
    subject_type character varying(30) NOT NULL,
    nickname character varying(100),
    birth_date date,
    sex character varying(10),
    birth_weight_kg numeric(4,2),
    birth_length_cm numeric(4,1),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT care_subjects_pkey PRIMARY KEY (care_subject_id)
);

CREATE TABLE public.care_tasks (
    task_id uuid DEFAULT gen_random_uuid() NOT NULL,
    task_type character varying(40) NOT NULL,
    owner_user_id uuid,
    care_group_id uuid,
    creator_user_id uuid,
    assignee_user_id uuid,
    care_subject_id uuid,
    title character varying(255) NOT NULL,
    description text,
    scheduled_at timestamp with time zone,
    recurrence_rule character varying(255),
    snoozed_until timestamp with time zone,
    completed_at timestamp with time zone,
    cancelled_at timestamp with time zone,
    skipped_at timestamp with time zone,
    status character varying(30) DEFAULT 'PENDING'::character varying NOT NULL,
    source_reference_type character varying(60),
    source_reference_id uuid,
    vaccination_record_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    metadata_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    journey_id uuid,
    baby_id uuid,
    recurrence_type character varying(30),
    recurrence_end_date timestamp with time zone,
    fcm_job_id character varying(255),
    item_type character varying(60),
    CONSTRAINT care_tasks_pkey1 PRIMARY KEY (task_id)
);

CREATE TABLE public.community_content (
    content_id uuid DEFAULT gen_random_uuid() NOT NULL,
    topic_id uuid,
    parent_content_id uuid,
    author_user_id uuid NOT NULL,
    content_type character varying(20) NOT NULL,
    title character varying(255),
    body text NOT NULL,
    stage character varying(30),
    urgency character varying(20),
    is_anonymous boolean DEFAULT false NOT NULL,
    moderation_status character varying(30) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    pregnancy_week smallint,
    baby_age_months smallint,
    like_count integer DEFAULT 0 NOT NULL,
    answer_count integer DEFAULT 0 NOT NULL,
    is_expert_labeled boolean DEFAULT false NOT NULL,
    is_personal_experience boolean DEFAULT false NOT NULL,
    CONSTRAINT community_content_pkey PRIMARY KEY (content_id)
);

CREATE TABLE public.community_interactions (
    interaction_id uuid DEFAULT gen_random_uuid() NOT NULL,
    actor_user_id uuid NOT NULL,
    interaction_type character varying(255) NOT NULL,
    content_id uuid,
    topic_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    target_content_type character varying(255),
    CONSTRAINT community_interactions_pkey PRIMARY KEY (interaction_id)
);

CREATE TABLE public.community_topics (
    id uuid NOT NULL,
    created_at timestamp with time zone NOT NULL,
    description text,
    name character varying(100) NOT NULL,
    updated_at timestamp with time zone DEFAULT now(),
    is_hidden boolean DEFAULT false NOT NULL,
    icon character varying(255),
    sort_order integer DEFAULT 0 NOT NULL,
    created_by uuid,
    type character varying(20) DEFAULT 'TOPIC'::character varying NOT NULL,
    slug character varying(140) NOT NULL,
    parent_id uuid,
    CONSTRAINT community_topics_pkey PRIMARY KEY (id)
);

CREATE TABLE public.consultation_bookings (
    booking_id uuid DEFAULT gen_random_uuid() NOT NULL,
    requester_user_id uuid NOT NULL,
    expert_profile_id uuid NOT NULL,
    availability_id uuid,
    expert_price_id uuid,
    price_band_id uuid,
    shared_summary_id uuid,
    topic character varying(500),
    scheduled_start timestamp with time zone,
    scheduled_end timestamp with time zone,
    price_snapshot_amount numeric,
    commission_rate_snapshot numeric,
    cancellation_policy_snapshot text,
    price_locked_at timestamp with time zone,
    status character varying(30) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT consultation_bookings_pkey PRIMARY KEY (booking_id)
);

CREATE TABLE public.consultation_context_citations (
    citation_snapshot_id uuid DEFAULT gen_random_uuid() NOT NULL,
    context_share_id uuid NOT NULL,
    evidence_source_id uuid NOT NULL,
    organization character varying(255) NOT NULL,
    source_url character varying(1000) NOT NULL,
    source_status_at_share character varying(30) NOT NULL,
    reviewed_at timestamp with time zone NOT NULL,
    ordinal smallint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT consultation_context_citations_pkey PRIMARY KEY (citation_snapshot_id)
);

CREATE TABLE public.consultation_context_shares (
    context_share_id uuid DEFAULT gen_random_uuid() NOT NULL,
    consultation_request_id uuid NOT NULL,
    owner_user_id uuid NOT NULL,
    intake_session_id uuid NOT NULL,
    expert_profile_id uuid NOT NULL,
    consent_grant_id bigint NOT NULL,
    idempotency_key uuid NOT NULL,
    journey_id uuid NOT NULL,
    origin_dashboard character varying(30) NOT NULL,
    origin_reference_id uuid NOT NULL,
    triage_stage character varying(20) NOT NULL,
    risk_level character varying(10) NOT NULL,
    intake_status character varying(20) NOT NULL,
    risk_summary character varying(500) NOT NULL,
    share_policy_version character varying(60) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT consultation_context_shares_pkey PRIMARY KEY (context_share_id)
);

CREATE TABLE public.consultation_sessions (
    session_id uuid DEFAULT gen_random_uuid() NOT NULL,
    booking_id uuid,
    communication_room_id character varying(255),
    started_at timestamp with time zone,
    ended_at timestamp with time zone,
    session_status character varying(30),
    expert_summary text,
    technical_log_json jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT consultation_sessions_pkey PRIMARY KEY (session_id)
);

CREATE TABLE public.content_item_sources (
    content_item_source_id uuid DEFAULT gen_random_uuid() NOT NULL,
    content_item_id uuid NOT NULL,
    knowledge_source_id uuid,
    source_title character varying(500) NOT NULL,
    source_url character varying(2000),
    source_publisher character varying(255),
    source_snapshot_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT content_item_sources_pkey PRIMARY KEY (content_item_source_id)
);

CREATE TABLE public.content_item_topics (
    content_item_id uuid NOT NULL,
    topic_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT content_item_topics_pkey PRIMARY KEY (content_item_id, topic_id)
);

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
    stage character varying(30),
    revision_reason text,
    revision_requested_at timestamp with time zone,
    revision_requested_by uuid,
    revision_requested_version integer,
    lock_version bigint DEFAULT 0 NOT NULL,
    summary character varying(150),
    CONSTRAINT content_items_pkey PRIMARY KEY (content_item_id)
);

CREATE TABLE public.conversation_calls (
    call_id uuid DEFAULT gen_random_uuid() NOT NULL,
    conversation_id uuid NOT NULL,
    initiated_by_user_id uuid NOT NULL,
    call_type character varying(10) NOT NULL,
    call_status character varying(20) DEFAULT 'INITIATED'::character varying NOT NULL,
    zego_room_id character varying(255) NOT NULL,
    initiated_at timestamp with time zone NOT NULL,
    answered_at timestamp with time zone,
    ended_at timestamp with time zone,
    duration_seconds integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT conversation_calls_pkey PRIMARY KEY (call_id)
);

CREATE TABLE public.data_permissions (
    permission_id uuid DEFAULT gen_random_uuid() NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone,
    granted_at timestamp(6) with time zone,
    grantee_user_id uuid,
    owner_user_id uuid,
    purpose character varying(60),
    revoked_at timestamp(6) with time zone,
    scope_reference_id uuid,
    scope_type character varying(60),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    updated_at timestamp(6) with time zone,
    permission_series_id uuid,
    version_number integer,
    supersedes_permission_id uuid,
    revoked_by uuid,
    policy_version character varying(60),
    consent_evidence_key character varying(255),
    legacy_consent_id bigint GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    permission_kind character varying(255) DEFAULT 'DATA_PERMISSION'::character varying NOT NULL,
    recipient character varying(120),
    scope_text text,
    evidence_key uuid,
    locale character varying(20),
    CONSTRAINT data_permissions_pkey PRIMARY KEY (permission_id)
);

CREATE TABLE public.development_milestones (
    milestone_id uuid DEFAULT gen_random_uuid() NOT NULL,
    baby_id uuid NOT NULL,
    milestone_type character varying(80) NOT NULL,
    achieved_date date,
    note text,
    source_type character varying(30),
    recorded_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    milestone_status character varying(20) DEFAULT 'ACHIEVED'::character varying NOT NULL,
    record_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    care_subject_id uuid NOT NULL,
    CONSTRAINT development_milestones_pkey PRIMARY KEY (milestone_id)
);

CREATE TABLE public.device_connections (
    device_connection_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    provider_name character varying(80) NOT NULL,
    device_name character varying(150),
    scopes_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    token_reference text,
    consent_granted_at timestamp with time zone,
    last_synced_at timestamp with time zone,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT device_connections_pkey PRIMARY KEY (device_connection_id)
);

CREATE TABLE public.device_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token character varying(512) NOT NULL,
    platform character varying(30) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT device_tokens_pkey PRIMARY KEY (id)
);

CREATE TABLE public.direct_conversations (
    conversation_id uuid DEFAULT gen_random_uuid() NOT NULL,
    mother_user_id uuid NOT NULL,
    expert_user_id uuid NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    last_activity_at timestamp with time zone,
    mother_last_read_at timestamp with time zone,
    mother_last_read_message_id uuid,
    expert_last_read_at timestamp with time zone,
    expert_last_read_message_id uuid,
    CONSTRAINT direct_conversations_pkey PRIMARY KEY (conversation_id)
);

CREATE TABLE public.direct_messages (
    message_id uuid DEFAULT gen_random_uuid() NOT NULL,
    conversation_id uuid NOT NULL,
    sender_user_id uuid NOT NULL,
    client_message_id uuid NOT NULL,
    message_type character varying(30) DEFAULT 'TEXT'::character varying NOT NULL,
    message_body text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT direct_messages_pkey PRIMARY KEY (message_id)
);

CREATE TABLE public.expense_entries (
    expense_entry_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL,
    care_subject_id uuid,
    mother_journey_id uuid,
    category character varying(80),
    amount numeric(38,2) NOT NULL,
    currency character varying(10) DEFAULT 'VND'::character varying NOT NULL,
    expense_date date NOT NULL,
    note text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT expense_entries_pkey PRIMARY KEY (expense_entry_id)
);

CREATE TABLE public.expert_availability (
    availability_id uuid DEFAULT gen_random_uuid() NOT NULL,
    start_at timestamp with time zone NOT NULL,
    end_at timestamp with time zone NOT NULL,
    channel_type character varying(30) NOT NULL,
    status character varying(20) DEFAULT 'AVAILABLE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    professional_profile_id uuid NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT expert_availability_pkey PRIMARY KEY (availability_id)
);

CREATE TABLE public.expert_consultation_requests (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    requester_user_id uuid NOT NULL,
    expert_profile_id uuid NOT NULL,
    client_request_id uuid NOT NULL,
    topic character varying(200) NOT NULL,
    description character varying(2000) NOT NULL,
    preferred_window_start timestamp with time zone,
    preferred_window_end timestamp with time zone,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    reject_reason character varying(500),
    direct_conversation_id uuid,
    responded_at timestamp with time zone,
    responded_by uuid,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT expert_consultation_requests_pkey PRIMARY KEY (id)
);

CREATE TABLE public.expert_location_shares (
    location_share_id uuid DEFAULT gen_random_uuid() NOT NULL,
    latitude numeric(10,8) NOT NULL,
    longitude numeric(11,8) NOT NULL,
    accuracy_meters numeric(6,2),
    availability_status character varying(20),
    shared_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone,
    consent_reference uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    professional_profile_id uuid NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT expert_location_shares_pkey PRIMARY KEY (location_share_id)
);

CREATE TABLE public.growth_measurements (
    growth_measurement_id uuid DEFAULT gen_random_uuid() NOT NULL,
    baby_id uuid NOT NULL,
    measured_date date NOT NULL,
    weight_kg numeric(5,2),
    height_cm numeric(5,2),
    head_circumference_cm numeric(5,2),
    source_type character varying(30),
    note text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    care_subject_id uuid NOT NULL,
    CONSTRAINT growth_measurements_pkey PRIMARY KEY (growth_measurement_id)
);

CREATE TABLE public.health_context_memories (
    memory_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    care_subject_id uuid,
    triage_session_id uuid,
    related_stage character varying(20) NOT NULL,
    summary_text text NOT NULL,
    memory_payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone,
    deleted_at timestamp with time zone,
    mother_profile_id uuid,
    baby_profile_id uuid,
    CONSTRAINT health_context_memories_pkey PRIMARY KEY (memory_id)
);

CREATE TABLE public.health_observations (
    health_observation_id uuid DEFAULT gen_random_uuid() NOT NULL,
    device_connection_id uuid,
    care_subject_id uuid NOT NULL,
    observation_type character varying(50) NOT NULL,
    value_numeric numeric(10,2),
    value_secondary numeric(10,2),
    unit character varying(30),
    observed_at timestamp with time zone NOT NULL,
    source_record_id uuid,
    quality_label character varying(30),
    raw_payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    legacy_source character varying(60),
    legacy_id character varying(100),
    severity character varying(30),
    source_type character varying(60) DEFAULT 'SYSTEM'::character varying NOT NULL,
    subject_type character varying(30) NOT NULL,
    text_value text,
    CONSTRAINT health_observations_pkey PRIMARY KEY (health_observation_id)
);

CREATE TABLE public.health_records (
    health_record_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL,
    journey_id uuid,
    baby_id uuid,
    record_type character varying(50) NOT NULL,
    title character varying(255) NOT NULL,
    file_url text,
    record_date date,
    source_type character varying(30),
    source_name character varying(200),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    care_subject_id uuid,
    summary_period character varying(30),
    period_start date,
    summary_json jsonb,
    CONSTRAINT health_records_pkey PRIMARY KEY (health_record_id)
);

CREATE TABLE public.knowledge_source_reviews (
    review_id uuid DEFAULT gen_random_uuid() NOT NULL,
    knowledge_source_id uuid NOT NULL,
    previous_status character varying(30),
    new_status character varying(30) NOT NULL,
    actor_user_id uuid,
    actor_role character varying(80),
    notes text,
    changed_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT knowledge_source_reviews_pkey PRIMARY KEY (review_id)
);

CREATE TABLE public.knowledge_sources (
    knowledge_source_id uuid DEFAULT gen_random_uuid() NOT NULL,
    domain character varying(255) NOT NULL,
    base_url character varying(500) NOT NULL,
    organization character varying(255) NOT NULL,
    category character varying(40) NOT NULL,
    status character varying(30) NOT NULL,
    discovery_mode character varying(40) NOT NULL,
    applicable_stages text,
    added_by uuid,
    reviewed_by uuid,
    reviewed_at timestamp with time zone,
    notes text,
    source_version character varying(80),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT knowledge_sources_pkey PRIMARY KEY (knowledge_source_id)
);

CREATE TABLE public.maternal_exercise_sessions (
    exercise_session_id uuid DEFAULT gen_random_uuid() NOT NULL,
    mother_journey_id uuid,
    owner_user_id uuid NOT NULL,
    exercise_template_id uuid NOT NULL,
    posture_config_id uuid,
    started_at timestamp with time zone NOT NULL,
    ended_at timestamp with time zone,
    paused_seconds integer DEFAULT 0 NOT NULL,
    completion_percent numeric(38,2),
    posture_score numeric(38,2),
    session_status character varying(20) NOT NULL,
    warning_count integer DEFAULT 0 NOT NULL,
    summary_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    safety_observation_id uuid,
    CONSTRAINT maternal_exercise_sessions_pkey PRIMARY KEY (exercise_session_id)
);

CREATE TABLE public.moderation_cases (
    moderation_case_id uuid DEFAULT gen_random_uuid() NOT NULL,
    reporter_user_id uuid,
    assigned_moderator_id uuid,
    target_type character varying(30) NOT NULL,
    target_id uuid NOT NULL,
    reason_code character varying(80),
    description text,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    opened_at timestamp with time zone DEFAULT now() NOT NULL,
    resolved_at timestamp with time zone,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    report_source character varying(20) DEFAULT 'USER'::character varying NOT NULL,
    reverted_at timestamp with time zone,
    reverted_by uuid,
    priority character varying(20) DEFAULT 'NORMAL'::character varying NOT NULL,
    claimed_at timestamp with time zone,
    ai_feedback_decision character varying(20),
    ai_feedback_reason text,
    ai_feedback_by uuid,
    ai_feedback_at timestamp with time zone,
    ai_feedback_assessment_id uuid,
    CONSTRAINT moderation_cases_pkey PRIMARY KEY (moderation_case_id)
);

CREATE TABLE public.mother_journeys (
    journey_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL,
    journey_type character varying(20) NOT NULL,
    start_date date,
    last_menstrual_date date,
    estimated_due_date date,
    delivery_date date,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    date_source character varying(30),
    date_confidence character varying(20),
    pregnancy_outcome character varying(30),
    pregnancy_outcome_date date,
    care_subject_id uuid NOT NULL,
    baseline_revision bigint,
    baseline_schema_version character varying(40),
    baseline_source character varying(30),
    baseline_lifecycle_goal character varying(40),
    baseline_locale character varying(20),
    baseline_time_zone character varying(80),
    baseline_preferences character varying(300),
    baseline_submission_id uuid,
    baseline_recorded_at timestamp with time zone,
    CONSTRAINT mother_journeys_pkey PRIMARY KEY (journey_id)
);

CREATE TABLE public.notification_records (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    type character varying(50) NOT NULL,
    title character varying(255) NOT NULL,
    body text NOT NULL,
    reference_id uuid,
    reference_type character varying(50),
    status character varying(20) DEFAULT 'SENT'::character varying NOT NULL,
    fcm_message_id character varying(255),
    attempt_count integer DEFAULT 1 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    sent_at timestamp with time zone,
    failed_at timestamp with time zone,
    is_read boolean DEFAULT false NOT NULL,
    read_at timestamp with time zone,
    metadata jsonb,
    processing_started_at timestamp with time zone,
    channel character varying(30) DEFAULT 'PUSH'::character varying NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    claim_token uuid,
    CONSTRAINT notification_records_pkey PRIMARY KEY (id)
);

CREATE TABLE public.partner_organizations (
    partner_id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(200) NOT NULL,
    organization_type character varying(20) NOT NULL,
    address character varying(500) NOT NULL,
    city character varying(100) NOT NULL,
    phone character varying(20) NOT NULL,
    email character varying(255) NOT NULL,
    website character varying(500),
    logo_url character varying(1000),
    description character varying(2000),
    organization_status character varying(30) NOT NULL,
    representative_user_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT partner_organizations_pkey PRIMARY KEY (partner_id)
);

CREATE TABLE public.preparation_checklist_items (
    checklist_item_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL,
    mother_journey_id uuid,
    template_entry_id uuid,
    title character varying(500) NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    status character varying(30) DEFAULT 'OPEN'::character varying NOT NULL,
    due_at timestamp with time zone,
    completed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    baby_id uuid,
    category character varying(50) DEFAULT 'GENERAL'::character varying NOT NULL,
    CONSTRAINT preparation_checklist_items_pkey PRIMARY KEY (checklist_item_id)
);

CREATE TABLE public.professional_specialties (
    professional_profile_id uuid NOT NULL,
    specialty_id uuid NOT NULL,
    is_primary boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT professional_specialties_pkey PRIMARY KEY (professional_profile_id, specialty_id)
);

CREATE TABLE public.red_flag_rules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    keyword character varying(255) NOT NULL,
    severity character varying(20) NOT NULL,
    action character varying(20) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    is_system_default boolean DEFAULT false NOT NULL,
    created_by uuid,
    updated_by uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT red_flag_rules_pkey PRIMARY KEY (id)
);

CREATE TABLE public.safety_configs (
    safety_config_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    fall_detection_enabled boolean DEFAULT false NOT NULL,
    sensitivity_level character varying(10) DEFAULT 'MEDIUM'::character varying NOT NULL,
    emergency_auto_alert boolean DEFAULT true NOT NULL,
    countdown_seconds integer DEFAULT 30 NOT NULL,
    sensor_permission_granted boolean DEFAULT false NOT NULL,
    sensor_permission_recorded_at timestamp with time zone,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by uuid,
    CONSTRAINT safety_configs_pkey PRIMARY KEY (safety_config_id)
);

CREATE TABLE public.safety_events (
    safety_event_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    care_subject_id uuid,
    monitoring_session_id uuid,
    source_event_id uuid,
    detected_at timestamp with time zone DEFAULT now() NOT NULL,
    event_type character varying(50) NOT NULL,
    confidence_score numeric,
    peak_acceleration numeric,
    angular_velocity numeric,
    inactivity_seconds integer,
    response_type character varying(30),
    response_at timestamp with time zone,
    false_positive_reason text,
    status character varying(20) DEFAULT 'DETECTED'::character varying NOT NULL,
    location_snapshot_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    record_type character varying(255) DEFAULT 'IMU_EVENT'::character varying NOT NULL,
    magnitude numeric(10,4),
    user_latitude numeric(10,7),
    user_longitude numeric(10,7),
    client_detected_at timestamp with time zone,
    resolved_at timestamp with time zone,
    notes character varying(255),
    signal_key character varying(200),
    countdown_deadline_at timestamp with time zone,
    response_reason character varying(500),
    escalation_started_at timestamp with time zone,
    emergency_session_id uuid,
    created_by_text character varying(255),
    created_by_user_id uuid,
    alert_generation bigint NOT NULL,
    alert_status character varying(20),
    alert_claim_token uuid,
    alert_claimed_at timestamp(6) with time zone,
    alert_lease_expires_at timestamp(6) with time zone,
    alert_completed_at timestamp(6) with time zone,
    alert_successful_recipient_count integer NOT NULL,
    alert_failed_recipient_count integer NOT NULL,
    alert_updated_at timestamp(6) with time zone,
    action_type character varying(40),
    action_status character varying(20),
    actor_type character varying(20),
    attempt_number integer,
    accuracy_meters numeric(6,2),
    captured_at timestamp with time zone,
    care_facility_id uuid,
    consent_status character varying(20),
    context_id uuid,
    context_type character varying(50),
    delivered_at timestamp with time zone,
    delivery_status character varying(30),
    device_token_id uuid,
    expires_at timestamp with time zone,
    failure_code character varying(120),
    fcm_message_id character varying(255),
    idempotency_key character varying(255),
    latitude numeric(10,8),
    longitude numeric(11,8),
    location_included boolean,
    notification_record_id uuid,
    parent_event_id uuid,
    reason character varying(500),
    recipient_count integer,
    recipient_user_id uuid,
    responded_at timestamp with time zone,
    risk_level character varying(20),
    triage_handoff_id uuid,
    device_identifier character varying(255),
    attempt_status character varying(20),
    started_at timestamp with time zone,
    completed_at timestamp with time zone,
    lease_expires_at timestamp with time zone,
    successful_recipient_count integer,
    failed_recipient_count integer,
    summary text,
    action_phase character varying(30),
    fence_token uuid,
    related_action_id uuid,
    owner_user_id uuid,
    CONSTRAINT safety_events_pkey PRIMARY KEY (safety_event_id)
);

CREATE TABLE public.safety_monitoring_sessions (
    monitoring_session_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    status character varying(10) DEFAULT 'ACTIVE'::character varying NOT NULL,
    sensitivity_level character varying(10) DEFAULT 'MEDIUM'::character varying NOT NULL,
    started_at timestamp with time zone DEFAULT now() NOT NULL,
    ended_at timestamp with time zone,
    created_by uuid,
    CONSTRAINT safety_monitoring_sessions_pkey PRIMARY KEY (monitoring_session_id)
);

CREATE TABLE public.specialties (
    specialty_id uuid NOT NULL,
    code character varying(80) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT specialties_canonical_pkey PRIMARY KEY (specialty_id)
);

CREATE TABLE public.system_configurations (
    system_configuration_id uuid DEFAULT gen_random_uuid() NOT NULL,
    api_rate_limit integer NOT NULL,
    connection_timeout_ms integer NOT NULL,
    max_upload_size_mb integer NOT NULL,
    administrator_email character varying(254) NOT NULL,
    email_alerts boolean DEFAULT true NOT NULL,
    sms_alerts boolean DEFAULT true NOT NULL,
    webhook_alerts boolean DEFAULT false NOT NULL,
    ai_moderation_enabled boolean DEFAULT true NOT NULL,
    maintenance_mode_enabled boolean DEFAULT false NOT NULL,
    updated_by uuid NOT NULL,
    row_version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT system_configurations_pkey PRIMARY KEY (system_configuration_id)
);

CREATE TABLE public.triage_session_evidence (
    evidence_id uuid DEFAULT gen_random_uuid() NOT NULL,
    triage_session_id uuid NOT NULL,
    evidence_type character varying(40) NOT NULL,
    claim_code character varying(100),
    claim_text text NOT NULL,
    knowledge_source_id uuid,
    citation_url text,
    citation_domain character varying(255),
    source_version character varying(80),
    source_snapshot_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    content_hash character varying(128) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT triage_session_evidence_pkey PRIMARY KEY (evidence_id)
);

CREATE TABLE public.triage_sessions (
    triage_session_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    care_subject_id uuid,
    stage character varying(20),
    profile_context_id uuid,
    risk_level character varying(10),
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    emergency boolean DEFAULT false NOT NULL,
    disclaimer_version character varying(80),
    input_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    result_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    conversation_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    schema_version character varying(30) DEFAULT '1'::character varying NOT NULL,
    content_hash character varying(128),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    completed_at timestamp with time zone,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    baby_profile_id uuid,
    mother_profile_id uuid,
    client_request_id character varying(64),
    symptoms text NOT NULL,
    raw_ai_response text,
    disclaimer_text text,
    created_by uuid NOT NULL,
    symptom_list jsonb,
    duration_days integer,
    intensity character varying(20),
    emergency_flag boolean,
    extracted_at timestamp with time zone,
    structured_created_by character varying(255),
    journey_id uuid,
    origin_dashboard character varying(30),
    origin_reference_id uuid,
    continuation_token uuid,
    continuation_expires_at timestamp(6) with time zone,
    continuation_acknowledged_at timestamp(6) with time zone,
    CONSTRAINT triage_sessions_pkey PRIMARY KEY (triage_session_id)
);

CREATE TABLE public.users (
    user_id uuid NOT NULL,
    avatar_url character varying(500),
    created_at timestamp(6) with time zone NOT NULL,
    email character varying(255),
    full_name character varying(120),
    password_hash character varying(255),
    phone character varying(20),
    updated_at timestamp(6) with time zone NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    locked boolean DEFAULT false NOT NULL,
    lock_type character varying(30),
    lock_reason character varying(500),
    locked_by uuid,
    lock_episode_id uuid,
    role character varying(50),
    failed_login_count integer DEFAULT 0 NOT NULL,
    locked_at timestamp with time zone,
    email_verified boolean DEFAULT false NOT NULL,
    phone_verified boolean DEFAULT false NOT NULL,
    account_status character varying(30),
    last_login_at timestamp with time zone,
    suspended_until timestamp with time zone,
    must_change_password boolean DEFAULT false NOT NULL,
    community_posting_restricted_until timestamp with time zone,
    person_id uuid NOT NULL,
    settings_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    deactivation_reason text,
    deactivated_at timestamp with time zone,
    deactivated_by uuid,
    display_name character varying(200),
    date_of_birth date,
    phone_number character varying(40),
    area character varying(200),
    professional_title character varying(150),
    workplace character varying(200),
    experience_years smallint,
    consultation_scope text,
    verification_status character varying(30) DEFAULT 'PENDING'::character varying NOT NULL,
    verified_at timestamp with time zone,
    verified_by uuid,
    rating_avg numeric,
    specialty character varying(100),
    facility_id uuid,
    trust_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    consultation_fee_vnd bigint,
    bio character varying(500),
    interest_stage character varying(30),
    is_visible boolean,
    public_avatar_url character varying(500),
    region character varying(120),
    social_identities jsonb DEFAULT '[]'::jsonb NOT NULL,
    specialty_ids uuid[],
    CONSTRAINT users_pkey PRIMARY KEY (user_id),
    CONSTRAINT users_lock_type_ck CHECK (lock_type IS NULL OR lock_type IN ('TEMPORARY', 'ADMIN')),
    CONSTRAINT users_lock_state_ck CHECK (
        (locked = false AND lock_type IS NULL AND lock_reason IS NULL
            AND locked_by IS NULL AND lock_episode_id IS NULL)
        OR
        (locked = true AND lock_type = 'TEMPORARY' AND lock_reason IS NULL
            AND locked_by IS NULL AND lock_episode_id IS NULL AND locked_at IS NOT NULL)
        OR
        (locked = true AND lock_type = 'ADMIN' AND lock_reason IS NOT NULL
            AND locked_by IS NOT NULL AND lock_episode_id IS NOT NULL AND locked_at IS NOT NULL)
    ),
    CONSTRAINT users_locked_by_fk FOREIGN KEY (locked_by) REFERENCES public.users(user_id)
);

CREATE TABLE public.account_lock_appeals (
    appeal_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    lock_episode_id uuid NOT NULL,
    reason character varying(1000) NOT NULL,
    status character varying(30) DEFAULT 'PENDING' NOT NULL,
    submitted_at timestamp with time zone DEFAULT now() NOT NULL,
    reviewed_by uuid,
    reviewed_at timestamp with time zone,
    review_note character varying(1000),
    CONSTRAINT account_lock_appeals_pkey PRIMARY KEY (appeal_id),
    CONSTRAINT account_lock_appeals_user_fk FOREIGN KEY (user_id)
        REFERENCES public.users(user_id) ON DELETE CASCADE,
    CONSTRAINT account_lock_appeals_reviewer_fk FOREIGN KEY (reviewed_by)
        REFERENCES public.users(user_id) ON DELETE SET NULL,
    CONSTRAINT account_lock_appeals_status_ck
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT account_lock_appeals_reason_ck CHECK (length(btrim(reason)) > 0),
    CONSTRAINT account_lock_appeals_review_ck CHECK (
        (status = 'PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL)
        OR
        (status <> 'PENDING' AND reviewed_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX account_lock_appeals_pending_episode_uq
    ON public.account_lock_appeals (user_id, lock_episode_id)
    WHERE status = 'PENDING';

CREATE INDEX account_lock_appeals_queue_ix
    ON public.account_lock_appeals (status, submitted_at DESC);

CREATE INDEX account_lock_appeals_user_ix
    ON public.account_lock_appeals (user_id, submitted_at DESC);

CREATE TABLE public.vaccination_records (
    vaccination_record_id uuid DEFAULT gen_random_uuid() NOT NULL,
    baby_id uuid NOT NULL,
    vaccine_name character varying(200) NOT NULL,
    dose_number smallint,
    scheduled_date date,
    administered_date date,
    status character varying(20) DEFAULT 'SCHEDULED'::character varying NOT NULL,
    facility_name character varying(200),
    proof_record_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    postpone_reason text,
    care_subject_id uuid NOT NULL,
    vaccination_schedule_id uuid,
    CONSTRAINT vaccination_records_pkey PRIMARY KEY (vaccination_record_id)
);

CREATE TABLE public.vaccination_schedules (
    vaccination_schedule_id uuid DEFAULT gen_random_uuid() NOT NULL,
    vaccine_name character varying(200) NOT NULL,
    dose_number smallint NOT NULL,
    offset_days integer NOT NULL,
    description text,
    schedule_version character varying(30) DEFAULT '1'::character varying NOT NULL,
    active_from date,
    active_to date,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT vaccination_schedules_pkey PRIMARY KEY (vaccination_schedule_id)
);

-- ============================================================================

-- ============================================================================
-- 2. Unique and check constraints
-- ============================================================================
DO $canonical_constraints$
DECLARE v_existing_def text;
BEGIN
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'administrative_areas_code_key' AND conrelid = 'public.administrative_areas'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.administrative_areas ADD CONSTRAINT administrative_areas_code_key UNIQUE (code)';
    ELSIF v_existing_def <> 'UNIQUE (code)' THEN
        EXECUTE 'ALTER TABLE public.administrative_areas DROP CONSTRAINT administrative_areas_code_key';
        EXECUTE 'ALTER TABLE public.administrative_areas ADD CONSTRAINT administrative_areas_code_key UNIQUE (code)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_ai_assessment_classification' AND conrelid = 'public.ai_content_assessments'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.ai_content_assessments ADD CONSTRAINT chk_ai_assessment_classification CHECK (((classification IS NULL) OR ((classification)::text = ANY (ARRAY[(''SAFE''::character varying)::text, (''VIOLATION''::character varying)::text, (''UNCERTAIN''::character varying)::text]))))';
    ELSIF v_existing_def <> 'CHECK (((classification IS NULL) OR ((classification)::text = ANY (ARRAY[(''SAFE''::character varying)::text, (''VIOLATION''::character varying)::text, (''UNCERTAIN''::character varying)::text]))))' THEN
        EXECUTE 'ALTER TABLE public.ai_content_assessments DROP CONSTRAINT chk_ai_assessment_classification';
        EXECUTE 'ALTER TABLE public.ai_content_assessments ADD CONSTRAINT chk_ai_assessment_classification CHECK (((classification IS NULL) OR ((classification)::text = ANY (ARRAY[(''SAFE''::character varying)::text, (''VIOLATION''::character varying)::text, (''UNCERTAIN''::character varying)::text]))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_ai_assessment_confidence' AND conrelid = 'public.ai_content_assessments'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.ai_content_assessments ADD CONSTRAINT chk_ai_assessment_confidence CHECK (((confidence IS NULL) OR ((confidence >= (0)::numeric) AND (confidence <= (1)::numeric))))';
    ELSIF v_existing_def <> 'CHECK (((confidence IS NULL) OR ((confidence >= (0)::numeric) AND (confidence <= (1)::numeric))))' THEN
        EXECUTE 'ALTER TABLE public.ai_content_assessments DROP CONSTRAINT chk_ai_assessment_confidence';
        EXECUTE 'ALTER TABLE public.ai_content_assessments ADD CONSTRAINT chk_ai_assessment_confidence CHECK (((confidence IS NULL) OR ((confidence >= (0)::numeric) AND (confidence <= (1)::numeric))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_ai_assessment_matches_array' AND conrelid = 'public.ai_content_assessments'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.ai_content_assessments ADD CONSTRAINT chk_ai_assessment_matches_array CHECK ((jsonb_typeof(matches_jsonb) = ''array''::text))';
    ELSIF v_existing_def <> 'CHECK ((jsonb_typeof(matches_jsonb) = ''array''::text))' THEN
        EXECUTE 'ALTER TABLE public.ai_content_assessments DROP CONSTRAINT chk_ai_assessment_matches_array';
        EXECUTE 'ALTER TABLE public.ai_content_assessments ADD CONSTRAINT chk_ai_assessment_matches_array CHECK ((jsonb_typeof(matches_jsonb) = ''array''::text))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_ai_assessment_status' AND conrelid = 'public.ai_content_assessments'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.ai_content_assessments ADD CONSTRAINT chk_ai_assessment_status CHECK (((status)::text = ANY (ARRAY[(''COMPLETED''::character varying)::text, (''FAILED''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((status)::text = ANY (ARRAY[(''COMPLETED''::character varying)::text, (''FAILED''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.ai_content_assessments DROP CONSTRAINT chk_ai_assessment_status';
        EXECUTE 'ALTER TABLE public.ai_content_assessments ADD CONSTRAINT chk_ai_assessment_status CHECK (((status)::text = ANY (ARRAY[(''COMPLETED''::character varying)::text, (''FAILED''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_ai_scan_job_status' AND conrelid = 'public.ai_content_scan_jobs'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.ai_content_scan_jobs ADD CONSTRAINT chk_ai_scan_job_status CHECK (((status)::text = ANY (ARRAY[(''QUEUED''::character varying)::text, (''PROCESSING''::character varying)::text, (''COMPLETED''::character varying)::text, (''FAILED''::character varying)::text, (''SKIPPED''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((status)::text = ANY (ARRAY[(''QUEUED''::character varying)::text, (''PROCESSING''::character varying)::text, (''COMPLETED''::character varying)::text, (''FAILED''::character varying)::text, (''SKIPPED''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.ai_content_scan_jobs DROP CONSTRAINT chk_ai_scan_job_status';
        EXECUTE 'ALTER TABLE public.ai_content_scan_jobs ADD CONSTRAINT chk_ai_scan_job_status CHECK (((status)::text = ANY (ARRAY[(''QUEUED''::character varying)::text, (''PROCESSING''::character varying)::text, (''COMPLETED''::character varying)::text, (''FAILED''::character varying)::text, (''SKIPPED''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_ai_scan_job_target_type' AND conrelid = 'public.ai_content_scan_jobs'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.ai_content_scan_jobs ADD CONSTRAINT chk_ai_scan_job_target_type CHECK (((target_type)::text = ANY (ARRAY[(''QUESTION''::character varying)::text, (''ANSWER''::character varying)::text, (''CONTENT''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((target_type)::text = ANY (ARRAY[(''QUESTION''::character varying)::text, (''ANSWER''::character varying)::text, (''CONTENT''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.ai_content_scan_jobs DROP CONSTRAINT chk_ai_scan_job_target_type';
        EXECUTE 'ALTER TABLE public.ai_content_scan_jobs ADD CONSTRAINT chk_ai_scan_job_target_type CHECK (((target_type)::text = ANY (ARRAY[(''QUESTION''::character varying)::text, (''ANSWER''::character varying)::text, (''CONTENT''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_ai_policy_confidence' AND conrelid = 'public.ai_moderation_policies'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.ai_moderation_policies ADD CONSTRAINT chk_ai_policy_confidence CHECK (((confidence_threshold >= (0)::numeric) AND (confidence_threshold <= (1)::numeric)))';
    ELSIF v_existing_def <> 'CHECK (((confidence_threshold >= (0)::numeric) AND (confidence_threshold <= (1)::numeric)))' THEN
        EXECUTE 'ALTER TABLE public.ai_moderation_policies DROP CONSTRAINT chk_ai_policy_confidence';
        EXECUTE 'ALTER TABLE public.ai_moderation_policies ADD CONSTRAINT chk_ai_policy_confidence CHECK (((confidence_threshold >= (0)::numeric) AND (confidence_threshold <= (1)::numeric)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_ai_policy_severity' AND conrelid = 'public.ai_moderation_policies'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.ai_moderation_policies ADD CONSTRAINT chk_ai_policy_severity CHECK (((severity)::text = ANY (ARRAY[(''LOW''::character varying)::text, (''MEDIUM''::character varying)::text, (''HIGH''::character varying)::text, (''CRITICAL''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((severity)::text = ANY (ARRAY[(''LOW''::character varying)::text, (''MEDIUM''::character varying)::text, (''HIGH''::character varying)::text, (''CRITICAL''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.ai_moderation_policies DROP CONSTRAINT chk_ai_policy_severity';
        EXECUTE 'ALTER TABLE public.ai_moderation_policies ADD CONSTRAINT chk_ai_policy_severity CHECK (((severity)::text = ANY (ARRAY[(''LOW''::character varying)::text, (''MEDIUM''::character varying)::text, (''HIGH''::character varying)::text, (''CRITICAL''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'uq_ai_moderation_policies_code' AND conrelid = 'public.ai_moderation_policies'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.ai_moderation_policies ADD CONSTRAINT uq_ai_moderation_policies_code UNIQUE (policy_code)';
    ELSIF v_existing_def <> 'UNIQUE (policy_code)' THEN
        EXECUTE 'ALTER TABLE public.ai_moderation_policies DROP CONSTRAINT uq_ai_moderation_policies_code';
        EXECUTE 'ALTER TABLE public.ai_moderation_policies ADD CONSTRAINT uq_ai_moderation_policies_code UNIQUE (policy_code)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'archived_records_source_uk' AND conrelid = 'public.archived_records'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.archived_records ADD CONSTRAINT archived_records_source_uk UNIQUE (legacy_table, legacy_id)';
    ELSIF v_existing_def <> 'UNIQUE (legacy_table, legacy_id)' THEN
        EXECUTE 'ALTER TABLE public.archived_records DROP CONSTRAINT archived_records_source_uk';
        EXECUTE 'ALTER TABLE public.archived_records ADD CONSTRAINT archived_records_source_uk UNIQUE (legacy_table, legacy_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'attachments_file_size_bytes_check' AND conrelid = 'public.attachments'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.attachments ADD CONSTRAINT attachments_file_size_bytes_check CHECK ((file_size_bytes >= 0))';
    ELSIF v_existing_def <> 'CHECK ((file_size_bytes >= 0))' THEN
        EXECUTE 'ALTER TABLE public.attachments DROP CONSTRAINT attachments_file_size_bytes_check';
        EXECUTE 'ALTER TABLE public.attachments ADD CONSTRAINT attachments_file_size_bytes_check CHECK ((file_size_bytes >= 0))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'attachments_storage_key_key' AND conrelid = 'public.attachments'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.attachments ADD CONSTRAINT attachments_storage_key_key UNIQUE (storage_key)';
    ELSIF v_existing_def <> 'UNIQUE (storage_key)' THEN
        EXECUTE 'ALTER TABLE public.attachments DROP CONSTRAINT attachments_storage_key_key';
        EXECUTE 'ALTER TABLE public.attachments ADD CONSTRAINT attachments_storage_key_key UNIQUE (storage_key)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'audit_events_security_note_text_ck' AND conrelid = 'public.audit_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.audit_events ADD CONSTRAINT audit_events_security_note_text_ck CHECK ((((event_category)::text <> ''SECURITY_INVESTIGATION_NOTE''::text) OR ((security_event_id IS NOT NULL) AND (length(TRIM(BOTH FROM note_text)) > 0))))';
    ELSIF v_existing_def <> 'CHECK ((((event_category)::text <> ''SECURITY_INVESTIGATION_NOTE''::text) OR ((security_event_id IS NOT NULL) AND (length(TRIM(BOTH FROM note_text)) > 0))))' THEN
        EXECUTE 'ALTER TABLE public.audit_events DROP CONSTRAINT audit_events_security_note_text_ck';
        EXECUTE 'ALTER TABLE public.audit_events ADD CONSTRAINT audit_events_security_note_text_ck CHECK ((((event_category)::text <> ''SECURITY_INVESTIGATION_NOTE''::text) OR ((security_event_id IS NOT NULL) AND (length(TRIM(BOTH FROM note_text)) > 0))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'auth_challenges_legacy_uk' AND conrelid = 'public.auth_challenges'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.auth_challenges ADD CONSTRAINT auth_challenges_legacy_uk UNIQUE (legacy_source, legacy_id)';
    ELSIF v_existing_def <> 'UNIQUE (legacy_source, legacy_id)' THEN
        EXECUTE 'ALTER TABLE public.auth_challenges DROP CONSTRAINT auth_challenges_legacy_uk';
        EXECUTE 'ALTER TABLE public.auth_challenges ADD CONSTRAINT auth_challenges_legacy_uk UNIQUE (legacy_source, legacy_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'auth_challenges_status_ck' AND conrelid = 'public.auth_challenges'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.auth_challenges ADD CONSTRAINT auth_challenges_status_ck CHECK (((status)::text = ANY (ARRAY[(''PENDING''::character varying)::text, (''VERIFIED''::character varying)::text, (''USED''::character varying)::text, (''EXPIRED''::character varying)::text, (''REVOKED''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((status)::text = ANY (ARRAY[(''PENDING''::character varying)::text, (''VERIFIED''::character varying)::text, (''USED''::character varying)::text, (''EXPIRED''::character varying)::text, (''REVOKED''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.auth_challenges DROP CONSTRAINT auth_challenges_status_ck';
        EXECUTE 'ALTER TABLE public.auth_challenges ADD CONSTRAINT auth_challenges_status_ck CHECK (((status)::text = ANY (ARRAY[(''PENDING''::character varying)::text, (''VERIFIED''::character varying)::text, (''USED''::character varying)::text, (''EXPIRED''::character varying)::text, (''REVOKED''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'auth_sessions_legacy_uk' AND conrelid = 'public.auth_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.auth_sessions ADD CONSTRAINT auth_sessions_legacy_uk UNIQUE (legacy_source, legacy_id)';
    ELSIF v_existing_def <> 'UNIQUE (legacy_source, legacy_id)' THEN
        EXECUTE 'ALTER TABLE public.auth_sessions DROP CONSTRAINT auth_sessions_legacy_uk';
        EXECUTE 'ALTER TABLE public.auth_sessions ADD CONSTRAINT auth_sessions_legacy_uk UNIQUE (legacy_source, legacy_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'auth_sessions_status_ck' AND conrelid = 'public.auth_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.auth_sessions ADD CONSTRAINT auth_sessions_status_ck CHECK (((status)::text = ANY (ARRAY[(''ACTIVE''::character varying)::text, (''ROTATED''::character varying)::text, (''REVOKED''::character varying)::text, (''EXPIRED''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((status)::text = ANY (ARRAY[(''ACTIVE''::character varying)::text, (''ROTATED''::character varying)::text, (''REVOKED''::character varying)::text, (''EXPIRED''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.auth_sessions DROP CONSTRAINT auth_sessions_status_ck';
        EXECUTE 'ALTER TABLE public.auth_sessions ADD CONSTRAINT auth_sessions_status_ck CHECK (((status)::text = ANY (ARRAY[(''ACTIVE''::character varying)::text, (''ROTATED''::character varying)::text, (''REVOKED''::character varying)::text, (''EXPIRED''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_facilities_ownership_type_check' AND conrelid = 'public.care_facilities'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_facilities ADD CONSTRAINT care_facilities_ownership_type_check CHECK (((ownership_type IS NULL) OR ((ownership_type)::text = ANY (ARRAY[(''PUBLIC''::character varying)::text, (''MILITARY''::character varying)::text]))))';
    ELSIF v_existing_def <> 'CHECK (((ownership_type IS NULL) OR ((ownership_type)::text = ANY (ARRAY[(''PUBLIC''::character varying)::text, (''MILITARY''::character varying)::text]))))' THEN
        EXECUTE 'ALTER TABLE public.care_facilities DROP CONSTRAINT care_facilities_ownership_type_check';
        EXECUTE 'ALTER TABLE public.care_facilities ADD CONSTRAINT care_facilities_ownership_type_check CHECK (((ownership_type IS NULL) OR ((ownership_type)::text = ANY (ARRAY[(''PUBLIC''::character varying)::text, (''MILITARY''::character varying)::text]))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_facilities_searchable_coordinates_check' AND conrelid = 'public.care_facilities'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_facilities ADD CONSTRAINT care_facilities_searchable_coordinates_check CHECK (((is_searchable = false) OR ((latitude IS NOT NULL) AND (longitude IS NOT NULL))))';
    ELSIF v_existing_def <> 'CHECK (((is_searchable = false) OR ((latitude IS NOT NULL) AND (longitude IS NOT NULL))))' THEN
        EXECUTE 'ALTER TABLE public.care_facilities DROP CONSTRAINT care_facilities_searchable_coordinates_check';
        EXECUTE 'ALTER TABLE public.care_facilities ADD CONSTRAINT care_facilities_searchable_coordinates_check CHECK (((is_searchable = false) OR ((latitude IS NOT NULL) AND (longitude IS NOT NULL))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_facilities_source_type_check' AND conrelid = 'public.care_facilities'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_facilities ADD CONSTRAINT care_facilities_source_type_check CHECK (((source_type IS NULL) OR ((source_type)::text = ANY (ARRAY[(''MANUAL''::character varying)::text, (''TRACKASIA''::character varying)::text, (''LEGACY_IMPORT''::character varying)::text]))))';
    ELSIF v_existing_def <> 'CHECK (((source_type IS NULL) OR ((source_type)::text = ANY (ARRAY[(''MANUAL''::character varying)::text, (''TRACKASIA''::character varying)::text, (''LEGACY_IMPORT''::character varying)::text]))))' THEN
        EXECUTE 'ALTER TABLE public.care_facilities DROP CONSTRAINT care_facilities_source_type_check';
        EXECUTE 'ALTER TABLE public.care_facilities ADD CONSTRAINT care_facilities_source_type_check CHECK (((source_type IS NULL) OR ((source_type)::text = ANY (ARRAY[(''MANUAL''::character varying)::text, (''TRACKASIA''::character varying)::text, (''LEGACY_IMPORT''::character varying)::text]))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_item_templates_type_ck' AND conrelid = 'public.care_item_templates'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_item_templates ADD CONSTRAINT care_item_templates_type_ck CHECK (((entry_type)::text = ANY (ARRAY[(''TEMPLATE_ROOT''::character varying)::text, (''CHECKLIST_ENTRY''::character varying)::text, (''EXERCISE_TEMPLATE''::character varying)::text, (''POSTURE_CONFIG''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((entry_type)::text = ANY (ARRAY[(''TEMPLATE_ROOT''::character varying)::text, (''CHECKLIST_ENTRY''::character varying)::text, (''EXERCISE_TEMPLATE''::character varying)::text, (''POSTURE_CONFIG''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.care_item_templates DROP CONSTRAINT care_item_templates_type_ck';
        EXECUTE 'ALTER TABLE public.care_item_templates ADD CONSTRAINT care_item_templates_type_ck CHECK (((entry_type)::text = ANY (ARRAY[(''TEMPLATE_ROOT''::character varying)::text, (''CHECKLIST_ENTRY''::character varying)::text, (''EXERCISE_TEMPLATE''::character varying)::text, (''POSTURE_CONFIG''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_care_item_templates_posture_confidence_threshold' AND conrelid = 'public.care_item_templates'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_item_templates ADD CONSTRAINT chk_care_item_templates_posture_confidence_threshold CHECK ((((entry_type)::text <> ''POSTURE_CONFIG''::text) OR (confidence_threshold IS NULL) OR ((confidence_threshold >= 0.0) AND (confidence_threshold <= 1.0))))';
    ELSIF v_existing_def <> 'CHECK ((((entry_type)::text <> ''POSTURE_CONFIG''::text) OR (confidence_threshold IS NULL) OR ((confidence_threshold >= 0.0) AND (confidence_threshold <= 1.0))))' THEN
        EXECUTE 'ALTER TABLE public.care_item_templates DROP CONSTRAINT chk_care_item_templates_posture_confidence_threshold';
        EXECUTE 'ALTER TABLE public.care_item_templates ADD CONSTRAINT chk_care_item_templates_posture_confidence_threshold CHECK ((((entry_type)::text <> ''POSTURE_CONFIG''::text) OR (confidence_threshold IS NULL) OR ((confidence_threshold >= 0.0) AND (confidence_threshold <= 1.0))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_subjects_type_ck' AND conrelid = 'public.care_subjects'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_subjects ADD CONSTRAINT care_subjects_type_ck CHECK (((subject_type)::text = ANY (ARRAY[(''MOTHER''::character varying)::text, (''BABY''::character varying)::text, (''DEPENDENT''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((subject_type)::text = ANY (ARRAY[(''MOTHER''::character varying)::text, (''BABY''::character varying)::text, (''DEPENDENT''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.care_subjects DROP CONSTRAINT care_subjects_type_ck';
        EXECUTE 'ALTER TABLE public.care_subjects ADD CONSTRAINT care_subjects_type_ck CHECK (((subject_type)::text = ANY (ARRAY[(''MOTHER''::character varying)::text, (''BABY''::character varying)::text, (''DEPENDENT''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_tasks_type_ck' AND conrelid = 'public.care_tasks'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_type_ck CHECK (((task_type)::text = ANY ((ARRAY[''SCHEDULED_REMINDER''::character varying, ''MANUAL_TASK''::character varying, ''CARE_LOG''::character varying])::text[])))';
    ELSIF v_existing_def <> 'CHECK (((task_type)::text = ANY ((ARRAY[''SCHEDULED_REMINDER''::character varying, ''MANUAL_TASK''::character varying, ''CARE_LOG''::character varying])::text[])))' THEN
        EXECUTE 'ALTER TABLE public.care_tasks DROP CONSTRAINT care_tasks_type_ck';
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_type_ck CHECK (((task_type)::text = ANY ((ARRAY[''SCHEDULED_REMINDER''::character varying, ''MANUAL_TASK''::character varying, ''CARE_LOG''::character varying])::text[])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_tasks_vaccination_ck' AND conrelid = 'public.care_tasks'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_vaccination_ck CHECK ((((task_type)::text <> ''SCHEDULED_REMINDER''::text) OR ((source_reference_type)::text <> ''VACCINATION''::text) OR (vaccination_record_id IS NOT NULL) OR (care_subject_id IS NOT NULL)))';
    ELSIF v_existing_def <> 'CHECK ((((task_type)::text <> ''SCHEDULED_REMINDER''::text) OR ((source_reference_type)::text <> ''VACCINATION''::text) OR (vaccination_record_id IS NOT NULL) OR (care_subject_id IS NOT NULL)))' THEN
        EXECUTE 'ALTER TABLE public.care_tasks DROP CONSTRAINT care_tasks_vaccination_ck';
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_vaccination_ck CHECK ((((task_type)::text <> ''SCHEDULED_REMINDER''::text) OR ((source_reference_type)::text <> ''VACCINATION''::text) OR (vaccination_record_id IS NOT NULL) OR (care_subject_id IS NOT NULL)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'community_content_type_ck' AND conrelid = 'public.community_content'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_content ADD CONSTRAINT community_content_type_ck CHECK (((content_type)::text = ANY (ARRAY[(''QUESTION''::character varying)::text, (''ANSWER''::character varying)::text, (''POST''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((content_type)::text = ANY (ARRAY[(''QUESTION''::character varying)::text, (''ANSWER''::character varying)::text, (''POST''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.community_content DROP CONSTRAINT community_content_type_ck';
        EXECUTE 'ALTER TABLE public.community_content ADD CONSTRAINT community_content_type_ck CHECK (((content_type)::text = ANY (ARRAY[(''QUESTION''::character varying)::text, (''ANSWER''::character varying)::text, (''POST''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'community_interactions_one_target_ck' AND conrelid = 'public.community_interactions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT community_interactions_one_target_ck CHECK (((content_id IS NOT NULL) <> (topic_id IS NOT NULL)))';
    ELSIF v_existing_def <> 'CHECK (((content_id IS NOT NULL) <> (topic_id IS NOT NULL)))' THEN
        EXECUTE 'ALTER TABLE public.community_interactions DROP CONSTRAINT community_interactions_one_target_ck';
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT community_interactions_one_target_ck CHECK (((content_id IS NOT NULL) <> (topic_id IS NOT NULL)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'community_interactions_type_ck' AND conrelid = 'public.community_interactions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT community_interactions_type_ck CHECK (((interaction_type)::text = ANY (ARRAY[(''REACTION''::character varying)::text, (''BOOKMARK''::character varying)::text, (''FOLLOW''::character varying)::text, (''MUTE''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((interaction_type)::text = ANY (ARRAY[(''REACTION''::character varying)::text, (''BOOKMARK''::character varying)::text, (''FOLLOW''::character varying)::text, (''MUTE''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.community_interactions DROP CONSTRAINT community_interactions_type_ck';
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT community_interactions_type_ck CHECK (((interaction_type)::text = ANY (ARRAY[(''REACTION''::character varying)::text, (''BOOKMARK''::character varying)::text, (''FOLLOW''::character varying)::text, (''MUTE''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'community_topics_parent_rule_check_v2' AND conrelid = 'public.community_topics'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_topics ADD CONSTRAINT community_topics_parent_rule_check_v2 CHECK (((((type)::text = ''CATEGORY''::text) AND (parent_id IS NULL)) OR (((type)::text = ''TOPIC''::text) AND (parent_id IS NOT NULL)) OR (((type)::text = ''TAG''::text) AND (parent_id IS NULL))))';
    ELSIF v_existing_def <> 'CHECK (((((type)::text = ''CATEGORY''::text) AND (parent_id IS NULL)) OR (((type)::text = ''TOPIC''::text) AND (parent_id IS NOT NULL)) OR (((type)::text = ''TAG''::text) AND (parent_id IS NULL))))' THEN
        EXECUTE 'ALTER TABLE public.community_topics DROP CONSTRAINT community_topics_parent_rule_check_v2';
        EXECUTE 'ALTER TABLE public.community_topics ADD CONSTRAINT community_topics_parent_rule_check_v2 CHECK (((((type)::text = ''CATEGORY''::text) AND (parent_id IS NULL)) OR (((type)::text = ''TOPIC''::text) AND (parent_id IS NOT NULL)) OR (((type)::text = ''TAG''::text) AND (parent_id IS NULL))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'community_topics_slug_unique' AND conrelid = 'public.community_topics'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_topics ADD CONSTRAINT community_topics_slug_unique UNIQUE (slug)';
    ELSIF v_existing_def <> 'UNIQUE (slug)' THEN
        EXECUTE 'ALTER TABLE public.community_topics DROP CONSTRAINT community_topics_slug_unique';
        EXECUTE 'ALTER TABLE public.community_topics ADD CONSTRAINT community_topics_slug_unique UNIQUE (slug)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'community_topics_type_check' AND conrelid = 'public.community_topics'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_topics ADD CONSTRAINT community_topics_type_check CHECK (((type)::text = ANY (ARRAY[(''TOPIC''::character varying)::text, (''CATEGORY''::character varying)::text, (''TAG''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((type)::text = ANY (ARRAY[(''TOPIC''::character varying)::text, (''CATEGORY''::character varying)::text, (''TAG''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.community_topics DROP CONSTRAINT community_topics_type_check';
        EXECUTE 'ALTER TABLE public.community_topics ADD CONSTRAINT community_topics_type_check CHECK (((type)::text = ANY (ARRAY[(''TOPIC''::character varying)::text, (''CATEGORY''::character varying)::text, (''TAG''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_context_citation_approved' AND conrelid = 'public.consultation_context_citations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_citations ADD CONSTRAINT chk_context_citation_approved CHECK (((source_status_at_share)::text = ''APPROVED''::text))';
    ELSIF v_existing_def <> 'CHECK (((source_status_at_share)::text = ''APPROVED''::text))' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_citations DROP CONSTRAINT chk_context_citation_approved';
        EXECUTE 'ALTER TABLE public.consultation_context_citations ADD CONSTRAINT chk_context_citation_approved CHECK (((source_status_at_share)::text = ''APPROVED''::text))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_context_citation_https' AND conrelid = 'public.consultation_context_citations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_citations ADD CONSTRAINT chk_context_citation_https CHECK (((source_url)::text ~~ ''https://%''::text))';
    ELSIF v_existing_def <> 'CHECK (((source_url)::text ~~ ''https://%''::text))' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_citations DROP CONSTRAINT chk_context_citation_https';
        EXECUTE 'ALTER TABLE public.consultation_context_citations ADD CONSTRAINT chk_context_citation_https CHECK (((source_url)::text ~~ ''https://%''::text))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_context_citation_ordinal' AND conrelid = 'public.consultation_context_citations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_citations ADD CONSTRAINT chk_context_citation_ordinal CHECK ((ordinal >= 0))';
    ELSIF v_existing_def <> 'CHECK ((ordinal >= 0))' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_citations DROP CONSTRAINT chk_context_citation_ordinal';
        EXECUTE 'ALTER TABLE public.consultation_context_citations ADD CONSTRAINT chk_context_citation_ordinal CHECK ((ordinal >= 0))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'uq_context_citation_source' AND conrelid = 'public.consultation_context_citations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_citations ADD CONSTRAINT uq_context_citation_source UNIQUE (context_share_id, evidence_source_id)';
    ELSIF v_existing_def <> 'UNIQUE (context_share_id, evidence_source_id)' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_citations DROP CONSTRAINT uq_context_citation_source';
        EXECUTE 'ALTER TABLE public.consultation_context_citations ADD CONSTRAINT uq_context_citation_source UNIQUE (context_share_id, evidence_source_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_context_completed' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT chk_context_completed CHECK (((intake_status)::text = ''COMPLETED''::text))';
    ELSIF v_existing_def <> 'CHECK (((intake_status)::text = ''COMPLETED''::text))' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT chk_context_completed';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT chk_context_completed CHECK (((intake_status)::text = ''COMPLETED''::text))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_context_origin' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT chk_context_origin CHECK (((origin_dashboard)::text = ANY (ARRAY[(''MOTHER_JOURNEY''::character varying)::text, (''BABY_PROFILE''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((origin_dashboard)::text = ANY (ARRAY[(''MOTHER_JOURNEY''::character varying)::text, (''BABY_PROFILE''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT chk_context_origin';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT chk_context_origin CHECK (((origin_dashboard)::text = ANY (ARRAY[(''MOTHER_JOURNEY''::character varying)::text, (''BABY_PROFILE''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_context_policy' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT chk_context_policy CHECK (((share_policy_version)::text = ''YELLOW_EXPERT_CONTEXT_V1''::text))';
    ELSIF v_existing_def <> 'CHECK (((share_policy_version)::text = ''YELLOW_EXPERT_CONTEXT_V1''::text))' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT chk_context_policy';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT chk_context_policy CHECK (((share_policy_version)::text = ''YELLOW_EXPERT_CONTEXT_V1''::text))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_context_stage' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT chk_context_stage CHECK (((triage_stage)::text = ANY (ARRAY[(''PRECONCEPTION''::character varying)::text, (''PREGNANCY''::character varying)::text, (''POSTPARTUM''::character varying)::text, (''INFANT''::character varying)::text, (''TODDLER''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((triage_stage)::text = ANY (ARRAY[(''PRECONCEPTION''::character varying)::text, (''PREGNANCY''::character varying)::text, (''POSTPARTUM''::character varying)::text, (''INFANT''::character varying)::text, (''TODDLER''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT chk_context_stage';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT chk_context_stage CHECK (((triage_stage)::text = ANY (ARRAY[(''PRECONCEPTION''::character varying)::text, (''PREGNANCY''::character varying)::text, (''POSTPARTUM''::character varying)::text, (''INFANT''::character varying)::text, (''TODDLER''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_context_summary' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT chk_context_summary CHECK (((length(btrim((risk_summary)::text)) >= 1) AND (length(btrim((risk_summary)::text)) <= 500)))';
    ELSIF v_existing_def <> 'CHECK (((length(btrim((risk_summary)::text)) >= 1) AND (length(btrim((risk_summary)::text)) <= 500)))' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT chk_context_summary';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT chk_context_summary CHECK (((length(btrim((risk_summary)::text)) >= 1) AND (length(btrim((risk_summary)::text)) <= 500)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_context_yellow' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT chk_context_yellow CHECK (((risk_level)::text = ''YELLOW''::text))';
    ELSIF v_existing_def <> 'CHECK (((risk_level)::text = ''YELLOW''::text))' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT chk_context_yellow';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT chk_context_yellow CHECK (((risk_level)::text = ''YELLOW''::text))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'consultation_context_shares_consent_grant_id_key' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT consultation_context_shares_consent_grant_id_key UNIQUE (consent_grant_id)';
    ELSIF v_existing_def <> 'UNIQUE (consent_grant_id)' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT consultation_context_shares_consent_grant_id_key';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT consultation_context_shares_consent_grant_id_key UNIQUE (consent_grant_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'consultation_context_shares_consultation_request_id_key' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT consultation_context_shares_consultation_request_id_key UNIQUE (consultation_request_id)';
    ELSIF v_existing_def <> 'UNIQUE (consultation_request_id)' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT consultation_context_shares_consultation_request_id_key';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT consultation_context_shares_consultation_request_id_key UNIQUE (consultation_request_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'uq_context_intake_expert' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT uq_context_intake_expert UNIQUE (owner_user_id, intake_session_id, expert_profile_id)';
    ELSIF v_existing_def <> 'UNIQUE (owner_user_id, intake_session_id, expert_profile_id)' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT uq_context_intake_expert';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT uq_context_intake_expert UNIQUE (owner_user_id, intake_session_id, expert_profile_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'uq_context_owner_key' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT uq_context_owner_key UNIQUE (owner_user_id, idempotency_key)';
    ELSIF v_existing_def <> 'UNIQUE (owner_user_id, idempotency_key)' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT uq_context_owner_key';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT uq_context_owner_key UNIQUE (owner_user_id, idempotency_key)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'content_item_sources_unique_url_uk' AND conrelid = 'public.content_item_sources'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.content_item_sources ADD CONSTRAINT content_item_sources_unique_url_uk UNIQUE (content_item_id, source_url)';
    ELSIF v_existing_def <> 'UNIQUE (content_item_id, source_url)' THEN
        EXECUTE 'ALTER TABLE public.content_item_sources DROP CONSTRAINT content_item_sources_unique_url_uk';
        EXECUTE 'ALTER TABLE public.content_item_sources ADD CONSTRAINT content_item_sources_unique_url_uk UNIQUE (content_item_id, source_url)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'conversation_calls_answered_ck' AND conrelid = 'public.conversation_calls'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_answered_ck CHECK (((answered_at IS NULL) OR (answered_at >= initiated_at)))';
    ELSIF v_existing_def <> 'CHECK (((answered_at IS NULL) OR (answered_at >= initiated_at)))' THEN
        EXECUTE 'ALTER TABLE public.conversation_calls DROP CONSTRAINT conversation_calls_answered_ck';
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_answered_ck CHECK (((answered_at IS NULL) OR (answered_at >= initiated_at)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'conversation_calls_duration_ck' AND conrelid = 'public.conversation_calls'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_duration_ck CHECK (((duration_seconds IS NULL) OR (duration_seconds >= 0)))';
    ELSIF v_existing_def <> 'CHECK (((duration_seconds IS NULL) OR (duration_seconds >= 0)))' THEN
        EXECUTE 'ALTER TABLE public.conversation_calls DROP CONSTRAINT conversation_calls_duration_ck';
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_duration_ck CHECK (((duration_seconds IS NULL) OR (duration_seconds >= 0)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'conversation_calls_ended_answered_ck' AND conrelid = 'public.conversation_calls'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_ended_answered_ck CHECK ((((call_status)::text <> ''ENDED''::text) OR (answered_at IS NOT NULL)))';
    ELSIF v_existing_def <> 'CHECK ((((call_status)::text <> ''ENDED''::text) OR (answered_at IS NOT NULL)))' THEN
        EXECUTE 'ALTER TABLE public.conversation_calls DROP CONSTRAINT conversation_calls_ended_answered_ck';
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_ended_answered_ck CHECK ((((call_status)::text <> ''ENDED''::text) OR (answered_at IS NOT NULL)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'conversation_calls_ended_ck' AND conrelid = 'public.conversation_calls'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_ended_ck CHECK (((ended_at IS NULL) OR (ended_at >= initiated_at)))';
    ELSIF v_existing_def <> 'CHECK (((ended_at IS NULL) OR (ended_at >= initiated_at)))' THEN
        EXECUTE 'ALTER TABLE public.conversation_calls DROP CONSTRAINT conversation_calls_ended_ck';
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_ended_ck CHECK (((ended_at IS NULL) OR (ended_at >= initiated_at)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'conversation_calls_status_ck' AND conrelid = 'public.conversation_calls'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_status_ck CHECK (((call_status)::text = ANY (ARRAY[(''INITIATED''::character varying)::text, (''RINGING''::character varying)::text, (''ANSWERED''::character varying)::text, (''DECLINED''::character varying)::text, (''MISSED''::character varying)::text, (''CANCELLED''::character varying)::text, (''ENDED''::character varying)::text, (''FAILED''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((call_status)::text = ANY (ARRAY[(''INITIATED''::character varying)::text, (''RINGING''::character varying)::text, (''ANSWERED''::character varying)::text, (''DECLINED''::character varying)::text, (''MISSED''::character varying)::text, (''CANCELLED''::character varying)::text, (''ENDED''::character varying)::text, (''FAILED''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.conversation_calls DROP CONSTRAINT conversation_calls_status_ck';
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_status_ck CHECK (((call_status)::text = ANY (ARRAY[(''INITIATED''::character varying)::text, (''RINGING''::character varying)::text, (''ANSWERED''::character varying)::text, (''DECLINED''::character varying)::text, (''MISSED''::character varying)::text, (''CANCELLED''::character varying)::text, (''ENDED''::character varying)::text, (''FAILED''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'conversation_calls_type_ck' AND conrelid = 'public.conversation_calls'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_type_ck CHECK (((call_type)::text = ANY (ARRAY[(''VOICE''::character varying)::text, (''VIDEO''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((call_type)::text = ANY (ARRAY[(''VOICE''::character varying)::text, (''VIDEO''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.conversation_calls DROP CONSTRAINT conversation_calls_type_ck';
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_type_ck CHECK (((call_type)::text = ANY (ARRAY[(''VOICE''::character varying)::text, (''VIDEO''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'device_tokens_platform_check' AND conrelid = 'public.device_tokens'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.device_tokens ADD CONSTRAINT device_tokens_platform_check CHECK (((platform)::text = ANY (ARRAY[(''ANDROID''::character varying)::text, (''IOS''::character varying)::text, (''WEB''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((platform)::text = ANY (ARRAY[(''ANDROID''::character varying)::text, (''IOS''::character varying)::text, (''WEB''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.device_tokens DROP CONSTRAINT device_tokens_platform_check';
        EXECUTE 'ALTER TABLE public.device_tokens ADD CONSTRAINT device_tokens_platform_check CHECK (((platform)::text = ANY (ARRAY[(''ANDROID''::character varying)::text, (''IOS''::character varying)::text, (''WEB''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'device_tokens_unique' AND conrelid = 'public.device_tokens'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.device_tokens ADD CONSTRAINT device_tokens_unique UNIQUE (user_id, token)';
    ELSIF v_existing_def <> 'UNIQUE (user_id, token)' THEN
        EXECUTE 'ALTER TABLE public.device_tokens DROP CONSTRAINT device_tokens_unique';
        EXECUTE 'ALTER TABLE public.device_tokens ADD CONSTRAINT device_tokens_unique UNIQUE (user_id, token)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'direct_conversations_activity_ck' AND conrelid = 'public.direct_conversations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.direct_conversations ADD CONSTRAINT direct_conversations_activity_ck CHECK (((last_activity_at IS NULL) OR (last_activity_at >= created_at)))';
    ELSIF v_existing_def <> 'CHECK (((last_activity_at IS NULL) OR (last_activity_at >= created_at)))' THEN
        EXECUTE 'ALTER TABLE public.direct_conversations DROP CONSTRAINT direct_conversations_activity_ck';
        EXECUTE 'ALTER TABLE public.direct_conversations ADD CONSTRAINT direct_conversations_activity_ck CHECK (((last_activity_at IS NULL) OR (last_activity_at >= created_at)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'direct_conversations_pair_uk' AND conrelid = 'public.direct_conversations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.direct_conversations ADD CONSTRAINT direct_conversations_pair_uk UNIQUE (mother_user_id, expert_user_id)';
    ELSIF v_existing_def <> 'UNIQUE (mother_user_id, expert_user_id)' THEN
        EXECUTE 'ALTER TABLE public.direct_conversations DROP CONSTRAINT direct_conversations_pair_uk';
        EXECUTE 'ALTER TABLE public.direct_conversations ADD CONSTRAINT direct_conversations_pair_uk UNIQUE (mother_user_id, expert_user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'direct_conversations_status_ck' AND conrelid = 'public.direct_conversations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.direct_conversations ADD CONSTRAINT direct_conversations_status_ck CHECK (((status)::text = ''ACTIVE''::text))';
    ELSIF v_existing_def <> 'CHECK (((status)::text = ''ACTIVE''::text))' THEN
        EXECUTE 'ALTER TABLE public.direct_conversations DROP CONSTRAINT direct_conversations_status_ck';
        EXECUTE 'ALTER TABLE public.direct_conversations ADD CONSTRAINT direct_conversations_status_ck CHECK (((status)::text = ''ACTIVE''::text))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'direct_messages_body_ck' AND conrelid = 'public.direct_messages'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.direct_messages ADD CONSTRAINT direct_messages_body_ck CHECK (((length(btrim(message_body)) >= 1) AND (length(btrim(message_body)) <= 2000)))';
    ELSIF v_existing_def <> 'CHECK (((length(btrim(message_body)) >= 1) AND (length(btrim(message_body)) <= 2000)))' THEN
        EXECUTE 'ALTER TABLE public.direct_messages DROP CONSTRAINT direct_messages_body_ck';
        EXECUTE 'ALTER TABLE public.direct_messages ADD CONSTRAINT direct_messages_body_ck CHECK (((length(btrim(message_body)) >= 1) AND (length(btrim(message_body)) <= 2000)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'direct_messages_client_uk' AND conrelid = 'public.direct_messages'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.direct_messages ADD CONSTRAINT direct_messages_client_uk UNIQUE (conversation_id, sender_user_id, client_message_id)';
    ELSIF v_existing_def <> 'UNIQUE (conversation_id, sender_user_id, client_message_id)' THEN
        EXECUTE 'ALTER TABLE public.direct_messages DROP CONSTRAINT direct_messages_client_uk';
        EXECUTE 'ALTER TABLE public.direct_messages ADD CONSTRAINT direct_messages_client_uk UNIQUE (conversation_id, sender_user_id, client_message_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'direct_messages_type_ck' AND conrelid = 'public.direct_messages'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.direct_messages ADD CONSTRAINT direct_messages_type_ck CHECK (((message_type)::text = ''TEXT''::text))';
    ELSIF v_existing_def <> 'CHECK (((message_type)::text = ''TEXT''::text))' THEN
        EXECUTE 'ALTER TABLE public.direct_messages DROP CONSTRAINT direct_messages_type_ck';
        EXECUTE 'ALTER TABLE public.direct_messages ADD CONSTRAINT direct_messages_type_ck CHECK (((message_type)::text = ''TEXT''::text))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'expert_consultation_requests_expiry_ck' AND conrelid = 'public.expert_consultation_requests'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_expiry_ck CHECK ((expires_at > created_at))';
    ELSIF v_existing_def <> 'CHECK ((expires_at > created_at))' THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests DROP CONSTRAINT expert_consultation_requests_expiry_ck';
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_expiry_ck CHECK ((expires_at > created_at))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'expert_consultation_requests_owner_client_uk' AND conrelid = 'public.expert_consultation_requests'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_owner_client_uk UNIQUE (requester_user_id, client_request_id)';
    ELSIF v_existing_def <> 'UNIQUE (requester_user_id, client_request_id)' THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests DROP CONSTRAINT expert_consultation_requests_owner_client_uk';
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_owner_client_uk UNIQUE (requester_user_id, client_request_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'expert_consultation_requests_responded_ck' AND conrelid = 'public.expert_consultation_requests'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_responded_ck CHECK ((((status)::text = ''PENDING''::text) OR (responded_at IS NOT NULL)))';
    ELSIF v_existing_def <> 'CHECK ((((status)::text = ''PENDING''::text) OR (responded_at IS NOT NULL)))' THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests DROP CONSTRAINT expert_consultation_requests_responded_ck';
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_responded_ck CHECK ((((status)::text = ''PENDING''::text) OR (responded_at IS NOT NULL)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'expert_consultation_requests_status_ck' AND conrelid = 'public.expert_consultation_requests'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_status_ck CHECK (((status)::text = ANY (ARRAY[(''PENDING''::character varying)::text, (''ACCEPTED''::character varying)::text, (''REJECTED''::character varying)::text, (''CANCELLED''::character varying)::text, (''EXPIRED''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((status)::text = ANY (ARRAY[(''PENDING''::character varying)::text, (''ACCEPTED''::character varying)::text, (''REJECTED''::character varying)::text, (''CANCELLED''::character varying)::text, (''EXPIRED''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests DROP CONSTRAINT expert_consultation_requests_status_ck';
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_status_ck CHECK (((status)::text = ANY (ARRAY[(''PENDING''::character varying)::text, (''ACCEPTED''::character varying)::text, (''REJECTED''::character varying)::text, (''CANCELLED''::character varying)::text, (''EXPIRED''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'expert_consultation_requests_window_ck' AND conrelid = 'public.expert_consultation_requests'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_window_ck CHECK ((((preferred_window_start IS NULL) AND (preferred_window_end IS NULL)) OR ((preferred_window_start IS NOT NULL) AND (preferred_window_end IS NOT NULL) AND (preferred_window_end > preferred_window_start))))';
    ELSIF v_existing_def <> 'CHECK ((((preferred_window_start IS NULL) AND (preferred_window_end IS NULL)) OR ((preferred_window_start IS NOT NULL) AND (preferred_window_end IS NOT NULL) AND (preferred_window_end > preferred_window_start))))' THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests DROP CONSTRAINT expert_consultation_requests_window_ck';
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_window_ck CHECK ((((preferred_window_start IS NULL) AND (preferred_window_end IS NULL)) OR ((preferred_window_start IS NOT NULL) AND (preferred_window_end IS NOT NULL) AND (preferred_window_end > preferred_window_start))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_moderation_cases_ai_feedback_decision' AND conrelid = 'public.moderation_cases'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.moderation_cases ADD CONSTRAINT chk_moderation_cases_ai_feedback_decision CHECK (((ai_feedback_decision IS NULL) OR ((ai_feedback_decision)::text = ANY (ARRAY[(''AGREE''::character varying)::text, (''DISAGREE''::character varying)::text]))))';
    ELSIF v_existing_def <> 'CHECK (((ai_feedback_decision IS NULL) OR ((ai_feedback_decision)::text = ANY (ARRAY[(''AGREE''::character varying)::text, (''DISAGREE''::character varying)::text]))))' THEN
        EXECUTE 'ALTER TABLE public.moderation_cases DROP CONSTRAINT chk_moderation_cases_ai_feedback_decision';
        EXECUTE 'ALTER TABLE public.moderation_cases ADD CONSTRAINT chk_moderation_cases_ai_feedback_decision CHECK (((ai_feedback_decision IS NULL) OR ((ai_feedback_decision)::text = ANY (ARRAY[(''AGREE''::character varying)::text, (''DISAGREE''::character varying)::text]))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_mother_journeys_date_confidence' AND conrelid = 'public.mother_journeys'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.mother_journeys ADD CONSTRAINT chk_mother_journeys_date_confidence CHECK (((date_confidence IS NULL) OR ((date_confidence)::text = ANY (ARRAY[(''CONFIRMED''::character varying)::text, (''ESTIMATED''::character varying)::text, (''UNKNOWN''::character varying)::text]))))';
    ELSIF v_existing_def <> 'CHECK (((date_confidence IS NULL) OR ((date_confidence)::text = ANY (ARRAY[(''CONFIRMED''::character varying)::text, (''ESTIMATED''::character varying)::text, (''UNKNOWN''::character varying)::text]))))' THEN
        EXECUTE 'ALTER TABLE public.mother_journeys DROP CONSTRAINT chk_mother_journeys_date_confidence';
        EXECUTE 'ALTER TABLE public.mother_journeys ADD CONSTRAINT chk_mother_journeys_date_confidence CHECK (((date_confidence IS NULL) OR ((date_confidence)::text = ANY (ARRAY[(''CONFIRMED''::character varying)::text, (''ESTIMATED''::character varying)::text, (''UNKNOWN''::character varying)::text]))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_mother_journeys_date_source' AND conrelid = 'public.mother_journeys'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.mother_journeys ADD CONSTRAINT chk_mother_journeys_date_source CHECK (((date_source IS NULL) OR ((date_source)::text = ANY (ARRAY[(''SELF_REPORTED''::character varying)::text, (''CLINICIAN_CONFIRMED''::character varying)::text, (''ULTRASOUND''::character varying)::text, (''SYSTEM_DERIVED''::character varying)::text, (''MIGRATION''::character varying)::text, (''UNKNOWN''::character varying)::text]))))';
    ELSIF v_existing_def <> 'CHECK (((date_source IS NULL) OR ((date_source)::text = ANY (ARRAY[(''SELF_REPORTED''::character varying)::text, (''CLINICIAN_CONFIRMED''::character varying)::text, (''ULTRASOUND''::character varying)::text, (''SYSTEM_DERIVED''::character varying)::text, (''MIGRATION''::character varying)::text, (''UNKNOWN''::character varying)::text]))))' THEN
        EXECUTE 'ALTER TABLE public.mother_journeys DROP CONSTRAINT chk_mother_journeys_date_source';
        EXECUTE 'ALTER TABLE public.mother_journeys ADD CONSTRAINT chk_mother_journeys_date_source CHECK (((date_source IS NULL) OR ((date_source)::text = ANY (ARRAY[(''SELF_REPORTED''::character varying)::text, (''CLINICIAN_CONFIRMED''::character varying)::text, (''ULTRASOUND''::character varying)::text, (''SYSTEM_DERIVED''::character varying)::text, (''MIGRATION''::character varying)::text, (''UNKNOWN''::character varying)::text]))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'ck_mother_journey_live_birth_date' AND conrelid = 'public.mother_journeys'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.mother_journeys ADD CONSTRAINT ck_mother_journey_live_birth_date CHECK ((((pregnancy_outcome)::text <> ''LIVE_BIRTH''::text) OR (pregnancy_outcome_date IS NOT NULL)))';
    ELSIF v_existing_def <> 'CHECK ((((pregnancy_outcome)::text <> ''LIVE_BIRTH''::text) OR (pregnancy_outcome_date IS NOT NULL)))' THEN
        EXECUTE 'ALTER TABLE public.mother_journeys DROP CONSTRAINT ck_mother_journey_live_birth_date';
        EXECUTE 'ALTER TABLE public.mother_journeys ADD CONSTRAINT ck_mother_journey_live_birth_date CHECK ((((pregnancy_outcome)::text <> ''LIVE_BIRTH''::text) OR (pregnancy_outcome_date IS NOT NULL)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'ck_mother_journey_pregnancy_outcome' AND conrelid = 'public.mother_journeys'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.mother_journeys ADD CONSTRAINT ck_mother_journey_pregnancy_outcome CHECK (((pregnancy_outcome IS NULL) OR ((pregnancy_outcome)::text = ANY (ARRAY[(''ONGOING''::character varying)::text, (''UNKNOWN''::character varying)::text, (''LIVE_BIRTH''::character varying)::text, (''PREGNANCY_LOSS''::character varying)::text, (''STILLBIRTH''::character varying)::text]))))';
    ELSIF v_existing_def <> 'CHECK (((pregnancy_outcome IS NULL) OR ((pregnancy_outcome)::text = ANY (ARRAY[(''ONGOING''::character varying)::text, (''UNKNOWN''::character varying)::text, (''LIVE_BIRTH''::character varying)::text, (''PREGNANCY_LOSS''::character varying)::text, (''STILLBIRTH''::character varying)::text]))))' THEN
        EXECUTE 'ALTER TABLE public.mother_journeys DROP CONSTRAINT ck_mother_journey_pregnancy_outcome';
        EXECUTE 'ALTER TABLE public.mother_journeys ADD CONSTRAINT ck_mother_journey_pregnancy_outcome CHECK (((pregnancy_outcome IS NULL) OR ((pregnancy_outcome)::text = ANY (ARRAY[(''ONGOING''::character varying)::text, (''UNKNOWN''::character varying)::text, (''LIVE_BIRTH''::character varying)::text, (''PREGNANCY_LOSS''::character varying)::text, (''STILLBIRTH''::character varying)::text]))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'mother_journeys_subject_uk' AND conrelid = 'public.mother_journeys'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.mother_journeys ADD CONSTRAINT mother_journeys_subject_uk UNIQUE (care_subject_id)';
    ELSIF v_existing_def <> 'UNIQUE (care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.mother_journeys DROP CONSTRAINT mother_journeys_subject_uk';
        EXECUTE 'ALTER TABLE public.mother_journeys ADD CONSTRAINT mother_journeys_subject_uk UNIQUE (care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'notification_records_channel_check' AND conrelid = 'public.notification_records'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.notification_records ADD CONSTRAINT notification_records_channel_check CHECK (((channel)::text = ANY (ARRAY[(''PUSH''::character varying)::text, (''EMAIL''::character varying)::text, (''IN_APP''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((channel)::text = ANY (ARRAY[(''PUSH''::character varying)::text, (''EMAIL''::character varying)::text, (''IN_APP''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.notification_records DROP CONSTRAINT notification_records_channel_check';
        EXECUTE 'ALTER TABLE public.notification_records ADD CONSTRAINT notification_records_channel_check CHECK (((channel)::text = ANY (ARRAY[(''PUSH''::character varying)::text, (''EMAIL''::character varying)::text, (''IN_APP''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'notification_records_status_check' AND conrelid = 'public.notification_records'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.notification_records ADD CONSTRAINT notification_records_status_check CHECK (((status)::text = ANY (ARRAY[(''PENDING''::character varying)::text, (''PROCESSING''::character varying)::text, (''SENT''::character varying)::text, (''DELIVERED''::character varying)::text, (''FAILED''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((status)::text = ANY (ARRAY[(''PENDING''::character varying)::text, (''PROCESSING''::character varying)::text, (''SENT''::character varying)::text, (''DELIVERED''::character varying)::text, (''FAILED''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.notification_records DROP CONSTRAINT notification_records_status_check';
        EXECUTE 'ALTER TABLE public.notification_records ADD CONSTRAINT notification_records_status_check CHECK (((status)::text = ANY (ARRAY[(''PENDING''::character varying)::text, (''PROCESSING''::character varying)::text, (''SENT''::character varying)::text, (''DELIVERED''::character varying)::text, (''FAILED''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'notification_records_type_check' AND conrelid = 'public.notification_records'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.notification_records ADD CONSTRAINT notification_records_type_check CHECK (((type)::text = ANY (ARRAY[(''REMINDER''::character varying)::text, (''COMMUNITY_REPLY''::character varying)::text, (''CONSULTATION''::character varying)::text, (''EMERGENCY''::character varying)::text, (''MESSAGE''::character varying)::text, (''GROUP_INVITE''::character varying)::text, (''CONTENT_REVIEW''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((type)::text = ANY (ARRAY[(''REMINDER''::character varying)::text, (''COMMUNITY_REPLY''::character varying)::text, (''CONSULTATION''::character varying)::text, (''EMERGENCY''::character varying)::text, (''MESSAGE''::character varying)::text, (''GROUP_INVITE''::character varying)::text, (''CONTENT_REVIEW''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.notification_records DROP CONSTRAINT notification_records_type_check';
        EXECUTE 'ALTER TABLE public.notification_records ADD CONSTRAINT notification_records_type_check CHECK (((type)::text = ANY (ARRAY[(''REMINDER''::character varying)::text, (''COMMUNITY_REPLY''::character varying)::text, (''CONSULTATION''::character varying)::text, (''EMERGENCY''::character varying)::text, (''MESSAGE''::character varying)::text, (''GROUP_INVITE''::character varying)::text, (''CONTENT_REVIEW''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'uk_partner_email' AND conrelid = 'public.partner_organizations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.partner_organizations ADD CONSTRAINT uk_partner_email UNIQUE (email)';
    ELSIF v_existing_def <> 'UNIQUE (email)' THEN
        EXECUTE 'ALTER TABLE public.partner_organizations DROP CONSTRAINT uk_partner_email';
        EXECUTE 'ALTER TABLE public.partner_organizations ADD CONSTRAINT uk_partner_email UNIQUE (email)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'uk_partner_representative' AND conrelid = 'public.partner_organizations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.partner_organizations ADD CONSTRAINT uk_partner_representative UNIQUE (representative_user_id)';
    ELSIF v_existing_def <> 'UNIQUE (representative_user_id)' THEN
        EXECUTE 'ALTER TABLE public.partner_organizations DROP CONSTRAINT uk_partner_representative';
        EXECUTE 'ALTER TABLE public.partner_organizations ADD CONSTRAINT uk_partner_representative UNIQUE (representative_user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_red_flag_rules_action' AND conrelid = 'public.red_flag_rules'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.red_flag_rules ADD CONSTRAINT chk_red_flag_rules_action CHECK (((action)::text = ANY (ARRAY[(''BLOCK''::character varying)::text, (''WARN''::character varying)::text, (''ESCALATE''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((action)::text = ANY (ARRAY[(''BLOCK''::character varying)::text, (''WARN''::character varying)::text, (''ESCALATE''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.red_flag_rules DROP CONSTRAINT chk_red_flag_rules_action';
        EXECUTE 'ALTER TABLE public.red_flag_rules ADD CONSTRAINT chk_red_flag_rules_action CHECK (((action)::text = ANY (ARRAY[(''BLOCK''::character varying)::text, (''WARN''::character varying)::text, (''ESCALATE''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_red_flag_rules_severity' AND conrelid = 'public.red_flag_rules'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.red_flag_rules ADD CONSTRAINT chk_red_flag_rules_severity CHECK (((severity)::text = ANY (ARRAY[(''GREEN''::character varying)::text, (''YELLOW''::character varying)::text, (''RED''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((severity)::text = ANY (ARRAY[(''GREEN''::character varying)::text, (''YELLOW''::character varying)::text, (''RED''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.red_flag_rules DROP CONSTRAINT chk_red_flag_rules_severity';
        EXECUTE 'ALTER TABLE public.red_flag_rules ADD CONSTRAINT chk_red_flag_rules_severity CHECK (((severity)::text = ANY (ARRAY[(''GREEN''::character varying)::text, (''YELLOW''::character varying)::text, (''RED''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'uq_red_flag_rules_keyword' AND conrelid = 'public.red_flag_rules'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.red_flag_rules ADD CONSTRAINT uq_red_flag_rules_keyword UNIQUE (keyword)';
    ELSIF v_existing_def <> 'UNIQUE (keyword)' THEN
        EXECUTE 'ALTER TABLE public.red_flag_rules DROP CONSTRAINT uq_red_flag_rules_keyword';
        EXECUTE 'ALTER TABLE public.red_flag_rules ADD CONSTRAINT uq_red_flag_rules_keyword UNIQUE (keyword)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_configs_countdown_seconds_check' AND conrelid = 'public.safety_configs'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_configs ADD CONSTRAINT safety_configs_countdown_seconds_check CHECK ((countdown_seconds = ANY (ARRAY[15, 30, 60])))';
    ELSIF v_existing_def <> 'CHECK ((countdown_seconds = ANY (ARRAY[15, 30, 60])))' THEN
        EXECUTE 'ALTER TABLE public.safety_configs DROP CONSTRAINT safety_configs_countdown_seconds_check';
        EXECUTE 'ALTER TABLE public.safety_configs ADD CONSTRAINT safety_configs_countdown_seconds_check CHECK ((countdown_seconds = ANY (ARRAY[15, 30, 60])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_configs_sensor_permission_ck' AND conrelid = 'public.safety_configs'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_configs ADD CONSTRAINT safety_configs_sensor_permission_ck CHECK (((sensor_permission_granted = false) OR (sensor_permission_recorded_at IS NOT NULL)))';
    ELSIF v_existing_def <> 'CHECK (((sensor_permission_granted = false) OR (sensor_permission_recorded_at IS NOT NULL)))' THEN
        EXECUTE 'ALTER TABLE public.safety_configs DROP CONSTRAINT safety_configs_sensor_permission_ck';
        EXECUTE 'ALTER TABLE public.safety_configs ADD CONSTRAINT safety_configs_sensor_permission_ck CHECK (((sensor_permission_granted = false) OR (sensor_permission_recorded_at IS NOT NULL)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_configs_user_id_key' AND conrelid = 'public.safety_configs'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_configs ADD CONSTRAINT safety_configs_user_id_key UNIQUE (user_id)';
    ELSIF v_existing_def <> 'UNIQUE (user_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_configs DROP CONSTRAINT safety_configs_user_id_key';
        EXECUTE 'ALTER TABLE public.safety_configs ADD CONSTRAINT safety_configs_user_id_key UNIQUE (user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_alert_claim_ck' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_alert_claim_ck CHECK (((((alert_status)::text = ''PROCESSING''::text) AND (alert_claim_token IS NOT NULL) AND (alert_lease_expires_at IS NOT NULL)) OR ((alert_status)::text IS DISTINCT FROM ''PROCESSING''::text)))';
    ELSIF v_existing_def <> 'CHECK (((((alert_status)::text = ''PROCESSING''::text) AND (alert_claim_token IS NOT NULL) AND (alert_lease_expires_at IS NOT NULL)) OR ((alert_status)::text IS DISTINCT FROM ''PROCESSING''::text)))' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_alert_claim_ck';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_alert_claim_ck CHECK (((((alert_status)::text = ''PROCESSING''::text) AND (alert_claim_token IS NOT NULL) AND (alert_lease_expires_at IS NOT NULL)) OR ((alert_status)::text IS DISTINCT FROM ''PROCESSING''::text)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_alert_generation_ck' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_alert_generation_ck CHECK ((alert_generation >= 0))';
    ELSIF v_existing_def <> 'CHECK ((alert_generation >= 0))' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_alert_generation_ck';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_alert_generation_ck CHECK ((alert_generation >= 0))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_alert_recipient_counts_ck' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_alert_recipient_counts_ck CHECK (((alert_successful_recipient_count >= 0) AND (alert_failed_recipient_count >= 0)))';
    ELSIF v_existing_def <> 'CHECK (((alert_successful_recipient_count >= 0) AND (alert_failed_recipient_count >= 0)))' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_alert_recipient_counts_ck';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_alert_recipient_counts_ck CHECK (((alert_successful_recipient_count >= 0) AND (alert_failed_recipient_count >= 0)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_alert_status_ck' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_alert_status_ck CHECK (((alert_status IS NULL) OR ((alert_status)::text = ANY (ARRAY[(''PROCESSING''::character varying)::text, (''FAILED''::character varying)::text, (''PARTIAL''::character varying)::text, (''NO_RECIPIENTS''::character varying)::text, (''SENT''::character varying)::text, (''SUPPRESSED''::character varying)::text]))))';
    ELSIF v_existing_def <> 'CHECK (((alert_status IS NULL) OR ((alert_status)::text = ANY (ARRAY[(''PROCESSING''::character varying)::text, (''FAILED''::character varying)::text, (''PARTIAL''::character varying)::text, (''NO_RECIPIENTS''::character varying)::text, (''SENT''::character varying)::text, (''SUPPRESSED''::character varying)::text]))))' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_alert_status_ck';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_alert_status_ck CHECK (((alert_status IS NULL) OR ((alert_status)::text = ANY (ARRAY[(''PROCESSING''::character varying)::text, (''FAILED''::character varying)::text, (''PARTIAL''::character varying)::text, (''NO_RECIPIENTS''::character varying)::text, (''SENT''::character varying)::text, (''SUPPRESSED''::character varying)::text]))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_record_type_ck' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_record_type_ck CHECK (((record_type)::text = ANY (ARRAY[(''IMU_EVENT''::character varying)::text, (''EMERGENCY_SESSION''::character varying)::text, (''SAFETY_ACTION''::character varying)::text])))';
    ELSIF v_existing_def <> 'CHECK (((record_type)::text = ANY (ARRAY[(''IMU_EVENT''::character varying)::text, (''EMERGENCY_SESSION''::character varying)::text, (''SAFETY_ACTION''::character varying)::text])))' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_record_type_ck';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_record_type_ck CHECK (((record_type)::text = ANY (ARRAY[(''IMU_EVENT''::character varying)::text, (''EMERGENCY_SESSION''::character varying)::text, (''SAFETY_ACTION''::character varying)::text])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'specialties_canonical_code_key' AND conrelid = 'public.specialties'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.specialties ADD CONSTRAINT specialties_canonical_code_key UNIQUE (code)';
    ELSIF v_existing_def <> 'UNIQUE (code)' THEN
        EXECUTE 'ALTER TABLE public.specialties DROP CONSTRAINT specialties_canonical_code_key';
        EXECUTE 'ALTER TABLE public.specialties ADD CONSTRAINT specialties_canonical_code_key UNIQUE (code)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'system_configurations_api_rate_limit_check' AND conrelid = 'public.system_configurations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.system_configurations ADD CONSTRAINT system_configurations_api_rate_limit_check CHECK (((api_rate_limit >= 1) AND (api_rate_limit <= 100000)))';
    ELSIF v_existing_def <> 'CHECK (((api_rate_limit >= 1) AND (api_rate_limit <= 100000)))' THEN
        EXECUTE 'ALTER TABLE public.system_configurations DROP CONSTRAINT system_configurations_api_rate_limit_check';
        EXECUTE 'ALTER TABLE public.system_configurations ADD CONSTRAINT system_configurations_api_rate_limit_check CHECK (((api_rate_limit >= 1) AND (api_rate_limit <= 100000)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'system_configurations_connection_timeout_ms_check' AND conrelid = 'public.system_configurations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.system_configurations ADD CONSTRAINT system_configurations_connection_timeout_ms_check CHECK (((connection_timeout_ms >= 1000) AND (connection_timeout_ms <= 300000)))';
    ELSIF v_existing_def <> 'CHECK (((connection_timeout_ms >= 1000) AND (connection_timeout_ms <= 300000)))' THEN
        EXECUTE 'ALTER TABLE public.system_configurations DROP CONSTRAINT system_configurations_connection_timeout_ms_check';
        EXECUTE 'ALTER TABLE public.system_configurations ADD CONSTRAINT system_configurations_connection_timeout_ms_check CHECK (((connection_timeout_ms >= 1000) AND (connection_timeout_ms <= 300000)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'system_configurations_max_upload_size_mb_check' AND conrelid = 'public.system_configurations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.system_configurations ADD CONSTRAINT system_configurations_max_upload_size_mb_check CHECK (((max_upload_size_mb >= 1) AND (max_upload_size_mb <= 1024)))';
    ELSIF v_existing_def <> 'CHECK (((max_upload_size_mb >= 1) AND (max_upload_size_mb <= 1024)))' THEN
        EXECUTE 'ALTER TABLE public.system_configurations DROP CONSTRAINT system_configurations_max_upload_size_mb_check';
        EXECUTE 'ALTER TABLE public.system_configurations ADD CONSTRAINT system_configurations_max_upload_size_mb_check CHECK (((max_upload_size_mb >= 1) AND (max_upload_size_mb <= 1024)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'triage_session_evidence_uk' AND conrelid = 'public.triage_session_evidence'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.triage_session_evidence ADD CONSTRAINT triage_session_evidence_uk UNIQUE (triage_session_id, evidence_type, content_hash)';
    ELSIF v_existing_def <> 'UNIQUE (triage_session_id, evidence_type, content_hash)' THEN
        EXECUTE 'ALTER TABLE public.triage_session_evidence DROP CONSTRAINT triage_session_evidence_uk';
        EXECUTE 'ALTER TABLE public.triage_session_evidence ADD CONSTRAINT triage_session_evidence_uk UNIQUE (triage_session_id, evidence_type, content_hash)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_triage_completed_snapshot' AND conrelid = 'public.triage_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT chk_triage_completed_snapshot CHECK ((((status)::text <> ''COMPLETED''::text) OR ((NULLIF((schema_version)::text, ''''::text) IS NOT NULL) AND (NULLIF((content_hash)::text, ''''::text) IS NOT NULL) AND (jsonb_typeof(result_jsonb) = ''object''::text))))';
    ELSIF v_existing_def <> 'CHECK ((((status)::text <> ''COMPLETED''::text) OR ((NULLIF((schema_version)::text, ''''::text) IS NOT NULL) AND (NULLIF((content_hash)::text, ''''::text) IS NOT NULL) AND (jsonb_typeof(result_jsonb) = ''object''::text))))' THEN
        EXECUTE 'ALTER TABLE public.triage_sessions DROP CONSTRAINT chk_triage_completed_snapshot';
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT chk_triage_completed_snapshot CHECK ((((status)::text <> ''COMPLETED''::text) OR ((NULLIF((schema_version)::text, ''''::text) IS NOT NULL) AND (NULLIF((content_hash)::text, ''''::text) IS NOT NULL) AND (jsonb_typeof(result_jsonb) = ''object''::text))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_triage_lifecycle_binding' AND conrelid = 'public.triage_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT chk_triage_lifecycle_binding CHECK ((((journey_id IS NULL) AND (origin_dashboard IS NULL) AND (origin_reference_id IS NULL) AND (continuation_token IS NULL) AND (continuation_expires_at IS NULL) AND (continuation_acknowledged_at IS NULL)) OR ((journey_id IS NOT NULL) AND (origin_dashboard IS NOT NULL) AND (origin_reference_id IS NOT NULL) AND (continuation_token IS NOT NULL) AND (continuation_expires_at IS NOT NULL))))';
    ELSIF v_existing_def <> 'CHECK ((((journey_id IS NULL) AND (origin_dashboard IS NULL) AND (origin_reference_id IS NULL) AND (continuation_token IS NULL) AND (continuation_expires_at IS NULL) AND (continuation_acknowledged_at IS NULL)) OR ((journey_id IS NOT NULL) AND (origin_dashboard IS NOT NULL) AND (origin_reference_id IS NOT NULL) AND (continuation_token IS NOT NULL) AND (continuation_expires_at IS NOT NULL))))' THEN
        EXECUTE 'ALTER TABLE public.triage_sessions DROP CONSTRAINT chk_triage_lifecycle_binding';
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT chk_triage_lifecycle_binding CHECK ((((journey_id IS NULL) AND (origin_dashboard IS NULL) AND (origin_reference_id IS NULL) AND (continuation_token IS NULL) AND (continuation_expires_at IS NULL) AND (continuation_acknowledged_at IS NULL)) OR ((journey_id IS NOT NULL) AND (origin_dashboard IS NOT NULL) AND (origin_reference_id IS NOT NULL) AND (continuation_token IS NOT NULL) AND (continuation_expires_at IS NOT NULL))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_triage_origin_dashboard' AND conrelid = 'public.triage_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT chk_triage_origin_dashboard CHECK (((origin_dashboard IS NULL) OR ((origin_dashboard)::text = ANY (ARRAY[(''MOTHER_JOURNEY''::character varying)::text, (''BABY_PROFILE''::character varying)::text]))))';
    ELSIF v_existing_def <> 'CHECK (((origin_dashboard IS NULL) OR ((origin_dashboard)::text = ANY (ARRAY[(''MOTHER_JOURNEY''::character varying)::text, (''BABY_PROFILE''::character varying)::text]))))' THEN
        EXECUTE 'ALTER TABLE public.triage_sessions DROP CONSTRAINT chk_triage_origin_dashboard';
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT chk_triage_origin_dashboard CHECK (((origin_dashboard IS NULL) OR ((origin_dashboard)::text = ANY (ARRAY[(''MOTHER_JOURNEY''::character varying)::text, (''BABY_PROFILE''::character varying)::text]))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_triage_origin_stage' AND conrelid = 'public.triage_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT chk_triage_origin_stage CHECK (((origin_dashboard IS NULL) OR (((origin_dashboard)::text = ''MOTHER_JOURNEY''::text) AND (origin_reference_id = journey_id) AND ((stage)::text = ANY (ARRAY[(''PRECONCEPTION''::character varying)::text, (''PREGNANCY''::character varying)::text, (''POSTPARTUM''::character varying)::text]))) OR (((origin_dashboard)::text = ''BABY_PROFILE''::text) AND ((stage)::text = ANY (ARRAY[(''INFANT''::character varying)::text, (''TODDLER''::character varying)::text])))))';
    ELSIF v_existing_def <> 'CHECK (((origin_dashboard IS NULL) OR (((origin_dashboard)::text = ''MOTHER_JOURNEY''::text) AND (origin_reference_id = journey_id) AND ((stage)::text = ANY (ARRAY[(''PRECONCEPTION''::character varying)::text, (''PREGNANCY''::character varying)::text, (''POSTPARTUM''::character varying)::text]))) OR (((origin_dashboard)::text = ''BABY_PROFILE''::text) AND ((stage)::text = ANY (ARRAY[(''INFANT''::character varying)::text, (''TODDLER''::character varying)::text])))))' THEN
        EXECUTE 'ALTER TABLE public.triage_sessions DROP CONSTRAINT chk_triage_origin_stage';
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT chk_triage_origin_stage CHECK (((origin_dashboard IS NULL) OR (((origin_dashboard)::text = ''MOTHER_JOURNEY''::text) AND (origin_reference_id = journey_id) AND ((stage)::text = ANY (ARRAY[(''PRECONCEPTION''::character varying)::text, (''PREGNANCY''::character varying)::text, (''POSTPARTUM''::character varying)::text]))) OR (((origin_dashboard)::text = ''BABY_PROFILE''::text) AND ((stage)::text = ANY (ARRAY[(''INFANT''::character varying)::text, (''TODDLER''::character varying)::text])))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'chk_triage_red_emergency' AND conrelid = 'public.triage_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT chk_triage_red_emergency CHECK ((((risk_level)::text <> ''RED''::text) OR emergency))';
    ELSIF v_existing_def <> 'CHECK ((((risk_level)::text <> ''RED''::text) OR emergency))' THEN
        EXECUTE 'ALTER TABLE public.triage_sessions DROP CONSTRAINT chk_triage_red_emergency';
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT chk_triage_red_emergency CHECK ((((risk_level)::text <> ''RED''::text) OR emergency))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'triage_sessions_intensity_ck' AND conrelid = 'public.triage_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT triage_sessions_intensity_ck CHECK (((intensity IS NULL) OR ((intensity)::text = ANY (ARRAY[(''LOW''::character varying)::text, (''MEDIUM''::character varying)::text, (''HIGH''::character varying)::text]))))';
    ELSIF v_existing_def <> 'CHECK (((intensity IS NULL) OR ((intensity)::text = ANY (ARRAY[(''LOW''::character varying)::text, (''MEDIUM''::character varying)::text, (''HIGH''::character varying)::text]))))' THEN
        EXECUTE 'ALTER TABLE public.triage_sessions DROP CONSTRAINT triage_sessions_intensity_ck';
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT triage_sessions_intensity_ck CHECK (((intensity IS NULL) OR ((intensity)::text = ANY (ARRAY[(''LOW''::character varying)::text, (''MEDIUM''::character varying)::text, (''HIGH''::character varying)::text]))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'uk6dotkott2kjsp8vw4d0m25fb7' AND conrelid = 'public.users'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email)';
    ELSIF v_existing_def <> 'UNIQUE (email)' THEN
        EXECUTE 'ALTER TABLE public.users DROP CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7';
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'users_person_uk' AND conrelid = 'public.users'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT users_person_uk UNIQUE (person_id)';
    ELSIF v_existing_def <> 'UNIQUE (person_id)' THEN
        EXECUTE 'ALTER TABLE public.users DROP CONSTRAINT users_person_uk';
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT users_person_uk UNIQUE (person_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'users_role_check' AND conrelid = 'public.users'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT users_role_check CHECK (((role IS NULL) OR ((role)::text = ANY (ARRAY[(''MOTHER''::character varying)::text, (''FAMILY''::character varying)::text, (''EXPERT''::character varying)::text, (''MODERATOR''::character varying)::text, (''CONTENT_ADMIN''::character varying)::text, (''SYSTEM_ADMIN''::character varying)::text, (''PARTNER''::character varying)::text]))))';
    ELSIF v_existing_def <> 'CHECK (((role IS NULL) OR ((role)::text = ANY (ARRAY[(''MOTHER''::character varying)::text, (''FAMILY''::character varying)::text, (''EXPERT''::character varying)::text, (''MODERATOR''::character varying)::text, (''CONTENT_ADMIN''::character varying)::text, (''SYSTEM_ADMIN''::character varying)::text, (''PARTNER''::character varying)::text]))))' THEN
        EXECUTE 'ALTER TABLE public.users DROP CONSTRAINT users_role_check';
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT users_role_check CHECK (((role IS NULL) OR ((role)::text = ANY (ARRAY[(''MOTHER''::character varying)::text, (''FAMILY''::character varying)::text, (''EXPERT''::character varying)::text, (''MODERATOR''::character varying)::text, (''CONTENT_ADMIN''::character varying)::text, (''SYSTEM_ADMIN''::character varying)::text, (''PARTNER''::character varying)::text]))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'users_settings_jsonb_object_ck' AND conrelid = 'public.users'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT users_settings_jsonb_object_ck CHECK ((jsonb_typeof(settings_jsonb) = ''object''::text))';
    ELSIF v_existing_def <> 'CHECK ((jsonb_typeof(settings_jsonb) = ''object''::text))' THEN
        EXECUTE 'ALTER TABLE public.users DROP CONSTRAINT users_settings_jsonb_object_ck';
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT users_settings_jsonb_object_ck CHECK ((jsonb_typeof(settings_jsonb) = ''object''::text))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'vaccination_schedules_key_uk' AND conrelid = 'public.vaccination_schedules'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.vaccination_schedules ADD CONSTRAINT vaccination_schedules_key_uk UNIQUE (vaccine_name, dose_number, schedule_version)';
    ELSIF v_existing_def <> 'UNIQUE (vaccine_name, dose_number, schedule_version)' THEN
        EXECUTE 'ALTER TABLE public.vaccination_schedules DROP CONSTRAINT vaccination_schedules_key_uk';
        EXECUTE 'ALTER TABLE public.vaccination_schedules ADD CONSTRAINT vaccination_schedules_key_uk UNIQUE (vaccine_name, dose_number, schedule_version)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'question_notification_mutes_user_question_unique' AND conrelid = 'public.community_interactions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT question_notification_mutes_user_question_unique UNIQUE (actor_user_id, interaction_type, content_id)';
    ELSIF v_existing_def <> 'UNIQUE (actor_user_id, interaction_type, content_id)' THEN
        EXECUTE 'ALTER TABLE public.community_interactions DROP CONSTRAINT question_notification_mutes_user_question_unique';
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT question_notification_mutes_user_question_unique UNIQUE (actor_user_id, interaction_type, content_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'uq_question_like' AND conrelid = 'public.community_interactions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT uq_question_like UNIQUE (actor_user_id, interaction_type, content_id)';
    ELSIF v_existing_def <> 'UNIQUE (actor_user_id, interaction_type, content_id)' THEN
        EXECUTE 'ALTER TABLE public.community_interactions DROP CONSTRAINT uq_question_like';
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT uq_question_like UNIQUE (actor_user_id, interaction_type, content_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'uq_user_topic_follow' AND conrelid = 'public.community_interactions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT uq_user_topic_follow UNIQUE (actor_user_id, interaction_type, topic_id)';
    ELSIF v_existing_def <> 'UNIQUE (actor_user_id, interaction_type, topic_id)' THEN
        EXECUTE 'ALTER TABLE public.community_interactions DROP CONSTRAINT uq_user_topic_follow';
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT uq_user_topic_follow UNIQUE (actor_user_id, interaction_type, topic_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'health_observations_legacy_uk' AND conrelid = 'public.health_observations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_legacy_uk UNIQUE (legacy_source, legacy_id)';
    ELSIF v_existing_def <> 'UNIQUE (legacy_source, legacy_id)' THEN
        EXECUTE 'ALTER TABLE public.health_observations DROP CONSTRAINT health_observations_legacy_uk';
        EXECUTE 'ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_legacy_uk UNIQUE (legacy_source, legacy_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'health_observations_type_ck' AND conrelid = 'public.health_observations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_type_ck CHECK (((subject_type)::text = ANY ((ARRAY[''MOTHER''::character varying, ''BABY''::character varying, ''DEPENDENT''::character varying])::text[])))';
    ELSIF v_existing_def <> 'CHECK (((subject_type)::text = ANY ((ARRAY[''MOTHER''::character varying, ''BABY''::character varying, ''DEPENDENT''::character varying])::text[])))' THEN
        EXECUTE 'ALTER TABLE public.health_observations DROP CONSTRAINT health_observations_type_ck';
        EXECUTE 'ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_type_ck CHECK (((subject_type)::text = ANY ((ARRAY[''MOTHER''::character varying, ''BABY''::character varying, ''DEPENDENT''::character varying])::text[])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_action_type_ck' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_action_type_ck CHECK (((action_type IS NULL) OR ((action_type)::text = ANY ((ARRAY[''RESPONSE''::character varying, ''DELIVERY''::character varying, ''FAMILY_ALERT''::character varying, ''ALERT_ATTEMPT''::character varying, ''MAP_HANDOFF''::character varying, ''LOCATION_SNAPSHOT''::character varying])::text[]))))';
    ELSIF v_existing_def <> 'CHECK (((action_type IS NULL) OR ((action_type)::text = ANY ((ARRAY[''RESPONSE''::character varying, ''DELIVERY''::character varying, ''FAMILY_ALERT''::character varying, ''ALERT_ATTEMPT''::character varying, ''MAP_HANDOFF''::character varying, ''LOCATION_SNAPSHOT''::character varying])::text[]))))' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_action_type_ck';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_action_type_ck CHECK (((action_type IS NULL) OR ((action_type)::text = ANY ((ARRAY[''RESPONSE''::character varying, ''DELIVERY''::character varying, ''FAMILY_ALERT''::character varying, ''ALERT_ATTEMPT''::character varying, ''MAP_HANDOFF''::character varying, ''LOCATION_SNAPSHOT''::character varying])::text[]))))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_attempt_ck' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_attempt_ck CHECK (((attempt_number IS NULL) OR (attempt_number >= 0)))';
    ELSIF v_existing_def <> 'CHECK (((attempt_number IS NULL) OR (attempt_number >= 0)))' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_attempt_ck';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_attempt_ck CHECK (((attempt_number IS NULL) OR (attempt_number >= 0)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_idempotency_key_key' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_idempotency_key_key UNIQUE (idempotency_key)';
    ELSIF v_existing_def <> 'UNIQUE (idempotency_key)' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_idempotency_key_key';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_idempotency_key_key UNIQUE (idempotency_key)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_parent_ck' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_parent_ck CHECK (((action_type IS NULL) OR ((action_type)::text = ANY ((ARRAY[''MAP_HANDOFF''::character varying, ''LOCATION_SNAPSHOT''::character varying])::text[])) OR (parent_event_id IS NOT NULL)))';
    ELSIF v_existing_def <> 'CHECK (((action_type IS NULL) OR ((action_type)::text = ANY ((ARRAY[''MAP_HANDOFF''::character varying, ''LOCATION_SNAPSHOT''::character varying])::text[])) OR (parent_event_id IS NOT NULL)))' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_parent_ck';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_parent_ck CHECK (((action_type IS NULL) OR ((action_type)::text = ANY ((ARRAY[''MAP_HANDOFF''::character varying, ''LOCATION_SNAPSHOT''::character varying])::text[])) OR (parent_event_id IS NOT NULL)))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'triage_sessions_origin_dashboard_check' AND conrelid = 'public.triage_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT triage_sessions_origin_dashboard_check CHECK (((origin_dashboard)::text = ANY ((ARRAY[''MOTHER_JOURNEY''::character varying, ''BABY_PROFILE''::character varying])::text[])))';
    ELSIF v_existing_def <> 'CHECK (((origin_dashboard)::text = ANY ((ARRAY[''MOTHER_JOURNEY''::character varying, ''BABY_PROFILE''::character varying])::text[])))' THEN
        EXECUTE 'ALTER TABLE public.triage_sessions DROP CONSTRAINT triage_sessions_origin_dashboard_check';
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT triage_sessions_origin_dashboard_check CHECK (((origin_dashboard)::text = ANY ((ARRAY[''MOTHER_JOURNEY''::character varying, ''BABY_PROFILE''::character varying])::text[])))';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'uk97ih1g5lcdf1s3fg7oo4e18jw' AND conrelid = 'public.users'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT uk97ih1g5lcdf1s3fg7oo4e18jw UNIQUE (person_id)';
    ELSIF v_existing_def <> 'UNIQUE (person_id)' THEN
        EXECUTE 'ALTER TABLE public.users DROP CONSTRAINT uk97ih1g5lcdf1s3fg7oo4e18jw';
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT uk97ih1g5lcdf1s3fg7oo4e18jw UNIQUE (person_id)';
    END IF;
END
$canonical_constraints$;


-- ============================================================================

-- ============================================================================
-- 3. Indexes
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_account_deletion_requests_scheduled_for ON public.account_deletion_requests USING btree (scheduled_for) WHERE ((status)::text = 'PENDING'::text);
CREATE INDEX IF NOT EXISTS idx_account_deletion_requests_status ON public.account_deletion_requests USING btree (status);
CREATE INDEX IF NOT EXISTS idx_account_deletion_requests_user_id ON public.account_deletion_requests USING btree (user_id);
CREATE INDEX IF NOT EXISTS ai_content_assessments_case_ix ON public.ai_content_assessments USING btree (moderation_case_id);
CREATE UNIQUE INDEX IF NOT EXISTS ai_content_assessments_completed_uq ON public.ai_content_assessments USING btree (target_type, target_id, content_hash, policy_set_hash, model) WHERE ((status)::text = 'COMPLETED'::text);
CREATE INDEX IF NOT EXISTS ai_content_assessments_target_ix ON public.ai_content_assessments USING btree (target_type, target_id, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS ai_content_scan_jobs_active_uq ON public.ai_content_scan_jobs USING btree (target_type, target_id, content_hash) WHERE ((status)::text = ANY (ARRAY[('QUEUED'::character varying)::text, ('PROCESSING'::character varying)::text]));
CREATE INDEX IF NOT EXISTS ai_content_scan_jobs_claim_ix ON public.ai_content_scan_jobs USING btree (status, next_attempt_at);
CREATE INDEX IF NOT EXISTS ai_content_scan_jobs_target_ix ON public.ai_content_scan_jobs USING btree (target_type, target_id, status);
CREATE INDEX IF NOT EXISTS ai_moderation_policies_active_ix ON public.ai_moderation_policies USING btree (active, violation_category);
CREATE INDEX IF NOT EXISTS attachments_owner_category_review_ix ON public.attachments USING btree (owner_user_id, attachment_category, review_status);
CREATE INDEX IF NOT EXISTS audit_events_category_time_ix ON public.audit_events USING btree (event_category, occurred_at);
CREATE INDEX IF NOT EXISTS audit_events_origin_time_ix ON public.audit_events USING btree (event_origin, occurred_at DESC);
CREATE INDEX IF NOT EXISTS audit_events_subject_time_ix ON public.audit_events USING btree (subject_user_id, occurred_at);
CREATE INDEX IF NOT EXISTS auth_challenges_subject_expiry_ix ON public.auth_challenges USING btree (subject_identifier, challenge_type, expires_at);
CREATE INDEX IF NOT EXISTS auth_sessions_family_ix ON public.auth_sessions USING btree (token_family_id);
CREATE INDEX IF NOT EXISTS auth_sessions_user_device_ix ON public.auth_sessions USING btree (user_id, device_identifier, status);
CREATE INDEX IF NOT EXISTS care_facilities_area_ix ON public.care_facilities USING btree (administrative_area_id);
CREATE INDEX IF NOT EXISTS idx_care_facilities_facility_type ON public.care_facilities USING btree (facility_type);
CREATE INDEX IF NOT EXISTS idx_care_facilities_nearby_eligible ON public.care_facilities USING btree (facility_type, province_id, district_id) WHERE ((is_active = true) AND (is_searchable = true) AND (latitude IS NOT NULL) AND (longitude IS NOT NULL));
CREATE INDEX IF NOT EXISTS idx_care_facilities_partner_id ON public.care_facilities USING btree (partner_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_care_facilities_external_source ON public.care_facilities USING btree (source_type, external_source_id) WHERE (external_source_id IS NOT NULL);
CREATE INDEX IF NOT EXISTS idx_care_group_members_care_group_id ON public.care_group_members USING btree (care_group_id);
CREATE INDEX IF NOT EXISTS idx_care_group_members_user_id ON public.care_group_members USING btree (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_care_group_members_invite_token ON public.care_group_members USING btree (invite_token) WHERE (invite_token IS NOT NULL);
CREATE INDEX IF NOT EXISTS idx_care_groups_owner_user_id ON public.care_groups USING btree (owner_user_id);
CREATE INDEX IF NOT EXISTS care_item_templates_content_status_ix ON public.care_item_templates USING btree (entry_type, content_status, stage, display_order);
CREATE INDEX IF NOT EXISTS care_item_templates_exercise_filter_ix ON public.care_item_templates USING btree (template_status, stage, difficulty_level, created_at DESC) WHERE ((entry_type)::text = 'EXERCISE_TEMPLATE'::text);
CREATE INDEX IF NOT EXISTS care_item_templates_parent_order_ix ON public.care_item_templates USING btree (parent_template_id, display_order);
CREATE INDEX IF NOT EXISTS care_item_templates_posture_version_ix ON public.care_item_templates USING btree (parent_template_id, template_status, effective_from DESC) WHERE ((entry_type)::text = 'POSTURE_CONFIG'::text);
CREATE INDEX IF NOT EXISTS care_tasks_assignee_status_ix ON public.care_tasks USING btree (assignee_user_id, status, scheduled_at) WHERE (assignee_user_id IS NOT NULL);
CREATE INDEX IF NOT EXISTS care_tasks_owner_status_ix ON public.care_tasks USING btree (owner_user_id, status, scheduled_at) WHERE (owner_user_id IS NOT NULL);
CREATE INDEX IF NOT EXISTS community_content_parent_ix ON public.community_content USING btree (parent_content_id);
CREATE INDEX IF NOT EXISTS community_content_topic_ix ON public.community_content USING btree (topic_id, created_at);
CREATE UNIQUE INDEX IF NOT EXISTS community_interactions_content_target_uk ON public.community_interactions USING btree (actor_user_id, interaction_type, content_id) WHERE (content_id IS NOT NULL);
CREATE UNIQUE INDEX IF NOT EXISTS community_interactions_topic_target_uk ON public.community_interactions USING btree (actor_user_id, interaction_type, topic_id) WHERE (topic_id IS NOT NULL);
CREATE INDEX IF NOT EXISTS idx_community_topics_hidden ON public.community_topics USING btree (is_hidden);
CREATE UNIQUE INDEX IF NOT EXISTS idx_community_topics_name_lower ON public.community_topics USING btree (lower((name)::text));
CREATE INDEX IF NOT EXISTS idx_community_topics_parent_id ON public.community_topics USING btree (parent_id);
CREATE INDEX IF NOT EXISTS idx_community_topics_sort_order ON public.community_topics USING btree (sort_order);
CREATE INDEX IF NOT EXISTS idx_community_topics_type ON public.community_topics USING btree (type);
CREATE INDEX IF NOT EXISTS consultation_bookings_requester_ix ON public.consultation_bookings USING btree (requester_user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_context_citations_share_ordinal ON public.consultation_context_citations USING btree (context_share_id, ordinal, citation_snapshot_id);
CREATE INDEX IF NOT EXISTS idx_context_shares_expert_created ON public.consultation_context_shares USING btree (expert_profile_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_context_shares_owner_created ON public.consultation_context_shares USING btree (owner_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_context_shares_participant_request ON public.consultation_context_shares USING btree (consultation_request_id, owner_user_id, expert_profile_id);
CREATE INDEX IF NOT EXISTS idx_content_items_published_at ON public.content_items USING btree (published_at DESC NULLS LAST) WHERE ((status)::text = 'APPROVED'::text);
CREATE INDEX IF NOT EXISTS idx_content_items_stage ON public.content_items USING btree (stage);
CREATE INDEX IF NOT EXISTS idx_content_items_stage_status ON public.content_items USING btree (stage, status);
CREATE INDEX IF NOT EXISTS idx_content_items_stage_type_approved ON public.content_items USING btree (stage, content_type, status) WHERE ((status)::text = 'APPROVED'::text);
CREATE INDEX IF NOT EXISTS idx_content_items_status ON public.content_items USING btree (status);
CREATE INDEX IF NOT EXISTS idx_content_items_title_search ON public.content_items USING btree (lower((title)::text));
CREATE INDEX IF NOT EXISTS idx_content_items_type ON public.content_items USING btree (content_type);
CREATE INDEX IF NOT EXISTS idx_content_items_type_status ON public.content_items USING btree (content_type, status);
CREATE INDEX IF NOT EXISTS conversation_calls_timeline_ix ON public.conversation_calls USING btree (conversation_id, initiated_at DESC);
CREATE INDEX IF NOT EXISTS data_permissions_consent_owner_ix ON public.data_permissions USING btree (owner_user_id, granted_at DESC) WHERE ((permission_kind)::text = 'CONSENT_GRANT'::text);
CREATE UNIQUE INDEX IF NOT EXISTS data_permissions_handoff_integrity_uk ON public.data_permissions USING btree (legacy_consent_id, owner_user_id, evidence_key);
CREATE UNIQUE INDEX IF NOT EXISTS data_permissions_legacy_consent_id_uk ON public.data_permissions USING btree (legacy_consent_id) WHERE (legacy_consent_id IS NOT NULL);
CREATE INDEX IF NOT EXISTS development_milestones_subject_ix ON public.development_milestones USING btree (care_subject_id, milestone_type, achieved_date);
CREATE INDEX IF NOT EXISTS idx_development_milestones_baby_id ON public.development_milestones USING btree (baby_id);
CREATE INDEX IF NOT EXISTS idx_development_milestones_baby_record_status ON public.development_milestones USING btree (baby_id, record_status);
CREATE INDEX IF NOT EXISTS device_connections_user_status_ix ON public.device_connections USING btree (user_id, status);
CREATE INDEX IF NOT EXISTS idx_device_tokens_user_id ON public.device_tokens USING btree (user_id);
CREATE INDEX IF NOT EXISTS direct_messages_timeline_ix ON public.direct_messages USING btree (conversation_id, created_at DESC);
CREATE INDEX IF NOT EXISTS expense_entries_owner_date_ix ON public.expense_entries USING btree (owner_user_id, expense_date);
CREATE INDEX IF NOT EXISTS expert_availability_profile_window_ix ON public.expert_availability USING btree (professional_profile_id, start_at, end_at);
CREATE INDEX IF NOT EXISTS idx_expert_availability_start_at ON public.expert_availability USING btree (start_at);
CREATE INDEX IF NOT EXISTS idx_expert_availability_status ON public.expert_availability USING btree (status);
CREATE INDEX IF NOT EXISTS expert_consultation_requests_expert_status_ix ON public.expert_consultation_requests USING btree (expert_profile_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS expert_consultation_requests_expiry_ix ON public.expert_consultation_requests USING btree (expires_at) WHERE ((status)::text = 'PENDING'::text);
CREATE UNIQUE INDEX IF NOT EXISTS expert_consultation_requests_integrity_uk ON public.expert_consultation_requests USING btree (id, requester_user_id, expert_profile_id, client_request_id);
CREATE INDEX IF NOT EXISTS expert_consultation_requests_owner_status_ix ON public.expert_consultation_requests USING btree (requester_user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS growth_measurements_chart_ix ON public.growth_measurements USING btree (care_subject_id, measured_date) WHERE (deleted_at IS NULL);
CREATE INDEX IF NOT EXISTS idx_growth_measurements_baby_id ON public.growth_measurements USING btree (baby_id);
CREATE INDEX IF NOT EXISTS health_context_memories_baby_ix ON public.health_context_memories USING btree (baby_profile_id, created_at DESC) WHERE (deleted_at IS NULL);
CREATE INDEX IF NOT EXISTS health_context_memories_mother_ix ON public.health_context_memories USING btree (mother_profile_id, created_at DESC) WHERE (deleted_at IS NULL);
CREATE INDEX IF NOT EXISTS health_context_memories_subject_expiry_ix ON public.health_context_memories USING btree (care_subject_id, expires_at) WHERE (deleted_at IS NULL);
CREATE INDEX IF NOT EXISTS health_observations_device_time_ix ON public.health_observations USING btree (device_connection_id, observed_at) WHERE (device_connection_id IS NOT NULL);
CREATE INDEX IF NOT EXISTS health_observations_subject_chart_ix ON public.health_observations USING btree (care_subject_id, observation_type, observed_at);
CREATE INDEX IF NOT EXISTS health_records_summary_filter_ix ON public.health_records USING btree (owner_user_id, summary_period, record_date DESC) WHERE (((record_type)::text = 'SUMMARY'::text) AND ((status)::text = 'ACTIVE'::text));
CREATE INDEX IF NOT EXISTS idx_health_records_baby_id ON public.health_records USING btree (baby_id);
CREATE INDEX IF NOT EXISTS idx_health_records_journey_id ON public.health_records USING btree (journey_id);
CREATE INDEX IF NOT EXISTS idx_health_records_owner_user_id ON public.health_records USING btree (owner_user_id);
CREATE INDEX IF NOT EXISTS knowledge_source_reviews_source_time_ix ON public.knowledge_source_reviews USING btree (knowledge_source_id, changed_at);
CREATE INDEX IF NOT EXISTS knowledge_sources_domain_status_ix ON public.knowledge_sources USING btree (domain, status);
CREATE UNIQUE INDEX IF NOT EXISTS knowledge_sources_domain_uk ON public.knowledge_sources USING btree (lower((domain)::text));
CREATE INDEX IF NOT EXISTS moderation_cases_priority_ix ON public.moderation_cases USING btree (status, priority, opened_at DESC);
CREATE INDEX IF NOT EXISTS moderation_cases_report_source_ix ON public.moderation_cases USING btree (report_source, status, opened_at DESC);
CREATE INDEX IF NOT EXISTS moderation_cases_target_ix ON public.moderation_cases USING btree (target_type, target_id, status);
CREATE INDEX IF NOT EXISTS idx_mother_journeys_owner_user_id ON public.mother_journeys USING btree (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_mother_journeys_status ON public.mother_journeys USING btree (status);
CREATE UNIQUE INDEX IF NOT EXISTS mother_journeys_handoff_owner_uk ON public.mother_journeys USING btree (journey_id, owner_user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mother_journeys_id_owner ON public.mother_journeys USING btree (journey_id, owner_user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mother_journeys_one_canonical_active ON public.mother_journeys USING btree (owner_user_id) WHERE (((status)::text = 'ACTIVE'::text) AND ((journey_type)::text = ANY ((ARRAY['PRE_PREGNANCY'::character varying, 'PREGNANCY'::character varying, 'POSTPARTUM'::character varying])::text[])));
CREATE INDEX IF NOT EXISTS idx_notification_records_type_status ON public.notification_records USING btree (type, status);
CREATE INDEX IF NOT EXISTS idx_notification_records_user_id ON public.notification_records USING btree (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_records_user_unread ON public.notification_records USING btree (user_id, is_read) WHERE (is_read = false);
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_records_consultation_request ON public.notification_records USING btree (user_id, reference_id, ((metadata ->> 'eventType'::text))) WHERE (((type)::text = 'CONSULTATION'::text) AND ((reference_type)::text = 'CONSULTATION_REQUEST'::text));
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_records_direct_message ON public.notification_records USING btree (user_id, reference_id) WHERE (((type)::text = 'MESSAGE'::text) AND ((reference_type)::text = 'DIRECT_MESSAGE'::text));
CREATE INDEX IF NOT EXISTS preparation_checklist_filter_ix ON public.preparation_checklist_items USING btree (owner_user_id, mother_journey_id, baby_id, status, display_order);
CREATE INDEX IF NOT EXISTS preparation_checklist_owner_journey_ix ON public.preparation_checklist_items USING btree (owner_user_id, mother_journey_id, display_order);
CREATE UNIQUE INDEX IF NOT EXISTS uq_preparation_checklist_baby_import_scope ON public.preparation_checklist_items USING btree (owner_user_id, baby_id, template_entry_id) WHERE ((baby_id IS NOT NULL) AND (template_entry_id IS NOT NULL));
CREATE UNIQUE INDEX IF NOT EXISTS uq_preparation_checklist_journey_import_scope ON public.preparation_checklist_items USING btree (owner_user_id, mother_journey_id, template_entry_id) NULLS NOT DISTINCT WHERE ((baby_id IS NULL) AND (template_entry_id IS NOT NULL));
CREATE INDEX IF NOT EXISTS professional_specialties_specialty_ix ON public.professional_specialties USING btree (specialty_id);
CREATE INDEX IF NOT EXISTS idx_red_flag_rules_active_severity ON public.red_flag_rules USING btree (is_active, severity);
CREATE INDEX IF NOT EXISTS idx_red_flag_rules_is_system_default ON public.red_flag_rules USING btree (is_system_default);
CREATE INDEX IF NOT EXISTS safety_events_alert_retry_ix ON public.safety_events USING btree (alert_status, alert_updated_at, alert_lease_expires_at) WHERE (((record_type)::text = 'EMERGENCY_SESSION'::text) AND ((status)::text = 'ACTIVE'::text));
CREATE UNIQUE INDEX IF NOT EXISTS safety_events_event_owner_uk ON public.safety_events USING btree (safety_event_id, user_id);
CREATE UNIQUE INDEX IF NOT EXISTS safety_events_imu_signal_uk ON public.safety_events USING btree (monitoring_session_id, signal_key) WHERE (((record_type)::text = 'IMU_EVENT'::text) AND (signal_key IS NOT NULL));
CREATE UNIQUE INDEX IF NOT EXISTS safety_events_one_active_emergency_user_uk ON public.safety_events USING btree (user_id) WHERE (((record_type)::text = 'EMERGENCY_SESSION'::text) AND ((status)::text = 'ACTIVE'::text));
CREATE INDEX IF NOT EXISTS safety_events_pending_countdown_ix ON public.safety_events USING btree (countdown_deadline_at) WHERE (((record_type)::text = 'IMU_EVENT'::text) AND ((status)::text = 'OPEN'::text) AND (response_type IS NULL));
CREATE INDEX IF NOT EXISTS safety_events_user_status_time_ix ON public.safety_events USING btree (user_id, status, detected_at);
CREATE UNIQUE INDEX IF NOT EXISTS safety_monitoring_sessions_one_active_user_uk ON public.safety_monitoring_sessions USING btree (user_id) WHERE ((status)::text = 'ACTIVE'::text);
CREATE INDEX IF NOT EXISTS safety_monitoring_sessions_user_status_ix ON public.safety_monitoring_sessions USING btree (user_id, status);
CREATE INDEX IF NOT EXISTS triage_session_evidence_session_ix ON public.triage_session_evidence USING btree (triage_session_id);
CREATE INDEX IF NOT EXISTS idx_triage_sessions_journey ON public.triage_sessions USING btree (journey_id, completed_at DESC) WHERE (journey_id IS NOT NULL);
CREATE UNIQUE INDEX IF NOT EXISTS triage_sessions_handoff_integrity_uk ON public.triage_sessions USING btree (triage_session_id, user_id, journey_id, origin_dashboard, origin_reference_id, stage, risk_level, status);
CREATE UNIQUE INDEX IF NOT EXISTS triage_sessions_owner_request_uk ON public.triage_sessions USING btree (user_id, client_request_id) WHERE (client_request_id IS NOT NULL);
CREATE INDEX IF NOT EXISTS triage_sessions_risk_ix ON public.triage_sessions USING btree (risk_level, emergency, created_at);
CREATE UNIQUE INDEX IF NOT EXISTS triage_sessions_session_owner_uk ON public.triage_sessions USING btree (triage_session_id, user_id);
CREATE INDEX IF NOT EXISTS triage_sessions_stage_ix ON public.triage_sessions USING btree (stage, created_at);
CREATE INDEX IF NOT EXISTS triage_sessions_user_time_ix ON public.triage_sessions USING btree (user_id, created_at);
CREATE UNIQUE INDEX IF NOT EXISTS uq_triage_sessions_continuation_token ON public.triage_sessions USING btree (continuation_token) WHERE (continuation_token IS NOT NULL);
CREATE INDEX IF NOT EXISTS idx_vaccination_records_baby_id ON public.vaccination_records USING btree (baby_id);
CREATE INDEX IF NOT EXISTS idx_vaccination_records_status ON public.vaccination_records USING btree (status);
CREATE INDEX IF NOT EXISTS vaccination_records_subject_status_ix ON public.vaccination_records USING btree (care_subject_id, status, scheduled_date);
CREATE INDEX IF NOT EXISTS audit_events_security_note_ix ON public.audit_events USING btree (security_event_id, occurred_at) WHERE ((event_category)::text = 'SECURITY_INVESTIGATION_NOTE'::text);
CREATE INDEX IF NOT EXISTS care_tasks_context_ix ON public.care_tasks USING btree (owner_user_id, journey_id, baby_id, status, scheduled_at) WHERE (owner_user_id IS NOT NULL);
CREATE INDEX IF NOT EXISTS community_content_author_ix ON public.community_content USING btree (author_user_id);
CREATE INDEX IF NOT EXISTS community_content_stage_ix ON public.community_content USING btree (stage);
CREATE INDEX IF NOT EXISTS community_content_status_ix ON public.community_content USING btree (moderation_status);
CREATE INDEX IF NOT EXISTS community_interactions_actor_ix ON public.community_interactions USING btree (actor_user_id);
CREATE INDEX IF NOT EXISTS community_interactions_content_ix ON public.community_interactions USING btree (content_id);
CREATE INDEX IF NOT EXISTS community_interactions_topic_ix ON public.community_interactions USING btree (topic_id);
CREATE INDEX IF NOT EXISTS expert_availability_user_window_ix ON public.expert_availability USING btree (user_id, start_at, end_at);
CREATE INDEX IF NOT EXISTS health_observations_severity_ix ON public.health_observations USING btree (severity, observed_at) WHERE (severity IS NOT NULL);
CREATE UNIQUE INDEX IF NOT EXISTS uq_preparation_checklist_import_scope ON public.preparation_checklist_items USING btree (owner_user_id, mother_journey_id, baby_id, template_entry_id) NULLS NOT DISTINCT WHERE (template_entry_id IS NOT NULL);
CREATE UNIQUE INDEX IF NOT EXISTS safety_events_attempt_event_uk ON public.safety_events USING btree (parent_event_id) WHERE ((action_type)::text = 'ALERT_ATTEMPT'::text);
CREATE UNIQUE INDEX IF NOT EXISTS safety_events_delivery_token_uk ON public.safety_events USING btree (parent_event_id, device_token_id) WHERE ((action_type)::text = 'DELIVERY'::text);
CREATE UNIQUE INDEX IF NOT EXISTS safety_events_family_alert_uk ON public.safety_events USING btree (parent_event_id) WHERE ((action_type)::text = 'FAMILY_ALERT'::text);
CREATE INDEX IF NOT EXISTS safety_events_handoff_status_ix ON public.safety_events USING btree (action_status, created_at DESC) WHERE ((action_type)::text = 'MAP_HANDOFF'::text);
CREATE INDEX IF NOT EXISTS safety_events_owner_location_ix ON public.safety_events USING btree (owner_user_id, captured_at DESC) WHERE ((action_type)::text = 'LOCATION_SNAPSHOT'::text);
CREATE INDEX IF NOT EXISTS safety_events_parent_ix ON public.safety_events USING btree (parent_event_id);


-- ============================================================================

-- ============================================================================
-- 4. Foreign keys
-- ============================================================================
DO $canonical_foreign_keys$
DECLARE v_existing_def text;
BEGIN
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'account_deletion_requests_processed_by_fkey' AND conrelid = 'public.account_deletion_requests'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.account_deletion_requests ADD CONSTRAINT account_deletion_requests_processed_by_fkey FOREIGN KEY (processed_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (processed_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.account_deletion_requests DROP CONSTRAINT account_deletion_requests_processed_by_fkey';
        EXECUTE 'ALTER TABLE public.account_deletion_requests ADD CONSTRAINT account_deletion_requests_processed_by_fkey FOREIGN KEY (processed_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'account_deletion_requests_user_id_fkey' AND conrelid = 'public.account_deletion_requests'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.account_deletion_requests ADD CONSTRAINT account_deletion_requests_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.account_deletion_requests DROP CONSTRAINT account_deletion_requests_user_id_fkey';
        EXECUTE 'ALTER TABLE public.account_deletion_requests ADD CONSTRAINT account_deletion_requests_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'administrative_areas_parent_area_id_fkey' AND conrelid = 'public.administrative_areas'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.administrative_areas ADD CONSTRAINT administrative_areas_parent_area_id_fkey FOREIGN KEY (parent_area_id) REFERENCES administrative_areas(administrative_area_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (parent_area_id) REFERENCES administrative_areas(administrative_area_id)' THEN
        EXECUTE 'ALTER TABLE public.administrative_areas DROP CONSTRAINT administrative_areas_parent_area_id_fkey';
        EXECUTE 'ALTER TABLE public.administrative_areas ADD CONSTRAINT administrative_areas_parent_area_id_fkey FOREIGN KEY (parent_area_id) REFERENCES administrative_areas(administrative_area_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'ai_content_assessments_job_id_fkey' AND conrelid = 'public.ai_content_assessments'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.ai_content_assessments ADD CONSTRAINT ai_content_assessments_job_id_fkey FOREIGN KEY (job_id) REFERENCES ai_content_scan_jobs(job_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (job_id) REFERENCES ai_content_scan_jobs(job_id)' THEN
        EXECUTE 'ALTER TABLE public.ai_content_assessments DROP CONSTRAINT ai_content_assessments_job_id_fkey';
        EXECUTE 'ALTER TABLE public.ai_content_assessments ADD CONSTRAINT ai_content_assessments_job_id_fkey FOREIGN KEY (job_id) REFERENCES ai_content_scan_jobs(job_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'ai_content_assessments_moderation_case_id_fkey' AND conrelid = 'public.ai_content_assessments'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.ai_content_assessments ADD CONSTRAINT ai_content_assessments_moderation_case_id_fkey FOREIGN KEY (moderation_case_id) REFERENCES moderation_cases(moderation_case_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (moderation_case_id) REFERENCES moderation_cases(moderation_case_id)' THEN
        EXECUTE 'ALTER TABLE public.ai_content_assessments DROP CONSTRAINT ai_content_assessments_moderation_case_id_fkey';
        EXECUTE 'ALTER TABLE public.ai_content_assessments ADD CONSTRAINT ai_content_assessments_moderation_case_id_fkey FOREIGN KEY (moderation_case_id) REFERENCES moderation_cases(moderation_case_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'ai_moderation_policies_created_by_fkey' AND conrelid = 'public.ai_moderation_policies'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.ai_moderation_policies ADD CONSTRAINT ai_moderation_policies_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (created_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.ai_moderation_policies DROP CONSTRAINT ai_moderation_policies_created_by_fkey';
        EXECUTE 'ALTER TABLE public.ai_moderation_policies ADD CONSTRAINT ai_moderation_policies_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'ai_moderation_policies_updated_by_fkey' AND conrelid = 'public.ai_moderation_policies'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.ai_moderation_policies ADD CONSTRAINT ai_moderation_policies_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (updated_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.ai_moderation_policies DROP CONSTRAINT ai_moderation_policies_updated_by_fkey';
        EXECUTE 'ALTER TABLE public.ai_moderation_policies ADD CONSTRAINT ai_moderation_policies_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'archived_records_owner_user_id_fkey' AND conrelid = 'public.archived_records'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.archived_records ADD CONSTRAINT archived_records_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (owner_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.archived_records DROP CONSTRAINT archived_records_owner_user_id_fkey';
        EXECUTE 'ALTER TABLE public.archived_records ADD CONSTRAINT archived_records_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'attachments_owner_user_id_fkey' AND conrelid = 'public.attachments'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.attachments ADD CONSTRAINT attachments_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (owner_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.attachments DROP CONSTRAINT attachments_owner_user_id_fkey';
        EXECUTE 'ALTER TABLE public.attachments ADD CONSTRAINT attachments_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'attachments_reviewed_by_fkey' AND conrelid = 'public.attachments'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.attachments ADD CONSTRAINT attachments_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (reviewed_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.attachments DROP CONSTRAINT attachments_reviewed_by_fkey';
        EXECUTE 'ALTER TABLE public.attachments ADD CONSTRAINT attachments_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'audit_events_actor_user_id_fkey' AND conrelid = 'public.audit_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.audit_events ADD CONSTRAINT audit_events_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (actor_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.audit_events DROP CONSTRAINT audit_events_actor_user_id_fkey';
        EXECUTE 'ALTER TABLE public.audit_events ADD CONSTRAINT audit_events_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'audit_events_subject_user_id_fkey' AND conrelid = 'public.audit_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.audit_events ADD CONSTRAINT audit_events_subject_user_id_fkey FOREIGN KEY (subject_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (subject_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.audit_events DROP CONSTRAINT audit_events_subject_user_id_fkey';
        EXECUTE 'ALTER TABLE public.audit_events ADD CONSTRAINT audit_events_subject_user_id_fkey FOREIGN KEY (subject_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'auth_challenges_user_id_fkey' AND conrelid = 'public.auth_challenges'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.auth_challenges ADD CONSTRAINT auth_challenges_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.auth_challenges DROP CONSTRAINT auth_challenges_user_id_fkey';
        EXECUTE 'ALTER TABLE public.auth_challenges ADD CONSTRAINT auth_challenges_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'auth_sessions_user_id_fkey' AND conrelid = 'public.auth_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.auth_sessions ADD CONSTRAINT auth_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.auth_sessions DROP CONSTRAINT auth_sessions_user_id_fkey';
        EXECUTE 'ALTER TABLE public.auth_sessions ADD CONSTRAINT auth_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_group_members_care_group_id_fkey' AND conrelid = 'public.care_group_members'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_group_members ADD CONSTRAINT care_group_members_care_group_id_fkey FOREIGN KEY (care_group_id) REFERENCES care_groups(care_group_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (care_group_id) REFERENCES care_groups(care_group_id)' THEN
        EXECUTE 'ALTER TABLE public.care_group_members DROP CONSTRAINT care_group_members_care_group_id_fkey';
        EXECUTE 'ALTER TABLE public.care_group_members ADD CONSTRAINT care_group_members_care_group_id_fkey FOREIGN KEY (care_group_id) REFERENCES care_groups(care_group_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_group_members_user_id_fkey' AND conrelid = 'public.care_group_members'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_group_members ADD CONSTRAINT care_group_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.care_group_members DROP CONSTRAINT care_group_members_user_id_fkey';
        EXECUTE 'ALTER TABLE public.care_group_members ADD CONSTRAINT care_group_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_groups_baby_id_fkey' AND conrelid = 'public.care_groups'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_groups ADD CONSTRAINT care_groups_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.care_groups DROP CONSTRAINT care_groups_baby_id_fkey';
        EXECUTE 'ALTER TABLE public.care_groups ADD CONSTRAINT care_groups_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_groups_journey_id_fkey' AND conrelid = 'public.care_groups'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_groups ADD CONSTRAINT care_groups_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES mother_journeys(journey_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (journey_id) REFERENCES mother_journeys(journey_id)' THEN
        EXECUTE 'ALTER TABLE public.care_groups DROP CONSTRAINT care_groups_journey_id_fkey';
        EXECUTE 'ALTER TABLE public.care_groups ADD CONSTRAINT care_groups_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES mother_journeys(journey_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_groups_owner_user_id_fkey' AND conrelid = 'public.care_groups'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_groups ADD CONSTRAINT care_groups_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (owner_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.care_groups DROP CONSTRAINT care_groups_owner_user_id_fkey';
        EXECUTE 'ALTER TABLE public.care_groups ADD CONSTRAINT care_groups_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_item_templates_parent_template_id_fkey' AND conrelid = 'public.care_item_templates'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_item_templates ADD CONSTRAINT care_item_templates_parent_template_id_fkey FOREIGN KEY (parent_template_id) REFERENCES care_item_templates(template_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (parent_template_id) REFERENCES care_item_templates(template_id)' THEN
        EXECUTE 'ALTER TABLE public.care_item_templates DROP CONSTRAINT care_item_templates_parent_template_id_fkey';
        EXECUTE 'ALTER TABLE public.care_item_templates ADD CONSTRAINT care_item_templates_parent_template_id_fkey FOREIGN KEY (parent_template_id) REFERENCES care_item_templates(template_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_subjects_journey_fk' AND conrelid = 'public.care_subjects'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_subjects ADD CONSTRAINT care_subjects_journey_fk FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id)' THEN
        EXECUTE 'ALTER TABLE public.care_subjects DROP CONSTRAINT care_subjects_journey_fk';
        EXECUTE 'ALTER TABLE public.care_subjects ADD CONSTRAINT care_subjects_journey_fk FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_subjects_owner_user_id_fkey' AND conrelid = 'public.care_subjects'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_subjects ADD CONSTRAINT care_subjects_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (owner_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.care_subjects DROP CONSTRAINT care_subjects_owner_user_id_fkey';
        EXECUTE 'ALTER TABLE public.care_subjects ADD CONSTRAINT care_subjects_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_tasks_assignee_user_id_fkey' AND conrelid = 'public.care_tasks'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_assignee_user_id_fkey FOREIGN KEY (assignee_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (assignee_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.care_tasks DROP CONSTRAINT care_tasks_assignee_user_id_fkey';
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_assignee_user_id_fkey FOREIGN KEY (assignee_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_tasks_care_group_id_fkey' AND conrelid = 'public.care_tasks'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_care_group_id_fkey FOREIGN KEY (care_group_id) REFERENCES care_groups(care_group_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (care_group_id) REFERENCES care_groups(care_group_id)' THEN
        EXECUTE 'ALTER TABLE public.care_tasks DROP CONSTRAINT care_tasks_care_group_id_fkey';
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_care_group_id_fkey FOREIGN KEY (care_group_id) REFERENCES care_groups(care_group_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_tasks_care_subject_id_fkey' AND conrelid = 'public.care_tasks'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.care_tasks DROP CONSTRAINT care_tasks_care_subject_id_fkey';
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_tasks_creator_user_id_fkey' AND conrelid = 'public.care_tasks'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_creator_user_id_fkey FOREIGN KEY (creator_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (creator_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.care_tasks DROP CONSTRAINT care_tasks_creator_user_id_fkey';
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_creator_user_id_fkey FOREIGN KEY (creator_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_tasks_owner_user_id_fkey' AND conrelid = 'public.care_tasks'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (owner_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.care_tasks DROP CONSTRAINT care_tasks_owner_user_id_fkey';
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_tasks_vaccination_record_id_fkey' AND conrelid = 'public.care_tasks'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_vaccination_record_id_fkey FOREIGN KEY (vaccination_record_id) REFERENCES vaccination_records(vaccination_record_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (vaccination_record_id) REFERENCES vaccination_records(vaccination_record_id)' THEN
        EXECUTE 'ALTER TABLE public.care_tasks DROP CONSTRAINT care_tasks_vaccination_record_id_fkey';
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_vaccination_record_id_fkey FOREIGN KEY (vaccination_record_id) REFERENCES vaccination_records(vaccination_record_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'community_content_author_user_id_fkey' AND conrelid = 'public.community_content'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_content ADD CONSTRAINT community_content_author_user_id_fkey FOREIGN KEY (author_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (author_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.community_content DROP CONSTRAINT community_content_author_user_id_fkey';
        EXECUTE 'ALTER TABLE public.community_content ADD CONSTRAINT community_content_author_user_id_fkey FOREIGN KEY (author_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'community_content_parent_content_id_fkey' AND conrelid = 'public.community_content'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_content ADD CONSTRAINT community_content_parent_content_id_fkey FOREIGN KEY (parent_content_id) REFERENCES community_content(content_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (parent_content_id) REFERENCES community_content(content_id)' THEN
        EXECUTE 'ALTER TABLE public.community_content DROP CONSTRAINT community_content_parent_content_id_fkey';
        EXECUTE 'ALTER TABLE public.community_content ADD CONSTRAINT community_content_parent_content_id_fkey FOREIGN KEY (parent_content_id) REFERENCES community_content(content_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'community_content_topic_id_fkey' AND conrelid = 'public.community_content'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_content ADD CONSTRAINT community_content_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES community_topics(id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (topic_id) REFERENCES community_topics(id)' THEN
        EXECUTE 'ALTER TABLE public.community_content DROP CONSTRAINT community_content_topic_id_fkey';
        EXECUTE 'ALTER TABLE public.community_content ADD CONSTRAINT community_content_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES community_topics(id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'community_interactions_actor_user_id_fkey' AND conrelid = 'public.community_interactions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT community_interactions_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (actor_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.community_interactions DROP CONSTRAINT community_interactions_actor_user_id_fkey';
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT community_interactions_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'community_interactions_content_id_fkey' AND conrelid = 'public.community_interactions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT community_interactions_content_id_fkey FOREIGN KEY (content_id) REFERENCES community_content(content_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (content_id) REFERENCES community_content(content_id)' THEN
        EXECUTE 'ALTER TABLE public.community_interactions DROP CONSTRAINT community_interactions_content_id_fkey';
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT community_interactions_content_id_fkey FOREIGN KEY (content_id) REFERENCES community_content(content_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'community_interactions_topic_id_fkey' AND conrelid = 'public.community_interactions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT community_interactions_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES community_topics(id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (topic_id) REFERENCES community_topics(id)' THEN
        EXECUTE 'ALTER TABLE public.community_interactions DROP CONSTRAINT community_interactions_topic_id_fkey';
        EXECUTE 'ALTER TABLE public.community_interactions ADD CONSTRAINT community_interactions_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES community_topics(id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'fk_community_topics_parent' AND conrelid = 'public.community_topics'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.community_topics ADD CONSTRAINT fk_community_topics_parent FOREIGN KEY (parent_id) REFERENCES community_topics(id) ON DELETE RESTRICT';
    ELSIF v_existing_def <> 'FOREIGN KEY (parent_id) REFERENCES community_topics(id) ON DELETE RESTRICT' THEN
        EXECUTE 'ALTER TABLE public.community_topics DROP CONSTRAINT fk_community_topics_parent';
        EXECUTE 'ALTER TABLE public.community_topics ADD CONSTRAINT fk_community_topics_parent FOREIGN KEY (parent_id) REFERENCES community_topics(id) ON DELETE RESTRICT';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'consultation_bookings_availability_id_fkey' AND conrelid = 'public.consultation_bookings'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_bookings ADD CONSTRAINT consultation_bookings_availability_id_fkey FOREIGN KEY (availability_id) REFERENCES expert_availability(availability_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (availability_id) REFERENCES expert_availability(availability_id)' THEN
        EXECUTE 'ALTER TABLE public.consultation_bookings DROP CONSTRAINT consultation_bookings_availability_id_fkey';
        EXECUTE 'ALTER TABLE public.consultation_bookings ADD CONSTRAINT consultation_bookings_availability_id_fkey FOREIGN KEY (availability_id) REFERENCES expert_availability(availability_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'consultation_bookings_expert_profile_id_fkey' AND conrelid = 'public.consultation_bookings'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_bookings ADD CONSTRAINT consultation_bookings_expert_profile_id_fkey FOREIGN KEY (expert_profile_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (expert_profile_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.consultation_bookings DROP CONSTRAINT consultation_bookings_expert_profile_id_fkey';
        EXECUTE 'ALTER TABLE public.consultation_bookings ADD CONSTRAINT consultation_bookings_expert_profile_id_fkey FOREIGN KEY (expert_profile_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'consultation_bookings_requester_user_id_fkey' AND conrelid = 'public.consultation_bookings'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_bookings ADD CONSTRAINT consultation_bookings_requester_user_id_fkey FOREIGN KEY (requester_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (requester_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.consultation_bookings DROP CONSTRAINT consultation_bookings_requester_user_id_fkey';
        EXECUTE 'ALTER TABLE public.consultation_bookings ADD CONSTRAINT consultation_bookings_requester_user_id_fkey FOREIGN KEY (requester_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'fk_context_citation_share' AND conrelid = 'public.consultation_context_citations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_citations ADD CONSTRAINT fk_context_citation_share FOREIGN KEY (context_share_id) REFERENCES consultation_context_shares(context_share_id) ON DELETE RESTRICT';
    ELSIF v_existing_def <> 'FOREIGN KEY (context_share_id) REFERENCES consultation_context_shares(context_share_id) ON DELETE RESTRICT' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_citations DROP CONSTRAINT fk_context_citation_share';
        EXECUTE 'ALTER TABLE public.consultation_context_citations ADD CONSTRAINT fk_context_citation_share FOREIGN KEY (context_share_id) REFERENCES consultation_context_shares(context_share_id) ON DELETE RESTRICT';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'fk_context_citation_source' AND conrelid = 'public.consultation_context_citations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_citations ADD CONSTRAINT fk_context_citation_source FOREIGN KEY (evidence_source_id) REFERENCES knowledge_sources(knowledge_source_id) ON DELETE RESTRICT';
    ELSIF v_existing_def <> 'FOREIGN KEY (evidence_source_id) REFERENCES knowledge_sources(knowledge_source_id) ON DELETE RESTRICT' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_citations DROP CONSTRAINT fk_context_citation_source';
        EXECUTE 'ALTER TABLE public.consultation_context_citations ADD CONSTRAINT fk_context_citation_source FOREIGN KEY (evidence_source_id) REFERENCES knowledge_sources(knowledge_source_id) ON DELETE RESTRICT';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'fk_context_consent_integrity' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT fk_context_consent_integrity FOREIGN KEY (consent_grant_id, owner_user_id, idempotency_key) REFERENCES data_permissions(legacy_consent_id, owner_user_id, evidence_key) ON DELETE RESTRICT';
    ELSIF v_existing_def <> 'FOREIGN KEY (consent_grant_id, owner_user_id, idempotency_key) REFERENCES data_permissions(legacy_consent_id, owner_user_id, evidence_key) ON DELETE RESTRICT' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT fk_context_consent_integrity';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT fk_context_consent_integrity FOREIGN KEY (consent_grant_id, owner_user_id, idempotency_key) REFERENCES data_permissions(legacy_consent_id, owner_user_id, evidence_key) ON DELETE RESTRICT';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'fk_context_expert' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT fk_context_expert FOREIGN KEY (expert_profile_id) REFERENCES users(user_id) ON DELETE RESTRICT';
    ELSIF v_existing_def <> 'FOREIGN KEY (expert_profile_id) REFERENCES users(user_id) ON DELETE RESTRICT' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT fk_context_expert';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT fk_context_expert FOREIGN KEY (expert_profile_id) REFERENCES users(user_id) ON DELETE RESTRICT';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'fk_context_intake_snapshot' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT fk_context_intake_snapshot FOREIGN KEY (intake_session_id, owner_user_id, journey_id, origin_dashboard, origin_reference_id, triage_stage, risk_level, intake_status) REFERENCES triage_sessions(triage_session_id, user_id, journey_id, origin_dashboard, origin_reference_id, stage, risk_level, status) ON DELETE RESTRICT';
    ELSIF v_existing_def <> 'FOREIGN KEY (intake_session_id, owner_user_id, journey_id, origin_dashboard, origin_reference_id, triage_stage, risk_level, intake_status) REFERENCES triage_sessions(triage_session_id, user_id, journey_id, origin_dashboard, origin_reference_id, stage, risk_level, status) ON DELETE RESTRICT' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT fk_context_intake_snapshot';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT fk_context_intake_snapshot FOREIGN KEY (intake_session_id, owner_user_id, journey_id, origin_dashboard, origin_reference_id, triage_stage, risk_level, intake_status) REFERENCES triage_sessions(triage_session_id, user_id, journey_id, origin_dashboard, origin_reference_id, stage, risk_level, status) ON DELETE RESTRICT';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'fk_context_journey_owner' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT fk_context_journey_owner FOREIGN KEY (journey_id, owner_user_id) REFERENCES mother_journeys(journey_id, owner_user_id) ON DELETE RESTRICT';
    ELSIF v_existing_def <> 'FOREIGN KEY (journey_id, owner_user_id) REFERENCES mother_journeys(journey_id, owner_user_id) ON DELETE RESTRICT' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT fk_context_journey_owner';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT fk_context_journey_owner FOREIGN KEY (journey_id, owner_user_id) REFERENCES mother_journeys(journey_id, owner_user_id) ON DELETE RESTRICT';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'fk_context_request_integrity' AND conrelid = 'public.consultation_context_shares'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT fk_context_request_integrity FOREIGN KEY (consultation_request_id, owner_user_id, expert_profile_id, idempotency_key) REFERENCES expert_consultation_requests(id, requester_user_id, expert_profile_id, client_request_id) ON DELETE RESTRICT';
    ELSIF v_existing_def <> 'FOREIGN KEY (consultation_request_id, owner_user_id, expert_profile_id, idempotency_key) REFERENCES expert_consultation_requests(id, requester_user_id, expert_profile_id, client_request_id) ON DELETE RESTRICT' THEN
        EXECUTE 'ALTER TABLE public.consultation_context_shares DROP CONSTRAINT fk_context_request_integrity';
        EXECUTE 'ALTER TABLE public.consultation_context_shares ADD CONSTRAINT fk_context_request_integrity FOREIGN KEY (consultation_request_id, owner_user_id, expert_profile_id, idempotency_key) REFERENCES expert_consultation_requests(id, requester_user_id, expert_profile_id, client_request_id) ON DELETE RESTRICT';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'consultation_sessions_booking_id_fkey' AND conrelid = 'public.consultation_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.consultation_sessions ADD CONSTRAINT consultation_sessions_booking_id_fkey FOREIGN KEY (booking_id) REFERENCES consultation_bookings(booking_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (booking_id) REFERENCES consultation_bookings(booking_id)' THEN
        EXECUTE 'ALTER TABLE public.consultation_sessions DROP CONSTRAINT consultation_sessions_booking_id_fkey';
        EXECUTE 'ALTER TABLE public.consultation_sessions ADD CONSTRAINT consultation_sessions_booking_id_fkey FOREIGN KEY (booking_id) REFERENCES consultation_bookings(booking_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'content_item_sources_content_item_id_fkey' AND conrelid = 'public.content_item_sources'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.content_item_sources ADD CONSTRAINT content_item_sources_content_item_id_fkey FOREIGN KEY (content_item_id) REFERENCES content_items(content_item_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (content_item_id) REFERENCES content_items(content_item_id)' THEN
        EXECUTE 'ALTER TABLE public.content_item_sources DROP CONSTRAINT content_item_sources_content_item_id_fkey';
        EXECUTE 'ALTER TABLE public.content_item_sources ADD CONSTRAINT content_item_sources_content_item_id_fkey FOREIGN KEY (content_item_id) REFERENCES content_items(content_item_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'content_item_sources_knowledge_source_id_fkey' AND conrelid = 'public.content_item_sources'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.content_item_sources ADD CONSTRAINT content_item_sources_knowledge_source_id_fkey FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources(knowledge_source_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources(knowledge_source_id)' THEN
        EXECUTE 'ALTER TABLE public.content_item_sources DROP CONSTRAINT content_item_sources_knowledge_source_id_fkey';
        EXECUTE 'ALTER TABLE public.content_item_sources ADD CONSTRAINT content_item_sources_knowledge_source_id_fkey FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources(knowledge_source_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'content_item_topics_content_item_id_fkey' AND conrelid = 'public.content_item_topics'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.content_item_topics ADD CONSTRAINT content_item_topics_content_item_id_fkey FOREIGN KEY (content_item_id) REFERENCES content_items(content_item_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (content_item_id) REFERENCES content_items(content_item_id)' THEN
        EXECUTE 'ALTER TABLE public.content_item_topics DROP CONSTRAINT content_item_topics_content_item_id_fkey';
        EXECUTE 'ALTER TABLE public.content_item_topics ADD CONSTRAINT content_item_topics_content_item_id_fkey FOREIGN KEY (content_item_id) REFERENCES content_items(content_item_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'content_item_topics_topic_id_fkey' AND conrelid = 'public.content_item_topics'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.content_item_topics ADD CONSTRAINT content_item_topics_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES community_topics(id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (topic_id) REFERENCES community_topics(id)' THEN
        EXECUTE 'ALTER TABLE public.content_item_topics DROP CONSTRAINT content_item_topics_topic_id_fkey';
        EXECUTE 'ALTER TABLE public.content_item_topics ADD CONSTRAINT content_item_topics_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES community_topics(id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'conversation_calls_conversation_id_fkey' AND conrelid = 'public.conversation_calls'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES direct_conversations(conversation_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (conversation_id) REFERENCES direct_conversations(conversation_id)' THEN
        EXECUTE 'ALTER TABLE public.conversation_calls DROP CONSTRAINT conversation_calls_conversation_id_fkey';
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES direct_conversations(conversation_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'conversation_calls_initiated_by_user_id_fkey' AND conrelid = 'public.conversation_calls'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_initiated_by_user_id_fkey FOREIGN KEY (initiated_by_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (initiated_by_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.conversation_calls DROP CONSTRAINT conversation_calls_initiated_by_user_id_fkey';
        EXECUTE 'ALTER TABLE public.conversation_calls ADD CONSTRAINT conversation_calls_initiated_by_user_id_fkey FOREIGN KEY (initiated_by_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'data_permissions_supersedes_fk' AND conrelid = 'public.data_permissions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.data_permissions ADD CONSTRAINT data_permissions_supersedes_fk FOREIGN KEY (supersedes_permission_id) REFERENCES data_permissions(permission_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (supersedes_permission_id) REFERENCES data_permissions(permission_id)' THEN
        EXECUTE 'ALTER TABLE public.data_permissions DROP CONSTRAINT data_permissions_supersedes_fk';
        EXECUTE 'ALTER TABLE public.data_permissions ADD CONSTRAINT data_permissions_supersedes_fk FOREIGN KEY (supersedes_permission_id) REFERENCES data_permissions(permission_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'development_milestones_baby_id_fkey' AND conrelid = 'public.development_milestones'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.development_milestones ADD CONSTRAINT development_milestones_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.development_milestones DROP CONSTRAINT development_milestones_baby_id_fkey';
        EXECUTE 'ALTER TABLE public.development_milestones ADD CONSTRAINT development_milestones_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'development_milestones_recorded_by_fkey' AND conrelid = 'public.development_milestones'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.development_milestones ADD CONSTRAINT development_milestones_recorded_by_fkey FOREIGN KEY (recorded_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (recorded_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.development_milestones DROP CONSTRAINT development_milestones_recorded_by_fkey';
        EXECUTE 'ALTER TABLE public.development_milestones ADD CONSTRAINT development_milestones_recorded_by_fkey FOREIGN KEY (recorded_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'development_milestones_subject_fk' AND conrelid = 'public.development_milestones'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.development_milestones ADD CONSTRAINT development_milestones_subject_fk FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.development_milestones DROP CONSTRAINT development_milestones_subject_fk';
        EXECUTE 'ALTER TABLE public.development_milestones ADD CONSTRAINT development_milestones_subject_fk FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'device_connections_user_id_fkey' AND conrelid = 'public.device_connections'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.device_connections ADD CONSTRAINT device_connections_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.device_connections DROP CONSTRAINT device_connections_user_id_fkey';
        EXECUTE 'ALTER TABLE public.device_connections ADD CONSTRAINT device_connections_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'device_tokens_user_fkey' AND conrelid = 'public.device_tokens'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.device_tokens ADD CONSTRAINT device_tokens_user_fkey FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE';
    ELSIF v_existing_def <> 'FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE' THEN
        EXECUTE 'ALTER TABLE public.device_tokens DROP CONSTRAINT device_tokens_user_fkey';
        EXECUTE 'ALTER TABLE public.device_tokens ADD CONSTRAINT device_tokens_user_fkey FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'direct_conversations_expert_user_id_fkey' AND conrelid = 'public.direct_conversations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.direct_conversations ADD CONSTRAINT direct_conversations_expert_user_id_fkey FOREIGN KEY (expert_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (expert_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.direct_conversations DROP CONSTRAINT direct_conversations_expert_user_id_fkey';
        EXECUTE 'ALTER TABLE public.direct_conversations ADD CONSTRAINT direct_conversations_expert_user_id_fkey FOREIGN KEY (expert_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'direct_conversations_mother_user_id_fkey' AND conrelid = 'public.direct_conversations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.direct_conversations ADD CONSTRAINT direct_conversations_mother_user_id_fkey FOREIGN KEY (mother_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (mother_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.direct_conversations DROP CONSTRAINT direct_conversations_mother_user_id_fkey';
        EXECUTE 'ALTER TABLE public.direct_conversations ADD CONSTRAINT direct_conversations_mother_user_id_fkey FOREIGN KEY (mother_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'direct_messages_conversation_id_fkey' AND conrelid = 'public.direct_messages'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.direct_messages ADD CONSTRAINT direct_messages_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES direct_conversations(conversation_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (conversation_id) REFERENCES direct_conversations(conversation_id)' THEN
        EXECUTE 'ALTER TABLE public.direct_messages DROP CONSTRAINT direct_messages_conversation_id_fkey';
        EXECUTE 'ALTER TABLE public.direct_messages ADD CONSTRAINT direct_messages_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES direct_conversations(conversation_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'direct_messages_sender_user_id_fkey' AND conrelid = 'public.direct_messages'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.direct_messages ADD CONSTRAINT direct_messages_sender_user_id_fkey FOREIGN KEY (sender_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (sender_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.direct_messages DROP CONSTRAINT direct_messages_sender_user_id_fkey';
        EXECUTE 'ALTER TABLE public.direct_messages ADD CONSTRAINT direct_messages_sender_user_id_fkey FOREIGN KEY (sender_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'expense_entries_care_subject_id_fkey' AND conrelid = 'public.expense_entries'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.expense_entries ADD CONSTRAINT expense_entries_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.expense_entries DROP CONSTRAINT expense_entries_care_subject_id_fkey';
        EXECUTE 'ALTER TABLE public.expense_entries ADD CONSTRAINT expense_entries_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'expense_entries_mother_journey_id_fkey' AND conrelid = 'public.expense_entries'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.expense_entries ADD CONSTRAINT expense_entries_mother_journey_id_fkey FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id)' THEN
        EXECUTE 'ALTER TABLE public.expense_entries DROP CONSTRAINT expense_entries_mother_journey_id_fkey';
        EXECUTE 'ALTER TABLE public.expense_entries ADD CONSTRAINT expense_entries_mother_journey_id_fkey FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'expense_entries_owner_user_id_fkey' AND conrelid = 'public.expense_entries'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.expense_entries ADD CONSTRAINT expense_entries_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (owner_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.expense_entries DROP CONSTRAINT expense_entries_owner_user_id_fkey';
        EXECUTE 'ALTER TABLE public.expense_entries ADD CONSTRAINT expense_entries_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'expert_consultation_requests_direct_conversation_id_fkey' AND conrelid = 'public.expert_consultation_requests'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_direct_conversation_id_fkey FOREIGN KEY (direct_conversation_id) REFERENCES direct_conversations(conversation_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (direct_conversation_id) REFERENCES direct_conversations(conversation_id)' THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests DROP CONSTRAINT expert_consultation_requests_direct_conversation_id_fkey';
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_direct_conversation_id_fkey FOREIGN KEY (direct_conversation_id) REFERENCES direct_conversations(conversation_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'expert_consultation_requests_expert_profile_id_fkey' AND conrelid = 'public.expert_consultation_requests'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_expert_profile_id_fkey FOREIGN KEY (expert_profile_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (expert_profile_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests DROP CONSTRAINT expert_consultation_requests_expert_profile_id_fkey';
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_expert_profile_id_fkey FOREIGN KEY (expert_profile_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'expert_consultation_requests_requester_user_id_fkey' AND conrelid = 'public.expert_consultation_requests'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_requester_user_id_fkey FOREIGN KEY (requester_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (requester_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests DROP CONSTRAINT expert_consultation_requests_requester_user_id_fkey';
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_requester_user_id_fkey FOREIGN KEY (requester_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'expert_consultation_requests_responded_by_fkey' AND conrelid = 'public.expert_consultation_requests'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_responded_by_fkey FOREIGN KEY (responded_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (responded_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.expert_consultation_requests DROP CONSTRAINT expert_consultation_requests_responded_by_fkey';
        EXECUTE 'ALTER TABLE public.expert_consultation_requests ADD CONSTRAINT expert_consultation_requests_responded_by_fkey FOREIGN KEY (responded_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'growth_measurements_baby_id_fkey' AND conrelid = 'public.growth_measurements'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.growth_measurements ADD CONSTRAINT growth_measurements_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.growth_measurements DROP CONSTRAINT growth_measurements_baby_id_fkey';
        EXECUTE 'ALTER TABLE public.growth_measurements ADD CONSTRAINT growth_measurements_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'growth_measurements_subject_fk' AND conrelid = 'public.growth_measurements'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.growth_measurements ADD CONSTRAINT growth_measurements_subject_fk FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.growth_measurements DROP CONSTRAINT growth_measurements_subject_fk';
        EXECUTE 'ALTER TABLE public.growth_measurements ADD CONSTRAINT growth_measurements_subject_fk FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'health_context_memories_care_subject_id_fkey' AND conrelid = 'public.health_context_memories'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.health_context_memories ADD CONSTRAINT health_context_memories_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.health_context_memories DROP CONSTRAINT health_context_memories_care_subject_id_fkey';
        EXECUTE 'ALTER TABLE public.health_context_memories ADD CONSTRAINT health_context_memories_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'health_context_memories_triage_session_id_fkey' AND conrelid = 'public.health_context_memories'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.health_context_memories ADD CONSTRAINT health_context_memories_triage_session_id_fkey FOREIGN KEY (triage_session_id) REFERENCES triage_sessions(triage_session_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (triage_session_id) REFERENCES triage_sessions(triage_session_id)' THEN
        EXECUTE 'ALTER TABLE public.health_context_memories DROP CONSTRAINT health_context_memories_triage_session_id_fkey';
        EXECUTE 'ALTER TABLE public.health_context_memories ADD CONSTRAINT health_context_memories_triage_session_id_fkey FOREIGN KEY (triage_session_id) REFERENCES triage_sessions(triage_session_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'health_context_memories_user_id_fkey' AND conrelid = 'public.health_context_memories'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.health_context_memories ADD CONSTRAINT health_context_memories_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.health_context_memories DROP CONSTRAINT health_context_memories_user_id_fkey';
        EXECUTE 'ALTER TABLE public.health_context_memories ADD CONSTRAINT health_context_memories_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'health_observations_care_subject_id_fkey' AND conrelid = 'public.health_observations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.health_observations DROP CONSTRAINT health_observations_care_subject_id_fkey';
        EXECUTE 'ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'health_observations_device_connection_id_fkey' AND conrelid = 'public.health_observations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_device_connection_id_fkey FOREIGN KEY (device_connection_id) REFERENCES device_connections(device_connection_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (device_connection_id) REFERENCES device_connections(device_connection_id)' THEN
        EXECUTE 'ALTER TABLE public.health_observations DROP CONSTRAINT health_observations_device_connection_id_fkey';
        EXECUTE 'ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_device_connection_id_fkey FOREIGN KEY (device_connection_id) REFERENCES device_connections(device_connection_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'health_records_baby_id_fkey' AND conrelid = 'public.health_records'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.health_records ADD CONSTRAINT health_records_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.health_records DROP CONSTRAINT health_records_baby_id_fkey';
        EXECUTE 'ALTER TABLE public.health_records ADD CONSTRAINT health_records_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'health_records_journey_id_fkey' AND conrelid = 'public.health_records'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.health_records ADD CONSTRAINT health_records_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES mother_journeys(journey_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (journey_id) REFERENCES mother_journeys(journey_id)' THEN
        EXECUTE 'ALTER TABLE public.health_records DROP CONSTRAINT health_records_journey_id_fkey';
        EXECUTE 'ALTER TABLE public.health_records ADD CONSTRAINT health_records_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES mother_journeys(journey_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'health_records_owner_user_id_fkey' AND conrelid = 'public.health_records'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.health_records ADD CONSTRAINT health_records_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (owner_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.health_records DROP CONSTRAINT health_records_owner_user_id_fkey';
        EXECUTE 'ALTER TABLE public.health_records ADD CONSTRAINT health_records_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'knowledge_source_reviews_actor_user_id_fkey' AND conrelid = 'public.knowledge_source_reviews'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.knowledge_source_reviews ADD CONSTRAINT knowledge_source_reviews_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (actor_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.knowledge_source_reviews DROP CONSTRAINT knowledge_source_reviews_actor_user_id_fkey';
        EXECUTE 'ALTER TABLE public.knowledge_source_reviews ADD CONSTRAINT knowledge_source_reviews_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'knowledge_source_reviews_knowledge_source_id_fkey' AND conrelid = 'public.knowledge_source_reviews'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.knowledge_source_reviews ADD CONSTRAINT knowledge_source_reviews_knowledge_source_id_fkey FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources(knowledge_source_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources(knowledge_source_id)' THEN
        EXECUTE 'ALTER TABLE public.knowledge_source_reviews DROP CONSTRAINT knowledge_source_reviews_knowledge_source_id_fkey';
        EXECUTE 'ALTER TABLE public.knowledge_source_reviews ADD CONSTRAINT knowledge_source_reviews_knowledge_source_id_fkey FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources(knowledge_source_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'knowledge_sources_added_by_fkey' AND conrelid = 'public.knowledge_sources'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.knowledge_sources ADD CONSTRAINT knowledge_sources_added_by_fkey FOREIGN KEY (added_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (added_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.knowledge_sources DROP CONSTRAINT knowledge_sources_added_by_fkey';
        EXECUTE 'ALTER TABLE public.knowledge_sources ADD CONSTRAINT knowledge_sources_added_by_fkey FOREIGN KEY (added_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'knowledge_sources_reviewed_by_fkey' AND conrelid = 'public.knowledge_sources'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.knowledge_sources ADD CONSTRAINT knowledge_sources_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (reviewed_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.knowledge_sources DROP CONSTRAINT knowledge_sources_reviewed_by_fkey';
        EXECUTE 'ALTER TABLE public.knowledge_sources ADD CONSTRAINT knowledge_sources_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'maternal_exercise_sessions_mother_journey_id_fkey' AND conrelid = 'public.maternal_exercise_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_mother_journey_id_fkey FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id)' THEN
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions DROP CONSTRAINT maternal_exercise_sessions_mother_journey_id_fkey';
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_mother_journey_id_fkey FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'maternal_exercise_sessions_owner_user_id_fkey' AND conrelid = 'public.maternal_exercise_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (owner_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions DROP CONSTRAINT maternal_exercise_sessions_owner_user_id_fkey';
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'maternal_exercise_sessions_posture_config_fk' AND conrelid = 'public.maternal_exercise_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_posture_config_fk FOREIGN KEY (posture_config_id) REFERENCES care_item_templates(template_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (posture_config_id) REFERENCES care_item_templates(template_id)' THEN
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions DROP CONSTRAINT maternal_exercise_sessions_posture_config_fk';
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_posture_config_fk FOREIGN KEY (posture_config_id) REFERENCES care_item_templates(template_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'maternal_exercise_sessions_template_fk' AND conrelid = 'public.maternal_exercise_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_template_fk FOREIGN KEY (exercise_template_id) REFERENCES care_item_templates(template_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (exercise_template_id) REFERENCES care_item_templates(template_id)' THEN
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions DROP CONSTRAINT maternal_exercise_sessions_template_fk';
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_template_fk FOREIGN KEY (exercise_template_id) REFERENCES care_item_templates(template_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'moderation_cases_ai_feedback_assessment_id_fkey' AND conrelid = 'public.moderation_cases'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.moderation_cases ADD CONSTRAINT moderation_cases_ai_feedback_assessment_id_fkey FOREIGN KEY (ai_feedback_assessment_id) REFERENCES ai_content_assessments(assessment_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (ai_feedback_assessment_id) REFERENCES ai_content_assessments(assessment_id)' THEN
        EXECUTE 'ALTER TABLE public.moderation_cases DROP CONSTRAINT moderation_cases_ai_feedback_assessment_id_fkey';
        EXECUTE 'ALTER TABLE public.moderation_cases ADD CONSTRAINT moderation_cases_ai_feedback_assessment_id_fkey FOREIGN KEY (ai_feedback_assessment_id) REFERENCES ai_content_assessments(assessment_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'moderation_cases_ai_feedback_by_fkey' AND conrelid = 'public.moderation_cases'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.moderation_cases ADD CONSTRAINT moderation_cases_ai_feedback_by_fkey FOREIGN KEY (ai_feedback_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (ai_feedback_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.moderation_cases DROP CONSTRAINT moderation_cases_ai_feedback_by_fkey';
        EXECUTE 'ALTER TABLE public.moderation_cases ADD CONSTRAINT moderation_cases_ai_feedback_by_fkey FOREIGN KEY (ai_feedback_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'moderation_cases_assigned_moderator_id_fkey' AND conrelid = 'public.moderation_cases'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.moderation_cases ADD CONSTRAINT moderation_cases_assigned_moderator_id_fkey FOREIGN KEY (assigned_moderator_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (assigned_moderator_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.moderation_cases DROP CONSTRAINT moderation_cases_assigned_moderator_id_fkey';
        EXECUTE 'ALTER TABLE public.moderation_cases ADD CONSTRAINT moderation_cases_assigned_moderator_id_fkey FOREIGN KEY (assigned_moderator_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'moderation_cases_reporter_user_id_fkey' AND conrelid = 'public.moderation_cases'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.moderation_cases ADD CONSTRAINT moderation_cases_reporter_user_id_fkey FOREIGN KEY (reporter_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (reporter_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.moderation_cases DROP CONSTRAINT moderation_cases_reporter_user_id_fkey';
        EXECUTE 'ALTER TABLE public.moderation_cases ADD CONSTRAINT moderation_cases_reporter_user_id_fkey FOREIGN KEY (reporter_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'mother_journeys_owner_user_id_fkey' AND conrelid = 'public.mother_journeys'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.mother_journeys ADD CONSTRAINT mother_journeys_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (owner_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.mother_journeys DROP CONSTRAINT mother_journeys_owner_user_id_fkey';
        EXECUTE 'ALTER TABLE public.mother_journeys ADD CONSTRAINT mother_journeys_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'mother_journeys_subject_fk' AND conrelid = 'public.mother_journeys'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.mother_journeys ADD CONSTRAINT mother_journeys_subject_fk FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.mother_journeys DROP CONSTRAINT mother_journeys_subject_fk';
        EXECUTE 'ALTER TABLE public.mother_journeys ADD CONSTRAINT mother_journeys_subject_fk FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'fk_notification_records_user' AND conrelid = 'public.notification_records'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.notification_records ADD CONSTRAINT fk_notification_records_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE';
    ELSIF v_existing_def <> 'FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE' THEN
        EXECUTE 'ALTER TABLE public.notification_records DROP CONSTRAINT fk_notification_records_user';
        EXECUTE 'ALTER TABLE public.notification_records ADD CONSTRAINT fk_notification_records_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'partner_organizations_representative_user_id_fkey' AND conrelid = 'public.partner_organizations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.partner_organizations ADD CONSTRAINT partner_organizations_representative_user_id_fkey FOREIGN KEY (representative_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (representative_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.partner_organizations DROP CONSTRAINT partner_organizations_representative_user_id_fkey';
        EXECUTE 'ALTER TABLE public.partner_organizations ADD CONSTRAINT partner_organizations_representative_user_id_fkey FOREIGN KEY (representative_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'preparation_checklist_items_mother_journey_id_fkey' AND conrelid = 'public.preparation_checklist_items'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.preparation_checklist_items ADD CONSTRAINT preparation_checklist_items_mother_journey_id_fkey FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id)' THEN
        EXECUTE 'ALTER TABLE public.preparation_checklist_items DROP CONSTRAINT preparation_checklist_items_mother_journey_id_fkey';
        EXECUTE 'ALTER TABLE public.preparation_checklist_items ADD CONSTRAINT preparation_checklist_items_mother_journey_id_fkey FOREIGN KEY (mother_journey_id) REFERENCES mother_journeys(journey_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'preparation_checklist_items_owner_user_id_fkey' AND conrelid = 'public.preparation_checklist_items'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.preparation_checklist_items ADD CONSTRAINT preparation_checklist_items_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (owner_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.preparation_checklist_items DROP CONSTRAINT preparation_checklist_items_owner_user_id_fkey';
        EXECUTE 'ALTER TABLE public.preparation_checklist_items ADD CONSTRAINT preparation_checklist_items_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'preparation_checklist_items_template_entry_id_fkey' AND conrelid = 'public.preparation_checklist_items'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.preparation_checklist_items ADD CONSTRAINT preparation_checklist_items_template_entry_id_fkey FOREIGN KEY (template_entry_id) REFERENCES care_item_templates(template_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (template_entry_id) REFERENCES care_item_templates(template_id)' THEN
        EXECUTE 'ALTER TABLE public.preparation_checklist_items DROP CONSTRAINT preparation_checklist_items_template_entry_id_fkey';
        EXECUTE 'ALTER TABLE public.preparation_checklist_items ADD CONSTRAINT preparation_checklist_items_template_entry_id_fkey FOREIGN KEY (template_entry_id) REFERENCES care_item_templates(template_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'professional_specialties_professional_profile_id_fkey' AND conrelid = 'public.professional_specialties'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.professional_specialties ADD CONSTRAINT professional_specialties_professional_profile_id_fkey FOREIGN KEY (professional_profile_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (professional_profile_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.professional_specialties DROP CONSTRAINT professional_specialties_professional_profile_id_fkey';
        EXECUTE 'ALTER TABLE public.professional_specialties ADD CONSTRAINT professional_specialties_professional_profile_id_fkey FOREIGN KEY (professional_profile_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'professional_specialties_specialty_id_fkey' AND conrelid = 'public.professional_specialties'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.professional_specialties ADD CONSTRAINT professional_specialties_specialty_id_fkey FOREIGN KEY (specialty_id) REFERENCES specialties(specialty_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (specialty_id) REFERENCES specialties(specialty_id)' THEN
        EXECUTE 'ALTER TABLE public.professional_specialties DROP CONSTRAINT professional_specialties_specialty_id_fkey';
        EXECUTE 'ALTER TABLE public.professional_specialties ADD CONSTRAINT professional_specialties_specialty_id_fkey FOREIGN KEY (specialty_id) REFERENCES specialties(specialty_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'fk_red_flag_rules_created_by' AND conrelid = 'public.red_flag_rules'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.red_flag_rules ADD CONSTRAINT fk_red_flag_rules_created_by FOREIGN KEY (created_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (created_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.red_flag_rules DROP CONSTRAINT fk_red_flag_rules_created_by';
        EXECUTE 'ALTER TABLE public.red_flag_rules ADD CONSTRAINT fk_red_flag_rules_created_by FOREIGN KEY (created_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'fk_red_flag_rules_updated_by' AND conrelid = 'public.red_flag_rules'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.red_flag_rules ADD CONSTRAINT fk_red_flag_rules_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (updated_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.red_flag_rules DROP CONSTRAINT fk_red_flag_rules_updated_by';
        EXECUTE 'ALTER TABLE public.red_flag_rules ADD CONSTRAINT fk_red_flag_rules_updated_by FOREIGN KEY (updated_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_configs_updated_by_fkey' AND conrelid = 'public.safety_configs'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_configs ADD CONSTRAINT safety_configs_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (updated_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_configs DROP CONSTRAINT safety_configs_updated_by_fkey';
        EXECUTE 'ALTER TABLE public.safety_configs ADD CONSTRAINT safety_configs_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_configs_user_id_fkey' AND conrelid = 'public.safety_configs'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_configs ADD CONSTRAINT safety_configs_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_configs DROP CONSTRAINT safety_configs_user_id_fkey';
        EXECUTE 'ALTER TABLE public.safety_configs ADD CONSTRAINT safety_configs_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_care_subject_id_fkey' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_care_subject_id_fkey';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_created_by_user_fk' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_created_by_user_fk FOREIGN KEY (created_by_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (created_by_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_created_by_user_fk';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_created_by_user_fk FOREIGN KEY (created_by_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_emergency_session_fk' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_emergency_session_fk FOREIGN KEY (emergency_session_id) REFERENCES safety_events(safety_event_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (emergency_session_id) REFERENCES safety_events(safety_event_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_emergency_session_fk';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_emergency_session_fk FOREIGN KEY (emergency_session_id) REFERENCES safety_events(safety_event_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_monitoring_session_id_fkey' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_monitoring_session_id_fkey FOREIGN KEY (monitoring_session_id) REFERENCES safety_monitoring_sessions(monitoring_session_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (monitoring_session_id) REFERENCES safety_monitoring_sessions(monitoring_session_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_monitoring_session_id_fkey';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_monitoring_session_id_fkey FOREIGN KEY (monitoring_session_id) REFERENCES safety_monitoring_sessions(monitoring_session_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_user_id_fkey' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_user_id_fkey';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_monitoring_sessions_created_by_fkey' AND conrelid = 'public.safety_monitoring_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_monitoring_sessions ADD CONSTRAINT safety_monitoring_sessions_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (created_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_monitoring_sessions DROP CONSTRAINT safety_monitoring_sessions_created_by_fkey';
        EXECUTE 'ALTER TABLE public.safety_monitoring_sessions ADD CONSTRAINT safety_monitoring_sessions_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_monitoring_sessions_user_id_fkey' AND conrelid = 'public.safety_monitoring_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_monitoring_sessions ADD CONSTRAINT safety_monitoring_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_monitoring_sessions DROP CONSTRAINT safety_monitoring_sessions_user_id_fkey';
        EXECUTE 'ALTER TABLE public.safety_monitoring_sessions ADD CONSTRAINT safety_monitoring_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'system_configurations_updated_by_fkey' AND conrelid = 'public.system_configurations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.system_configurations ADD CONSTRAINT system_configurations_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (updated_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.system_configurations DROP CONSTRAINT system_configurations_updated_by_fkey';
        EXECUTE 'ALTER TABLE public.system_configurations ADD CONSTRAINT system_configurations_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'triage_session_evidence_source_fk' AND conrelid = 'public.triage_session_evidence'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.triage_session_evidence ADD CONSTRAINT triage_session_evidence_source_fk FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources(knowledge_source_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources(knowledge_source_id)' THEN
        EXECUTE 'ALTER TABLE public.triage_session_evidence DROP CONSTRAINT triage_session_evidence_source_fk';
        EXECUTE 'ALTER TABLE public.triage_session_evidence ADD CONSTRAINT triage_session_evidence_source_fk FOREIGN KEY (knowledge_source_id) REFERENCES knowledge_sources(knowledge_source_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'triage_session_evidence_triage_session_id_fkey' AND conrelid = 'public.triage_session_evidence'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.triage_session_evidence ADD CONSTRAINT triage_session_evidence_triage_session_id_fkey FOREIGN KEY (triage_session_id) REFERENCES triage_sessions(triage_session_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (triage_session_id) REFERENCES triage_sessions(triage_session_id)' THEN
        EXECUTE 'ALTER TABLE public.triage_session_evidence DROP CONSTRAINT triage_session_evidence_triage_session_id_fkey';
        EXECUTE 'ALTER TABLE public.triage_session_evidence ADD CONSTRAINT triage_session_evidence_triage_session_id_fkey FOREIGN KEY (triage_session_id) REFERENCES triage_sessions(triage_session_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'fk_triage_journey_owner' AND conrelid = 'public.triage_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT fk_triage_journey_owner FOREIGN KEY (journey_id, user_id) REFERENCES mother_journeys(journey_id, owner_user_id) ON DELETE RESTRICT';
    ELSIF v_existing_def <> 'FOREIGN KEY (journey_id, user_id) REFERENCES mother_journeys(journey_id, owner_user_id) ON DELETE RESTRICT' THEN
        EXECUTE 'ALTER TABLE public.triage_sessions DROP CONSTRAINT fk_triage_journey_owner';
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT fk_triage_journey_owner FOREIGN KEY (journey_id, user_id) REFERENCES mother_journeys(journey_id, owner_user_id) ON DELETE RESTRICT';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'triage_sessions_care_subject_id_fkey' AND conrelid = 'public.triage_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT triage_sessions_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.triage_sessions DROP CONSTRAINT triage_sessions_care_subject_id_fkey';
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT triage_sessions_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'triage_sessions_user_id_fkey' AND conrelid = 'public.triage_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT triage_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.triage_sessions DROP CONSTRAINT triage_sessions_user_id_fkey';
        EXECUTE 'ALTER TABLE public.triage_sessions ADD CONSTRAINT triage_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'users_deactivated_by_fkey' AND conrelid = 'public.users'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT users_deactivated_by_fkey FOREIGN KEY (deactivated_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (deactivated_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.users DROP CONSTRAINT users_deactivated_by_fkey';
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT users_deactivated_by_fkey FOREIGN KEY (deactivated_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'vaccination_records_baby_id_fkey' AND conrelid = 'public.vaccination_records'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.vaccination_records ADD CONSTRAINT vaccination_records_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.vaccination_records DROP CONSTRAINT vaccination_records_baby_id_fkey';
        EXECUTE 'ALTER TABLE public.vaccination_records ADD CONSTRAINT vaccination_records_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'vaccination_records_proof_record_id_fkey' AND conrelid = 'public.vaccination_records'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.vaccination_records ADD CONSTRAINT vaccination_records_proof_record_id_fkey FOREIGN KEY (proof_record_id) REFERENCES health_records(health_record_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (proof_record_id) REFERENCES health_records(health_record_id)' THEN
        EXECUTE 'ALTER TABLE public.vaccination_records DROP CONSTRAINT vaccination_records_proof_record_id_fkey';
        EXECUTE 'ALTER TABLE public.vaccination_records ADD CONSTRAINT vaccination_records_proof_record_id_fkey FOREIGN KEY (proof_record_id) REFERENCES health_records(health_record_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'vaccination_records_schedule_fk' AND conrelid = 'public.vaccination_records'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.vaccination_records ADD CONSTRAINT vaccination_records_schedule_fk FOREIGN KEY (vaccination_schedule_id) REFERENCES vaccination_schedules(vaccination_schedule_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (vaccination_schedule_id) REFERENCES vaccination_schedules(vaccination_schedule_id)' THEN
        EXECUTE 'ALTER TABLE public.vaccination_records DROP CONSTRAINT vaccination_records_schedule_fk';
        EXECUTE 'ALTER TABLE public.vaccination_records ADD CONSTRAINT vaccination_records_schedule_fk FOREIGN KEY (vaccination_schedule_id) REFERENCES vaccination_schedules(vaccination_schedule_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'vaccination_records_subject_fk' AND conrelid = 'public.vaccination_records'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.vaccination_records ADD CONSTRAINT vaccination_records_subject_fk FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)' THEN
        EXECUTE 'ALTER TABLE public.vaccination_records DROP CONSTRAINT vaccination_records_subject_fk';
        EXECUTE 'ALTER TABLE public.vaccination_records ADD CONSTRAINT vaccination_records_subject_fk FOREIGN KEY (care_subject_id) REFERENCES care_subjects(care_subject_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'attachments_health_record_id_fkey' AND conrelid = 'public.attachments'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.attachments ADD CONSTRAINT attachments_health_record_id_fkey FOREIGN KEY (health_record_id) REFERENCES health_records(health_record_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (health_record_id) REFERENCES health_records(health_record_id)' THEN
        EXECUTE 'ALTER TABLE public.attachments DROP CONSTRAINT attachments_health_record_id_fkey';
        EXECUTE 'ALTER TABLE public.attachments ADD CONSTRAINT attachments_health_record_id_fkey FOREIGN KEY (health_record_id) REFERENCES health_records(health_record_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'audit_events_reviewed_by_fkey' AND conrelid = 'public.audit_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.audit_events ADD CONSTRAINT audit_events_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (reviewed_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.audit_events DROP CONSTRAINT audit_events_reviewed_by_fkey';
        EXECUTE 'ALTER TABLE public.audit_events ADD CONSTRAINT audit_events_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'audit_events_security_event_fk' AND conrelid = 'public.audit_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.audit_events ADD CONSTRAINT audit_events_security_event_fk FOREIGN KEY (security_event_id) REFERENCES audit_events(audit_event_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (security_event_id) REFERENCES audit_events(audit_event_id)' THEN
        EXECUTE 'ALTER TABLE public.audit_events DROP CONSTRAINT audit_events_security_event_fk';
        EXECUTE 'ALTER TABLE public.audit_events ADD CONSTRAINT audit_events_security_event_fk FOREIGN KEY (security_event_id) REFERENCES audit_events(audit_event_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_facilities_partner_archive_fk' AND conrelid = 'public.care_facilities'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_facilities ADD CONSTRAINT care_facilities_partner_archive_fk FOREIGN KEY (partner_id) REFERENCES archived_records(archive_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (partner_id) REFERENCES archived_records(archive_id)' THEN
        EXECUTE 'ALTER TABLE public.care_facilities DROP CONSTRAINT care_facilities_partner_archive_fk';
        EXECUTE 'ALTER TABLE public.care_facilities ADD CONSTRAINT care_facilities_partner_archive_fk FOREIGN KEY (partner_id) REFERENCES archived_records(archive_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_subjects_person_id_fkey' AND conrelid = 'public.care_subjects'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_subjects ADD CONSTRAINT care_subjects_person_id_fkey FOREIGN KEY (person_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (person_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.care_subjects DROP CONSTRAINT care_subjects_person_id_fkey';
        EXECUTE 'ALTER TABLE public.care_subjects ADD CONSTRAINT care_subjects_person_id_fkey FOREIGN KEY (person_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'care_tasks_care_group_id_fkey1' AND conrelid = 'public.care_tasks'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_care_group_id_fkey1 FOREIGN KEY (care_group_id) REFERENCES care_groups(care_group_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (care_group_id) REFERENCES care_groups(care_group_id)' THEN
        EXECUTE 'ALTER TABLE public.care_tasks DROP CONSTRAINT care_tasks_care_group_id_fkey1';
        EXECUTE 'ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_care_group_id_fkey1 FOREIGN KEY (care_group_id) REFERENCES care_groups(care_group_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'health_observations_device_fk' AND conrelid = 'public.health_observations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_device_fk FOREIGN KEY (device_connection_id) REFERENCES device_connections(device_connection_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (device_connection_id) REFERENCES device_connections(device_connection_id)' THEN
        EXECUTE 'ALTER TABLE public.health_observations DROP CONSTRAINT health_observations_device_fk';
        EXECUTE 'ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_device_fk FOREIGN KEY (device_connection_id) REFERENCES device_connections(device_connection_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'health_observations_session_fk' AND conrelid = 'public.health_observations'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_session_fk FOREIGN KEY (source_record_id) REFERENCES maternal_exercise_sessions(exercise_session_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (source_record_id) REFERENCES maternal_exercise_sessions(exercise_session_id)' THEN
        EXECUTE 'ALTER TABLE public.health_observations DROP CONSTRAINT health_observations_session_fk';
        EXECUTE 'ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_session_fk FOREIGN KEY (source_record_id) REFERENCES maternal_exercise_sessions(exercise_session_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'maternal_exercise_sessions_safety_fk' AND conrelid = 'public.maternal_exercise_sessions'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_safety_fk FOREIGN KEY (safety_observation_id) REFERENCES health_observations(health_observation_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (safety_observation_id) REFERENCES health_observations(health_observation_id)' THEN
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions DROP CONSTRAINT maternal_exercise_sessions_safety_fk';
        EXECUTE 'ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_safety_fk FOREIGN KEY (safety_observation_id) REFERENCES health_observations(health_observation_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_care_facility_id_fkey' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_care_facility_id_fkey FOREIGN KEY (care_facility_id) REFERENCES care_facilities(facility_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (care_facility_id) REFERENCES care_facilities(facility_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_care_facility_id_fkey';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_care_facility_id_fkey FOREIGN KEY (care_facility_id) REFERENCES care_facilities(facility_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_device_token_fk' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_device_token_fk FOREIGN KEY (device_token_id) REFERENCES device_tokens(id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (device_token_id) REFERENCES device_tokens(id)' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_device_token_fk';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_device_token_fk FOREIGN KEY (device_token_id) REFERENCES device_tokens(id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_notification_record_id_fkey' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_notification_record_id_fkey FOREIGN KEY (notification_record_id) REFERENCES notification_records(id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (notification_record_id) REFERENCES notification_records(id)' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_notification_record_id_fkey';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_notification_record_id_fkey FOREIGN KEY (notification_record_id) REFERENCES notification_records(id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_owner_fk' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_owner_fk FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (owner_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_owner_fk';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_owner_fk FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_owner_user_id_fkey' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (owner_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_owner_user_id_fkey';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_parent_event_id_fkey' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_parent_event_id_fkey FOREIGN KEY (parent_event_id) REFERENCES safety_events(safety_event_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (parent_event_id) REFERENCES safety_events(safety_event_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_parent_event_id_fkey';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_parent_event_id_fkey FOREIGN KEY (parent_event_id) REFERENCES safety_events(safety_event_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'safety_events_recipient_user_id_fkey' AND conrelid = 'public.safety_events'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_recipient_user_id_fkey FOREIGN KEY (recipient_user_id) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (recipient_user_id) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.safety_events DROP CONSTRAINT safety_events_recipient_user_id_fkey';
        EXECUTE 'ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_recipient_user_id_fkey FOREIGN KEY (recipient_user_id) REFERENCES users(user_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'users_facility_id_fkey' AND conrelid = 'public.users'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT users_facility_id_fkey FOREIGN KEY (facility_id) REFERENCES care_facilities(facility_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (facility_id) REFERENCES care_facilities(facility_id)' THEN
        EXECUTE 'ALTER TABLE public.users DROP CONSTRAINT users_facility_id_fkey';
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT users_facility_id_fkey FOREIGN KEY (facility_id) REFERENCES care_facilities(facility_id)';
    END IF;
    SELECT pg_get_constraintdef(oid) INTO v_existing_def FROM pg_constraint WHERE conname = 'users_verified_by_fkey' AND conrelid = 'public.users'::regclass;
    IF v_existing_def IS NULL THEN
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT users_verified_by_fkey FOREIGN KEY (verified_by) REFERENCES users(user_id)';
    ELSIF v_existing_def <> 'FOREIGN KEY (verified_by) REFERENCES users(user_id)' THEN
        EXECUTE 'ALTER TABLE public.users DROP CONSTRAINT users_verified_by_fkey';
        EXECUTE 'ALTER TABLE public.users ADD CONSTRAINT users_verified_by_fkey FOREIGN KEY (verified_by) REFERENCES users(user_id)';
    END IF;
END
$canonical_foreign_keys$;


-- ============================================================================

-- ============================================================================
-- 5. Functions
-- ============================================================================
CREATE OR REPLACE FUNCTION public.carebridge_care_logs_view_write() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE row_id uuid;
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM public.care_tasks WHERE task_id = OLD.care_log_id AND task_type = 'CARE_LOG';
        RETURN OLD;
    END IF;
    row_id := CASE WHEN TG_OP = 'INSERT'
                  THEN coalesce(NEW.care_log_id, gen_random_uuid())
                  ELSE coalesce(NEW.care_log_id, OLD.care_log_id, gen_random_uuid()) END;
    INSERT INTO public.care_tasks (
        task_id, task_type, creator_user_id, care_subject_id, title, description,
        scheduled_at, completed_at, status, source_reference_type, source_reference_id,
        metadata_jsonb, created_at, updated_at
    ) VALUES (
        row_id, 'CARE_LOG', NEW.recorded_by, NEW.care_subject_id,
        'Care log: ' || NEW.log_type, NEW.note, NEW.started_at, NEW.ended_at,
        coalesce(NEW.status, 'ACTIVE'), 'CARE_LOG', row_id,
        coalesce(NEW.payload_jsonb, '{}'::jsonb) || jsonb_build_object(
            'logType', NEW.log_type, 'endedAt', NEW.ended_at, 'quantity', NEW.quantity,
            'unit', NEW.unit, 'recordedBy', NEW.recorded_by
        ),
        coalesce(NEW.created_at, now()), coalesce(NEW.updated_at, now())
    ) ON CONFLICT (task_id) DO UPDATE
        SET creator_user_id = excluded.creator_user_id,
            care_subject_id = excluded.care_subject_id,
            title = excluded.title,
            description = excluded.description,
            scheduled_at = excluded.scheduled_at,
            completed_at = excluded.completed_at,
            status = excluded.status,
            metadata_jsonb = excluded.metadata_jsonb,
            updated_at = excluded.updated_at;
    NEW.care_log_id := row_id;
    RETURN NEW;
END
$$;

CREATE OR REPLACE FUNCTION public.carebridge_emergency_contacts_view_write() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE member_id uuid;
BEGIN
    IF TG_OP = 'DELETE' THEN
        UPDATE public.care_group_members
           SET is_emergency_contact = false,
               emergency_contact_priority = NULL,
               updated_at = now()
         WHERE care_group_member_id = OLD.id;
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        IF NEW.name IS DISTINCT FROM OLD.name OR NEW.phone IS DISTINCT FROM OLD.phone THEN
            RAISE EXCEPTION 'EMERGENCY_CONTACT_PROFILE_MANAGED_BY_CARE_GROUP_MEMBER';
        END IF;
        UPDATE public.care_group_members
           SET is_emergency_contact = coalesce(NEW.primary_contact, false),
               emergency_contact_priority = CASE WHEN coalesce(NEW.primary_contact, false) THEN 1 ELSE NULL END,
               member_role = coalesce(nullif(NEW.relationship, ''), member_role),
               updated_at = now()
         WHERE care_group_member_id = OLD.id;
        RETURN NEW;
    END IF;

    SELECT cgm.care_group_member_id
      INTO member_id
      FROM public.care_groups cg
      JOIN public.care_group_members cgm ON cgm.care_group_id = cg.care_group_id
      JOIN public.users member_user ON member_user.user_id = cgm.user_id
     WHERE cg.owner_user_id = NEW.user_id
       AND regexp_replace(coalesce(member_user.phone, ''), '\D', '', 'g') =
           regexp_replace(coalesce(NEW.phone, ''), '\D', '', 'g')
     ORDER BY cgm.updated_at DESC
     LIMIT 1;

    IF member_id IS NULL THEN
        RAISE EXCEPTION 'EMERGENCY_CONTACT_MEMBER_NOT_FOUND: invite the contact to the care group first';
    END IF;

    UPDATE public.care_group_members
       SET is_emergency_contact = coalesce(NEW.primary_contact, true),
           emergency_contact_priority = CASE WHEN coalesce(NEW.primary_contact, true) THEN 1 ELSE NULL END,
           member_role = coalesce(nullif(NEW.relationship, ''), member_role),
           updated_at = now()
     WHERE care_group_member_id = member_id;
    NEW.id := member_id;
    RETURN NEW;
END
$$;

CREATE OR REPLACE FUNCTION public.carebridge_expert_credentials_view_write() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE row_id uuid;
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM public.attachments
         WHERE attachment_id = OLD.credential_id
           AND attachment_category = 'EXPERT_CREDENTIAL';
        RETURN OLD;
    END IF;

    row_id := CASE WHEN TG_OP = 'INSERT'
                  THEN coalesce(NEW.credential_id, gen_random_uuid())
                  ELSE coalesce(NEW.credential_id, OLD.credential_id, gen_random_uuid()) END;
    INSERT INTO public.attachments (
        attachment_id, owner_user_id, storage_key, original_name, mime_type,
        file_size_bytes, status, attachment_category, credential_type,
        credential_number, issuer, issued_date, expiry_date, review_status,
        review_note, reviewed_by, reviewed_at, file_url, file_id, created_at, updated_at
    ) VALUES (
        row_id, NEW.user_id,
        coalesce(nullif(NEW.file_url, ''), 'expert-credential/' || row_id::text),
        coalesce(nullif(NEW.credential_type, ''), 'expert-credential') || '.document',
        'application/octet-stream', 0, 'ACTIVE', 'EXPERT_CREDENTIAL',
        NEW.credential_type, NEW.credential_number, NEW.issuer, NEW.issued_date,
        NEW.expiry_date, coalesce(NEW.review_status, 'PENDING'), NEW.review_note,
        NEW.reviewed_by, NEW.reviewed_at, NEW.file_url, NEW.file_id,
        coalesce(NEW.created_at, now()), coalesce(NEW.updated_at, now())
    ) ON CONFLICT (attachment_id) DO UPDATE
        SET owner_user_id = excluded.owner_user_id,
            credential_type = excluded.credential_type,
            credential_number = excluded.credential_number,
            issuer = excluded.issuer,
            issued_date = excluded.issued_date,
            expiry_date = excluded.expiry_date,
            review_status = excluded.review_status,
            review_note = excluded.review_note,
            reviewed_by = excluded.reviewed_by,
            reviewed_at = excluded.reviewed_at,
            file_url = excluded.file_url,
            file_id = excluded.file_id,
            updated_at = excluded.updated_at;
    NEW.credential_id := row_id;
    RETURN NEW;
END
$$;

CREATE OR REPLACE FUNCTION public.carebridge_guard_completed_triage_snapshot() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.status = 'COMPLETED' THEN
            RAISE EXCEPTION 'completed triage snapshot is immutable';
        END IF;
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' AND OLD.status = 'COMPLETED' THEN
        IF (to_jsonb(NEW) - ARRAY[
                'continuation_acknowledged_at','updated_at',
                'symptom_list','duration_days','intensity','emergency_flag',
                'extracted_at','structured_created_by'])
                IS DISTINCT FROM
           (to_jsonb(OLD) - ARRAY[
                'continuation_acknowledged_at','updated_at',
                'symptom_list','duration_days','intensity','emergency_flag',
                'extracted_at','structured_created_by']) THEN
            RAISE EXCEPTION 'completed triage snapshot is immutable';
        END IF;
        IF NEW.continuation_acknowledged_at IS DISTINCT FROM
               OLD.continuation_acknowledged_at
           AND NOT (
               OLD.continuation_acknowledged_at IS NULL
               AND NEW.continuation_acknowledged_at IS NOT NULL) THEN
            RAISE EXCEPTION 'completed triage acknowledgement is one-way';
        END IF;
    END IF;

    IF NEW.status = 'COMPLETED' THEN
        NEW.result_jsonb := coalesce(NEW.result_jsonb, '{}'::jsonb);
        NEW.schema_version := coalesce(nullif(NEW.schema_version, ''), '1');
        NEW.content_hash := coalesce(
            nullif(NEW.content_hash, ''), md5(NEW.result_jsonb::text));
        IF NEW.risk_level = 'RED' THEN
            NEW.emergency := true;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.carebridge_reject_mutation() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION 'IMMUTABLE_TABLE: %.% does not allow UPDATE or DELETE', TG_TABLE_SCHEMA, TG_TABLE_NAME;
END
$$;

CREATE OR REPLACE FUNCTION public.carebridge_reject_nearby_support_interaction() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION 'NEARBY_PEER_SUPPORT_DISABLED';
END
$$;

CREATE OR REPLACE FUNCTION public.carebridge_validate_expert_request_conversation() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.direct_conversation_id IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
             FROM public.direct_conversations conversation
            WHERE conversation.conversation_id = NEW.direct_conversation_id) THEN
        RAISE EXCEPTION 'STORY68_DIRECT_CONVERSATION_SOURCE_MISMATCH';
    END IF;
    RETURN NEW;
END
$$;

CREATE OR REPLACE FUNCTION public.enforce_community_topic_parent_category() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.type = 'TOPIC'
       AND NOT EXISTS (
           SELECT 1
           FROM community_topics AS parent
           WHERE parent.id = NEW.parent_id
             AND parent.type = 'CATEGORY'
       ) THEN
        RAISE EXCEPTION 'TOPIC parent_id must reference a CATEGORY'
            USING ERRCODE = '23514', CONSTRAINT = 'community_topics_parent_category_check';
    END IF;

    IF TG_OP = 'UPDATE'
       AND OLD.type = 'CATEGORY'
       AND NEW.type <> 'CATEGORY'
       AND EXISTS (
           SELECT 1
           FROM community_topics AS child
           WHERE child.parent_id = NEW.id
       ) THEN
        RAISE EXCEPTION 'A referenced CATEGORY cannot change type'
            USING ERRCODE = '23514', CONSTRAINT = 'community_topics_parent_category_check';
    END IF;

    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.reject_consultation_context_mutation() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME;
END
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

-- ============================================================================

-- ============================================================================
-- 6. Compatibility views
-- ============================================================================
DROP VIEW IF EXISTS public.care_logs;
CREATE VIEW public.care_logs AS
 SELECT task_id AS care_log_id,
    care_subject_id,
    COALESCE((metadata_jsonb ->> 'logType'::text), replace((title)::text, 'Care log: '::text, ''::text)) AS log_type,
    scheduled_at AS started_at,
    COALESCE(((metadata_jsonb ->> 'endedAt'::text))::timestamp with time zone, completed_at) AS ended_at,
    ((metadata_jsonb ->> 'quantity'::text))::numeric AS quantity,
    (metadata_jsonb ->> 'unit'::text) AS unit,
    description AS note,
    COALESCE(((metadata_jsonb ->> 'recordedBy'::text))::uuid, creator_user_id) AS recorded_by,
    status,
    metadata_jsonb AS payload_jsonb,
    created_at,
    updated_at
   FROM public.care_tasks
  WHERE ((task_type)::text = 'CARE_LOG'::text);

DROP VIEW IF EXISTS public.emergency_contacts;
CREATE VIEW public.emergency_contacts AS
 SELECT DISTINCT ON (cg.owner_user_id) cgm.care_group_member_id AS id,
    cg.owner_user_id AS user_id,
    member_user.full_name AS name,
    member_user.phone,
    cgm.member_role AS relationship,
    cgm.is_emergency_contact AS primary_contact,
    cgm.updated_at,
    cgm.user_id AS updated_by
   FROM ((public.care_group_members cgm
     JOIN public.care_groups cg ON ((cg.care_group_id = cgm.care_group_id)))
     JOIN public.users member_user ON ((member_user.user_id = cgm.user_id)))
  WHERE cgm.is_emergency_contact
  ORDER BY cg.owner_user_id, cgm.emergency_contact_priority, cgm.updated_at DESC;

DROP VIEW IF EXISTS public.expert_credentials;
CREATE VIEW public.expert_credentials AS
 SELECT attachment_id AS credential_id,
    owner_user_id AS user_id,
    credential_type,
    credential_number,
    issuer,
    issued_date,
    expiry_date,
    file_url,
    file_id,
    review_status,
    review_note,
    reviewed_by,
    reviewed_at,
    created_at,
    updated_at
   FROM public.attachments
  WHERE ((attachment_category)::text = 'EXPERT_CREDENTIAL'::text);

DROP VIEW IF EXISTS public.nearby_support_interactions;
CREATE VIEW public.nearby_support_interactions AS
 SELECT NULL::uuid AS interaction_id,
    NULL::uuid AS parent_interaction_id,
    NULL::uuid AS user_id,
    NULL::character varying(30) AS interaction_type,
    NULL::character varying(30) AS status,
    NULL::text AS message,
    NULL::numeric AS latitude,
    NULL::numeric AS longitude,
    NULL::timestamp with time zone AS created_at,
    NULL::timestamp with time zone AS updated_at
  WHERE false;

-- ============================================================================

-- ============================================================================
-- 7. Triggers
-- ============================================================================
DROP TRIGGER IF EXISTS audit_events_immutable_trg ON public.audit_events;
CREATE TRIGGER audit_events_immutable_trg BEFORE DELETE OR UPDATE ON public.audit_events FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();

DROP TRIGGER IF EXISTS care_logs_view_write_trg ON public.care_logs;
CREATE TRIGGER care_logs_view_write_trg INSTEAD OF INSERT OR DELETE OR UPDATE ON public.care_logs FOR EACH ROW EXECUTE FUNCTION public.carebridge_care_logs_view_write();

DROP TRIGGER IF EXISTS emergency_contacts_view_write_trg ON public.emergency_contacts;
CREATE TRIGGER emergency_contacts_view_write_trg INSTEAD OF INSERT OR DELETE OR UPDATE ON public.emergency_contacts FOR EACH ROW EXECUTE FUNCTION public.carebridge_emergency_contacts_view_write();

DROP TRIGGER IF EXISTS expert_consultation_request_conversation_source_trg ON public.expert_consultation_requests;
CREATE TRIGGER expert_consultation_request_conversation_source_trg BEFORE INSERT OR UPDATE OF direct_conversation_id ON public.expert_consultation_requests FOR EACH ROW EXECUTE FUNCTION public.carebridge_validate_expert_request_conversation();

DROP TRIGGER IF EXISTS expert_credentials_view_write_trg ON public.expert_credentials;
CREATE TRIGGER expert_credentials_view_write_trg INSTEAD OF INSERT OR DELETE OR UPDATE ON public.expert_credentials FOR EACH ROW EXECUTE FUNCTION public.carebridge_expert_credentials_view_write();

DROP TRIGGER IF EXISTS knowledge_source_reviews_immutable_trg ON public.knowledge_source_reviews;
CREATE TRIGGER knowledge_source_reviews_immutable_trg BEFORE DELETE OR UPDATE ON public.knowledge_source_reviews FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();

DROP TRIGGER IF EXISTS nearby_support_interactions_disabled_trg ON public.nearby_support_interactions;
CREATE TRIGGER nearby_support_interactions_disabled_trg INSTEAD OF INSERT OR DELETE OR UPDATE ON public.nearby_support_interactions FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_nearby_support_interaction();

DROP TRIGGER IF EXISTS trg_community_topic_parent_category ON public.community_topics;
CREATE TRIGGER trg_community_topic_parent_category BEFORE INSERT OR UPDATE OF type, parent_id ON public.community_topics FOR EACH ROW EXECUTE FUNCTION public.enforce_community_topic_parent_category();

DROP TRIGGER IF EXISTS trg_consultation_context_citations_append_only ON public.consultation_context_citations;
CREATE TRIGGER trg_consultation_context_citations_append_only BEFORE DELETE OR UPDATE ON public.consultation_context_citations FOR EACH ROW EXECUTE FUNCTION public.reject_consultation_context_mutation();

DROP TRIGGER IF EXISTS trg_consultation_context_shares_append_only ON public.consultation_context_shares;
CREATE TRIGGER trg_consultation_context_shares_append_only BEFORE DELETE OR UPDATE ON public.consultation_context_shares FOR EACH ROW EXECUTE FUNCTION public.reject_consultation_context_mutation();

DROP TRIGGER IF EXISTS triage_completed_snapshot_guard_trg ON public.triage_sessions;
CREATE TRIGGER triage_completed_snapshot_guard_trg BEFORE INSERT OR DELETE OR UPDATE ON public.triage_sessions FOR EACH ROW EXECUTE FUNCTION public.carebridge_guard_completed_triage_snapshot();

DROP TRIGGER IF EXISTS triage_session_evidence_immutable_trg ON public.triage_session_evidence;
CREATE TRIGGER triage_session_evidence_immutable_trg BEFORE DELETE OR UPDATE ON public.triage_session_evidence FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();


-- ============================================================================

-- ============================================================================
-- 8. Initialization validation
-- ============================================================================
DO $init_validation$
DECLARE
    expected_tables text[] := ARRAY[
        'account_deletion_requests',
        'administrative_areas',
        'ai_content_assessments',
        'ai_content_scan_jobs',
        'ai_moderation_policies',
        'account_lock_appeals',
        'archived_records',
        'attachments',
        'audit_events',
        'auth_challenges',
        'auth_sessions',
        'care_facilities',
        'care_group_members',
        'care_groups',
        'care_item_templates',
        'care_subjects',
        'care_tasks',
        'community_content',
        'community_interactions',
        'community_topics',
        'consultation_bookings',
        'consultation_context_citations',
        'consultation_context_shares',
        'consultation_sessions',
        'content_item_sources',
        'content_item_topics',
        'content_items',
        'conversation_calls',
        'data_permissions',
        'development_milestones',
        'device_connections',
        'device_tokens',
        'direct_conversations',
        'direct_messages',
        'expense_entries',
        'expert_availability',
        'expert_consultation_requests',
        'expert_location_shares',
        'growth_measurements',
        'health_context_memories',
        'health_observations',
        'health_records',
        'knowledge_source_reviews',
        'knowledge_sources',
        'maternal_exercise_sessions',
        'moderation_cases',
        'mother_journeys',
        'notification_records',
        'partner_organizations',
        'preparation_checklist_items',
        'professional_specialties',
        'red_flag_rules',
        'safety_configs',
        'safety_events',
        'safety_monitoring_sessions',
        'specialties',
        'system_configurations',
        'triage_session_evidence',
        'triage_sessions',
        'users',
        'vaccination_records',
        'vaccination_schedules'
    ];
    expected_views text[] := ARRAY[
        'care_logs',
        'emergency_contacts',
        'expert_credentials',
        'nearby_support_interactions'
    ];
    missing_tables text;
    missing_views text;
BEGIN
    SELECT string_agg(item.name, ', ' ORDER BY item.name)
      INTO missing_tables
      FROM unnest(expected_tables) AS item(name)
     WHERE to_regclass(format('public.%I', item.name)) IS NULL;

    IF missing_tables IS NOT NULL THEN
        RAISE EXCEPTION 'INIT_MISSING_TABLES: %', missing_tables;
    END IF;

    SELECT string_agg(item.name, ', ' ORDER BY item.name)
      INTO missing_views
      FROM unnest(expected_views) AS item(name)
     WHERE NOT EXISTS (
         SELECT 1
           FROM information_schema.views view_info
          WHERE view_info.table_schema = 'public'
            AND view_info.table_name = item.name
     );

    IF missing_views IS NOT NULL THEN
        RAISE EXCEPTION 'INIT_MISSING_VIEWS: %', missing_views;
    END IF;

    RAISE NOTICE
        'INIT_COMPLETE: % canonical tables and % compatibility views are available',
        cardinality(expected_tables),
        cardinality(expected_views);
END
$init_validation$;

COMMIT;

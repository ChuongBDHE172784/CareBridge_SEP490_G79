-- CareBridge canonical 70-table baseline (69 business tables; Flyway creates history).
-- Source: clean PostgreSQL 18.1 scratch database after the complete V migration chain.
-- Reference-data whitelist: administrative_areas, care_facilities, care_item_templates,
-- community_topics, knowledge_sources, red_flag_rules, specialties, vaccination_schedules.

--
-- PostgreSQL database dump
--


-- Dumped from database version 18.1 (Homebrew)
-- Dumped by pg_dump version 18.1 (Homebrew)


--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA IF NOT EXISTS public;


--
-- Name: carebridge_reject_mutation(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.carebridge_reject_mutation() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION 'IMMUTABLE_TABLE: %.% does not allow UPDATE or DELETE', TG_TABLE_SCHEMA, TG_TABLE_NAME;
END
$$;


--
-- Name: enforce_community_topic_parent_category(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.enforce_community_topic_parent_category() RETURNS trigger
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


--
-- Name: enforce_mother_journey_event_owner(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.enforce_mother_journey_event_owner() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE journey_owner uuid;
BEGIN
  IF NEW.mother_journey_id IS NULL THEN
    RETURN NEW;
  END IF;

  SELECT owner_user_id INTO journey_owner
    FROM public.mother_journeys
   WHERE journey_id=NEW.mother_journey_id;

  IF journey_owner IS NULL OR NEW.owner_user_id<>journey_owner THEN
    RAISE EXCEPTION 'mother journey event owner must match journey owner';
  END IF;
  IF NEW.legacy_source='PREGNANCY_OUTCOME'
     AND (NEW.actor_user_id IS NULL OR NEW.actor_user_id<>journey_owner) THEN
    RAISE EXCEPTION 'pregnancy outcome actor must match journey owner';
  END IF;
  RETURN NEW;
END
$$;


--
-- Name: enforce_pregnancy_outcome_evidence_owner(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.enforce_pregnancy_outcome_evidence_owner() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    journey_owner UUID;
BEGIN
    SELECT owner_user_id
      INTO journey_owner
      FROM mother_journeys
     WHERE journey_id = NEW.journey_id;

    IF journey_owner IS NULL
       OR NEW.owner_user_id <> journey_owner
       OR NEW.actor_user_id <> journey_owner THEN
        RAISE EXCEPTION 'pregnancy outcome evidence owner must match journey owner';
    END IF;

    RETURN NEW;
END;
$$;


--
-- Name: fill_contribution_profile(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fill_contribution_profile() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
  IF NEW.professional_profile_id IS NULL THEN
    SELECT professional_profile_id INTO NEW.professional_profile_id
      FROM public.professional_profiles WHERE user_id=NEW.actor_user_id;
  END IF;
  RETURN NEW;
END $$;


--
-- Name: reject_mother_journey_transition_mutation(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.reject_mother_journey_transition_mutation() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF current_setting('carebridge.allow_transition_mutation', true) = 'on' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'mother_journey_transitions is append-only';
END;
$$;


--
-- Name: reject_pregnancy_outcome_evidence_mutation(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.reject_pregnancy_outcome_evidence_mutation() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF current_setting('carebridge.allow_outcome_mutation', true) = 'on' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'pregnancy outcome evidence is append-only';
END;
$$;




--
-- Name: account_deletion_requests; Type: TABLE; Schema: public; Owner: -
--

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
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: administrative_areas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.administrative_areas (
    administrative_area_id uuid DEFAULT gen_random_uuid() NOT NULL,
    parent_area_id uuid,
    area_type character varying(30) NOT NULL,
    code character varying(80) NOT NULL,
    name character varying(255) NOT NULL,
    legacy_code character varying(80),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: archived_consultation_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archived_consultation_records (
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
    configured_by uuid,
    channel_type character varying(30),
    duration_minutes smallint,
    specialty_scope character varying(100),
    minimum_price numeric,
    maximum_price numeric,
    commission_rate numeric,
    currency character varying(10),
    effective_from timestamp with time zone,
    effective_to timestamp with time zone,
    status character varying(30),
    updated_at timestamp with time zone,
    expert_profile_id uuid,
    price_band_id uuid,
    price_amount numeric,
    cancellation_policy text,
    version_no integer,
    requester_user_id uuid,
    availability_id uuid,
    expert_price_id uuid,
    shared_summary_id uuid,
    topic character varying(500),
    scheduled_start timestamp with time zone,
    scheduled_end timestamp with time zone,
    price_snapshot_amount numeric,
    commission_rate_snapshot numeric,
    cancellation_policy_snapshot text,
    price_locked_at timestamp with time zone,
    booking_id uuid,
    communication_room_id character varying(255),
    started_at timestamp with time zone,
    ended_at timestamp with time zone,
    session_status character varying(30),
    expert_summary text,
    technical_log_json jsonb
);


--
-- Name: archived_partner_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archived_partner_records (
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
    name character varying(200),
    organization_type character varying(20),
    address character varying(500),
    city character varying(100),
    phone character varying(20),
    email character varying(255),
    website character varying(500),
    logo_url character varying(1000),
    description character varying(2000),
    organization_status character varying(30),
    representative_user_id uuid,
    updated_at timestamp with time zone,
    CONSTRAINT archived_partner_shape_ck CHECK ((((legacy_table)::text <> 'partner_organizations'::text) OR ((name IS NOT NULL) AND ((organization_type)::text = ANY ((ARRAY['CLINIC'::character varying, 'HOSPITAL'::character varying, 'NGO'::character varying, 'COMPANY'::character varying])::text[])) AND (address IS NOT NULL) AND (city IS NOT NULL) AND (phone IS NOT NULL) AND (email IS NOT NULL) AND ((organization_status)::text = ANY ((ARRAY['PENDING_APPROVAL'::character varying, 'APPROVED'::character varying, 'SUSPENDED'::character varying, 'REJECTED'::character varying])::text[])) AND (representative_user_id IS NOT NULL) AND (original_created_at IS NOT NULL))))
);


--
-- Name: archived_realtime_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archived_realtime_records (
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
    conversation_id uuid,
    mother_user_id uuid,
    expert_user_id uuid,
    status character varying(20),
    last_activity_at timestamp with time zone,
    mother_last_read_at timestamp with time zone,
    mother_last_read_message_id uuid,
    expert_last_read_at timestamp with time zone,
    expert_last_read_message_id uuid,
    sender_user_id uuid,
    client_message_id uuid,
    message_type character varying(30),
    message_body text,
    initiated_by_user_id uuid,
    call_type character varying(10),
    call_status character varying(20),
    zego_room_id character varying(255),
    initiated_at timestamp with time zone,
    answered_at timestamp with time zone,
    ended_at timestamp with time zone,
    duration_seconds integer,
    CONSTRAINT archived_realtime_call_shape_ck CHECK ((((legacy_table)::text <> 'conversation_calls'::text) OR ((conversation_id IS NOT NULL) AND (initiated_by_user_id IS NOT NULL) AND ((call_type)::text = ANY ((ARRAY['VOICE'::character varying, 'VIDEO'::character varying])::text[])) AND ((call_status)::text = ANY ((ARRAY['INITIATED'::character varying, 'RINGING'::character varying, 'ANSWERED'::character varying, 'DECLINED'::character varying, 'MISSED'::character varying, 'CANCELLED'::character varying, 'ENDED'::character varying, 'FAILED'::character varying])::text[])) AND (zego_room_id IS NOT NULL) AND (initiated_at IS NOT NULL) AND (original_created_at IS NOT NULL) AND ((duration_seconds IS NULL) OR (duration_seconds >= 0)) AND ((answered_at IS NULL) OR (answered_at >= initiated_at)) AND ((ended_at IS NULL) OR (ended_at >= initiated_at)) AND (((call_status)::text <> 'ENDED'::text) OR (answered_at IS NOT NULL))))),
    CONSTRAINT archived_realtime_conversation_shape_ck CHECK ((((legacy_table)::text <> 'direct_conversations'::text) OR ((mother_user_id IS NOT NULL) AND (expert_user_id IS NOT NULL) AND ((status)::text = 'ACTIVE'::text) AND (original_created_at IS NOT NULL) AND ((last_activity_at IS NULL) OR (last_activity_at >= original_created_at))))),
    CONSTRAINT archived_realtime_message_shape_ck CHECK ((((legacy_table)::text <> 'direct_messages'::text) OR ((conversation_id IS NOT NULL) AND (sender_user_id IS NOT NULL) AND (client_message_id IS NOT NULL) AND ((message_type)::text = 'TEXT'::text) AND ((length(btrim(message_body)) >= 1) AND (length(btrim(message_body)) <= 2000)) AND (original_created_at IS NOT NULL))))
);


--
-- Name: attachments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attachments (
    attachment_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL,
    storage_key character varying(500) NOT NULL,
    original_name character varying(255) NOT NULL,
    mime_type character varying(100) NOT NULL,
    file_size_bytes bigint NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    checksum character varying(128),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT attachments_file_size_bytes_check CHECK ((file_size_bytes >= 0))
);


--
-- Name: audit_events; Type: TABLE; Schema: public; Owner: -
--

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
    security_event_id bigint,
    note_text text,
    event_origin character varying(40) DEFAULT 'AUDIT_LOG'::character varying NOT NULL,
    ip_address character varying(80),
    CONSTRAINT audit_events_security_note_text_ck CHECK ((((event_category)::text <> 'SECURITY_INVESTIGATION_NOTE'::text) OR ((security_event_id IS NOT NULL) AND (length(TRIM(BOTH FROM note_text)) > 0))))
);


--
-- Name: auth_challenges; Type: TABLE; Schema: public; Owner: -
--

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
    CONSTRAINT auth_challenges_status_ck CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'VERIFIED'::character varying, 'USED'::character varying, 'EXPIRED'::character varying, 'REVOKED'::character varying])::text[])))
);


--
-- Name: auth_revocations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auth_revocations (
    revocation_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    session_id uuid,
    token_family_id uuid,
    token_hash character varying(255),
    reason character varying(100) NOT NULL,
    revoked_at timestamp with time zone NOT NULL,
    expires_at timestamp with time zone,
    detected_reuse boolean DEFAULT false NOT NULL,
    metadata_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    legacy_source character varying(40),
    legacy_id character varying(100)
);


--
-- Name: auth_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auth_sessions (
    session_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token_family_id uuid NOT NULL,
    device_identifier character varying(255) NOT NULL,
    device_name character varying(255),
    refresh_token_hash character varying(255),
    issued_at timestamp with time zone NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    last_used_at timestamp with time zone,
    rotated_at timestamp with time zone,
    revoked_at timestamp with time zone,
    revoke_reason character varying(100),
    status character varying(30) NOT NULL,
    created_ip_hash character varying(255),
    user_agent_hash character varying(255),
    legacy_source character varying(40),
    legacy_id character varying(100),
    CONSTRAINT auth_sessions_status_ck CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'ROTATED'::character varying, 'REVOKED'::character varying, 'EXPIRED'::character varying])::text[])))
);


--
-- Name: care_facilities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.care_facilities (
    facility_id uuid DEFAULT gen_random_uuid() NOT NULL,
    partner_id uuid,
    name character varying(255) NOT NULL,
    facility_type character varying(50),
    address character varying(500),
    latitude numeric,
    longitude numeric,
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
    CONSTRAINT care_facilities_ownership_type_check CHECK (((ownership_type IS NULL) OR ((ownership_type)::text = ANY ((ARRAY['PUBLIC'::character varying, 'MILITARY'::character varying])::text[])))),
    CONSTRAINT care_facilities_searchable_coordinates_check CHECK (((is_searchable = false) OR ((latitude IS NOT NULL) AND (longitude IS NOT NULL)))),
    CONSTRAINT care_facilities_source_type_check CHECK (((source_type IS NULL) OR ((source_type)::text = ANY ((ARRAY['MANUAL'::character varying, 'TRACKASIA'::character varying, 'LEGACY_IMPORT'::character varying])::text[]))))
);


--
-- Name: care_group_members; Type: TABLE; Schema: public; Owner: -
--

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
    data_permission_id uuid
);


--
-- Name: care_groups; Type: TABLE; Schema: public; Owner: -
--

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
    care_subject_id uuid
);


--
-- Name: care_item_templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.care_item_templates (
    template_id uuid DEFAULT gen_random_uuid() NOT NULL,
    parent_template_id uuid,
    entry_type character varying(30) NOT NULL,
    title character varying(255) NOT NULL,
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
    confidence_threshold numeric,
    feedback_level character varying(30),
    content_status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    is_required boolean,
    CONSTRAINT care_item_templates_type_ck CHECK (((entry_type)::text = ANY ((ARRAY['TEMPLATE_ROOT'::character varying, 'CHECKLIST_ENTRY'::character varying, 'EXERCISE_TEMPLATE'::character varying, 'POSTURE_CONFIG'::character varying])::text[]))),
    CONSTRAINT chk_care_item_templates_posture_confidence_threshold CHECK ((((entry_type)::text <> 'POSTURE_CONFIG'::text) OR (confidence_threshold IS NULL) OR ((confidence_threshold >= 0.0) AND (confidence_threshold <= 1.0))))
);


--
-- Name: care_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.care_logs (
    care_log_id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_subject_id uuid NOT NULL,
    log_type character varying(40) NOT NULL,
    started_at timestamp with time zone,
    ended_at timestamp with time zone,
    quantity numeric,
    unit character varying(30),
    note text,
    recorded_by uuid,
    status character varying(30) NOT NULL,
    payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: care_subjects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.care_subjects (
    care_subject_id uuid DEFAULT gen_random_uuid() NOT NULL,
    person_id uuid NOT NULL,
    owner_user_id uuid NOT NULL,
    mother_journey_id uuid,
    subject_type character varying(30) NOT NULL,
    nickname character varying(200),
    birth_date date,
    sex character varying(30),
    birth_weight_kg numeric(6,3),
    birth_length_cm numeric(6,2),
    status character varying(30) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT care_subjects_type_ck CHECK (((subject_type)::text = ANY ((ARRAY['MOTHER'::character varying, 'BABY'::character varying, 'DEPENDENT'::character varying])::text[])))
);


--
-- Name: community_content; Type: TABLE; Schema: public; Owner: -
--

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
    CONSTRAINT community_content_type_ck CHECK (((content_type)::text = ANY ((ARRAY['QUESTION'::character varying, 'ANSWER'::character varying, 'POST'::character varying])::text[])))
);


--
-- Name: community_interactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.community_interactions (
    interaction_id uuid DEFAULT gen_random_uuid() NOT NULL,
    actor_user_id uuid NOT NULL,
    interaction_type character varying(30) NOT NULL,
    content_id uuid,
    topic_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    target_content_type character varying(20),
    CONSTRAINT community_interactions_one_target_ck CHECK (((content_id IS NOT NULL) <> (topic_id IS NOT NULL))),
    CONSTRAINT community_interactions_type_ck CHECK (((interaction_type)::text = ANY ((ARRAY['REACTION'::character varying, 'BOOKMARK'::character varying, 'FOLLOW'::character varying, 'MUTE'::character varying])::text[])))
);


--
-- Name: community_profiles; Type: TABLE; Schema: public; Owner: -
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
-- Name: community_topics; Type: TABLE; Schema: public; Owner: -
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
    created_by uuid,
    type character varying(20) DEFAULT 'TOPIC'::character varying NOT NULL,
    slug character varying(140) NOT NULL,
    parent_id uuid,
    CONSTRAINT community_topics_parent_rule_check_v2 CHECK (((((type)::text = 'CATEGORY'::text) AND (parent_id IS NULL)) OR (((type)::text = 'TOPIC'::text) AND (parent_id IS NOT NULL)) OR (((type)::text = 'TAG'::text) AND (parent_id IS NULL)))),
    CONSTRAINT community_topics_type_check CHECK (((type)::text = ANY ((ARRAY['TOPIC'::character varying, 'CATEGORY'::character varying, 'TAG'::character varying])::text[])))
);


--
-- Name: content_item_sources; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.content_item_sources (
    content_item_source_id uuid DEFAULT gen_random_uuid() NOT NULL,
    content_item_id uuid NOT NULL,
    knowledge_source_id uuid,
    source_title character varying(500) NOT NULL,
    source_url character varying(2000),
    source_publisher character varying(255),
    source_snapshot_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: content_item_topics; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.content_item_topics (
    content_item_id uuid NOT NULL,
    topic_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: content_items; Type: TABLE; Schema: public; Owner: -
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
-- Name: data_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.data_permissions (
    permission_id uuid DEFAULT gen_random_uuid() NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone,
    granted_at timestamp(6) with time zone,
    grantee_user_id uuid,
    owner_user_id uuid,
    purpose character varying(255),
    revoked_at timestamp(6) with time zone,
    scope_reference_id uuid,
    scope_type character varying(50),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    updated_at timestamp(6) with time zone,
    permission_series_id uuid,
    version_number integer,
    supersedes_permission_id uuid,
    revoked_by uuid,
    policy_version character varying(80),
    consent_evidence_key character varying(255),
    legacy_consent_id bigint NOT NULL,
    permission_kind character varying(30) DEFAULT 'DATA_PERMISSION'::character varying NOT NULL,
    recipient character varying(120),
    scope_text text,
    evidence_key uuid,
    locale character varying(20)
);


--
-- Name: data_permissions_legacy_consent_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.data_permissions ALTER COLUMN legacy_consent_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.data_permissions_legacy_consent_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: development_milestones; Type: TABLE; Schema: public; Owner: -
--

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
    care_subject_id uuid NOT NULL
);


--
-- Name: device_connections; Type: TABLE; Schema: public; Owner: -
--

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
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: device_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_tokens (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token character varying(512) NOT NULL,
    platform character varying(30) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT device_tokens_platform_check CHECK (((platform)::text = ANY (ARRAY[('ANDROID'::character varying)::text, ('IOS'::character varying)::text, ('WEB'::character varying)::text])))
);


--
-- Name: emergency_contacts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.emergency_contacts (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    name character varying(120) NOT NULL,
    phone character varying(32) NOT NULL,
    relationship character varying(80),
    primary_contact boolean DEFAULT true NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    updated_by uuid NOT NULL
);


--
-- Name: expense_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expense_entries (
    expense_entry_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL,
    care_subject_id uuid,
    mother_journey_id uuid,
    category character varying(80),
    amount numeric NOT NULL,
    currency character varying(10) DEFAULT 'VND'::character varying NOT NULL,
    expense_date date NOT NULL,
    note text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: expert_availability; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expert_availability (
    availability_id uuid DEFAULT gen_random_uuid() NOT NULL,
    start_at timestamp with time zone NOT NULL,
    end_at timestamp with time zone NOT NULL,
    channel_type character varying(30) NOT NULL,
    status character varying(20) DEFAULT 'AVAILABLE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    professional_profile_id uuid NOT NULL
);


--
-- Name: expert_contribution_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expert_contribution_events (
    contribution_event_id uuid DEFAULT gen_random_uuid() NOT NULL,
    professional_profile_id uuid,
    actor_user_id uuid NOT NULL,
    points integer NOT NULL,
    reason text NOT NULL,
    source_type character varying(60),
    source_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: expert_credentials; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expert_credentials (
    credential_id uuid DEFAULT gen_random_uuid() NOT NULL,
    credential_type character varying(50) NOT NULL,
    credential_number character varying(100),
    issuer character varying(200),
    issued_date date,
    expiry_date date,
    file_url text,
    review_status character varying(30) DEFAULT 'PENDING'::character varying NOT NULL,
    review_note text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    reviewed_by uuid,
    reviewed_at timestamp without time zone,
    file_id uuid,
    professional_profile_id uuid NOT NULL
);


--
-- Name: expert_location_shares; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.expert_location_shares (
    location_share_id uuid DEFAULT gen_random_uuid() NOT NULL,
    latitude numeric NOT NULL,
    longitude numeric NOT NULL,
    accuracy_meters numeric,
    availability_status character varying(20),
    shared_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone,
    consent_reference uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    professional_profile_id uuid NOT NULL
);


--
-- Name: family_tasks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.family_tasks (
    task_id uuid DEFAULT gen_random_uuid() NOT NULL,
    care_group_id uuid NOT NULL,
    creator_user_id uuid,
    assignee_user_id uuid,
    care_subject_id uuid,
    title character varying(255) NOT NULL,
    description text,
    due_at timestamp with time zone,
    status character varying(30) DEFAULT 'OPEN'::character varying NOT NULL,
    completed_at timestamp with time zone,
    cancelled_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: growth_measurements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.growth_measurements (
    growth_measurement_id uuid DEFAULT gen_random_uuid() NOT NULL,
    baby_id uuid NOT NULL,
    measured_date date NOT NULL,
    weight_kg numeric,
    height_cm numeric,
    head_circumference_cm numeric,
    source_type character varying(30),
    note text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    care_subject_id uuid NOT NULL
);


--
-- Name: health_context_memories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.health_context_memories (
    memory_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    care_subject_id uuid,
    triage_session_id uuid,
    related_stage character varying(30) NOT NULL,
    summary_text text NOT NULL,
    memory_payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone,
    deleted_at timestamp with time zone,
    mother_profile_id uuid,
    baby_profile_id uuid
);


--
-- Name: health_observations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.health_observations (
    health_observation_id uuid DEFAULT gen_random_uuid() NOT NULL,
    device_connection_id uuid,
    care_subject_id uuid,
    observation_type character varying(50) NOT NULL,
    value_numeric numeric,
    value_secondary numeric,
    unit character varying(30),
    observed_at timestamp with time zone NOT NULL,
    source_record_id uuid,
    quality_label character varying(30),
    raw_payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: health_record_attachments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.health_record_attachments (
    health_record_attachment_id uuid DEFAULT gen_random_uuid() NOT NULL,
    health_record_id uuid NOT NULL,
    attachment_id uuid NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: health_records; Type: TABLE; Schema: public; Owner: -
--

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
    summary_json jsonb
);


--
-- Name: knowledge_source_reviews; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.knowledge_source_reviews (
    review_id uuid DEFAULT gen_random_uuid() NOT NULL,
    knowledge_source_id uuid NOT NULL,
    previous_status character varying(30),
    new_status character varying(30) NOT NULL,
    actor_user_id uuid,
    actor_role character varying(80),
    notes text,
    changed_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: knowledge_sources; Type: TABLE; Schema: public; Owner: -
--

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
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: maternal_exercise_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.maternal_exercise_sessions (
    exercise_session_id uuid DEFAULT gen_random_uuid() NOT NULL,
    mother_journey_id uuid,
    owner_user_id uuid NOT NULL,
    exercise_template_id uuid NOT NULL,
    posture_config_id uuid,
    started_at timestamp with time zone NOT NULL,
    ended_at timestamp with time zone,
    paused_seconds integer DEFAULT 0 NOT NULL,
    completion_percent numeric(5,2),
    posture_score numeric(6,3),
    session_status character varying(30) NOT NULL,
    warning_count integer DEFAULT 0 NOT NULL,
    summary_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    safety_observation_id uuid
);


--
-- Name: maternal_observations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.maternal_observations (
    observation_id uuid DEFAULT gen_random_uuid() NOT NULL,
    observation_type character varying(60) NOT NULL,
    mother_journey_id uuid,
    exercise_session_id uuid,
    numeric_value numeric,
    secondary_numeric_value numeric,
    unit character varying(40),
    text_value text,
    severity character varying(30),
    observed_at timestamp with time zone NOT NULL,
    payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    schema_version character varying(30) DEFAULT '1'::character varying NOT NULL,
    source_type character varying(60) NOT NULL,
    legacy_source character varying(60),
    legacy_id character varying(100),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    source_reference_id uuid,
    record_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    observation_date date,
    submission_id uuid,
    mood_level smallint,
    breastfeeding_note text,
    exercise_template_id uuid,
    owner_user_id uuid,
    check_code character varying(100),
    response_boolean boolean,
    blocked_boolean boolean,
    event_time_ms bigint,
    posture_config_id uuid,
    posture_code character varying(80)
);


--
-- Name: moderation_cases; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.moderation_cases (
    moderation_case_id uuid DEFAULT gen_random_uuid() NOT NULL,
    reporter_user_id uuid,
    assigned_moderator_id uuid,
    target_type character varying(40) NOT NULL,
    target_id uuid NOT NULL,
    reason_code character varying(80),
    description text,
    status character varying(30) DEFAULT 'OPEN'::character varying NOT NULL,
    opened_at timestamp with time zone DEFAULT now() NOT NULL,
    resolved_at timestamp with time zone,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    report_source character varying(20) DEFAULT 'USER'::character varying NOT NULL,
    reverted_at timestamp with time zone,
    reverted_by uuid
);


--
-- Name: moderation_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.moderation_events (
    moderation_event_id uuid DEFAULT gen_random_uuid() NOT NULL,
    moderation_case_id uuid,
    moderator_user_id uuid,
    action_type character varying(40) NOT NULL,
    target_type character varying(40) NOT NULL,
    target_id uuid NOT NULL,
    reason text,
    expires_at timestamp with time zone,
    action_at timestamp with time zone DEFAULT now() NOT NULL,
    event_payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL
);


--
-- Name: mother_journey_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.mother_journey_events (
    event_id uuid DEFAULT gen_random_uuid() NOT NULL,
    mother_journey_id uuid,
    owner_user_id uuid NOT NULL,
    event_type character varying(60) NOT NULL,
    from_stage character varying(40),
    to_stage character varying(40),
    event_payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    schema_version character varying(30) DEFAULT '1'::character varying NOT NULL,
    actor_user_id uuid,
    effective_at timestamp with time zone NOT NULL,
    recorded_at timestamp with time zone NOT NULL,
    journey_version bigint,
    legacy_source character varying(60),
    legacy_id character varying(100),
    submission_id uuid,
    event_source character varying(30),
    confidence character varying(20),
    reason character varying(500),
    lifecycle_goal character varying(40),
    locale character varying(20),
    time_zone character varying(80),
    preferences character varying(300),
    outcome_type character varying(30),
    outcome_date date,
    revision_number integer,
    supersedes_evidence_id uuid,
    semantic_hash character varying(500),
    correction boolean,
    operation_type character varying(30),
    semantic_intent character varying(1000),
    care_subject_id uuid
);


--
-- Name: mother_journeys; Type: TABLE; Schema: public; Owner: -
--

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
    CONSTRAINT chk_mother_journeys_date_confidence CHECK (((date_confidence IS NULL) OR ((date_confidence)::text = ANY ((ARRAY['CONFIRMED'::character varying, 'ESTIMATED'::character varying, 'UNKNOWN'::character varying])::text[])))),
    CONSTRAINT chk_mother_journeys_date_source CHECK (((date_source IS NULL) OR ((date_source)::text = ANY ((ARRAY['SELF_REPORTED'::character varying, 'CLINICIAN_CONFIRMED'::character varying, 'ULTRASOUND'::character varying, 'SYSTEM_DERIVED'::character varying, 'MIGRATION'::character varying, 'UNKNOWN'::character varying])::text[])))),
    CONSTRAINT ck_mother_journey_live_birth_date CHECK ((((pregnancy_outcome)::text <> 'LIVE_BIRTH'::text) OR (pregnancy_outcome_date IS NOT NULL))),
    CONSTRAINT ck_mother_journey_pregnancy_outcome CHECK (((pregnancy_outcome IS NULL) OR ((pregnancy_outcome)::text = ANY ((ARRAY['ONGOING'::character varying, 'UNKNOWN'::character varying, 'LIVE_BIRTH'::character varying, 'PREGNANCY_LOSS'::character varying, 'STILLBIRTH'::character varying])::text[]))))
);


--
-- Name: nearby_support_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nearby_support_requests (
    request_id uuid DEFAULT gen_random_uuid() NOT NULL,
    requester_user_id uuid NOT NULL,
    support_type character varying(50) NOT NULL,
    description text,
    latitude numeric(10,8) NOT NULL,
    longitude numeric(11,8) NOT NULL,
    consent_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    responded_at timestamp with time zone,
    completed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    care_subject_id uuid,
    CONSTRAINT nearby_support_requests_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'ACCEPTED'::character varying, 'CANCELLED'::character varying, 'COMPLETED'::character varying])::text[])))
);


--
-- Name: nearby_support_responses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nearby_support_responses (
    response_id uuid DEFAULT gen_random_uuid() NOT NULL,
    request_id uuid NOT NULL,
    expert_profile_id uuid NOT NULL,
    action character varying(20) NOT NULL,
    note text,
    responded_at timestamp with time zone DEFAULT now() NOT NULL,
    professional_profile_id uuid,
    CONSTRAINT nearby_support_responses_action_check CHECK (((action)::text = ANY ((ARRAY['ACCEPT'::character varying, 'DECLINE'::character varying, 'STOP'::character varying])::text[])))
);


--
-- Name: notification_records; Type: TABLE; Schema: public; Owner: -
--

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
    CONSTRAINT notification_records_channel_check CHECK (((channel)::text = ANY ((ARRAY['PUSH'::character varying, 'EMAIL'::character varying, 'IN_APP'::character varying])::text[]))),
    CONSTRAINT notification_records_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'SENT'::character varying, 'DELIVERED'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT notification_records_type_check CHECK (((type)::text = ANY ((ARRAY['REMINDER'::character varying, 'COMMUNITY_REPLY'::character varying, 'CONSULTATION'::character varying, 'EMERGENCY'::character varying, 'MESSAGE'::character varying, 'GROUP_INVITE'::character varying])::text[])))
);


--
-- Name: persons; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.persons (
    person_id uuid DEFAULT gen_random_uuid() NOT NULL,
    display_name character varying(200),
    date_of_birth date,
    phone_number character varying(40),
    avatar_url text,
    area character varying(200),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: preparation_checklist_items; Type: TABLE; Schema: public; Owner: -
--

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
    category character varying(50) DEFAULT 'GENERAL'::character varying NOT NULL
);


--
-- Name: professional_profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.professional_profiles (
    professional_profile_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    professional_title character varying(150),
    workplace character varying(200),
    experience_years smallint,
    consultation_scope text,
    verification_status character varying(30) DEFAULT 'PENDING'::character varying NOT NULL,
    verified_at timestamp with time zone,
    verified_by uuid,
    rating_avg numeric,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    specialty character varying(100),
    facility_id uuid,
    trust_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL
);


--
-- Name: professional_specialties; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.professional_specialties (
    professional_profile_id uuid NOT NULL,
    specialty_id uuid NOT NULL,
    is_primary boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: red_flag_rules; Type: TABLE; Schema: public; Owner: -
--

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
    CONSTRAINT chk_red_flag_rules_action CHECK (((action)::text = ANY ((ARRAY['BLOCK'::character varying, 'WARN'::character varying, 'ESCALATE'::character varying])::text[]))),
    CONSTRAINT chk_red_flag_rules_severity CHECK (((severity)::text = ANY ((ARRAY['GREEN'::character varying, 'YELLOW'::character varying, 'RED'::character varying])::text[])))
);


--
-- Name: safety_configs; Type: TABLE; Schema: public; Owner: -
--

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
    CONSTRAINT safety_configs_countdown_seconds_check CHECK ((countdown_seconds = ANY (ARRAY[15, 30, 60]))),
    CONSTRAINT safety_configs_sensor_permission_ck CHECK (((sensor_permission_granted = false) OR (sensor_permission_recorded_at IS NOT NULL)))
);


--
-- Name: safety_event_actions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.safety_event_actions (
    safety_event_action_id uuid DEFAULT gen_random_uuid() NOT NULL,
    safety_event_id uuid,
    action_type character varying(40) NOT NULL,
    recipient_user_id uuid,
    device_identifier character varying(255),
    notification_record_id uuid,
    care_facility_id uuid,
    attempt_number integer DEFAULT 1 NOT NULL,
    idempotency_key character varying(255) NOT NULL,
    response_type character varying(30),
    delivery_status character varying(30),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    delivered_at timestamp with time zone,
    owner_user_id uuid,
    context_type character varying(50),
    context_id uuid,
    latitude numeric(10,8),
    longitude numeric(11,8),
    accuracy_meters numeric(6,2),
    captured_at timestamp with time zone,
    expires_at timestamp with time zone,
    consent_status character varying(20),
    device_token_id uuid,
    fcm_message_id character varying(255),
    failure_code character varying(120),
    reason character varying(500),
    responded_at timestamp with time zone,
    created_by_user_id uuid,
    actor_type character varying(20),
    attempt_status character varying(20),
    started_at timestamp with time zone,
    completed_at timestamp with time zone,
    lease_expires_at timestamp with time zone,
    successful_recipient_count integer,
    failed_recipient_count integer,
    recipient_count integer,
    location_included boolean,
    created_by_text character varying(50),
    triage_handoff_id uuid,
    risk_level character varying(20),
    summary text,
    action_status character varying(20),
    updated_at timestamp with time zone,
    CONSTRAINT safety_event_actions_attempt_ck CHECK ((attempt_number >= 0)),
    CONSTRAINT safety_event_actions_parent_ck CHECK ((((action_type)::text = ANY ((ARRAY['MAP_HANDOFF'::character varying, 'LOCATION_SNAPSHOT'::character varying])::text[])) OR (safety_event_id IS NOT NULL))),
    CONSTRAINT safety_event_actions_type_ck CHECK (((action_type)::text = ANY ((ARRAY['RESPONSE'::character varying, 'DELIVERY'::character varying, 'FAMILY_ALERT'::character varying, 'ALERT_ATTEMPT'::character varying, 'MAP_HANDOFF'::character varying, 'LOCATION_SNAPSHOT'::character varying])::text[])))
);


--
-- Name: safety_events; Type: TABLE; Schema: public; Owner: -
--

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
    record_type character varying(30) DEFAULT 'IMU_EVENT'::character varying NOT NULL,
    magnitude numeric(10,4),
    user_latitude numeric(10,8),
    user_longitude numeric(11,8),
    client_detected_at timestamp with time zone,
    resolved_at timestamp with time zone,
    notes text,
    signal_key character varying(200),
    countdown_deadline_at timestamp with time zone,
    response_reason character varying(500),
    escalation_started_at timestamp with time zone,
    emergency_session_id uuid,
    created_by_text character varying(50),
    created_by_user_id uuid,
    CONSTRAINT safety_events_record_type_ck CHECK (((record_type)::text = ANY ((ARRAY['IMU_EVENT'::character varying, 'EMERGENCY_SESSION'::character varying])::text[])))
);


--
-- Name: safety_monitoring_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.safety_monitoring_sessions (
    monitoring_session_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    sensitivity_level character varying(10) DEFAULT 'MEDIUM'::character varying NOT NULL,
    started_at timestamp with time zone DEFAULT now() NOT NULL,
    ended_at timestamp with time zone,
    created_by uuid
);


--
-- Name: scheduled_care_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scheduled_care_items (
    care_item_id uuid DEFAULT gen_random_uuid() NOT NULL,
    owner_user_id uuid NOT NULL,
    care_subject_id uuid,
    item_type character varying(40) NOT NULL,
    title character varying(255) NOT NULL,
    scheduled_at timestamp with time zone NOT NULL,
    recurrence_rule character varying(255),
    snoozed_until timestamp with time zone,
    completed_at timestamp with time zone,
    skipped_at timestamp with time zone,
    status character varying(30) DEFAULT 'PENDING'::character varying NOT NULL,
    source_reference_type character varying(60),
    source_reference_id uuid,
    vaccination_record_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    journey_id uuid,
    baby_id uuid,
    recurrence_type character varying(30),
    recurrence_end_date timestamp with time zone,
    fcm_job_id character varying(255),
    CONSTRAINT scheduled_care_items_vaccination_ck CHECK ((((item_type)::text <> 'VACCINATION'::text) OR (vaccination_record_id IS NOT NULL) OR (care_subject_id IS NOT NULL)))
);


--
-- Name: security_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.security_events (
    id bigint NOT NULL,
    details text,
    event_type character varying(80) NOT NULL,
    ip_address character varying(80),
    occurred_at timestamp(6) with time zone CONSTRAINT security_events_timestamp_not_null NOT NULL,
    user_id uuid,
    user_agent character varying(500),
    payload jsonb,
    correlation_id uuid,
    severity character varying(20) DEFAULT 'MEDIUM'::character varying NOT NULL,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    reviewed_by uuid,
    reviewed_at timestamp with time zone,
    CONSTRAINT security_events_event_type_check CHECK (((event_type)::text = ANY (ARRAY[('LOGIN_FAILED'::character varying)::text, ('PERMISSION_DENIED'::character varying)::text, ('SUSPICIOUS_ACTIVITY'::character varying)::text, ('TOKEN_REVOKED'::character varying)::text, ('OTP_ATTEMPT_LIMIT_EXCEEDED'::character varying)::text]))),
    CONSTRAINT security_events_severity_check CHECK (((severity)::text = ANY (ARRAY[('LOW'::character varying)::text, ('MEDIUM'::character varying)::text, ('HIGH'::character varying)::text, ('CRITICAL'::character varying)::text]))),
    CONSTRAINT security_events_status_check CHECK (((status)::text = ANY (ARRAY[('OPEN'::character varying)::text, ('UNDER_REVIEW'::character varying)::text, ('RESOLVED'::character varying)::text, ('FALSE_POSITIVE'::character varying)::text])))
);


--
-- Name: security_events_id_seq; Type: SEQUENCE; Schema: public; Owner: -
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
-- Name: specialties; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.specialties (
    specialty_id uuid CONSTRAINT specialties_canonical_specialty_id_not_null NOT NULL,
    code character varying(80) CONSTRAINT specialties_canonical_code_not_null NOT NULL,
    name character varying(150) CONSTRAINT specialties_canonical_name_not_null NOT NULL,
    description text,
    is_active boolean DEFAULT true CONSTRAINT specialties_canonical_is_active_not_null NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT specialties_canonical_created_at_not_null NOT NULL
);


--
-- Name: system_configurations; Type: TABLE; Schema: public; Owner: -
--

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
    CONSTRAINT system_configurations_api_rate_limit_check CHECK (((api_rate_limit >= 1) AND (api_rate_limit <= 100000))),
    CONSTRAINT system_configurations_connection_timeout_ms_check CHECK (((connection_timeout_ms >= 1000) AND (connection_timeout_ms <= 300000))),
    CONSTRAINT system_configurations_max_upload_size_mb_check CHECK (((max_upload_size_mb >= 1) AND (max_upload_size_mb <= 1024)))
);


--
-- Name: triage_session_evidence; Type: TABLE; Schema: public; Owner: -
--

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
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: triage_sessions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.triage_sessions (
    triage_session_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    care_subject_id uuid,
    stage character varying(30),
    profile_context_id uuid,
    risk_level character varying(20),
    status character varying(30) DEFAULT 'PENDING'::character varying NOT NULL,
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
    structured_created_by character varying(50),
    CONSTRAINT triage_sessions_intensity_ck CHECK (((intensity IS NULL) OR ((intensity)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying])::text[]))))
);


--
-- Name: user_identities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_identities (
    identity_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    provider character varying(20) NOT NULL,
    provider_subject character varying(255) NOT NULL,
    provider_email character varying(255),
    provider_phone character varying(30),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    last_used_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT user_identities_provider_check CHECK (((provider)::text = ANY ((ARRAY['GOOGLE'::character varying, 'PHONE'::character varying])::text[])))
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
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
    CONSTRAINT users_role_check CHECK (((role IS NULL) OR ((role)::text = ANY ((ARRAY['MOTHER'::character varying, 'FAMILY'::character varying, 'EXPERT'::character varying, 'MODERATOR'::character varying, 'CONTENT_ADMIN'::character varying, 'SYSTEM_ADMIN'::character varying, 'PARTNER'::character varying])::text[]))))
);


--
-- Name: vaccination_records; Type: TABLE; Schema: public; Owner: -
--

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
    vaccination_schedule_id uuid
);


--
-- Name: vaccination_schedules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vaccination_schedules (
    vaccination_schedule_id uuid DEFAULT gen_random_uuid() NOT NULL,
    vaccine_name character varying(200) NOT NULL,
    dose_number smallint NOT NULL,
    offset_days integer NOT NULL,
    description text,
    schedule_version character varying(30) DEFAULT '1'::character varying NOT NULL,
    active_from date,
    active_to date,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- PostgreSQL database dump complete
--


--
-- PostgreSQL database dump
--


-- Dumped from database version 18.1 (Homebrew)
-- Dumped by pg_dump version 18.1 (Homebrew)


--
-- Data for Name: administrative_areas; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('05f7f30f-722e-bdbe-28fc-5334b1d5815f', NULL, 'PROVINCE', 'PROVINCE:79', 'TP. Hồ Chí Minh', '79', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('28f33527-9e3d-ba0d-d79d-3371a647f861', NULL, 'PROVINCE', 'PROVINCE:48', 'Đà Nẵng', '48', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('5f68e2cf-d21f-1c69-3fbd-1404b89f26ff', NULL, 'PROVINCE', 'PROVINCE:01', 'Thành phố Hà Nội', '01', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('54173ff1-ab23-321e-3f1d-5a5ca34caba5', NULL, 'PROVINCE', 'PROVINCE:02', 'Thành phố Hồ Chí Minh', '02', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('3a3e6ef6-8822-3757-7451-53003e9b1556', NULL, 'PROVINCE', 'PROVINCE:03', 'Thành phố Hải Phòng', '03', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('d6221f69-9136-cbdb-1739-f86152b80412', NULL, 'PROVINCE', 'PROVINCE:04', 'Thành phố Đà Nẵng', '04', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('be0e0558-806e-f92f-c0a0-8fd47aa870cf', NULL, 'PROVINCE', 'PROVINCE:05', 'Thành phố Cần Thơ', '05', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('9a8a161c-8fe3-a78c-b4e8-1dbd7cdebdfa', NULL, 'PROVINCE', 'PROVINCE:06', 'Thành phố Huế', '06', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('1ceb6cc2-a8ae-5067-ae5c-5d42a730ff09', NULL, 'PROVINCE', 'PROVINCE:07', 'Hà Giang', '07', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('b4bfb05e-455b-08b5-ea08-ba3a41f3cb83', NULL, 'PROVINCE', 'PROVINCE:08', 'Cao Bằng', '08', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('047d0f0c-6ca3-d049-4e36-57226432fdae', NULL, 'PROVINCE', 'PROVINCE:09', 'Bắc Kạn', '09', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('549743da-19e5-542c-0088-c9d7bc8d0a3e', NULL, 'PROVINCE', 'PROVINCE:10', 'Tuyên Quang', '10', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('884a1f3d-3333-f18d-f4d4-0d71dee3945e', NULL, 'PROVINCE', 'PROVINCE:11', 'Lào Cai', '11', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('c06c4425-030d-680a-9358-c0c35b588c55', NULL, 'PROVINCE', 'PROVINCE:12', 'Điện Biên', '12', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('a5047af3-97e5-7bd7-abc2-e9ee2720c52f', NULL, 'PROVINCE', 'PROVINCE:13', 'Lai Châu', '13', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('2877277f-e82d-f25b-3b9d-03364016e524', NULL, 'PROVINCE', 'PROVINCE:14', 'Sơn La', '14', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('57888f5f-f0bc-e172-63e1-132949222b8d', NULL, 'PROVINCE', 'PROVINCE:15', 'Yên Bái', '15', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('7da0d6ca-f707-9236-a01e-c477852609b3', NULL, 'PROVINCE', 'PROVINCE:16', 'Hòa Bình', '16', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('027a7a0b-ada2-921f-c3eb-a1da06bdc26f', NULL, 'PROVINCE', 'PROVINCE:17', 'Thái Nguyên', '17', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('e9166928-12f0-b2f8-284b-87dbfc99125f', NULL, 'PROVINCE', 'PROVINCE:18', 'Lạng Sơn', '18', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('878bf0b3-f4cf-9204-5415-3da9c7d106a2', NULL, 'PROVINCE', 'PROVINCE:19', 'Quảng Ninh', '19', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('f342204f-a16f-554e-811a-4f6b81ee843e', NULL, 'PROVINCE', 'PROVINCE:20', 'Bắc Giang', '20', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('c2703286-deba-84a7-44d1-14a3f85d283a', NULL, 'PROVINCE', 'PROVINCE:21', 'Bắc Ninh', '21', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('0f860457-af27-4625-6cea-672ea18daaf2', NULL, 'PROVINCE', 'PROVINCE:22', 'Vĩnh Phúc', '22', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('67b27c94-7e29-8405-c453-989de9a7d932', NULL, 'PROVINCE', 'PROVINCE:23', 'Phú Thọ', '23', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('e5833479-b73b-0e96-5c9c-85deb2f93a71', NULL, 'PROVINCE', 'PROVINCE:24', 'Hà Nam', '24', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('a47ed690-a7bb-b30a-2772-f16e79386628', NULL, 'PROVINCE', 'PROVINCE:25', 'Hưng Yên', '25', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('69057ea9-5cc6-fd72-9eab-f91deb1dfac0', NULL, 'PROVINCE', 'PROVINCE:26', 'Nam Định', '26', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('9c6c350d-1d40-b984-54b4-04a563c4bc0f', NULL, 'PROVINCE', 'PROVINCE:27', 'Thái Bình', '27', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('cc4cfa65-dce4-ba8b-ea4e-c7ebcc50c6ae', NULL, 'PROVINCE', 'PROVINCE:28', 'Ninh Bình', '28', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('4c33ea16-7335-6a4e-1d89-4205297d2175', NULL, 'PROVINCE', 'PROVINCE:29', 'Thanh Hóa', '29', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('b342845a-ff8c-73fc-20a4-0c272e26852e', NULL, 'PROVINCE', 'PROVINCE:30', 'Nghệ An', '30', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('a9dd8806-19b9-c840-f7a7-97d23812c803', NULL, 'PROVINCE', 'PROVINCE:31', 'Hà Tĩnh', '31', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('b9553330-922a-9253-f6ac-414418e03539', NULL, 'PROVINCE', 'PROVINCE:32', 'Quảng Bình', '32', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('3d907d5c-77e6-535a-cace-7657331612ef', NULL, 'PROVINCE', 'PROVINCE:33', 'Quảng Trị', '33', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('8abfa088-1ffd-5871-1abb-4cc0bb075037', NULL, 'PROVINCE', 'PROVINCE:34', 'Quảng Nam', '34', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('26a92b90-b7ee-0527-1be7-543c36bc222f', '5f68e2cf-d21f-1c69-3fbd-1404b89f26ff', 'DISTRICT', 'DISTRICT:001', 'Ba Đình', '001', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('8eae4818-8e97-2948-bec7-9ea985c686c6', '5f68e2cf-d21f-1c69-3fbd-1404b89f26ff', 'DISTRICT', 'DISTRICT:002', 'Hoàn Kiếm', '002', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('194a5c24-bc4b-380b-c39b-d28f58706245', '05f7f30f-722e-bdbe-28fc-5334b1d5815f', 'DISTRICT', 'DISTRICT:701', 'Quận 1', '701', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('010605e0-bf06-6bb7-68a5-1d311ecda7ee', '05f7f30f-722e-bdbe-28fc-5334b1d5815f', 'DISTRICT', 'DISTRICT:702', 'Quận 3', '702', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('ff12d3d4-e489-8c43-110a-a04735dd140a', '5f68e2cf-d21f-1c69-3fbd-1404b89f26ff', 'DISTRICT', 'DISTRICT:0101', 'Ba Đình', '0101', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('d51fb1b1-c09c-6a47-4f87-648c75580e6e', '5f68e2cf-d21f-1c69-3fbd-1404b89f26ff', 'DISTRICT', 'DISTRICT:0102', 'Hoàn Kiếm', '0102', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('4cf1b40e-29fc-986c-da7f-657254de89e7', '5f68e2cf-d21f-1c69-3fbd-1404b89f26ff', 'DISTRICT', 'DISTRICT:0103', 'Hai Bà Trưng', '0103', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('0c8d29cb-e45b-fed6-0231-b8ce2ff614a5', '5f68e2cf-d21f-1c69-3fbd-1404b89f26ff', 'DISTRICT', 'DISTRICT:0104', 'Đống Đa', '0104', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('c2e9ec61-2c54-448a-a2d6-3b1615d280ff', '5f68e2cf-d21f-1c69-3fbd-1404b89f26ff', 'DISTRICT', 'DISTRICT:0105', 'Tây Hồ', '0105', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('9ad404f6-ff4e-3581-be76-65bc1e5f910f', '5f68e2cf-d21f-1c69-3fbd-1404b89f26ff', 'DISTRICT', 'DISTRICT:0106', 'Cầu Giấy', '0106', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('46624ea6-ebe9-0b39-fc02-72b3e1bb8f6c', '5f68e2cf-d21f-1c69-3fbd-1404b89f26ff', 'DISTRICT', 'DISTRICT:0107', 'Thanh Xuân', '0107', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('876e206a-ddf2-3afd-ebec-221201ef8343', '5f68e2cf-d21f-1c69-3fbd-1404b89f26ff', 'DISTRICT', 'DISTRICT:0108', 'Hoàng Mai', '0108', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('e49245e5-175b-85a6-5311-0fac0671a0cb', '5f68e2cf-d21f-1c69-3fbd-1404b89f26ff', 'DISTRICT', 'DISTRICT:0109', 'Long Biên', '0109', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('bb5ee008-aa75-b241-7cd0-0bc1d81cedc0', '5f68e2cf-d21f-1c69-3fbd-1404b89f26ff', 'DISTRICT', 'DISTRICT:0110', 'Nam Từ Liêm', '0110', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('732abf39-2274-40f3-7b15-abaac799d51b', '5f68e2cf-d21f-1c69-3fbd-1404b89f26ff', 'DISTRICT', 'DISTRICT:0111', 'Bắc Từ Liêm', '0111', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('bda3e160-dddf-ad3d-7ffa-0e297f6fb354', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0201', 'Quận 1', '0201', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('76bfda00-2cbe-ddc2-6ffc-3691ddf256b8', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0202', 'Quận 3', '0202', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('10f8ac1f-f00c-d6fe-8a4e-9913073da20d', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0203', 'Quận 4', '0203', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('f220e733-3abe-f8b0-59f3-f3536a65c176', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0204', 'Quận 5', '0204', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('71d523b4-e9d9-e584-a262-bb0e5c6c7806', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0205', 'Quận 6', '0205', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('f5c61e4e-c39d-9197-49a4-45d50b2a2c1e', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0206', 'Quận 7', '0206', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('cd6dd119-3f4e-5603-9797-5dab2a7f9622', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0207', 'Quận 8', '0207', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('b06b7d8b-7f6c-fa43-c334-14a882732d39', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0208', 'Quận 10', '0208', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('2c8ca6d0-437c-f5f3-8ccd-9c6fc329236e', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0209', 'Quận 11', '0209', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('85841239-f08f-2717-f1bd-236733493b4e', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0210', 'Quận 12', '0210', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('99572e13-e6b9-3bd6-9565-5a83c8e810ee', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0211', 'Bình Thạnh', '0211', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('38f74743-ed17-5c7e-4182-7ed7a45cc453', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0212', 'Tân Bình', '0212', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('f78ca429-0065-2265-81bd-afa546ea3241', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0213', 'Tân Phú', '0213', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('64a3a2dc-5243-2854-77c1-1f135acdf890', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0214', 'Phú Nhuận', '0214', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('8867c631-84f3-3b21-0b78-cc5190833913', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0215', 'Gò Vấp', '0215', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('c80546ea-5304-f554-4f2f-f35b332fa20d', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0216', 'Bình Tân', '0216', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('8c3a49f5-c635-4d14-7bb4-f66888931cd5', '54173ff1-ab23-321e-3f1d-5a5ca34caba5', 'DISTRICT', 'DISTRICT:0217', 'Thủ Đức', '0217', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('9f3f0dec-5ca4-03ae-9940-1f375a86c370', '3a3e6ef6-8822-3757-7451-53003e9b1556', 'DISTRICT', 'DISTRICT:0301', 'Hồng Bàng', '0301', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('951428f7-8b74-f53e-effa-7c8f00fd366a', '3a3e6ef6-8822-3757-7451-53003e9b1556', 'DISTRICT', 'DISTRICT:0302', 'Ngô Quyền', '0302', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('2d70f4dc-e155-1f0c-f95b-53d4ef21092b', '3a3e6ef6-8822-3757-7451-53003e9b1556', 'DISTRICT', 'DISTRICT:0303', 'Lê Chân', '0303', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('922255e4-8f5e-387d-8dd8-1cd359c59126', '3a3e6ef6-8822-3757-7451-53003e9b1556', 'DISTRICT', 'DISTRICT:0304', 'Hải An', '0304', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('50c3d30a-4f00-336c-90ae-fe4dda8abcb2', '3a3e6ef6-8822-3757-7451-53003e9b1556', 'DISTRICT', 'DISTRICT:0305', 'Kiến An', '0305', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('899384e9-c9d9-96a3-f264-037c29305812', 'd6221f69-9136-cbdb-1739-f86152b80412', 'DISTRICT', 'DISTRICT:0401', 'Hải Châu', '0401', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('7589c2c7-24f0-82d6-ab46-1647786f49a5', 'd6221f69-9136-cbdb-1739-f86152b80412', 'DISTRICT', 'DISTRICT:0402', 'Thanh Khê', '0402', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('c6ae1a27-68a1-5ced-42ce-bf66b5270b47', 'd6221f69-9136-cbdb-1739-f86152b80412', 'DISTRICT', 'DISTRICT:0403', 'Sơn Trà', '0403', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('54100741-9be7-5c4a-6c65-d9d80e40b53a', 'd6221f69-9136-cbdb-1739-f86152b80412', 'DISTRICT', 'DISTRICT:0404', 'Ngũ Hành Sơn', '0404', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('e32531d4-f4b6-237b-05d3-d8675bf511c3', 'd6221f69-9136-cbdb-1739-f86152b80412', 'DISTRICT', 'DISTRICT:0405', 'Liên Chiểu', '0405', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('91b2c990-19f7-031c-f9ea-4ffe35bc9949', 'be0e0558-806e-f92f-c0a0-8fd47aa870cf', 'DISTRICT', 'DISTRICT:0501', 'Ninh Kiều', '0501', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('25d9ad16-1887-0cb8-4ee7-c3e98870d7d0', 'be0e0558-806e-f92f-c0a0-8fd47aa870cf', 'DISTRICT', 'DISTRICT:0502', 'Bình Thuỷ', '0502', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('64dccc23-8685-e5a9-4227-b16f8e53c5b8', 'be0e0558-806e-f92f-c0a0-8fd47aa870cf', 'DISTRICT', 'DISTRICT:0503', 'Cái Răng', '0503', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('367417ac-a0bd-57cd-4fe5-efbbb294a600', 'be0e0558-806e-f92f-c0a0-8fd47aa870cf', 'DISTRICT', 'DISTRICT:0504', 'Ô Môn', '0504', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('7bbda2cd-cb38-8a07-5592-e25e2fa76b1b', 'be0e0558-806e-f92f-c0a0-8fd47aa870cf', 'DISTRICT', 'DISTRICT:0505', 'Thốt Nốt', '0505', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('715c8fb0-55c3-2ee9-34f1-8aea546957b4', '9a8a161c-8fe3-a78c-b4e8-1dbd7cdebdfa', 'DISTRICT', 'DISTRICT:0601', 'Phú Nhuận', '0601', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('9be47159-417c-1940-3734-1e8775e0c2a0', '9a8a161c-8fe3-a78c-b4e8-1dbd7cdebdfa', 'DISTRICT', 'DISTRICT:0602', 'Thuận Hóa', '0602', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('a23e56ad-999c-a009-8eaf-e1f19afbb6fd', '9a8a161c-8fe3-a78c-b4e8-1dbd7cdebdfa', 'DISTRICT', 'DISTRICT:0603', 'Hương Thủy', '0603', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('6c493253-a121-8ec2-d74d-9713f9f22e4c', '9a8a161c-8fe3-a78c-b4e8-1dbd7cdebdfa', 'DISTRICT', 'DISTRICT:0604', 'Hương Trà', '0604', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('bc867bc0-5920-cad3-46fd-aaec3841071d', '9a8a161c-8fe3-a78c-b4e8-1dbd7cdebdfa', 'DISTRICT', 'DISTRICT:0605', 'A Lưới', '0605', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('8ef545b6-3741-01a6-5447-303aaf8cccd9', '884a1f3d-3333-f18d-f4d4-0d71dee3945e', 'DISTRICT', 'DISTRICT:1101', 'Lào Cai', '1101', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('e4240760-e29d-b23c-edd9-962f1ea843e0', '884a1f3d-3333-f18d-f4d4-0d71dee3945e', 'DISTRICT', 'DISTRICT:1102', 'Bắc Hà', '1102', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('a4cb47dd-cd0a-7803-bcb2-8e75e6e08475', '884a1f3d-3333-f18d-f4d4-0d71dee3945e', 'DISTRICT', 'DISTRICT:1103', 'Sa Pa', '1103', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('e703af33-b8ad-2394-2d98-0fef9d342559', 'c06c4425-030d-680a-9358-c0c35b588c55', 'DISTRICT', 'DISTRICT:1201', 'Điện Biên Phủ', '1201', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('f33d00b3-8a70-e222-ba25-e0056ab43612', 'c06c4425-030d-680a-9358-c0c35b588c55', 'DISTRICT', 'DISTRICT:1202', 'Mường Chà', '1202', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('52347a6d-ab4f-f6ab-c331-1172d37da79d', 'a5047af3-97e5-7bd7-abc2-e9ee2720c52f', 'DISTRICT', 'DISTRICT:1301', 'Lai Châu', '1301', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('20cadbe7-ad9c-6a97-5726-5118d014d05a', '2877277f-e82d-f25b-3b9d-03364016e524', 'DISTRICT', 'DISTRICT:1401', 'Sơn La', '1401', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('3f7bac67-7f38-0f22-99b3-5f10e897f3ae', '57888f5f-f0bc-e172-63e1-132949222b8d', 'DISTRICT', 'DISTRICT:1501', 'Yên Bái', '1501', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('511edd7c-8799-c268-8673-1fcd3f4a162d', '7da0d6ca-f707-9236-a01e-c477852609b3', 'DISTRICT', 'DISTRICT:1601', 'Hòa Bình', '1601', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('9ade0c57-388d-1319-83d0-62429a9bdd5c', '027a7a0b-ada2-921f-c3eb-a1da06bdc26f', 'DISTRICT', 'DISTRICT:1701', 'Thái Nguyên', '1701', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('d8abbd71-38ea-664b-0131-8d8722b421cc', 'e9166928-12f0-b2f8-284b-87dbfc99125f', 'DISTRICT', 'DISTRICT:1801', 'Lạng Sơn', '1801', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('d8399e20-424e-5e67-f217-15db0aa1e5fd', '878bf0b3-f4cf-9204-5415-3da9c7d106a2', 'DISTRICT', 'DISTRICT:1901', 'Hạ Long', '1901', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('2ac7ae07-b94a-3e5d-d0c6-3a77a9ef82d1', '878bf0b3-f4cf-9204-5415-3da9c7d106a2', 'DISTRICT', 'DISTRICT:1902', 'Cẩm Phả', '1902', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('2420655d-edc2-c1a9-c60f-91152c2d4dfa', '878bf0b3-f4cf-9204-5415-3da9c7d106a2', 'DISTRICT', 'DISTRICT:1903', 'Uông Bí', '1903', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('e4781f44-49ef-38b9-0a42-8bc0ee3f7eb8', 'f342204f-a16f-554e-811a-4f6b81ee843e', 'DISTRICT', 'DISTRICT:2001', 'Bắc Giang', '2001', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('f9d5ee1e-0eae-0a48-30a5-5462c8d7203e', 'c2703286-deba-84a7-44d1-14a3f85d283a', 'DISTRICT', 'DISTRICT:2101', 'Bắc Ninh', '2101', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('d54fcc11-ecc9-8491-765e-0c61d77e95c1', '0f860457-af27-4625-6cea-672ea18daaf2', 'DISTRICT', 'DISTRICT:2201', 'Vĩnh Yên', '2201', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('a17027d7-a233-07a6-8c96-7ad16c5674ee', '67b27c94-7e29-8405-c453-989de9a7d932', 'DISTRICT', 'DISTRICT:2301', 'Việt Trì', '2301', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('feb886e2-3032-4460-7d0e-a4cddb57b73b', 'e5833479-b73b-0e96-5c9c-85deb2f93a71', 'DISTRICT', 'DISTRICT:2401', 'Phủ Lý', '2401', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('b7d4d118-8048-67d4-a7ee-fdffe94dcbc1', 'a47ed690-a7bb-b30a-2772-f16e79386628', 'DISTRICT', 'DISTRICT:2501', 'Hưng Yên', '2501', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('8e57862f-f1cc-4974-c355-3d347a363a50', '69057ea9-5cc6-fd72-9eab-f91deb1dfac0', 'DISTRICT', 'DISTRICT:2601', 'Nam Định', '2601', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('ff99ff43-71c2-2b79-4ab2-4095f90ab013', '9c6c350d-1d40-b984-54b4-04a563c4bc0f', 'DISTRICT', 'DISTRICT:2701', 'Thái Bình', '2701', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('8873b026-b800-dae3-3dd0-222ba61c7a1f', 'cc4cfa65-dce4-ba8b-ea4e-c7ebcc50c6ae', 'DISTRICT', 'DISTRICT:2801', 'Ninh Bình', '2801', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('59878bad-0014-57ac-1762-af6d6cc77569', '4c33ea16-7335-6a4e-1d89-4205297d2175', 'DISTRICT', 'DISTRICT:2901', 'Thanh Hóa', '2901', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('51bbca04-04f6-98f5-5eb8-8817da62f6b3', 'b342845a-ff8c-73fc-20a4-0c272e26852e', 'DISTRICT', 'DISTRICT:3001', 'Vinh', '3001', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('22c267d1-f601-1ec5-f219-8c54ac790502', 'a9dd8806-19b9-c840-f7a7-97d23812c803', 'DISTRICT', 'DISTRICT:3101', 'Hà Tĩnh', '3101', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('e978f0af-6f6c-da01-9ccb-8d96e349b9a5', 'b9553330-922a-9253-f6ac-414418e03539', 'DISTRICT', 'DISTRICT:3201', 'Đồng Hới', '3201', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('c887a7db-3616-a4cd-eb08-f625fb5ba23e', '3d907d5c-77e6-535a-cace-7657331612ef', 'DISTRICT', 'DISTRICT:3301', 'Đông Hà', '3301', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('659338d1-49a5-167f-8afa-38cbb51b79c8', '8abfa088-1ffd-5871-1abb-4cc0bb075037', 'DISTRICT', 'DISTRICT:3401', 'Tam Kỳ', '3401', '2026-07-24 11:36:51.79997+07');
INSERT INTO public.administrative_areas (administrative_area_id, parent_area_id, area_type, code, name, legacy_code, created_at) VALUES ('74bbbe68-ee29-b396-1def-31387ce9001d', '8abfa088-1ffd-5871-1abb-4cc0bb075037', 'DISTRICT', 'DISTRICT:3402', 'Hội An', '3402', '2026-07-24 11:36:51.79997+07');


--
-- Data for Name: care_facilities; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('00000000-0000-0000-0000-000000000101', NULL, 'Bệnh viện Phụ sản Trung ương Cần Thơ', 'HOSPITAL', '360 Đ. Nguyễn Văn Cừ, An Khánh, Ninh Kiều, Cần Thơ', 10.0186, 105.7878, '02923888888', NULL, NULL, 'VERIFIED', '2026-07-24 11:36:51.040941+07', '2026-07-24 11:36:51.040941+07', NULL, NULL, NULL, NULL, NULL, true, true, NULL);
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('00000000-0000-0000-0000-000000000102', NULL, 'Phòng khám sản phụ khoa Hồng Hạc', 'CLINIC', '45B Đ. Lê Lợi, Tân An, Ninh Kiều, Cần Thơ', 10.0123, 105.7856, '0292123456', NULL, NULL, 'VERIFIED', '2026-07-24 11:36:51.040941+07', '2026-07-24 11:36:51.040941+07', NULL, NULL, NULL, NULL, NULL, true, true, NULL);
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('00000000-0000-0000-0000-000000000103', NULL, 'Bệnh viện Đa khoa Trung ương Cần Thơ', 'HOSPITAL', '5 Đ. Nguyễn Văn Cừ, Hưng Lợi, Ninh Kiều, Cần Thơ', 10.0156, 105.7867, '02923868888', NULL, NULL, 'VERIFIED', '2026-07-24 11:36:51.040941+07', '2026-07-24 11:36:51.040941+07', NULL, NULL, NULL, NULL, NULL, true, true, NULL);
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('00000000-0000-0000-0000-000000000104', NULL, 'Phòng khám Nhi Cửu Long', 'CLINIC', '12 Đ. Nguyễn Trãi, Xuân Khánh, Ninh Kiều, Cần Thơ', 10.0190, 105.7890, '0292765432', NULL, NULL, 'VERIFIED', '2026-07-24 11:36:51.040941+07', '2026-07-24 11:36:51.040941+07', NULL, NULL, NULL, NULL, NULL, true, true, NULL);
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('00000000-0000-0000-0000-000000000105', NULL, 'Trạm y tế phường An Khánh', 'HEALTH_STATION', '88 Đ. Mậu Thân, An Khánh, Ninh Kiều, Cần Thơ', 10.0170, 105.7840, '0292111222', NULL, NULL, 'VERIFIED', '2026-07-24 11:36:51.040941+07', '2026-07-24 11:36:51.040941+07', NULL, NULL, NULL, NULL, NULL, true, true, NULL);
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('7cc0b26c-e90a-be26-f9d8-7a3741135c64', NULL, 'Bệnh viện Bạch Mai', 'HOSPITAL', '78 Giải Phóng, Đống Đa, Hà Nội', NULL, NULL, '024-3869-6666', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '01', '0103', 'H001', true, false, '4cf1b40e-29fc-986c-da7f-657254de89e7');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('c5feed05-ea74-b476-8801-f4209bff1f68', NULL, 'Bệnh viện Chợ Rẫy', 'HOSPITAL', '201 Hoàng Văn Thụ, Quận 5, TP.HCM', NULL, NULL, '028-3855-4269', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '02', '0201', 'H002', true, false, 'bda3e160-dddf-ad3d-7ffa-0e297f6fb354');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('e1b2e539-29c8-9a51-90eb-0a2424ce6ab6', NULL, 'Bệnh viện Việt Đức', 'HOSPITAL', '40 Tràng Thi, Hoàn Kiếm, Hà Nội', NULL, NULL, '024-3936-2222', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '01', '0102', 'H003', true, false, 'd51fb1b1-c09c-6a47-4f87-648c75580e6e');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('63152b7b-de69-34a7-6f4f-a31df772fb4b', NULL, 'Bệnh viện 108', 'HOSPITAL', '1 Trần Hưng Đạo, Ba Đình, Hà Nội', NULL, NULL, '024-3940-9188', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '01', '0101', 'H004', true, false, 'ff12d3d4-e489-8c43-110a-a04735dd140a');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('3a5f77e6-8629-748c-be8e-5124c34bcecc', NULL, 'Bệnh viện Nhi Trung ương', 'HOSPITAL', '18/879 La Thành, Đống Đa, Hà Nội', NULL, NULL, '024-3772-3778', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '01', '0105', 'H005', true, false, 'c2e9ec61-2c54-448a-a2d6-3b1615d280ff');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('9d338c48-5a7c-a41c-8d91-792e9ca51df3', NULL, 'Bệnh viện Từ Dũ', 'HOSPITAL', '284 Cộng Hòa, Tân Bình, TP.HCM', NULL, NULL, '028-3811-0022', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '02', '0205', 'H006', true, false, '71d523b4-e9d9-e584-a262-bb0e5c6c7806');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('aa1fdf54-ea1e-5e4e-63de-267161a2fd98', NULL, 'Bệnh viện Nhi đồng 1', 'HOSPITAL', '341 Su Văn Hạnh, Quận 10, TP.HCM', NULL, NULL, '028-3866-9966', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '02', '0203', 'H007', true, false, '10f8ac1f-f00c-d6fe-8a4e-9913073da20d');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('5f8e5ef6-9014-c645-f5a7-b55a2593fe13', NULL, 'Bệnh viện Đại học Y Dược TP.HCM', 'HOSPITAL', '215 Hồng Bàng, Quận 5, TP.HCM', NULL, NULL, '028-3855-4781', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '02', '0211', 'H008', true, false, '99572e13-e6b9-3bd6-9565-5a83c8e810ee');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('207c848b-9703-39bb-cae8-6280984af8c0', NULL, 'Bệnh viện Cần Thơ', 'HOSPITAL', '194-196-198 30/4, Ninh Kiều, Cần Thơ', NULL, NULL, '0292-389-9595', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '05', '0501', 'H009', true, false, '91b2c990-19f7-031c-f9ea-4ffe35bc9949');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('7ecc52e7-a73a-a064-12fa-610d58fedb88', NULL, 'Bệnh viện Đà Nẵng', 'HOSPITAL', '124 Hải Phòng, Hải Châu, Đà Nẵng', NULL, NULL, '0236-382-1818', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '04', '0401', 'H010', true, false, '899384e9-c9d9-96a3-f264-037c29305812');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('bd515ec5-3f0c-0a75-5463-7c8b54ad738f', NULL, 'Bệnh viện Huế', 'HOSPITAL', '3 Lê Lợi, Vĩnh Ninh, TP. Huế', NULL, NULL, '0234-382-2888', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '06', '0601', 'H011', true, false, '715c8fb0-55c3-2ee9-34f1-8aea546957b4');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('1f65a9de-5e09-29f9-0727-8177d6e19d9c', NULL, 'Bệnh viện Hải Phòng', 'HOSPITAL', '208 Trần Phú, Hồng Bàng, Hải Phòng', NULL, NULL, '0225-382-2555', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '03', '0301', 'H012', true, false, '9f3f0dec-5ca4-03ae-9940-1f375a86c370');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('74d445bb-dfb9-528b-2e6c-ad36721a8bd0', NULL, 'Bệnh viện Y học Cổ truyền Trung ương', 'HOSPITAL', '39-43 Hàng Đài, Hoàn Kiếm, Hà Nội', NULL, NULL, '024-3935-2111', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '01', '0105', 'H013', true, false, 'c2e9ec61-2c54-448a-a2d6-3b1615d280ff');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('59c8e34e-2819-21be-b9f5-df44e9959995', NULL, 'Bệnh viện Ung thư Trung ương', 'HOSPITAL', '44-54 Khuất Duy Tiến, Thanh Xuân, Hà Nội', NULL, NULL, '024-3556-5666', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '01', '0105', 'H014', true, false, 'c2e9ec61-2c54-448a-a2d6-3b1615d280ff');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('4ea99e58-c7c7-460a-ab2c-e4aaaa86a9cc', NULL, 'Bệnh viện Tim TP.HCM', 'HOSPITAL', '141 Nguyễn Chí Thanh, Quận 5, TP.HCM', NULL, NULL, '028-3925-2925', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '02', '0205', 'H015', true, false, '71d523b4-e9d9-e584-a262-bb0e5c6c7806');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('00e43fa8-8ae5-e4bc-f314-7a90fee5bd63', NULL, 'Bệnh viện Phổi TP.HCM', 'HOSPITAL', '123 Phổ Quang, Tân Phú, TP.HCM', NULL, NULL, '028-3812-2121', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '02', '0215', 'H016', true, false, '8867c631-84f3-3b21-0b78-cc5190833913');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('ef90d044-c2ec-804f-2808-deba40428089', NULL, 'Bệnh viện Nhi Đồng 2', 'HOSPITAL', '298-300 Đồng Khởi, Quận 1, TP.HCM', NULL, NULL, '028-3829-2593', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '02', '0217', 'H017', true, false, '8c3a49f5-c635-4d14-7bb4-f66888931cd5');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('921f63bc-5169-18d5-b77c-dfaf5834fd18', NULL, 'Bệnh viện Phụ sản Trung ương', 'HOSPITAL', '644 Láng, Đống Đa, Hà Nội', NULL, NULL, '024-3855-4343', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '01', '0106', 'H018', true, false, '9ad404f6-ff4e-3581-be76-65bc1e5f910f');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('13a41ee5-6275-6fc9-6904-001d0ecda301', NULL, 'Bệnh viện Mắt Trung ương', 'HOSPITAL', '406-408 Nguyễn Trãi, Thịng Bình, Hà Nội', NULL, NULL, '024-3855-5548', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '01', '0105', 'H019', true, false, 'c2e9ec61-2c54-448a-a2d6-3b1615d280ff');
INSERT INTO public.care_facilities (facility_id, partner_id, name, facility_type, address, latitude, longitude, phone, opening_hours_json, source_type, verification_status, created_at, updated_at, facility_level, ownership_type, province_id, district_id, external_source_id, is_active, is_searchable, administrative_area_id) VALUES ('f8e360a7-c57b-dba7-c3b0-c5ea46e031af', NULL, 'Bệnh viện Răng Hàm Mặt Trung ương', 'HOSPITAL', '414-416 Nguyễn Trãi, Thịng Bình, Hà Nội', NULL, NULL, '024-3855-5051', NULL, 'LEGACY_IMPORT', 'UNVERIFIED', '2026-07-24 11:36:51.447129+07', '2026-07-24 11:36:51.447129+07', 'Hạng I', NULL, '01', '0105', 'H020', true, false, 'c2e9ec61-2c54-448a-a2d6-3b1615d280ff');


--
-- Data for Name: care_item_templates; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: community_topics; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.community_topics (id, created_at, description, name, updated_at, is_hidden, icon, sort_order, created_by, type, slug, parent_id) VALUES ('b1b2c3d4-e5f6-7890-abcd-ef1234567801', '2026-07-24 11:36:51.691528+07', 'Chuẩn bị sức khoẻ, tâm lý trước khi mang thai', 'Chuẩn bị mang thai', '2026-07-24 11:36:51.691528+07', false, 'favorite', 1, NULL, 'CATEGORY', 'chuan-bi-mang-thai', NULL);
INSERT INTO public.community_topics (id, created_at, description, name, updated_at, is_hidden, icon, sort_order, created_by, type, slug, parent_id) VALUES ('b1b2c3d4-e5f6-7890-abcd-ef1234567802', '2026-07-24 11:36:51.691528+07', 'Chăm sóc và theo dõi trong thai kỳ', 'Mang thai', '2026-07-24 11:36:51.691528+07', false, 'pregnant_woman', 2, NULL, 'CATEGORY', 'mang-thai', NULL);
INSERT INTO public.community_topics (id, created_at, description, name, updated_at, is_hidden, icon, sort_order, created_by, type, slug, parent_id) VALUES ('b1b2c3d4-e5f6-7890-abcd-ef1234567803', '2026-07-24 11:36:51.691528+07', 'Hồi phục và chăm sóc sau khi sinh', 'Sau sinh', '2026-07-24 11:36:51.691528+07', false, 'healing', 3, NULL, 'CATEGORY', 'sau-sinh', NULL);
INSERT INTO public.community_topics (id, created_at, description, name, updated_at, is_hidden, icon, sort_order, created_by, type, slug, parent_id) VALUES ('b1b2c3d4-e5f6-7890-abcd-ef1234567804', '2026-07-24 11:36:51.691528+07', 'Chăm sóc và nuôi dạy bé sơ sinh', 'Chăm bé', '2026-07-24 11:36:51.691528+07', false, 'child_care', 4, NULL, 'CATEGORY', 'cham-be', NULL);
INSERT INTO public.community_topics (id, created_at, description, name, updated_at, is_hidden, icon, sort_order, created_by, type, slug, parent_id) VALUES ('b1b2c3d4-e5f6-7890-abcd-ef1234567805', '2026-07-24 11:36:51.691528+07', 'Các chủ đề khác không thuộc nhóm trên', 'Khác', '2026-07-24 11:36:51.691528+07', false, 'more_horiz', 5, NULL, 'CATEGORY', 'khac', NULL);
INSERT INTO public.community_topics (id, created_at, description, name, updated_at, is_hidden, icon, sort_order, created_by, type, slug, parent_id) VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567801', '2026-07-24 11:36:50.948483+07', 'Chế độ ăn, bổ sung vi chất, thực phẩm an toàn khi mang thai', 'Dinh dưỡng thai kỳ', '2026-07-24 11:36:50.948483+07', false, 'restaurant', 1, NULL, 'TOPIC', 'dinh-duong-thai-ky', 'b1b2c3d4-e5f6-7890-abcd-ef1234567802');
INSERT INTO public.community_topics (id, created_at, description, name, updated_at, is_hidden, icon, sort_order, created_by, type, slug, parent_id) VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567802', '2026-07-24 11:36:50.948483+07', 'Theo dõi sự phát triển, siêu âm, xét nghiệm thai kỳ', 'Sức khỏe thai nhi', '2026-07-24 11:36:50.948483+07', false, 'health_and_safety', 2, NULL, 'TOPIC', 'suc-khoe-thai-nhi', 'b1b2c3d4-e5f6-7890-abcd-ef1234567802');
INSERT INTO public.community_topics (id, created_at, description, name, updated_at, is_hidden, icon, sort_order, created_by, type, slug, parent_id) VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567805', '2026-07-24 11:36:50.948483+07', 'Tư thế ngủ, vận động an toàn, giảm đau lưng khi mang thai', 'Giấc ngủ và thể chất', '2026-07-24 11:36:50.948483+07', false, 'bedtime', 5, NULL, 'TOPIC', 'giac-ngu-va-the-chat', 'b1b2c3d4-e5f6-7890-abcd-ef1234567802');
INSERT INTO public.community_topics (id, created_at, description, name, updated_at, is_hidden, icon, sort_order, created_by, type, slug, parent_id) VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567803', '2026-07-24 11:36:50.948483+07', 'Hồi phục sau sinh, chăm sóc vết thương, tâm lý sau sinh', 'Chăm sóc sau sinh', '2026-07-24 11:36:50.948483+07', false, 'vaccines', 3, NULL, 'TOPIC', 'cham-soc-sau-sinh', 'b1b2c3d4-e5f6-7890-abcd-ef1234567803');
INSERT INTO public.community_topics (id, created_at, description, name, updated_at, is_hidden, icon, sort_order, created_by, type, slug, parent_id) VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567804', '2026-07-24 11:36:50.948483+07', 'Kỹ thuật cho bú, tăng sữa, cai sữa', 'Nuôi con bằng sữa mẹ', '2026-07-24 11:36:50.948483+07', false, 'child_care', 4, NULL, 'TOPIC', 'nuoi-con-bang-sua-me', 'b1b2c3d4-e5f6-7890-abcd-ef1234567803');
INSERT INTO public.community_topics (id, created_at, description, name, updated_at, is_hidden, icon, sort_order, created_by, type, slug, parent_id) VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567807', '2026-07-24 11:36:50.948483+07', 'Tắm bé, chăm rốn, lịch tiêm chủng, phát triển trẻ 0–12 tháng', 'Chăm sóc bé sơ sinh', '2026-07-24 11:36:50.948483+07', false, 'pregnant_woman', 7, NULL, 'TOPIC', 'cham-soc-be-so-sinh', 'b1b2c3d4-e5f6-7890-abcd-ef1234567804');
INSERT INTO public.community_topics (id, created_at, description, name, updated_at, is_hidden, icon, sort_order, created_by, type, slug, parent_id) VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567806', '2026-07-24 11:36:50.948483+07', 'Lo âu, trầm cảm thai kỳ, hỗ trợ tinh thần mẹ bầu', 'Tâm lý & Cảm xúc', '2026-07-24 11:36:50.948483+07', false, 'psychology', 6, NULL, 'TOPIC', 'tam-ly-va-cam-xuc', 'b1b2c3d4-e5f6-7890-abcd-ef1234567805');
INSERT INTO public.community_topics (id, created_at, description, name, updated_at, is_hidden, icon, sort_order, created_by, type, slug, parent_id) VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567808', '2026-07-24 11:36:50.948483+07', 'Các câu hỏi khác về thai kỳ và làm mẹ', 'Hỏi đáp chung', '2026-07-24 11:36:50.948483+07', false, 'forum', 8, NULL, 'TOPIC', 'hoi-dap-chung', 'b1b2c3d4-e5f6-7890-abcd-ef1234567805');


--
-- Data for Name: knowledge_sources; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.knowledge_sources (knowledge_source_id, domain, base_url, organization, category, status, discovery_mode, applicable_stages, added_by, reviewed_by, reviewed_at, notes, source_version, created_at, updated_at) VALUES ('8e674331-6c45-4d7a-bb0e-3501f51e4c6d', 'moh.gov.vn', 'https://moh.gov.vn', 'Bộ Y tế Việt Nam', 'GOVERNMENT', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,INFANT,TODDLER', NULL, NULL, '2026-07-24 11:36:51.201351+07', 'Seeded official government source', NULL, '2026-07-24 11:36:51.201351+07', '2026-07-24 11:36:51.201351+07');
INSERT INTO public.knowledge_sources (knowledge_source_id, domain, base_url, organization, category, status, discovery_mode, applicable_stages, added_by, reviewed_by, reviewed_at, notes, source_version, created_at, updated_at) VALUES ('1af7a3c4-0d5c-4df6-a5bd-5973743aa918', 'adminmoh.moh.gov.vn', 'https://adminmoh.moh.gov.vn', 'Bộ Y tế Việt Nam', 'GOVERNMENT', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,INFANT,TODDLER', NULL, NULL, '2026-07-24 11:36:51.201351+07', 'Seeded official government source', NULL, '2026-07-24 11:36:51.201351+07', '2026-07-24 11:36:51.201351+07');
INSERT INTO public.knowledge_sources (knowledge_source_id, domain, base_url, organization, category, status, discovery_mode, applicable_stages, added_by, reviewed_by, reviewed_at, notes, source_version, created_at, updated_at) VALUES ('bfcc907d-e931-41c5-921a-f804f8bba767', 'mch.moh.gov.vn', 'https://mch.moh.gov.vn', 'Cục Bà mẹ và Trẻ em', 'GOVERNMENT', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,INFANT,TODDLER', NULL, NULL, '2026-07-24 11:36:51.201351+07', 'Seeded official maternal and child health source', NULL, '2026-07-24 11:36:51.201351+07', '2026-07-24 11:36:51.201351+07');
INSERT INTO public.knowledge_sources (knowledge_source_id, domain, base_url, organization, category, status, discovery_mode, applicable_stages, added_by, reviewed_by, reviewed_at, notes, source_version, created_at, updated_at) VALUES ('33d52c0c-0928-4807-8a57-14bfb551f0a5', 'who.int', 'https://www.who.int', 'World Health Organization', 'WHO_UNICEF', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,INFANT,TODDLER', NULL, NULL, '2026-07-24 11:36:51.201351+07', 'Seeded official international source', NULL, '2026-07-24 11:36:51.201351+07', '2026-07-24 11:36:51.201351+07');
INSERT INTO public.knowledge_sources (knowledge_source_id, domain, base_url, organization, category, status, discovery_mode, applicable_stages, added_by, reviewed_by, reviewed_at, notes, source_version, created_at, updated_at) VALUES ('23e06b15-e750-469f-9e61-dc61d369b15e', 'iris.who.int', 'https://iris.who.int', 'World Health Organization IRIS', 'WHO_UNICEF', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,INFANT,TODDLER', NULL, NULL, '2026-07-24 11:36:51.201351+07', 'Seeded official international source', NULL, '2026-07-24 11:36:51.201351+07', '2026-07-24 11:36:51.201351+07');
INSERT INTO public.knowledge_sources (knowledge_source_id, domain, base_url, organization, category, status, discovery_mode, applicable_stages, added_by, reviewed_by, reviewed_at, notes, source_version, created_at, updated_at) VALUES ('713361e4-5e62-48b7-bedc-01f34ef000f9', 'unicef.org', 'https://www.unicef.org', 'UNICEF', 'WHO_UNICEF', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,INFANT,TODDLER', NULL, NULL, '2026-07-24 11:36:51.201351+07', 'Seeded official international source', NULL, '2026-07-24 11:36:51.201351+07', '2026-07-24 11:36:51.201351+07');
INSERT INTO public.knowledge_sources (knowledge_source_id, domain, base_url, organization, category, status, discovery_mode, applicable_stages, added_by, reviewed_by, reviewed_at, notes, source_version, created_at, updated_at) VALUES ('c717b1ba-6794-4b77-b5ac-dc4409e766b7', 'cdc.gov', 'https://www.cdc.gov', 'Centers for Disease Control and Prevention', 'CDC', 'APPROVED', 'SEED', 'PRECONCEPTION,PREGNANCY,INFANT,TODDLER', NULL, NULL, '2026-07-24 11:36:51.201351+07', 'Seeded official international source', NULL, '2026-07-24 11:36:51.201351+07', '2026-07-24 11:36:51.201351+07');
INSERT INTO public.knowledge_sources (knowledge_source_id, domain, base_url, organization, category, status, discovery_mode, applicable_stages, added_by, reviewed_by, reviewed_at, notes, source_version, created_at, updated_at) VALUES ('adbb043f-fde5-4f61-846a-662686d2db3e', 'benhviennhitrunguong.gov.vn', 'https://benhviennhitrunguong.gov.vn', 'Bệnh viện Nhi Trung ương', 'HOSPITAL', 'APPROVED', 'SEED', 'INFANT,TODDLER', NULL, NULL, '2026-07-24 11:36:51.201351+07', 'Seeded pediatric hospital source', NULL, '2026-07-24 11:36:51.201351+07', '2026-07-24 11:36:51.201351+07');
INSERT INTO public.knowledge_sources (knowledge_source_id, domain, base_url, organization, category, status, discovery_mode, applicable_stages, added_by, reviewed_by, reviewed_at, notes, source_version, created_at, updated_at) VALUES ('b5c1c3e5-5c85-4de1-ae51-c2d45fe34719', 'nhidong.org.vn', 'https://nhidong.org.vn', 'Bệnh viện Nhi Đồng 1', 'HOSPITAL', 'APPROVED', 'SEED', 'INFANT,TODDLER', NULL, NULL, '2026-07-24 11:36:51.201351+07', 'Seeded pediatric hospital source', NULL, '2026-07-24 11:36:51.201351+07', '2026-07-24 11:36:51.201351+07');
INSERT INTO public.knowledge_sources (knowledge_source_id, domain, base_url, organization, category, status, discovery_mode, applicable_stages, added_by, reviewed_by, reviewed_at, notes, source_version, created_at, updated_at) VALUES ('330f714b-7e87-414e-a844-c3b865dfad78', 'bvndtp.org.vn', 'https://bvndtp.org.vn', 'Bệnh viện Nhi Đồng Thành phố', 'HOSPITAL', 'APPROVED', 'SEED', 'INFANT,TODDLER', NULL, NULL, '2026-07-24 11:36:51.201351+07', 'Seeded pediatric hospital source', NULL, '2026-07-24 11:36:51.201351+07', '2026-07-24 11:36:51.201351+07');


--
-- Data for Name: red_flag_rules; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('1b9a40fc-7311-44bd-8d3e-f243c3b70e0e', 'chảy máu nhiều', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('bd835cb7-76c6-40ec-8d3e-da934566fdcb', 'ngất xỉu', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('718f9b54-cac4-41d3-bbbb-1311ff7c6d9d', 'khó thở', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('8c024590-1c8e-47d9-8970-606f74542681', 'co giật', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('921edcc6-2b73-45c2-8b1f-f925518d6d6f', 'tim ngừng đập', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('c5b804ed-499f-435e-8394-72f3724fee52', 'xuất huyết', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('8cdc47a1-3823-4349-92ea-81f205933d52', 'hôn mê', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('3797ee02-0901-466c-92d2-662a1c4322c1', 'đau ngực dữ dội', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('f2843a27-9b60-49be-a1db-c13c95c4fc05', 'sảy thai', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('62766919-fdfc-4c47-af9c-0254e88d4dd2', 'sinh non', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('323f4927-1961-4978-b4b0-fa78145e5800', 'ngộ độc', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('c7372a48-6730-4f56-9678-b5f4b4526397', 'bất tỉnh', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('c73f4476-c1d2-41d2-8d82-bdb0b4ce487e', 'đuối nước', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('928f1f96-0d58-4a89-ae03-0d86009c2c5a', 'gãy xương hở', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('b27ff614-4c21-49ac-963b-fbb9629a4e0d', 'bỏng nặng', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('063342e1-af78-4b41-9a70-c28d4b847708', 'mất ý thức', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('f62e16e8-d792-4609-b129-97b3d112bd89', 'không thở', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('849b84a8-3635-443e-8707-b7d5a8e9e5ae', 'đau bụng dữ dội', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');
INSERT INTO public.red_flag_rules (id, keyword, severity, action, is_active, is_system_default, created_by, updated_by, created_at, updated_at) VALUES ('f9876515-4629-4bfb-9210-edfd9f482bf0', 'chảy máu âm đạo nhiều', 'RED', 'ESCALATE', true, true, NULL, NULL, '2026-07-24 11:36:51.03614+07', '2026-07-24 11:36:51.03614+07');


--
-- Data for Name: specialties; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.specialties (specialty_id, code, name, description, is_active, created_at) VALUES ('7c3cd28b-3623-7a79-adce-c1b410cc7706', 'S01', 'Sản khoa', 'Chăm sóc sức khỏe sinh sản, theo dõi thai kỳ và sinh nở', true, '2026-07-24 11:36:51.69611+07');
INSERT INTO public.specialties (specialty_id, code, name, description, is_active, created_at) VALUES ('5805de43-8235-789b-97d0-b0fed18db1b7', 'S02', 'Nhi khoa', 'Chăm sóc và điều trị bệnh lý cho trẻ em', true, '2026-07-24 11:36:51.69611+07');
INSERT INTO public.specialties (specialty_id, code, name, description, is_active, created_at) VALUES ('749faa9b-9dd7-5a8e-558d-34f70e140c59', 'S03', 'Sơ sinh', 'Chăm sóc đặc biệt cho trẻ sơ sinh và trẻ sinh non', true, '2026-07-24 11:36:51.69611+07');
INSERT INTO public.specialties (specialty_id, code, name, description, is_active, created_at) VALUES ('4b656550-12a7-2aa9-1476-49de16a08ec2', 'S04', 'Dinh dưỡng Nhi khoa', 'Tư vấn dinh dưỡng cho trẻ em trong các giai đoạn phát triển', true, '2026-07-24 11:36:51.69611+07');
INSERT INTO public.specialties (specialty_id, code, name, description, is_active, created_at) VALUES ('2f8197ec-4aa5-2b68-018b-48b16a5d939b', 'S05', 'Tâm lý Mẹ và Bé', 'Hỗ trợ tâm lý thai kỳ, trầm cảm sau sinh và tâm lý trẻ thơ', true, '2026-07-24 11:36:51.69611+07');
INSERT INTO public.specialties (specialty_id, code, name, description, is_active, created_at) VALUES ('d1d932b8-3242-2b90-f134-5007c68ab89c', 'S06', 'Điều dưỡng Sản Nhi', 'Chăm sóc điều dưỡng chuyên sâu cho mẹ và trẻ', true, '2026-07-24 11:36:51.69611+07');
INSERT INTO public.specialties (specialty_id, code, name, description, is_active, created_at) VALUES ('5e9cc1b8-01e1-af62-aa48-de3ed13ecb77', 'S07', 'Hỗ trợ nuôi con bằng sữa mẹ', 'Tư vấn và hướng dẫn kỹ thuật cho con bú', true, '2026-07-24 11:36:51.69611+07');
INSERT INTO public.specialties (specialty_id, code, name, description, is_active, created_at) VALUES ('9172e001-d7d5-1552-99c4-ad4a6327b2c7', 'S08', 'Phục hồi chức năng Nhi', 'Vật lý trị liệu và phục hồi chức năng cho trẻ em', true, '2026-07-24 11:36:51.69611+07');


--
-- Data for Name: vaccination_schedules; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.vaccination_schedules (vaccination_schedule_id, vaccine_name, dose_number, offset_days, description, schedule_version, active_from, active_to, created_at) VALUES ('b6f69eb8-d570-46fc-a507-4dd0d53e9fae', 'BCG', 1, 0, 'BCG — phòng lao, tiêm ngay sau sinh', 'legacy-b6f69eb8', NULL, NULL, '2026-07-24 11:36:50.992959+07');
INSERT INTO public.vaccination_schedules (vaccination_schedule_id, vaccine_name, dose_number, offset_days, description, schedule_version, active_from, active_to, created_at) VALUES ('ace5d2d9-55f0-4483-b23b-daa5ad42a00c', 'Viem gan B', 1, 0, 'Viêm gan B liều 1 — tiêm trong 24h sau sinh', 'legacy-ace5d2d9', NULL, NULL, '2026-07-24 11:36:50.992959+07');
INSERT INTO public.vaccination_schedules (vaccination_schedule_id, vaccine_name, dose_number, offset_days, description, schedule_version, active_from, active_to, created_at) VALUES ('979b971a-1622-4f49-af37-ad8c2aeebbda', 'Viem gan B', 2, 30, 'Viêm gan B liều 2 — 1 tháng tuổi', 'legacy-979b971a', NULL, NULL, '2026-07-24 11:36:50.992959+07');
INSERT INTO public.vaccination_schedules (vaccination_schedule_id, vaccine_name, dose_number, offset_days, description, schedule_version, active_from, active_to, created_at) VALUES ('11f23fad-f397-486e-b1a1-1930201d5489', 'Viem gan B', 3, 60, 'Viêm gan B liều 3 — 2 tháng tuổi', 'legacy-11f23fad', NULL, NULL, '2026-07-24 11:36:50.992959+07');
INSERT INTO public.vaccination_schedules (vaccination_schedule_id, vaccine_name, dose_number, offset_days, description, schedule_version, active_from, active_to, created_at) VALUES ('93279d93-12d3-4447-94b9-27e7c7ccc6e9', 'DTP-VGB-Hib', 1, 60, 'DTP-VGB-Hib liều 1 — 2 tháng tuổi', 'legacy-93279d93', NULL, NULL, '2026-07-24 11:36:50.992959+07');
INSERT INTO public.vaccination_schedules (vaccination_schedule_id, vaccine_name, dose_number, offset_days, description, schedule_version, active_from, active_to, created_at) VALUES ('42f81489-fb9b-428f-bc80-8b02fe9cb187', 'DTP-VGB-Hib', 2, 90, 'DTP-VGB-Hib liều 2 — 3 tháng tuổi', 'legacy-42f81489', NULL, NULL, '2026-07-24 11:36:50.992959+07');
INSERT INTO public.vaccination_schedules (vaccination_schedule_id, vaccine_name, dose_number, offset_days, description, schedule_version, active_from, active_to, created_at) VALUES ('233620ad-1a44-4dc1-9919-fd49029d91d1', 'DTP-VGB-Hib', 3, 120, 'DTP-VGB-Hib liều 3 — 4 tháng tuổi', 'legacy-233620ad', NULL, NULL, '2026-07-24 11:36:50.992959+07');


--
-- PostgreSQL database dump complete
--


--
-- PostgreSQL database dump
--


-- Dumped from database version 18.1 (Homebrew)
-- Dumped by pg_dump version 18.1 (Homebrew)



--
-- Name: account_deletion_requests account_deletion_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_deletion_requests
    ADD CONSTRAINT account_deletion_requests_pkey PRIMARY KEY (id);


--
-- Name: administrative_areas administrative_areas_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.administrative_areas
    ADD CONSTRAINT administrative_areas_code_key UNIQUE (code);


--
-- Name: administrative_areas administrative_areas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.administrative_areas
    ADD CONSTRAINT administrative_areas_pkey PRIMARY KEY (administrative_area_id);


--
-- Name: archived_consultation_records archived_consultation_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_consultation_records
    ADD CONSTRAINT archived_consultation_records_pkey PRIMARY KEY (archive_id);


--
-- Name: archived_consultation_records archived_consultation_records_source_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_consultation_records
    ADD CONSTRAINT archived_consultation_records_source_uk UNIQUE (legacy_table, legacy_id);


--
-- Name: archived_partner_records archived_partner_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_partner_records
    ADD CONSTRAINT archived_partner_records_pkey PRIMARY KEY (archive_id);


--
-- Name: archived_partner_records archived_partner_records_source_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_partner_records
    ADD CONSTRAINT archived_partner_records_source_uk UNIQUE (legacy_table, legacy_id);


--
-- Name: archived_realtime_records archived_realtime_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_realtime_records
    ADD CONSTRAINT archived_realtime_records_pkey PRIMARY KEY (archive_id);


--
-- Name: archived_realtime_records archived_realtime_records_source_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_realtime_records
    ADD CONSTRAINT archived_realtime_records_source_uk UNIQUE (legacy_table, legacy_id);


--
-- Name: attachments attachments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attachments
    ADD CONSTRAINT attachments_pkey PRIMARY KEY (attachment_id);


--
-- Name: attachments attachments_storage_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attachments
    ADD CONSTRAINT attachments_storage_key_key UNIQUE (storage_key);


--
-- Name: audit_events audit_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_events
    ADD CONSTRAINT audit_events_pkey PRIMARY KEY (audit_event_id);


--
-- Name: auth_challenges auth_challenges_legacy_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_challenges
    ADD CONSTRAINT auth_challenges_legacy_uk UNIQUE (legacy_source, legacy_id);


--
-- Name: auth_challenges auth_challenges_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_challenges
    ADD CONSTRAINT auth_challenges_pkey PRIMARY KEY (challenge_id);


--
-- Name: auth_revocations auth_revocations_legacy_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_revocations
    ADD CONSTRAINT auth_revocations_legacy_uk UNIQUE (legacy_source, legacy_id);


--
-- Name: auth_revocations auth_revocations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_revocations
    ADD CONSTRAINT auth_revocations_pkey PRIMARY KEY (revocation_id);


--
-- Name: auth_sessions auth_sessions_legacy_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_sessions
    ADD CONSTRAINT auth_sessions_legacy_uk UNIQUE (legacy_source, legacy_id);


--
-- Name: auth_sessions auth_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_sessions
    ADD CONSTRAINT auth_sessions_pkey PRIMARY KEY (session_id);


--
-- Name: care_facilities care_facilities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_facilities
    ADD CONSTRAINT care_facilities_pkey PRIMARY KEY (facility_id);


--
-- Name: care_group_members care_group_members_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_group_members
    ADD CONSTRAINT care_group_members_pkey PRIMARY KEY (care_group_member_id);


--
-- Name: care_groups care_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_groups
    ADD CONSTRAINT care_groups_pkey PRIMARY KEY (care_group_id);


--
-- Name: care_item_templates care_item_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_item_templates
    ADD CONSTRAINT care_item_templates_pkey PRIMARY KEY (template_id);


--
-- Name: care_logs care_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_logs
    ADD CONSTRAINT care_logs_pkey PRIMARY KEY (care_log_id);


--
-- Name: care_subjects care_subjects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_subjects
    ADD CONSTRAINT care_subjects_pkey PRIMARY KEY (care_subject_id);


--
-- Name: community_content community_content_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.community_content
    ADD CONSTRAINT community_content_pkey PRIMARY KEY (content_id);


--
-- Name: community_interactions community_interactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.community_interactions
    ADD CONSTRAINT community_interactions_pkey PRIMARY KEY (interaction_id);


--
-- Name: community_profiles community_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.community_profiles
    ADD CONSTRAINT community_profiles_pkey PRIMARY KEY (community_profile_id);


--
-- Name: community_topics community_topics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.community_topics
    ADD CONSTRAINT community_topics_pkey PRIMARY KEY (id);


--
-- Name: community_topics community_topics_slug_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.community_topics
    ADD CONSTRAINT community_topics_slug_unique UNIQUE (slug);


--
-- Name: content_item_sources content_item_sources_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_item_sources
    ADD CONSTRAINT content_item_sources_pkey PRIMARY KEY (content_item_source_id);


--
-- Name: content_item_sources content_item_sources_unique_url_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_item_sources
    ADD CONSTRAINT content_item_sources_unique_url_uk UNIQUE (content_item_id, source_url);


--
-- Name: content_item_topics content_item_topics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_item_topics
    ADD CONSTRAINT content_item_topics_pkey PRIMARY KEY (content_item_id, topic_id);


--
-- Name: content_items content_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_items
    ADD CONSTRAINT content_items_pkey PRIMARY KEY (content_item_id);


--
-- Name: data_permissions data_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_permissions
    ADD CONSTRAINT data_permissions_pkey PRIMARY KEY (permission_id);


--
-- Name: development_milestones development_milestones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.development_milestones
    ADD CONSTRAINT development_milestones_pkey PRIMARY KEY (milestone_id);


--
-- Name: device_connections device_connections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_connections
    ADD CONSTRAINT device_connections_pkey PRIMARY KEY (device_connection_id);


--
-- Name: device_tokens device_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_tokens
    ADD CONSTRAINT device_tokens_pkey PRIMARY KEY (id);


--
-- Name: device_tokens device_tokens_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_tokens
    ADD CONSTRAINT device_tokens_unique UNIQUE (user_id, token);


--
-- Name: emergency_contacts emergency_contacts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.emergency_contacts
    ADD CONSTRAINT emergency_contacts_pkey PRIMARY KEY (id);


--
-- Name: emergency_contacts emergency_contacts_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.emergency_contacts
    ADD CONSTRAINT emergency_contacts_user_id_key UNIQUE (user_id);


--
-- Name: expense_entries expense_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense_entries
    ADD CONSTRAINT expense_entries_pkey PRIMARY KEY (expense_entry_id);


--
-- Name: expert_availability expert_availability_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expert_availability
    ADD CONSTRAINT expert_availability_pkey PRIMARY KEY (availability_id);


--
-- Name: expert_contribution_events expert_contribution_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expert_contribution_events
    ADD CONSTRAINT expert_contribution_events_pkey PRIMARY KEY (contribution_event_id);


--
-- Name: expert_credentials expert_credentials_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expert_credentials
    ADD CONSTRAINT expert_credentials_pkey PRIMARY KEY (credential_id);


--
-- Name: expert_location_shares expert_location_shares_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expert_location_shares
    ADD CONSTRAINT expert_location_shares_pkey PRIMARY KEY (location_share_id);


--
-- Name: family_tasks family_tasks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.family_tasks
    ADD CONSTRAINT family_tasks_pkey PRIMARY KEY (task_id);


--
-- Name: growth_measurements growth_measurements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.growth_measurements
    ADD CONSTRAINT growth_measurements_pkey PRIMARY KEY (growth_measurement_id);


--
-- Name: health_context_memories health_context_memories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_context_memories
    ADD CONSTRAINT health_context_memories_pkey PRIMARY KEY (memory_id);


--
-- Name: health_observations health_observations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_observations
    ADD CONSTRAINT health_observations_pkey PRIMARY KEY (health_observation_id);


--
-- Name: health_record_attachments health_record_attachments_pair_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_record_attachments
    ADD CONSTRAINT health_record_attachments_pair_uk UNIQUE (health_record_id, attachment_id);


--
-- Name: health_record_attachments health_record_attachments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_record_attachments
    ADD CONSTRAINT health_record_attachments_pkey PRIMARY KEY (health_record_attachment_id);


--
-- Name: health_records health_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_records
    ADD CONSTRAINT health_records_pkey PRIMARY KEY (health_record_id);


--
-- Name: knowledge_source_reviews knowledge_source_reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_source_reviews
    ADD CONSTRAINT knowledge_source_reviews_pkey PRIMARY KEY (review_id);


--
-- Name: knowledge_sources knowledge_sources_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_sources
    ADD CONSTRAINT knowledge_sources_pkey PRIMARY KEY (knowledge_source_id);


--
-- Name: maternal_exercise_sessions maternal_exercise_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maternal_exercise_sessions
    ADD CONSTRAINT maternal_exercise_sessions_pkey PRIMARY KEY (exercise_session_id);


--
-- Name: maternal_observations maternal_observations_legacy_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maternal_observations
    ADD CONSTRAINT maternal_observations_legacy_uk UNIQUE (legacy_source, legacy_id);


--
-- Name: maternal_observations maternal_observations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maternal_observations
    ADD CONSTRAINT maternal_observations_pkey PRIMARY KEY (observation_id);


--
-- Name: moderation_cases moderation_cases_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_cases
    ADD CONSTRAINT moderation_cases_pkey PRIMARY KEY (moderation_case_id);


--
-- Name: moderation_events moderation_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_events
    ADD CONSTRAINT moderation_events_pkey PRIMARY KEY (moderation_event_id);


--
-- Name: mother_journey_events mother_journey_events_legacy_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mother_journey_events
    ADD CONSTRAINT mother_journey_events_legacy_uk UNIQUE (legacy_source, legacy_id);


--
-- Name: mother_journey_events mother_journey_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mother_journey_events
    ADD CONSTRAINT mother_journey_events_pkey PRIMARY KEY (event_id);


--
-- Name: mother_journeys mother_journeys_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mother_journeys
    ADD CONSTRAINT mother_journeys_pkey PRIMARY KEY (journey_id);


--
-- Name: mother_journeys mother_journeys_subject_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mother_journeys
    ADD CONSTRAINT mother_journeys_subject_uk UNIQUE (care_subject_id);


--
-- Name: nearby_support_requests nearby_support_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nearby_support_requests
    ADD CONSTRAINT nearby_support_requests_pkey PRIMARY KEY (request_id);


--
-- Name: nearby_support_responses nearby_support_responses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nearby_support_responses
    ADD CONSTRAINT nearby_support_responses_pkey PRIMARY KEY (response_id);


--
-- Name: notification_records notification_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_records
    ADD CONSTRAINT notification_records_pkey PRIMARY KEY (id);


--
-- Name: persons persons_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.persons
    ADD CONSTRAINT persons_pkey PRIMARY KEY (person_id);


--
-- Name: preparation_checklist_items preparation_checklist_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preparation_checklist_items
    ADD CONSTRAINT preparation_checklist_items_pkey PRIMARY KEY (checklist_item_id);


--
-- Name: professional_profiles professional_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.professional_profiles
    ADD CONSTRAINT professional_profiles_pkey PRIMARY KEY (professional_profile_id);


--
-- Name: professional_profiles professional_profiles_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.professional_profiles
    ADD CONSTRAINT professional_profiles_user_id_key UNIQUE (user_id);


--
-- Name: professional_specialties professional_specialties_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.professional_specialties
    ADD CONSTRAINT professional_specialties_pkey PRIMARY KEY (professional_profile_id, specialty_id);


--
-- Name: red_flag_rules red_flag_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.red_flag_rules
    ADD CONSTRAINT red_flag_rules_pkey PRIMARY KEY (id);


--
-- Name: safety_configs safety_configs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_configs
    ADD CONSTRAINT safety_configs_pkey PRIMARY KEY (safety_config_id);


--
-- Name: safety_configs safety_configs_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_configs
    ADD CONSTRAINT safety_configs_user_id_key UNIQUE (user_id);


--
-- Name: safety_event_actions safety_event_actions_idempotency_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_event_actions
    ADD CONSTRAINT safety_event_actions_idempotency_uk UNIQUE (idempotency_key);


--
-- Name: safety_event_actions safety_event_actions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_event_actions
    ADD CONSTRAINT safety_event_actions_pkey PRIMARY KEY (safety_event_action_id);


--
-- Name: safety_events safety_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_events
    ADD CONSTRAINT safety_events_pkey PRIMARY KEY (safety_event_id);


--
-- Name: safety_monitoring_sessions safety_monitoring_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_monitoring_sessions
    ADD CONSTRAINT safety_monitoring_sessions_pkey PRIMARY KEY (monitoring_session_id);


--
-- Name: scheduled_care_items scheduled_care_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheduled_care_items
    ADD CONSTRAINT scheduled_care_items_pkey PRIMARY KEY (care_item_id);


--
-- Name: security_events security_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.security_events
    ADD CONSTRAINT security_events_pkey PRIMARY KEY (id);


--
-- Name: specialties specialties_canonical_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.specialties
    ADD CONSTRAINT specialties_canonical_code_key UNIQUE (code);


--
-- Name: specialties specialties_canonical_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.specialties
    ADD CONSTRAINT specialties_canonical_pkey PRIMARY KEY (specialty_id);


--
-- Name: system_configurations system_configurations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.system_configurations
    ADD CONSTRAINT system_configurations_pkey PRIMARY KEY (system_configuration_id);


--
-- Name: triage_session_evidence triage_session_evidence_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.triage_session_evidence
    ADD CONSTRAINT triage_session_evidence_pkey PRIMARY KEY (evidence_id);


--
-- Name: triage_session_evidence triage_session_evidence_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.triage_session_evidence
    ADD CONSTRAINT triage_session_evidence_uk UNIQUE (triage_session_id, evidence_type, content_hash);


--
-- Name: triage_sessions triage_sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.triage_sessions
    ADD CONSTRAINT triage_sessions_pkey PRIMARY KEY (triage_session_id);


--
-- Name: users uk6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: user_identities uk_user_identities_provider_subject; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_identities
    ADD CONSTRAINT uk_user_identities_provider_subject UNIQUE (provider, provider_subject);


--
-- Name: user_identities uk_user_identities_user_provider; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_identities
    ADD CONSTRAINT uk_user_identities_user_provider UNIQUE (user_id, provider);


--
-- Name: red_flag_rules uq_red_flag_rules_keyword; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.red_flag_rules
    ADD CONSTRAINT uq_red_flag_rules_keyword UNIQUE (keyword);


--
-- Name: user_identities user_identities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_identities
    ADD CONSTRAINT user_identities_pkey PRIMARY KEY (identity_id);


--
-- Name: users users_person_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_person_uk UNIQUE (person_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- Name: vaccination_records vaccination_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vaccination_records
    ADD CONSTRAINT vaccination_records_pkey PRIMARY KEY (vaccination_record_id);


--
-- Name: vaccination_schedules vaccination_schedules_key_uk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vaccination_schedules
    ADD CONSTRAINT vaccination_schedules_key_uk UNIQUE (vaccine_name, dose_number, schedule_version);


--
-- Name: vaccination_schedules vaccination_schedules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vaccination_schedules
    ADD CONSTRAINT vaccination_schedules_pkey PRIMARY KEY (vaccination_schedule_id);


--
-- Name: archived_consultation_booking_requester_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX archived_consultation_booking_requester_ix ON public.archived_consultation_records USING btree (requester_user_id, status, original_created_at DESC) WHERE ((legacy_table)::text = 'consultation_bookings'::text);


--
-- Name: archived_partner_email_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX archived_partner_email_uk ON public.archived_partner_records USING btree (email) WHERE ((legacy_table)::text = 'partner_organizations'::text);


--
-- Name: archived_partner_representative_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX archived_partner_representative_uk ON public.archived_partner_records USING btree (representative_user_id) WHERE ((legacy_table)::text = 'partner_organizations'::text);


--
-- Name: archived_realtime_call_timeline_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX archived_realtime_call_timeline_ix ON public.archived_realtime_records USING btree (conversation_id, initiated_at DESC) WHERE ((legacy_table)::text = 'conversation_calls'::text);


--
-- Name: archived_realtime_conversation_pair_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX archived_realtime_conversation_pair_uk ON public.archived_realtime_records USING btree (mother_user_id, expert_user_id) WHERE ((legacy_table)::text = 'direct_conversations'::text);


--
-- Name: archived_realtime_message_client_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX archived_realtime_message_client_uk ON public.archived_realtime_records USING btree (conversation_id, sender_user_id, client_message_id) WHERE ((legacy_table)::text = 'direct_messages'::text);


--
-- Name: archived_realtime_message_timeline_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX archived_realtime_message_timeline_ix ON public.archived_realtime_records USING btree (conversation_id, original_created_at DESC) WHERE ((legacy_table)::text = 'direct_messages'::text);


--
-- Name: audit_events_category_time_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX audit_events_category_time_ix ON public.audit_events USING btree (event_category, occurred_at);


--
-- Name: audit_events_origin_time_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX audit_events_origin_time_ix ON public.audit_events USING btree (event_origin, occurred_at DESC);


--
-- Name: audit_events_security_note_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX audit_events_security_note_ix ON public.audit_events USING btree (security_event_id, occurred_at) WHERE ((event_category)::text = 'SECURITY_INVESTIGATION_NOTE'::text);


--
-- Name: audit_events_subject_time_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX audit_events_subject_time_ix ON public.audit_events USING btree (subject_user_id, occurred_at);


--
-- Name: auth_challenges_subject_expiry_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_challenges_subject_expiry_ix ON public.auth_challenges USING btree (subject_identifier, challenge_type, expires_at);


--
-- Name: auth_revocations_token_hash_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX auth_revocations_token_hash_uk ON public.auth_revocations USING btree (token_hash) WHERE (token_hash IS NOT NULL);


--
-- Name: auth_revocations_user_expiry_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_revocations_user_expiry_ix ON public.auth_revocations USING btree (user_id, expires_at);


--
-- Name: auth_sessions_family_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_sessions_family_ix ON public.auth_sessions USING btree (token_family_id);


--
-- Name: auth_sessions_user_device_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_sessions_user_device_ix ON public.auth_sessions USING btree (user_id, device_identifier, status);


--
-- Name: care_facilities_area_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX care_facilities_area_ix ON public.care_facilities USING btree (administrative_area_id);


--
-- Name: care_item_templates_content_status_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX care_item_templates_content_status_ix ON public.care_item_templates USING btree (entry_type, content_status, stage, display_order);


--
-- Name: care_item_templates_exercise_filter_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX care_item_templates_exercise_filter_ix ON public.care_item_templates USING btree (template_status, stage, difficulty_level, created_at DESC) WHERE ((entry_type)::text = 'EXERCISE_TEMPLATE'::text);


--
-- Name: care_item_templates_parent_order_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX care_item_templates_parent_order_ix ON public.care_item_templates USING btree (parent_template_id, display_order);


--
-- Name: care_item_templates_posture_version_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX care_item_templates_posture_version_ix ON public.care_item_templates USING btree (parent_template_id, template_status, effective_from DESC) WHERE ((entry_type)::text = 'POSTURE_CONFIG'::text);


--
-- Name: care_logs_subject_type_time_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX care_logs_subject_type_time_ix ON public.care_logs USING btree (care_subject_id, log_type, started_at);


--
-- Name: community_content_parent_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX community_content_parent_ix ON public.community_content USING btree (parent_content_id);


--
-- Name: community_content_topic_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX community_content_topic_ix ON public.community_content USING btree (topic_id, created_at);


--
-- Name: community_interactions_content_target_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX community_interactions_content_target_uk ON public.community_interactions USING btree (actor_user_id, interaction_type, content_id) WHERE (content_id IS NOT NULL);


--
-- Name: community_interactions_topic_target_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX community_interactions_topic_target_uk ON public.community_interactions USING btree (actor_user_id, interaction_type, topic_id) WHERE (topic_id IS NOT NULL);


--
-- Name: data_permissions_consent_owner_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX data_permissions_consent_owner_ix ON public.data_permissions USING btree (owner_user_id, granted_at DESC) WHERE ((permission_kind)::text = 'CONSENT_GRANT'::text);


--
-- Name: data_permissions_legacy_consent_id_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX data_permissions_legacy_consent_id_uk ON public.data_permissions USING btree (legacy_consent_id) WHERE (legacy_consent_id IS NOT NULL);


--
-- Name: development_milestones_subject_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX development_milestones_subject_ix ON public.development_milestones USING btree (care_subject_id, milestone_type, achieved_date);


--
-- Name: device_connections_user_status_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX device_connections_user_status_ix ON public.device_connections USING btree (user_id, status);


--
-- Name: expense_entries_owner_date_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX expense_entries_owner_date_ix ON public.expense_entries USING btree (owner_user_id, expense_date);


--
-- Name: expert_availability_profile_window_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX expert_availability_profile_window_ix ON public.expert_availability USING btree (professional_profile_id, start_at, end_at);


--
-- Name: expert_contribution_profile_time_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX expert_contribution_profile_time_ix ON public.expert_contribution_events USING btree (professional_profile_id, created_at);


--
-- Name: expert_credentials_profile_status_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX expert_credentials_profile_status_ix ON public.expert_credentials USING btree (professional_profile_id, review_status);


--
-- Name: family_tasks_assignee_status_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX family_tasks_assignee_status_ix ON public.family_tasks USING btree (assignee_user_id, status, due_at);


--
-- Name: growth_measurements_chart_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX growth_measurements_chart_ix ON public.growth_measurements USING btree (care_subject_id, measured_date) WHERE (deleted_at IS NULL);


--
-- Name: health_context_memories_baby_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX health_context_memories_baby_ix ON public.health_context_memories USING btree (baby_profile_id, created_at DESC) WHERE (deleted_at IS NULL);


--
-- Name: health_context_memories_mother_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX health_context_memories_mother_ix ON public.health_context_memories USING btree (mother_profile_id, created_at DESC) WHERE (deleted_at IS NULL);


--
-- Name: health_context_memories_subject_expiry_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX health_context_memories_subject_expiry_ix ON public.health_context_memories USING btree (care_subject_id, expires_at) WHERE (deleted_at IS NULL);


--
-- Name: health_observations_device_time_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX health_observations_device_time_ix ON public.health_observations USING btree (device_connection_id, observed_at);


--
-- Name: health_observations_subject_chart_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX health_observations_subject_chart_ix ON public.health_observations USING btree (care_subject_id, observation_type, observed_at);


--
-- Name: health_record_attachments_record_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX health_record_attachments_record_ix ON public.health_record_attachments USING btree (health_record_id, display_order);


--
-- Name: health_records_summary_filter_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX health_records_summary_filter_ix ON public.health_records USING btree (owner_user_id, summary_period, record_date DESC) WHERE (((record_type)::text = 'SUMMARY'::text) AND ((status)::text = 'ACTIVE'::text));


--
-- Name: idx_account_deletion_requests_scheduled_for; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_deletion_requests_scheduled_for ON public.account_deletion_requests USING btree (scheduled_for) WHERE ((status)::text = 'PENDING'::text);


--
-- Name: idx_account_deletion_requests_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_deletion_requests_status ON public.account_deletion_requests USING btree (status);


--
-- Name: idx_account_deletion_requests_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_deletion_requests_user_id ON public.account_deletion_requests USING btree (user_id);


--
-- Name: idx_care_facilities_facility_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_care_facilities_facility_type ON public.care_facilities USING btree (facility_type);


--
-- Name: idx_care_facilities_nearby_eligible; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_care_facilities_nearby_eligible ON public.care_facilities USING btree (facility_type, province_id, district_id) WHERE ((is_active = true) AND (is_searchable = true) AND (latitude IS NOT NULL) AND (longitude IS NOT NULL));


--
-- Name: idx_care_facilities_partner_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_care_facilities_partner_id ON public.care_facilities USING btree (partner_id);


--
-- Name: idx_care_group_members_care_group_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_care_group_members_care_group_id ON public.care_group_members USING btree (care_group_id);


--
-- Name: idx_care_group_members_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_care_group_members_user_id ON public.care_group_members USING btree (user_id);


--
-- Name: idx_care_groups_owner_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_care_groups_owner_user_id ON public.care_groups USING btree (owner_user_id);


--
-- Name: idx_community_topics_hidden; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_community_topics_hidden ON public.community_topics USING btree (is_hidden);


--
-- Name: idx_community_topics_name_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_community_topics_name_lower ON public.community_topics USING btree (lower((name)::text));


--
-- Name: idx_community_topics_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_community_topics_parent_id ON public.community_topics USING btree (parent_id);


--
-- Name: idx_community_topics_sort_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_community_topics_sort_order ON public.community_topics USING btree (sort_order);


--
-- Name: idx_community_topics_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_community_topics_type ON public.community_topics USING btree (type);


--
-- Name: idx_content_items_published_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_items_published_at ON public.content_items USING btree (published_at DESC NULLS LAST) WHERE ((status)::text = 'APPROVED'::text);


--
-- Name: idx_content_items_stage; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_items_stage ON public.content_items USING btree (stage);


--
-- Name: idx_content_items_stage_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_items_stage_status ON public.content_items USING btree (stage, status);


--
-- Name: idx_content_items_stage_type_approved; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_items_stage_type_approved ON public.content_items USING btree (stage, content_type, status) WHERE ((status)::text = 'APPROVED'::text);


--
-- Name: idx_content_items_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_items_status ON public.content_items USING btree (status);


--
-- Name: idx_content_items_title_search; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_items_title_search ON public.content_items USING btree (lower((title)::text));


--
-- Name: idx_content_items_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_items_type ON public.content_items USING btree (content_type);


--
-- Name: idx_content_items_type_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_content_items_type_status ON public.content_items USING btree (content_type, status);


--
-- Name: idx_development_milestones_baby_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_development_milestones_baby_id ON public.development_milestones USING btree (baby_id);


--
-- Name: idx_development_milestones_baby_record_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_development_milestones_baby_record_status ON public.development_milestones USING btree (baby_id, record_status);


--
-- Name: idx_device_tokens_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_device_tokens_user_id ON public.device_tokens USING btree (user_id);


--
-- Name: idx_emergency_contacts_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_emergency_contacts_user_id ON public.emergency_contacts USING btree (user_id);


--
-- Name: idx_expert_availability_start_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expert_availability_start_at ON public.expert_availability USING btree (start_at);


--
-- Name: idx_expert_availability_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expert_availability_status ON public.expert_availability USING btree (status);


--
-- Name: idx_expert_credentials_file_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expert_credentials_file_id ON public.expert_credentials USING btree (file_id);


--
-- Name: idx_expert_credentials_review_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_expert_credentials_review_status ON public.expert_credentials USING btree (reviewed_by, reviewed_at);


--
-- Name: idx_growth_measurements_baby_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_growth_measurements_baby_id ON public.growth_measurements USING btree (baby_id);


--
-- Name: idx_health_records_baby_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_health_records_baby_id ON public.health_records USING btree (baby_id);


--
-- Name: idx_health_records_journey_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_health_records_journey_id ON public.health_records USING btree (journey_id);


--
-- Name: idx_health_records_owner_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_health_records_owner_user_id ON public.health_records USING btree (owner_user_id);


--
-- Name: idx_mother_journeys_owner_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mother_journeys_owner_user_id ON public.mother_journeys USING btree (owner_user_id);


--
-- Name: idx_mother_journeys_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mother_journeys_status ON public.mother_journeys USING btree (status);


--
-- Name: idx_nearby_support_requests_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_nearby_support_requests_status ON public.nearby_support_requests USING btree (status, created_at);


--
-- Name: idx_nearby_support_responses_expert; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_nearby_support_responses_expert ON public.nearby_support_responses USING btree (expert_profile_id);


--
-- Name: idx_nearby_support_responses_request; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_nearby_support_responses_request ON public.nearby_support_responses USING btree (request_id);


--
-- Name: idx_notification_records_type_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notification_records_type_status ON public.notification_records USING btree (type, status);


--
-- Name: idx_notification_records_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notification_records_user_id ON public.notification_records USING btree (user_id, created_at DESC);


--
-- Name: idx_notification_records_user_unread; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notification_records_user_unread ON public.notification_records USING btree (user_id, is_read) WHERE (is_read = false);


--
-- Name: idx_red_flag_rules_active_severity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_red_flag_rules_active_severity ON public.red_flag_rules USING btree (is_active, severity);


--
-- Name: idx_red_flag_rules_is_system_default; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_red_flag_rules_is_system_default ON public.red_flag_rules USING btree (is_system_default);


--
-- Name: idx_security_events_correlation_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_security_events_correlation_id ON public.security_events USING btree (correlation_id);


--
-- Name: idx_security_events_event_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_security_events_event_type ON public.security_events USING btree (event_type, occurred_at DESC);


--
-- Name: idx_security_events_ip_address; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_security_events_ip_address ON public.security_events USING btree (ip_address);


--
-- Name: idx_security_events_status_occurred; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_security_events_status_occurred ON public.security_events USING btree (status, occurred_at DESC);


--
-- Name: idx_security_events_status_severity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_security_events_status_severity ON public.security_events USING btree (status, severity);


--
-- Name: idx_security_events_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_security_events_user_id ON public.security_events USING btree (user_id, occurred_at DESC);


--
-- Name: idx_user_identities_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_identities_user_id ON public.user_identities USING btree (user_id);


--
-- Name: idx_vaccination_records_baby_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vaccination_records_baby_id ON public.vaccination_records USING btree (baby_id);


--
-- Name: idx_vaccination_records_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vaccination_records_status ON public.vaccination_records USING btree (status);


--
-- Name: knowledge_source_reviews_source_time_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX knowledge_source_reviews_source_time_ix ON public.knowledge_source_reviews USING btree (knowledge_source_id, changed_at);


--
-- Name: knowledge_sources_domain_status_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX knowledge_sources_domain_status_ix ON public.knowledge_sources USING btree (domain, status);


--
-- Name: knowledge_sources_domain_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX knowledge_sources_domain_uk ON public.knowledge_sources USING btree (lower((domain)::text));


--
-- Name: maternal_observations_chart_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX maternal_observations_chart_ix ON public.maternal_observations USING btree (mother_journey_id, observation_type, observed_at);


--
-- Name: maternal_observations_safety_query_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX maternal_observations_safety_query_ix ON public.maternal_observations USING btree (exercise_template_id, owner_user_id, created_at DESC) WHERE ((legacy_source)::text = ANY ((ARRAY['EXERCISE_SAFETY'::character varying, 'EXERCISE_SAFETY_ANSWER'::character varying])::text[]));


--
-- Name: maternal_observations_severity_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX maternal_observations_severity_ix ON public.maternal_observations USING btree (severity, observed_at) WHERE (severity IS NOT NULL);


--
-- Name: moderation_cases_report_source_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX moderation_cases_report_source_ix ON public.moderation_cases USING btree (report_source, status, opened_at DESC);


--
-- Name: moderation_cases_target_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX moderation_cases_target_ix ON public.moderation_cases USING btree (target_type, target_id, status);


--
-- Name: moderation_events_case_time_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX moderation_events_case_time_ix ON public.moderation_events USING btree (moderation_case_id, action_at);


--
-- Name: moderation_events_target_history_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX moderation_events_target_history_ix ON public.moderation_events USING btree (target_type, target_id, action_at DESC);


--
-- Name: mother_journey_events_baby_link_submission_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX mother_journey_events_baby_link_submission_uk ON public.mother_journey_events USING btree (owner_user_id, operation_type, submission_id, legacy_source) WHERE ((legacy_source)::text = 'BABY_LINK'::text);


--
-- Name: mother_journey_events_baseline_submission_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX mother_journey_events_baseline_submission_uk ON public.mother_journey_events USING btree (owner_user_id, submission_id, legacy_source) WHERE ((legacy_source)::text = 'MOTHER_BASELINE'::text);


--
-- Name: mother_journey_events_journey_time_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX mother_journey_events_journey_time_ix ON public.mother_journey_events USING btree (mother_journey_id, effective_at);


--
-- Name: mother_journey_events_outcome_submission_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX mother_journey_events_outcome_submission_uk ON public.mother_journey_events USING btree (mother_journey_id, submission_id, legacy_source) WHERE ((legacy_source)::text = 'PREGNANCY_OUTCOME'::text);


--
-- Name: nearby_support_responses_professional_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX nearby_support_responses_professional_ix ON public.nearby_support_responses USING btree (professional_profile_id);


--
-- Name: preparation_checklist_filter_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX preparation_checklist_filter_ix ON public.preparation_checklist_items USING btree (owner_user_id, mother_journey_id, baby_id, status, display_order);


--
-- Name: preparation_checklist_owner_journey_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX preparation_checklist_owner_journey_ix ON public.preparation_checklist_items USING btree (owner_user_id, mother_journey_id, display_order);


--
-- Name: professional_specialties_specialty_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX professional_specialties_specialty_ix ON public.professional_specialties USING btree (specialty_id);


--
-- Name: safety_event_actions_attempt_event_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX safety_event_actions_attempt_event_uk ON public.safety_event_actions USING btree (safety_event_id) WHERE ((action_type)::text = 'ALERT_ATTEMPT'::text);


--
-- Name: safety_event_actions_delivery_device_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX safety_event_actions_delivery_device_uk ON public.safety_event_actions USING btree (safety_event_id, recipient_user_id, device_identifier) WHERE ((action_type)::text = ANY ((ARRAY['DELIVERY'::character varying, 'FAMILY_ALERT'::character varying])::text[]));


--
-- Name: safety_event_actions_delivery_token_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX safety_event_actions_delivery_token_uk ON public.safety_event_actions USING btree (safety_event_id, device_token_id) WHERE ((action_type)::text = 'DELIVERY'::text);


--
-- Name: safety_event_actions_event_status_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX safety_event_actions_event_status_ix ON public.safety_event_actions USING btree (safety_event_id, delivery_status, created_at);


--
-- Name: safety_event_actions_family_alert_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX safety_event_actions_family_alert_uk ON public.safety_event_actions USING btree (safety_event_id) WHERE ((action_type)::text = 'FAMILY_ALERT'::text);


--
-- Name: safety_event_actions_handoff_status_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX safety_event_actions_handoff_status_ix ON public.safety_event_actions USING btree (action_status, created_at DESC) WHERE ((action_type)::text = 'MAP_HANDOFF'::text);


--
-- Name: safety_event_actions_owner_location_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX safety_event_actions_owner_location_ix ON public.safety_event_actions USING btree (owner_user_id, captured_at DESC) WHERE ((action_type)::text = 'LOCATION_SNAPSHOT'::text);


--
-- Name: safety_event_actions_terminal_response_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX safety_event_actions_terminal_response_uk ON public.safety_event_actions USING btree (safety_event_id) WHERE (response_type IS NOT NULL);


--
-- Name: safety_events_imu_signal_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX safety_events_imu_signal_uk ON public.safety_events USING btree (monitoring_session_id, signal_key) WHERE (((record_type)::text = 'IMU_EVENT'::text) AND (signal_key IS NOT NULL));


--
-- Name: safety_events_pending_countdown_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX safety_events_pending_countdown_ix ON public.safety_events USING btree (countdown_deadline_at) WHERE (((record_type)::text = 'IMU_EVENT'::text) AND ((status)::text = 'OPEN'::text) AND (response_type IS NULL));


--
-- Name: safety_events_user_status_time_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX safety_events_user_status_time_ix ON public.safety_events USING btree (user_id, status, detected_at);


--
-- Name: safety_monitoring_sessions_user_status_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX safety_monitoring_sessions_user_status_ix ON public.safety_monitoring_sessions USING btree (user_id, status);


--
-- Name: scheduled_care_items_context_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX scheduled_care_items_context_ix ON public.scheduled_care_items USING btree (owner_user_id, journey_id, baby_id, status, scheduled_at);


--
-- Name: scheduled_care_items_owner_status_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX scheduled_care_items_owner_status_ix ON public.scheduled_care_items USING btree (owner_user_id, status, scheduled_at);


--
-- Name: triage_session_evidence_session_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX triage_session_evidence_session_ix ON public.triage_session_evidence USING btree (triage_session_id);


--
-- Name: triage_sessions_owner_request_uk; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX triage_sessions_owner_request_uk ON public.triage_sessions USING btree (user_id, client_request_id) WHERE (client_request_id IS NOT NULL);


--
-- Name: triage_sessions_risk_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX triage_sessions_risk_ix ON public.triage_sessions USING btree (risk_level, emergency, created_at);


--
-- Name: triage_sessions_stage_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX triage_sessions_stage_ix ON public.triage_sessions USING btree (stage, created_at);


--
-- Name: triage_sessions_user_time_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX triage_sessions_user_time_ix ON public.triage_sessions USING btree (user_id, created_at);


--
-- Name: uq_care_facilities_external_source; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_care_facilities_external_source ON public.care_facilities USING btree (source_type, external_source_id) WHERE (external_source_id IS NOT NULL);


--
-- Name: uq_care_group_members_invite_token; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_care_group_members_invite_token ON public.care_group_members USING btree (invite_token) WHERE (invite_token IS NOT NULL);


--
-- Name: uq_mother_journeys_one_canonical_active; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_mother_journeys_one_canonical_active ON public.mother_journeys USING btree (owner_user_id) WHERE (((status)::text = 'ACTIVE'::text) AND ((journey_type)::text = ANY ((ARRAY['PRE_PREGNANCY'::character varying, 'PREGNANCY'::character varying, 'POSTPARTUM'::character varying])::text[])));


--
-- Name: uq_notification_records_direct_message; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_notification_records_direct_message ON public.notification_records USING btree (user_id, reference_id) WHERE (((type)::text = 'MESSAGE'::text) AND ((reference_type)::text = 'DIRECT_MESSAGE'::text));


--
-- Name: uq_preparation_checklist_import_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_preparation_checklist_import_scope ON public.preparation_checklist_items USING btree (owner_user_id, mother_journey_id, baby_id, template_entry_id) NULLS NOT DISTINCT WHERE (template_entry_id IS NOT NULL);


--
-- Name: vaccination_records_subject_status_ix; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX vaccination_records_subject_status_ix ON public.vaccination_records USING btree (care_subject_id, status, scheduled_date);


--
-- Name: audit_events audit_events_immutable_trg; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER audit_events_immutable_trg BEFORE DELETE OR UPDATE ON public.audit_events FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();


--
-- Name: auth_revocations auth_revocations_immutable_trg; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER auth_revocations_immutable_trg BEFORE DELETE OR UPDATE ON public.auth_revocations FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();


--
-- Name: expert_contribution_events expert_contribution_events_immutable_trg; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER expert_contribution_events_immutable_trg BEFORE DELETE OR UPDATE ON public.expert_contribution_events FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();


--
-- Name: expert_contribution_events expert_contribution_fill_profile_trg; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER expert_contribution_fill_profile_trg BEFORE INSERT ON public.expert_contribution_events FOR EACH ROW EXECUTE FUNCTION public.fill_contribution_profile();


--
-- Name: knowledge_source_reviews knowledge_source_reviews_immutable_trg; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER knowledge_source_reviews_immutable_trg BEFORE DELETE OR UPDATE ON public.knowledge_source_reviews FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();


--
-- Name: moderation_events moderation_events_immutable_trg; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER moderation_events_immutable_trg BEFORE DELETE OR UPDATE ON public.moderation_events FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();


--
-- Name: mother_journey_events mother_journey_events_immutable_trg; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER mother_journey_events_immutable_trg BEFORE DELETE OR UPDATE ON public.mother_journey_events FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();


--
-- Name: mother_journey_events mother_journey_events_owner_trg; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER mother_journey_events_owner_trg BEFORE INSERT ON public.mother_journey_events FOR EACH ROW EXECUTE FUNCTION public.enforce_mother_journey_event_owner();


--
-- Name: community_topics trg_community_topic_parent_category; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_community_topic_parent_category BEFORE INSERT OR UPDATE OF type, parent_id ON public.community_topics FOR EACH ROW EXECUTE FUNCTION public.enforce_community_topic_parent_category();


--
-- Name: triage_session_evidence triage_session_evidence_immutable_trg; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER triage_session_evidence_immutable_trg BEFORE DELETE OR UPDATE ON public.triage_session_evidence FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();


--
-- Name: administrative_areas administrative_areas_parent_area_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.administrative_areas
    ADD CONSTRAINT administrative_areas_parent_area_id_fkey FOREIGN KEY (parent_area_id) REFERENCES public.administrative_areas(administrative_area_id);


--
-- Name: archived_consultation_records archived_consultation_availability_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_consultation_records
    ADD CONSTRAINT archived_consultation_availability_fk FOREIGN KEY (availability_id) REFERENCES public.expert_availability(availability_id);


--
-- Name: archived_consultation_records archived_consultation_booking_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_consultation_records
    ADD CONSTRAINT archived_consultation_booking_fk FOREIGN KEY (booking_id) REFERENCES public.archived_consultation_records(archive_id);


--
-- Name: archived_consultation_records archived_consultation_configured_by_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_consultation_records
    ADD CONSTRAINT archived_consultation_configured_by_fk FOREIGN KEY (configured_by) REFERENCES public.users(user_id);


--
-- Name: archived_consultation_records archived_consultation_expert_price_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_consultation_records
    ADD CONSTRAINT archived_consultation_expert_price_fk FOREIGN KEY (expert_price_id) REFERENCES public.archived_consultation_records(archive_id);


--
-- Name: archived_consultation_records archived_consultation_expert_profile_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_consultation_records
    ADD CONSTRAINT archived_consultation_expert_profile_fk FOREIGN KEY (expert_profile_id) REFERENCES public.professional_profiles(professional_profile_id);


--
-- Name: archived_consultation_records archived_consultation_price_band_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_consultation_records
    ADD CONSTRAINT archived_consultation_price_band_fk FOREIGN KEY (price_band_id) REFERENCES public.archived_consultation_records(archive_id);


--
-- Name: archived_consultation_records archived_consultation_records_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_consultation_records
    ADD CONSTRAINT archived_consultation_records_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: archived_consultation_records archived_consultation_requester_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_consultation_records
    ADD CONSTRAINT archived_consultation_requester_fk FOREIGN KEY (requester_user_id) REFERENCES public.users(user_id);


--
-- Name: archived_partner_records archived_partner_records_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_partner_records
    ADD CONSTRAINT archived_partner_records_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: archived_partner_records archived_partner_representative_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_partner_records
    ADD CONSTRAINT archived_partner_representative_fk FOREIGN KEY (representative_user_id) REFERENCES public.users(user_id);


--
-- Name: archived_realtime_records archived_realtime_conversation_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_realtime_records
    ADD CONSTRAINT archived_realtime_conversation_fk FOREIGN KEY (conversation_id) REFERENCES public.archived_realtime_records(archive_id);


--
-- Name: archived_realtime_records archived_realtime_expert_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_realtime_records
    ADD CONSTRAINT archived_realtime_expert_fk FOREIGN KEY (expert_user_id) REFERENCES public.users(user_id);


--
-- Name: archived_realtime_records archived_realtime_initiator_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_realtime_records
    ADD CONSTRAINT archived_realtime_initiator_fk FOREIGN KEY (initiated_by_user_id) REFERENCES public.users(user_id);


--
-- Name: archived_realtime_records archived_realtime_mother_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_realtime_records
    ADD CONSTRAINT archived_realtime_mother_fk FOREIGN KEY (mother_user_id) REFERENCES public.users(user_id);


--
-- Name: archived_realtime_records archived_realtime_records_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_realtime_records
    ADD CONSTRAINT archived_realtime_records_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: archived_realtime_records archived_realtime_sender_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archived_realtime_records
    ADD CONSTRAINT archived_realtime_sender_fk FOREIGN KEY (sender_user_id) REFERENCES public.users(user_id);


--
-- Name: attachments attachments_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attachments
    ADD CONSTRAINT attachments_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: audit_events audit_events_actor_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_events
    ADD CONSTRAINT audit_events_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.users(user_id);


--
-- Name: audit_events audit_events_security_event_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_events
    ADD CONSTRAINT audit_events_security_event_fk FOREIGN KEY (security_event_id) REFERENCES public.security_events(id);


--
-- Name: audit_events audit_events_subject_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_events
    ADD CONSTRAINT audit_events_subject_user_id_fkey FOREIGN KEY (subject_user_id) REFERENCES public.users(user_id);


--
-- Name: auth_challenges auth_challenges_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_challenges
    ADD CONSTRAINT auth_challenges_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: auth_revocations auth_revocations_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_revocations
    ADD CONSTRAINT auth_revocations_session_id_fkey FOREIGN KEY (session_id) REFERENCES public.auth_sessions(session_id);


--
-- Name: auth_revocations auth_revocations_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_revocations
    ADD CONSTRAINT auth_revocations_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: auth_sessions auth_sessions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_sessions
    ADD CONSTRAINT auth_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: care_facilities care_facilities_partner_archive_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_facilities
    ADD CONSTRAINT care_facilities_partner_archive_fk FOREIGN KEY (partner_id) REFERENCES public.archived_partner_records(archive_id);


--
-- Name: care_group_members care_group_members_care_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_group_members
    ADD CONSTRAINT care_group_members_care_group_id_fkey FOREIGN KEY (care_group_id) REFERENCES public.care_groups(care_group_id);


--
-- Name: care_group_members care_group_members_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_group_members
    ADD CONSTRAINT care_group_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: care_groups care_groups_baby_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_groups
    ADD CONSTRAINT care_groups_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: care_groups care_groups_journey_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_groups
    ADD CONSTRAINT care_groups_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(journey_id);


--
-- Name: care_groups care_groups_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_groups
    ADD CONSTRAINT care_groups_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: care_item_templates care_item_templates_parent_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_item_templates
    ADD CONSTRAINT care_item_templates_parent_template_id_fkey FOREIGN KEY (parent_template_id) REFERENCES public.care_item_templates(template_id);


--
-- Name: care_logs care_logs_care_subject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_logs
    ADD CONSTRAINT care_logs_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: care_logs care_logs_recorded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_logs
    ADD CONSTRAINT care_logs_recorded_by_fkey FOREIGN KEY (recorded_by) REFERENCES public.users(user_id);


--
-- Name: care_subjects care_subjects_journey_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_subjects
    ADD CONSTRAINT care_subjects_journey_fk FOREIGN KEY (mother_journey_id) REFERENCES public.mother_journeys(journey_id);


--
-- Name: care_subjects care_subjects_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_subjects
    ADD CONSTRAINT care_subjects_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: care_subjects care_subjects_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.care_subjects
    ADD CONSTRAINT care_subjects_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.persons(person_id);


--
-- Name: community_content community_content_author_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.community_content
    ADD CONSTRAINT community_content_author_user_id_fkey FOREIGN KEY (author_user_id) REFERENCES public.users(user_id);


--
-- Name: community_content community_content_parent_content_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.community_content
    ADD CONSTRAINT community_content_parent_content_id_fkey FOREIGN KEY (parent_content_id) REFERENCES public.community_content(content_id);


--
-- Name: community_content community_content_topic_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.community_content
    ADD CONSTRAINT community_content_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES public.community_topics(id);


--
-- Name: community_interactions community_interactions_actor_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.community_interactions
    ADD CONSTRAINT community_interactions_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.users(user_id);


--
-- Name: community_interactions community_interactions_content_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.community_interactions
    ADD CONSTRAINT community_interactions_content_id_fkey FOREIGN KEY (content_id) REFERENCES public.community_content(content_id);


--
-- Name: community_interactions community_interactions_topic_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.community_interactions
    ADD CONSTRAINT community_interactions_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES public.community_topics(id);


--
-- Name: content_item_sources content_item_sources_content_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_item_sources
    ADD CONSTRAINT content_item_sources_content_item_id_fkey FOREIGN KEY (content_item_id) REFERENCES public.content_items(content_item_id);


--
-- Name: content_item_sources content_item_sources_knowledge_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_item_sources
    ADD CONSTRAINT content_item_sources_knowledge_source_id_fkey FOREIGN KEY (knowledge_source_id) REFERENCES public.knowledge_sources(knowledge_source_id);


--
-- Name: content_item_topics content_item_topics_content_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_item_topics
    ADD CONSTRAINT content_item_topics_content_item_id_fkey FOREIGN KEY (content_item_id) REFERENCES public.content_items(content_item_id);


--
-- Name: content_item_topics content_item_topics_topic_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.content_item_topics
    ADD CONSTRAINT content_item_topics_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES public.community_topics(id);


--
-- Name: data_permissions data_permissions_supersedes_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_permissions
    ADD CONSTRAINT data_permissions_supersedes_fk FOREIGN KEY (supersedes_permission_id) REFERENCES public.data_permissions(permission_id);


--
-- Name: development_milestones development_milestones_baby_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.development_milestones
    ADD CONSTRAINT development_milestones_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: development_milestones development_milestones_recorded_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.development_milestones
    ADD CONSTRAINT development_milestones_recorded_by_fkey FOREIGN KEY (recorded_by) REFERENCES public.users(user_id);


--
-- Name: development_milestones development_milestones_subject_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.development_milestones
    ADD CONSTRAINT development_milestones_subject_fk FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: device_connections device_connections_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_connections
    ADD CONSTRAINT device_connections_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: device_tokens device_tokens_user_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_tokens
    ADD CONSTRAINT device_tokens_user_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- Name: expense_entries expense_entries_care_subject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense_entries
    ADD CONSTRAINT expense_entries_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: expense_entries expense_entries_mother_journey_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense_entries
    ADD CONSTRAINT expense_entries_mother_journey_id_fkey FOREIGN KEY (mother_journey_id) REFERENCES public.mother_journeys(journey_id);


--
-- Name: expense_entries expense_entries_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expense_entries
    ADD CONSTRAINT expense_entries_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: expert_contribution_events expert_contribution_events_actor_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expert_contribution_events
    ADD CONSTRAINT expert_contribution_events_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.users(user_id);


--
-- Name: expert_contribution_events expert_contribution_events_professional_profile_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expert_contribution_events
    ADD CONSTRAINT expert_contribution_events_professional_profile_id_fkey FOREIGN KEY (professional_profile_id) REFERENCES public.professional_profiles(professional_profile_id);


--
-- Name: family_tasks family_tasks_assignee_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.family_tasks
    ADD CONSTRAINT family_tasks_assignee_user_id_fkey FOREIGN KEY (assignee_user_id) REFERENCES public.users(user_id);


--
-- Name: family_tasks family_tasks_care_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.family_tasks
    ADD CONSTRAINT family_tasks_care_group_id_fkey FOREIGN KEY (care_group_id) REFERENCES public.care_groups(care_group_id);


--
-- Name: family_tasks family_tasks_care_subject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.family_tasks
    ADD CONSTRAINT family_tasks_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: family_tasks family_tasks_creator_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.family_tasks
    ADD CONSTRAINT family_tasks_creator_user_id_fkey FOREIGN KEY (creator_user_id) REFERENCES public.users(user_id);


--
-- Name: community_topics fk_community_topics_parent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.community_topics
    ADD CONSTRAINT fk_community_topics_parent FOREIGN KEY (parent_id) REFERENCES public.community_topics(id) ON DELETE RESTRICT;


--
-- Name: expert_credentials fk_expert_credentials_file; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expert_credentials
    ADD CONSTRAINT fk_expert_credentials_file FOREIGN KEY (file_id) REFERENCES public.attachments(attachment_id);


--
-- Name: expert_credentials fk_expert_credentials_reviewed_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.expert_credentials
    ADD CONSTRAINT fk_expert_credentials_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES public.users(user_id) ON DELETE SET NULL;


--
-- Name: nearby_support_responses fk_nearby_support_responses_request; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nearby_support_responses
    ADD CONSTRAINT fk_nearby_support_responses_request FOREIGN KEY (request_id) REFERENCES public.nearby_support_requests(request_id) ON DELETE CASCADE;


--
-- Name: notification_records fk_notification_records_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_records
    ADD CONSTRAINT fk_notification_records_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- Name: red_flag_rules fk_red_flag_rules_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.red_flag_rules
    ADD CONSTRAINT fk_red_flag_rules_created_by FOREIGN KEY (created_by) REFERENCES public.users(user_id);


--
-- Name: red_flag_rules fk_red_flag_rules_updated_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.red_flag_rules
    ADD CONSTRAINT fk_red_flag_rules_updated_by FOREIGN KEY (updated_by) REFERENCES public.users(user_id);


--
-- Name: growth_measurements growth_measurements_baby_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.growth_measurements
    ADD CONSTRAINT growth_measurements_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: growth_measurements growth_measurements_subject_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.growth_measurements
    ADD CONSTRAINT growth_measurements_subject_fk FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: health_context_memories health_context_memories_care_subject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_context_memories
    ADD CONSTRAINT health_context_memories_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: health_context_memories health_context_memories_triage_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_context_memories
    ADD CONSTRAINT health_context_memories_triage_session_id_fkey FOREIGN KEY (triage_session_id) REFERENCES public.triage_sessions(triage_session_id);


--
-- Name: health_context_memories health_context_memories_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_context_memories
    ADD CONSTRAINT health_context_memories_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: health_observations health_observations_care_subject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_observations
    ADD CONSTRAINT health_observations_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: health_observations health_observations_device_connection_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_observations
    ADD CONSTRAINT health_observations_device_connection_id_fkey FOREIGN KEY (device_connection_id) REFERENCES public.device_connections(device_connection_id);


--
-- Name: health_record_attachments health_record_attachments_attachment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_record_attachments
    ADD CONSTRAINT health_record_attachments_attachment_id_fkey FOREIGN KEY (attachment_id) REFERENCES public.attachments(attachment_id);


--
-- Name: health_record_attachments health_record_attachments_health_record_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_record_attachments
    ADD CONSTRAINT health_record_attachments_health_record_id_fkey FOREIGN KEY (health_record_id) REFERENCES public.health_records(health_record_id);


--
-- Name: health_records health_records_baby_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_records
    ADD CONSTRAINT health_records_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: health_records health_records_journey_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_records
    ADD CONSTRAINT health_records_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(journey_id);


--
-- Name: health_records health_records_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.health_records
    ADD CONSTRAINT health_records_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: knowledge_source_reviews knowledge_source_reviews_actor_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_source_reviews
    ADD CONSTRAINT knowledge_source_reviews_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.users(user_id);


--
-- Name: knowledge_source_reviews knowledge_source_reviews_knowledge_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_source_reviews
    ADD CONSTRAINT knowledge_source_reviews_knowledge_source_id_fkey FOREIGN KEY (knowledge_source_id) REFERENCES public.knowledge_sources(knowledge_source_id);


--
-- Name: knowledge_sources knowledge_sources_added_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_sources
    ADD CONSTRAINT knowledge_sources_added_by_fkey FOREIGN KEY (added_by) REFERENCES public.users(user_id);


--
-- Name: knowledge_sources knowledge_sources_reviewed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.knowledge_sources
    ADD CONSTRAINT knowledge_sources_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.users(user_id);


--
-- Name: maternal_exercise_sessions maternal_exercise_sessions_mother_journey_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maternal_exercise_sessions
    ADD CONSTRAINT maternal_exercise_sessions_mother_journey_id_fkey FOREIGN KEY (mother_journey_id) REFERENCES public.mother_journeys(journey_id);


--
-- Name: maternal_exercise_sessions maternal_exercise_sessions_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maternal_exercise_sessions
    ADD CONSTRAINT maternal_exercise_sessions_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: maternal_exercise_sessions maternal_exercise_sessions_posture_config_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maternal_exercise_sessions
    ADD CONSTRAINT maternal_exercise_sessions_posture_config_fk FOREIGN KEY (posture_config_id) REFERENCES public.care_item_templates(template_id);


--
-- Name: maternal_exercise_sessions maternal_exercise_sessions_safety_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maternal_exercise_sessions
    ADD CONSTRAINT maternal_exercise_sessions_safety_fk FOREIGN KEY (safety_observation_id) REFERENCES public.maternal_observations(observation_id);


--
-- Name: maternal_exercise_sessions maternal_exercise_sessions_template_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maternal_exercise_sessions
    ADD CONSTRAINT maternal_exercise_sessions_template_fk FOREIGN KEY (exercise_template_id) REFERENCES public.care_item_templates(template_id);


--
-- Name: maternal_observations maternal_observations_exercise_template_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maternal_observations
    ADD CONSTRAINT maternal_observations_exercise_template_fk FOREIGN KEY (exercise_template_id) REFERENCES public.care_item_templates(template_id);


--
-- Name: maternal_observations maternal_observations_mother_journey_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maternal_observations
    ADD CONSTRAINT maternal_observations_mother_journey_id_fkey FOREIGN KEY (mother_journey_id) REFERENCES public.mother_journeys(journey_id);


--
-- Name: maternal_observations maternal_observations_owner_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maternal_observations
    ADD CONSTRAINT maternal_observations_owner_fk FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: maternal_observations maternal_observations_posture_config_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maternal_observations
    ADD CONSTRAINT maternal_observations_posture_config_fk FOREIGN KEY (posture_config_id) REFERENCES public.care_item_templates(template_id);


--
-- Name: maternal_observations maternal_observations_session_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.maternal_observations
    ADD CONSTRAINT maternal_observations_session_fk FOREIGN KEY (exercise_session_id) REFERENCES public.maternal_exercise_sessions(exercise_session_id);


--
-- Name: moderation_cases moderation_cases_assigned_moderator_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_cases
    ADD CONSTRAINT moderation_cases_assigned_moderator_id_fkey FOREIGN KEY (assigned_moderator_id) REFERENCES public.users(user_id);


--
-- Name: moderation_cases moderation_cases_reporter_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_cases
    ADD CONSTRAINT moderation_cases_reporter_user_id_fkey FOREIGN KEY (reporter_user_id) REFERENCES public.users(user_id);


--
-- Name: moderation_events moderation_events_moderation_case_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_events
    ADD CONSTRAINT moderation_events_moderation_case_id_fkey FOREIGN KEY (moderation_case_id) REFERENCES public.moderation_cases(moderation_case_id);


--
-- Name: moderation_events moderation_events_moderator_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moderation_events
    ADD CONSTRAINT moderation_events_moderator_user_id_fkey FOREIGN KEY (moderator_user_id) REFERENCES public.users(user_id);


--
-- Name: mother_journey_events mother_journey_events_actor_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mother_journey_events
    ADD CONSTRAINT mother_journey_events_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.users(user_id);


--
-- Name: mother_journey_events mother_journey_events_care_subject_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mother_journey_events
    ADD CONSTRAINT mother_journey_events_care_subject_fk FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: mother_journey_events mother_journey_events_mother_journey_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mother_journey_events
    ADD CONSTRAINT mother_journey_events_mother_journey_id_fkey FOREIGN KEY (mother_journey_id) REFERENCES public.mother_journeys(journey_id);


--
-- Name: mother_journey_events mother_journey_events_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mother_journey_events
    ADD CONSTRAINT mother_journey_events_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: mother_journey_events mother_journey_events_supersedes_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mother_journey_events
    ADD CONSTRAINT mother_journey_events_supersedes_fk FOREIGN KEY (supersedes_evidence_id) REFERENCES public.mother_journey_events(event_id);


--
-- Name: mother_journeys mother_journeys_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mother_journeys
    ADD CONSTRAINT mother_journeys_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: mother_journeys mother_journeys_subject_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mother_journeys
    ADD CONSTRAINT mother_journeys_subject_fk FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: preparation_checklist_items preparation_checklist_items_mother_journey_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preparation_checklist_items
    ADD CONSTRAINT preparation_checklist_items_mother_journey_id_fkey FOREIGN KEY (mother_journey_id) REFERENCES public.mother_journeys(journey_id);


--
-- Name: preparation_checklist_items preparation_checklist_items_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preparation_checklist_items
    ADD CONSTRAINT preparation_checklist_items_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: preparation_checklist_items preparation_checklist_items_template_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preparation_checklist_items
    ADD CONSTRAINT preparation_checklist_items_template_entry_id_fkey FOREIGN KEY (template_entry_id) REFERENCES public.care_item_templates(template_id);


--
-- Name: professional_profiles professional_profiles_facility_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.professional_profiles
    ADD CONSTRAINT professional_profiles_facility_fk FOREIGN KEY (facility_id) REFERENCES public.care_facilities(facility_id);


--
-- Name: professional_profiles professional_profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.professional_profiles
    ADD CONSTRAINT professional_profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: professional_profiles professional_profiles_verified_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.professional_profiles
    ADD CONSTRAINT professional_profiles_verified_by_fkey FOREIGN KEY (verified_by) REFERENCES public.users(user_id);


--
-- Name: professional_specialties professional_specialties_professional_profile_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.professional_specialties
    ADD CONSTRAINT professional_specialties_professional_profile_id_fkey FOREIGN KEY (professional_profile_id) REFERENCES public.professional_profiles(professional_profile_id);


--
-- Name: professional_specialties professional_specialties_specialty_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.professional_specialties
    ADD CONSTRAINT professional_specialties_specialty_id_fkey FOREIGN KEY (specialty_id) REFERENCES public.specialties(specialty_id);


--
-- Name: safety_configs safety_configs_updated_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_configs
    ADD CONSTRAINT safety_configs_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users(user_id);


--
-- Name: safety_configs safety_configs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_configs
    ADD CONSTRAINT safety_configs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: safety_event_actions safety_event_actions_care_facility_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_event_actions
    ADD CONSTRAINT safety_event_actions_care_facility_id_fkey FOREIGN KEY (care_facility_id) REFERENCES public.care_facilities(facility_id);


--
-- Name: safety_event_actions safety_event_actions_created_by_user_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_event_actions
    ADD CONSTRAINT safety_event_actions_created_by_user_fk FOREIGN KEY (created_by_user_id) REFERENCES public.users(user_id);


--
-- Name: safety_event_actions safety_event_actions_device_token_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_event_actions
    ADD CONSTRAINT safety_event_actions_device_token_fk FOREIGN KEY (device_token_id) REFERENCES public.device_tokens(id);


--
-- Name: safety_event_actions safety_event_actions_notification_record_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_event_actions
    ADD CONSTRAINT safety_event_actions_notification_record_id_fkey FOREIGN KEY (notification_record_id) REFERENCES public.notification_records(id);


--
-- Name: safety_event_actions safety_event_actions_owner_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_event_actions
    ADD CONSTRAINT safety_event_actions_owner_fk FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: safety_event_actions safety_event_actions_recipient_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_event_actions
    ADD CONSTRAINT safety_event_actions_recipient_user_id_fkey FOREIGN KEY (recipient_user_id) REFERENCES public.users(user_id);


--
-- Name: safety_event_actions safety_event_actions_safety_event_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_event_actions
    ADD CONSTRAINT safety_event_actions_safety_event_id_fkey FOREIGN KEY (safety_event_id) REFERENCES public.safety_events(safety_event_id);


--
-- Name: safety_events safety_events_care_subject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_events
    ADD CONSTRAINT safety_events_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: safety_events safety_events_created_by_user_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_events
    ADD CONSTRAINT safety_events_created_by_user_fk FOREIGN KEY (created_by_user_id) REFERENCES public.users(user_id);


--
-- Name: safety_events safety_events_emergency_session_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_events
    ADD CONSTRAINT safety_events_emergency_session_fk FOREIGN KEY (emergency_session_id) REFERENCES public.safety_events(safety_event_id);


--
-- Name: safety_events safety_events_monitoring_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_events
    ADD CONSTRAINT safety_events_monitoring_session_id_fkey FOREIGN KEY (monitoring_session_id) REFERENCES public.safety_monitoring_sessions(monitoring_session_id);


--
-- Name: safety_events safety_events_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_events
    ADD CONSTRAINT safety_events_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: safety_monitoring_sessions safety_monitoring_sessions_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_monitoring_sessions
    ADD CONSTRAINT safety_monitoring_sessions_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(user_id);


--
-- Name: safety_monitoring_sessions safety_monitoring_sessions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.safety_monitoring_sessions
    ADD CONSTRAINT safety_monitoring_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: scheduled_care_items scheduled_care_items_care_subject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheduled_care_items
    ADD CONSTRAINT scheduled_care_items_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: scheduled_care_items scheduled_care_items_owner_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheduled_care_items
    ADD CONSTRAINT scheduled_care_items_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


--
-- Name: scheduled_care_items scheduled_care_items_vaccination_record_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheduled_care_items
    ADD CONSTRAINT scheduled_care_items_vaccination_record_id_fkey FOREIGN KEY (vaccination_record_id) REFERENCES public.vaccination_records(vaccination_record_id);


--
-- Name: security_events security_events_reviewed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.security_events
    ADD CONSTRAINT security_events_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.users(user_id);


--
-- Name: system_configurations system_configurations_updated_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.system_configurations
    ADD CONSTRAINT system_configurations_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users(user_id);


--
-- Name: triage_session_evidence triage_session_evidence_source_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.triage_session_evidence
    ADD CONSTRAINT triage_session_evidence_source_fk FOREIGN KEY (knowledge_source_id) REFERENCES public.knowledge_sources(knowledge_source_id);


--
-- Name: triage_session_evidence triage_session_evidence_triage_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.triage_session_evidence
    ADD CONSTRAINT triage_session_evidence_triage_session_id_fkey FOREIGN KEY (triage_session_id) REFERENCES public.triage_sessions(triage_session_id);


--
-- Name: triage_sessions triage_sessions_care_subject_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.triage_sessions
    ADD CONSTRAINT triage_sessions_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: triage_sessions triage_sessions_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.triage_sessions
    ADD CONSTRAINT triage_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: user_identities user_identities_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_identities
    ADD CONSTRAINT user_identities_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- Name: users users_person_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_person_fk FOREIGN KEY (person_id) REFERENCES public.persons(person_id);


--
-- Name: vaccination_records vaccination_records_baby_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vaccination_records
    ADD CONSTRAINT vaccination_records_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.care_subjects(care_subject_id);


--
-- Name: vaccination_records vaccination_records_proof_record_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vaccination_records
    ADD CONSTRAINT vaccination_records_proof_record_id_fkey FOREIGN KEY (proof_record_id) REFERENCES public.health_records(health_record_id);


--
-- Name: vaccination_records vaccination_records_schedule_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vaccination_records
    ADD CONSTRAINT vaccination_records_schedule_fk FOREIGN KEY (vaccination_schedule_id) REFERENCES public.vaccination_schedules(vaccination_schedule_id);


--
-- Name: vaccination_records vaccination_records_subject_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vaccination_records
    ADD CONSTRAINT vaccination_records_subject_fk FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


--
-- PostgreSQL database dump complete
--


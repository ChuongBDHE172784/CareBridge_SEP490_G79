-- DROP SCHEMA public;

CREATE SCHEMA public AUTHORIZATION postgres;

-- DROP SEQUENCE public.data_permissions_legacy_consent_id_seq;

CREATE SEQUENCE public.data_permissions_legacy_consent_id_seq
	INCREMENT BY 1
	MINVALUE 1
	MAXVALUE 9223372036854775807
	START 1
	CACHE 1
	NO CYCLE;

-- Permissions

ALTER SEQUENCE public.data_permissions_legacy_consent_id_seq OWNER TO postgres;
GRANT ALL ON SEQUENCE public.data_permissions_legacy_consent_id_seq TO postgres;
-- public.account_deletion_requests definition

-- Drop table

-- DROP TABLE public.account_deletion_requests;

CREATE TABLE public.account_deletion_requests ( id uuid DEFAULT gen_random_uuid() NOT NULL, user_id uuid NOT NULL, status varchar(30) DEFAULT 'PENDING'::character varying NOT NULL, reason text NULL, requested_at timestamptz DEFAULT now() NOT NULL, scheduled_for timestamptz NULL, processed_at timestamptz NULL, processed_by uuid NULL, notes text NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT account_deletion_requests_pkey PRIMARY KEY (id));
CREATE INDEX idx_account_deletion_requests_scheduled_for ON public.account_deletion_requests USING btree (scheduled_for) WHERE ((status)::text = 'PENDING'::text);
CREATE INDEX idx_account_deletion_requests_status ON public.account_deletion_requests USING btree (status);
CREATE INDEX idx_account_deletion_requests_user_id ON public.account_deletion_requests USING btree (user_id);

-- Permissions

ALTER TABLE public.account_deletion_requests OWNER TO postgres;
GRANT ALL ON TABLE public.account_deletion_requests TO postgres;


-- public.content_items definition

-- Drop table

-- DROP TABLE public.content_items;

CREATE TABLE public.content_items ( content_item_id uuid NOT NULL, author_user_id uuid NULL, body text NULL, content_type varchar(30) NULL, created_at timestamptz(6) NOT NULL, published_at timestamptz(6) NULL, source_label varchar(255) NULL, status varchar(20) NOT NULL, title varchar(250) NULL, topic_id uuid NULL, updated_at timestamptz(6) NULL, version_no int4 NULL, stage varchar(30) NULL, CONSTRAINT content_items_pkey PRIMARY KEY (content_item_id));
CREATE INDEX idx_content_items_published_at ON public.content_items USING btree (published_at DESC NULLS LAST) WHERE ((status)::text = 'APPROVED'::text);
CREATE INDEX idx_content_items_stage ON public.content_items USING btree (stage);
CREATE INDEX idx_content_items_stage_status ON public.content_items USING btree (stage, status);
CREATE INDEX idx_content_items_stage_type_approved ON public.content_items USING btree (stage, content_type, status) WHERE ((status)::text = 'APPROVED'::text);
CREATE INDEX idx_content_items_status ON public.content_items USING btree (status);
CREATE INDEX idx_content_items_title_search ON public.content_items USING btree (lower((title)::text));
CREATE INDEX idx_content_items_type ON public.content_items USING btree (content_type);
CREATE INDEX idx_content_items_type_status ON public.content_items USING btree (content_type, status);

-- Permissions

ALTER TABLE public.content_items OWNER TO postgres;
GRANT ALL ON TABLE public.content_items TO postgres;


-- public.emergency_contacts definition

-- Drop table

-- DROP TABLE public.emergency_contacts;

CREATE TABLE public.emergency_contacts ( id uuid NOT NULL, user_id uuid NOT NULL, "name" varchar(120) NOT NULL, phone varchar(32) NOT NULL, relationship varchar(80) NULL, primary_contact bool DEFAULT true NOT NULL, updated_at timestamptz NOT NULL, updated_by uuid NOT NULL, CONSTRAINT emergency_contacts_pkey PRIMARY KEY (id), CONSTRAINT emergency_contacts_user_id_key UNIQUE (user_id));
CREATE INDEX idx_emergency_contacts_user_id ON public.emergency_contacts USING btree (user_id);

-- Permissions

ALTER TABLE public.emergency_contacts OWNER TO postgres;
GRANT ALL ON TABLE public.emergency_contacts TO postgres;


-- public.expert_availability definition

-- Drop table

-- DROP TABLE public.expert_availability;

CREATE TABLE public.expert_availability ( availability_id uuid DEFAULT gen_random_uuid() NOT NULL, start_at timestamptz NOT NULL, end_at timestamptz NOT NULL, channel_type varchar(30) NOT NULL, status varchar(20) DEFAULT 'AVAILABLE'::character varying NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, user_id uuid NOT NULL, CONSTRAINT expert_availability_pkey PRIMARY KEY (availability_id));
CREATE INDEX expert_availability_user_window_ix ON public.expert_availability USING btree (user_id, start_at, end_at);
CREATE INDEX idx_expert_availability_start_at ON public.expert_availability USING btree (start_at);
CREATE INDEX idx_expert_availability_status ON public.expert_availability USING btree (status);

-- Permissions

ALTER TABLE public.expert_availability OWNER TO postgres;
GRANT ALL ON TABLE public.expert_availability TO postgres;


-- public.expert_location_shares definition

-- Drop table

-- DROP TABLE public.expert_location_shares;

CREATE TABLE public.expert_location_shares ( location_share_id uuid DEFAULT gen_random_uuid() NOT NULL, latitude numeric NOT NULL, longitude numeric NOT NULL, accuracy_meters numeric NULL, availability_status varchar(20) NULL, shared_at timestamptz DEFAULT now() NOT NULL, expires_at timestamptz NULL, consent_reference uuid NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, user_id uuid NOT NULL, CONSTRAINT expert_location_shares_pkey PRIMARY KEY (location_share_id));

-- Permissions

ALTER TABLE public.expert_location_shares OWNER TO postgres;
GRANT ALL ON TABLE public.expert_location_shares TO postgres;


-- public.flyway_schema_history definition

-- Drop table

-- DROP TABLE public.flyway_schema_history;

CREATE TABLE public.flyway_schema_history ( installed_rank int4 NOT NULL, "version" varchar(50) NULL, description varchar(200) NOT NULL, "type" varchar(20) NOT NULL, script varchar(1000) NOT NULL, checksum int4 NULL, installed_by varchar(100) NOT NULL, installed_on timestamp DEFAULT now() NOT NULL, execution_time int4 NOT NULL, success bool NOT NULL, CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank));
CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);

-- Permissions

ALTER TABLE public.flyway_schema_history OWNER TO postgres;
GRANT ALL ON TABLE public.flyway_schema_history TO postgres;


-- public.specialties definition

-- Drop table

-- DROP TABLE public.specialties;

CREATE TABLE public.specialties ( specialty_id uuid NOT NULL, code varchar(80) NOT NULL, "name" varchar(150) NOT NULL, description text NULL, is_active bool DEFAULT true NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT specialties_canonical_code_key UNIQUE (code), CONSTRAINT specialties_canonical_pkey PRIMARY KEY (specialty_id));

-- Permissions

ALTER TABLE public.specialties OWNER TO postgres;
GRANT ALL ON TABLE public.specialties TO postgres;


-- public.vaccination_schedules definition

-- Drop table

-- DROP TABLE public.vaccination_schedules;

CREATE TABLE public.vaccination_schedules ( vaccination_schedule_id uuid DEFAULT gen_random_uuid() NOT NULL, vaccine_name varchar(200) NOT NULL, dose_number int2 NOT NULL, offset_days int4 NOT NULL, description text NULL, schedule_version varchar(30) DEFAULT '1'::character varying NOT NULL, active_from date NULL, active_to date NULL, created_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT vaccination_schedules_key_uk UNIQUE (vaccine_name, dose_number, schedule_version), CONSTRAINT vaccination_schedules_pkey PRIMARY KEY (vaccination_schedule_id));

-- Permissions

ALTER TABLE public.vaccination_schedules OWNER TO postgres;
GRANT ALL ON TABLE public.vaccination_schedules TO postgres;


-- public.administrative_areas definition

-- Drop table

-- DROP TABLE public.administrative_areas;

CREATE TABLE public.administrative_areas ( administrative_area_id uuid DEFAULT gen_random_uuid() NOT NULL, parent_area_id uuid NULL, area_type varchar(30) NOT NULL, code varchar(80) NOT NULL, "name" varchar(255) NOT NULL, legacy_code varchar(80) NULL, created_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT administrative_areas_code_key UNIQUE (code), CONSTRAINT administrative_areas_pkey PRIMARY KEY (administrative_area_id), CONSTRAINT administrative_areas_parent_area_id_fkey FOREIGN KEY (parent_area_id) REFERENCES public.administrative_areas(administrative_area_id));

-- Permissions

ALTER TABLE public.administrative_areas OWNER TO postgres;
GRANT ALL ON TABLE public.administrative_areas TO postgres;


-- public.care_item_templates definition

-- Drop table

-- DROP TABLE public.care_item_templates;

CREATE TABLE public.care_item_templates ( template_id uuid DEFAULT gen_random_uuid() NOT NULL, parent_template_id uuid NULL, entry_type varchar(30) NOT NULL, title varchar(255) NOT NULL, description text NULL, display_order int4 DEFAULT 0 NOT NULL, stage varchar(30) NULL, is_active bool DEFAULT true NOT NULL, "version" int4 DEFAULT 1 NOT NULL, effective_from timestamptz NULL, effective_to timestamptz NULL, configuration_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, configuration_hash varchar(128) NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, created_by uuid NULL, difficulty_level varchar(30) NULL, duration_minutes int2 NULL, instruction_content text NULL, media_url text NULL, safety_warning text NULL, supports_posture_analysis bool NULL, template_status varchar(20) DEFAULT 'ACTIVE'::character varying NOT NULL, configured_by uuid NULL, analysis_mode varchar(30) NULL, rule_or_model_version varchar(80) NULL, confidence_threshold numeric NULL, feedback_level varchar(30) NULL, content_status varchar(20) DEFAULT 'DRAFT'::character varying NOT NULL, is_required bool NULL, CONSTRAINT care_item_templates_pkey PRIMARY KEY (template_id), CONSTRAINT care_item_templates_type_ck CHECK (((entry_type)::text = ANY ((ARRAY['TEMPLATE_ROOT'::character varying, 'CHECKLIST_ENTRY'::character varying, 'EXERCISE_TEMPLATE'::character varying, 'POSTURE_CONFIG'::character varying])::text[]))), CONSTRAINT chk_care_item_templates_posture_confidence_threshold CHECK ((((entry_type)::text <> 'POSTURE_CONFIG'::text) OR (confidence_threshold IS NULL) OR ((confidence_threshold >= 0.0) AND (confidence_threshold <= 1.0)))), CONSTRAINT care_item_templates_parent_template_id_fkey FOREIGN KEY (parent_template_id) REFERENCES public.care_item_templates(template_id));
CREATE INDEX care_item_templates_content_status_ix ON public.care_item_templates USING btree (entry_type, content_status, stage, display_order);
CREATE INDEX care_item_templates_exercise_filter_ix ON public.care_item_templates USING btree (template_status, stage, difficulty_level, created_at DESC) WHERE ((entry_type)::text = 'EXERCISE_TEMPLATE'::text);
CREATE INDEX care_item_templates_parent_order_ix ON public.care_item_templates USING btree (parent_template_id, display_order);
CREATE INDEX care_item_templates_posture_version_ix ON public.care_item_templates USING btree (parent_template_id, template_status, effective_from DESC) WHERE ((entry_type)::text = 'POSTURE_CONFIG'::text);

-- Permissions

ALTER TABLE public.care_item_templates OWNER TO postgres;
GRANT ALL ON TABLE public.care_item_templates TO postgres;


-- public.community_topics definition

-- Drop table

-- DROP TABLE public.community_topics;

CREATE TABLE public.community_topics ( id uuid NOT NULL, created_at timestamptz NOT NULL, description text NULL, "name" varchar(100) NOT NULL, updated_at timestamptz DEFAULT now() NULL, is_hidden bool DEFAULT false NOT NULL, icon varchar(255) NULL, sort_order int4 DEFAULT 0 NOT NULL, created_by uuid NULL, "type" varchar(20) DEFAULT 'TOPIC'::character varying NOT NULL, slug varchar(140) NOT NULL, parent_id uuid NULL, CONSTRAINT community_topics_parent_rule_check_v2 CHECK (((((type)::text = 'CATEGORY'::text) AND (parent_id IS NULL)) OR (((type)::text = 'TOPIC'::text) AND (parent_id IS NOT NULL)) OR (((type)::text = 'TAG'::text) AND (parent_id IS NULL)))), CONSTRAINT community_topics_pkey PRIMARY KEY (id), CONSTRAINT community_topics_slug_unique UNIQUE (slug), CONSTRAINT community_topics_type_check CHECK (((type)::text = ANY ((ARRAY['TOPIC'::character varying, 'CATEGORY'::character varying, 'TAG'::character varying])::text[]))), CONSTRAINT fk_community_topics_parent FOREIGN KEY (parent_id) REFERENCES public.community_topics(id) ON DELETE RESTRICT);
CREATE INDEX idx_community_topics_hidden ON public.community_topics USING btree (is_hidden);
CREATE UNIQUE INDEX idx_community_topics_name_lower ON public.community_topics USING btree (lower((name)::text));
CREATE INDEX idx_community_topics_parent_id ON public.community_topics USING btree (parent_id);
CREATE INDEX idx_community_topics_sort_order ON public.community_topics USING btree (sort_order);
CREATE INDEX idx_community_topics_type ON public.community_topics USING btree (type);

-- Table Triggers

create trigger trg_community_topic_parent_category before
insert
    or
update
    of type,
    parent_id on
    public.community_topics for each row execute function enforce_community_topic_parent_category();

-- Permissions

ALTER TABLE public.community_topics OWNER TO postgres;
GRANT ALL ON TABLE public.community_topics TO postgres;


-- public.content_item_topics definition

-- Drop table

-- DROP TABLE public.content_item_topics;

CREATE TABLE public.content_item_topics ( content_item_id uuid NOT NULL, topic_id uuid NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT content_item_topics_pkey PRIMARY KEY (content_item_id, topic_id), CONSTRAINT content_item_topics_content_item_id_fkey FOREIGN KEY (content_item_id) REFERENCES public.content_items(content_item_id), CONSTRAINT content_item_topics_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES public.community_topics(id));

-- Permissions

ALTER TABLE public.content_item_topics OWNER TO postgres;
GRANT ALL ON TABLE public.content_item_topics TO postgres;


-- public.data_permissions definition

-- Drop table

-- DROP TABLE public.data_permissions;

CREATE TABLE public.data_permissions ( permission_id uuid DEFAULT gen_random_uuid() NOT NULL, created_at timestamptz(6) NOT NULL, expires_at timestamptz(6) NULL, granted_at timestamptz(6) NULL, grantee_user_id uuid NULL, owner_user_id uuid NULL, purpose varchar(255) NULL, revoked_at timestamptz(6) NULL, scope_reference_id uuid NULL, scope_type varchar(50) NULL, status varchar(20) DEFAULT 'ACTIVE'::character varying NOT NULL, updated_at timestamptz(6) NULL, permission_series_id uuid NULL, version_number int4 NULL, supersedes_permission_id uuid NULL, revoked_by uuid NULL, policy_version varchar(80) NULL, consent_evidence_key varchar(255) NULL, legacy_consent_id int8 GENERATED BY DEFAULT AS IDENTITY( INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1 NO CYCLE) NOT NULL, permission_kind varchar(30) DEFAULT 'DATA_PERMISSION'::character varying NOT NULL, recipient varchar(120) NULL, scope_text text NULL, evidence_key uuid NULL, locale varchar(20) NULL, CONSTRAINT data_permissions_pkey PRIMARY KEY (permission_id), CONSTRAINT data_permissions_supersedes_fk FOREIGN KEY (supersedes_permission_id) REFERENCES public.data_permissions(permission_id));
CREATE INDEX data_permissions_consent_owner_ix ON public.data_permissions USING btree (owner_user_id, granted_at DESC) WHERE ((permission_kind)::text = 'CONSENT_GRANT'::text);
CREATE UNIQUE INDEX data_permissions_legacy_consent_id_uk ON public.data_permissions USING btree (legacy_consent_id) WHERE (legacy_consent_id IS NOT NULL);

-- Permissions

ALTER TABLE public.data_permissions OWNER TO postgres;
GRANT ALL ON TABLE public.data_permissions TO postgres;


-- public.archived_records definition

-- Drop table

-- DROP TABLE public.archived_records;

CREATE TABLE public.archived_records ( archive_id uuid DEFAULT gen_random_uuid() NOT NULL, legacy_table varchar(150) NOT NULL, legacy_id varchar(150) NOT NULL, owner_user_id uuid NULL, payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, original_created_at timestamptz NULL, archived_at timestamptz DEFAULT now() NOT NULL, retention_until timestamptz NULL, archive_reason varchar(255) DEFAULT 'RUNTIME_COMPATIBILITY'::character varying NOT NULL, source_schema_version varchar(80) DEFAULT 'PHASE2_WAVE9'::character varying NULL, checksum varchar(128) DEFAULT md5(gen_random_uuid()::text) NOT NULL, configured_by uuid NULL, channel_type varchar(30) NULL, duration_minutes int2 NULL, specialty_scope varchar(100) NULL, minimum_price numeric NULL, maximum_price numeric NULL, commission_rate numeric NULL, currency varchar(10) NULL, effective_from timestamptz NULL, effective_to timestamptz NULL, status varchar(30) NULL, updated_at timestamptz NULL, expert_profile_id uuid NULL, price_band_id uuid NULL, price_amount numeric NULL, cancellation_policy text NULL, version_no int4 NULL, requester_user_id uuid NULL, availability_id uuid NULL, expert_price_id uuid NULL, shared_summary_id uuid NULL, topic varchar(500) NULL, scheduled_start timestamptz NULL, scheduled_end timestamptz NULL, price_snapshot_amount numeric NULL, commission_rate_snapshot numeric NULL, cancellation_policy_snapshot text NULL, price_locked_at timestamptz NULL, booking_id uuid NULL, communication_room_id varchar(255) NULL, started_at timestamptz NULL, ended_at timestamptz NULL, session_status varchar(30) NULL, expert_summary text NULL, technical_log_json jsonb NULL, conversation_id uuid NULL, mother_user_id uuid NULL, expert_user_id uuid NULL, last_activity_at timestamptz NULL, mother_last_read_at timestamptz NULL, mother_last_read_message_id uuid NULL, expert_last_read_at timestamptz NULL, expert_last_read_message_id uuid NULL, sender_user_id uuid NULL, client_message_id uuid NULL, message_type varchar(30) NULL, message_body text NULL, initiated_by_user_id uuid NULL, call_type varchar(10) NULL, call_status varchar(20) NULL, zego_room_id varchar(255) NULL, initiated_at timestamptz NULL, answered_at timestamptz NULL, duration_seconds int4 NULL, "name" varchar(200) NULL, organization_type varchar(20) NULL, address varchar(500) NULL, city varchar(100) NULL, phone varchar(20) NULL, email varchar(255) NULL, website varchar(500) NULL, logo_url varchar(1000) NULL, description varchar(2000) NULL, organization_status varchar(30) NULL, representative_user_id uuid NULL, CONSTRAINT archived_partner_shape_ck CHECK ((((legacy_table)::text <> 'partner_organizations'::text) OR ((name IS NOT NULL) AND ((organization_type)::text = ANY ((ARRAY['CLINIC'::character varying, 'HOSPITAL'::character varying, 'NGO'::character varying, 'COMPANY'::character varying])::text[])) AND (address IS NOT NULL) AND (city IS NOT NULL) AND (phone IS NOT NULL) AND (email IS NOT NULL) AND ((organization_status)::text = ANY ((ARRAY['PENDING_APPROVAL'::character varying, 'APPROVED'::character varying, 'SUSPENDED'::character varying, 'REJECTED'::character varying])::text[])) AND (representative_user_id IS NOT NULL) AND (original_created_at IS NOT NULL)))), CONSTRAINT archived_realtime_call_shape_ck CHECK ((((legacy_table)::text <> 'conversation_calls'::text) OR ((conversation_id IS NOT NULL) AND (initiated_by_user_id IS NOT NULL) AND ((call_type)::text = ANY ((ARRAY['VOICE'::character varying, 'VIDEO'::character varying])::text[])) AND ((call_status)::text = ANY ((ARRAY['INITIATED'::character varying, 'RINGING'::character varying, 'ANSWERED'::character varying, 'DECLINED'::character varying, 'MISSED'::character varying, 'CANCELLED'::character varying, 'ENDED'::character varying, 'FAILED'::character varying])::text[])) AND (zego_room_id IS NOT NULL) AND (initiated_at IS NOT NULL) AND (original_created_at IS NOT NULL) AND ((duration_seconds IS NULL) OR (duration_seconds >= 0)) AND ((answered_at IS NULL) OR (answered_at >= initiated_at)) AND ((ended_at IS NULL) OR (ended_at >= initiated_at)) AND (((call_status)::text <> 'ENDED'::text) OR (answered_at IS NOT NULL))))), CONSTRAINT archived_realtime_conversation_shape_ck CHECK ((((legacy_table)::text <> 'direct_conversations'::text) OR ((mother_user_id IS NOT NULL) AND (expert_user_id IS NOT NULL) AND ((status)::text = 'ACTIVE'::text) AND (original_created_at IS NOT NULL) AND ((last_activity_at IS NULL) OR (last_activity_at >= original_created_at))))), CONSTRAINT archived_realtime_message_shape_ck CHECK ((((legacy_table)::text <> 'direct_messages'::text) OR ((conversation_id IS NOT NULL) AND (sender_user_id IS NOT NULL) AND (client_message_id IS NOT NULL) AND ((message_type)::text = 'TEXT'::text) AND ((length(btrim(message_body)) >= 1) AND (length(btrim(message_body)) <= 2000)) AND (original_created_at IS NOT NULL)))), CONSTRAINT archived_records_pkey PRIMARY KEY (archive_id), CONSTRAINT archived_records_source_uk UNIQUE (legacy_table, legacy_id));
CREATE INDEX archived_consultation_booking_requester_ix ON public.archived_records USING btree (requester_user_id, status, original_created_at DESC) WHERE ((legacy_table)::text = 'consultation_bookings'::text);
CREATE UNIQUE INDEX archived_partner_email_uk ON public.archived_records USING btree (email) WHERE ((legacy_table)::text = 'partner_organizations'::text);
CREATE UNIQUE INDEX archived_partner_representative_uk ON public.archived_records USING btree (representative_user_id) WHERE ((legacy_table)::text = 'partner_organizations'::text);
CREATE INDEX archived_realtime_call_timeline_ix ON public.archived_records USING btree (conversation_id, initiated_at DESC) WHERE ((legacy_table)::text = 'conversation_calls'::text);
CREATE UNIQUE INDEX archived_realtime_conversation_pair_uk ON public.archived_records USING btree (mother_user_id, expert_user_id) WHERE ((legacy_table)::text = 'direct_conversations'::text);
CREATE UNIQUE INDEX archived_realtime_message_client_uk ON public.archived_records USING btree (conversation_id, sender_user_id, client_message_id) WHERE ((legacy_table)::text = 'direct_messages'::text);
CREATE INDEX archived_realtime_message_timeline_ix ON public.archived_records USING btree (conversation_id, original_created_at DESC) WHERE ((legacy_table)::text = 'direct_messages'::text);

-- Permissions

ALTER TABLE public.archived_records OWNER TO postgres;
GRANT ALL ON TABLE public.archived_records TO postgres;


-- public.attachments definition

-- Drop table

-- DROP TABLE public.attachments;

CREATE TABLE public.attachments ( attachment_id uuid DEFAULT gen_random_uuid() NOT NULL, owner_user_id uuid NOT NULL, health_record_id uuid NULL, storage_key varchar(500) NOT NULL, original_name varchar(255) NOT NULL, mime_type varchar(100) NOT NULL, file_size_bytes int8 NOT NULL, status varchar(20) DEFAULT 'ACTIVE'::character varying NOT NULL, checksum varchar(128) NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT attachments_file_size_bytes_check CHECK ((file_size_bytes >= 0)), CONSTRAINT attachments_pkey PRIMARY KEY (attachment_id), CONSTRAINT attachments_storage_key_key UNIQUE (storage_key));

-- Permissions

ALTER TABLE public.attachments OWNER TO postgres;
GRANT ALL ON TABLE public.attachments TO postgres;


-- public.audit_events definition

-- Drop table

-- DROP TABLE public.audit_events;

CREATE TABLE public.audit_events ( audit_event_id uuid DEFAULT gen_random_uuid() NOT NULL, actor_user_id uuid NULL, event_category varchar(80) NOT NULL, subject_user_id uuid NULL, subject_reference_id uuid NULL, resource_type varchar(100) NULL, resource_id uuid NULL, purpose varchar(255) NULL, decision varchar(50) NULL, ip_hash varchar(128) NULL, ip_address varchar(80) NULL, user_agent varchar(500) NULL, before_payload_jsonb jsonb NULL, after_payload_jsonb jsonb NULL, payload jsonb NULL, correlation_id uuid NULL, severity varchar(20) DEFAULT 'MEDIUM'::character varying NOT NULL, status varchar(20) DEFAULT 'OPEN'::character varying NOT NULL, reviewed_by uuid NULL, reviewed_at timestamptz NULL, checksum varchar(128) NULL, note_text text NULL, occurred_at timestamptz DEFAULT now() NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, security_event_id uuid NULL, event_origin varchar(40) DEFAULT 'AUDIT_LOG'::character varying NOT NULL, CONSTRAINT audit_events_pkey PRIMARY KEY (audit_event_id), CONSTRAINT audit_events_security_note_text_ck CHECK ((((event_category)::text <> 'SECURITY_INVESTIGATION_NOTE'::text) OR ((security_event_id IS NOT NULL) AND (length(TRIM(BOTH FROM note_text)) > 0)))));
CREATE INDEX audit_events_category_time_ix ON public.audit_events USING btree (event_category, occurred_at);
CREATE INDEX audit_events_origin_time_ix ON public.audit_events USING btree (event_origin, occurred_at DESC);
CREATE INDEX audit_events_security_note_ix ON public.audit_events USING btree (security_event_id, occurred_at) WHERE ((event_category)::text = 'SECURITY_INVESTIGATION_NOTE'::text);
CREATE INDEX audit_events_subject_time_ix ON public.audit_events USING btree (subject_user_id, occurred_at);

-- Table Triggers

create trigger audit_events_immutable_trg before
delete
    or
update
    on
    public.audit_events for each row execute function carebridge_reject_mutation();

-- Permissions

ALTER TABLE public.audit_events OWNER TO postgres;
GRANT ALL ON TABLE public.audit_events TO postgres;


-- public.auth_challenges definition

-- Drop table

-- DROP TABLE public.auth_challenges;

CREATE TABLE public.auth_challenges ( challenge_id uuid DEFAULT gen_random_uuid() NOT NULL, user_id uuid NULL, challenge_type varchar(40) NOT NULL, subject_identifier varchar(255) NULL, challenge_hash varchar(255) NOT NULL, attempts int4 DEFAULT 0 NOT NULL, expires_at timestamptz NOT NULL, used_at timestamptz NULL, status varchar(30) NOT NULL, requested_role varchar(40) NULL, created_at timestamptz DEFAULT now() NOT NULL, legacy_source varchar(40) NULL, legacy_id varchar(100) NULL, CONSTRAINT auth_challenges_legacy_uk UNIQUE (legacy_source, legacy_id), CONSTRAINT auth_challenges_pkey PRIMARY KEY (challenge_id), CONSTRAINT auth_challenges_status_ck CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'VERIFIED'::character varying, 'USED'::character varying, 'EXPIRED'::character varying, 'REVOKED'::character varying])::text[]))));
CREATE INDEX auth_challenges_subject_expiry_ix ON public.auth_challenges USING btree (subject_identifier, challenge_type, expires_at);

-- Permissions

ALTER TABLE public.auth_challenges OWNER TO postgres;
GRANT ALL ON TABLE public.auth_challenges TO postgres;


-- public.auth_sessions definition

-- Drop table

-- DROP TABLE public.auth_sessions;

CREATE TABLE public.auth_sessions ( session_id uuid DEFAULT gen_random_uuid() NOT NULL, user_id uuid NULL, token_family_id uuid NOT NULL, device_identifier varchar(255) NOT NULL, device_name varchar(255) NULL, refresh_token_hash varchar(255) NULL, issued_at timestamptz NOT NULL, expires_at timestamptz NOT NULL, last_used_at timestamptz NULL, rotated_at timestamptz NULL, revoked_at timestamptz NULL, revoke_reason varchar(100) NULL, status varchar(30) NOT NULL, created_ip_hash varchar(255) NULL, user_agent_hash varchar(255) NULL, legacy_source varchar(40) NULL, legacy_id varchar(100) NULL, detected_reuse bool DEFAULT false NOT NULL, revocation_metadata_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, CONSTRAINT auth_sessions_legacy_uk UNIQUE (legacy_source, legacy_id), CONSTRAINT auth_sessions_pkey PRIMARY KEY (session_id), CONSTRAINT auth_sessions_status_ck CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'ROTATED'::character varying, 'REVOKED'::character varying, 'EXPIRED'::character varying])::text[]))));
CREATE INDEX auth_sessions_family_ix ON public.auth_sessions USING btree (token_family_id);
CREATE INDEX auth_sessions_user_device_ix ON public.auth_sessions USING btree (user_id, device_identifier, status);

-- Permissions

ALTER TABLE public.auth_sessions OWNER TO postgres;
GRANT ALL ON TABLE public.auth_sessions TO postgres;


-- public.care_facilities definition

-- Drop table

-- DROP TABLE public.care_facilities;

CREATE TABLE public.care_facilities ( facility_id uuid DEFAULT gen_random_uuid() NOT NULL, partner_id uuid NULL, "name" varchar(255) NOT NULL, facility_type varchar(50) NULL, address varchar(500) NULL, latitude numeric NULL, longitude numeric NULL, phone varchar(30) NULL, opening_hours_json jsonb NULL, source_type varchar(30) NULL, verification_status varchar(30) DEFAULT 'UNVERIFIED'::character varying NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, facility_level varchar(50) NULL, ownership_type varchar(30) NULL, province_id varchar(2) NULL, district_id varchar(4) NULL, external_source_id varchar(150) NULL, is_active bool DEFAULT true NOT NULL, is_searchable bool DEFAULT true NOT NULL, administrative_area_id uuid NULL, CONSTRAINT care_facilities_ownership_type_check CHECK (((ownership_type IS NULL) OR ((ownership_type)::text = ANY ((ARRAY['PUBLIC'::character varying, 'MILITARY'::character varying])::text[])))), CONSTRAINT care_facilities_pkey PRIMARY KEY (facility_id), CONSTRAINT care_facilities_searchable_coordinates_check CHECK (((is_searchable = false) OR ((latitude IS NOT NULL) AND (longitude IS NOT NULL)))), CONSTRAINT care_facilities_source_type_check CHECK (((source_type IS NULL) OR ((source_type)::text = ANY ((ARRAY['MANUAL'::character varying, 'TRACKASIA'::character varying, 'LEGACY_IMPORT'::character varying])::text[])))));
CREATE INDEX care_facilities_area_ix ON public.care_facilities USING btree (administrative_area_id);
CREATE INDEX idx_care_facilities_facility_type ON public.care_facilities USING btree (facility_type);
CREATE INDEX idx_care_facilities_nearby_eligible ON public.care_facilities USING btree (facility_type, province_id, district_id) WHERE ((is_active = true) AND (is_searchable = true) AND (latitude IS NOT NULL) AND (longitude IS NOT NULL));
CREATE INDEX idx_care_facilities_partner_id ON public.care_facilities USING btree (partner_id);
CREATE UNIQUE INDEX uq_care_facilities_external_source ON public.care_facilities USING btree (source_type, external_source_id) WHERE (external_source_id IS NOT NULL);

-- Permissions

ALTER TABLE public.care_facilities OWNER TO postgres;
GRANT ALL ON TABLE public.care_facilities TO postgres;


-- public.care_group_members definition

-- Drop table

-- DROP TABLE public.care_group_members;

CREATE TABLE public.care_group_members ( care_group_member_id uuid DEFAULT gen_random_uuid() NOT NULL, care_group_id uuid NOT NULL, user_id uuid NOT NULL, member_role varchar(50) NULL, invitation_status varchar(20) DEFAULT 'PENDING'::character varying NOT NULL, permission_json jsonb NULL, joined_at timestamptz NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, invite_token varchar(64) NULL, invite_channel varchar(20) NULL, invite_expires_at timestamptz NULL, invited_phone varchar(20) NULL, data_permission_id uuid NULL, CONSTRAINT care_group_members_pkey PRIMARY KEY (care_group_member_id));
CREATE INDEX idx_care_group_members_care_group_id ON public.care_group_members USING btree (care_group_id);
CREATE INDEX idx_care_group_members_user_id ON public.care_group_members USING btree (user_id);
CREATE UNIQUE INDEX uq_care_group_members_invite_token ON public.care_group_members USING btree (invite_token) WHERE (invite_token IS NOT NULL);

-- Permissions

ALTER TABLE public.care_group_members OWNER TO postgres;
GRANT ALL ON TABLE public.care_group_members TO postgres;


-- public.care_groups definition

-- Drop table

-- DROP TABLE public.care_groups;

CREATE TABLE public.care_groups ( care_group_id uuid DEFAULT gen_random_uuid() NOT NULL, owner_user_id uuid NOT NULL, journey_id uuid NULL, baby_id uuid NULL, group_name varchar(200) NOT NULL, status varchar(20) DEFAULT 'ACTIVE'::character varying NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, description varchar(500) NULL, linked_journey_id uuid NULL, linked_baby_profile_id uuid NULL, care_subject_id uuid NULL, CONSTRAINT care_groups_pkey PRIMARY KEY (care_group_id));
CREATE INDEX idx_care_groups_owner_user_id ON public.care_groups USING btree (owner_user_id);

-- Permissions

ALTER TABLE public.care_groups OWNER TO postgres;
GRANT ALL ON TABLE public.care_groups TO postgres;


-- public.care_logs definition

-- Drop table

-- DROP TABLE public.care_logs;

CREATE TABLE public.care_logs ( care_log_id uuid DEFAULT gen_random_uuid() NOT NULL, care_subject_id uuid NOT NULL, log_type varchar(40) NOT NULL, started_at timestamptz NULL, ended_at timestamptz NULL, quantity numeric NULL, unit varchar(30) NULL, note text NULL, recorded_by uuid NULL, status varchar(30) NOT NULL, payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT care_logs_pkey PRIMARY KEY (care_log_id));
CREATE INDEX care_logs_subject_type_time_ix ON public.care_logs USING btree (care_subject_id, log_type, started_at);

-- Permissions

ALTER TABLE public.care_logs OWNER TO postgres;
GRANT ALL ON TABLE public.care_logs TO postgres;


-- public.care_subjects definition

-- Drop table

-- DROP TABLE public.care_subjects;

CREATE TABLE public.care_subjects ( care_subject_id uuid DEFAULT gen_random_uuid() NOT NULL, person_id uuid NOT NULL, owner_user_id uuid NOT NULL, mother_journey_id uuid NULL, subject_type varchar(30) NOT NULL, nickname varchar(200) NULL, birth_date date NULL, sex varchar(30) NULL, birth_weight_kg numeric(6, 3) NULL, birth_length_cm numeric(6, 2) NULL, status varchar(30) DEFAULT 'ACTIVE'::character varying NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT care_subjects_pkey PRIMARY KEY (care_subject_id), CONSTRAINT care_subjects_type_ck CHECK (((subject_type)::text = ANY ((ARRAY['MOTHER'::character varying, 'BABY'::character varying, 'DEPENDENT'::character varying])::text[]))));

-- Permissions

ALTER TABLE public.care_subjects OWNER TO postgres;
GRANT ALL ON TABLE public.care_subjects TO postgres;


-- public.care_tasks definition

-- Drop table

-- DROP TABLE public.care_tasks;

CREATE TABLE public.care_tasks ( task_id uuid DEFAULT gen_random_uuid() NOT NULL, task_type varchar(40) NOT NULL, owner_user_id uuid NULL, care_group_id uuid NULL, creator_user_id uuid NULL, assignee_user_id uuid NULL, care_subject_id uuid NULL, title varchar(255) NOT NULL, description text NULL, scheduled_at timestamptz NULL, recurrence_rule varchar(255) NULL, snoozed_until timestamptz NULL, completed_at timestamptz NULL, cancelled_at timestamptz NULL, skipped_at timestamptz NULL, status varchar(30) DEFAULT 'PENDING'::character varying NOT NULL, source_reference_type varchar(60) NULL, source_reference_id uuid NULL, vaccination_record_id uuid NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, journey_id uuid NULL, baby_id uuid NULL, recurrence_type varchar(30) NULL, recurrence_end_date timestamptz NULL, fcm_job_id varchar(255) NULL, CONSTRAINT care_tasks_pkey1 PRIMARY KEY (task_id), CONSTRAINT care_tasks_type_ck CHECK (((task_type)::text = ANY ((ARRAY['SCHEDULED_REMINDER'::character varying, 'MANUAL_TASK'::character varying])::text[]))), CONSTRAINT care_tasks_vaccination_ck CHECK ((((task_type)::text <> 'SCHEDULED_REMINDER'::text) OR ((source_reference_type)::text <> 'VACCINATION'::text) OR (vaccination_record_id IS NOT NULL) OR (care_subject_id IS NOT NULL))));
CREATE INDEX care_tasks_assignee_status_ix ON public.care_tasks USING btree (assignee_user_id, status, scheduled_at) WHERE (assignee_user_id IS NOT NULL);
CREATE INDEX care_tasks_context_ix ON public.care_tasks USING btree (owner_user_id, journey_id, baby_id, status, scheduled_at) WHERE (owner_user_id IS NOT NULL);
CREATE INDEX care_tasks_owner_status_ix ON public.care_tasks USING btree (owner_user_id, status, scheduled_at) WHERE (owner_user_id IS NOT NULL);

-- Permissions

ALTER TABLE public.care_tasks OWNER TO postgres;
GRANT ALL ON TABLE public.care_tasks TO postgres;


-- public.community_content definition

-- Drop table

-- DROP TABLE public.community_content;

CREATE TABLE public.community_content ( content_id uuid DEFAULT gen_random_uuid() NOT NULL, topic_id uuid NULL, parent_content_id uuid NULL, author_user_id uuid NOT NULL, content_type varchar(20) NOT NULL, title varchar(255) NULL, body text NOT NULL, stage varchar(30) NULL, urgency varchar(20) NULL, is_anonymous bool DEFAULT false NOT NULL, moderation_status varchar(30) DEFAULT 'PENDING'::character varying NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, pregnancy_week int2 NULL, baby_age_months int2 NULL, like_count int4 DEFAULT 0 NOT NULL, answer_count int4 DEFAULT 0 NOT NULL, is_expert_labeled bool DEFAULT false NOT NULL, is_personal_experience bool DEFAULT false NOT NULL, CONSTRAINT community_content_pkey PRIMARY KEY (content_id), CONSTRAINT community_content_type_ck CHECK (((content_type)::text = ANY ((ARRAY['QUESTION'::character varying, 'ANSWER'::character varying, 'POST'::character varying])::text[]))));
CREATE INDEX community_content_parent_ix ON public.community_content USING btree (parent_content_id);
CREATE INDEX community_content_topic_ix ON public.community_content USING btree (topic_id, created_at);

-- Permissions

ALTER TABLE public.community_content OWNER TO postgres;
GRANT ALL ON TABLE public.community_content TO postgres;


-- public.community_interactions definition

-- Drop table

-- DROP TABLE public.community_interactions;

CREATE TABLE public.community_interactions ( interaction_id uuid DEFAULT gen_random_uuid() NOT NULL, actor_user_id uuid NOT NULL, interaction_type varchar(30) NOT NULL, content_id uuid NULL, topic_id uuid NULL, created_at timestamptz DEFAULT now() NOT NULL, target_content_type varchar(20) NULL, CONSTRAINT community_interactions_one_target_ck CHECK (((content_id IS NOT NULL) <> (topic_id IS NOT NULL))), CONSTRAINT community_interactions_pkey PRIMARY KEY (interaction_id), CONSTRAINT community_interactions_type_ck CHECK (((interaction_type)::text = ANY ((ARRAY['REACTION'::character varying, 'BOOKMARK'::character varying, 'FOLLOW'::character varying, 'MUTE'::character varying])::text[]))));
CREATE UNIQUE INDEX community_interactions_content_target_uk ON public.community_interactions USING btree (actor_user_id, interaction_type, content_id) WHERE (content_id IS NOT NULL);
CREATE UNIQUE INDEX community_interactions_topic_target_uk ON public.community_interactions USING btree (actor_user_id, interaction_type, topic_id) WHERE (topic_id IS NOT NULL);

-- Permissions

ALTER TABLE public.community_interactions OWNER TO postgres;
GRANT ALL ON TABLE public.community_interactions TO postgres;


-- public.content_item_sources definition

-- Drop table

-- DROP TABLE public.content_item_sources;

CREATE TABLE public.content_item_sources ( content_item_source_id uuid DEFAULT gen_random_uuid() NOT NULL, content_item_id uuid NOT NULL, knowledge_source_id uuid NULL, source_title varchar(500) NOT NULL, source_url varchar(2000) NULL, source_publisher varchar(255) NULL, source_snapshot_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT content_item_sources_pkey PRIMARY KEY (content_item_source_id), CONSTRAINT content_item_sources_unique_url_uk UNIQUE (content_item_id, source_url));

-- Permissions

ALTER TABLE public.content_item_sources OWNER TO postgres;
GRANT ALL ON TABLE public.content_item_sources TO postgres;


-- public.development_milestones definition

-- Drop table

-- DROP TABLE public.development_milestones;

CREATE TABLE public.development_milestones ( milestone_id uuid DEFAULT gen_random_uuid() NOT NULL, baby_id uuid NOT NULL, milestone_type varchar(80) NOT NULL, achieved_date date NULL, note text NULL, source_type varchar(30) NULL, recorded_by uuid NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, milestone_status varchar(20) DEFAULT 'ACHIEVED'::character varying NOT NULL, record_status varchar(20) DEFAULT 'ACTIVE'::character varying NOT NULL, care_subject_id uuid NOT NULL, CONSTRAINT development_milestones_pkey PRIMARY KEY (milestone_id));
CREATE INDEX development_milestones_subject_ix ON public.development_milestones USING btree (care_subject_id, milestone_type, achieved_date);
CREATE INDEX idx_development_milestones_baby_id ON public.development_milestones USING btree (baby_id);
CREATE INDEX idx_development_milestones_baby_record_status ON public.development_milestones USING btree (baby_id, record_status);

-- Permissions

ALTER TABLE public.development_milestones OWNER TO postgres;
GRANT ALL ON TABLE public.development_milestones TO postgres;


-- public.device_connections definition

-- Drop table

-- DROP TABLE public.device_connections;

CREATE TABLE public.device_connections ( device_connection_id uuid DEFAULT gen_random_uuid() NOT NULL, user_id uuid NOT NULL, provider_name varchar(80) NOT NULL, device_name varchar(150) NULL, scopes_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, token_reference text NULL, consent_granted_at timestamptz NULL, last_synced_at timestamptz NULL, status varchar(20) DEFAULT 'ACTIVE'::character varying NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT device_connections_pkey PRIMARY KEY (device_connection_id));
CREATE INDEX device_connections_user_status_ix ON public.device_connections USING btree (user_id, status);

-- Permissions

ALTER TABLE public.device_connections OWNER TO postgres;
GRANT ALL ON TABLE public.device_connections TO postgres;


-- public.device_tokens definition

-- Drop table

-- DROP TABLE public.device_tokens;

CREATE TABLE public.device_tokens ( id uuid DEFAULT gen_random_uuid() NOT NULL, user_id uuid NOT NULL, "token" varchar(512) NOT NULL, platform varchar(30) NOT NULL, active bool DEFAULT true NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT device_tokens_pkey PRIMARY KEY (id), CONSTRAINT device_tokens_platform_check CHECK (((platform)::text = ANY (ARRAY[('ANDROID'::character varying)::text, ('IOS'::character varying)::text, ('WEB'::character varying)::text]))), CONSTRAINT device_tokens_unique UNIQUE (user_id, token));
CREATE INDEX idx_device_tokens_user_id ON public.device_tokens USING btree (user_id);

-- Permissions

ALTER TABLE public.device_tokens OWNER TO postgres;
GRANT ALL ON TABLE public.device_tokens TO postgres;


-- public.expense_entries definition

-- Drop table

-- DROP TABLE public.expense_entries;

CREATE TABLE public.expense_entries ( expense_entry_id uuid DEFAULT gen_random_uuid() NOT NULL, owner_user_id uuid NOT NULL, care_subject_id uuid NULL, mother_journey_id uuid NULL, category varchar(80) NULL, amount numeric NOT NULL, currency varchar(10) DEFAULT 'VND'::character varying NOT NULL, expense_date date NOT NULL, note text NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT expense_entries_pkey PRIMARY KEY (expense_entry_id));
CREATE INDEX expense_entries_owner_date_ix ON public.expense_entries USING btree (owner_user_id, expense_date);

-- Permissions

ALTER TABLE public.expense_entries OWNER TO postgres;
GRANT ALL ON TABLE public.expense_entries TO postgres;


-- public.expert_credentials definition

-- Drop table

-- DROP TABLE public.expert_credentials;

CREATE TABLE public.expert_credentials ( credential_id uuid DEFAULT gen_random_uuid() NOT NULL, credential_type varchar(50) NOT NULL, credential_number varchar(100) NULL, issuer varchar(200) NULL, issued_date date NULL, expiry_date date NULL, file_url text NULL, review_status varchar(30) DEFAULT 'PENDING'::character varying NOT NULL, review_note text NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, reviewed_by uuid NULL, reviewed_at timestamp NULL, file_id uuid NULL, user_id uuid NOT NULL, CONSTRAINT expert_credentials_pkey PRIMARY KEY (credential_id));
CREATE INDEX expert_credentials_user_status_ix ON public.expert_credentials USING btree (user_id, review_status);
CREATE INDEX idx_expert_credentials_file_id ON public.expert_credentials USING btree (file_id);
CREATE INDEX idx_expert_credentials_review_status ON public.expert_credentials USING btree (reviewed_by, reviewed_at);

-- Permissions

ALTER TABLE public.expert_credentials OWNER TO postgres;
GRANT ALL ON TABLE public.expert_credentials TO postgres;


-- public.growth_measurements definition

-- Drop table

-- DROP TABLE public.growth_measurements;

CREATE TABLE public.growth_measurements ( growth_measurement_id uuid DEFAULT gen_random_uuid() NOT NULL, baby_id uuid NOT NULL, measured_date date NOT NULL, weight_kg numeric NULL, height_cm numeric NULL, head_circumference_cm numeric NULL, source_type varchar(30) NULL, note text NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, deleted_at timestamptz NULL, care_subject_id uuid NOT NULL, CONSTRAINT growth_measurements_pkey PRIMARY KEY (growth_measurement_id));
CREATE INDEX growth_measurements_chart_ix ON public.growth_measurements USING btree (care_subject_id, measured_date) WHERE (deleted_at IS NULL);
CREATE INDEX idx_growth_measurements_baby_id ON public.growth_measurements USING btree (baby_id);

-- Permissions

ALTER TABLE public.growth_measurements OWNER TO postgres;
GRANT ALL ON TABLE public.growth_measurements TO postgres;


-- public.health_context_memories definition

-- Drop table

-- DROP TABLE public.health_context_memories;

CREATE TABLE public.health_context_memories ( memory_id uuid DEFAULT gen_random_uuid() NOT NULL, user_id uuid NOT NULL, care_subject_id uuid NULL, triage_session_id uuid NULL, related_stage varchar(30) NOT NULL, summary_text text NOT NULL, memory_payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, expires_at timestamptz NULL, deleted_at timestamptz NULL, mother_profile_id uuid NULL, baby_profile_id uuid NULL, CONSTRAINT health_context_memories_pkey PRIMARY KEY (memory_id));
CREATE INDEX health_context_memories_baby_ix ON public.health_context_memories USING btree (baby_profile_id, created_at DESC) WHERE (deleted_at IS NULL);
CREATE INDEX health_context_memories_mother_ix ON public.health_context_memories USING btree (mother_profile_id, created_at DESC) WHERE (deleted_at IS NULL);
CREATE INDEX health_context_memories_subject_expiry_ix ON public.health_context_memories USING btree (care_subject_id, expires_at) WHERE (deleted_at IS NULL);

-- Permissions

ALTER TABLE public.health_context_memories OWNER TO postgres;
GRANT ALL ON TABLE public.health_context_memories TO postgres;


-- public.health_observations definition

-- Drop table

-- DROP TABLE public.health_observations;

CREATE TABLE public.health_observations ( health_observation_id uuid DEFAULT gen_random_uuid() NOT NULL, care_subject_id uuid NOT NULL, device_connection_id uuid NULL, subject_type varchar(30) NOT NULL, observation_type varchar(60) NOT NULL, value_numeric numeric NULL, value_secondary numeric NULL, unit varchar(40) NULL, text_value text NULL, severity varchar(30) NULL, observed_at timestamptz NOT NULL, source_record_id uuid NULL, source_type varchar(60) DEFAULT 'SYSTEM'::character varying NOT NULL, quality_label varchar(30) NULL, raw_payload_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, legacy_source varchar(60) NULL, legacy_id varchar(100) NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT health_observations_legacy_uk UNIQUE (legacy_source, legacy_id), CONSTRAINT health_observations_pkey PRIMARY KEY (health_observation_id), CONSTRAINT health_observations_type_ck CHECK (((subject_type)::text = ANY ((ARRAY['MOTHER'::character varying, 'BABY'::character varying, 'DEPENDENT'::character varying])::text[]))));
CREATE INDEX health_observations_device_time_ix ON public.health_observations USING btree (device_connection_id, observed_at) WHERE (device_connection_id IS NOT NULL);
CREATE INDEX health_observations_severity_ix ON public.health_observations USING btree (severity, observed_at) WHERE (severity IS NOT NULL);
CREATE INDEX health_observations_subject_chart_ix ON public.health_observations USING btree (care_subject_id, observation_type, observed_at);

-- Permissions

ALTER TABLE public.health_observations OWNER TO postgres;
GRANT ALL ON TABLE public.health_observations TO postgres;


-- public.health_records definition

-- Drop table

-- DROP TABLE public.health_records;

CREATE TABLE public.health_records ( health_record_id uuid DEFAULT gen_random_uuid() NOT NULL, owner_user_id uuid NOT NULL, journey_id uuid NULL, baby_id uuid NULL, record_type varchar(50) NOT NULL, title varchar(255) NOT NULL, file_url text NULL, record_date date NULL, source_type varchar(30) NULL, source_name varchar(200) NULL, status varchar(20) DEFAULT 'ACTIVE'::character varying NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, care_subject_id uuid NULL, summary_period varchar(30) NULL, period_start date NULL, summary_json jsonb NULL, CONSTRAINT health_records_pkey PRIMARY KEY (health_record_id));
CREATE INDEX health_records_summary_filter_ix ON public.health_records USING btree (owner_user_id, summary_period, record_date DESC) WHERE (((record_type)::text = 'SUMMARY'::text) AND ((status)::text = 'ACTIVE'::text));
CREATE INDEX idx_health_records_baby_id ON public.health_records USING btree (baby_id);
CREATE INDEX idx_health_records_journey_id ON public.health_records USING btree (journey_id);
CREATE INDEX idx_health_records_owner_user_id ON public.health_records USING btree (owner_user_id);

-- Permissions

ALTER TABLE public.health_records OWNER TO postgres;
GRANT ALL ON TABLE public.health_records TO postgres;


-- public.knowledge_source_reviews definition

-- Drop table

-- DROP TABLE public.knowledge_source_reviews;

CREATE TABLE public.knowledge_source_reviews ( review_id uuid DEFAULT gen_random_uuid() NOT NULL, knowledge_source_id uuid NOT NULL, previous_status varchar(30) NULL, new_status varchar(30) NOT NULL, actor_user_id uuid NULL, actor_role varchar(80) NULL, notes text NULL, changed_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT knowledge_source_reviews_pkey PRIMARY KEY (review_id));
CREATE INDEX knowledge_source_reviews_source_time_ix ON public.knowledge_source_reviews USING btree (knowledge_source_id, changed_at);

-- Table Triggers

create trigger knowledge_source_reviews_immutable_trg before
delete
    or
update
    on
    public.knowledge_source_reviews for each row execute function carebridge_reject_mutation();

-- Permissions

ALTER TABLE public.knowledge_source_reviews OWNER TO postgres;
GRANT ALL ON TABLE public.knowledge_source_reviews TO postgres;


-- public.knowledge_sources definition

-- Drop table

-- DROP TABLE public.knowledge_sources;

CREATE TABLE public.knowledge_sources ( knowledge_source_id uuid DEFAULT gen_random_uuid() NOT NULL, "domain" varchar(255) NOT NULL, base_url varchar(500) NOT NULL, organization varchar(255) NOT NULL, category varchar(40) NOT NULL, status varchar(30) NOT NULL, discovery_mode varchar(40) NOT NULL, applicable_stages text NULL, added_by uuid NULL, reviewed_by uuid NULL, reviewed_at timestamptz NULL, notes text NULL, source_version varchar(80) NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT knowledge_sources_pkey PRIMARY KEY (knowledge_source_id));
CREATE INDEX knowledge_sources_domain_status_ix ON public.knowledge_sources USING btree (domain, status);
CREATE UNIQUE INDEX knowledge_sources_domain_uk ON public.knowledge_sources USING btree (lower((domain)::text));

-- Permissions

ALTER TABLE public.knowledge_sources OWNER TO postgres;
GRANT ALL ON TABLE public.knowledge_sources TO postgres;


-- public.maternal_exercise_sessions definition

-- Drop table

-- DROP TABLE public.maternal_exercise_sessions;

CREATE TABLE public.maternal_exercise_sessions ( exercise_session_id uuid DEFAULT gen_random_uuid() NOT NULL, mother_journey_id uuid NULL, owner_user_id uuid NOT NULL, exercise_template_id uuid NOT NULL, posture_config_id uuid NULL, started_at timestamptz NOT NULL, ended_at timestamptz NULL, paused_seconds int4 DEFAULT 0 NOT NULL, completion_percent numeric(5, 2) NULL, posture_score numeric(6, 3) NULL, session_status varchar(30) NOT NULL, warning_count int4 DEFAULT 0 NOT NULL, summary_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, safety_observation_id uuid NULL, CONSTRAINT maternal_exercise_sessions_pkey PRIMARY KEY (exercise_session_id));

-- Permissions

ALTER TABLE public.maternal_exercise_sessions OWNER TO postgres;
GRANT ALL ON TABLE public.maternal_exercise_sessions TO postgres;


-- public.moderation_cases definition

-- Drop table

-- DROP TABLE public.moderation_cases;

CREATE TABLE public.moderation_cases ( moderation_case_id uuid DEFAULT gen_random_uuid() NOT NULL, reporter_user_id uuid NULL, assigned_moderator_id uuid NULL, target_type varchar(40) NOT NULL, target_id uuid NOT NULL, reason_code varchar(80) NULL, description text NULL, status varchar(30) DEFAULT 'OPEN'::character varying NOT NULL, opened_at timestamptz DEFAULT now() NOT NULL, resolved_at timestamptz NULL, updated_at timestamptz DEFAULT now() NOT NULL, report_source varchar(20) DEFAULT 'USER'::character varying NOT NULL, reverted_at timestamptz NULL, reverted_by uuid NULL, CONSTRAINT moderation_cases_pkey PRIMARY KEY (moderation_case_id));
CREATE INDEX moderation_cases_report_source_ix ON public.moderation_cases USING btree (report_source, status, opened_at DESC);
CREATE INDEX moderation_cases_target_ix ON public.moderation_cases USING btree (target_type, target_id, status);

-- Permissions

ALTER TABLE public.moderation_cases OWNER TO postgres;
GRANT ALL ON TABLE public.moderation_cases TO postgres;


-- public.mother_journeys definition

-- Drop table

-- DROP TABLE public.mother_journeys;

CREATE TABLE public.mother_journeys ( journey_id uuid DEFAULT gen_random_uuid() NOT NULL, owner_user_id uuid NOT NULL, journey_type varchar(20) NOT NULL, start_date date NULL, last_menstrual_date date NULL, estimated_due_date date NULL, delivery_date date NULL, status varchar(20) DEFAULT 'ACTIVE'::character varying NOT NULL, notes text NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, "version" int8 DEFAULT 0 NOT NULL, date_source varchar(30) NULL, date_confidence varchar(20) NULL, pregnancy_outcome varchar(30) NULL, pregnancy_outcome_date date NULL, care_subject_id uuid NOT NULL, baseline_revision int8 NULL, baseline_schema_version varchar(40) NULL, baseline_source varchar(30) NULL, baseline_lifecycle_goal varchar(40) NULL, baseline_locale varchar(20) NULL, baseline_time_zone varchar(80) NULL, baseline_preferences varchar(300) NULL, baseline_submission_id uuid NULL, baseline_recorded_at timestamptz NULL, CONSTRAINT chk_mother_journeys_date_confidence CHECK (((date_confidence IS NULL) OR ((date_confidence)::text = ANY ((ARRAY['CONFIRMED'::character varying, 'ESTIMATED'::character varying, 'UNKNOWN'::character varying])::text[])))), CONSTRAINT chk_mother_journeys_date_source CHECK (((date_source IS NULL) OR ((date_source)::text = ANY ((ARRAY['SELF_REPORTED'::character varying, 'CLINICIAN_CONFIRMED'::character varying, 'ULTRASOUND'::character varying, 'SYSTEM_DERIVED'::character varying, 'MIGRATION'::character varying, 'UNKNOWN'::character varying])::text[])))), CONSTRAINT ck_mother_journey_live_birth_date CHECK ((((pregnancy_outcome)::text <> 'LIVE_BIRTH'::text) OR (pregnancy_outcome_date IS NOT NULL))), CONSTRAINT ck_mother_journey_pregnancy_outcome CHECK (((pregnancy_outcome IS NULL) OR ((pregnancy_outcome)::text = ANY ((ARRAY['ONGOING'::character varying, 'UNKNOWN'::character varying, 'LIVE_BIRTH'::character varying, 'PREGNANCY_LOSS'::character varying, 'STILLBIRTH'::character varying])::text[])))), CONSTRAINT mother_journeys_pkey PRIMARY KEY (journey_id), CONSTRAINT mother_journeys_subject_uk UNIQUE (care_subject_id));
CREATE INDEX idx_mother_journeys_owner_user_id ON public.mother_journeys USING btree (owner_user_id);
CREATE INDEX idx_mother_journeys_status ON public.mother_journeys USING btree (status);
CREATE UNIQUE INDEX uq_mother_journeys_one_canonical_active ON public.mother_journeys USING btree (owner_user_id) WHERE (((status)::text = 'ACTIVE'::text) AND ((journey_type)::text = ANY ((ARRAY['PRE_PREGNANCY'::character varying, 'PREGNANCY'::character varying, 'POSTPARTUM'::character varying])::text[])));

-- Permissions

ALTER TABLE public.mother_journeys OWNER TO postgres;
GRANT ALL ON TABLE public.mother_journeys TO postgres;


-- public.nearby_support_interactions definition

-- Drop table

-- DROP TABLE public.nearby_support_interactions;

CREATE TABLE public.nearby_support_interactions ( interaction_id uuid DEFAULT gen_random_uuid() NOT NULL, interaction_type varchar(30) NOT NULL, parent_interaction_id uuid NULL, user_id uuid NOT NULL, care_subject_id uuid NULL, latitude numeric NULL, longitude numeric NULL, radius_meters int4 NULL, message text NULL, status varchar(30) DEFAULT 'OPEN'::character varying NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT nearby_support_interactions_pkey PRIMARY KEY (interaction_id), CONSTRAINT nearby_support_interactions_type_ck CHECK (((interaction_type)::text = ANY ((ARRAY['REQUEST'::character varying, 'RESPONSE'::character varying])::text[]))));

-- Permissions

ALTER TABLE public.nearby_support_interactions OWNER TO postgres;
GRANT ALL ON TABLE public.nearby_support_interactions TO postgres;


-- public.notification_records definition

-- Drop table

-- DROP TABLE public.notification_records;

CREATE TABLE public.notification_records ( id uuid DEFAULT gen_random_uuid() NOT NULL, user_id uuid NOT NULL, "type" varchar(50) NOT NULL, title varchar(255) NOT NULL, body text NOT NULL, reference_id uuid NULL, reference_type varchar(50) NULL, status varchar(20) DEFAULT 'SENT'::character varying NOT NULL, fcm_message_id varchar(255) NULL, attempt_count int4 DEFAULT 1 NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, sent_at timestamptz NULL, failed_at timestamptz NULL, is_read bool DEFAULT false NOT NULL, read_at timestamptz NULL, metadata jsonb NULL, processing_started_at timestamptz NULL, channel varchar(30) DEFAULT 'PUSH'::character varying NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT notification_records_channel_check CHECK (((channel)::text = ANY ((ARRAY['PUSH'::character varying, 'EMAIL'::character varying, 'IN_APP'::character varying])::text[]))), CONSTRAINT notification_records_pkey PRIMARY KEY (id), CONSTRAINT notification_records_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'SENT'::character varying, 'DELIVERED'::character varying, 'FAILED'::character varying])::text[]))), CONSTRAINT notification_records_type_check CHECK (((type)::text = ANY ((ARRAY['REMINDER'::character varying, 'COMMUNITY_REPLY'::character varying, 'CONSULTATION'::character varying, 'EMERGENCY'::character varying, 'MESSAGE'::character varying, 'GROUP_INVITE'::character varying])::text[]))));
CREATE INDEX idx_notification_records_type_status ON public.notification_records USING btree (type, status);
CREATE INDEX idx_notification_records_user_id ON public.notification_records USING btree (user_id, created_at DESC);
CREATE INDEX idx_notification_records_user_unread ON public.notification_records USING btree (user_id, is_read) WHERE (is_read = false);
CREATE UNIQUE INDEX uq_notification_records_direct_message ON public.notification_records USING btree (user_id, reference_id) WHERE (((type)::text = 'MESSAGE'::text) AND ((reference_type)::text = 'DIRECT_MESSAGE'::text));

-- Permissions

ALTER TABLE public.notification_records OWNER TO postgres;
GRANT ALL ON TABLE public.notification_records TO postgres;


-- public.preparation_checklist_items definition

-- Drop table

-- DROP TABLE public.preparation_checklist_items;

CREATE TABLE public.preparation_checklist_items ( checklist_item_id uuid DEFAULT gen_random_uuid() NOT NULL, owner_user_id uuid NOT NULL, mother_journey_id uuid NULL, template_entry_id uuid NULL, title varchar(500) NOT NULL, display_order int4 DEFAULT 0 NOT NULL, status varchar(30) DEFAULT 'OPEN'::character varying NOT NULL, due_at timestamptz NULL, completed_at timestamptz NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, baby_id uuid NULL, category varchar(50) DEFAULT 'GENERAL'::character varying NOT NULL, CONSTRAINT preparation_checklist_items_pkey PRIMARY KEY (checklist_item_id));
CREATE INDEX preparation_checklist_filter_ix ON public.preparation_checklist_items USING btree (owner_user_id, mother_journey_id, baby_id, status, display_order);
CREATE INDEX preparation_checklist_owner_journey_ix ON public.preparation_checklist_items USING btree (owner_user_id, mother_journey_id, display_order);
CREATE UNIQUE INDEX uq_preparation_checklist_import_scope ON public.preparation_checklist_items USING btree (owner_user_id, mother_journey_id, baby_id, template_entry_id) NULLS NOT DISTINCT WHERE (template_entry_id IS NOT NULL);

-- Permissions

ALTER TABLE public.preparation_checklist_items OWNER TO postgres;
GRANT ALL ON TABLE public.preparation_checklist_items TO postgres;


-- public.red_flag_rules definition

-- Drop table

-- DROP TABLE public.red_flag_rules;

CREATE TABLE public.red_flag_rules ( id uuid DEFAULT gen_random_uuid() NOT NULL, keyword varchar(255) NOT NULL, severity varchar(20) NOT NULL, "action" varchar(20) NOT NULL, is_active bool DEFAULT true NOT NULL, is_system_default bool DEFAULT false NOT NULL, created_by uuid NULL, updated_by uuid NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT chk_red_flag_rules_action CHECK (((action)::text = ANY ((ARRAY['BLOCK'::character varying, 'WARN'::character varying, 'ESCALATE'::character varying])::text[]))), CONSTRAINT chk_red_flag_rules_severity CHECK (((severity)::text = ANY ((ARRAY['GREEN'::character varying, 'YELLOW'::character varying, 'RED'::character varying])::text[]))), CONSTRAINT red_flag_rules_pkey PRIMARY KEY (id), CONSTRAINT uq_red_flag_rules_keyword UNIQUE (keyword));
CREATE INDEX idx_red_flag_rules_active_severity ON public.red_flag_rules USING btree (is_active, severity);
CREATE INDEX idx_red_flag_rules_is_system_default ON public.red_flag_rules USING btree (is_system_default);

-- Permissions

ALTER TABLE public.red_flag_rules OWNER TO postgres;
GRANT ALL ON TABLE public.red_flag_rules TO postgres;


-- public.safety_configs definition

-- Drop table

-- DROP TABLE public.safety_configs;

CREATE TABLE public.safety_configs ( safety_config_id uuid DEFAULT gen_random_uuid() NOT NULL, user_id uuid NOT NULL, fall_detection_enabled bool DEFAULT false NOT NULL, sensitivity_level varchar(10) DEFAULT 'MEDIUM'::character varying NOT NULL, emergency_auto_alert bool DEFAULT true NOT NULL, countdown_seconds int4 DEFAULT 30 NOT NULL, sensor_permission_granted bool DEFAULT false NOT NULL, sensor_permission_recorded_at timestamptz NULL, updated_at timestamptz DEFAULT now() NOT NULL, updated_by uuid NULL, CONSTRAINT safety_configs_countdown_seconds_check CHECK ((countdown_seconds = ANY (ARRAY[15, 30, 60]))), CONSTRAINT safety_configs_pkey PRIMARY KEY (safety_config_id), CONSTRAINT safety_configs_sensor_permission_ck CHECK (((sensor_permission_granted = false) OR (sensor_permission_recorded_at IS NOT NULL))), CONSTRAINT safety_configs_user_id_key UNIQUE (user_id));

-- Permissions

ALTER TABLE public.safety_configs OWNER TO postgres;
GRANT ALL ON TABLE public.safety_configs TO postgres;


-- public.safety_events definition

-- Drop table

-- DROP TABLE public.safety_events;

CREATE TABLE public.safety_events ( safety_event_id uuid DEFAULT gen_random_uuid() NOT NULL, user_id uuid NOT NULL, care_subject_id uuid NULL, monitoring_session_id uuid NULL, source_event_id uuid NULL, parent_event_id uuid NULL, detected_at timestamptz DEFAULT now() NOT NULL, event_type varchar(50) NOT NULL, action_type varchar(40) NULL, confidence_score numeric NULL, peak_acceleration numeric NULL, angular_velocity numeric NULL, inactivity_seconds int4 NULL, response_type varchar(30) NULL, response_at timestamptz NULL, false_positive_reason text NULL, status varchar(20) DEFAULT 'DETECTED'::character varying NOT NULL, location_snapshot_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, recipient_user_id uuid NULL, device_identifier varchar(255) NULL, notification_record_id uuid NULL, care_facility_id uuid NULL, attempt_number int4 NULL, idempotency_key varchar(255) NULL, delivery_status varchar(30) NULL, delivered_at timestamptz NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, record_type varchar(30) DEFAULT 'IMU_EVENT'::character varying NOT NULL, magnitude numeric(10, 4) NULL, user_latitude numeric(10, 8) NULL, user_longitude numeric(11, 8) NULL, client_detected_at timestamptz NULL, resolved_at timestamptz NULL, notes text NULL, signal_key varchar(200) NULL, countdown_deadline_at timestamptz NULL, response_reason varchar(500) NULL, escalation_started_at timestamptz NULL, emergency_session_id uuid NULL, created_by_text varchar(50) NULL, created_by_user_id uuid NULL, owner_user_id uuid NULL, context_type varchar(50) NULL, context_id uuid NULL, latitude numeric(10, 8) NULL, longitude numeric(11, 8) NULL, accuracy_meters numeric(6, 2) NULL, captured_at timestamptz NULL, expires_at timestamptz NULL, consent_status varchar(20) NULL, device_token_id uuid NULL, fcm_message_id varchar(255) NULL, failure_code varchar(120) NULL, reason varchar(500) NULL, responded_at timestamptz NULL, actor_type varchar(20) NULL, attempt_status varchar(20) NULL, started_at timestamptz NULL, completed_at timestamptz NULL, lease_expires_at timestamptz NULL, successful_recipient_count int4 NULL, failed_recipient_count int4 NULL, recipient_count int4 NULL, location_included bool NULL, triage_handoff_id uuid NULL, risk_level varchar(20) NULL, summary text NULL, action_status varchar(20) NULL, CONSTRAINT safety_events_action_type_ck CHECK (((action_type IS NULL) OR ((action_type)::text = ANY ((ARRAY['RESPONSE'::character varying, 'DELIVERY'::character varying, 'FAMILY_ALERT'::character varying, 'ALERT_ATTEMPT'::character varying, 'MAP_HANDOFF'::character varying, 'LOCATION_SNAPSHOT'::character varying])::text[])))), CONSTRAINT safety_events_attempt_ck CHECK (((attempt_number IS NULL) OR (attempt_number >= 0))), CONSTRAINT safety_events_idempotency_key_key UNIQUE (idempotency_key), CONSTRAINT safety_events_parent_ck CHECK (((action_type IS NULL) OR ((action_type)::text = ANY ((ARRAY['MAP_HANDOFF'::character varying, 'LOCATION_SNAPSHOT'::character varying])::text[])) OR (parent_event_id IS NOT NULL))), CONSTRAINT safety_events_pkey PRIMARY KEY (safety_event_id), CONSTRAINT safety_events_record_type_ck CHECK (((record_type)::text = ANY ((ARRAY['IMU_EVENT'::character varying, 'EMERGENCY_SESSION'::character varying])::text[]))));
CREATE UNIQUE INDEX safety_events_attempt_event_uk ON public.safety_events USING btree (parent_event_id) WHERE ((action_type)::text = 'ALERT_ATTEMPT'::text);
CREATE UNIQUE INDEX safety_events_delivery_token_uk ON public.safety_events USING btree (parent_event_id, device_token_id) WHERE ((action_type)::text = 'DELIVERY'::text);
CREATE UNIQUE INDEX safety_events_family_alert_uk ON public.safety_events USING btree (parent_event_id) WHERE ((action_type)::text = 'FAMILY_ALERT'::text);
CREATE INDEX safety_events_handoff_status_ix ON public.safety_events USING btree (action_status, created_at DESC) WHERE ((action_type)::text = 'MAP_HANDOFF'::text);
CREATE UNIQUE INDEX safety_events_imu_signal_uk ON public.safety_events USING btree (monitoring_session_id, signal_key) WHERE (((record_type)::text = 'IMU_EVENT'::text) AND (signal_key IS NOT NULL));
CREATE INDEX safety_events_owner_location_ix ON public.safety_events USING btree (owner_user_id, captured_at DESC) WHERE ((action_type)::text = 'LOCATION_SNAPSHOT'::text);
CREATE INDEX safety_events_parent_ix ON public.safety_events USING btree (parent_event_id);
CREATE INDEX safety_events_pending_countdown_ix ON public.safety_events USING btree (countdown_deadline_at) WHERE (((record_type)::text = 'IMU_EVENT'::text) AND ((status)::text = 'OPEN'::text) AND (response_type IS NULL));
CREATE INDEX safety_events_user_status_time_ix ON public.safety_events USING btree (user_id, status, detected_at);

-- Permissions

ALTER TABLE public.safety_events OWNER TO postgres;
GRANT ALL ON TABLE public.safety_events TO postgres;


-- public.safety_monitoring_sessions definition

-- Drop table

-- DROP TABLE public.safety_monitoring_sessions;

CREATE TABLE public.safety_monitoring_sessions ( monitoring_session_id uuid DEFAULT gen_random_uuid() NOT NULL, user_id uuid NOT NULL, status varchar(20) DEFAULT 'ACTIVE'::character varying NOT NULL, sensitivity_level varchar(10) DEFAULT 'MEDIUM'::character varying NOT NULL, started_at timestamptz DEFAULT now() NOT NULL, ended_at timestamptz NULL, created_by uuid NULL, CONSTRAINT safety_monitoring_sessions_pkey PRIMARY KEY (monitoring_session_id));
CREATE INDEX safety_monitoring_sessions_user_status_ix ON public.safety_monitoring_sessions USING btree (user_id, status);

-- Permissions

ALTER TABLE public.safety_monitoring_sessions OWNER TO postgres;
GRANT ALL ON TABLE public.safety_monitoring_sessions TO postgres;


-- public.system_configurations definition

-- Drop table

-- DROP TABLE public.system_configurations;

CREATE TABLE public.system_configurations ( system_configuration_id uuid DEFAULT gen_random_uuid() NOT NULL, api_rate_limit int4 NOT NULL, connection_timeout_ms int4 NOT NULL, max_upload_size_mb int4 NOT NULL, administrator_email varchar(254) NOT NULL, email_alerts bool DEFAULT true NOT NULL, sms_alerts bool DEFAULT true NOT NULL, webhook_alerts bool DEFAULT false NOT NULL, ai_moderation_enabled bool DEFAULT true NOT NULL, maintenance_mode_enabled bool DEFAULT false NOT NULL, updated_by uuid NOT NULL, row_version int8 DEFAULT 0 NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT system_configurations_api_rate_limit_check CHECK (((api_rate_limit >= 1) AND (api_rate_limit <= 100000))), CONSTRAINT system_configurations_connection_timeout_ms_check CHECK (((connection_timeout_ms >= 1000) AND (connection_timeout_ms <= 300000))), CONSTRAINT system_configurations_max_upload_size_mb_check CHECK (((max_upload_size_mb >= 1) AND (max_upload_size_mb <= 1024))), CONSTRAINT system_configurations_pkey PRIMARY KEY (system_configuration_id));
COMMENT ON TABLE public.system_configurations IS 'Single active, SYSTEM_ADMIN-managed configuration record for platform operational settings.';

-- Permissions

ALTER TABLE public.system_configurations OWNER TO postgres;
GRANT ALL ON TABLE public.system_configurations TO postgres;


-- public.triage_session_evidence definition

-- Drop table

-- DROP TABLE public.triage_session_evidence;

CREATE TABLE public.triage_session_evidence ( evidence_id uuid DEFAULT gen_random_uuid() NOT NULL, triage_session_id uuid NOT NULL, evidence_type varchar(40) NOT NULL, claim_code varchar(100) NULL, claim_text text NOT NULL, knowledge_source_id uuid NULL, citation_url text NULL, citation_domain varchar(255) NULL, source_version varchar(80) NULL, source_snapshot_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, content_hash varchar(128) NOT NULL, created_at timestamptz DEFAULT now() NOT NULL, CONSTRAINT triage_session_evidence_pkey PRIMARY KEY (evidence_id), CONSTRAINT triage_session_evidence_uk UNIQUE (triage_session_id, evidence_type, content_hash));
CREATE INDEX triage_session_evidence_session_ix ON public.triage_session_evidence USING btree (triage_session_id);

-- Table Triggers

create trigger triage_session_evidence_immutable_trg before
delete
    or
update
    on
    public.triage_session_evidence for each row execute function carebridge_reject_mutation();

-- Permissions

ALTER TABLE public.triage_session_evidence OWNER TO postgres;
GRANT ALL ON TABLE public.triage_session_evidence TO postgres;


-- public.triage_sessions definition

-- Drop table

-- DROP TABLE public.triage_sessions;

CREATE TABLE public.triage_sessions ( triage_session_id uuid DEFAULT gen_random_uuid() NOT NULL, user_id uuid NOT NULL, care_subject_id uuid NULL, stage varchar(30) NULL, profile_context_id uuid NULL, risk_level varchar(20) NULL, status varchar(30) DEFAULT 'PENDING'::character varying NOT NULL, emergency bool DEFAULT false NOT NULL, disclaimer_version varchar(80) NULL, input_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, result_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, conversation_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, schema_version varchar(30) DEFAULT '1'::character varying NOT NULL, content_hash varchar(128) NULL, created_at timestamptz DEFAULT now() NOT NULL, completed_at timestamptz NULL, updated_at timestamptz DEFAULT now() NOT NULL, baby_profile_id uuid NULL, mother_profile_id uuid NULL, client_request_id varchar(64) NULL, symptoms text NOT NULL, raw_ai_response text NULL, disclaimer_text text NULL, created_by uuid NOT NULL, symptom_list jsonb NULL, duration_days int4 NULL, intensity varchar(20) NULL, emergency_flag bool NULL, extracted_at timestamptz NULL, structured_created_by varchar(50) NULL, CONSTRAINT triage_sessions_intensity_ck CHECK (((intensity IS NULL) OR ((intensity)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying])::text[])))), CONSTRAINT triage_sessions_pkey PRIMARY KEY (triage_session_id));
CREATE UNIQUE INDEX triage_sessions_owner_request_uk ON public.triage_sessions USING btree (user_id, client_request_id) WHERE (client_request_id IS NOT NULL);
CREATE INDEX triage_sessions_risk_ix ON public.triage_sessions USING btree (risk_level, emergency, created_at);
CREATE INDEX triage_sessions_stage_ix ON public.triage_sessions USING btree (stage, created_at);
CREATE INDEX triage_sessions_user_time_ix ON public.triage_sessions USING btree (user_id, created_at);

-- Permissions

ALTER TABLE public.triage_sessions OWNER TO postgres;
GRANT ALL ON TABLE public.triage_sessions TO postgres;


-- public.users definition

-- Drop table

-- DROP TABLE public.users;

CREATE TABLE public.users ( user_id uuid NOT NULL, avatar_url varchar(500) NULL, created_at timestamptz(6) NOT NULL, email varchar(255) NULL, full_name varchar(150) NULL, password_hash varchar(255) NULL, phone varchar(30) NULL, updated_at timestamptz(6) NOT NULL, enabled bool DEFAULT true NOT NULL, "locked" bool DEFAULT false NOT NULL, "role" varchar(50) NULL, failed_login_count int4 DEFAULT 0 NOT NULL, locked_at timestamptz NULL, email_verified bool DEFAULT false NOT NULL, phone_verified bool DEFAULT false NOT NULL, account_status varchar(30) NULL, last_login_at timestamptz NULL, suspended_until timestamptz NULL, must_change_password bool DEFAULT false NOT NULL, community_posting_restricted_until timestamptz NULL, settings_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL, display_name varchar(200) NULL, date_of_birth date NULL, phone_number varchar(40) NULL, area varchar(200) NULL, bio varchar(500) NULL, interest_stage varchar(30) NULL, is_visible bool NULL, public_avatar_url varchar(500) NULL, region varchar(120) NULL, social_identities jsonb DEFAULT '[]'::jsonb NOT NULL, professional_title varchar(150) NULL, workplace varchar(200) NULL, experience_years int2 NULL, consultation_scope text NULL, verification_status varchar(30) DEFAULT 'PENDING'::character varying NOT NULL, verified_at timestamptz NULL, verified_by uuid NULL, rating_avg numeric NULL, specialty_ids _uuid NULL, specialty varchar(100) NULL, facility_id uuid NULL, trust_status varchar(20) DEFAULT 'ACTIVE'::character varying NOT NULL, CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email), CONSTRAINT users_pkey PRIMARY KEY (user_id), CONSTRAINT users_role_check CHECK (((role IS NULL) OR ((role)::text = ANY ((ARRAY['MOTHER'::character varying, 'FAMILY'::character varying, 'EXPERT'::character varying, 'MODERATOR'::character varying, 'CONTENT_ADMIN'::character varying, 'SYSTEM_ADMIN'::character varying, 'PARTNER'::character varying])::text[])))));

-- Column comments

COMMENT ON COLUMN public.users."role" IS 'Canonical nullable Release 1 RBAC role: MOTHER, FAMILY, EXPERT, MODERATOR, CONTENT_ADMIN, SYSTEM_ADMIN, or PARTNER.';
COMMENT ON COLUMN public.users.suspended_until IS 'Moderation-driven time-bound account suspension (UC-102). NULL = not suspended. Non-null = suspended until this timestamp. Decoupled from locked/locked_at (security-domain brute-force lockout — see UC-102 TDS ADR-001) and from enabled (permanent account disable/deactivation).';
COMMENT ON COLUMN public.users.must_change_password IS 'UC115: true when an admin-issued temporary password has not yet been rotated by the staff member.';
COMMENT ON COLUMN public.users.community_posting_restricted_until IS 'Until this time, the user may read community content but may not create questions or answers.';

-- Permissions

ALTER TABLE public.users OWNER TO postgres;
GRANT ALL ON TABLE public.users TO postgres;


-- public.vaccination_records definition

-- Drop table

-- DROP TABLE public.vaccination_records;

CREATE TABLE public.vaccination_records ( vaccination_record_id uuid DEFAULT gen_random_uuid() NOT NULL, baby_id uuid NOT NULL, vaccine_name varchar(200) NOT NULL, dose_number int2 NULL, scheduled_date date NULL, administered_date date NULL, status varchar(20) DEFAULT 'SCHEDULED'::character varying NOT NULL, facility_name varchar(200) NULL, proof_record_id uuid NULL, created_at timestamptz DEFAULT now() NOT NULL, updated_at timestamptz DEFAULT now() NOT NULL, postpone_reason text NULL, care_subject_id uuid NOT NULL, vaccination_schedule_id uuid NULL, CONSTRAINT vaccination_records_pkey PRIMARY KEY (vaccination_record_id));
CREATE INDEX idx_vaccination_records_baby_id ON public.vaccination_records USING btree (baby_id);
CREATE INDEX idx_vaccination_records_status ON public.vaccination_records USING btree (status);
CREATE INDEX vaccination_records_subject_status_ix ON public.vaccination_records USING btree (care_subject_id, status, scheduled_date);

-- Permissions

ALTER TABLE public.vaccination_records OWNER TO postgres;
GRANT ALL ON TABLE public.vaccination_records TO postgres;


-- public.archived_records foreign keys

ALTER TABLE public.archived_records ADD CONSTRAINT archived_consultation_availability_fk FOREIGN KEY (availability_id) REFERENCES public.expert_availability(availability_id);
ALTER TABLE public.archived_records ADD CONSTRAINT archived_consultation_booking_fk FOREIGN KEY (booking_id) REFERENCES public.archived_records(archive_id);
ALTER TABLE public.archived_records ADD CONSTRAINT archived_consultation_configured_by_fk FOREIGN KEY (configured_by) REFERENCES public.users(user_id);
ALTER TABLE public.archived_records ADD CONSTRAINT archived_consultation_expert_price_fk FOREIGN KEY (expert_price_id) REFERENCES public.archived_records(archive_id);
ALTER TABLE public.archived_records ADD CONSTRAINT archived_consultation_price_band_fk FOREIGN KEY (price_band_id) REFERENCES public.archived_records(archive_id);
ALTER TABLE public.archived_records ADD CONSTRAINT archived_consultation_requester_fk FOREIGN KEY (requester_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.archived_records ADD CONSTRAINT archived_partner_representative_fk FOREIGN KEY (representative_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.archived_records ADD CONSTRAINT archived_realtime_conversation_fk FOREIGN KEY (conversation_id) REFERENCES public.archived_records(archive_id);
ALTER TABLE public.archived_records ADD CONSTRAINT archived_realtime_expert_fk FOREIGN KEY (expert_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.archived_records ADD CONSTRAINT archived_realtime_initiator_fk FOREIGN KEY (initiated_by_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.archived_records ADD CONSTRAINT archived_realtime_mother_fk FOREIGN KEY (mother_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.archived_records ADD CONSTRAINT archived_realtime_sender_fk FOREIGN KEY (sender_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.archived_records ADD CONSTRAINT archived_records_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


-- public.attachments foreign keys

ALTER TABLE public.attachments ADD CONSTRAINT attachments_health_record_id_fkey FOREIGN KEY (health_record_id) REFERENCES public.health_records(health_record_id);
ALTER TABLE public.attachments ADD CONSTRAINT attachments_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


-- public.audit_events foreign keys

ALTER TABLE public.audit_events ADD CONSTRAINT audit_events_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.audit_events ADD CONSTRAINT audit_events_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.users(user_id);
ALTER TABLE public.audit_events ADD CONSTRAINT audit_events_security_event_fk FOREIGN KEY (security_event_id) REFERENCES public.audit_events(audit_event_id);
ALTER TABLE public.audit_events ADD CONSTRAINT audit_events_subject_user_id_fkey FOREIGN KEY (subject_user_id) REFERENCES public.users(user_id);


-- public.auth_challenges foreign keys

ALTER TABLE public.auth_challenges ADD CONSTRAINT auth_challenges_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


-- public.auth_sessions foreign keys

ALTER TABLE public.auth_sessions ADD CONSTRAINT auth_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


-- public.care_facilities foreign keys

ALTER TABLE public.care_facilities ADD CONSTRAINT care_facilities_partner_archive_fk FOREIGN KEY (partner_id) REFERENCES public.archived_records(archive_id);


-- public.care_group_members foreign keys

ALTER TABLE public.care_group_members ADD CONSTRAINT care_group_members_care_group_id_fkey FOREIGN KEY (care_group_id) REFERENCES public.care_groups(care_group_id);
ALTER TABLE public.care_group_members ADD CONSTRAINT care_group_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


-- public.care_groups foreign keys

ALTER TABLE public.care_groups ADD CONSTRAINT care_groups_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.care_subjects(care_subject_id);
ALTER TABLE public.care_groups ADD CONSTRAINT care_groups_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(journey_id);
ALTER TABLE public.care_groups ADD CONSTRAINT care_groups_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


-- public.care_logs foreign keys

ALTER TABLE public.care_logs ADD CONSTRAINT care_logs_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);
ALTER TABLE public.care_logs ADD CONSTRAINT care_logs_recorded_by_fkey FOREIGN KEY (recorded_by) REFERENCES public.users(user_id);


-- public.care_subjects foreign keys

ALTER TABLE public.care_subjects ADD CONSTRAINT care_subjects_journey_fk FOREIGN KEY (mother_journey_id) REFERENCES public.mother_journeys(journey_id);
ALTER TABLE public.care_subjects ADD CONSTRAINT care_subjects_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.care_subjects ADD CONSTRAINT care_subjects_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.users(user_id);


-- public.care_tasks foreign keys

ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_assignee_user_id_fkey FOREIGN KEY (assignee_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_care_group_id_fkey1 FOREIGN KEY (care_group_id) REFERENCES public.care_groups(care_group_id);
ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);
ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_creator_user_id_fkey FOREIGN KEY (creator_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.care_tasks ADD CONSTRAINT care_tasks_vaccination_record_id_fkey FOREIGN KEY (vaccination_record_id) REFERENCES public.vaccination_records(vaccination_record_id);


-- public.community_content foreign keys

ALTER TABLE public.community_content ADD CONSTRAINT community_content_author_user_id_fkey FOREIGN KEY (author_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.community_content ADD CONSTRAINT community_content_parent_content_id_fkey FOREIGN KEY (parent_content_id) REFERENCES public.community_content(content_id);
ALTER TABLE public.community_content ADD CONSTRAINT community_content_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES public.community_topics(id);


-- public.community_interactions foreign keys

ALTER TABLE public.community_interactions ADD CONSTRAINT community_interactions_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.community_interactions ADD CONSTRAINT community_interactions_content_id_fkey FOREIGN KEY (content_id) REFERENCES public.community_content(content_id);
ALTER TABLE public.community_interactions ADD CONSTRAINT community_interactions_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES public.community_topics(id);


-- public.content_item_sources foreign keys

ALTER TABLE public.content_item_sources ADD CONSTRAINT content_item_sources_content_item_id_fkey FOREIGN KEY (content_item_id) REFERENCES public.content_items(content_item_id);
ALTER TABLE public.content_item_sources ADD CONSTRAINT content_item_sources_knowledge_source_id_fkey FOREIGN KEY (knowledge_source_id) REFERENCES public.knowledge_sources(knowledge_source_id);


-- public.development_milestones foreign keys

ALTER TABLE public.development_milestones ADD CONSTRAINT development_milestones_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.care_subjects(care_subject_id);
ALTER TABLE public.development_milestones ADD CONSTRAINT development_milestones_recorded_by_fkey FOREIGN KEY (recorded_by) REFERENCES public.users(user_id);
ALTER TABLE public.development_milestones ADD CONSTRAINT development_milestones_subject_fk FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


-- public.device_connections foreign keys

ALTER TABLE public.device_connections ADD CONSTRAINT device_connections_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


-- public.device_tokens foreign keys

ALTER TABLE public.device_tokens ADD CONSTRAINT device_tokens_user_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


-- public.expense_entries foreign keys

ALTER TABLE public.expense_entries ADD CONSTRAINT expense_entries_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);
ALTER TABLE public.expense_entries ADD CONSTRAINT expense_entries_mother_journey_id_fkey FOREIGN KEY (mother_journey_id) REFERENCES public.mother_journeys(journey_id);
ALTER TABLE public.expense_entries ADD CONSTRAINT expense_entries_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


-- public.expert_credentials foreign keys

ALTER TABLE public.expert_credentials ADD CONSTRAINT fk_expert_credentials_file FOREIGN KEY (file_id) REFERENCES public.attachments(attachment_id);
ALTER TABLE public.expert_credentials ADD CONSTRAINT fk_expert_credentials_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES public.users(user_id) ON DELETE SET NULL;


-- public.growth_measurements foreign keys

ALTER TABLE public.growth_measurements ADD CONSTRAINT growth_measurements_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.care_subjects(care_subject_id);
ALTER TABLE public.growth_measurements ADD CONSTRAINT growth_measurements_subject_fk FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


-- public.health_context_memories foreign keys

ALTER TABLE public.health_context_memories ADD CONSTRAINT health_context_memories_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);
ALTER TABLE public.health_context_memories ADD CONSTRAINT health_context_memories_triage_session_id_fkey FOREIGN KEY (triage_session_id) REFERENCES public.triage_sessions(triage_session_id);
ALTER TABLE public.health_context_memories ADD CONSTRAINT health_context_memories_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


-- public.health_observations foreign keys

ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);
ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_device_fk FOREIGN KEY (device_connection_id) REFERENCES public.device_connections(device_connection_id);
ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_session_fk FOREIGN KEY (source_record_id) REFERENCES public.maternal_exercise_sessions(exercise_session_id);


-- public.health_records foreign keys

ALTER TABLE public.health_records ADD CONSTRAINT health_records_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.care_subjects(care_subject_id);
ALTER TABLE public.health_records ADD CONSTRAINT health_records_journey_id_fkey FOREIGN KEY (journey_id) REFERENCES public.mother_journeys(journey_id);
ALTER TABLE public.health_records ADD CONSTRAINT health_records_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);


-- public.knowledge_source_reviews foreign keys

ALTER TABLE public.knowledge_source_reviews ADD CONSTRAINT knowledge_source_reviews_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.knowledge_source_reviews ADD CONSTRAINT knowledge_source_reviews_knowledge_source_id_fkey FOREIGN KEY (knowledge_source_id) REFERENCES public.knowledge_sources(knowledge_source_id);


-- public.knowledge_sources foreign keys

ALTER TABLE public.knowledge_sources ADD CONSTRAINT knowledge_sources_added_by_fkey FOREIGN KEY (added_by) REFERENCES public.users(user_id);
ALTER TABLE public.knowledge_sources ADD CONSTRAINT knowledge_sources_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.users(user_id);


-- public.maternal_exercise_sessions foreign keys

ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_mother_journey_id_fkey FOREIGN KEY (mother_journey_id) REFERENCES public.mother_journeys(journey_id);
ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_posture_config_fk FOREIGN KEY (posture_config_id) REFERENCES public.care_item_templates(template_id);
ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_safety_fk FOREIGN KEY (safety_observation_id) REFERENCES public.health_observations(health_observation_id);
ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_template_fk FOREIGN KEY (exercise_template_id) REFERENCES public.care_item_templates(template_id);


-- public.moderation_cases foreign keys

ALTER TABLE public.moderation_cases ADD CONSTRAINT moderation_cases_assigned_moderator_id_fkey FOREIGN KEY (assigned_moderator_id) REFERENCES public.users(user_id);
ALTER TABLE public.moderation_cases ADD CONSTRAINT moderation_cases_reporter_user_id_fkey FOREIGN KEY (reporter_user_id) REFERENCES public.users(user_id);


-- public.mother_journeys foreign keys

ALTER TABLE public.mother_journeys ADD CONSTRAINT mother_journeys_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.mother_journeys ADD CONSTRAINT mother_journeys_subject_fk FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);


-- public.nearby_support_interactions foreign keys

ALTER TABLE public.nearby_support_interactions ADD CONSTRAINT nearby_support_interactions_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);
ALTER TABLE public.nearby_support_interactions ADD CONSTRAINT nearby_support_interactions_parent_interaction_id_fkey FOREIGN KEY (parent_interaction_id) REFERENCES public.nearby_support_interactions(interaction_id);
ALTER TABLE public.nearby_support_interactions ADD CONSTRAINT nearby_support_interactions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


-- public.notification_records foreign keys

ALTER TABLE public.notification_records ADD CONSTRAINT fk_notification_records_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


-- public.preparation_checklist_items foreign keys

ALTER TABLE public.preparation_checklist_items ADD CONSTRAINT preparation_checklist_items_mother_journey_id_fkey FOREIGN KEY (mother_journey_id) REFERENCES public.mother_journeys(journey_id);
ALTER TABLE public.preparation_checklist_items ADD CONSTRAINT preparation_checklist_items_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.preparation_checklist_items ADD CONSTRAINT preparation_checklist_items_template_entry_id_fkey FOREIGN KEY (template_entry_id) REFERENCES public.care_item_templates(template_id);


-- public.red_flag_rules foreign keys

ALTER TABLE public.red_flag_rules ADD CONSTRAINT fk_red_flag_rules_created_by FOREIGN KEY (created_by) REFERENCES public.users(user_id);
ALTER TABLE public.red_flag_rules ADD CONSTRAINT fk_red_flag_rules_updated_by FOREIGN KEY (updated_by) REFERENCES public.users(user_id);


-- public.safety_configs foreign keys

ALTER TABLE public.safety_configs ADD CONSTRAINT safety_configs_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users(user_id);
ALTER TABLE public.safety_configs ADD CONSTRAINT safety_configs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


-- public.safety_events foreign keys

ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_care_facility_id_fkey FOREIGN KEY (care_facility_id) REFERENCES public.care_facilities(facility_id);
ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);
ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_created_by_user_fk FOREIGN KEY (created_by_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_device_token_fk FOREIGN KEY (device_token_id) REFERENCES public.device_tokens(id);
ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_emergency_session_fk FOREIGN KEY (emergency_session_id) REFERENCES public.safety_events(safety_event_id);
ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_monitoring_session_id_fkey FOREIGN KEY (monitoring_session_id) REFERENCES public.safety_monitoring_sessions(monitoring_session_id);
ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_notification_record_id_fkey FOREIGN KEY (notification_record_id) REFERENCES public.notification_records(id);
ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_owner_fk FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_parent_event_id_fkey FOREIGN KEY (parent_event_id) REFERENCES public.safety_events(safety_event_id);
ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_recipient_user_id_fkey FOREIGN KEY (recipient_user_id) REFERENCES public.users(user_id);
ALTER TABLE public.safety_events ADD CONSTRAINT safety_events_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


-- public.safety_monitoring_sessions foreign keys

ALTER TABLE public.safety_monitoring_sessions ADD CONSTRAINT safety_monitoring_sessions_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(user_id);
ALTER TABLE public.safety_monitoring_sessions ADD CONSTRAINT safety_monitoring_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


-- public.system_configurations foreign keys

ALTER TABLE public.system_configurations ADD CONSTRAINT system_configurations_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users(user_id);


-- public.triage_session_evidence foreign keys

ALTER TABLE public.triage_session_evidence ADD CONSTRAINT triage_session_evidence_source_fk FOREIGN KEY (knowledge_source_id) REFERENCES public.knowledge_sources(knowledge_source_id);
ALTER TABLE public.triage_session_evidence ADD CONSTRAINT triage_session_evidence_triage_session_id_fkey FOREIGN KEY (triage_session_id) REFERENCES public.triage_sessions(triage_session_id);


-- public.triage_sessions foreign keys

ALTER TABLE public.triage_sessions ADD CONSTRAINT triage_sessions_care_subject_id_fkey FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);
ALTER TABLE public.triage_sessions ADD CONSTRAINT triage_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id);


-- public.users foreign keys

ALTER TABLE public.users ADD CONSTRAINT users_facility_id_fkey FOREIGN KEY (facility_id) REFERENCES public.care_facilities(facility_id);
ALTER TABLE public.users ADD CONSTRAINT users_verified_by_fkey FOREIGN KEY (verified_by) REFERENCES public.users(user_id);


-- public.vaccination_records foreign keys

ALTER TABLE public.vaccination_records ADD CONSTRAINT vaccination_records_baby_id_fkey FOREIGN KEY (baby_id) REFERENCES public.care_subjects(care_subject_id);
ALTER TABLE public.vaccination_records ADD CONSTRAINT vaccination_records_proof_record_id_fkey FOREIGN KEY (proof_record_id) REFERENCES public.health_records(health_record_id);
ALTER TABLE public.vaccination_records ADD CONSTRAINT vaccination_records_schedule_fk FOREIGN KEY (vaccination_schedule_id) REFERENCES public.vaccination_schedules(vaccination_schedule_id);
ALTER TABLE public.vaccination_records ADD CONSTRAINT vaccination_records_subject_fk FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);



-- DROP FUNCTION public.carebridge_reject_mutation();

CREATE OR REPLACE FUNCTION public.carebridge_reject_mutation()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    RAISE EXCEPTION 'IMMUTABLE_TABLE: %.% does not allow UPDATE or DELETE', TG_TABLE_SCHEMA, TG_TABLE_NAME;
END
$function$
;

-- Permissions

ALTER FUNCTION public.carebridge_reject_mutation() OWNER TO postgres;
GRANT ALL ON FUNCTION public.carebridge_reject_mutation() TO postgres;

-- DROP FUNCTION public.enforce_community_topic_parent_category();

CREATE OR REPLACE FUNCTION public.enforce_community_topic_parent_category()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
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
$function$
;

-- Permissions

ALTER FUNCTION public.enforce_community_topic_parent_category() OWNER TO postgres;
GRANT ALL ON FUNCTION public.enforce_community_topic_parent_category() TO postgres;

-- DROP FUNCTION public.enforce_pregnancy_outcome_evidence_owner();

CREATE OR REPLACE FUNCTION public.enforce_pregnancy_outcome_evidence_owner()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
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
$function$
;

-- Permissions

ALTER FUNCTION public.enforce_pregnancy_outcome_evidence_owner() OWNER TO postgres;
GRANT ALL ON FUNCTION public.enforce_pregnancy_outcome_evidence_owner() TO postgres;

-- DROP FUNCTION public.reject_mother_journey_transition_mutation();

CREATE OR REPLACE FUNCTION public.reject_mother_journey_transition_mutation()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    IF current_setting('carebridge.allow_transition_mutation', true) = 'on' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'mother_journey_transitions is append-only';
END;
$function$
;

-- Permissions

ALTER FUNCTION public.reject_mother_journey_transition_mutation() OWNER TO postgres;
GRANT ALL ON FUNCTION public.reject_mother_journey_transition_mutation() TO postgres;

-- DROP FUNCTION public.reject_pregnancy_outcome_evidence_mutation();

CREATE OR REPLACE FUNCTION public.reject_pregnancy_outcome_evidence_mutation()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    IF current_setting('carebridge.allow_outcome_mutation', true) = 'on' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'pregnancy outcome evidence is append-only';
END;
$function$
;

-- Permissions

ALTER FUNCTION public.reject_pregnancy_outcome_evidence_mutation() OWNER TO postgres;
GRANT ALL ON FUNCTION public.reject_pregnancy_outcome_evidence_mutation() TO postgres;


-- Permissions

GRANT ALL ON SCHEMA public TO postgres;
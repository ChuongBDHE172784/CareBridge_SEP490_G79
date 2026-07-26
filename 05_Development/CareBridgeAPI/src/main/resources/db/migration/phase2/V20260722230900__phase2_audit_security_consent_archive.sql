-- Phase 2 wave 9: audit/security/consent plus domain-specific archives.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

ALTER TABLE public.data_permissions
    ADD COLUMN IF NOT EXISTS permission_series_id uuid,
    ADD COLUMN IF NOT EXISTS version_number integer,
    ADD COLUMN IF NOT EXISTS supersedes_permission_id uuid,
    ADD COLUMN IF NOT EXISTS revoked_by uuid,
    ADD COLUMN IF NOT EXISTS policy_version varchar(80),
    ADD COLUMN IF NOT EXISTS consent_evidence_key varchar(255);

CREATE TABLE IF NOT EXISTS public.audit_events (
    audit_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id uuid REFERENCES public.users(user_id),
    event_category varchar(80) NOT NULL,
    subject_user_id uuid REFERENCES public.users(user_id),
    subject_reference_id uuid,
    resource_type varchar(100),
    resource_id uuid,
    purpose varchar(255),
    decision varchar(50),
    ip_hash varchar(128),
    before_payload_jsonb jsonb,
    after_payload_jsonb jsonb,
    checksum varchar(128),
    occurred_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS audit_events_subject_time_ix ON public.audit_events(subject_user_id, occurred_at);
CREATE INDEX IF NOT EXISTS audit_events_category_time_ix ON public.audit_events(event_category, occurred_at);

CREATE TABLE IF NOT EXISTS public.expense_entries (
    expense_entry_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id uuid NOT NULL REFERENCES public.users(user_id),
    care_subject_id uuid REFERENCES public.care_subjects(care_subject_id),
    mother_journey_id uuid REFERENCES public.mother_journeys(journey_id),
    category varchar(80),
    amount numeric NOT NULL,
    currency varchar(10) NOT NULL DEFAULT 'VND',
    expense_date date NOT NULL,
    note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS expense_entries_owner_date_ix ON public.expense_entries(owner_user_id, expense_date);

CREATE TABLE IF NOT EXISTS public.archived_consultation_records (
    archive_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    legacy_table varchar(150) NOT NULL,
    legacy_id varchar(150) NOT NULL,
    owner_user_id uuid REFERENCES public.users(user_id),
    payload_jsonb jsonb NOT NULL,
    original_created_at timestamptz,
    archived_at timestamptz NOT NULL DEFAULT now(),
    retention_until timestamptz,
    archive_reason varchar(255) NOT NULL,
    source_schema_version varchar(80),
    checksum varchar(128) NOT NULL,
    CONSTRAINT archived_consultation_records_source_uk UNIQUE (legacy_table, legacy_id)
);

CREATE TABLE IF NOT EXISTS public.archived_realtime_records (
    archive_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    legacy_table varchar(150) NOT NULL,
    legacy_id varchar(150) NOT NULL,
    owner_user_id uuid REFERENCES public.users(user_id),
    payload_jsonb jsonb NOT NULL,
    original_created_at timestamptz,
    archived_at timestamptz NOT NULL DEFAULT now(),
    retention_until timestamptz,
    archive_reason varchar(255) NOT NULL,
    source_schema_version varchar(80),
    checksum varchar(128) NOT NULL,
    CONSTRAINT archived_realtime_records_source_uk UNIQUE (legacy_table, legacy_id)
);

CREATE TABLE IF NOT EXISTS public.archived_partner_records (
    archive_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    legacy_table varchar(150) NOT NULL,
    legacy_id varchar(150) NOT NULL,
    owner_user_id uuid REFERENCES public.users(user_id),
    payload_jsonb jsonb NOT NULL,
    original_created_at timestamptz,
    archived_at timestamptz NOT NULL DEFAULT now(),
    retention_until timestamptz,
    archive_reason varchar(255) NOT NULL,
    source_schema_version varchar(80),
    checksum varchar(128) NOT NULL,
    CONSTRAINT archived_partner_records_source_uk UNIQUE (legacy_table, legacy_id)
);

DO $audit_mapping$
BEGIN
    IF to_regclass('public.audit_logs') IS NOT NULL THEN
        INSERT INTO public.audit_events
            (audit_event_id, actor_user_id, event_category, subject_reference_id,
             resource_type, resource_id, before_payload_jsonb, after_payload_jsonb, occurred_at, created_at)
        SELECT a.audit_log_id, a.actor_user_id, a.action, a.entity_id, a.entity_type,
               a.entity_id, a.old_value_json, a.new_value_json, a.created_at, a.created_at
          FROM public.audit_logs a
        ON CONFLICT (audit_event_id) DO NOTHING;
    END IF;
    IF to_regclass('public.expenses') IS NOT NULL THEN
        INSERT INTO public.expense_entries
            (expense_entry_id, owner_user_id, mother_journey_id, category, amount, currency,
             expense_date, note, created_at, updated_at)
        SELECT e.expense_id, e.owner_user_id, e.journey_id, e.category, e.amount, e.currency,
               e.expense_date, e.note, e.created_at, e.updated_at
          FROM public.expenses e
        ON CONFLICT (expense_entry_id) DO NOTHING;
    END IF;
END
$audit_mapping$;

DO $archive_mapping$
DECLARE
    source_name text;
    source_id_column text;
    archive_target text;
BEGIN
    FOREACH source_name IN ARRAY ARRAY['consultation_bookings','consultation_price_bands','consultation_sessions','expert_consultation_prices'] LOOP
        IF to_regclass('public.' || source_name) IS NOT NULL THEN
            source_id_column := CASE WHEN source_name = 'consultation_bookings' THEN 'booking_id'
                                     WHEN source_name = 'consultation_price_bands' THEN 'price_band_id'
                                     WHEN source_name = 'consultation_sessions' THEN 'session_id'
                                     ELSE 'expert_price_id' END;
            EXECUTE format(
                'INSERT INTO public.archived_consultation_records (legacy_table, legacy_id, payload_jsonb, archive_reason, checksum) SELECT %L, s.%I::text, to_jsonb(s), %L, md5(to_jsonb(s)::text) FROM public.%I s ON CONFLICT DO NOTHING',
                source_name, source_id_column, 'PHASE2_DOMAIN_ARCHIVE', source_name);
        END IF;
    END LOOP;
    FOREACH source_name IN ARRAY ARRAY['direct_conversations','direct_messages','conversation_calls'] LOOP
        IF to_regclass('public.' || source_name) IS NOT NULL THEN
            source_id_column := CASE WHEN source_name = 'direct_conversations' THEN 'conversation_id'
                                     WHEN source_name = 'direct_messages' THEN 'message_id'
                                     ELSE 'call_id' END;
            EXECUTE format(
                'INSERT INTO public.archived_realtime_records (legacy_table, legacy_id, payload_jsonb, archive_reason, checksum) SELECT %L, s.%I::text, to_jsonb(s), %L, md5(to_jsonb(s)::text) FROM public.%I s ON CONFLICT DO NOTHING',
                source_name, source_id_column, 'PHASE2_DOMAIN_ARCHIVE', source_name);
        END IF;
    END LOOP;
    IF to_regclass('public.partner_organizations') IS NOT NULL THEN
        EXECUTE 'INSERT INTO public.archived_partner_records (legacy_table, legacy_id, payload_jsonb, archive_reason, checksum) SELECT ''partner_organizations'', s.partner_id::text, to_jsonb(s), ''PHASE2_DOMAIN_ARCHIVE'', md5(to_jsonb(s)::text) FROM public.partner_organizations s ON CONFLICT DO NOTHING';
    END IF;
END
$archive_mapping$;

DO $permission_constraints$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'data_permissions_supersedes_fk') THEN
        ALTER TABLE public.data_permissions ADD CONSTRAINT data_permissions_supersedes_fk
            FOREIGN KEY (supersedes_permission_id) REFERENCES public.data_permissions(permission_id);
    END IF;
END
$permission_constraints$;

DROP TRIGGER IF EXISTS audit_events_immutable_trg ON public.audit_events;
CREATE TRIGGER audit_events_immutable_trg
BEFORE UPDATE OR DELETE ON public.audit_events
FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();

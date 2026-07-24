-- Phase 2 wave 5: health records, files and device observations.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

ALTER TABLE public.health_records ADD COLUMN IF NOT EXISTS care_subject_id uuid;

CREATE TABLE IF NOT EXISTS public.attachments (
    attachment_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id uuid NOT NULL REFERENCES public.users(user_id),
    storage_key varchar(500) NOT NULL UNIQUE,
    original_name varchar(255) NOT NULL,
    mime_type varchar(100) NOT NULL,
    file_size_bytes bigint NOT NULL CHECK (file_size_bytes >= 0),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    checksum varchar(128),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.health_record_attachments (
    health_record_attachment_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    health_record_id uuid NOT NULL REFERENCES public.health_records(health_record_id),
    attachment_id uuid NOT NULL REFERENCES public.attachments(attachment_id),
    display_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT health_record_attachments_pair_uk UNIQUE (health_record_id, attachment_id)
);
CREATE INDEX IF NOT EXISTS health_record_attachments_record_ix ON public.health_record_attachments(health_record_id, display_order);

CREATE TABLE IF NOT EXISTS public.device_connections (
    device_connection_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.users(user_id),
    provider_name varchar(80) NOT NULL,
    device_name varchar(150),
    scopes_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    token_reference text,
    consent_granted_at timestamptz,
    last_synced_at timestamptz,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS device_connections_user_status_ix ON public.device_connections(user_id, status);

CREATE TABLE IF NOT EXISTS public.health_observations (
    health_observation_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    device_connection_id uuid REFERENCES public.device_connections(device_connection_id),
    care_subject_id uuid REFERENCES public.care_subjects(care_subject_id),
    observation_type varchar(50) NOT NULL,
    value_numeric numeric,
    value_secondary numeric,
    unit varchar(30),
    observed_at timestamptz NOT NULL,
    source_record_id uuid,
    quality_label varchar(30),
    raw_payload_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS health_observations_subject_chart_ix
    ON public.health_observations(care_subject_id, observation_type, observed_at);
CREATE INDEX IF NOT EXISTS health_observations_device_time_ix
    ON public.health_observations(device_connection_id, observed_at);

DO $health_mapping$
BEGIN
    IF to_regclass('public.uploaded_files') IS NOT NULL THEN
        INSERT INTO public.attachments
            (attachment_id, owner_user_id, storage_key, original_name, mime_type,
             file_size_bytes, status, created_at, updated_at)
        SELECT u.file_id, u.owner_user_id, u.storage_key, u.original_name, u.mime_type,
               u.file_size_bytes, u.status, u.created_at, u.updated_at
          FROM public.uploaded_files u
        ON CONFLICT (attachment_id) DO NOTHING;
    END IF;
    IF to_regclass('public.health_record_files') IS NOT NULL THEN
        INSERT INTO public.health_record_attachments
            (health_record_attachment_id, health_record_id, attachment_id, display_order, created_at)
        SELECT f.id, f.health_record_id, f.file_id, f.display_order, f.created_at
          FROM public.health_record_files f
         WHERE EXISTS (SELECT 1 FROM public.attachments a WHERE a.attachment_id = f.file_id)
        ON CONFLICT (health_record_attachment_id) DO NOTHING;
    END IF;
    IF to_regclass('public.health_device_connections') IS NOT NULL THEN
        INSERT INTO public.device_connections
            (device_connection_id, user_id, provider_name, device_name, scopes_jsonb,
             token_reference, consent_granted_at, last_synced_at, status, created_at, updated_at)
        SELECT h.connection_id, h.user_id, h.provider_name, h.device_name,
               coalesce(h.scopes_json, '{}'::jsonb), h.token_reference,
               h.consent_granted_at, h.last_synced_at, h.status, h.created_at, h.updated_at
          FROM public.health_device_connections h
        ON CONFLICT (device_connection_id) DO NOTHING;
    END IF;
    IF to_regclass('public.device_measurements') IS NOT NULL THEN
        INSERT INTO public.health_observations
            (health_observation_id, device_connection_id, observation_type, value_numeric,
             value_secondary, unit, observed_at, source_record_id, quality_label,
             raw_payload_jsonb, created_at, updated_at)
        SELECT d.device_measurement_id, d.connection_id, d.measurement_type,
               d.value_numeric, d.value_secondary, d.unit, d.measured_at,
               d.source_record_id, d.quality_label, coalesce(d.raw_metadata_json, '{}'::jsonb),
               d.created_at, d.updated_at
          FROM public.device_measurements d
        ON CONFLICT (health_observation_id) DO NOTHING;
    END IF;
    IF to_regclass('public.health_summaries') IS NOT NULL THEN
        INSERT INTO public.health_records
            (health_record_id, owner_user_id, journey_id, baby_id, record_type, title,
             record_date, source_type, source_name, status, created_at, updated_at)
        SELECT s.summary_id, s.owner_user_id, s.journey_id, s.baby_id, 'SUMMARY',
               'Health summary', s.period_end, 'SUMMARY', s.generated_by, s.status,
               s.created_at, s.updated_at
          FROM public.health_summaries s
        ON CONFLICT (health_record_id) DO NOTHING;
    END IF;
END
$health_mapping$;

UPDATE public.health_records hr
   SET care_subject_id = cs.care_subject_id
  FROM public.care_subjects cs
 WHERE hr.care_subject_id IS NULL
   AND hr.baby_id = cs.care_subject_id;

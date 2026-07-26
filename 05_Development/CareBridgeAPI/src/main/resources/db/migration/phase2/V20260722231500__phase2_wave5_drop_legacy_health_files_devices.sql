-- Phase 2 wave 5 cutover: files, record attachments, device observations, and summaries.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

ALTER TABLE public.health_records
  ADD COLUMN IF NOT EXISTS summary_period varchar(30),
  ADD COLUMN IF NOT EXISTS period_start date,
  ADD COLUMN IF NOT EXISTS summary_json jsonb;

INSERT INTO public.attachments (
    attachment_id,owner_user_id,storage_key,original_name,mime_type,file_size_bytes,
    status,created_at,updated_at)
SELECT file_id,owner_user_id,storage_key,original_name,mime_type,file_size_bytes,
       status,created_at,updated_at
  FROM public.uploaded_files
ON CONFLICT (attachment_id) DO UPDATE SET
  owner_user_id=excluded.owner_user_id,storage_key=excluded.storage_key,
  original_name=excluded.original_name,mime_type=excluded.mime_type,
  file_size_bytes=excluded.file_size_bytes,status=excluded.status,
  updated_at=excluded.updated_at;

UPDATE public.attachments a
   SET health_record_id = f.health_record_id
  FROM public.health_record_files f
 WHERE a.attachment_id = f.file_id;

INSERT INTO public.device_connections (
    device_connection_id,user_id,provider_name,device_name,scopes_jsonb,token_reference,
    consent_granted_at,last_synced_at,status,created_at,updated_at)
SELECT connection_id,user_id,provider_name,device_name,coalesce(scopes_json,'{}'::jsonb),
       token_reference,consent_granted_at,last_synced_at,status,created_at,updated_at
  FROM public.health_device_connections
ON CONFLICT (device_connection_id) DO UPDATE SET
  user_id=excluded.user_id,provider_name=excluded.provider_name,device_name=excluded.device_name,
  scopes_jsonb=excluded.scopes_jsonb,token_reference=excluded.token_reference,
  consent_granted_at=excluded.consent_granted_at,last_synced_at=excluded.last_synced_at,
  status=excluded.status,updated_at=excluded.updated_at;

INSERT INTO public.health_observations (
    health_observation_id,device_connection_id,care_subject_id,subject_type,observation_type,value_numeric,
    value_secondary,unit,observed_at,source_record_id,quality_label,
    raw_payload_jsonb,created_at,updated_at)
SELECT d.device_measurement_id,d.connection_id,cs.care_subject_id,'MOTHER',d.measurement_type,d.value_numeric,d.value_secondary,
       d.unit,d.measured_at,d.source_record_id,d.quality_label,coalesce(d.raw_metadata_json,'{}'::jsonb),
       d.created_at,d.updated_at
  FROM public.device_measurements d
  JOIN public.device_connections dc ON dc.device_connection_id = d.connection_id
  JOIN public.care_subjects cs ON cs.person_id = dc.user_id AND cs.subject_type = 'MOTHER'
ON CONFLICT (health_observation_id) DO UPDATE SET
  device_connection_id=excluded.device_connection_id,
  care_subject_id=excluded.care_subject_id,
  subject_type=excluded.subject_type,
  observation_type=excluded.observation_type,value_numeric=excluded.value_numeric,
  value_secondary=excluded.value_secondary,unit=excluded.unit,observed_at=excluded.observed_at,
  source_record_id=excluded.source_record_id,quality_label=excluded.quality_label,
  raw_payload_jsonb=excluded.raw_payload_jsonb,updated_at=excluded.updated_at;

INSERT INTO public.health_records (
    health_record_id,owner_user_id,journey_id,baby_id,record_type,title,record_date,
    source_type,source_name,status,summary_period,period_start,summary_json,created_at,updated_at)
SELECT summary_id,owner_user_id,journey_id,baby_id,'SUMMARY','Health summary',period_end,
       'SUMMARY',generated_by,status,summary_period,period_start,summary_json,created_at,updated_at
  FROM public.health_summaries
ON CONFLICT (health_record_id) DO UPDATE SET
  owner_user_id=excluded.owner_user_id,journey_id=excluded.journey_id,baby_id=excluded.baby_id,
  record_type='SUMMARY',title=excluded.title,record_date=excluded.record_date,
  source_type='SUMMARY',source_name=excluded.source_name,status=excluded.status,
  summary_period=excluded.summary_period,period_start=excluded.period_start,
  summary_json=excluded.summary_json,updated_at=excluded.updated_at;

UPDATE public.health_records hr
   SET care_subject_id=coalesce(hr.baby_id,hr.journey_id)
 WHERE hr.care_subject_id IS NULL
   AND EXISTS (SELECT 1 FROM public.care_subjects cs
                WHERE cs.care_subject_id=coalesce(hr.baby_id,hr.journey_id));

DO $wave5_reconcile$
BEGIN
  IF (SELECT count(*) FROM public.uploaded_files) <>
     (SELECT count(*) FROM public.attachments) THEN
    RAISE EXCEPTION 'WAVE5_RECONCILIATION: attachments';
  END IF;
  IF (SELECT count(*) FROM public.health_record_files) <>
     (SELECT count(*) FROM public.attachments WHERE health_record_id IS NOT NULL) THEN
    RAISE EXCEPTION 'WAVE5_RECONCILIATION: record attachments';
  END IF;
  IF (SELECT count(*) FROM public.health_device_connections) <>
     (SELECT count(*) FROM public.device_connections) THEN
    RAISE EXCEPTION 'WAVE5_RECONCILIATION: device connections';
  END IF;
  IF (SELECT count(*) FROM public.device_measurements) <>
     (SELECT count(*) FROM public.health_observations) THEN
    RAISE EXCEPTION 'WAVE5_RECONCILIATION: observations';
  END IF;
  IF (SELECT count(*) FROM public.health_summaries) <>
     (SELECT count(*) FROM public.health_records hr
       WHERE EXISTS (SELECT 1 FROM public.health_summaries s WHERE s.summary_id=hr.health_record_id)) THEN
    RAISE EXCEPTION 'WAVE5_RECONCILIATION: summaries';
  END IF;
  IF EXISTS (SELECT 1 FROM public.health_record_files f
              LEFT JOIN public.health_records r ON r.health_record_id=f.health_record_id
              LEFT JOIN public.uploaded_files u ON u.file_id=f.file_id
              WHERE r.health_record_id IS NULL OR u.file_id IS NULL) OR
     EXISTS (SELECT 1 FROM public.device_measurements m
              LEFT JOIN public.health_device_connections c ON c.connection_id=m.connection_id
              WHERE c.connection_id IS NULL) THEN
    RAISE EXCEPTION 'WAVE5_ORPHAN_SOURCE';
  END IF;
END $wave5_reconcile$;

DO $wave5_retarget_fks$
DECLARE m record; c record; new_def text;
BEGIN
  FOR m IN SELECT * FROM (VALUES
    ('uploaded_files','attachments','file_id','attachment_id'),
    ('health_device_connections','device_connections','connection_id','device_connection_id'),
    ('device_measurements','health_observations','device_measurement_id','health_observation_id'),
    ('health_summaries','health_records','summary_id','health_record_id')
  ) x(source_table,target_table,source_key,target_key)
  LOOP
    FOR c IN SELECT conrelid::regclass AS rel,conname,pg_get_constraintdef(oid) AS def
      FROM pg_constraint WHERE contype='f' AND confrelid=to_regclass('public.'||m.source_table)
    LOOP
      new_def:=replace(c.def,m.source_table,m.target_table);
      new_def:=replace(new_def,m.target_table||'('||m.source_key||')',m.target_table||'('||m.target_key||')');
      IF new_def=c.def THEN
        RAISE EXCEPTION 'WAVE5_DEPENDENCY: cannot retarget %.%',c.rel,c.conname;
      END IF;
      EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I',c.rel,c.conname);
      EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s',c.rel,c.conname,new_def);
    END LOOP;
  END LOOP;
END $wave5_retarget_fks$;

CREATE INDEX IF NOT EXISTS health_records_summary_filter_ix
  ON public.health_records(owner_user_id,summary_period,record_date DESC)
  WHERE record_type='SUMMARY' AND status='ACTIVE';

DROP TABLE IF EXISTS public.health_record_files;
DROP TABLE IF EXISTS public.device_measurements;
DROP TABLE IF EXISTS public.health_summaries;
DROP TABLE IF EXISTS public.uploaded_files;
DROP TABLE IF EXISTS public.health_device_connections;

DO $wave5_absence_gate$
DECLARE name text;
BEGIN
  FOREACH name IN ARRAY ARRAY['uploaded_files','health_record_files','health_device_connections',
    'device_measurements','health_summaries']
  LOOP
    IF to_regclass('public.'||name) IS NOT NULL THEN
      RAISE EXCEPTION 'WAVE5_DROP_FAILED: %',name;
    END IF;
  END LOOP;
END $wave5_absence_gate$;

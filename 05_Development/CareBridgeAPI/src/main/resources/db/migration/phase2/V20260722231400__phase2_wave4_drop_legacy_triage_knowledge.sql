-- Phase 2 wave 4 cutover: triage, structured intake, knowledge, and health context.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

ALTER TABLE public.triage_sessions
  ADD COLUMN IF NOT EXISTS baby_profile_id uuid,
  ADD COLUMN IF NOT EXISTS mother_profile_id uuid,
  ADD COLUMN IF NOT EXISTS client_request_id varchar(64),
  ADD COLUMN IF NOT EXISTS symptoms text,
  ADD COLUMN IF NOT EXISTS raw_ai_response text,
  ADD COLUMN IF NOT EXISTS disclaimer_text text,
  ADD COLUMN IF NOT EXISTS created_by uuid,
  ADD COLUMN IF NOT EXISTS symptom_list jsonb,
  ADD COLUMN IF NOT EXISTS duration_days integer,
  ADD COLUMN IF NOT EXISTS intensity varchar(20),
  ADD COLUMN IF NOT EXISTS emergency_flag boolean,
  ADD COLUMN IF NOT EXISTS extracted_at timestamptz,
  ADD COLUMN IF NOT EXISTS structured_created_by varchar(50);

ALTER TABLE public.health_context_memories
  ADD COLUMN IF NOT EXISTS mother_profile_id uuid,
  ADD COLUMN IF NOT EXISTS baby_profile_id uuid;

INSERT INTO public.triage_sessions (
    triage_session_id,user_id,baby_profile_id,mother_profile_id,stage,client_request_id,
    symptoms,raw_ai_response,risk_level,status,emergency,disclaimer_text,disclaimer_version,
    input_jsonb,result_jsonb,created_at,completed_at,updated_at,created_by)
SELECT i.id,i.user_id,i.baby_profile_id,i.mother_profile_id,i.stage,i.client_request_id,
       i.symptoms,i.raw_ai_response,i.risk_level,i.status,
       coalesce(i.risk_level='RED',false),i.disclaimer,i.disclaimer,
       jsonb_build_object('symptoms',i.symptoms),
       jsonb_build_object('rawAiResponse',i.raw_ai_response),
       i.created_at,i.completed_at,coalesce(i.completed_at,i.created_at),i.created_by
  FROM public.intake_sessions i
ON CONFLICT (triage_session_id) DO UPDATE SET
  user_id=excluded.user_id,baby_profile_id=excluded.baby_profile_id,
  mother_profile_id=excluded.mother_profile_id,stage=excluded.stage,
  client_request_id=excluded.client_request_id,symptoms=excluded.symptoms,
  raw_ai_response=excluded.raw_ai_response,risk_level=excluded.risk_level,
  status=excluded.status,emergency=excluded.emergency,
  disclaimer_text=excluded.disclaimer_text,disclaimer_version=excluded.disclaimer_version,
  input_jsonb=excluded.input_jsonb,result_jsonb=excluded.result_jsonb,
  created_at=excluded.created_at,completed_at=excluded.completed_at,
  updated_at=excluded.updated_at,created_by=excluded.created_by;

UPDATE public.triage_sessions ts
   SET symptom_list=s.symptom_list,duration_days=s.duration_days,intensity=s.intensity,
       emergency_flag=s.emergency_flag,extracted_at=s.extracted_at,
       structured_created_by=s.created_by,emergency=ts.emergency OR s.emergency_flag,
       input_jsonb=ts.input_jsonb || jsonb_build_object('structured',jsonb_build_object(
         'symptomList',s.symptom_list,'durationDays',s.duration_days,
         'intensity',s.intensity,'emergencyFlag',s.emergency_flag))
  FROM public.structured_intake_data s
 WHERE ts.triage_session_id=s.session_id;

INSERT INTO public.knowledge_sources (
    knowledge_source_id,domain,base_url,organization,category,status,discovery_mode,
    applicable_stages,added_by,reviewed_by,reviewed_at,notes,created_at,updated_at)
SELECT id,domain,base_url,organization,category,status,discovery_mode,applicable_stages,
       added_by,reviewed_by,reviewed_at,notes,created_at,updated_at
  FROM public.evidence_sources
ON CONFLICT (knowledge_source_id) DO UPDATE SET
  domain=excluded.domain,base_url=excluded.base_url,organization=excluded.organization,
  category=excluded.category,status=excluded.status,discovery_mode=excluded.discovery_mode,
  applicable_stages=excluded.applicable_stages,added_by=excluded.added_by,
  reviewed_by=excluded.reviewed_by,reviewed_at=excluded.reviewed_at,
  notes=excluded.notes,updated_at=excluded.updated_at;

INSERT INTO public.knowledge_source_reviews (
    review_id,knowledge_source_id,previous_status,new_status,actor_user_id,
    actor_role,notes,changed_at)
SELECT id,evidence_source_id,previous_status,new_status,actor_user_id,
       actor_role,notes,changed_at
  FROM public.evidence_source_review_log
ON CONFLICT (review_id) DO NOTHING;

INSERT INTO public.health_context_memories (
    memory_id,user_id,mother_profile_id,baby_profile_id,triage_session_id,
    related_stage,summary_text,created_at,expires_at,deleted_at)
SELECT id,user_id,mother_profile_id,baby_profile_id,source_session_id,
       related_stage,summary_text,created_at,expires_at,deleted_at
  FROM public.health_memory_entries
ON CONFLICT (memory_id) DO UPDATE SET
  user_id=excluded.user_id,mother_profile_id=excluded.mother_profile_id,
  baby_profile_id=excluded.baby_profile_id,triage_session_id=excluded.triage_session_id,
  related_stage=excluded.related_stage,summary_text=excluded.summary_text,
  expires_at=excluded.expires_at,deleted_at=excluded.deleted_at;

DO $wave4_reconcile$
BEGIN
  IF (SELECT count(*) FROM public.intake_sessions) <>
     (SELECT count(*) FROM public.triage_sessions) THEN
    RAISE EXCEPTION 'WAVE4_RECONCILIATION: triage sessions';
  END IF;
  IF (SELECT count(*) FROM public.structured_intake_data) <>
     (SELECT count(*) FROM public.triage_sessions WHERE symptom_list IS NOT NULL) THEN
    RAISE EXCEPTION 'WAVE4_RECONCILIATION: structured intake';
  END IF;
  IF (SELECT count(*) FROM public.evidence_sources) <>
     (SELECT count(*) FROM public.knowledge_sources) THEN
    RAISE EXCEPTION 'WAVE4_RECONCILIATION: knowledge sources';
  END IF;
  IF (SELECT count(*) FROM public.evidence_source_review_log) <>
     (SELECT count(*) FROM public.knowledge_source_reviews) THEN
    RAISE EXCEPTION 'WAVE4_RECONCILIATION: knowledge reviews';
  END IF;
  IF (SELECT count(*) FROM public.health_memory_entries) <>
     (SELECT count(*) FROM public.health_context_memories) THEN
    RAISE EXCEPTION 'WAVE4_RECONCILIATION: health context memories';
  END IF;
  IF EXISTS (SELECT 1 FROM public.structured_intake_data s
              LEFT JOIN public.intake_sessions i ON i.id=s.session_id WHERE i.id IS NULL) OR
     EXISTS (SELECT 1 FROM public.evidence_source_review_log r
              LEFT JOIN public.evidence_sources e ON e.id=r.evidence_source_id WHERE e.id IS NULL) THEN
    RAISE EXCEPTION 'WAVE4_ORPHAN_SOURCE';
  END IF;
END $wave4_reconcile$;

DO $wave4_structured_dependency_gate$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_constraint
              WHERE contype='f' AND confrelid='public.structured_intake_data'::regclass) THEN
    RAISE EXCEPTION 'WAVE4_DEPENDENCY: structured intake row identity has no canonical equivalent';
  END IF;
END $wave4_structured_dependency_gate$;

DO $wave4_retarget_fks$
DECLARE m record; c record; new_def text;
BEGIN
  FOR m IN SELECT * FROM (VALUES
    ('intake_sessions','triage_sessions','id','triage_session_id'),
    ('evidence_sources','knowledge_sources','id','knowledge_source_id'),
    ('evidence_source_review_log','knowledge_source_reviews','id','review_id'),
    ('health_memory_entries','health_context_memories','id','memory_id')
  ) x(source_table,target_table,source_key,target_key)
  LOOP
    FOR c IN SELECT conrelid::regclass AS rel,conname,pg_get_constraintdef(oid) AS def
      FROM pg_constraint WHERE contype='f' AND confrelid=to_regclass('public.'||m.source_table)
    LOOP
      new_def:=replace(c.def,m.source_table,m.target_table);
      new_def:=replace(new_def,m.target_table||'('||m.source_key||')',m.target_table||'('||m.target_key||')');
      IF new_def=c.def THEN
        RAISE EXCEPTION 'WAVE4_DEPENDENCY: cannot retarget %.%',c.rel,c.conname;
      END IF;
      EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I',c.rel,c.conname);
      EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s',c.rel,c.conname,new_def);
    END LOOP;
  END LOOP;
END $wave4_retarget_fks$;

CREATE UNIQUE INDEX IF NOT EXISTS triage_sessions_owner_request_uk
  ON public.triage_sessions(user_id,client_request_id) WHERE client_request_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS triage_sessions_stage_ix ON public.triage_sessions(stage,created_at);
CREATE INDEX IF NOT EXISTS health_context_memories_mother_ix
  ON public.health_context_memories(mother_profile_id,created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS health_context_memories_baby_ix
  ON public.health_context_memories(baby_profile_id,created_at DESC) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS knowledge_sources_domain_uk ON public.knowledge_sources(lower(domain));

ALTER TABLE public.triage_sessions
  ALTER COLUMN symptoms SET NOT NULL,
  ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE public.triage_sessions
  ADD CONSTRAINT triage_sessions_intensity_ck
  CHECK (intensity IS NULL OR intensity IN ('LOW','MEDIUM','HIGH'));

DROP TRIGGER IF EXISTS knowledge_source_reviews_immutable_trg ON public.knowledge_source_reviews;
CREATE TRIGGER knowledge_source_reviews_immutable_trg
BEFORE UPDATE OR DELETE ON public.knowledge_source_reviews
FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();

DROP TABLE IF EXISTS public.structured_intake_data;
DROP TABLE IF EXISTS public.evidence_source_review_log;
DROP TABLE IF EXISTS public.health_memory_entries;
DROP TABLE IF EXISTS public.evidence_sources;
DROP TABLE IF EXISTS public.intake_sessions;

DO $wave4_absence_gate$
DECLARE name text;
BEGIN
  FOREACH name IN ARRAY ARRAY['intake_sessions','structured_intake_data','evidence_sources',
    'evidence_source_review_log','health_memory_entries']
  LOOP
    IF to_regclass('public.'||name) IS NOT NULL THEN
      RAISE EXCEPTION 'WAVE4_DROP_FAILED: %',name;
    END IF;
  END LOOP;
END $wave4_absence_gate$;

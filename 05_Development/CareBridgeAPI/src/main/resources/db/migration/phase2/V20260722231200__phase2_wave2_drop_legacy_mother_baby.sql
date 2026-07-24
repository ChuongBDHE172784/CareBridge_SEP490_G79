-- Phase 2 application cutover, wave 2: canonical mother/baby runtime and legacy cleanup.
-- Every source is copied and reconciled before removal; CASCADE is intentionally absent.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

ALTER TABLE public.mother_journeys
    ADD COLUMN IF NOT EXISTS baseline_revision bigint,
    ADD COLUMN IF NOT EXISTS baseline_schema_version varchar(40),
    ADD COLUMN IF NOT EXISTS baseline_source varchar(30),
    ADD COLUMN IF NOT EXISTS baseline_lifecycle_goal varchar(40),
    ADD COLUMN IF NOT EXISTS baseline_locale varchar(20),
    ADD COLUMN IF NOT EXISTS baseline_time_zone varchar(80),
    ADD COLUMN IF NOT EXISTS baseline_preferences varchar(300),
    ADD COLUMN IF NOT EXISTS baseline_submission_id uuid,
    ADD COLUMN IF NOT EXISTS baseline_recorded_at timestamptz;

ALTER TABLE public.mother_journey_events
    ADD COLUMN IF NOT EXISTS submission_id uuid,
    ADD COLUMN IF NOT EXISTS event_source varchar(30),
    ADD COLUMN IF NOT EXISTS confidence varchar(20),
    ADD COLUMN IF NOT EXISTS reason varchar(500),
    ADD COLUMN IF NOT EXISTS lifecycle_goal varchar(40),
    ADD COLUMN IF NOT EXISTS locale varchar(20),
    ADD COLUMN IF NOT EXISTS time_zone varchar(80),
    ADD COLUMN IF NOT EXISTS preferences varchar(300),
    ADD COLUMN IF NOT EXISTS outcome_type varchar(30),
    ADD COLUMN IF NOT EXISTS outcome_date date,
    ADD COLUMN IF NOT EXISTS revision_number integer,
    ADD COLUMN IF NOT EXISTS supersedes_evidence_id uuid,
    ADD COLUMN IF NOT EXISTS semantic_hash varchar(500),
    ADD COLUMN IF NOT EXISTS correction boolean,
    ADD COLUMN IF NOT EXISTS operation_type varchar(30),
    ADD COLUMN IF NOT EXISTS semantic_intent varchar(1000),
    ADD COLUMN IF NOT EXISTS care_subject_id uuid;

ALTER TABLE public.maternal_observations
    ADD COLUMN IF NOT EXISTS source_reference_id uuid,
    ADD COLUMN IF NOT EXISTS record_status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS observation_date date,
    ADD COLUMN IF NOT EXISTS submission_id uuid,
    ADD COLUMN IF NOT EXISTS mood_level smallint,
    ADD COLUMN IF NOT EXISTS breastfeeding_note text,
    ADD COLUMN IF NOT EXISTS exercise_template_id uuid,
    ADD COLUMN IF NOT EXISTS owner_user_id uuid,
    ADD COLUMN IF NOT EXISTS check_code varchar(100),
    ADD COLUMN IF NOT EXISTS response_boolean boolean,
    ADD COLUMN IF NOT EXISTS blocked_boolean boolean,
    ADD COLUMN IF NOT EXISTS event_time_ms bigint,
    ADD COLUMN IF NOT EXISTS posture_config_id uuid,
    ADD COLUMN IF NOT EXISTS posture_code varchar(80);

ALTER TABLE public.maternal_exercise_sessions
    ADD COLUMN IF NOT EXISTS safety_observation_id uuid;

ALTER TABLE public.care_item_templates
    ADD COLUMN IF NOT EXISTS created_by uuid,
    ADD COLUMN IF NOT EXISTS difficulty_level varchar(30),
    ADD COLUMN IF NOT EXISTS duration_minutes smallint,
    ADD COLUMN IF NOT EXISTS instruction_content text,
    ADD COLUMN IF NOT EXISTS media_url text,
    ADD COLUMN IF NOT EXISTS safety_warning text,
    ADD COLUMN IF NOT EXISTS supports_posture_analysis boolean,
    ADD COLUMN IF NOT EXISTS template_status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS configured_by uuid,
    ADD COLUMN IF NOT EXISTS analysis_mode varchar(30),
    ADD COLUMN IF NOT EXISTS rule_or_model_version varchar(80),
    ADD COLUMN IF NOT EXISTS confidence_threshold numeric,
    ADD COLUMN IF NOT EXISTS feedback_level varchar(30);

DROP TRIGGER IF EXISTS mother_journey_events_immutable_trg ON public.mother_journey_events;

-- Versioned exercise and posture definitions are canonical template rows.
DO $wave2_templates$
BEGIN
  IF to_regclass('public.pregnancy_exercises') IS NOT NULL THEN
    INSERT INTO public.care_item_templates (
      template_id,entry_type,title,description,stage,is_active,version,effective_from,
      configuration_jsonb,configuration_hash,created_by,difficulty_level,duration_minutes,
      instruction_content,media_url,safety_warning,supports_posture_analysis,template_status,
      created_at,updated_at)
    SELECT e.exercise_id,'EXERCISE_TEMPLATE',e.title,e.description,e.trimester_scope,
      e.status='PUBLISHED',e.version_no,e.created_at,
      jsonb_build_object('difficulty',e.difficulty_level,'durationMinutes',e.duration_minutes,
        'supportsPostureAnalysis',e.supports_posture_analysis),
      md5(jsonb_build_object('difficulty',e.difficulty_level,'durationMinutes',e.duration_minutes,
        'supportsPostureAnalysis',e.supports_posture_analysis)::text),
      e.created_by,e.difficulty_level,e.duration_minutes,e.instruction_content,e.media_url,
      e.safety_warning,e.supports_posture_analysis,e.status,e.created_at,e.updated_at
    FROM public.pregnancy_exercises e
    ON CONFLICT (template_id) DO UPDATE SET
      entry_type=excluded.entry_type,title=excluded.title,description=excluded.description,
      stage=excluded.stage,is_active=excluded.is_active,version=excluded.version,
      effective_from=excluded.effective_from,configuration_jsonb=excluded.configuration_jsonb,
      configuration_hash=excluded.configuration_hash,created_by=excluded.created_by,
      difficulty_level=excluded.difficulty_level,duration_minutes=excluded.duration_minutes,
      instruction_content=excluded.instruction_content,media_url=excluded.media_url,
      safety_warning=excluded.safety_warning,
      supports_posture_analysis=excluded.supports_posture_analysis,
      template_status=excluded.template_status,updated_at=excluded.updated_at;
  END IF;

  IF to_regclass('public.posture_analysis_configs') IS NOT NULL THEN
    INSERT INTO public.care_item_templates (
      template_id,parent_template_id,entry_type,title,is_active,version,effective_from,effective_to,
      configuration_jsonb,configuration_hash,configured_by,analysis_mode,rule_or_model_version,
      confidence_threshold,feedback_level,template_status,created_at,updated_at)
    SELECT c.posture_config_id,c.exercise_id,'POSTURE_CONFIG',
      'Posture config '||coalesce(c.rule_or_model_version,c.posture_config_id::text),
      c.status='ACTIVE',row_number() OVER (PARTITION BY c.exercise_id ORDER BY c.effective_from,c.posture_config_id),
      c.effective_from,c.effective_to,coalesce(c.config_json,'{}'::jsonb),
      md5(coalesce(c.config_json,'{}'::jsonb)::text),c.configured_by,c.analysis_mode,
      c.rule_or_model_version,c.confidence_threshold,c.feedback_level,c.status,c.created_at,c.updated_at
    FROM public.posture_analysis_configs c
    ON CONFLICT (template_id) DO UPDATE SET
      parent_template_id=excluded.parent_template_id,entry_type=excluded.entry_type,
      title=excluded.title,is_active=excluded.is_active,version=excluded.version,
      effective_from=excluded.effective_from,effective_to=excluded.effective_to,
      configuration_jsonb=excluded.configuration_jsonb,configuration_hash=excluded.configuration_hash,
      configured_by=excluded.configured_by,analysis_mode=excluded.analysis_mode,
      rule_or_model_version=excluded.rule_or_model_version,
      confidence_threshold=excluded.confidence_threshold,feedback_level=excluded.feedback_level,
      template_status=excluded.template_status,updated_at=excluded.updated_at;
  END IF;
END $wave2_templates$;

-- Sessions retain the template, safety-result and effective posture-version identifiers.
DO $wave2_sessions$
BEGIN
  IF to_regclass('public.exercise_sessions') IS NOT NULL THEN
    INSERT INTO public.maternal_exercise_sessions (
      exercise_session_id,mother_journey_id,owner_user_id,exercise_template_id,
      posture_config_id,safety_observation_id,started_at,ended_at,paused_seconds,
      completion_percent,posture_score,session_status,warning_count,summary_jsonb,created_at,updated_at)
    SELECT s.exercise_session_id,s.journey_id,s.user_id,s.exercise_id,
      pc.posture_config_id,s.safety_check_id,s.started_at,s.ended_at,coalesce(s.paused_seconds,0),
      s.completion_percent,s.posture_score,s.session_status,coalesce(s.warning_count,0),
      coalesce(s.summary_json,'{}'::jsonb),s.created_at,s.updated_at
    FROM public.exercise_sessions s
    LEFT JOIN LATERAL (
      SELECT c.posture_config_id FROM public.posture_analysis_configs c
       WHERE c.exercise_id=s.exercise_id AND c.effective_from<=s.started_at
       ORDER BY (c.status='ACTIVE') DESC,c.effective_from DESC,c.posture_config_id LIMIT 1
    ) pc ON to_regclass('public.posture_analysis_configs') IS NOT NULL
    ON CONFLICT (exercise_session_id) DO UPDATE SET
      mother_journey_id=excluded.mother_journey_id,owner_user_id=excluded.owner_user_id,
      exercise_template_id=excluded.exercise_template_id,posture_config_id=excluded.posture_config_id,
      safety_observation_id=excluded.safety_observation_id,started_at=excluded.started_at,
      ended_at=excluded.ended_at,paused_seconds=excluded.paused_seconds,
      completion_percent=excluded.completion_percent,posture_score=excluded.posture_score,
      session_status=excluded.session_status,warning_count=excluded.warning_count,
      summary_jsonb=excluded.summary_jsonb,created_at=excluded.created_at,updated_at=excluded.updated_at;
  END IF;
END $wave2_sessions$;

-- Rebuild derived event representations with exact source UUIDs and scalar query fields.
DELETE FROM public.mother_journey_events WHERE legacy_source IN (
  'mother_baseline_contexts','mother_journey_transitions','pregnancy_outcome_evidence','baby_link_submissions');

DO $wave2_events$
BEGIN
  IF to_regclass('public.mother_baseline_contexts') IS NOT NULL THEN
    INSERT INTO public.mother_journey_events (
      event_id,mother_journey_id,owner_user_id,event_type,event_payload_jsonb,schema_version,
      effective_at,recorded_at,journey_version,legacy_source,legacy_id,submission_id,event_source,
      lifecycle_goal,locale,time_zone,preferences)
    SELECT b.baseline_id,j.journey_id,b.owner_user_id,'BASELINE_CONTEXT','{}'::jsonb,b.schema_version,
      b.recorded_at,b.recorded_at,b.revision,'MOTHER_BASELINE',b.baseline_id::text,b.submission_id,
      b.source,b.lifecycle_goal,b.locale,b.time_zone,b.preferences
    FROM public.mother_baseline_contexts b
    LEFT JOIN LATERAL (
      SELECT journey_id FROM public.mother_journeys mj WHERE mj.owner_user_id=b.owner_user_id
       ORDER BY mj.created_at DESC,mj.journey_id LIMIT 1) j ON true
    ON CONFLICT (legacy_source,legacy_id) DO NOTHING;

    WITH latest AS (
      SELECT DISTINCT ON (owner_user_id) * FROM public.mother_baseline_contexts
       ORDER BY owner_user_id,revision DESC,recorded_at DESC,baseline_id)
    UPDATE public.mother_journeys mj SET
      baseline_revision=b.revision,baseline_schema_version=b.schema_version,
      baseline_source=b.source,baseline_lifecycle_goal=b.lifecycle_goal,
      baseline_locale=b.locale,baseline_time_zone=b.time_zone,
      baseline_preferences=b.preferences,baseline_submission_id=b.submission_id,
      baseline_recorded_at=b.recorded_at
    FROM latest b WHERE mj.owner_user_id=b.owner_user_id
      AND mj.journey_id=(SELECT journey_id FROM public.mother_journeys x
        WHERE x.owner_user_id=b.owner_user_id ORDER BY x.created_at DESC,x.journey_id LIMIT 1);
  END IF;

  IF to_regclass('public.mother_journey_transitions') IS NOT NULL THEN
    INSERT INTO public.mother_journey_events (
      event_id,mother_journey_id,owner_user_id,event_type,from_stage,to_stage,event_payload_jsonb,
      schema_version,actor_user_id,effective_at,recorded_at,journey_version,legacy_source,legacy_id,
      event_source,confidence,reason)
    SELECT t.transition_id,t.journey_id,j.owner_user_id,t.event_type,t.from_stage,t.to_stage,
      coalesce(t.changes_json,'{}'::jsonb),'1',t.actor_user_id,t.effective_at,t.recorded_at,
      t.journey_version,'JOURNEY_TRANSITION',t.transition_id::text,t.source,t.confidence,t.reason
    FROM public.mother_journey_transitions t JOIN public.mother_journeys j ON j.journey_id=t.journey_id
    ON CONFLICT (legacy_source,legacy_id) DO NOTHING;
  END IF;

  IF to_regclass('public.pregnancy_outcome_evidence') IS NOT NULL THEN
    INSERT INTO public.mother_journey_events (
      event_id,mother_journey_id,owner_user_id,event_type,event_payload_jsonb,schema_version,
      actor_user_id,effective_at,recorded_at,journey_version,legacy_source,legacy_id,
      submission_id,event_source,reason,outcome_type,outcome_date,revision_number,
      supersedes_evidence_id,semantic_hash,correction)
    SELECT e.evidence_id,e.journey_id,e.owner_user_id,'PREGNANCY_OUTCOME_EVIDENCE','{}'::jsonb,'1',
      e.actor_user_id,e.effective_at,e.recorded_at,e.journey_version,'PREGNANCY_OUTCOME',e.evidence_id::text,
      e.submission_id,e.source,e.reason,e.outcome_type,e.outcome_date,e.revision_number,
      e.supersedes_evidence_id,e.semantic_hash,e.correction
    FROM public.pregnancy_outcome_evidence e
    ON CONFLICT (legacy_source,legacy_id) DO NOTHING;
  END IF;

  IF to_regclass('public.baby_link_submissions') IS NOT NULL THEN
    INSERT INTO public.mother_journey_events (
      event_id,mother_journey_id,owner_user_id,event_type,event_payload_jsonb,schema_version,
      effective_at,recorded_at,legacy_source,legacy_id,submission_id,operation_type,
      semantic_intent,care_subject_id)
    SELECT b.link_submission_id,b.journey_id,b.owner_user_id,'BABY_LINK_'||b.operation_type,
      '{}'::jsonb,'1',b.created_at,b.created_at,'BABY_LINK',b.link_submission_id::text,
      b.submission_id,b.operation_type,b.semantic_intent,b.baby_id
    FROM public.baby_link_submissions b
    ON CONFLICT (legacy_source,legacy_id) DO NOTHING;
  END IF;
END $wave2_events$;

-- Rebuild observation subtypes. Safety answer payloads become one typed row per check code.
DELETE FROM public.maternal_observations WHERE legacy_source IN (
  'maternal_health_metrics','postpartum_logs','exercise_safety_checks','posture_feedback_events');

DO $wave2_observations$
BEGIN
  IF to_regclass('public.maternal_health_metrics') IS NOT NULL THEN
    INSERT INTO public.maternal_observations (
      observation_id,observation_type,mother_journey_id,numeric_value,secondary_numeric_value,
      unit,text_value,observed_at,payload_jsonb,schema_version,source_type,legacy_source,legacy_id,
      source_reference_id,record_status,created_at,updated_at)
    SELECT m.metric_id,m.metric_type,m.journey_id,m.value_numeric,m.value_secondary,m.unit,m.note,
      m.measured_at,'{}'::jsonb,'1',coalesce(m.source_type,'MANUAL'),'MATERNAL_METRIC',m.metric_id::text,
      m.source_reference_id,m.status,m.created_at,m.updated_at
    FROM public.maternal_health_metrics m
    ON CONFLICT (legacy_source,legacy_id) DO NOTHING;
  END IF;

  IF to_regclass('public.postpartum_logs') IS NOT NULL THEN
    INSERT INTO public.maternal_observations (
      observation_id,observation_type,mother_journey_id,numeric_value,secondary_numeric_value,
      text_value,severity,observed_at,payload_jsonb,schema_version,source_type,legacy_source,legacy_id,
      observation_date,submission_id,mood_level,breastfeeding_note,record_status,created_at,updated_at)
    SELECT p.postpartum_log_id,'POSTPARTUM_LOG',p.journey_id,p.pain_level,p.sleep_hours,
      p.symptom_note,p.bleeding_level,p.log_date::timestamptz,'{}'::jsonb,'1','POSTPARTUM_LOG',
      'POSTPARTUM_LOG',p.postpartum_log_id::text,p.log_date,p.submission_id,p.mood_level,
      p.breastfeeding_note,p.status,p.created_at,p.updated_at
    FROM public.postpartum_logs p
    ON CONFLICT (legacy_source,legacy_id) DO NOTHING;
  END IF;

  IF to_regclass('public.exercise_safety_checks') IS NOT NULL THEN
    INSERT INTO public.maternal_observations (
      observation_id,observation_type,mother_journey_id,text_value,observed_at,payload_jsonb,
      schema_version,source_type,legacy_source,legacy_id,exercise_template_id,owner_user_id,
      blocked_boolean,record_status,created_at,updated_at)
    SELECT s.safety_check_id,'EXERCISE_SAFETY_RESULT',s.journey_id,s.blocked_reason,
      coalesce(s.completed_at,s.created_at),coalesce(s.answer_json,'{}'::jsonb),'1','EXERCISE_SAFETY',
      'EXERCISE_SAFETY',s.safety_check_id::text,s.exercise_id,s.user_id,s.red_flag_detected,
      s.result_status,s.created_at,s.created_at
    FROM public.exercise_safety_checks s
    ON CONFLICT (legacy_source,legacy_id) DO NOTHING;

    INSERT INTO public.maternal_observations (
      observation_id,observation_type,mother_journey_id,observed_at,payload_jsonb,schema_version,
      source_type,legacy_source,legacy_id,exercise_template_id,owner_user_id,check_code,
      response_boolean,blocked_boolean,record_status,created_at,updated_at)
    SELECT (substr(md5('safety-answer:'||s.safety_check_id||':'||a.key),1,8)||'-'||
      substr(md5('safety-answer:'||s.safety_check_id||':'||a.key),9,4)||'-'||
      substr(md5('safety-answer:'||s.safety_check_id||':'||a.key),13,4)||'-'||
      substr(md5('safety-answer:'||s.safety_check_id||':'||a.key),17,4)||'-'||
      substr(md5('safety-answer:'||s.safety_check_id||':'||a.key),21,12))::uuid,
      'EXERCISE_SAFETY_CHECK',s.journey_id,coalesce(s.completed_at,s.created_at),'{}'::jsonb,'1',
      'EXERCISE_SAFETY','EXERCISE_SAFETY_ANSWER',s.safety_check_id::text||':'||a.key,
      s.exercise_id,s.user_id,a.key,a.value::boolean,s.red_flag_detected,s.result_status,s.created_at,s.created_at
    FROM public.exercise_safety_checks s CROSS JOIN LATERAL jsonb_each_text(coalesce(s.answer_json,'{}'::jsonb)) a
    ON CONFLICT (legacy_source,legacy_id) DO NOTHING;
  END IF;

  IF to_regclass('public.posture_feedback_events') IS NOT NULL THEN
    INSERT INTO public.maternal_observations (
      observation_id,observation_type,mother_journey_id,exercise_session_id,numeric_value,
      text_value,severity,observed_at,payload_jsonb,schema_version,source_type,legacy_source,legacy_id,
      event_time_ms,posture_config_id,posture_code,created_at,updated_at)
    SELECT f.feedback_event_id,'POSTURE_FEEDBACK',s.mother_journey_id,f.exercise_session_id,
      f.confidence_score,f.feedback_text,f.severity,
      s.started_at+make_interval(secs=>f.event_time_ms/1000.0),
      coalesce(f.keypoint_summary_json,'{}'::jsonb),'1','POSTURE_ANALYSIS','POSTURE_FEEDBACK',
      f.feedback_event_id::text,f.event_time_ms,f.posture_config_id,f.posture_code,f.created_at,f.created_at
    FROM public.posture_feedback_events f
    JOIN public.maternal_exercise_sessions s ON s.exercise_session_id=f.exercise_session_id
    ON CONFLICT (legacy_source,legacy_id) DO NOTHING;
  END IF;
END $wave2_observations$;

DO $wave2_baby$
BEGIN
  IF to_regclass('public.baby_daily_logs') IS NOT NULL THEN
    INSERT INTO public.care_logs (
      care_log_id,care_subject_id,log_type,started_at,ended_at,quantity,unit,note,
      recorded_by,status,payload_jsonb,created_at,updated_at)
    SELECT b.baby_log_id,b.baby_id,b.log_type,b.started_at,b.ended_at,b.quantity,b.unit,b.note,
      b.recorded_by,b.status,'{}'::jsonb,b.created_at,b.updated_at
    FROM public.baby_daily_logs b
    ON CONFLICT (care_log_id) DO UPDATE SET
      care_subject_id=excluded.care_subject_id,log_type=excluded.log_type,
      started_at=excluded.started_at,ended_at=excluded.ended_at,quantity=excluded.quantity,
      unit=excluded.unit,note=excluded.note,recorded_by=excluded.recorded_by,
      status=excluded.status,updated_at=excluded.updated_at;
  END IF;

  IF to_regclass('public.vaccination_reference_schedules') IS NOT NULL THEN
    UPDATE public.vaccination_schedules t
       SET schedule_version='legacy-'||left(t.vaccination_schedule_id::text,8)
      FROM public.vaccination_reference_schedules v
     WHERE t.vaccination_schedule_id=v.ref_id;
    INSERT INTO public.vaccination_schedules (
      vaccination_schedule_id,vaccine_name,dose_number,offset_days,description,schedule_version,created_at)
    SELECT v.ref_id,v.vaccine_name,v.dose_number,v.offset_days,v.description,
      'legacy-'||left(v.ref_id::text,8),v.created_at
    FROM public.vaccination_reference_schedules v
    ON CONFLICT (vaccination_schedule_id) DO UPDATE SET
      vaccine_name=excluded.vaccine_name,dose_number=excluded.dose_number,
      offset_days=excluded.offset_days,description=excluded.description;
  END IF;
END $wave2_baby$;

-- Exact row/subtype reconciliation, including expanded multi-answer safety observations.
DO $wave2_reconcile$
DECLARE source_count bigint; target_count bigint;
BEGIN
  IF to_regclass('public.mother_baseline_contexts') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.mother_baseline_contexts;
    SELECT count(*) INTO target_count FROM public.mother_journey_events WHERE legacy_source='MOTHER_BASELINE';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: mother baseline %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.mother_journey_transitions') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.mother_journey_transitions;
    SELECT count(*) INTO target_count FROM public.mother_journey_events WHERE legacy_source='JOURNEY_TRANSITION';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: journey transition %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.pregnancy_outcome_evidence') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.pregnancy_outcome_evidence;
    SELECT count(*) INTO target_count FROM public.mother_journey_events WHERE legacy_source='PREGNANCY_OUTCOME';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: outcome evidence %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.baby_link_submissions') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.baby_link_submissions;
    SELECT count(*) INTO target_count FROM public.mother_journey_events WHERE legacy_source='BABY_LINK';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: baby links %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.maternal_health_metrics') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.maternal_health_metrics;
    SELECT count(*) INTO target_count FROM public.maternal_observations WHERE legacy_source='MATERNAL_METRIC';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: maternal metrics %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.postpartum_logs') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.postpartum_logs;
    SELECT count(*) INTO target_count FROM public.maternal_observations WHERE legacy_source='POSTPARTUM_LOG';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: postpartum logs %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.exercise_safety_checks') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.exercise_safety_checks;
    SELECT count(*) INTO target_count FROM public.maternal_observations WHERE legacy_source='EXERCISE_SAFETY';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: safety results %/%',source_count,target_count; END IF;
    SELECT count(*) INTO source_count FROM public.exercise_safety_checks s
      CROSS JOIN LATERAL jsonb_each(coalesce(s.answer_json,'{}'::jsonb));
    SELECT count(*) INTO target_count FROM public.maternal_observations WHERE legacy_source='EXERCISE_SAFETY_ANSWER';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: safety answers %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.exercise_sessions') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.exercise_sessions;
    SELECT count(*) INTO target_count FROM public.exercise_sessions s JOIN public.maternal_exercise_sessions t ON t.exercise_session_id=s.exercise_session_id;
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: exercise sessions %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.posture_feedback_events') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.posture_feedback_events;
    SELECT count(*) INTO target_count FROM public.maternal_observations WHERE legacy_source='POSTURE_FEEDBACK';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: posture feedback %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.pregnancy_exercises') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.pregnancy_exercises;
    SELECT count(*) INTO target_count FROM public.pregnancy_exercises s JOIN public.care_item_templates t ON t.template_id=s.exercise_id AND t.entry_type='EXERCISE_TEMPLATE';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: exercise templates %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.posture_analysis_configs') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.posture_analysis_configs;
    SELECT count(*) INTO target_count FROM public.posture_analysis_configs s JOIN public.care_item_templates t ON t.template_id=s.posture_config_id AND t.entry_type='POSTURE_CONFIG';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: posture configs %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.baby_daily_logs') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.baby_daily_logs;
    SELECT count(*) INTO target_count FROM public.baby_daily_logs s JOIN public.care_logs t ON t.care_log_id=s.baby_log_id;
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: baby care logs %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.vaccination_reference_schedules') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.vaccination_reference_schedules;
    SELECT count(*) INTO target_count FROM public.vaccination_reference_schedules s JOIN public.vaccination_schedules t ON t.vaccination_schedule_id=s.ref_id;
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: vaccination schedules %/%',source_count,target_count; END IF;
  END IF;
END $wave2_reconcile$;

CREATE UNIQUE INDEX IF NOT EXISTS mother_journey_events_baseline_submission_uk
  ON public.mother_journey_events(owner_user_id,submission_id,legacy_source)
  WHERE legacy_source='MOTHER_BASELINE';
CREATE UNIQUE INDEX IF NOT EXISTS mother_journey_events_outcome_submission_uk
  ON public.mother_journey_events(mother_journey_id,submission_id,legacy_source)
  WHERE legacy_source='PREGNANCY_OUTCOME';
CREATE UNIQUE INDEX IF NOT EXISTS mother_journey_events_baby_link_submission_uk
  ON public.mother_journey_events(owner_user_id,operation_type,submission_id,legacy_source)
  WHERE legacy_source='BABY_LINK';
CREATE INDEX IF NOT EXISTS maternal_observations_safety_query_ix
  ON public.maternal_observations(exercise_template_id,owner_user_id,created_at DESC)
  WHERE legacy_source IN ('EXERCISE_SAFETY','EXERCISE_SAFETY_ANSWER');
CREATE INDEX IF NOT EXISTS care_item_templates_exercise_filter_ix
  ON public.care_item_templates(template_status,stage,difficulty_level,created_at DESC)
  WHERE entry_type='EXERCISE_TEMPLATE';
CREATE INDEX IF NOT EXISTS care_item_templates_posture_version_ix
  ON public.care_item_templates(parent_template_id,template_status,effective_from DESC)
  WHERE entry_type='POSTURE_CONFIG';

-- Retarget every inbound legacy FK to the canonical row with the same UUID key.
DO $wave2_retarget_fks$
DECLARE m record; c record; new_def text;
BEGIN
  FOR m IN SELECT * FROM (VALUES
    ('mother_baseline_contexts','mother_journey_events','baseline_id','event_id'),
    ('mother_journey_transitions','mother_journey_events','transition_id','event_id'),
    ('pregnancy_outcome_evidence','mother_journey_events','evidence_id','event_id'),
    ('maternal_health_metrics','maternal_observations','metric_id','observation_id'),
    ('postpartum_logs','maternal_observations','postpartum_log_id','observation_id'),
    ('exercise_safety_checks','maternal_observations','safety_check_id','observation_id'),
    ('exercise_sessions','maternal_exercise_sessions','exercise_session_id','exercise_session_id'),
    ('posture_feedback_events','maternal_observations','feedback_event_id','observation_id'),
    ('posture_analysis_configs','care_item_templates','posture_config_id','template_id'),
    ('pregnancy_exercises','care_item_templates','exercise_id','template_id'),
    ('baby_daily_logs','care_logs','baby_log_id','care_log_id'),
    ('baby_link_submissions','mother_journey_events','link_submission_id','event_id'),
    ('vaccination_reference_schedules','vaccination_schedules','ref_id','vaccination_schedule_id')
  ) x(source_table,target_table,source_key,target_key)
  LOOP
    IF to_regclass('public.'||m.source_table) IS NOT NULL THEN
      FOR c IN SELECT conrelid::regclass AS rel,conname,pg_get_constraintdef(oid) AS def
        FROM pg_constraint WHERE contype='f' AND confrelid=to_regclass('public.'||m.source_table)
      LOOP
        new_def:=replace(c.def,m.source_table,m.target_table);
        new_def:=replace(new_def,
          m.target_table||'('||m.source_key||')',
          m.target_table||'('||m.target_key||')');
        IF new_def=c.def THEN
          RAISE EXCEPTION 'WAVE2_DEPENDENCY: cannot retarget %.%',c.rel,c.conname;
        END IF;
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I',c.rel,c.conname);
        EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s',c.rel,c.conname,new_def);
      END LOOP;
    END IF;
  END LOOP;
END $wave2_retarget_fks$;

DO $wave2_canonical_fks$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='mother_journey_events_care_subject_fk') THEN
    ALTER TABLE public.mother_journey_events ADD CONSTRAINT mother_journey_events_care_subject_fk
      FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='mother_journey_events_supersedes_fk') THEN
    ALTER TABLE public.mother_journey_events ADD CONSTRAINT mother_journey_events_supersedes_fk
      FOREIGN KEY (supersedes_evidence_id) REFERENCES public.mother_journey_events(event_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='maternal_observations_exercise_template_fk') THEN
    ALTER TABLE public.maternal_observations ADD CONSTRAINT maternal_observations_exercise_template_fk
      FOREIGN KEY (exercise_template_id) REFERENCES public.care_item_templates(template_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='maternal_observations_owner_fk') THEN
    ALTER TABLE public.maternal_observations ADD CONSTRAINT maternal_observations_owner_fk
      FOREIGN KEY (owner_user_id) REFERENCES public.users(user_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='maternal_observations_posture_config_fk') THEN
    ALTER TABLE public.maternal_observations ADD CONSTRAINT maternal_observations_posture_config_fk
      FOREIGN KEY (posture_config_id) REFERENCES public.care_item_templates(template_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='maternal_exercise_sessions_template_fk') THEN
    ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_template_fk
      FOREIGN KEY (exercise_template_id) REFERENCES public.care_item_templates(template_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='maternal_exercise_sessions_posture_config_fk') THEN
    ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_posture_config_fk
      FOREIGN KEY (posture_config_id) REFERENCES public.care_item_templates(template_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='maternal_exercise_sessions_safety_fk') THEN
    ALTER TABLE public.maternal_exercise_sessions ADD CONSTRAINT maternal_exercise_sessions_safety_fk
      FOREIGN KEY (safety_observation_id) REFERENCES public.maternal_observations(observation_id);
  END IF;
END $wave2_canonical_fks$;

CREATE OR REPLACE FUNCTION public.enforce_mother_journey_event_owner()
RETURNS trigger LANGUAGE plpgsql AS $$
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

DROP TRIGGER IF EXISTS mother_journey_events_owner_trg ON public.mother_journey_events;
CREATE TRIGGER mother_journey_events_owner_trg
BEFORE INSERT ON public.mother_journey_events
FOR EACH ROW EXECUTE FUNCTION public.enforce_mother_journey_event_owner();

CREATE TRIGGER mother_journey_events_immutable_trg
BEFORE UPDATE OR DELETE ON public.mother_journey_events
FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();

DO $wave2_dependency_gate$
DECLARE legacy regclass; name text;
BEGIN
  FOREACH name IN ARRAY ARRAY[
    'mother_baseline_contexts','mother_journey_transitions','pregnancy_outcome_evidence',
    'maternal_health_metrics','postpartum_logs','exercise_safety_checks','exercise_sessions',
    'posture_feedback_events','posture_analysis_configs','pregnancy_exercises','baby_daily_logs',
    'baby_link_submissions','vaccination_reference_schedules']
  LOOP
    legacy:=to_regclass('public.'||name);
    IF legacy IS NOT NULL AND EXISTS (SELECT 1 FROM pg_constraint WHERE confrelid=legacy) THEN
      RAISE EXCEPTION 'WAVE2_DEPENDENCY: inbound FK remains for %',name;
    END IF;
  END LOOP;
END $wave2_dependency_gate$;

DROP TABLE IF EXISTS public.posture_feedback_events;
DROP TABLE IF EXISTS public.exercise_sessions;
DROP TABLE IF EXISTS public.exercise_safety_checks;
DROP TABLE IF EXISTS public.posture_analysis_configs;
DROP TABLE IF EXISTS public.pregnancy_exercises;
DROP TABLE IF EXISTS public.maternal_health_metrics;
DROP TABLE IF EXISTS public.postpartum_logs;
DROP TABLE IF EXISTS public.mother_journey_transitions;
DROP TABLE IF EXISTS public.pregnancy_outcome_evidence;
DROP TABLE IF EXISTS public.mother_baseline_contexts;
DROP TABLE IF EXISTS public.baby_daily_logs;
DROP TABLE IF EXISTS public.baby_link_submissions;
DROP TABLE IF EXISTS public.vaccination_reference_schedules;

DO $wave2_absence_gate$
DECLARE name text;
BEGIN
  FOREACH name IN ARRAY ARRAY[
    'mother_baseline_contexts','mother_journey_transitions','pregnancy_outcome_evidence',
    'maternal_health_metrics','postpartum_logs','exercise_safety_checks','exercise_sessions',
    'posture_feedback_events','posture_analysis_configs','pregnancy_exercises','baby_daily_logs',
    'baby_link_submissions','vaccination_reference_schedules']
  LOOP
    IF to_regclass('public.'||name) IS NOT NULL THEN
      RAISE EXCEPTION 'WAVE2_DROP_FAILED: %',name;
    END IF;
  END LOOP;
END $wave2_absence_gate$;

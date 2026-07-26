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

-- Rebuild derived event representations inside audit_events.
DELETE FROM public.audit_events WHERE event_category IN ('BASELINE_CONTEXT', 'MOTHER_JOURNEY_TRANSITION', 'PREGNANCY_OUTCOME_EVIDENCE') OR event_category LIKE 'BABY_LINK_%';

DO $wave2_events$
BEGIN
  IF to_regclass('public.mother_baseline_contexts') IS NOT NULL THEN
    INSERT INTO public.audit_events (
      actor_user_id, event_category, subject_reference_id, resource_type, resource_id, payload, occurred_at, created_at, severity, status
    )
    SELECT b.owner_user_id, 'BASELINE_CONTEXT', j.journey_id, 'mother_journeys', j.journey_id,
      jsonb_build_object(
        'baselineId', b.baseline_id,
        'lifecycleGoal', b.lifecycle_goal,
        'locale', b.locale,
        'timeZone', b.time_zone,
        'preferences', b.preferences,
        'source', b.source,
        'submissionId', b.submission_id,
        'legacySource', 'mother_baseline_contexts'
      ), b.recorded_at, b.recorded_at, 'INFO', 'CLOSED'
    FROM public.mother_baseline_contexts b
    LEFT JOIN LATERAL (
      SELECT journey_id FROM public.mother_journeys mj WHERE mj.owner_user_id=b.owner_user_id
       ORDER BY mj.created_at DESC,mj.journey_id LIMIT 1) j ON true
    ON CONFLICT DO NOTHING;

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
    INSERT INTO public.audit_events (
      audit_event_id, actor_user_id, event_category, subject_reference_id, resource_type, resource_id, payload, occurred_at, created_at, severity, status
    )
    SELECT t.transition_id, t.actor_user_id, 'MOTHER_JOURNEY_TRANSITION', t.journey_id, 'mother_journeys', t.journey_id,
      jsonb_build_object(
        'transitionId', t.transition_id,
        'fromStage', t.from_stage,
        'toStage', t.to_stage,
        'changes', t.changes_json,
        'journeyVersion', t.journey_version,
        'source', t.source,
        'confidence', t.confidence,
        'reason', t.reason,
        'legacySource', 'mother_journey_transitions'
      ), t.effective_at, t.recorded_at, 'INFO', 'CLOSED'
    FROM public.mother_journey_transitions t JOIN public.mother_journeys j ON j.journey_id=t.journey_id
    ON CONFLICT (audit_event_id) DO NOTHING;
  END IF;

  IF to_regclass('public.pregnancy_outcome_evidence') IS NOT NULL THEN
    INSERT INTO public.audit_events (
      audit_event_id, actor_user_id, event_category, subject_reference_id, resource_type, resource_id, payload, occurred_at, created_at, severity, status
    )
    SELECT e.evidence_id, e.actor_user_id, 'PREGNANCY_OUTCOME_EVIDENCE', e.journey_id, 'mother_journeys', e.journey_id,
      jsonb_build_object(
        'outcomeType', e.outcome_type,
        'outcomeDate', e.outcome_date,
        'source', e.source,
        'reason', e.reason,
        'revisionNumber', e.revision_number,
        'supersedesEvidenceId', e.supersedes_evidence_id,
        'semanticHash', e.semantic_hash,
        'correction', e.correction,
        'submissionId', e.submission_id,
        'legacySource', 'pregnancy_outcome_evidence'
      ), e.effective_at, e.recorded_at, 'INFO', 'CLOSED'
    FROM public.pregnancy_outcome_evidence e
    ON CONFLICT (audit_event_id) DO NOTHING;
  END IF;

  IF to_regclass('public.baby_link_submissions') IS NOT NULL THEN
    INSERT INTO public.audit_events (
      actor_user_id, event_category, subject_reference_id, resource_type, resource_id, payload, occurred_at, created_at, severity, status
    )
    SELECT b.owner_user_id, 'BABY_LINK_'||b.operation_type, b.journey_id, 'care_subjects', b.baby_id,
      jsonb_build_object(
        'submissionId', b.submission_id,
        'operationType', b.operation_type,
        'semanticIntent', b.semantic_intent,
        'legacySource', 'baby_link_submissions'
      ), b.created_at, b.created_at, 'INFO', 'CLOSED'
    FROM public.baby_link_submissions b
    ON CONFLICT DO NOTHING;
  END IF;
END $wave2_events$;

-- Rebuild observation subtypes inside health_observations.
DELETE FROM public.health_observations WHERE legacy_source IN (
  'maternal_health_metrics','postpartum_logs','exercise_safety_checks','posture_feedback_events');

DO $wave2_observations$
BEGIN
  IF to_regclass('public.maternal_health_metrics') IS NOT NULL THEN
    INSERT INTO public.health_observations (
      health_observation_id,care_subject_id,subject_type,observation_type,value_numeric,value_secondary,
      unit,text_value,observed_at,raw_payload_jsonb,source_type,legacy_source,legacy_id,
      created_at,updated_at)
    SELECT m.metric_id,m.journey_id,'MOTHER',m.metric_type,m.value_numeric,m.value_secondary,m.unit,m.note,
      m.measured_at,jsonb_build_object('sourceReferenceId',m.source_reference_id,'recordStatus',m.status),'MANUAL',
      'maternal_health_metrics',m.metric_id::text,m.created_at,m.updated_at
    FROM public.maternal_health_metrics m
    ON CONFLICT (legacy_source,legacy_id) DO NOTHING;
  END IF;

  IF to_regclass('public.postpartum_logs') IS NOT NULL THEN
    INSERT INTO public.health_observations (
      health_observation_id,care_subject_id,subject_type,observation_type,value_numeric,value_secondary,
      unit,text_value,severity,observed_at,raw_payload_jsonb,source_type,legacy_source,legacy_id,
      created_at,updated_at)
    SELECT p.postpartum_log_id,p.journey_id,'MOTHER','POSTPARTUM_LOG',p.pain_level,p.sleep_hours,
      'MIXED',p.symptom_note,p.bleeding_level,p.log_date::timestamptz,
      jsonb_build_object('submissionId',p.submission_id,'moodLevel',p.mood_level,'breastfeedingNote',p.breastfeeding_note,'recordStatus',p.status),
      'POSTPARTUM_LOG','postpartum_logs',p.postpartum_log_id::text,p.created_at,p.updated_at
    FROM public.postpartum_logs p
    ON CONFLICT (legacy_source,legacy_id) DO NOTHING;
  END IF;

  IF to_regclass('public.exercise_safety_checks') IS NOT NULL THEN
    INSERT INTO public.health_observations (
      health_observation_id,care_subject_id,subject_type,observation_type,text_value,observed_at,raw_payload_jsonb,
      source_type,legacy_source,legacy_id,created_at)
    SELECT s.safety_check_id,s.journey_id,'MOTHER','EXERCISE_SAFETY_RESULT',s.blocked_reason,
      coalesce(s.completed_at,s.created_at),
      jsonb_build_object('exerciseTemplateId',s.exercise_id,'ownerUserId',s.user_id,'blockedBoolean',s.red_flag_detected,'recordStatus',s.result_status),
      'EXERCISE_SAFETY','exercise_safety_checks',s.safety_check_id::text,s.created_at
    FROM public.exercise_safety_checks s
    ON CONFLICT (legacy_source,legacy_id) DO NOTHING;

    INSERT INTO public.health_observations (
      health_observation_id,care_subject_id,subject_type,observation_type,observed_at,raw_payload_jsonb,
      source_type,legacy_source,legacy_id,created_at)
    SELECT (substr(md5('safety-answer:'||s.safety_check_id||':'||a.key),1,8)||'-'||
      substr(md5('safety-answer:'||s.safety_check_id||':'||a.key),9,4)||'-'||
      substr(md5('safety-answer:'||s.safety_check_id||':'||a.key),13,4)||'-'||
      substr(md5('safety-answer:'||s.safety_check_id||':'||a.key),17,4)||'-'||
      substr(md5('safety-answer:'||s.safety_check_id||':'||a.key),21,12))::uuid,
      s.journey_id,'MOTHER','EXERCISE_SAFETY_CHECK',coalesce(s.completed_at,s.created_at),
      jsonb_build_object('exerciseTemplateId',s.exercise_id,'ownerUserId',s.user_id,'checkCode',a.key,'responseBoolean',a.value::boolean,'blockedBoolean',s.red_flag_detected,'recordStatus',s.result_status),
      'EXERCISE_SAFETY','exercise_safety_checks_answer',s.safety_check_id::text||':'||a.key,s.created_at
    FROM public.exercise_safety_checks s CROSS JOIN LATERAL jsonb_each_text(coalesce(s.answer_json,'{}'::jsonb)) a
    ON CONFLICT (legacy_source,legacy_id) DO NOTHING;
  END IF;

  IF to_regclass('public.posture_feedback_events') IS NOT NULL THEN
    INSERT INTO public.health_observations (
      health_observation_id,observation_type,care_subject_id,subject_type,source_record_id,value_numeric,
      unit,text_value,severity,observed_at,raw_payload_jsonb,source_type,legacy_source,legacy_id,created_at)
    SELECT f.feedback_event_id,'POSTURE_FEEDBACK',s.mother_journey_id,'MOTHER',f.exercise_session_id,f.confidence_score,
      'CONFIDENCE',f.feedback_text,f.severity,s.started_at+make_interval(secs=>f.event_time_ms/1000.0),
      jsonb_build_object('eventTimeMs',f.event_time_ms,'postureConfigId',f.posture_config_id,'postureCode',f.posture_code),
      'POSTURE_ANALYSIS','posture_feedback_events',f.feedback_event_id::text,f.created_at
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
    SELECT count(*) INTO target_count FROM public.audit_events WHERE event_category='BASELINE_CONTEXT';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: mother baseline %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.mother_journey_transitions') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.mother_journey_transitions;
    SELECT count(*) INTO target_count FROM public.audit_events WHERE event_category='MOTHER_JOURNEY_TRANSITION';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: journey transition %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.pregnancy_outcome_evidence') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.pregnancy_outcome_evidence;
    SELECT count(*) INTO target_count FROM public.audit_events WHERE event_category='PREGNANCY_OUTCOME_EVIDENCE';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: outcome evidence %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.baby_link_submissions') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.baby_link_submissions;
    SELECT count(*) INTO target_count FROM public.audit_events WHERE event_category LIKE 'BABY_LINK_%';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: baby links %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.maternal_health_metrics') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.maternal_health_metrics;
    SELECT count(*) INTO target_count FROM public.health_observations WHERE legacy_source='maternal_health_metrics';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: maternal metrics %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.postpartum_logs') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.postpartum_logs;
    SELECT count(*) INTO target_count FROM public.health_observations WHERE legacy_source='postpartum_logs';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: postpartum logs %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.exercise_safety_checks') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.exercise_safety_checks;
    SELECT count(*) INTO target_count FROM public.health_observations WHERE legacy_source='exercise_safety_checks';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: safety results %/%',source_count,target_count; END IF;
    SELECT count(*) INTO source_count FROM public.exercise_safety_checks s
      CROSS JOIN LATERAL jsonb_each(coalesce(s.answer_json,'{}'::jsonb));
    SELECT count(*) INTO target_count FROM public.health_observations WHERE legacy_source='exercise_safety_checks_answer';
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: safety answers %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.exercise_sessions') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.exercise_sessions;
    SELECT count(*) INTO target_count FROM public.exercise_sessions s JOIN public.maternal_exercise_sessions t ON t.exercise_session_id=s.exercise_session_id;
    IF source_count<>target_count THEN RAISE EXCEPTION 'WAVE2_RECONCILIATION: exercise sessions %/%',source_count,target_count; END IF;
  END IF;
  IF to_regclass('public.posture_feedback_events') IS NOT NULL THEN
    SELECT count(*) INTO source_count FROM public.posture_feedback_events;
    SELECT count(*) INTO target_count FROM public.health_observations WHERE legacy_source='posture_feedback_events';
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
    ('exercise_sessions','maternal_exercise_sessions','exercise_session_id','exercise_session_id'),
    ('posture_analysis_configs','care_item_templates','posture_config_id','template_id'),
    ('pregnancy_exercises','care_item_templates','exercise_id','template_id'),
    ('baby_daily_logs','care_logs','baby_log_id','care_log_id'),
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
      FOREIGN KEY (safety_observation_id) REFERENCES public.health_observations(health_observation_id);
  END IF;
END $wave2_canonical_fks$;

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
    IF legacy IS NOT NULL AND EXISTS (
      SELECT 1 FROM pg_constraint 
       WHERE confrelid=legacy AND conrelid <> confrelid
         AND replace(conrelid::regclass::text, 'public.', '') NOT IN (
           'mother_baseline_contexts','mother_journey_transitions','pregnancy_outcome_evidence',
           'maternal_health_metrics','postpartum_logs','exercise_safety_checks','exercise_sessions',
           'posture_feedback_events','posture_analysis_configs','pregnancy_exercises','baby_daily_logs',
           'baby_link_submissions','vaccination_reference_schedules'
         )
    ) THEN
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

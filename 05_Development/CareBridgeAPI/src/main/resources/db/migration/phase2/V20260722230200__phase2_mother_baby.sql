-- Phase 2 wave 2: mother journey, maternal observations and baby care.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

ALTER TABLE public.care_subjects DROP CONSTRAINT IF EXISTS care_subjects_owner_person_uk;

CREATE TABLE IF NOT EXISTS public.mother_journeys (
    journey_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    care_subject_id uuid,
    owner_user_id uuid NOT NULL REFERENCES public.users(user_id),
    journey_type varchar(30) NOT NULL,
    start_date date,
    last_menstrual_date date,
    estimated_due_date date,
    delivery_date date,
    status varchar(30) NOT NULL,
    notes text,
    version bigint NOT NULL DEFAULT 0,
    date_source varchar(50),
    date_confidence varchar(30),
    pregnancy_outcome varchar(50),
    pregnancy_outcome_date date,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.mother_journeys ADD COLUMN IF NOT EXISTS care_subject_id uuid;

INSERT INTO public.care_subjects (
    care_subject_id, person_id, owner_user_id, mother_journey_id, subject_type,
    nickname, status, created_at, updated_at
)
SELECT mj.journey_id, u.user_id, mj.owner_user_id, mj.journey_id, 'MOTHER',
       u.display_name, mj.status, mj.created_at, mj.updated_at
  FROM public.mother_journeys mj
  JOIN public.users u ON u.user_id = mj.owner_user_id
ON CONFLICT (care_subject_id) DO NOTHING;

UPDATE public.mother_journeys SET care_subject_id = journey_id WHERE care_subject_id IS NULL;
DO $baby_journey_link$
BEGIN
    IF to_regclass('public.baby_profiles') IS NOT NULL THEN
        UPDATE public.care_subjects cs
           SET mother_journey_id = bp.related_journey_id
          FROM public.baby_profiles bp
         WHERE cs.care_subject_id = bp.baby_id
           AND cs.mother_journey_id IS NULL;
    END IF;
END
$baby_journey_link$;

DO $journey_constraints$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'mother_journeys_subject_fk') THEN
        ALTER TABLE public.mother_journeys ADD CONSTRAINT mother_journeys_subject_fk
            FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'mother_journeys_subject_uk') THEN
        ALTER TABLE public.mother_journeys ADD CONSTRAINT mother_journeys_subject_uk UNIQUE (care_subject_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'care_subjects_journey_fk') THEN
        ALTER TABLE public.care_subjects ADD CONSTRAINT care_subjects_journey_fk
            FOREIGN KEY (mother_journey_id) REFERENCES public.mother_journeys(journey_id);
    END IF;
END
$journey_constraints$;
ALTER TABLE public.mother_journeys ALTER COLUMN care_subject_id SET NOT NULL;

CREATE TABLE IF NOT EXISTS public.health_observations (
    health_observation_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    care_subject_id uuid NOT NULL REFERENCES public.care_subjects(care_subject_id),
    device_connection_id uuid, -- FK added in Wave 5
    subject_type varchar(30) NOT NULL,
    observation_type varchar(60) NOT NULL,
    value_numeric numeric,
    value_secondary numeric,
    unit varchar(40),
    text_value text,
    severity varchar(30),
    observed_at timestamptz NOT NULL,
    source_record_id uuid,
    source_type varchar(60) NOT NULL DEFAULT 'SYSTEM',
    quality_label varchar(30),
    raw_payload_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    legacy_source varchar(60),
    legacy_id varchar(100),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT health_observations_legacy_uk UNIQUE (legacy_source, legacy_id),
    CONSTRAINT health_observations_type_ck CHECK (subject_type IN ('MOTHER', 'BABY', 'DEPENDENT'))
);
CREATE INDEX IF NOT EXISTS health_observations_subject_chart_ix
    ON public.health_observations(care_subject_id, observation_type, observed_at);
CREATE INDEX IF NOT EXISTS health_observations_severity_ix
    ON public.health_observations(severity, observed_at) WHERE severity IS NOT NULL;

CREATE TABLE IF NOT EXISTS public.maternal_exercise_sessions (
    exercise_session_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    mother_journey_id uuid REFERENCES public.mother_journeys(journey_id),
    owner_user_id uuid NOT NULL REFERENCES public.users(user_id),
    exercise_template_id uuid NOT NULL,
    posture_config_id uuid,
    started_at timestamptz NOT NULL,
    ended_at timestamptz,
    paused_seconds integer NOT NULL DEFAULT 0,
    completion_percent numeric(5,2),
    posture_score numeric(6,3),
    session_status varchar(30) NOT NULL,
    warning_count integer NOT NULL DEFAULT 0,
    summary_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

DO $exercise_mapping$
BEGIN
    IF to_regclass('public.exercise_sessions') IS NOT NULL THEN
        INSERT INTO public.maternal_exercise_sessions (
            exercise_session_id, mother_journey_id, owner_user_id, exercise_template_id,
            started_at, ended_at, paused_seconds, completion_percent, posture_score,
            session_status, warning_count, summary_jsonb, created_at, updated_at
        )
        SELECT es.exercise_session_id, es.journey_id, es.user_id, es.exercise_id,
               es.started_at, es.ended_at, es.paused_seconds, es.completion_percent,
               es.posture_score, es.session_status, es.warning_count,
               coalesce(es.summary_json, '{}'::jsonb), es.created_at, es.updated_at
          FROM public.exercise_sessions es
        ON CONFLICT (exercise_session_id) DO NOTHING;

        IF (SELECT count(*) FROM public.exercise_sessions) <>
           (SELECT count(*) FROM public.maternal_exercise_sessions) THEN
            RAISE EXCEPTION 'PHASE2_RECONCILIATION: exercise_sessions mismatch';
        END IF;
    END IF;
END
$exercise_mapping$;

DO $event_mapping$
BEGIN
    IF to_regclass('public.mother_journey_transitions') IS NOT NULL THEN
        INSERT INTO public.audit_events (
            audit_event_id, actor_user_id, event_category, subject_reference_id,
            resource_type, resource_id, payload, occurred_at, created_at,
            severity, status
        )
        SELECT t.transition_id, t.actor_user_id, 'MOTHER_JOURNEY_TRANSITION', t.journey_id,
               'mother_journeys', t.journey_id,
               jsonb_build_object(
                   'transitionId', t.transition_id,
                   'fromStage', t.from_stage,
                   'toStage', t.to_stage,
                   'changes', t.changes_json,
                   'journeyVersion', t.journey_version,
                   'legacySource', 'mother_journey_transitions'
               ), t.effective_at, t.recorded_at,
               'INFO', 'CLOSED'
          FROM public.mother_journey_transitions t
          JOIN public.mother_journeys j ON j.journey_id = t.journey_id
        ON CONFLICT (audit_event_id) DO NOTHING;
    END IF;

    IF to_regclass('public.pregnancy_outcome_evidence') IS NOT NULL THEN
        INSERT INTO public.audit_events (
            audit_event_id, actor_user_id, event_category, subject_reference_id,
            resource_type, resource_id, payload, occurred_at, created_at,
            severity, status
        )
        SELECT e.evidence_id, e.actor_user_id, 'PREGNANCY_OUTCOME_EVIDENCE', e.journey_id,
               'mother_journeys', e.journey_id,
               jsonb_build_object(
                   'outcomeType', e.outcome_type,
                   'outcomeDate', e.outcome_date,
                   'source', e.source,
                   'reason', e.reason,
                   'revisionNumber', e.revision_number,
                   'supersedesEvidenceId', e.supersedes_evidence_id,
                   'semanticHash', e.semantic_hash,
                   'correction', e.correction,
                   'legacySource', 'pregnancy_outcome_evidence'
               ), e.effective_at, e.recorded_at,
               'INFO', 'CLOSED'
          FROM public.pregnancy_outcome_evidence e
        ON CONFLICT (audit_event_id) DO NOTHING;
    END IF;

    IF to_regclass('public.mother_baseline_contexts') IS NOT NULL THEN
        INSERT INTO public.audit_events (
            actor_user_id, event_category, subject_reference_id,
            resource_type, resource_id, payload, occurred_at, created_at,
            severity, status
        )
        SELECT b.owner_user_id, 'BASELINE_CONTEXT', j.journey_id,
               'mother_journeys', j.journey_id,
               jsonb_build_object(
                   'baselineId', b.baseline_id,
                   'lifecycleGoal', b.lifecycle_goal,
                   'locale', b.locale,
                   'timeZone', b.time_zone,
                   'preferences', b.preferences,
                   'source', b.source,
                   'legacySource', 'mother_baseline_contexts'
               ), b.recorded_at, b.recorded_at,
               'INFO', 'CLOSED'
          FROM public.mother_baseline_contexts b
          LEFT JOIN LATERAL (
              SELECT mj.journey_id FROM public.mother_journeys mj
               WHERE mj.owner_user_id = b.owner_user_id
               ORDER BY mj.created_at DESC, mj.journey_id LIMIT 1
          ) j ON true
        ON CONFLICT DO NOTHING;
    END IF;

    IF to_regclass('public.baby_link_submissions') IS NOT NULL THEN
        INSERT INTO public.audit_events (
            actor_user_id, event_category, subject_reference_id,
            resource_type, resource_id, payload, occurred_at, created_at,
            severity, status
        )
        SELECT bl.owner_user_id, 'BABY_LINK_' || bl.operation_type, bl.journey_id,
               'care_subjects', bl.baby_id,
               jsonb_build_object(
                   'submissionId', bl.submission_id,
                   'semanticIntent', bl.semantic_intent,
                   'legacySource', 'baby_link_submissions'
               ), bl.created_at, bl.created_at,
               'INFO', 'CLOSED'
          FROM public.baby_link_submissions bl
        ON CONFLICT DO NOTHING;
    END IF;
END
$event_mapping$;

DO $observation_mapping$
BEGIN
    IF to_regclass('public.maternal_health_metrics') IS NOT NULL THEN
        INSERT INTO public.health_observations (
            health_observation_id, care_subject_id, subject_type, observation_type,
            value_numeric, value_secondary, unit, text_value, observed_at, raw_payload_jsonb,
            source_type, legacy_source, legacy_id, created_at
        )
        SELECT m.metric_id, m.journey_id, 'MOTHER', 'MATERNAL_METRIC', m.value_numeric,
               m.value_secondary, m.unit, m.note, m.measured_at,
               jsonb_build_object('metricType', m.metric_type, 'status', m.status,
                                  'sourceReferenceId', m.source_reference_id),
               coalesce(m.source_type, 'LEGACY'), 'maternal_health_metrics',
               m.metric_id::text, m.created_at
          FROM public.maternal_health_metrics m
        ON CONFLICT (legacy_source, legacy_id) DO NOTHING;
    END IF;

    IF to_regclass('public.postpartum_logs') IS NOT NULL THEN
        INSERT INTO public.health_observations (
            health_observation_id, care_subject_id, subject_type, observation_type,
            value_numeric, value_secondary, unit, text_value, severity, observed_at,
            raw_payload_jsonb, source_type, legacy_source, legacy_id, created_at
        )
        SELECT p.postpartum_log_id, p.journey_id, 'MOTHER', 'POSTPARTUM_LOG', p.pain_level,
               p.sleep_hours, 'MIXED', p.symptom_note, p.bleeding_level,
               p.log_date::timestamptz,
               jsonb_build_object('moodLevel', p.mood_level,
                                  'breastfeedingNote', p.breastfeeding_note,
                                  'submissionId', p.submission_id, 'status', p.status),
               'POSTPARTUM_LOG', 'postpartum_logs', p.postpartum_log_id::text, p.created_at
          FROM public.postpartum_logs p
        ON CONFLICT (legacy_source, legacy_id) DO NOTHING;
    END IF;

    IF to_regclass('public.exercise_safety_checks') IS NOT NULL THEN
        INSERT INTO public.health_observations (
            health_observation_id, care_subject_id, subject_type, observation_type,
            text_value, severity, observed_at, raw_payload_jsonb, source_type,
            legacy_source, legacy_id, created_at
        )
        SELECT s.safety_check_id, s.journey_id, 'MOTHER',
               CASE WHEN s.red_flag_detected THEN 'EXERCISE_SAFETY_BLOCK'
                    ELSE 'EXERCISE_SAFETY_ANSWER' END,
               s.blocked_reason,
               CASE WHEN s.red_flag_detected THEN 'HIGH' ELSE NULL END,
               coalesce(s.completed_at, s.created_at),
               jsonb_build_object('exerciseId', s.exercise_id, 'userId', s.user_id,
                                  'answer', s.answer_json, 'resultStatus', s.result_status),
               'EXERCISE_SAFETY', 'exercise_safety_checks', s.safety_check_id::text, s.created_at
          FROM public.exercise_safety_checks s
        ON CONFLICT (legacy_source, legacy_id) DO NOTHING;
    END IF;

    IF to_regclass('public.posture_feedback_events') IS NOT NULL THEN
        INSERT INTO public.health_observations (
            health_observation_id, care_subject_id, subject_type, observation_type,
            source_record_id, value_numeric, unit, text_value, severity, observed_at,
            raw_payload_jsonb, source_type, legacy_source, legacy_id, created_at
        )
        SELECT f.feedback_event_id, es.mother_journey_id, 'MOTHER', 'POSTURE_FEEDBACK',
               f.exercise_session_id, f.confidence_score, 'CONFIDENCE', f.feedback_text,
               f.severity, es.started_at + make_interval(secs => f.event_time_ms / 1000.0),
               jsonb_build_object('postureConfigId', f.posture_config_id,
                                  'postureCode', f.posture_code,
                                  'keypointSummary', f.keypoint_summary_json),
               'POSTURE_ANALYSIS', 'posture_feedback_events', f.feedback_event_id::text, f.created_at
          FROM public.posture_feedback_events f
          JOIN public.maternal_exercise_sessions es
            ON es.exercise_session_id = f.exercise_session_id
        ON CONFLICT (legacy_source, legacy_id) DO NOTHING;
    END IF;
END
$observation_mapping$;

DO $observation_fk$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'health_observations_session_fk') THEN
        ALTER TABLE public.health_observations ADD CONSTRAINT health_observations_session_fk
            FOREIGN KEY (source_record_id)
            REFERENCES public.maternal_exercise_sessions(exercise_session_id);
    END IF;
END
$observation_fk$;

CREATE TABLE IF NOT EXISTS public.care_logs (
    care_log_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    care_subject_id uuid NOT NULL REFERENCES public.care_subjects(care_subject_id),
    log_type varchar(40) NOT NULL,
    started_at timestamptz,
    ended_at timestamptz,
    quantity numeric,
    unit varchar(30),
    note text,
    recorded_by uuid REFERENCES public.users(user_id),
    status varchar(30) NOT NULL,
    payload_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS care_logs_subject_type_time_ix
    ON public.care_logs(care_subject_id, log_type, started_at);

DO $care_log_mapping$
BEGIN
    IF to_regclass('public.baby_daily_logs') IS NOT NULL THEN
        INSERT INTO public.care_logs (
            care_log_id, care_subject_id, log_type, started_at, ended_at, quantity,
            unit, note, recorded_by, status, created_at, updated_at
        )
        SELECT b.baby_log_id, b.baby_id, b.log_type, b.started_at, b.ended_at,
               b.quantity, b.unit, b.note, b.recorded_by, b.status, b.created_at, b.updated_at
          FROM public.baby_daily_logs b
        ON CONFLICT (care_log_id) DO NOTHING;
        IF (SELECT count(*) FROM public.baby_daily_logs) <>
           (SELECT count(*) FROM public.care_logs) THEN
            RAISE EXCEPTION 'PHASE2_RECONCILIATION: baby_daily_logs mismatch';
        END IF;
    END IF;
END
$care_log_mapping$;

CREATE TABLE IF NOT EXISTS public.growth_measurements (
    growth_measurement_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    care_subject_id uuid,
    baby_id uuid,
    measured_date date NOT NULL,
    weight_kg numeric,
    height_cm numeric,
    head_circumference_cm numeric,
    source_type varchar(40),
    note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);
ALTER TABLE public.growth_measurements ADD COLUMN IF NOT EXISTS care_subject_id uuid;
UPDATE public.growth_measurements SET care_subject_id = baby_id WHERE care_subject_id IS NULL;

CREATE TABLE IF NOT EXISTS public.development_milestones (
    milestone_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    care_subject_id uuid,
    baby_id uuid,
    milestone_type varchar(60) NOT NULL,
    achieved_date date,
    note text,
    source_type varchar(40),
    recorded_by uuid REFERENCES public.users(user_id),
    milestone_status varchar(30) NOT NULL DEFAULT 'RECORDED',
    record_status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.development_milestones ADD COLUMN IF NOT EXISTS care_subject_id uuid;
UPDATE public.development_milestones SET care_subject_id = baby_id WHERE care_subject_id IS NULL;

CREATE TABLE IF NOT EXISTS public.vaccination_schedules (
    vaccination_schedule_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    vaccine_name varchar(200) NOT NULL,
    dose_number smallint NOT NULL,
    offset_days integer NOT NULL,
    description text,
    schedule_version varchar(30) NOT NULL DEFAULT '1',
    active_from date,
    active_to date,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT vaccination_schedules_key_uk
        UNIQUE (vaccine_name, dose_number, schedule_version)
);

DO $schedule_mapping$
BEGIN
    IF to_regclass('public.vaccination_reference_schedules') IS NOT NULL THEN
        INSERT INTO public.vaccination_schedules (
            vaccination_schedule_id, vaccine_name, dose_number, offset_days,
            description, schedule_version, created_at
        )
        SELECT v.ref_id, v.vaccine_name, v.dose_number, v.offset_days,
               v.description, 'legacy-1', v.created_at
          FROM public.vaccination_reference_schedules v
        ON CONFLICT (vaccination_schedule_id) DO NOTHING;
    END IF;
END
$schedule_mapping$;

CREATE TABLE IF NOT EXISTS public.vaccination_records (
    vaccination_record_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    care_subject_id uuid,
    baby_id uuid,
    vaccination_schedule_id uuid REFERENCES public.vaccination_schedules(vaccination_schedule_id),
    vaccine_name varchar(200) NOT NULL,
    dose_number smallint,
    scheduled_date date,
    administered_date date,
    status varchar(30) NOT NULL,
    facility_name varchar(255),
    proof_record_id uuid,
    postpone_reason text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE public.vaccination_records ADD COLUMN IF NOT EXISTS care_subject_id uuid;
ALTER TABLE public.vaccination_records ADD COLUMN IF NOT EXISTS vaccination_schedule_id uuid;
UPDATE public.vaccination_records SET care_subject_id = baby_id WHERE care_subject_id IS NULL;
UPDATE public.vaccination_records vr
   SET vaccination_schedule_id = vs.vaccination_schedule_id
  FROM public.vaccination_schedules vs
 WHERE vr.vaccination_schedule_id IS NULL
   AND lower(vr.vaccine_name) = lower(vs.vaccine_name)
   AND coalesce(vr.dose_number, 0) = coalesce(vs.dose_number, 0);

DO $baby_constraints$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'growth_measurements_subject_fk') THEN
        ALTER TABLE public.growth_measurements ADD CONSTRAINT growth_measurements_subject_fk
            FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'development_milestones_subject_fk') THEN
        ALTER TABLE public.development_milestones ADD CONSTRAINT development_milestones_subject_fk
            FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'vaccination_records_subject_fk') THEN
        ALTER TABLE public.vaccination_records ADD CONSTRAINT vaccination_records_subject_fk
            FOREIGN KEY (care_subject_id) REFERENCES public.care_subjects(care_subject_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'vaccination_records_schedule_fk') THEN
        ALTER TABLE public.vaccination_records ADD CONSTRAINT vaccination_records_schedule_fk
            FOREIGN KEY (vaccination_schedule_id)
            REFERENCES public.vaccination_schedules(vaccination_schedule_id);
    END IF;
END
$baby_constraints$;

ALTER TABLE public.growth_measurements ALTER COLUMN care_subject_id SET NOT NULL;
ALTER TABLE public.development_milestones ALTER COLUMN care_subject_id SET NOT NULL;
ALTER TABLE public.vaccination_records ALTER COLUMN care_subject_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS growth_measurements_chart_ix
    ON public.growth_measurements(care_subject_id, measured_date) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS development_milestones_subject_ix
    ON public.development_milestones(care_subject_id, milestone_type, achieved_date);
CREATE INDEX IF NOT EXISTS vaccination_records_subject_status_ix
    ON public.vaccination_records(care_subject_id, status, scheduled_date);



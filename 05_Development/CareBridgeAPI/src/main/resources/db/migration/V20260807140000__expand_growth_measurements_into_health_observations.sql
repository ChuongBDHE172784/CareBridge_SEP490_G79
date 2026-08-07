-- Wave 13 expand step: define the baby growth metrics and backfill growth_measurements
-- into health_observations.
--
-- Mapping decisions and their evidence: 08_References/Wave13_Growth_To_Observations_Mapping.md
-- Prerequisites 1-3 shipped in V20260807130000.
--
-- This is expand only. growth_measurements is not read, written or dropped here, and no
-- application query changes — the rows land in health_observations and wait for the cutover
-- release. Dropping the source table happens later, after a fresh idempotent rerun catches
-- anything written into it between this migration and the cutover deploy.

-- ---------------------------------------------------------------------------
-- 1. Metric definitions for the three baby measurements
-- ---------------------------------------------------------------------------
-- Distinct codes rather than reusing WEIGHT/HEIGHT. health_metric_definitions is unique on
-- (metric_code, version), so a BABY 'WEIGHT' would collide with the MOTHER 'WEIGHT' that
-- already exists — and, more importantly, an observation_type shared between mother and baby
-- lets a query that forgets subject_type silently plot a 3.4 kg newborn against a 60 kg
-- adult. Distinct types make that mistake impossible rather than merely unlikely.
INSERT INTO public.health_metric_definitions (
    metric_definition_id, metric_code, version, display_name, observation_shape,
    subject_type, manual_entry_supported, device_import_supported, canonical_unit,
    accepted_input_units_jsonb, precision_scale, is_active)
SELECT gen_random_uuid(), code, 1, display, 'POINT', 'BABY', true, false, unit,
       units, 2, true
  FROM (VALUES
        ('BABY_WEIGHT', 'Cân nặng của bé', 'kg', '["kg","g"]'::jsonb),
        ('BABY_HEIGHT', 'Chiều dài/chiều cao của bé', 'cm', '["cm"]'::jsonb),
        ('BABY_HEAD_CIRCUMFERENCE', 'Chu vi vòng đầu của bé', 'cm', '["cm"]'::jsonb)
       ) AS m(code, display, unit, units)
 WHERE NOT EXISTS (
        SELECT 1 FROM public.health_metric_definitions d
         WHERE d.metric_code = m.code AND d.version = 1);

-- ---------------------------------------------------------------------------
-- 2. Backfill — one growth row becomes up to three observations
-- ---------------------------------------------------------------------------
-- Idempotent through health_observations_legacy_uk (legacy_source, legacy_id), where
-- legacy_id is '<growth_measurement_id>:<TYPE>' exactly as V3 §3.12 specifies. Rerunning
-- this migration, or running it again before the contract step, inserts nothing new.
--
-- Only non-null measurements produce a row: a session that recorded weight alone must not
-- invent a height observation of zero.
--
-- Guard first: every source row must point at a care subject that really is a BABY, because
-- subject_type is written as 'BABY' unconditionally below.
DO $$
DECLARE
    v_bad bigint;
BEGIN
    SELECT count(*) INTO v_bad
      FROM public.growth_measurements g
      LEFT JOIN public.care_subjects cs ON cs.care_subject_id = g.care_subject_id
     WHERE cs.care_subject_id IS NULL OR cs.subject_type <> 'BABY';

    IF v_bad > 0 THEN
        RAISE EXCEPTION
            'WAVE13_BACKFILL_BLOCKED: % growth row(s) whose care subject is missing or not a BABY',
            v_bad;
    END IF;
END
$$;

INSERT INTO public.health_observations (
    health_observation_id, care_subject_id, observation_type, subject_type,
    value_numeric, unit, observed_at, source_type, context_jsonb,
    measurement_group_id, legacy_source, legacy_id,
    created_at, updated_at, deleted_at)
SELECT gen_random_uuid(),
       g.care_subject_id,
       m.observation_type,
       'BABY',
       m.value,
       m.unit,
       -- measured_date is a date; anchor it at local midnight so the calendar day survives
       -- the round trip through timestamptz.
       (g.measured_date::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh'),
       -- The application-facing source vocabulary. growth's own source_type answers a
       -- different question and is preserved in context_jsonb instead.
       'MANUAL',
       jsonb_strip_nulls(jsonb_build_object(
           'measurementSetting', g.source_type,
           'note', g.note)),
       g.growth_measurement_id,
       'growth_measurements',
       g.growth_measurement_id || ':' || m.observation_type,
       g.created_at,
       g.updated_at,
       g.deleted_at
  FROM public.growth_measurements g
 CROSS JOIN LATERAL (VALUES
        ('BABY_WEIGHT', g.weight_kg, 'kg'),
        ('BABY_HEIGHT', g.height_cm, 'cm'),
        ('BABY_HEAD_CIRCUMFERENCE', g.head_circumference_cm, 'cm')
       ) AS m(observation_type, value, unit)
 WHERE m.value IS NOT NULL
   AND NOT EXISTS (
        SELECT 1 FROM public.health_observations o
         WHERE o.legacy_source = 'growth_measurements'
           AND o.legacy_id = g.growth_measurement_id || ':' || m.observation_type);

-- ---------------------------------------------------------------------------
-- 3. Verify — every non-null measurement is represented, with the right value
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_expected bigint;
    v_actual bigint;
    v_divergent bigint;
BEGIN
    SELECT count(*) INTO v_expected
      FROM public.growth_measurements g
     CROSS JOIN LATERAL (VALUES (g.weight_kg), (g.height_cm), (g.head_circumference_cm))
           AS m(value)
     WHERE m.value IS NOT NULL;

    SELECT count(*) INTO v_actual
      FROM public.health_observations
     WHERE legacy_source = 'growth_measurements';

    IF v_expected <> v_actual THEN
        RAISE EXCEPTION
            'WAVE13_BACKFILL_FAILED: expected % observation(s) from growth, found %',
            v_expected, v_actual;
    END IF;

    -- Values, grouping and soft-delete state must match the source, not merely the count.
    SELECT count(*) INTO v_divergent
      FROM public.growth_measurements g
      JOIN public.health_observations o
        ON o.measurement_group_id = g.growth_measurement_id
     WHERE o.care_subject_id IS DISTINCT FROM g.care_subject_id
        OR o.deleted_at IS DISTINCT FROM g.deleted_at
        OR (o.observation_type = 'BABY_WEIGHT' AND o.value_numeric IS DISTINCT FROM g.weight_kg)
        OR (o.observation_type = 'BABY_HEIGHT' AND o.value_numeric IS DISTINCT FROM g.height_cm)
        OR (o.observation_type = 'BABY_HEAD_CIRCUMFERENCE'
            AND o.value_numeric IS DISTINCT FROM g.head_circumference_cm)
        OR (o.observed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date IS DISTINCT FROM g.measured_date;

    IF v_divergent > 0 THEN
        RAISE EXCEPTION
            'WAVE13_BACKFILL_FAILED: % observation(s) diverge from their source growth row',
            v_divergent;
    END IF;
END
$$;

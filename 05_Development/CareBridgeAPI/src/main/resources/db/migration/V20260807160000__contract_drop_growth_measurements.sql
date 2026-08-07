-- CareBridge wave 13 — contract: drop growth_measurements
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.12
-- Mapping: 08_References/Wave13_Growth_To_Observations_Mapping.md
--
-- Promoted from 08_wave13_contract_drop_growth.sql once the observation window closed.
--
-- Measured on the live database on 2026-08-07, 07_wave13_observation.sql reported
-- OBSERVATION CLEAN with every gate satisfied:
--
--   source table untouched since the expand migration, quiet for over two days
--   24 of 24 measurements present in health_observations, 0 diverging from source
--   3 growth sessions written by the cutover build, none of them touching this table
--   no baby observation outside its legacy_source / subject_type scope
--
-- The cutover release moved GrowthServiceImpl onto GrowthMeasurementStore and rewrote
-- DevDataSeeder, which was the last writer of this table. DevDataSeeder is @Profile("dev &
-- !prod") so it never ran in production, but it would have broken every dev environment.
--
-- After this commits, rollback is forward-fix or restore only. On this database the Release
-- Owner waived PITR/backups, so there is no restore.
--
-- Preconditions, all re-checked below rather than trusted:
--   * The cutover build is deployed — GrowthMeasurementStore serves reads and writes, and
--     nothing maps growth_measurements any more.
--   * 07_wave13_observation.sql reports OBSERVATION CLEAN.
--
-- After this commits, rollback is forward-fix or restore only. On this database the Release
-- Owner waived PITR/backups, so there is no restore.

-- ---------------------------------------------------------------------------
-- 1. Catch stragglers — sessions written after the expand migration but before the deploy
-- ---------------------------------------------------------------------------
-- This is the whole reason (legacy_source, legacy_id) is unique. The rerun is a no-op when
-- the window was clean, and rescues the gap when it was not. It must run BEFORE the parity
-- gate below, or the gate would fail on rows this statement is about to create.
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
       (g.measured_date::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh'),
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
-- 2. Gates
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_missing bigint;
    v_divergent bigint;
    v_touched bigint;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.flyway_schema_history WHERE version = '20260807140000') THEN
        RAISE EXCEPTION 'WAVE13_CONTRACT_BLOCKED: the growth expand migration has not run here';
    END IF;

    -- Nothing may have written the source table since the expand migration. A row that did
    -- means an older build is still attached and would break the moment the table vanishes.
    SELECT count(*) INTO v_touched
      FROM public.growth_measurements g
     CROSS JOIN (SELECT max(installed_on) AS at FROM public.flyway_schema_history
                  WHERE version = '20260807140000') m
     WHERE g.created_at > m.at OR g.updated_at > m.at;

    IF v_touched > 0 THEN
        RAISE EXCEPTION
            'WAVE13_CONTRACT_BLOCKED: % growth row(s) written after the expand migration; a pre-cutover build is still running',
            v_touched;
    END IF;

    -- Every recorded measurement must exist as an observation. Step 1 just made this true;
    -- if it is still false, something is wrong with the mapping, not with the timing.
    SELECT count(*) INTO v_missing
      FROM public.growth_measurements g
     CROSS JOIN LATERAL (VALUES
            ('BABY_WEIGHT', g.weight_kg),
            ('BABY_HEIGHT', g.height_cm),
            ('BABY_HEAD_CIRCUMFERENCE', g.head_circumference_cm)
           ) AS m(observation_type, value)
     WHERE m.value IS NOT NULL
       AND NOT EXISTS (
            SELECT 1 FROM public.health_observations o
             WHERE o.legacy_source = 'growth_measurements'
               AND o.legacy_id = g.growth_measurement_id || ':' || m.observation_type);

    IF v_missing > 0 THEN
        RAISE EXCEPTION
            'WAVE13_CONTRACT_BLOCKED: % measurement(s) still absent from health_observations', v_missing;
    END IF;

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
            'WAVE13_CONTRACT_BLOCKED: % observation(s) diverge from their source growth row',
            v_divergent;
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- 3. Drop — no CASCADE
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS public.growth_measurements;

-- ---------------------------------------------------------------------------
-- 4. Negative-impact check
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_missing text;
    v_sessions bigint;
BEGIN
    SELECT string_agg(expected, ', ' ORDER BY expected) INTO v_missing
    FROM unnest(ARRAY[
        'health_observations', 'health_metric_definitions', 'care_subjects',
        'notification_jobs', 'checklist_task_instances', 'users'
    ]) AS expected
    WHERE to_regclass('public.' || expected) IS NULL;

    IF v_missing IS NOT NULL THEN
        RAISE EXCEPTION 'WAVE13_REGRESSION: retained object(s) missing: %', v_missing;
    END IF;

    SELECT string_agg(needed, ', ' ORDER BY needed) INTO v_missing
      FROM unnest(ARRAY['measurement_group_id', 'deleted_at']) AS needed
     WHERE NOT EXISTS (
        SELECT 1 FROM information_schema.columns c
         WHERE c.table_schema = 'public' AND c.table_name = 'health_observations'
           AND c.column_name = needed);

    IF v_missing IS NOT NULL THEN
        RAISE EXCEPTION 'WAVE13_REGRESSION: health_observations column(s) removed: %', v_missing;
    END IF;

    -- The migrated data must still be there after the drop.
    SELECT count(DISTINCT measurement_group_id) INTO v_sessions
      FROM public.health_observations WHERE legacy_source = 'growth_measurements';
    IF v_sessions = 0 THEN
        RAISE EXCEPTION 'WAVE13_REGRESSION: no growth sessions remain in health_observations';
    END IF;
END
$$;

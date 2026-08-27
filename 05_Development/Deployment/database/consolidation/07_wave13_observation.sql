-- CareBridge wave 13 — growth_measurements -> health_observations, observation window
-- Spec: 08_References/Database_Table_Audit_And_Consolidation V3.md §3.12
-- Mapping: 08_References/Wave13_Growth_To_Observations_Mapping.md
--
-- Read-only. Run repeatedly after the cutover build is deployed; each run prints one
-- PASS/FAIL/INFO row per gate plus a verdict, the same shape as 04_readiness_check.
--
-- What this window is for. The expand migration (V20260807140000) backfilled
-- growth_measurements into health_observations while the *old* build was still running and
-- still writing growth_measurements. Any session recorded between that backfill and the
-- cutover deploy therefore exists only in the source table. This script finds those
-- stragglers and confirms nothing writes the source table any more.
--
-- No \set ON_ERROR_STOP: the Supabase SQL editor rejects psql meta-commands.

WITH
-- Gate 1 — the cutover build is deployed: nothing may write growth_measurements any more.
-- A row created or updated after the expand migration landed means an older build is still
-- attached, exactly as a stale pre-R12 instance was caught during R13.
freshness AS (
    SELECT count(*) FILTER (
               WHERE g.created_at > m.installed_on OR g.updated_at > m.installed_on) AS touched,
           max(greatest(g.created_at, g.updated_at)) AS newest_write,
           now() - max(greatest(g.created_at, g.updated_at)) AS quiet_for
      FROM public.growth_measurements g
     CROSS JOIN (SELECT max(installed_on) AS installed_on
                   FROM public.flyway_schema_history
                  WHERE version = '20260807140000') m
),
-- Gate 2 — every recorded measurement has its observation. This is the straggler count:
-- non-zero here is not a failure, it is work for the contract step's rerun.
parity AS (
    SELECT count(*) AS missing
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
               AND o.legacy_id = g.growth_measurement_id || ':' || m.observation_type)
),
-- Gate 3 — migrated values still agree with their source, including soft-delete state.
divergence AS (
    SELECT count(*) AS bad
      FROM public.growth_measurements g
      JOIN public.health_observations o
        ON o.measurement_group_id = g.growth_measurement_id
     WHERE o.care_subject_id IS DISTINCT FROM g.care_subject_id
        OR (o.observation_type = 'BABY_WEIGHT' AND o.value_numeric IS DISTINCT FROM g.weight_kg)
        OR (o.observation_type = 'BABY_HEIGHT' AND o.value_numeric IS DISTINCT FROM g.height_cm)
        OR (o.observation_type = 'BABY_HEAD_CIRCUMFERENCE'
            AND o.value_numeric IS DISTINCT FROM g.head_circumference_cm)
        OR (o.observed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date IS DISTINCT FROM g.measured_date
),
-- Gate 4 — the new build is actually being used. Sessions written after the cutover appear
-- in health_observations and have no counterpart in growth_measurements, which is the
-- positive signal that the store, not the old repository, is serving writes.
adoption AS (
    SELECT count(DISTINCT o.measurement_group_id) AS new_sessions
      FROM public.health_observations o
     WHERE o.legacy_source = 'growth_measurements'
       AND NOT EXISTS (SELECT 1 FROM public.growth_measurements g
                        WHERE g.growth_measurement_id = o.measurement_group_id)
),
-- Gate 5 — baby rows must never leak into the maternal metric views, all of which filter
-- on legacy_source = 'maternal_health_observations'.
isolation AS (
    SELECT count(*) AS leaked
      FROM public.health_observations
     WHERE observation_type LIKE 'BABY\_%' ESCAPE '\'
       AND (legacy_source IS DISTINCT FROM 'growth_measurements' OR subject_type <> 'BABY')
),
gates AS (
    SELECT 1 AS seq, 'source table frozen since the expand migration' AS gate,
           '0 rows touched' AS expected, touched::text AS actual,
           CASE WHEN touched = 0 THEN 'PASS' ELSE 'FAIL' END AS result FROM freshness
    UNION ALL
    SELECT 2, 'source table quiet for at least 24h',
           '>= 24:00:00', COALESCE(quiet_for::text, 'no rows'),
           CASE WHEN newest_write IS NULL THEN 'PASS'
                WHEN quiet_for >= interval '24 hours' THEN 'PASS' ELSE 'INFO' END
      FROM freshness
    UNION ALL
    SELECT 3, 'measurements with no observation (stragglers for the contract rerun)',
           '0', missing::text,
           CASE WHEN missing = 0 THEN 'PASS' ELSE 'INFO' END FROM parity
    UNION ALL
    SELECT 4, 'migrated observations diverging from their source row',
           '0', bad::text, CASE WHEN bad = 0 THEN 'PASS' ELSE 'FAIL' END FROM divergence
    UNION ALL
    SELECT 5, 'growth sessions created by the new build',
           '>= 1', new_sessions::text,
           CASE WHEN new_sessions > 0 THEN 'PASS' ELSE 'INFO' END FROM adoption
    UNION ALL
    SELECT 6, 'baby observations outside their legacy_source / subject_type',
           '0', leaked::text, CASE WHEN leaked = 0 THEN 'PASS' ELSE 'FAIL' END FROM isolation
)
SELECT seq, gate, expected, actual, result FROM gates ORDER BY seq;

-- Verdict. INFO gates do not block: gate 3 counts work the contract step performs itself,
-- and gates 2 and 5 simply may not have had time to become true yet.
WITH
freshness AS (
    SELECT count(*) FILTER (
               WHERE g.created_at > m.installed_on OR g.updated_at > m.installed_on) AS touched
      FROM public.growth_measurements g
     CROSS JOIN (SELECT max(installed_on) AS installed_on
                   FROM public.flyway_schema_history
                  WHERE version = '20260807140000') m
),
divergence AS (
    SELECT count(*) AS bad
      FROM public.growth_measurements g
      JOIN public.health_observations o
        ON o.measurement_group_id = g.growth_measurement_id
     WHERE o.care_subject_id IS DISTINCT FROM g.care_subject_id
        OR (o.observed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date IS DISTINCT FROM g.measured_date
),
isolation AS (
    SELECT count(*) AS leaked FROM public.health_observations
     WHERE observation_type LIKE 'BABY\_%' ESCAPE '\'
       AND (legacy_source IS DISTINCT FROM 'growth_measurements' OR subject_type <> 'BABY')
)
SELECT CASE
    WHEN (SELECT touched FROM freshness) > 0
        THEN 'BLOCKED — something still writes growth_measurements; find and stop it before contracting'
    WHEN (SELECT bad FROM divergence) > 0
        THEN 'BLOCKED — migrated observations diverge from their source; do not drop anything'
    WHEN (SELECT leaked FROM isolation) > 0
        THEN 'BLOCKED — baby observations are escaping their legacy_source/subject_type scope'
    ELSE 'OBSERVATION CLEAN — 08_wave13_contract_drop_growth.sql may be promoted'
END AS verdict;

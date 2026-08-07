-- Wave 13 prerequisites 1-3 (V3 §3.12) for growth_measurements -> health_observations.
--
-- This migration only opens the schema. It deliberately does NOT backfill, does not touch
-- growth_measurements, and does not change any read path — V3 §3.12 lists five conditions
-- and prerequisites 4 (mapping decisions) and 5 (moving the four services and the frontend,
-- or shipping a compatibility view) are still open. Merging before those are settled would
-- strand data behind queries that cannot see it.
--
-- Prerequisite 3 was found to be half-done already: health_observations.subject_type has
-- allowed 'BABY' since the canonical schema (health_observations_type_ck covers MOTHER,
-- BABY, DEPENDENT). Only health_metric_definitions was still MOTHER-only.

-- ---------------------------------------------------------------------------
-- 1. measurement_group_id — keep one measuring session a single logical aggregate
-- ---------------------------------------------------------------------------
-- One growth row yields up to three observations (weight, height, head circumference).
-- They must stay linked so an update or delete can address the session rather than an
-- individual reading. V3 §3.12 fixes the value as the source growth_measurement_id, so the
-- column is a plain uuid and needs no generator.
--
-- Nullable on purpose: the 513 observations already recorded were never part of a group,
-- and a NOT NULL default would invent groups that do not exist.
ALTER TABLE public.health_observations
    ADD COLUMN IF NOT EXISTS measurement_group_id uuid;

COMMENT ON COLUMN public.health_observations.measurement_group_id IS
    'Groups the observations produced by a single measuring session. For rows migrated from '
    'growth_measurements this is the source growth_measurement_id (V3 §3.12).';

CREATE INDEX IF NOT EXISTS health_observations_measurement_group_ix
    ON public.health_observations (measurement_group_id)
    WHERE measurement_group_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 2. Soft delete — growth_measurements.deleted_at needs a counterpart
-- ---------------------------------------------------------------------------
-- growth_measurements soft-deletes; health_observations had no equivalent, so a merge
-- would either resurrect deleted measurements or lose the fact that they were deleted.
--
-- WARNING for whoever implements the backfill: this column is inert until the read paths
-- filter on it. Nothing in the application currently excludes deleted_at IS NOT NULL, so
-- marking a row deleted today would hide nothing. Adding the filter to every
-- health_observation query is part of wave 13 proper, not of this prerequisite.
ALTER TABLE public.health_observations
    ADD COLUMN IF NOT EXISTS deleted_at timestamptz;

COMMENT ON COLUMN public.health_observations.deleted_at IS
    'Soft-delete marker mirroring growth_measurements.deleted_at. Read paths must filter on '
    'this before wave 13 backfills any soft-deleted growth row (V3 §3.12).';

CREATE INDEX IF NOT EXISTS health_observations_live_subject_ix
    ON public.health_observations (care_subject_id, observed_at DESC)
    WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 3. health_metric_definitions must be able to describe BABY metrics
-- ---------------------------------------------------------------------------
-- Baby weight, height and head circumference cannot be defined while the constraint admits
-- MOTHER only. DEPENDENT is left out: health_observations accepts it, but no metric is
-- defined for it and widening beyond what wave 13 needs would be speculative.
ALTER TABLE public.health_metric_definitions
    DROP CONSTRAINT IF EXISTS health_metric_definitions_subject_ck;

ALTER TABLE public.health_metric_definitions
    ADD CONSTRAINT health_metric_definitions_subject_ck
        CHECK (subject_type IN ('MOTHER', 'BABY'));

-- ---------------------------------------------------------------------------
-- 4. Verify — the schema really is in the state the next wave assumes
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_missing text;
BEGIN
    SELECT string_agg(detail, '; ' ORDER BY detail) INTO v_missing FROM (
        SELECT 'health_observations.measurement_group_id' AS detail
         WHERE NOT EXISTS (
            SELECT 1 FROM information_schema.columns
             WHERE table_schema='public' AND table_name='health_observations'
               AND column_name='measurement_group_id')
        UNION ALL
        SELECT 'health_observations.deleted_at'
         WHERE NOT EXISTS (
            SELECT 1 FROM information_schema.columns
             WHERE table_schema='public' AND table_name='health_observations'
               AND column_name='deleted_at')
        UNION ALL
        SELECT 'health_metric_definitions accepts BABY'
         WHERE NOT EXISTS (
            SELECT 1 FROM pg_constraint
             WHERE conrelid='public.health_metric_definitions'::regclass
               AND conname='health_metric_definitions_subject_ck'
               AND pg_get_constraintdef(oid) LIKE '%BABY%')
    ) AS gaps;

    IF v_missing IS NOT NULL THEN
        RAISE EXCEPTION 'WAVE13_PREREQ_INCOMPLETE: %', v_missing;
    END IF;

    -- The identity mechanism V3 §3.12 relies on for an idempotent backfill must exist.
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conrelid='public.health_observations'::regclass AND contype='u'
           AND pg_get_constraintdef(oid) = 'UNIQUE (legacy_source, legacy_id)') THEN
        RAISE EXCEPTION
            'WAVE13_PREREQ_INCOMPLETE: health_observations lost the (legacy_source, legacy_id) '
            'unique key the growth backfill needs to stay idempotent';
    END IF;

    -- Nothing here may have touched the source table.
    IF to_regclass('public.growth_measurements') IS NULL THEN
        RAISE EXCEPTION 'WAVE13_PREREQ_INCOMPLETE: growth_measurements must not be dropped yet';
    END IF;
END
$$;

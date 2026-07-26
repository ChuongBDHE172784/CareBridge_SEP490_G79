-- =============================================================================
-- V20260724214150 — Restore legacy expert columns for fresh databases
-- (D2 fix — CB-TRIAGE-FDBB-IMP-001)
--
-- WHY THIS EXISTS
-- ---------------
-- On a fresh database Flyway takes the baseline path
-- (B20260724111500__canonical_70_table_baseline.sql). That baseline is an
-- END-STATE snapshot: expert_credentials / expert_availability /
-- expert_location_shares already carry only the canonical
-- professional_profile_id column. But the immutable post-baseline migration
-- V20260724214200__canonicalize_expert_profile_references.sql gates on BOTH
-- the legacy expert_profile_id column and the canonical column being present
-- (it performs the legacy -> canonical cutover and then drops the legacy
-- column), so it fails on the baseline path with
--   CANONICAL_EXPERT_REFERENCE: expert_credentials must contain both legacy
--   and canonical identifiers
--
-- This migration, versioned between V20260724214100 and V20260724214200,
-- re-adds the legacy column (nullable, no data) so the immutable
-- V20260724214200 can run its gates on the empty tables and drop the column
-- again. Net schema effect: ZERO on every path.
--
-- WHEN IT IS A NO-OP (guard below)
-- --------------------------------
-- * Fully-migrated team databases (V20260724214200 already in history) apply
--   this file out-of-order (spring.flyway.out-of-order=true) and skip
--   immediately — re-adding the column AFTER the cutover dropped it would
--   corrupt the canonical end state.
-- * Mid-chain / replay databases that have not reached 214200 on the legacy
--   path still carry the real legacy columns WITH data —
--   ADD COLUMN IF NOT EXISTS is an exact no-op there.
-- `AND success` deliberately lets a fresh database whose first bootstrap
-- attempt failed re-attempt after repair.
-- =============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM public.flyway_schema_history
         WHERE version = '20260724214200'
           AND success
    ) THEN
        RAISE NOTICE 'restore_legacy_expert_columns_for_fresh_db: V20260724214200 already applied — cutover done; skipping (out-of-order no-op)';
        RETURN;
    END IF;

    ALTER TABLE public.expert_credentials
        ADD COLUMN IF NOT EXISTS expert_profile_id uuid;
    ALTER TABLE public.expert_availability
        ADD COLUMN IF NOT EXISTS expert_profile_id uuid;
    ALTER TABLE public.expert_location_shares
        ADD COLUMN IF NOT EXISTS expert_profile_id uuid;
END $$;

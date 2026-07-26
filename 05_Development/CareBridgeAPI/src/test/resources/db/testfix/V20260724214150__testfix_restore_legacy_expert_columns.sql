-- =============================================================================
-- TEST-ONLY Flyway shim (classpath:db/testfix — never on the production
-- classpath; wired in only by AbstractPostgresIntegrationTest).
--
-- WHY: on a fresh database Flyway takes the baseline path
-- (B20260724111500__canonical_70_table_baseline.sql). That baseline is an
-- END-STATE snapshot: expert_credentials / expert_availability /
-- expert_location_shares already carry only the canonical
-- professional_profile_id column. But the post-baseline migration
-- V20260724214200__canonicalize_expert_profile_references.sql gates on BOTH
-- the legacy expert_profile_id column and the canonical column being present
-- (it performs the legacy -> canonical cutover and then drops the legacy
-- column), so it fails on the baseline path with
--   CANONICAL_EXPERT_REFERENCE: expert_credentials must contain both legacy
--   and canonical identifiers
--
-- This shim, versioned between V20260724214100 and V20260724214200, re-adds
-- the legacy column (nullable, no data) so the immutable V20260724214200 can
-- run its gates on the empty tables and drop the column again. Net schema
-- effect: none. Underlying defect (baseline snapshot taken past the
-- migrations that follow it) needs a team-level fix in the main chain.
-- =============================================================================

ALTER TABLE public.expert_credentials
    ADD COLUMN IF NOT EXISTS expert_profile_id uuid;
ALTER TABLE public.expert_availability
    ADD COLUMN IF NOT EXISTS expert_profile_id uuid;
ALTER TABLE public.expert_location_shares
    ADD COLUMN IF NOT EXISTS expert_profile_id uuid;

-- =============================================================================
-- TEST-ONLY bootstrap for fresh Testcontainers databases (baseline path).
--
-- WHY THIS EXISTS
-- ---------------
-- A fresh (empty) database takes the Flyway baseline path: it starts from
-- B20260724111500__canonical_70_table_baseline.sql and SKIPS every pre-baseline
-- migration. But several POST-baseline migrations still reference bridge
-- tables in schema `carebridge_migration_bridge` that were only ever created
-- by PRE-baseline migrations (e.g. phase2/V20260722231360__bridge_story66_
-- safety_state.sql, V20260722019950__bridge_story65_branch_history.sql).
-- On the baseline path those creators never run, so e.g.
-- V20260724210000__canonical_safety_action_invariants.sql fails with
--   ERROR: relation "carebridge_migration_bridge.story66_notification_outbox_bridge" does not exist
-- and every fresh-DB integration context dies.
--
-- This script runs ONCE at container start (PostgreSQLContainer.withInitScript),
-- i.e. before any Flyway run, and pre-creates exactly the bridge objects that
-- post-baseline migrations expect to find. All DDL below is copied VERBATIM
-- from the original pre-baseline creator migrations (referenced per block).
-- Tables are left EMPTY (all post-baseline UPDATE/INSERT..SELECT against them
-- become no-ops), with one documented exception (story65 state row, below).
--
-- NOTE: this is a TEST-HARNESS workaround only. The underlying defect — a
-- post-baseline migration depending on pre-baseline-only bridge tables — is a
-- committed, already-applied migration chain issue that needs a team-level fix
-- (applied migrations must never be modified).
--
-- Tables deliberately NOT created here because the post-baseline migrations
-- create them themselves (CREATE TABLE IF NOT EXISTS) before first reference,
-- and duplicating potentially divergent DDL would be riskier than omitting:
--   * triage_lifecycle_bridge, lifecycle_safety_outcome_bridge
--     (created by V20260724211000)
--   * story68_request_bridge, story68_context_share_bridge,
--     story68_context_citation_bridge (created by V20260724211500)
--   * function sync_triage_lifecycle_bridge() (only DROP FUNCTION IF EXISTS)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS carebridge_migration_bridge;

-- -----------------------------------------------------------------------------
-- Story 6.6 safety-state bridges.
-- Required (empty) by V20260724210000__canonical_safety_action_invariants.sql:
-- referenced in UPDATE..FROM joins, a consumption gate, and a final
-- DROP TABLE without IF EXISTS.
-- DDL verbatim from phase2/V20260722231360__bridge_story66_safety_state.sql.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story66_triage_escalation_bridge (
    intake_session_id uuid PRIMARY KEY,
    emergency_session_id uuid NOT NULL,
    owner_user_id uuid NOT NULL,
    triggered_at timestamptz NOT NULL,
    captured_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story66_notification_outbox_bridge (
    emergency_session_id uuid PRIMARY KEY,
    owner_user_id uuid NOT NULL,
    status varchar(20) NOT NULL,
    attempt_count integer NOT NULL,
    next_attempt_at timestamptz NOT NULL,
    last_error_code varchar(120),
    claim_token uuid,
    created_at timestamptz NOT NULL,
    delivered_at timestamptz,
    terminal_at timestamptz,
    captured_at timestamptz NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------------
-- Story 6.5 branch-history state.
-- Required by V20260724212450__retire_story65_branch_history_shadows.sql, which
-- demands EXACTLY ONE row keyed 'V20260722019950'. This is the one table that
-- cannot stay empty. With both synthetic flags FALSE, that migration's checks
-- reduce to asserting that legacy public.baby_profiles /
-- public.pregnancy_outcome_evidence do not exist — true on the canonical
-- baseline (which models these facts in care_subjects/mother_journey_events).
-- DDL verbatim from V20260722019950__bridge_story65_branch_history.sql
-- (IF NOT EXISTS added for init-script idempotency).
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story65_branch_history_state (
    migration_key text PRIMARY KEY,
    synthetic_baby_profiles boolean NOT NULL,
    synthetic_outcome_evidence boolean NOT NULL,
    CONSTRAINT chk_story65_branch_history_state_key
        CHECK (migration_key = 'V20260722019950')
);

INSERT INTO carebridge_migration_bridge.story65_branch_history_state (
    migration_key, synthetic_baby_profiles, synthetic_outcome_evidence
) VALUES (
    'V20260722019950', false, false
)
ON CONFLICT (migration_key) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Story 6.8 state tables.
-- Only referenced by V20260724211500__canonical_story68_handoff_integrity.sql
-- via guarded blocks (to_regclass NULL-check / DROP TABLE IF EXISTS), so they
-- are not strictly required — created empty for completeness; harmless.
-- story68_history_state DDL verbatim from
--   V20260722020450__preserve_story68_handoff_history.sql.
-- story68_shadow_parent_registry DDL verbatim from
--   phase2/V20260722231950__bridge_story68_handoff_history.sql.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story68_history_state (
    history_key text PRIMARY KEY,
    source_graph_present boolean NOT NULL,
    recorded_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story68_shadow_parent_registry (
    table_name text PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT now()
);

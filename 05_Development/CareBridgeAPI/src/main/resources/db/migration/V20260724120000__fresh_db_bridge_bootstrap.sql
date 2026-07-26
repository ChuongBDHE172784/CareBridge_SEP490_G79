-- =============================================================================
-- V20260724120000 — Fresh-DB bridge bootstrap (D1 fix — CB-TRIAGE-FDBB-IMP-001)
--
-- WHY THIS EXISTS
-- ---------------
-- A fresh (empty) database takes the Flyway baseline path: it starts from
-- B20260724111500__canonical_70_table_baseline.sql and SKIPS every pre-baseline
-- migration. But several immutable POST-baseline migrations still reference
-- bridge tables in schema `carebridge_migration_bridge` that were only ever
-- created by PRE-baseline migrations (phase2/V20260722231360__bridge_story66_
-- safety_state.sql, V20260722019950__bridge_story65_branch_history.sql,
-- V20260722020450__preserve_story68_handoff_history.sql,
-- phase2/V20260722231950__bridge_story68_handoff_history.sql). On the baseline
-- path those creators never run, so V20260724210000__canonical_safety_action_
-- invariants.sql fails with
--   ERROR: relation "carebridge_migration_bridge.story66_notification_outbox_bridge" does not exist
-- and no empty database can ever bootstrap.
--
-- This migration is versioned INTO the broken window — after the baseline
-- (20260724111500), before the first consumer (20260724210000) — and
-- pre-creates exactly the bridge objects the immutable consumers expect.
-- All DDL below is copied VERBATIM from the original pre-baseline creator
-- migrations (referenced per block). Tables are left EMPTY (all post-baseline
-- UPDATE/INSERT..SELECT against them become no-ops), with one documented
-- exception (story65 state row, below). The chain itself then consumes and
-- drops everything created here (210000 drops the story66 tables, 211500 the
-- story68 tables, 212450 the story65 table and the schema once empty), so the
-- net schema effect is ZERO on every path.
--
-- WHEN IT IS A NO-OP (guard below)
-- --------------------------------
-- * Fully-migrated team databases (V20260724210000 already in history) apply
--   this file out-of-order (spring.flyway.out-of-order=true) and skip
--   immediately: on those databases the bridge tables were already consumed
--   and dropped, and re-creating them would leave stray objects behind.
-- * Mid-chain / replay databases (target below 20260724210000 on the legacy
--   path) pass the guard, but every statement is IF NOT EXISTS /
--   ON CONFLICT DO NOTHING against the REAL bridge objects the pre-baseline
--   creators produced — exact no-ops that never overwrite real captured state.
-- `public.flyway_schema_history` is guaranteed to exist here: Flyway records
-- the baseline row before running this file (precedent for querying it inside
-- a migration: V20260722020450 :151). `AND success` deliberately lets a fresh
-- database whose first bootstrap attempt failed re-attempt after repair.
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

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM public.flyway_schema_history
         WHERE version = '20260724210000'
           AND success
    ) THEN
        RAISE NOTICE 'fresh_db_bridge_bootstrap: V20260724210000 already applied — bridge tables already consumed; skipping (out-of-order no-op)';
        RETURN;
    END IF;

    CREATE SCHEMA IF NOT EXISTS carebridge_migration_bridge;

    -- -------------------------------------------------------------------------
    -- Story 6.6 safety-state bridges.
    -- Required (empty) by V20260724210000__canonical_safety_action_invariants.sql:
    -- referenced in UPDATE..FROM joins, a consumption gate, and a final
    -- DROP TABLE without IF EXISTS.
    -- DDL verbatim from phase2/V20260722231360__bridge_story66_safety_state.sql.
    -- -------------------------------------------------------------------------
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

    -- -------------------------------------------------------------------------
    -- Story 6.5 branch-history state.
    -- Required by V20260724212450__retire_story65_branch_history_shadows.sql,
    -- which demands EXACTLY ONE row keyed 'V20260722019950' (raises on 0 rows).
    -- This is the one table that cannot stay empty. With both synthetic flags
    -- FALSE, that migration's checks reduce to asserting that legacy
    -- public.baby_profiles / public.pregnancy_outcome_evidence do NOT exist —
    -- true on the canonical baseline (which models these facts in
    -- care_subjects / mother_journey_events). No safety assertion is bypassed:
    -- every RAISE EXCEPTION in 212450 still executes against real catalog
    -- state. ON CONFLICT DO NOTHING preserves a REAL pre-baseline row on the
    -- legacy/replay path.
    -- DDL verbatim from V20260722019950__bridge_story65_branch_history.sql
    -- (IF NOT EXISTS added for bootstrap idempotency).
    -- -------------------------------------------------------------------------
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

    -- -------------------------------------------------------------------------
    -- Story 6.8 state tables.
    -- Only referenced by V20260724211500__canonical_story68_handoff_integrity.sql
    -- via guarded blocks (to_regclass NULL-check / DROP TABLE IF EXISTS), so they
    -- are not strictly required — created empty for completeness; harmless.
    -- story68_history_state DDL verbatim from
    --   V20260722020450__preserve_story68_handoff_history.sql.
    -- story68_shadow_parent_registry DDL verbatim from
    --   phase2/V20260722231950__bridge_story68_handoff_history.sql.
    -- -------------------------------------------------------------------------
    CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story68_history_state (
        history_key text PRIMARY KEY,
        source_graph_present boolean NOT NULL,
        recorded_at timestamptz NOT NULL DEFAULT now()
    );

    CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story68_shadow_parent_registry (
        table_name text PRIMARY KEY,
        created_at timestamptz NOT NULL DEFAULT now()
    );
END $$;

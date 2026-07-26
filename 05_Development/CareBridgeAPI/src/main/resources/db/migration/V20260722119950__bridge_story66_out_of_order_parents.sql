-- Permit the immutable Story 6.6/6.7 migrations to be discovered out of
-- order after a Phase 2 cutover has already retired their legacy parents.
-- Synthetic parents are zero-row, explicitly marked, and retired by the
-- pre-wave Story 6.6 bridge after all Story 6.6/6.7 children are captured.

CREATE SCHEMA IF NOT EXISTS carebridge_migration_bridge;

CREATE TABLE carebridge_migration_bridge.story66_branch_history_state (
    migration_key text PRIMARY KEY,
    synthetic_intake_sessions boolean NOT NULL,
    synthetic_emergency_sessions boolean NOT NULL,
    CONSTRAINT chk_story66_branch_history_state_key
        CHECK (migration_key = 'V20260722119950')
);

DO $story66_branch_history_bridge$
DECLARE
    intake_sessions_missing boolean;
    emergency_sessions_missing boolean;
BEGIN
    IF EXISTS (
        SELECT 1
          FROM carebridge_migration_bridge.story66_branch_history_state
    ) THEN
        RAISE EXCEPTION 'Story 6.6 branch-history bridge state already exists';
    END IF;

    intake_sessions_missing := to_regclass('public.intake_sessions') IS NULL;
    emergency_sessions_missing := to_regclass('public.emergency_sessions') IS NULL;

    IF intake_sessions_missing THEN
        CREATE TABLE public.intake_sessions (
            id uuid PRIMARY KEY,
            user_id uuid NOT NULL,
            stage varchar(20) NOT NULL,
            completed_at timestamptz,
            CONSTRAINT story66_shadow_intake_owner_uk UNIQUE (id, user_id)
        );
    END IF;

    IF emergency_sessions_missing THEN
        CREATE TABLE public.emergency_sessions (
            id uuid PRIMARY KEY,
            user_id uuid NOT NULL,
            status varchar(20) NOT NULL,
            created_at timestamptz NOT NULL,
            resolved_at timestamptz,
            CONSTRAINT story66_shadow_emergency_owner_uk UNIQUE (id, user_id)
        );
    END IF;

    INSERT INTO carebridge_migration_bridge.story66_branch_history_state (
        migration_key, synthetic_intake_sessions,
        synthetic_emergency_sessions
    ) VALUES (
        'V20260722119950', intake_sessions_missing,
        emergency_sessions_missing
    );
END
$story66_branch_history_bridge$;

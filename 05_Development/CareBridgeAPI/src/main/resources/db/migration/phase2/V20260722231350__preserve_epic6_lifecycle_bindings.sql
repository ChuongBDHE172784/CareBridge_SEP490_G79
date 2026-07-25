-- Preserve Story 6.7 lifecycle state before Wave 4 retires intake_sessions.
--
-- This migration also handles databases that applied the original Story 6.7
-- migration, whose lifecycle_safety_outcomes foreign keys otherwise prevent
-- the Wave 4/Wave 8 legacy-table drops.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

CREATE SCHEMA IF NOT EXISTS carebridge_migration_bridge;

CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.triage_lifecycle_bridge (
    triage_session_id uuid PRIMARY KEY,
    owner_user_id uuid NOT NULL,
    journey_id uuid,
    origin_dashboard varchar(30),
    origin_reference_id uuid,
    continuation_token uuid,
    continuation_expires_at timestamptz,
    continuation_acknowledged_at timestamptz,
    captured_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.lifecycle_safety_outcome_bridge (
    outcome_id uuid PRIMARY KEY,
    owner_user_id uuid NOT NULL,
    journey_id uuid NOT NULL,
    triage_session_id uuid NOT NULL UNIQUE,
    emergency_session_id uuid,
    risk_level varchar(10) NOT NULL,
    stage varchar(20) NOT NULL,
    origin_dashboard varchar(30) NOT NULL,
    origin_reference_id uuid NOT NULL,
    origin_action varchar(40) NOT NULL,
    occurred_at timestamptz NOT NULL,
    recorded_at timestamptz NOT NULL,
    captured_at timestamptz NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION carebridge_migration_bridge.sync_triage_lifecycle_bridge()
RETURNS trigger
LANGUAGE plpgsql
AS $triage_lifecycle_bridge_sync$
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM carebridge_migration_bridge.triage_lifecycle_bridge
         WHERE triage_session_id = OLD.id;
        RETURN OLD;
    END IF;

    INSERT INTO carebridge_migration_bridge.triage_lifecycle_bridge (
        triage_session_id, owner_user_id, journey_id, origin_dashboard,
        origin_reference_id, continuation_token, continuation_expires_at,
        continuation_acknowledged_at, captured_at)
    VALUES (
        NEW.id, NEW.user_id, NEW.journey_id, NEW.origin_dashboard,
        NEW.origin_reference_id, NEW.continuation_token,
        NEW.continuation_expires_at, NEW.continuation_acknowledged_at, now())
    ON CONFLICT (triage_session_id) DO UPDATE SET
        owner_user_id = excluded.owner_user_id,
        journey_id = excluded.journey_id,
        origin_dashboard = excluded.origin_dashboard,
        origin_reference_id = excluded.origin_reference_id,
        continuation_token = excluded.continuation_token,
        continuation_expires_at = excluded.continuation_expires_at,
        continuation_acknowledged_at = excluded.continuation_acknowledged_at,
        captured_at = excluded.captured_at;
    RETURN NEW;
END
$triage_lifecycle_bridge_sync$;

DO $preserve_story67_lifecycle$
BEGIN
    IF to_regclass('public.intake_sessions') IS NOT NULL THEN
        -- Freeze legacy writers before the snapshot. The lock is held until
        -- commit, spanning both copy and trigger installation.
        LOCK TABLE public.intake_sessions IN SHARE ROW EXCLUSIVE MODE;

        IF (
            SELECT count(*)
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'intake_sessions'
               AND column_name IN (
                   'journey_id','origin_dashboard','origin_reference_id',
                   'continuation_token','continuation_expires_at',
                   'continuation_acknowledged_at')
        ) <> 6 THEN
            RAISE EXCEPTION 'STORY67_LIFECYCLE_BRIDGE_COLUMNS_MISSING';
        END IF;

        INSERT INTO carebridge_migration_bridge.triage_lifecycle_bridge (
            triage_session_id, owner_user_id, journey_id, origin_dashboard,
            origin_reference_id, continuation_token, continuation_expires_at,
            continuation_acknowledged_at, captured_at)
        SELECT legacy.id, legacy.user_id, legacy.journey_id,
               legacy.origin_dashboard, legacy.origin_reference_id,
               legacy.continuation_token, legacy.continuation_expires_at,
               legacy.continuation_acknowledged_at, now()
          FROM public.intake_sessions legacy
        ON CONFLICT (triage_session_id) DO UPDATE SET
            owner_user_id = excluded.owner_user_id,
            journey_id = excluded.journey_id,
            origin_dashboard = excluded.origin_dashboard,
            origin_reference_id = excluded.origin_reference_id,
            continuation_token = excluded.continuation_token,
            continuation_expires_at = excluded.continuation_expires_at,
            continuation_acknowledged_at = excluded.continuation_acknowledged_at,
            captured_at = excluded.captured_at;

        EXECUTE 'DROP TRIGGER IF EXISTS triage_lifecycle_bridge_sync_trg '
            || 'ON public.intake_sessions';
        EXECUTE 'CREATE TRIGGER triage_lifecycle_bridge_sync_trg '
            || 'AFTER INSERT OR UPDATE OR DELETE ON public.intake_sessions '
            || 'FOR EACH ROW EXECUTE FUNCTION '
            || 'carebridge_migration_bridge.sync_triage_lifecycle_bridge()';
    END IF;

    IF to_regclass('public.lifecycle_safety_outcomes') IS NOT NULL THEN
        -- Acquire the DROP-strength lock before copying so no append can commit
        -- between reconciliation and retirement of the legacy projection.
        LOCK TABLE public.lifecycle_safety_outcomes IN ACCESS EXCLUSIVE MODE;

        INSERT INTO carebridge_migration_bridge.lifecycle_safety_outcome_bridge (
            outcome_id, owner_user_id, journey_id, triage_session_id,
            emergency_session_id, risk_level, stage, origin_dashboard,
            origin_reference_id, origin_action, occurred_at, recorded_at,
            captured_at)
        SELECT legacy.outcome_id, legacy.owner_user_id, legacy.journey_id,
               legacy.intake_session_id, legacy.emergency_session_id,
               legacy.risk_level, legacy.stage, legacy.origin_dashboard,
               legacy.origin_reference_id, legacy.origin_action,
               legacy.occurred_at, legacy.recorded_at, now()
          FROM public.lifecycle_safety_outcomes legacy
        ON CONFLICT (outcome_id) DO UPDATE SET
            owner_user_id = excluded.owner_user_id,
            journey_id = excluded.journey_id,
            triage_session_id = excluded.triage_session_id,
            emergency_session_id = excluded.emergency_session_id,
            risk_level = excluded.risk_level,
            stage = excluded.stage,
            origin_dashboard = excluded.origin_dashboard,
            origin_reference_id = excluded.origin_reference_id,
            origin_action = excluded.origin_action,
            occurred_at = excluded.occurred_at,
            recorded_at = excluded.recorded_at,
            captured_at = excluded.captured_at;

        IF EXISTS (
            SELECT 1
              FROM public.lifecycle_safety_outcomes legacy
              LEFT JOIN carebridge_migration_bridge.lifecycle_safety_outcome_bridge bridge
                ON bridge.outcome_id = legacy.outcome_id
               AND bridge.owner_user_id = legacy.owner_user_id
               AND bridge.journey_id = legacy.journey_id
               AND bridge.triage_session_id = legacy.intake_session_id
               AND bridge.emergency_session_id IS NOT DISTINCT FROM
                   legacy.emergency_session_id
               AND bridge.risk_level = legacy.risk_level
               AND bridge.stage = legacy.stage
               AND bridge.origin_dashboard = legacy.origin_dashboard
               AND bridge.origin_reference_id = legacy.origin_reference_id
               AND bridge.origin_action = legacy.origin_action
               AND bridge.occurred_at = legacy.occurred_at
               AND bridge.recorded_at = legacy.recorded_at
             WHERE bridge.outcome_id IS NULL
        ) THEN
            RAISE EXCEPTION 'STORY67_SAFETY_OUTCOME_BRIDGE_RECONCILIATION_FAILED';
        END IF;

        DROP TABLE public.lifecycle_safety_outcomes;
    END IF;
END
$preserve_story67_lifecycle$;

DROP FUNCTION IF EXISTS public.reject_lifecycle_safety_outcome_mutation();

-- Preserve the published Story 6.6 association/outbox state before Wave 4
-- and Wave 8 retire their intake/emergency parent tables.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

CREATE SCHEMA IF NOT EXISTS carebridge_migration_bridge;

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

-- Freeze the owner graph and both mutable leaves for the entire Flyway
-- transaction. Without these locks, a concurrent legacy insert/update could
-- commit after the snapshot and be silently discarded by the DROP below.
DO $lock_story66_legacy_state$
BEGIN
    IF to_regclass('public.intake_sessions') IS NOT NULL THEN
        EXECUTE 'LOCK TABLE public.intake_sessions IN SHARE MODE';
    END IF;
    IF to_regclass('public.emergency_sessions') IS NOT NULL THEN
        EXECUTE 'LOCK TABLE public.emergency_sessions IN SHARE MODE';
    END IF;
    IF to_regclass('public.triage_emergency_escalations') IS NOT NULL THEN
        EXECUTE 'LOCK TABLE public.triage_emergency_escalations '
            || 'IN SHARE ROW EXCLUSIVE MODE';
    END IF;
    IF to_regclass('public.emergency_notification_outbox') IS NOT NULL THEN
        EXECUTE 'LOCK TABLE public.emergency_notification_outbox '
            || 'IN SHARE ROW EXCLUSIVE MODE';
    END IF;
END
$lock_story66_legacy_state$;

DO $preserve_story66_safety_state$
DECLARE
    source_count bigint;
    bridge_count bigint;
BEGIN
    IF to_regclass('public.triage_emergency_escalations') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
              FROM public.triage_emergency_escalations legacy
              LEFT JOIN public.intake_sessions intake
                ON intake.id = legacy.intake_session_id
               AND intake.user_id = legacy.user_id
              LEFT JOIN public.emergency_sessions emergency
                ON emergency.id = legacy.emergency_session_id
               AND emergency.user_id = legacy.user_id
             WHERE intake.id IS NULL OR emergency.id IS NULL
        ) THEN
            RAISE EXCEPTION 'STORY66_TRIAGE_ESCALATION_OWNER_RECONCILIATION_FAILED';
        END IF;

        INSERT INTO carebridge_migration_bridge.story66_triage_escalation_bridge (
            intake_session_id, emergency_session_id, owner_user_id,
            triggered_at, captured_at)
        SELECT legacy.intake_session_id, legacy.emergency_session_id,
               legacy.user_id, legacy.triggered_at, now()
          FROM public.triage_emergency_escalations legacy
        ON CONFLICT (intake_session_id) DO UPDATE SET
            emergency_session_id = excluded.emergency_session_id,
            owner_user_id = excluded.owner_user_id,
            triggered_at = excluded.triggered_at,
            captured_at = excluded.captured_at;

        SELECT count(*) INTO source_count
          FROM public.triage_emergency_escalations;
        SELECT count(*) INTO bridge_count
          FROM carebridge_migration_bridge.story66_triage_escalation_bridge;
        IF source_count <> bridge_count OR EXISTS (
            SELECT 1
              FROM public.triage_emergency_escalations legacy
              LEFT JOIN carebridge_migration_bridge.story66_triage_escalation_bridge bridge
                ON bridge.intake_session_id = legacy.intake_session_id
               AND bridge.emergency_session_id = legacy.emergency_session_id
               AND bridge.owner_user_id = legacy.user_id
               AND bridge.triggered_at = legacy.triggered_at
             WHERE bridge.intake_session_id IS NULL
        ) THEN
            RAISE EXCEPTION
                'STORY66_TRIAGE_ESCALATION_BRIDGE_RECONCILIATION_FAILED source=% bridge=%',
                source_count, bridge_count;
        END IF;
    END IF;

    IF to_regclass('public.emergency_notification_outbox') IS NOT NULL THEN
        INSERT INTO carebridge_migration_bridge.story66_notification_outbox_bridge (
            emergency_session_id, owner_user_id, status, attempt_count,
            next_attempt_at, last_error_code, claim_token, created_at,
            delivered_at, terminal_at, captured_at)
        SELECT legacy.emergency_session_id, emergency.user_id, legacy.status,
               legacy.attempt_count, legacy.next_attempt_at,
               legacy.last_error_code, legacy.claim_token, legacy.created_at,
               legacy.delivered_at, legacy.terminal_at, now()
          FROM public.emergency_notification_outbox legacy
          JOIN public.emergency_sessions emergency
            ON emergency.id = legacy.emergency_session_id
        ON CONFLICT (emergency_session_id) DO UPDATE SET
            owner_user_id = excluded.owner_user_id,
            status = excluded.status,
            attempt_count = excluded.attempt_count,
            next_attempt_at = excluded.next_attempt_at,
            last_error_code = excluded.last_error_code,
            claim_token = excluded.claim_token,
            created_at = excluded.created_at,
            delivered_at = excluded.delivered_at,
            terminal_at = excluded.terminal_at,
            captured_at = excluded.captured_at;

        SELECT count(*) INTO source_count
          FROM public.emergency_notification_outbox;
        SELECT count(*) INTO bridge_count
          FROM carebridge_migration_bridge.story66_notification_outbox_bridge;
        IF source_count <> bridge_count OR EXISTS (
            SELECT 1
              FROM public.emergency_notification_outbox legacy
              JOIN public.emergency_sessions emergency
                ON emergency.id = legacy.emergency_session_id
              LEFT JOIN carebridge_migration_bridge.story66_notification_outbox_bridge bridge
                ON bridge.emergency_session_id = legacy.emergency_session_id
               AND bridge.owner_user_id = emergency.user_id
               AND bridge.status = legacy.status
               AND bridge.attempt_count = legacy.attempt_count
               AND bridge.next_attempt_at = legacy.next_attempt_at
               AND bridge.last_error_code IS NOT DISTINCT FROM legacy.last_error_code
               AND bridge.claim_token IS NOT DISTINCT FROM legacy.claim_token
               AND bridge.created_at = legacy.created_at
               AND bridge.delivered_at IS NOT DISTINCT FROM legacy.delivered_at
               AND bridge.terminal_at IS NOT DISTINCT FROM legacy.terminal_at
             WHERE bridge.emergency_session_id IS NULL
        ) THEN
            RAISE EXCEPTION
                'STORY66_NOTIFICATION_OUTBOX_BRIDGE_RECONCILIATION_FAILED source=% bridge=%',
                source_count, bridge_count;
        END IF;
    END IF;
END
$preserve_story66_safety_state$;

-- Both tables are leaves that reference parents retired in later waves.
DROP TABLE IF EXISTS public.emergency_notification_outbox;
DROP TABLE IF EXISTS public.triage_emergency_escalations;

DO $story66_legacy_absence_gate$
BEGIN
    IF to_regclass('public.emergency_notification_outbox') IS NOT NULL
       OR to_regclass('public.triage_emergency_escalations') IS NOT NULL THEN
        RAISE EXCEPTION 'STORY66_LEGACY_SAFETY_TABLE_DROP_FAILED';
    END IF;
END
$story66_legacy_absence_gate$;

-- V20260722119950 creates marked, zero-row parents only when immutable Story
-- 6.6/6.7 migrations are discovered after Wave 9. Story 6.7's child has been
-- captured by V20260722231350 and both Story 6.6 children are gone above, so
-- the marked parents can now be retired. Real parents remain for Wave 4/8.
DO $retire_story66_branch_history_parents$
DECLARE
    state_count bigint;
    synthetic_intake_sessions boolean;
    synthetic_emergency_sessions boolean;
    relation_count bigint;
    inbound_fk_count bigint;
BEGIN
    IF to_regclass(
        'carebridge_migration_bridge.story66_branch_history_state'
    ) IS NULL THEN
        RAISE EXCEPTION 'Story 6.6 branch-history bridge state is missing';
    END IF;

    SELECT count(*) INTO state_count
      FROM carebridge_migration_bridge.story66_branch_history_state;
    IF state_count <> 1 THEN
        RAISE EXCEPTION
            'expected one Story 6.6 branch-history state row, found %',
            state_count;
    END IF;

    SELECT state.synthetic_intake_sessions,
           state.synthetic_emergency_sessions
      INTO synthetic_intake_sessions, synthetic_emergency_sessions
      FROM carebridge_migration_bridge.story66_branch_history_state state
     WHERE state.migration_key = 'V20260722119950';
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Story 6.6 branch-history state key is missing';
    END IF;

    IF synthetic_intake_sessions THEN
        IF to_regclass('public.intake_sessions') IS NULL THEN
            RAISE EXCEPTION
                'synthetic intake_sessions disappeared before retirement';
        END IF;
        SELECT count(*) INTO relation_count FROM public.intake_sessions;
        SELECT count(*) INTO inbound_fk_count
          FROM pg_constraint
         WHERE contype = 'f'
           AND confrelid = 'public.intake_sessions'::regclass;
        IF relation_count <> 0 OR inbound_fk_count <> 0 THEN
            RAISE EXCEPTION
                'synthetic intake_sessions is not safely disposable: rows %, inbound_fks %',
                relation_count, inbound_fk_count;
        END IF;
        EXECUTE 'DROP TABLE public.intake_sessions';
    ELSIF to_regclass('public.intake_sessions') IS NULL THEN
        RAISE EXCEPTION
            'real intake_sessions disappeared before Wave 4 reconciliation';
    END IF;

    IF synthetic_emergency_sessions THEN
        IF to_regclass('public.emergency_sessions') IS NULL THEN
            RAISE EXCEPTION
                'synthetic emergency_sessions disappeared before retirement';
        END IF;
        SELECT count(*) INTO relation_count FROM public.emergency_sessions;
        SELECT count(*) INTO inbound_fk_count
          FROM pg_constraint
         WHERE contype = 'f'
           AND confrelid = 'public.emergency_sessions'::regclass;
        IF relation_count <> 0 OR inbound_fk_count <> 0 THEN
            RAISE EXCEPTION
                'synthetic emergency_sessions is not safely disposable: rows %, inbound_fks %',
                relation_count, inbound_fk_count;
        END IF;
        EXECUTE 'DROP TABLE public.emergency_sessions';
    ELSIF to_regclass('public.emergency_sessions') IS NULL THEN
        RAISE EXCEPTION
            'real emergency_sessions disappeared before Wave 8 reconciliation';
    END IF;
END
$retire_story66_branch_history_parents$;

DROP TABLE carebridge_migration_bridge.story66_branch_history_state;

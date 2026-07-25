-- Canonical safety/emergency invariants.
--
-- safety_events owns mutable alert projection/claim state.  The action table is
-- an immutable journal: every attempt, delivery and escalation transition is a
-- separate row guarded by a generation and (for claimed work) a fence token.

SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '5min';

ALTER TABLE public.safety_events
    ADD COLUMN IF NOT EXISTS alert_generation bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS alert_status varchar(20),
    ADD COLUMN IF NOT EXISTS alert_claim_token uuid,
    ADD COLUMN IF NOT EXISTS alert_claimed_at timestamptz,
    ADD COLUMN IF NOT EXISTS alert_lease_expires_at timestamptz,
    ADD COLUMN IF NOT EXISTS alert_completed_at timestamptz,
    ADD COLUMN IF NOT EXISTS alert_successful_recipient_count integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS alert_failed_recipient_count integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS alert_updated_at timestamptz;

ALTER TABLE public.safety_event_actions
    ADD COLUMN IF NOT EXISTS action_phase varchar(20),
    ADD COLUMN IF NOT EXISTS alert_generation bigint,
    ADD COLUMN IF NOT EXISTS fence_token uuid,
    ADD COLUMN IF NOT EXISTS related_action_id uuid;

-- The old canonical cut-over retained one mutable ALERT_ATTEMPT row.  Project
-- that state to the parent before converting the row into a historical
-- snapshot.  Deterministic ranking also protects databases that were manually
-- repaired with more than one legacy attempt row.
WITH ranked_attempt AS (
    SELECT a.*,
           row_number() OVER (
               PARTITION BY a.safety_event_id
               ORDER BY a.attempt_number DESC, a.updated_at DESC NULLS LAST,
                        a.created_at DESC, a.safety_event_action_id DESC
           ) AS attempt_rank
      FROM public.safety_event_actions a
     WHERE a.action_type = 'ALERT_ATTEMPT'
)
UPDATE public.safety_events event
   SET alert_generation = greatest(event.alert_generation, greatest(attempt.attempt_number, 1)),
       alert_status = attempt.attempt_status,
       alert_claim_token = CASE
           WHEN attempt.attempt_status = 'PROCESSING'
               THEN coalesce(event.alert_claim_token, gen_random_uuid())
           ELSE event.alert_claim_token
       END,
       alert_claimed_at = attempt.started_at,
       alert_lease_expires_at = attempt.lease_expires_at,
       alert_completed_at = attempt.completed_at,
       alert_successful_recipient_count = coalesce(attempt.successful_recipient_count, 0),
       alert_failed_recipient_count = coalesce(attempt.failed_recipient_count, 0),
       alert_updated_at = coalesce(attempt.updated_at, attempt.completed_at,
                                   attempt.started_at, event.updated_at, now())
  FROM ranked_attempt attempt
 WHERE attempt.attempt_rank = 1
   AND event.safety_event_id = attempt.safety_event_id
   AND event.record_type = 'EMERGENCY_SESSION';

-- The published Story 6.6 outbox was the orchestration authority around the
-- legacy attempt rows. Terminal outbox states override the attempt projection;
-- a pending claim is deliberately invalidated across the cut-over and becomes
-- retryable at its original next-attempt boundary. A prior SENT projection is
-- retained to avoid replay after an ambiguous legacy mark-delivered failure.
UPDATE public.safety_events event
   SET alert_generation = greatest(
           event.alert_generation, bridge.attempt_count::bigint),
       alert_status = CASE bridge.status
           WHEN 'DELIVERED' THEN 'SENT'
           WHEN 'SUPPRESSED' THEN 'SUPPRESSED'
           WHEN 'PENDING' THEN CASE
               WHEN event.alert_status IN ('SENT','PARTIAL','NO_RECIPIENTS')
                   THEN event.alert_status
               ELSE 'FAILED'
           END
       END,
       alert_claim_token = NULL,
       alert_claimed_at = NULL,
       alert_lease_expires_at = NULL,
       alert_completed_at = CASE
           WHEN bridge.status = 'DELIVERED'
               THEN coalesce(bridge.delivered_at, bridge.terminal_at,
                             event.alert_completed_at)
           WHEN bridge.status = 'SUPPRESSED'
               THEN coalesce(bridge.terminal_at, event.alert_completed_at)
           WHEN event.alert_status IN ('SENT','PARTIAL','NO_RECIPIENTS')
               THEN event.alert_completed_at
           ELSE NULL
       END,
       alert_successful_recipient_count = CASE
           WHEN bridge.status = 'DELIVERED'
               THEN greatest(event.alert_successful_recipient_count, 1)
           ELSE event.alert_successful_recipient_count
       END,
       alert_updated_at = CASE
           WHEN bridge.status = 'PENDING'
               THEN bridge.next_attempt_at - interval '1 minute'
           ELSE coalesce(bridge.terminal_at, bridge.delivered_at,
                         bridge.created_at)
       END,
       updated_at = now()
  FROM carebridge_migration_bridge.story66_notification_outbox_bridge bridge
 WHERE event.safety_event_id = bridge.emergency_session_id
   AND event.user_id = bridge.owner_user_id
   AND event.record_type = 'EMERGENCY_SESSION';

UPDATE public.safety_event_actions action
   SET action_phase = CASE
           WHEN action.action_type = 'ALERT_ATTEMPT'
                AND action.attempt_status = 'PROCESSING' THEN 'STARTED'
           WHEN action.action_type IN ('ALERT_ATTEMPT','DELIVERY','FAMILY_ALERT') THEN 'RESULT'
           ELSE 'RECORDED'
       END,
       alert_generation = CASE
           WHEN action.action_type = 'ALERT_ATTEMPT'
               THEN greatest(action.attempt_number, 1)
           WHEN action.action_type IN ('DELIVERY','FAMILY_ALERT')
               THEN greatest(coalesce(event.alert_generation, 0), 1)
           ELSE 0
       END,
       fence_token = CASE
           WHEN action.action_type IN ('ALERT_ATTEMPT','DELIVERY','FAMILY_ALERT')
               THEN event.alert_claim_token
           ELSE NULL
       END
  FROM public.safety_events event
 WHERE event.safety_event_id = action.safety_event_id;

UPDATE public.safety_event_actions
   SET action_phase = coalesce(action_phase, 'RECORDED'),
       alert_generation = coalesce(alert_generation, 0);

-- Preserve every completed legacy outbox generation as an immutable result.
-- A PENDING generation with attempt_count > 0 is closed as FAILED at the
-- cut-over boundary because its process-local claim is intentionally revoked;
-- the parent retry schedule above remains authoritative for the next claim.
UPDATE public.safety_event_actions action
   SET owner_user_id = bridge.owner_user_id,
       attempt_status = CASE bridge.status
           WHEN 'DELIVERED' THEN 'SENT'
           WHEN 'SUPPRESSED' THEN 'SUPPRESSED'
           WHEN 'PENDING' THEN 'FAILED'
       END,
       started_at = coalesce(action.started_at, bridge.created_at),
       completed_at = CASE bridge.status
           WHEN 'DELIVERED' THEN coalesce(
               bridge.delivered_at, bridge.terminal_at, bridge.captured_at)
           WHEN 'SUPPRESSED' THEN coalesce(
               bridge.terminal_at, bridge.captured_at)
           WHEN 'PENDING' THEN bridge.captured_at
       END,
       lease_expires_at = CASE
           WHEN bridge.status = 'PENDING' THEN bridge.next_attempt_at
           ELSE action.lease_expires_at
       END,
       successful_recipient_count = CASE
           WHEN bridge.status = 'DELIVERED'
               THEN greatest(coalesce(action.successful_recipient_count, 0), 1)
           ELSE coalesce(action.successful_recipient_count, 0)
       END,
       failed_recipient_count = coalesce(
           action.failed_recipient_count, 0),
       failure_code = bridge.last_error_code,
       fence_token = bridge.claim_token,
       updated_at = bridge.captured_at
  FROM carebridge_migration_bridge.story66_notification_outbox_bridge bridge
 WHERE bridge.attempt_count > 0
   AND action.safety_event_id = bridge.emergency_session_id
   AND action.action_type = 'ALERT_ATTEMPT'
   AND action.action_phase = 'RESULT'
   AND action.alert_generation = bridge.attempt_count;

INSERT INTO public.safety_event_actions (
    safety_event_action_id, safety_event_id, action_type, owner_user_id,
    attempt_number, idempotency_key, action_phase, alert_generation,
    fence_token, attempt_status, started_at, completed_at,
    lease_expires_at, successful_recipient_count,
    failed_recipient_count, failure_code, created_at, updated_at
)
SELECT gen_random_uuid(), bridge.emergency_session_id, 'ALERT_ATTEMPT',
       bridge.owner_user_id, bridge.attempt_count,
       'legacy-outbox-attempt:' || bridge.emergency_session_id::text || ':'
           || bridge.attempt_count::text || ':result',
       'RESULT', bridge.attempt_count, bridge.claim_token,
       CASE bridge.status
           WHEN 'DELIVERED' THEN 'SENT'
           WHEN 'SUPPRESSED' THEN 'SUPPRESSED'
           WHEN 'PENDING' THEN 'FAILED'
       END,
       bridge.created_at,
       CASE bridge.status
           WHEN 'DELIVERED' THEN coalesce(
               bridge.delivered_at, bridge.terminal_at, bridge.captured_at)
           WHEN 'SUPPRESSED' THEN coalesce(
               bridge.terminal_at, bridge.captured_at)
           WHEN 'PENDING' THEN bridge.captured_at
       END,
       CASE WHEN bridge.status = 'PENDING'
           THEN bridge.next_attempt_at ELSE NULL END,
       CASE WHEN bridge.status = 'DELIVERED' THEN 1 ELSE 0 END,
       0, bridge.last_error_code, bridge.created_at, bridge.captured_at
  FROM carebridge_migration_bridge.story66_notification_outbox_bridge bridge
 WHERE bridge.attempt_count > 0
   AND NOT EXISTS (
       SELECT 1
         FROM public.safety_event_actions existing
        WHERE existing.safety_event_id = bridge.emergency_session_id
          AND existing.action_type = 'ALERT_ATTEMPT'
          AND existing.action_phase = 'RESULT'
          AND existing.alert_generation = bridge.attempt_count
   );

ALTER TABLE public.safety_event_actions
    ALTER COLUMN action_phase SET DEFAULT 'RECORDED',
    ALTER COLUMN action_phase SET NOT NULL,
    ALTER COLUMN alert_generation SET DEFAULT 0,
    ALTER COLUMN alert_generation SET NOT NULL;

-- Reconcile duplicate ACTIVE emergency sessions before installing the partial
-- unique index.  The earliest created/id row is the deterministic survivor.
WITH ranked_active AS (
    SELECT safety_event_id,
           row_number() OVER (
               PARTITION BY user_id
               ORDER BY created_at ASC, safety_event_id ASC
           ) AS active_rank
      FROM public.safety_events
     WHERE record_type = 'EMERGENCY_SESSION'
       AND status = 'ACTIVE'
)
UPDATE public.safety_events event
   SET status = 'CANCELLED',
       resolved_at = coalesce(event.resolved_at, now()),
       alert_status = CASE
           WHEN event.alert_status = 'SENT' THEN event.alert_status
           ELSE 'SUPPRESSED'
       END,
       alert_lease_expires_at = NULL,
       alert_updated_at = now(),
       updated_at = now()
  FROM ranked_active ranked
 WHERE event.safety_event_id = ranked.safety_event_id
   AND ranked.active_rank > 1;

CREATE UNIQUE INDEX IF NOT EXISTS safety_events_one_active_emergency_user_uk
    ON public.safety_events(user_id)
    WHERE record_type = 'EMERGENCY_SESSION' AND status = 'ACTIVE';

-- Runtime enable/disable takes the same per-user advisory fence. Reconcile
-- historical duplicates before installing the database backstop: the earliest
-- started/id row survives and every later ACTIVE row becomes STOPPED.
WITH ranked_monitoring AS (
    SELECT monitoring_session_id,
           row_number() OVER (
               PARTITION BY user_id
               ORDER BY started_at ASC, monitoring_session_id ASC
           ) AS active_rank
      FROM public.safety_monitoring_sessions
     WHERE status = 'ACTIVE'
)
UPDATE public.safety_monitoring_sessions session
   SET status = 'STOPPED',
       ended_at = coalesce(session.ended_at, session.started_at)
  FROM ranked_monitoring ranked
 WHERE session.monitoring_session_id = ranked.monitoring_session_id
   AND ranked.active_rank > 1;

CREATE UNIQUE INDEX IF NOT EXISTS safety_monitoring_sessions_one_active_user_uk
    ON public.safety_monitoring_sessions(user_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS safety_events_event_owner_uk
    ON public.safety_events(safety_event_id, user_id);

CREATE UNIQUE INDEX IF NOT EXISTS triage_sessions_session_owner_uk
    ON public.triage_sessions(triage_session_id, user_id);

-- Allow the canonical escalation action before backfilling it.
ALTER TABLE public.safety_event_actions
    DROP CONSTRAINT IF EXISTS safety_event_actions_type_ck;
ALTER TABLE public.safety_event_actions
    ADD CONSTRAINT safety_event_actions_type_ck
    CHECK (action_type IN ('RESPONSE','DELIVERY','FAMILY_ALERT','ALERT_ATTEMPT',
                           'MAP_HANDOFF','LOCATION_SNAPSHOT','TRIAGE_ESCALATION'));

-- Preserve every valid legacy source_event_id association.  source_event_id is
-- deliberately retained as compatibility metadata, but all runtime lookups use
-- this journal row after the migration.
INSERT INTO public.safety_event_actions (
    safety_event_action_id, safety_event_id, action_type, owner_user_id,
    triage_handoff_id, attempt_number, idempotency_key, action_phase,
    alert_generation, created_at
)
SELECT gen_random_uuid(), event.safety_event_id, 'TRIAGE_ESCALATION', event.user_id,
       event.source_event_id, 1, 'triage-escalation:' || event.source_event_id::text,
       'LINKED', 0, coalesce(event.created_at, now())
  FROM public.safety_events event
  JOIN public.triage_sessions intake
    ON intake.triage_session_id = event.source_event_id
   AND intake.user_id = event.user_id
 WHERE event.record_type = 'EMERGENCY_SESSION'
   AND event.source_event_id IS NOT NULL
ON CONFLICT (idempotency_key) DO NOTHING;

-- V20260722231360 captures these links before Wave 4/Wave 8 remove the
-- legacy parents. Consume every owner-bound bridge row into the immutable
-- canonical journal.
INSERT INTO public.safety_event_actions (
    safety_event_action_id, safety_event_id, action_type, owner_user_id,
    triage_handoff_id, attempt_number, idempotency_key, action_phase,
    alert_generation, created_at
)
SELECT gen_random_uuid(), event.safety_event_id, 'TRIAGE_ESCALATION',
       bridge.owner_user_id, bridge.intake_session_id, 1,
       'triage-escalation:' || bridge.intake_session_id::text,
       'LINKED', 0, bridge.triggered_at
  FROM carebridge_migration_bridge.story66_triage_escalation_bridge bridge
  JOIN public.triage_sessions intake
    ON intake.triage_session_id = bridge.intake_session_id
   AND intake.user_id = bridge.owner_user_id
  JOIN public.safety_events event
    ON event.safety_event_id = bridge.emergency_session_id
   AND event.user_id = bridge.owner_user_id
   AND event.record_type = 'EMERGENCY_SESSION'
ON CONFLICT (idempotency_key) DO NOTHING;

-- Historical MAP_HANDOFF rows created before owner validation may contain an
-- unresolvable triage UUID.  Keep the immutable evidence but mark the context as
-- legacy; all new RECORDED rows must carry an owner-bound triage session.
UPDATE public.safety_event_actions action
   SET action_phase = 'LEGACY',
       triage_handoff_id = NULL
 WHERE action.action_type = 'MAP_HANDOFF'
   AND NOT EXISTS (
       SELECT 1
         FROM public.triage_sessions intake
        WHERE intake.triage_session_id = action.triage_handoff_id
          AND intake.user_id = action.owner_user_id
   );

DROP INDEX IF EXISTS public.safety_event_actions_delivery_device_uk;
DROP INDEX IF EXISTS public.safety_event_actions_delivery_token_uk;
DROP INDEX IF EXISTS public.safety_event_actions_family_alert_uk;
DROP INDEX IF EXISTS public.safety_event_actions_attempt_event_uk;

CREATE UNIQUE INDEX IF NOT EXISTS safety_event_actions_attempt_generation_phase_uk
    ON public.safety_event_actions(safety_event_id, alert_generation, action_phase)
    WHERE action_type = 'ALERT_ATTEMPT';

CREATE UNIQUE INDEX IF NOT EXISTS safety_event_actions_delivery_generation_device_phase_uk
    ON public.safety_event_actions(
        safety_event_id,
        alert_generation,
        (coalesce(device_token_id::text, device_identifier)),
        action_phase
    )
    WHERE action_type = 'DELIVERY';

CREATE UNIQUE INDEX IF NOT EXISTS safety_event_actions_family_generation_uk
    ON public.safety_event_actions(safety_event_id, alert_generation)
    WHERE action_type = 'FAMILY_ALERT';

CREATE UNIQUE INDEX IF NOT EXISTS safety_event_actions_triage_intake_uk
    ON public.safety_event_actions(triage_handoff_id)
    WHERE action_type = 'TRIAGE_ESCALATION';

CREATE INDEX IF NOT EXISTS safety_events_alert_retry_ix
    ON public.safety_events(alert_status, alert_updated_at, alert_lease_expires_at)
    WHERE record_type = 'EMERGENCY_SESSION' AND status = 'ACTIVE';

DO $canonical_safety_constraints$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conname = 'safety_event_actions_related_action_fk') THEN
        ALTER TABLE public.safety_event_actions
            ADD CONSTRAINT safety_event_actions_related_action_fk
            FOREIGN KEY (related_action_id)
            REFERENCES public.safety_event_actions(safety_event_action_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conname = 'safety_event_actions_triage_owner_fk') THEN
        ALTER TABLE public.safety_event_actions
            ADD CONSTRAINT safety_event_actions_triage_owner_fk
            FOREIGN KEY (triage_handoff_id, owner_user_id)
            REFERENCES public.triage_sessions(triage_session_id, user_id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conname = 'safety_event_actions_event_owner_fk') THEN
        ALTER TABLE public.safety_event_actions
            ADD CONSTRAINT safety_event_actions_event_owner_fk
            FOREIGN KEY (safety_event_id, owner_user_id)
            REFERENCES public.safety_events(safety_event_id, user_id);
    END IF;
END $canonical_safety_constraints$;

ALTER TABLE public.safety_events
    ADD CONSTRAINT safety_events_alert_generation_ck CHECK (alert_generation >= 0),
    ADD CONSTRAINT safety_events_alert_recipient_counts_ck
        CHECK (alert_successful_recipient_count >= 0 AND alert_failed_recipient_count >= 0),
    ADD CONSTRAINT safety_events_alert_status_ck
        CHECK (alert_status IS NULL OR alert_status IN
               ('PROCESSING','FAILED','PARTIAL','NO_RECIPIENTS','SENT','SUPPRESSED')),
    ADD CONSTRAINT safety_events_alert_claim_ck
        CHECK ((alert_status = 'PROCESSING' AND alert_claim_token IS NOT NULL
                AND alert_lease_expires_at IS NOT NULL)
            OR alert_status IS DISTINCT FROM 'PROCESSING');

ALTER TABLE public.safety_event_actions
    ADD CONSTRAINT safety_event_actions_generation_ck CHECK (alert_generation >= 0),
    ADD CONSTRAINT safety_event_actions_snapshot_ck CHECK (
        (action_type = 'ALERT_ATTEMPT'
            AND alert_generation > 0
            AND action_phase IN ('STARTED','RESULT'))
        OR (action_type = 'DELIVERY'
            AND alert_generation > 0
            AND action_phase IN ('INTENT','RESULT'))
        OR (action_type = 'FAMILY_ALERT'
            AND alert_generation > 0
            AND action_phase = 'RESULT')
        OR (action_type = 'TRIAGE_ESCALATION'
            AND action_phase = 'LINKED'
            AND safety_event_id IS NOT NULL
            AND owner_user_id IS NOT NULL
            AND triage_handoff_id IS NOT NULL)
        OR (action_type = 'MAP_HANDOFF'
            AND (action_phase = 'LEGACY'
                 OR (action_phase = 'RECORDED'
                     AND owner_user_id IS NOT NULL
                     AND triage_handoff_id IS NOT NULL)))
        OR action_type IN ('RESPONSE','LOCATION_SNAPSHOT')
    );

CREATE OR REPLACE FUNCTION public.reject_safety_event_action_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $immutable_safety_action$
BEGIN
    RAISE EXCEPTION 'SAFETY_EVENT_ACTION_IMMUTABLE: % is not allowed', TG_OP
        USING ERRCODE = '55000';
END
$immutable_safety_action$;

DROP TRIGGER IF EXISTS safety_event_actions_immutable_trg
    ON public.safety_event_actions;
CREATE TRIGGER safety_event_actions_immutable_trg
    BEFORE UPDATE OR DELETE ON public.safety_event_actions
    FOR EACH ROW EXECUTE FUNCTION public.reject_safety_event_action_mutation();

DO $story66_bridge_consumption_gate$
BEGIN
    IF to_regclass('public.triage_emergency_escalations') IS NOT NULL
       OR to_regclass('public.emergency_notification_outbox') IS NOT NULL THEN
        RAISE EXCEPTION 'STORY66_LEGACY_SAFETY_TABLE_REAPPEARED';
    END IF;

    IF to_regclass(
        'carebridge_migration_bridge.story66_triage_escalation_bridge'
    ) IS NULL OR to_regclass(
        'carebridge_migration_bridge.story66_notification_outbox_bridge'
    ) IS NULL THEN
        RAISE EXCEPTION 'STORY66_SAFETY_BRIDGE_STATE_MISSING';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM carebridge_migration_bridge.story66_triage_escalation_bridge bridge
          LEFT JOIN public.safety_event_actions action
            ON action.action_type = 'TRIAGE_ESCALATION'
           AND action.action_phase = 'LINKED'
           AND action.safety_event_id = bridge.emergency_session_id
           AND action.owner_user_id = bridge.owner_user_id
           AND action.triage_handoff_id = bridge.intake_session_id
         WHERE action.safety_event_action_id IS NULL
    ) THEN
        RAISE EXCEPTION 'STORY66_TRIAGE_ESCALATION_BRIDGE_CONSUMPTION_FAILED';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM carebridge_migration_bridge.story66_notification_outbox_bridge bridge
          LEFT JOIN public.safety_events event
            ON event.safety_event_id = bridge.emergency_session_id
           AND event.user_id = bridge.owner_user_id
           AND event.record_type = 'EMERGENCY_SESSION'
           AND event.alert_generation >= bridge.attempt_count
           AND event.alert_claim_token IS NULL
           AND event.alert_lease_expires_at IS NULL
           AND event.alert_updated_at = CASE
                WHEN bridge.status = 'PENDING'
                    THEN bridge.next_attempt_at - interval '1 minute'
                ELSE coalesce(bridge.terminal_at, bridge.delivered_at,
                              bridge.created_at)
           END
           AND (
                (bridge.status = 'DELIVERED'
                    AND event.alert_status = 'SENT'
                    AND event.alert_completed_at = coalesce(
                        bridge.delivered_at, bridge.terminal_at,
                        event.alert_completed_at)
                    AND event.alert_successful_recipient_count >= 1)
                OR (bridge.status = 'SUPPRESSED'
                    AND event.alert_status = 'SUPPRESSED'
                    AND event.alert_completed_at = coalesce(
                        bridge.terminal_at, event.alert_completed_at))
                OR (bridge.status = 'PENDING'
                    AND event.alert_status IN
                        ('FAILED','SENT','PARTIAL','NO_RECIPIENTS'))
           )
         WHERE event.safety_event_id IS NULL
    ) THEN
        RAISE EXCEPTION 'STORY66_NOTIFICATION_OUTBOX_BRIDGE_CONSUMPTION_FAILED';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM carebridge_migration_bridge.story66_notification_outbox_bridge bridge
         WHERE bridge.attempt_count > 0
           AND NOT EXISTS (
               SELECT 1
                 FROM public.safety_event_actions action
                WHERE action.safety_event_id = bridge.emergency_session_id
                  AND action.owner_user_id = bridge.owner_user_id
                  AND action.action_type = 'ALERT_ATTEMPT'
                  AND action.action_phase = 'RESULT'
                  AND action.alert_generation = bridge.attempt_count
                  AND action.attempt_number = bridge.attempt_count
                  AND action.attempt_status = CASE bridge.status
                      WHEN 'DELIVERED' THEN 'SENT'
                      WHEN 'SUPPRESSED' THEN 'SUPPRESSED'
                      WHEN 'PENDING' THEN 'FAILED'
                  END
                  AND action.completed_at = CASE bridge.status
                      WHEN 'DELIVERED' THEN coalesce(
                          bridge.delivered_at, bridge.terminal_at,
                          bridge.captured_at)
                      WHEN 'SUPPRESSED' THEN coalesce(
                          bridge.terminal_at, bridge.captured_at)
                      WHEN 'PENDING' THEN bridge.captured_at
                  END
                  AND action.failure_code IS NOT DISTINCT FROM
                      bridge.last_error_code
                  AND action.fence_token IS NOT DISTINCT FROM bridge.claim_token
                  AND (bridge.status <> 'PENDING'
                       OR action.lease_expires_at = bridge.next_attempt_at)
           )
    ) THEN
        RAISE EXCEPTION 'STORY66_OUTBOX_ATTEMPT_SNAPSHOT_CONSUMPTION_FAILED';
    END IF;
END
$story66_bridge_consumption_gate$;

DROP TABLE carebridge_migration_bridge.story66_notification_outbox_bridge;
DROP TABLE carebridge_migration_bridge.story66_triage_escalation_bridge;

DO $drop_empty_story66_safety_bridge_schema$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_class relation
          JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
         WHERE namespace.nspname = 'carebridge_migration_bridge'
    ) AND NOT EXISTS (
        SELECT 1
          FROM pg_proc routine
          JOIN pg_namespace namespace ON namespace.oid = routine.pronamespace
         WHERE namespace.nspname = 'carebridge_migration_bridge'
    ) AND NOT EXISTS (
        SELECT 1
          FROM pg_type type_entry
          JOIN pg_namespace namespace ON namespace.oid = type_entry.typnamespace
         WHERE namespace.nspname = 'carebridge_migration_bridge'
    ) THEN
        DROP SCHEMA carebridge_migration_bridge;
    END IF;
END
$drop_empty_story66_safety_bridge_schema$;

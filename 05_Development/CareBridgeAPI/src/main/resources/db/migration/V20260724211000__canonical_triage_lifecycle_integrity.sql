-- Canonical triage/lifecycle integrity remediation.
-- Supports both a clean migration chain and databases already cut over through
-- Phase 2 before the unpublished Story 6.7 migration became visible.

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

CREATE OR REPLACE FUNCTION pg_temp.carebridge_try_jsonb(value text)
RETURNS jsonb
LANGUAGE plpgsql
IMMUTABLE
STRICT
AS $$
BEGIN
    RETURN value::jsonb;
EXCEPTION WHEN others THEN
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION pg_temp.carebridge_try_uuid(value text)
RETURNS uuid
LANGUAGE plpgsql
IMMUTABLE
STRICT
AS $$
BEGIN
    RETURN value::uuid;
EXCEPTION WHEN others THEN
    RETURN NULL;
END;
$$;

-- A partially upgraded database may still expose the original Story 6.7
-- projection table. Copy it into the FK-free bridge before any canonical
-- reconciliation so the source rows can be consumed uniformly.
DO $capture_late_story67_outcomes$
BEGIN
    IF to_regclass('public.lifecycle_safety_outcomes') IS NULL THEN
        RETURN;
    END IF;

    EXECUTE $copy$
        INSERT INTO carebridge_migration_bridge.lifecycle_safety_outcome_bridge (
            outcome_id, owner_user_id, journey_id, triage_session_id,
            emergency_session_id, risk_level, stage, origin_dashboard,
            origin_reference_id, origin_action, occurred_at, recorded_at,
            captured_at)
        SELECT outcome_id, owner_user_id, journey_id, intake_session_id,
               emergency_session_id, risk_level, stage, origin_dashboard,
               origin_reference_id, origin_action, occurred_at, recorded_at,
               now()
          FROM public.lifecycle_safety_outcomes
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
            captured_at = excluded.captured_at
    $copy$;
END
$capture_late_story67_outcomes$;

ALTER TABLE public.triage_sessions
    ADD COLUMN IF NOT EXISTS journey_id uuid,
    ADD COLUMN IF NOT EXISTS origin_dashboard varchar(30),
    ADD COLUMN IF NOT EXISTS origin_reference_id uuid,
    ADD COLUMN IF NOT EXISTS continuation_token uuid,
    ADD COLUMN IF NOT EXISTS continuation_expires_at timestamptz,
    ADD COLUMN IF NOT EXISTS continuation_acknowledged_at timestamptz,
    ADD COLUMN IF NOT EXISTS emergency boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS result_jsonb jsonb NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS schema_version varchar(30) NOT NULL DEFAULT '1',
    ADD COLUMN IF NOT EXISTS content_hash varchar(128);

-- Add the projection scalars early so an interrupted/manual prior
-- reconciliation can also restore its triage source before immutability.
ALTER TABLE public.mother_journey_events
    ADD COLUMN IF NOT EXISTS triage_session_id uuid,
    ADD COLUMN IF NOT EXISTS emergency_session_id uuid,
    ADD COLUMN IF NOT EXISTS risk_level varchar(10),
    ADD COLUMN IF NOT EXISTS stage varchar(20),
    ADD COLUMN IF NOT EXISTS origin_dashboard varchar(30),
    ADD COLUMN IF NOT EXISTS origin_reference_id uuid,
    ADD COLUMN IF NOT EXISTS origin_action varchar(40);

DO $bridge_owner_reconcile$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM carebridge_migration_bridge.triage_lifecycle_bridge bridge
          JOIN public.triage_sessions session
            ON session.triage_session_id = bridge.triage_session_id
         WHERE session.user_id <> bridge.owner_user_id
    ) THEN
        RAISE EXCEPTION 'TRIAGE_LIFECYCLE_BRIDGE_OWNER_MISMATCH';
    END IF;
END
$bridge_owner_reconcile$;

UPDATE public.triage_sessions session
   SET journey_id = coalesce(bridge.journey_id, session.journey_id),
       origin_dashboard = coalesce(bridge.origin_dashboard, session.origin_dashboard),
       origin_reference_id = coalesce(bridge.origin_reference_id, session.origin_reference_id),
       continuation_token = coalesce(bridge.continuation_token, session.continuation_token),
       continuation_expires_at = coalesce(
           bridge.continuation_expires_at, session.continuation_expires_at),
       continuation_acknowledged_at = coalesce(
           bridge.continuation_acknowledged_at, session.continuation_acknowledged_at)
  FROM carebridge_migration_bridge.triage_lifecycle_bridge bridge
 WHERE session.triage_session_id = bridge.triage_session_id
   AND session.user_id = bridge.owner_user_id;

DO $late_legacy_lifecycle_copy$
BEGIN
    IF to_regclass('public.intake_sessions') IS NOT NULL
       AND (
           SELECT count(*)
             FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'intake_sessions'
              AND column_name IN (
                  'journey_id','origin_dashboard','origin_reference_id',
                  'continuation_token','continuation_expires_at',
                  'continuation_acknowledged_at')
       ) = 6 THEN
        EXECUTE $copy$
            UPDATE public.triage_sessions session
               SET journey_id = coalesce(legacy.journey_id, session.journey_id),
                   origin_dashboard = coalesce(
                       legacy.origin_dashboard, session.origin_dashboard),
                   origin_reference_id = coalesce(
                       legacy.origin_reference_id, session.origin_reference_id),
                   continuation_token = coalesce(
                       legacy.continuation_token, session.continuation_token),
                   continuation_expires_at = coalesce(
                       legacy.continuation_expires_at,
                       session.continuation_expires_at),
                   continuation_acknowledged_at = coalesce(
                       legacy.continuation_acknowledged_at,
                       session.continuation_acknowledged_at)
              FROM public.intake_sessions legacy
             WHERE session.triage_session_id = legacy.id
               AND session.user_id = legacy.user_id
        $copy$;
    END IF;
END
$late_legacy_lifecycle_copy$;

-- Restore lifecycle provenance from the durable Story 6.7 projection bridge.
-- This source is authoritative for historical terminal sessions even when a
-- pre-bridge Phase 2 cutover omitted the lifecycle columns.
UPDATE public.triage_sessions session
   SET journey_id = coalesce(session.journey_id, outcome.journey_id),
       origin_dashboard = coalesce(
           session.origin_dashboard, outcome.origin_dashboard),
       origin_reference_id = coalesce(
           session.origin_reference_id, outcome.origin_reference_id)
  FROM carebridge_migration_bridge.lifecycle_safety_outcome_bridge outcome
 WHERE session.triage_session_id = outcome.triage_session_id
   AND session.user_id = outcome.owner_user_id;

-- A canonical event may already exist on a manually reconciled database.
-- Recover its minimum lifecycle binding before completed snapshots become
-- immutable. Invalid or conflicting source data is rejected by the gate below.
UPDATE public.triage_sessions session
   SET journey_id = coalesce(session.journey_id, event.mother_journey_id),
       origin_dashboard = coalesce(
           session.origin_dashboard,
           event.origin_dashboard,
           nullif(event.event_payload_jsonb ->> 'originDashboard', '')),
       origin_reference_id = coalesce(
           session.origin_reference_id,
           event.origin_reference_id,
           pg_temp.carebridge_try_uuid(
               nullif(event.event_payload_jsonb ->> 'originReferenceId', '')))
  FROM public.mother_journey_events event
 WHERE event.event_type = 'SAFETY_OUTCOME'
   AND session.triage_session_id = coalesce(
           event.triage_session_id,
           pg_temp.carebridge_try_uuid(
               nullif(event.event_payload_jsonb ->> 'intakeSessionId', '')))
   AND session.user_id = event.owner_user_id;

-- Historical outcomes did not always retain a continuation. Create a retired,
-- acknowledged token rather than exposing a newly usable continuation.
UPDATE public.triage_sessions session
   SET continuation_token = coalesce(
           session.continuation_token, gen_random_uuid()),
       continuation_expires_at = least(
           coalesce(
               session.continuation_expires_at,
               coalesce(session.completed_at, session.updated_at,
                        session.created_at, now())),
           coalesce(session.completed_at, session.updated_at,
                    session.created_at, now())),
       continuation_acknowledged_at = coalesce(
           session.continuation_acknowledged_at,
           coalesce(session.completed_at, session.updated_at,
                    session.created_at, now()))
 WHERE session.status = 'COMPLETED'
   AND session.journey_id IS NOT NULL
   AND session.origin_dashboard IS NOT NULL
   AND session.origin_reference_id IS NOT NULL
   AND (session.continuation_token IS NULL
        OR session.continuation_expires_at IS NULL);

UPDATE public.triage_sessions
   SET emergency = true
 WHERE risk_level = 'RED'
   AND NOT emergency;

UPDATE public.triage_sessions session
   SET result_jsonb = pg_temp.carebridge_try_jsonb(session.raw_ai_response)
 WHERE session.status = 'COMPLETED'
   AND pg_temp.carebridge_try_jsonb(session.raw_ai_response) IS NOT NULL
   AND jsonb_typeof(pg_temp.carebridge_try_jsonb(session.raw_ai_response)) = 'object'
   AND (session.result_jsonb = '{}'::jsonb
        OR session.result_jsonb ? 'rawAiResponse');

UPDATE public.triage_sessions
   SET schema_version = coalesce(
           nullif(result_jsonb #>> '{triageResult,responseSchemaVersion}', ''),
           nullif(result_jsonb ->> 'responseSchemaVersion', ''),
           nullif(schema_version, ''),
           '1'),
       content_hash = coalesce(
           nullif(content_hash, ''),
           md5(result_jsonb::text))
 WHERE status = 'COMPLETED';

CREATE OR REPLACE FUNCTION public.carebridge_guard_completed_triage_snapshot()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.status = 'COMPLETED' THEN
            RAISE EXCEPTION 'completed triage snapshot is immutable';
        END IF;
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' AND OLD.status = 'COMPLETED' THEN
        IF (to_jsonb(NEW) - ARRAY[
                'continuation_acknowledged_at','updated_at',
                'symptom_list','duration_days','intensity','emergency_flag',
                'extracted_at','structured_created_by'])
                IS DISTINCT FROM
           (to_jsonb(OLD) - ARRAY[
                'continuation_acknowledged_at','updated_at',
                'symptom_list','duration_days','intensity','emergency_flag',
                'extracted_at','structured_created_by']) THEN
            RAISE EXCEPTION 'completed triage snapshot is immutable';
        END IF;
        IF NEW.continuation_acknowledged_at IS DISTINCT FROM
               OLD.continuation_acknowledged_at
           AND NOT (
               OLD.continuation_acknowledged_at IS NULL
               AND NEW.continuation_acknowledged_at IS NOT NULL) THEN
            RAISE EXCEPTION 'completed triage acknowledgement is one-way';
        END IF;
    END IF;

    IF NEW.status = 'COMPLETED' THEN
        NEW.result_jsonb := coalesce(NEW.result_jsonb, '{}'::jsonb);
        NEW.schema_version := coalesce(nullif(NEW.schema_version, ''), '1');
        NEW.content_hash := coalesce(
            nullif(NEW.content_hash, ''), md5(NEW.result_jsonb::text));
        IF NEW.risk_level = 'RED' THEN
            NEW.emergency := true;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_mother_journeys_id_owner
    ON public.mother_journeys(journey_id, owner_user_id);
CREATE UNIQUE INDEX IF NOT EXISTS triage_sessions_session_owner_uk
    ON public.triage_sessions(triage_session_id, user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_triage_sessions_continuation_token
    ON public.triage_sessions(continuation_token)
    WHERE continuation_token IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_triage_sessions_journey
    ON public.triage_sessions(journey_id, completed_at DESC)
    WHERE journey_id IS NOT NULL;

DO $triage_constraints$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.triage_sessions'::regclass
          AND conname = 'fk_triage_journey_owner'
    ) THEN
        ALTER TABLE public.triage_sessions
            ADD CONSTRAINT fk_triage_journey_owner
            FOREIGN KEY (journey_id, user_id)
            REFERENCES public.mother_journeys(journey_id, owner_user_id)
            ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.triage_sessions'::regclass
          AND conname = 'chk_triage_lifecycle_binding'
    ) THEN
        ALTER TABLE public.triage_sessions
            ADD CONSTRAINT chk_triage_lifecycle_binding CHECK (
                (journey_id IS NULL
                    AND origin_dashboard IS NULL
                    AND origin_reference_id IS NULL
                    AND continuation_token IS NULL
                    AND continuation_expires_at IS NULL
                    AND continuation_acknowledged_at IS NULL)
                OR
                (journey_id IS NOT NULL
                    AND origin_dashboard IS NOT NULL
                    AND origin_reference_id IS NOT NULL
                    AND continuation_token IS NOT NULL
                    AND continuation_expires_at IS NOT NULL));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.triage_sessions'::regclass
          AND conname = 'chk_triage_origin_dashboard'
    ) THEN
        ALTER TABLE public.triage_sessions
            ADD CONSTRAINT chk_triage_origin_dashboard CHECK (
                origin_dashboard IS NULL
                OR origin_dashboard IN ('MOTHER_JOURNEY','BABY_PROFILE'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.triage_sessions'::regclass
          AND conname = 'chk_triage_origin_stage'
    ) THEN
        ALTER TABLE public.triage_sessions
            ADD CONSTRAINT chk_triage_origin_stage CHECK (
                origin_dashboard IS NULL
                OR (origin_dashboard = 'MOTHER_JOURNEY'
                    AND origin_reference_id = journey_id
                    AND stage IN ('PRECONCEPTION','PREGNANCY','POSTPARTUM'))
                OR (origin_dashboard = 'BABY_PROFILE'
                    AND stage IN ('INFANT','TODDLER')));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.triage_sessions'::regclass
          AND conname = 'chk_triage_completed_snapshot'
    ) THEN
        ALTER TABLE public.triage_sessions
            ADD CONSTRAINT chk_triage_completed_snapshot CHECK (
                status <> 'COMPLETED'
                OR (nullif(schema_version, '') IS NOT NULL
                    AND nullif(content_hash, '') IS NOT NULL
                    AND jsonb_typeof(result_jsonb) = 'object'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.triage_sessions'::regclass
          AND conname = 'chk_triage_red_emergency'
    ) THEN
        ALTER TABLE public.triage_sessions
            ADD CONSTRAINT chk_triage_red_emergency CHECK (
                risk_level <> 'RED' OR emergency);
    END IF;
END
$triage_constraints$;

-- Lifecycle restoration and retired-continuation synthesis are complete.
-- From this point onward, completed source snapshots are immutable.
DROP TRIGGER IF EXISTS triage_completed_snapshot_guard_trg
    ON public.triage_sessions;
CREATE TRIGGER triage_completed_snapshot_guard_trg
BEFORE INSERT OR UPDATE OR DELETE ON public.triage_sessions
FOR EACH ROW EXECUTE FUNCTION public.carebridge_guard_completed_triage_snapshot();

-- Backfill validated citation snapshots. Rows are immutable and uniquely
-- identified by their canonical JSON hash, making reruns idempotent.
WITH completed AS (
    SELECT session.triage_session_id,
           CASE
               WHEN jsonb_typeof(session.result_jsonb -> 'triageResult') = 'object'
                   THEN session.result_jsonb -> 'triageResult'
               ELSE session.result_jsonb
           END AS result
      FROM public.triage_sessions session
     WHERE session.status = 'COMPLETED'
), citation_rows AS (
    SELECT completed.triage_session_id,
           citation.value AS citation,
           lower(regexp_replace(
               coalesce(nullif(citation.value ->> 'domain', ''),
                        substring(citation.value ->> 'url'
                                  from '^https://([^/:?#]+)')),
               '^www\.', '')) AS canonical_domain
      FROM completed
      CROSS JOIN LATERAL jsonb_array_elements(
          CASE WHEN jsonb_typeof(completed.result -> 'citations') = 'array'
               THEN completed.result -> 'citations'
               ELSE '[]'::jsonb END) citation
)
INSERT INTO public.triage_session_evidence (
    evidence_id, triage_session_id, evidence_type, claim_code, claim_text,
    knowledge_source_id, citation_url, citation_domain, source_version,
    source_snapshot_jsonb, content_hash, created_at)
SELECT gen_random_uuid(), row.triage_session_id, 'CITATION',
       coalesce(nullif(row.citation ->> 'sourceId', ''),
                nullif(row.citation ->> 'id', '')),
       coalesce(nullif(row.citation ->> 'excerpt', ''),
                row.citation ->> 'title'),
       source.knowledge_source_id,
       row.citation ->> 'url', row.canonical_domain,
       coalesce(nullif(row.citation ->> 'sourceVersion', ''), 'legacy-unknown'),
       row.citation, md5(row.citation::text), now()
  FROM citation_rows row
  LEFT JOIN LATERAL (
      SELECT candidate.knowledge_source_id
        FROM public.knowledge_sources candidate
       WHERE candidate.knowledge_source_id =
                 pg_temp.carebridge_try_uuid(row.citation ->> 'sourceId')
          OR lower(candidate.domain) = row.canonical_domain
       ORDER BY CASE
           WHEN candidate.knowledge_source_id =
                    pg_temp.carebridge_try_uuid(row.citation ->> 'sourceId')
               THEN 0 ELSE 1 END
       LIMIT 1
  ) source ON true
 WHERE jsonb_typeof(row.citation) = 'object'
   AND nullif(row.citation ->> 'title', '') IS NOT NULL
   AND coalesce(nullif(row.citation ->> 'organization', ''),
                nullif(row.citation ->> 'source', '')) IS NOT NULL
   AND nullif(row.citation ->> 'excerpt', '') IS NOT NULL
   AND row.citation ->> 'url' ~* '^https://[^[:space:]]+$'
ON CONFLICT (triage_session_id, evidence_type, content_hash) DO NOTHING;

WITH completed AS (
    SELECT session.triage_session_id,
           CASE
               WHEN jsonb_typeof(session.result_jsonb -> 'triageResult') = 'object'
                   THEN session.result_jsonb -> 'triageResult'
               ELSE session.result_jsonb
           END AS result
      FROM public.triage_sessions session
     WHERE session.status = 'COMPLETED'
), claim_rows AS (
    SELECT completed.triage_session_id, completed.result,
           claim.value AS claim
      FROM completed
      CROSS JOIN LATERAL jsonb_array_elements(
          CASE WHEN jsonb_typeof(completed.result -> 'claims') = 'array'
               THEN completed.result -> 'claims'
               ELSE '[]'::jsonb END) claim
)
INSERT INTO public.triage_session_evidence (
    evidence_id, triage_session_id, evidence_type, claim_code, claim_text,
    knowledge_source_id, citation_url, citation_domain, source_version,
    source_snapshot_jsonb, content_hash, created_at)
SELECT gen_random_uuid(), row.triage_session_id, 'CLAIM',
       row.claim ->> 'claimId', row.claim ->> 'text',
       source.knowledge_source_id,
       linked.citation ->> 'url',
       lower(regexp_replace(
           coalesce(nullif(linked.citation ->> 'domain', ''),
                    substring(linked.citation ->> 'url'
                              from '^https://([^/:?#]+)')),
           '^www\.', '')),
       coalesce(nullif(linked.citation ->> 'sourceVersion', ''), 'legacy-unknown'),
       row.claim, md5(row.claim::text), now()
  FROM claim_rows row
  JOIN LATERAL (
      SELECT citation.value AS citation
        FROM jsonb_array_elements(
            CASE WHEN jsonb_typeof(row.result -> 'citations') = 'array'
                 THEN row.result -> 'citations'
                 ELSE '[]'::jsonb END) citation
       WHERE citation.value ->> 'sourceId' IN (
           SELECT evidence_id.value
             FROM jsonb_array_elements_text(
                 CASE WHEN jsonb_typeof(row.claim -> 'evidenceIds') = 'array'
                      THEN row.claim -> 'evidenceIds'
                      ELSE '[]'::jsonb END) evidence_id)
       LIMIT 1
  ) linked ON true
  LEFT JOIN LATERAL (
      SELECT candidate.knowledge_source_id
        FROM public.knowledge_sources candidate
       WHERE candidate.knowledge_source_id =
                 pg_temp.carebridge_try_uuid(linked.citation ->> 'sourceId')
          OR lower(candidate.domain) = lower(regexp_replace(
                 coalesce(nullif(linked.citation ->> 'domain', ''),
                          substring(linked.citation ->> 'url'
                                    from '^https://([^/:?#]+)')),
                 '^www\.', ''))
       ORDER BY CASE
           WHEN candidate.knowledge_source_id =
                    pg_temp.carebridge_try_uuid(linked.citation ->> 'sourceId')
               THEN 0 ELSE 1 END
       LIMIT 1
  ) source ON true
 WHERE jsonb_typeof(row.claim) = 'object'
   AND nullif(row.claim ->> 'claimId', '') IS NOT NULL
   AND nullif(row.claim ->> 'text', '') IS NOT NULL
   AND jsonb_typeof(row.claim -> 'evidenceIds') = 'array'
ON CONFLICT (triage_session_id, evidence_type, content_hash) DO NOTHING;

ALTER TABLE public.mother_journey_events
    ADD COLUMN IF NOT EXISTS triage_session_id uuid,
    ADD COLUMN IF NOT EXISTS emergency_session_id uuid,
    ADD COLUMN IF NOT EXISTS risk_level varchar(10),
    ADD COLUMN IF NOT EXISTS stage varchar(20),
    ADD COLUMN IF NOT EXISTS origin_dashboard varchar(30),
    ADD COLUMN IF NOT EXISTS origin_reference_id uuid,
    ADD COLUMN IF NOT EXISTS origin_action varchar(40);

-- Canonical event history remains append-only. The trigger is removed only
-- inside this migration transaction while historical JSON snapshots and the
-- retired lifecycle table are reconciled into scalar columns.
DROP TRIGGER IF EXISTS mother_journey_events_immutable_trg
    ON public.mother_journey_events;

UPDATE public.mother_journey_events event
   SET triage_session_id = coalesce(
           event.triage_session_id,
           pg_temp.carebridge_try_uuid(
               nullif(event.event_payload_jsonb ->> 'intakeSessionId', ''))),
       emergency_session_id = coalesce(
           event.emergency_session_id,
           pg_temp.carebridge_try_uuid(
               nullif(event.event_payload_jsonb ->> 'emergencySessionId', ''))),
       risk_level = coalesce(
           event.risk_level,
           nullif(event.event_payload_jsonb ->> 'riskLevel', '')),
       stage = coalesce(
           event.stage,
           nullif(event.event_payload_jsonb ->> 'stage', '')),
       origin_dashboard = coalesce(
           event.origin_dashboard,
           nullif(event.event_payload_jsonb ->> 'originDashboard', '')),
       origin_reference_id = coalesce(
           event.origin_reference_id,
           pg_temp.carebridge_try_uuid(
               nullif(event.event_payload_jsonb ->> 'originReferenceId', ''))),
       origin_action = coalesce(
           event.origin_action,
           nullif(event.event_payload_jsonb ->> 'originAction', ''))
 WHERE event.event_type = 'SAFETY_OUTCOME';

DO $legacy_outcome_reconcile$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM carebridge_migration_bridge.lifecycle_safety_outcome_bridge
    ) THEN
        IF to_regclass('public.lifecycle_safety_outcomes') IS NOT NULL THEN
            EXECUTE 'DROP TABLE public.lifecycle_safety_outcomes';
        END IF;
        RETURN;
    END IF;

    INSERT INTO public.mother_journey_events (
        event_id, mother_journey_id, owner_user_id, event_type,
        event_payload_jsonb, schema_version, actor_user_id,
        effective_at, recorded_at, legacy_source, legacy_id,
        triage_session_id, emergency_session_id, risk_level, stage,
        origin_dashboard, origin_reference_id, origin_action)
    SELECT CASE
               WHEN EXISTS (
                   SELECT 1 FROM public.mother_journey_events collision
                   WHERE collision.event_id = legacy.outcome_id)
                   THEN gen_random_uuid()
               ELSE legacy.outcome_id
           END,
           legacy.journey_id, legacy.owner_user_id, 'SAFETY_OUTCOME',
           jsonb_build_object(
               'intakeSessionId', legacy.triage_session_id::text,
               'emergencySessionId', legacy.emergency_session_id::text,
               'riskLevel', legacy.risk_level,
               'stage', legacy.stage,
               'originDashboard', legacy.origin_dashboard,
               'originReferenceId', legacy.origin_reference_id::text,
               'originAction', legacy.origin_action),
           '1', legacy.owner_user_id, legacy.occurred_at, legacy.recorded_at,
           'SAFETY_OUTCOME', legacy.triage_session_id::text,
           legacy.triage_session_id, legacy.emergency_session_id,
           legacy.risk_level, legacy.stage, legacy.origin_dashboard,
           legacy.origin_reference_id, legacy.origin_action
      FROM carebridge_migration_bridge.lifecycle_safety_outcome_bridge legacy
    ON CONFLICT (legacy_source, legacy_id) DO NOTHING;

    UPDATE public.mother_journey_events event
       SET mother_journey_id = legacy.journey_id,
           owner_user_id = legacy.owner_user_id,
           triage_session_id = legacy.triage_session_id,
           emergency_session_id = legacy.emergency_session_id,
           risk_level = legacy.risk_level,
           stage = legacy.stage,
           origin_dashboard = legacy.origin_dashboard,
           origin_reference_id = legacy.origin_reference_id,
           origin_action = legacy.origin_action
      FROM carebridge_migration_bridge.lifecycle_safety_outcome_bridge legacy
     WHERE event.legacy_source = 'SAFETY_OUTCOME'
       AND event.legacy_id = legacy.triage_session_id::text;

    IF EXISTS (
        SELECT 1
          FROM carebridge_migration_bridge.lifecycle_safety_outcome_bridge legacy
          LEFT JOIN public.mother_journey_events event
            ON event.legacy_source = 'SAFETY_OUTCOME'
           AND event.legacy_id = legacy.triage_session_id::text
           AND event.event_type = 'SAFETY_OUTCOME'
           AND event.owner_user_id = legacy.owner_user_id
           AND event.mother_journey_id = legacy.journey_id
           AND event.triage_session_id = legacy.triage_session_id
           AND event.emergency_session_id IS NOT DISTINCT FROM
               legacy.emergency_session_id
           AND event.risk_level = legacy.risk_level
           AND event.stage = legacy.stage
           AND event.origin_dashboard = legacy.origin_dashboard
           AND event.origin_reference_id = legacy.origin_reference_id
           AND event.origin_action = legacy.origin_action
         WHERE event.event_id IS NULL
    ) THEN
        RAISE EXCEPTION 'LIFECYCLE_SAFETY_OUTCOME_RECONCILIATION_FAILED';
    END IF;

    IF to_regclass('public.lifecycle_safety_outcomes') IS NOT NULL THEN
        EXECUTE 'DROP TABLE public.lifecycle_safety_outcomes';
    END IF;
END
$legacy_outcome_reconcile$;

DROP FUNCTION IF EXISTS public.reject_lifecycle_safety_outcome_mutation();

CREATE UNIQUE INDEX IF NOT EXISTS mother_journey_events_safety_triage_uk
    ON public.mother_journey_events(triage_session_id)
    WHERE event_type = 'SAFETY_OUTCOME';
CREATE INDEX IF NOT EXISTS mother_journey_events_safety_timeline_ix
    ON public.mother_journey_events(
        mother_journey_id, effective_at DESC, recorded_at DESC, event_id DESC)
    WHERE event_type = 'SAFETY_OUTCOME';
CREATE INDEX IF NOT EXISTS mother_journey_events_safety_emergency_ix
    ON public.mother_journey_events(emergency_session_id)
    WHERE event_type = 'SAFETY_OUTCOME'
      AND emergency_session_id IS NOT NULL;

DO $safety_outcome_constraints$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.mother_journey_events'::regclass
          AND conname = 'mother_journey_events_safety_shape_ck'
    ) THEN
        ALTER TABLE public.mother_journey_events
            ADD CONSTRAINT mother_journey_events_safety_shape_ck CHECK (
                (event_type = 'SAFETY_OUTCOME'
                    AND mother_journey_id IS NOT NULL
                    AND triage_session_id IS NOT NULL
                    AND risk_level IS NOT NULL
                    AND stage IS NOT NULL
                    AND origin_dashboard IS NOT NULL
                    AND origin_reference_id IS NOT NULL
                    AND origin_action IS NOT NULL)
                OR
                (event_type <> 'SAFETY_OUTCOME'
                    AND triage_session_id IS NULL
                    AND emergency_session_id IS NULL
                    AND risk_level IS NULL
                    AND stage IS NULL
                    AND origin_dashboard IS NULL
                    AND origin_reference_id IS NULL
                    AND origin_action IS NULL));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.mother_journey_events'::regclass
          AND conname = 'mother_journey_events_safety_risk_ck'
    ) THEN
        ALTER TABLE public.mother_journey_events
            ADD CONSTRAINT mother_journey_events_safety_risk_ck CHECK (
                risk_level IS NULL OR risk_level IN ('GREEN','YELLOW','RED'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.mother_journey_events'::regclass
          AND conname = 'mother_journey_events_safety_stage_ck'
    ) THEN
        ALTER TABLE public.mother_journey_events
            ADD CONSTRAINT mother_journey_events_safety_stage_ck CHECK (
                stage IS NULL OR stage IN (
                    'PRECONCEPTION','PREGNANCY','POSTPARTUM','INFANT','TODDLER'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.mother_journey_events'::regclass
          AND conname = 'mother_journey_events_safety_origin_ck'
    ) THEN
        ALTER TABLE public.mother_journey_events
            ADD CONSTRAINT mother_journey_events_safety_origin_ck CHECK (
                origin_dashboard IS NULL
                OR origin_dashboard IN ('MOTHER_JOURNEY','BABY_PROFILE'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.mother_journey_events'::regclass
          AND conname = 'mother_journey_events_safety_action_ck'
    ) THEN
        ALTER TABLE public.mother_journey_events
            ADD CONSTRAINT mother_journey_events_safety_action_ck CHECK (
                origin_action IS NULL
                OR origin_action IN (
                    'RETURN_TO_MOTHER_JOURNEY','RETURN_TO_BABY_PROFILE'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.mother_journey_events'::regclass
          AND conname = 'mother_journey_events_safety_origin_action_ck'
    ) THEN
        ALTER TABLE public.mother_journey_events
            ADD CONSTRAINT mother_journey_events_safety_origin_action_ck CHECK (
                event_type <> 'SAFETY_OUTCOME'
                OR (origin_dashboard = 'MOTHER_JOURNEY'
                    AND origin_action = 'RETURN_TO_MOTHER_JOURNEY'
                    AND origin_reference_id = mother_journey_id
                    AND stage IN ('PRECONCEPTION','PREGNANCY','POSTPARTUM'))
                OR (origin_dashboard = 'BABY_PROFILE'
                    AND origin_action = 'RETURN_TO_BABY_PROFILE'
                    AND stage IN ('INFANT','TODDLER')));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.mother_journey_events'::regclass
          AND conname = 'mother_journey_events_safety_emergency_risk_ck'
    ) THEN
        ALTER TABLE public.mother_journey_events
            ADD CONSTRAINT mother_journey_events_safety_emergency_risk_ck CHECK (
                emergency_session_id IS NULL OR risk_level = 'RED');
    END IF;
END
$safety_outcome_constraints$;

DO $safety_outcome_foreign_keys$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.mother_journey_events'::regclass
          AND conname = 'mother_journey_events_safety_journey_owner_fk'
    ) THEN
        ALTER TABLE public.mother_journey_events
            ADD CONSTRAINT mother_journey_events_safety_journey_owner_fk
            FOREIGN KEY (mother_journey_id, owner_user_id)
            REFERENCES public.mother_journeys(journey_id, owner_user_id)
            ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.mother_journey_events'::regclass
          AND conname = 'mother_journey_events_safety_triage_owner_fk'
    ) THEN
        ALTER TABLE public.mother_journey_events
            ADD CONSTRAINT mother_journey_events_safety_triage_owner_fk
            FOREIGN KEY (triage_session_id, owner_user_id)
            REFERENCES public.triage_sessions(triage_session_id, user_id)
            ON DELETE RESTRICT;
    END IF;

    IF to_regclass('public.safety_events') IS NULL THEN
        RAISE EXCEPTION
            'CANONICAL_SAFETY_MIGRATION_REQUIRED: safety_events is absent';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.mother_journey_events'::regclass
          AND conname = 'mother_journey_events_safety_emergency_owner_fk'
    ) THEN
        BEGIN
            ALTER TABLE public.mother_journey_events
                ADD CONSTRAINT mother_journey_events_safety_emergency_owner_fk
                FOREIGN KEY (emergency_session_id, owner_user_id)
                REFERENCES public.safety_events(safety_event_id, user_id)
                ON DELETE RESTRICT;
        EXCEPTION WHEN invalid_foreign_key THEN
            RAISE EXCEPTION
                'CANONICAL_SAFETY_MIGRATION_REQUIRED: '
                'V20260724210000 must create safety_events(safety_event_id,user_id) uniqueness';
        END;
    END IF;
END
$safety_outcome_foreign_keys$;

-- Historical RED outcomes may predate the canonical immutable action journal.
-- Recreate only an absent association; an existing but conflicting escalation
-- is left untouched and rejected by the reconciliation gate below.
INSERT INTO public.safety_event_actions (
    safety_event_action_id, safety_event_id, action_type, owner_user_id,
    triage_handoff_id, attempt_number, idempotency_key, action_phase,
    alert_generation, created_at)
SELECT gen_random_uuid(), outcome.emergency_session_id, 'TRIAGE_ESCALATION',
       outcome.owner_user_id, outcome.triage_session_id, 1,
       'triage-escalation:' || outcome.triage_session_id::text,
       'LINKED', 0, coalesce(outcome.recorded_at, now())
  FROM public.mother_journey_events outcome
  JOIN public.safety_events emergency
    ON emergency.safety_event_id = outcome.emergency_session_id
   AND emergency.user_id = outcome.owner_user_id
   AND emergency.record_type = 'EMERGENCY_SESSION'
  JOIN public.triage_sessions triage
    ON triage.triage_session_id = outcome.triage_session_id
   AND triage.user_id = outcome.owner_user_id
 WHERE outcome.event_type = 'SAFETY_OUTCOME'
   AND outcome.emergency_session_id IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
         FROM public.safety_event_actions existing
        WHERE existing.action_type = 'TRIAGE_ESCALATION'
          AND existing.triage_handoff_id = outcome.triage_session_id)
ON CONFLICT (idempotency_key) DO NOTHING;

DO $safety_outcome_source_reconciliation$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM public.mother_journey_events outcome
          LEFT JOIN public.triage_sessions triage
            ON triage.triage_session_id = outcome.triage_session_id
           AND triage.user_id = outcome.owner_user_id
         WHERE outcome.event_type = 'SAFETY_OUTCOME'
           AND (triage.triage_session_id IS NULL
                OR triage.status <> 'COMPLETED'
                OR outcome.mother_journey_id IS DISTINCT FROM triage.journey_id
                OR outcome.risk_level IS DISTINCT FROM triage.risk_level
                OR outcome.stage IS DISTINCT FROM triage.stage
                OR outcome.origin_dashboard IS DISTINCT FROM
                    triage.origin_dashboard
                OR outcome.origin_reference_id IS DISTINCT FROM
                    triage.origin_reference_id
                OR outcome.origin_action IS DISTINCT FROM CASE
                    WHEN triage.origin_dashboard = 'MOTHER_JOURNEY'
                        THEN 'RETURN_TO_MOTHER_JOURNEY'
                    WHEN triage.origin_dashboard = 'BABY_PROFILE'
                        THEN 'RETURN_TO_BABY_PROFILE'
                    ELSE NULL
                END)
    ) THEN
        RAISE EXCEPTION 'SAFETY_OUTCOME_TRIAGE_SOURCE_MISMATCH';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.mother_journey_events outcome
         WHERE outcome.event_type = 'SAFETY_OUTCOME'
           AND outcome.emergency_session_id IS NOT NULL
           AND (
               NOT EXISTS (
                   SELECT 1
                     FROM public.safety_events emergency
                    WHERE emergency.safety_event_id = outcome.emergency_session_id
                      AND emergency.user_id = outcome.owner_user_id
                      AND emergency.record_type = 'EMERGENCY_SESSION')
               OR NOT EXISTS (
                   SELECT 1
                     FROM public.safety_event_actions action
                    WHERE action.action_type = 'TRIAGE_ESCALATION'
                      AND action.action_phase = 'LINKED'
                      AND action.safety_event_id = outcome.emergency_session_id
                      AND action.triage_handoff_id = outcome.triage_session_id
                      AND action.owner_user_id = outcome.owner_user_id))
    ) THEN
        RAISE EXCEPTION 'SAFETY_OUTCOME_EMERGENCY_SOURCE_MISMATCH';
    END IF;
END
$safety_outcome_source_reconciliation$;

CREATE OR REPLACE FUNCTION public.carebridge_validate_safety_outcome_source()
RETURNS trigger
LANGUAGE plpgsql
AS $validate_safety_outcome_source$
BEGIN
    IF NEW.event_type <> 'SAFETY_OUTCOME' THEN
        RETURN NEW;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM public.triage_sessions triage
         WHERE triage.triage_session_id = NEW.triage_session_id
           AND triage.user_id = NEW.owner_user_id
           AND triage.status = 'COMPLETED'
           AND triage.journey_id IS NOT DISTINCT FROM NEW.mother_journey_id
           AND triage.risk_level IS NOT DISTINCT FROM NEW.risk_level
           AND triage.stage IS NOT DISTINCT FROM NEW.stage
           AND triage.origin_dashboard IS NOT DISTINCT FROM NEW.origin_dashboard
           AND triage.origin_reference_id IS NOT DISTINCT FROM
               NEW.origin_reference_id
           AND NEW.origin_action IS NOT DISTINCT FROM CASE
               WHEN triage.origin_dashboard = 'MOTHER_JOURNEY'
                   THEN 'RETURN_TO_MOTHER_JOURNEY'
               WHEN triage.origin_dashboard = 'BABY_PROFILE'
                   THEN 'RETURN_TO_BABY_PROFILE'
               ELSE NULL
           END
    ) THEN
        RAISE EXCEPTION 'SAFETY_OUTCOME_TRIAGE_SOURCE_MISMATCH';
    END IF;

    IF NEW.emergency_session_id IS NOT NULL
       AND (
           NOT EXISTS (
               SELECT 1
                 FROM public.safety_events emergency
                WHERE emergency.safety_event_id = NEW.emergency_session_id
                  AND emergency.user_id = NEW.owner_user_id
                  AND emergency.record_type = 'EMERGENCY_SESSION')
           OR NOT EXISTS (
               SELECT 1
                 FROM public.safety_event_actions action
                WHERE action.action_type = 'TRIAGE_ESCALATION'
                  AND action.action_phase = 'LINKED'
                  AND action.safety_event_id = NEW.emergency_session_id
                  AND action.triage_handoff_id = NEW.triage_session_id
                  AND action.owner_user_id = NEW.owner_user_id)) THEN
        RAISE EXCEPTION 'SAFETY_OUTCOME_EMERGENCY_SOURCE_MISMATCH';
    END IF;

    RETURN NEW;
END
$validate_safety_outcome_source$;

DROP TRIGGER IF EXISTS mother_journey_events_safety_source_trg
    ON public.mother_journey_events;
DROP TRIGGER IF EXISTS mother_journey_events_00_safety_source_trg
    ON public.mother_journey_events;
-- PostgreSQL orders same-kind triggers by name. The 00 prefix ensures source
-- provenance is checked before the generic journey-owner trigger.
CREATE TRIGGER mother_journey_events_00_safety_source_trg
BEFORE INSERT ON public.mother_journey_events
FOR EACH ROW EXECUTE FUNCTION public.carebridge_validate_safety_outcome_source();

DROP TRIGGER IF EXISTS mother_journey_events_immutable_trg
    ON public.mother_journey_events;
CREATE TRIGGER mother_journey_events_immutable_trg
BEFORE UPDATE OR DELETE ON public.mother_journey_events
FOR EACH ROW EXECUTE FUNCTION public.carebridge_reject_mutation();

-- Prove every durable bridge value reached its canonical owner-bound target
-- before retiring any migration evidence. A NULL legacy value is not evidence
-- that the canonical value must remain NULL: completed historical bindings are
-- deliberately enriched above with a retired continuation when one was absent.
DO $triage_lifecycle_bridge_consumption_gate$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM carebridge_migration_bridge.triage_lifecycle_bridge bridge
          LEFT JOIN public.triage_sessions target
            ON target.triage_session_id = bridge.triage_session_id
           AND target.user_id = bridge.owner_user_id
           AND (bridge.journey_id IS NULL
                OR target.journey_id = bridge.journey_id)
           AND (bridge.origin_dashboard IS NULL
                OR target.origin_dashboard = bridge.origin_dashboard)
           AND (bridge.origin_reference_id IS NULL
                OR target.origin_reference_id = bridge.origin_reference_id)
           AND (bridge.continuation_token IS NULL
                OR target.continuation_token = bridge.continuation_token)
           AND (bridge.continuation_expires_at IS NULL
                OR target.continuation_expires_at = bridge.continuation_expires_at)
           AND (bridge.continuation_acknowledged_at IS NULL
                OR target.continuation_acknowledged_at =
                   bridge.continuation_acknowledged_at)
         WHERE target.triage_session_id IS NULL
    ) THEN
        RAISE EXCEPTION 'TRIAGE_LIFECYCLE_BRIDGE_CONSUMPTION_FAILED';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM carebridge_migration_bridge.lifecycle_safety_outcome_bridge bridge
          LEFT JOIN public.mother_journey_events target
            ON target.event_type = 'SAFETY_OUTCOME'
           AND target.legacy_source = 'SAFETY_OUTCOME'
           AND target.legacy_id = bridge.triage_session_id::text
           AND target.owner_user_id = bridge.owner_user_id
           AND target.mother_journey_id = bridge.journey_id
           AND target.triage_session_id = bridge.triage_session_id
           AND target.emergency_session_id IS NOT DISTINCT FROM
               bridge.emergency_session_id
           AND target.risk_level = bridge.risk_level
           AND target.stage = bridge.stage
           AND target.origin_dashboard = bridge.origin_dashboard
           AND target.origin_reference_id = bridge.origin_reference_id
           AND target.origin_action = bridge.origin_action
           AND target.effective_at = bridge.occurred_at
           AND target.recorded_at = bridge.recorded_at
         WHERE target.event_id IS NULL
    ) THEN
        RAISE EXCEPTION 'LIFECYCLE_SAFETY_OUTCOME_BRIDGE_CONSUMPTION_FAILED';
    END IF;
END
$triage_lifecycle_bridge_consumption_gate$;

DROP TABLE IF EXISTS public.triage_lifecycle_bridge;
DROP TABLE IF EXISTS carebridge_migration_bridge.lifecycle_safety_outcome_bridge;
DROP TABLE IF EXISTS carebridge_migration_bridge.triage_lifecycle_bridge;
DROP FUNCTION IF EXISTS carebridge_migration_bridge.sync_triage_lifecycle_bridge();

-- Other historical bridges intentionally survive until their own later
-- consumers. Remove the shared schema only when it is genuinely empty.
DO $drop_empty_migration_bridge_schema$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_namespace
         WHERE nspname = 'carebridge_migration_bridge'
    )
       AND NOT EXISTS (
           SELECT 1
             FROM pg_class relation
             JOIN pg_namespace namespace
               ON namespace.oid = relation.relnamespace
            WHERE namespace.nspname = 'carebridge_migration_bridge')
       AND NOT EXISTS (
           SELECT 1
             FROM pg_proc routine
             JOIN pg_namespace namespace
               ON namespace.oid = routine.pronamespace
            WHERE namespace.nspname = 'carebridge_migration_bridge')
       AND NOT EXISTS (
           SELECT 1
             FROM pg_type type
             JOIN pg_namespace namespace
               ON namespace.oid = type.typnamespace
            WHERE namespace.nspname = 'carebridge_migration_bridge') THEN
        DROP SCHEMA carebridge_migration_bridge;
    END IF;
END
$drop_empty_migration_bridge_schema$;

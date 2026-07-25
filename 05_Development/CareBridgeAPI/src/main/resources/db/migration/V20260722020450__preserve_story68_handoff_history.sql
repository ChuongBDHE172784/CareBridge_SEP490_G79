-- Preserve an already-applied Story 6.8 handoff graph before the earliest legacy
-- consultation cleanup and Phase 2 parent-table retirements.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

CREATE SCHEMA IF NOT EXISTS carebridge_migration_bridge;

CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story68_history_state (
    history_key text PRIMARY KEY,
    source_graph_present boolean NOT NULL,
    recorded_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story68_request_bridge (
    id uuid PRIMARY KEY,
    requester_user_id uuid NOT NULL,
    expert_profile_id uuid NOT NULL,
    client_request_id uuid NOT NULL,
    topic varchar(200) NOT NULL,
    description varchar(2000) NOT NULL,
    preferred_window_start timestamptz,
    preferred_window_end timestamptz,
    status varchar(20) NOT NULL,
    reject_reason varchar(500),
    direct_conversation_id uuid,
    responded_at timestamptz,
    responded_by uuid,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    captured_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story68_context_share_bridge (
    context_share_id uuid PRIMARY KEY,
    consultation_request_id uuid NOT NULL UNIQUE,
    owner_user_id uuid NOT NULL,
    intake_session_id uuid NOT NULL,
    expert_profile_id uuid NOT NULL,
    consent_grant_id bigint NOT NULL UNIQUE,
    idempotency_key uuid NOT NULL,
    journey_id uuid NOT NULL,
    origin_dashboard varchar(30) NOT NULL,
    origin_reference_id uuid NOT NULL,
    triage_stage varchar(20) NOT NULL,
    risk_level varchar(10) NOT NULL,
    intake_status varchar(20) NOT NULL,
    risk_summary varchar(500) NOT NULL,
    share_policy_version varchar(60) NOT NULL,
    created_at timestamptz NOT NULL,
    captured_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story68_context_citation_bridge (
    citation_snapshot_id uuid PRIMARY KEY,
    context_share_id uuid NOT NULL,
    evidence_source_id uuid NOT NULL,
    organization varchar(255) NOT NULL,
    source_url varchar(1000) NOT NULL,
    source_status_at_share varchar(30) NOT NULL,
    reviewed_at timestamptz NOT NULL,
    ordinal smallint NOT NULL,
    created_at timestamptz NOT NULL,
    captured_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story68_notification_reference_bridge (
    notification_id uuid PRIMARY KEY,
    snapshot_jsonb jsonb NOT NULL,
    captured_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story68_audit_reference_bridge (
    audit_log_id uuid PRIMARY KEY,
    snapshot_jsonb jsonb NOT NULL,
    captured_at timestamptz NOT NULL DEFAULT now()
);

-- V22020800 intentionally refuses consultation-request notification/audit
-- history. Retag only the discriminator while preserving an exact snapshot;
-- V22020850 restores it immediately after the legacy parent cleanup.
DO $detach_story68_references$
BEGIN
    IF to_regclass('public.notification_records') IS NULL THEN
        RAISE EXCEPTION 'STORY68_NOTIFICATION_REFERENCE_SOURCE_MISSING';
    END IF;

    LOCK TABLE public.notification_records IN SHARE ROW EXCLUSIVE MODE;

    IF EXISTS (
        SELECT 1 FROM public.notification_records
         WHERE reference_type = 'STORY68_CONSULTATION_REQUEST'
    ) THEN
        RAISE EXCEPTION 'STORY68_TEMPORARY_NOTIFICATION_TAG_COLLISION';
    END IF;

    INSERT INTO carebridge_migration_bridge.story68_notification_reference_bridge (
        notification_id, snapshot_jsonb, captured_at)
    SELECT notification.id, to_jsonb(notification), now()
      FROM public.notification_records notification
     WHERE notification.reference_type = 'CONSULTATION_REQUEST'
    ON CONFLICT (notification_id) DO UPDATE SET
        snapshot_jsonb = excluded.snapshot_jsonb,
        captured_at = excluded.captured_at;

    UPDATE public.notification_records
       SET reference_type = 'STORY68_CONSULTATION_REQUEST'
     WHERE reference_type = 'CONSULTATION_REQUEST';

    IF (SELECT count(*)
          FROM carebridge_migration_bridge.story68_notification_reference_bridge)
           <> (SELECT count(*) FROM public.notification_records
                WHERE reference_type = 'STORY68_CONSULTATION_REQUEST') THEN
        RAISE EXCEPTION 'STORY68_NOTIFICATION_REFERENCE_DETACH_RECONCILIATION_FAILED';
    END IF;

    IF to_regclass('public.audit_logs') IS NOT NULL THEN
        LOCK TABLE public.audit_logs IN SHARE ROW EXCLUSIVE MODE;

        IF EXISTS (
            SELECT 1 FROM public.audit_logs
             WHERE entity_type = 'STORY68_CONSULTATION_REQUEST'
        ) THEN
            RAISE EXCEPTION 'STORY68_TEMPORARY_AUDIT_TAG_COLLISION';
        END IF;

        INSERT INTO carebridge_migration_bridge.story68_audit_reference_bridge (
            audit_log_id, snapshot_jsonb, captured_at)
        SELECT audit.audit_log_id, to_jsonb(audit), now()
          FROM public.audit_logs audit
         WHERE upper(coalesce(audit.entity_type, '')) = 'CONSULTATION_REQUEST'
        ON CONFLICT (audit_log_id) DO UPDATE SET
            snapshot_jsonb = excluded.snapshot_jsonb,
            captured_at = excluded.captured_at;

        UPDATE public.audit_logs
           SET entity_type = 'STORY68_CONSULTATION_REQUEST'
         WHERE upper(coalesce(entity_type, '')) = 'CONSULTATION_REQUEST';

        IF (SELECT count(*)
              FROM carebridge_migration_bridge.story68_audit_reference_bridge)
               <> (SELECT count(*) FROM public.audit_logs
                    WHERE entity_type = 'STORY68_CONSULTATION_REQUEST') THEN
            RAISE EXCEPTION 'STORY68_AUDIT_REFERENCE_DETACH_RECONCILIATION_FAILED';
        END IF;
    ELSIF EXISTS (
        SELECT 1 FROM carebridge_migration_bridge.story68_audit_reference_bridge
    ) OR NOT EXISTS (
        SELECT 1
          FROM public.flyway_schema_history
         WHERE version = '20260722231900'
           AND success
    ) THEN
        RAISE EXCEPTION 'STORY68_AUDIT_REFERENCE_SOURCE_MISSING';
    END IF;
END
$detach_story68_references$;

DO $preserve_story68_history$
BEGIN
    IF to_regclass('public.consultation_requests') IS NULL THEN
        IF EXISTS (
            SELECT 1 FROM public.flyway_schema_history
             WHERE version = '20260722020800' AND success
        ) THEN
            INSERT INTO carebridge_migration_bridge.story68_history_state (
                history_key, source_graph_present, recorded_at)
            VALUES ('consultation_requests', false, now())
            ON CONFLICT (history_key) DO UPDATE SET
                source_graph_present = excluded.source_graph_present,
                recorded_at = excluded.recorded_at;
            RETURN;
        END IF;
        RAISE EXCEPTION 'STORY68_HANDOFF_SOURCE_GRAPH_INCOMPLETE';
    END IF;

    INSERT INTO carebridge_migration_bridge.story68_history_state (
        history_key, source_graph_present, recorded_at)
    VALUES ('consultation_requests', true, now())
    ON CONFLICT (history_key) DO UPDATE SET
        source_graph_present = excluded.source_graph_present,
        recorded_at = excluded.recorded_at;

    LOCK TABLE public.consultation_requests IN SHARE ROW EXCLUSIVE MODE;
    IF to_regclass('public.consultation_context_shares') IS NOT NULL THEN
        IF to_regclass('public.consultation_context_citations') IS NULL THEN
            RAISE EXCEPTION 'STORY68_HANDOFF_SOURCE_GRAPH_INCOMPLETE';
        END IF;
        LOCK TABLE public.consultation_context_shares,
                   public.consultation_context_citations
            IN SHARE ROW EXCLUSIVE MODE;
    END IF;

    INSERT INTO carebridge_migration_bridge.story68_request_bridge (
        id, requester_user_id, expert_profile_id, client_request_id, topic,
        description, preferred_window_start, preferred_window_end, status,
        reject_reason, direct_conversation_id, responded_at, responded_by,
        expires_at, created_at, updated_at, captured_at)
    SELECT request.id, request.requester_user_id, request.expert_profile_id,
           request.client_request_id, request.topic, request.description,
           request.preferred_window_start, request.preferred_window_end,
           request.status, request.reject_reason, request.direct_conversation_id,
           request.responded_at, request.responded_by, request.expires_at,
           request.created_at, request.updated_at, now()
      FROM public.consultation_requests request
    ON CONFLICT (id) DO UPDATE SET
        requester_user_id = excluded.requester_user_id,
        expert_profile_id = excluded.expert_profile_id,
        client_request_id = excluded.client_request_id,
        topic = excluded.topic,
        description = excluded.description,
        preferred_window_start = excluded.preferred_window_start,
        preferred_window_end = excluded.preferred_window_end,
        status = excluded.status,
        reject_reason = excluded.reject_reason,
        direct_conversation_id = excluded.direct_conversation_id,
        responded_at = excluded.responded_at,
        responded_by = excluded.responded_by,
        expires_at = excluded.expires_at,
        created_at = excluded.created_at,
        updated_at = excluded.updated_at,
        captured_at = excluded.captured_at;

    IF (SELECT count(*)
          FROM carebridge_migration_bridge.story68_request_bridge)
           <> (SELECT count(*) FROM public.consultation_requests)
       OR EXISTS (
        SELECT 1
          FROM public.consultation_requests source
          LEFT JOIN carebridge_migration_bridge.story68_request_bridge bridge
            ON bridge.id = source.id
           AND (to_jsonb(bridge) - 'captured_at') = to_jsonb(source)
         WHERE bridge.id IS NULL
    ) THEN
        RAISE EXCEPTION 'STORY68_REQUEST_BRIDGE_RECONCILIATION_FAILED';
    END IF;

    IF to_regclass('public.consultation_context_shares') IS NOT NULL THEN
        IF to_regclass('public.consultation_context_citations') IS NULL THEN
            RAISE EXCEPTION 'STORY68_HANDOFF_SOURCE_GRAPH_INCOMPLETE';
        END IF;

        INSERT INTO carebridge_migration_bridge.story68_context_share_bridge (
            context_share_id, consultation_request_id, owner_user_id,
            intake_session_id, expert_profile_id, consent_grant_id,
            idempotency_key, journey_id, origin_dashboard, origin_reference_id,
            triage_stage, risk_level, intake_status, risk_summary,
            share_policy_version, created_at, captured_at)
        SELECT context_share_id, consultation_request_id, owner_user_id,
               intake_session_id, expert_profile_id, consent_grant_id,
               idempotency_key, journey_id, origin_dashboard, origin_reference_id,
               triage_stage, risk_level, intake_status, risk_summary,
               share_policy_version, created_at, now()
          FROM public.consultation_context_shares
        ON CONFLICT (context_share_id) DO UPDATE SET
            consultation_request_id = excluded.consultation_request_id,
            owner_user_id = excluded.owner_user_id,
            intake_session_id = excluded.intake_session_id,
            expert_profile_id = excluded.expert_profile_id,
            consent_grant_id = excluded.consent_grant_id,
            idempotency_key = excluded.idempotency_key,
            journey_id = excluded.journey_id,
            origin_dashboard = excluded.origin_dashboard,
            origin_reference_id = excluded.origin_reference_id,
            triage_stage = excluded.triage_stage,
            risk_level = excluded.risk_level,
            intake_status = excluded.intake_status,
            risk_summary = excluded.risk_summary,
            share_policy_version = excluded.share_policy_version,
            created_at = excluded.created_at,
            captured_at = excluded.captured_at;

        INSERT INTO carebridge_migration_bridge.story68_context_citation_bridge (
            citation_snapshot_id, context_share_id, evidence_source_id,
            organization, source_url, source_status_at_share, reviewed_at,
            ordinal, created_at, captured_at)
        SELECT citation_snapshot_id, context_share_id, evidence_source_id,
               organization, source_url, source_status_at_share, reviewed_at,
               ordinal, created_at, now()
          FROM public.consultation_context_citations
        ON CONFLICT (citation_snapshot_id) DO UPDATE SET
            context_share_id = excluded.context_share_id,
            evidence_source_id = excluded.evidence_source_id,
            organization = excluded.organization,
            source_url = excluded.source_url,
            source_status_at_share = excluded.source_status_at_share,
            reviewed_at = excluded.reviewed_at,
            ordinal = excluded.ordinal,
            created_at = excluded.created_at,
            captured_at = excluded.captured_at;

        IF (SELECT count(*)
              FROM carebridge_migration_bridge.story68_context_share_bridge)
               <> (SELECT count(*) FROM public.consultation_context_shares)
           OR (SELECT count(*)
                 FROM carebridge_migration_bridge.story68_context_citation_bridge)
               <> (SELECT count(*) FROM public.consultation_context_citations)
           OR EXISTS (
                SELECT 1
                  FROM public.consultation_context_shares source
                  LEFT JOIN carebridge_migration_bridge.story68_context_share_bridge bridge
                    ON bridge.context_share_id = source.context_share_id
                   AND (to_jsonb(bridge) - 'captured_at') = to_jsonb(source)
                 WHERE bridge.context_share_id IS NULL)
           OR EXISTS (
                SELECT 1
                  FROM public.consultation_context_citations source
                  LEFT JOIN carebridge_migration_bridge.story68_context_citation_bridge bridge
                    ON bridge.citation_snapshot_id = source.citation_snapshot_id
                   AND (to_jsonb(bridge) - 'captured_at') = to_jsonb(source)
                 WHERE bridge.citation_snapshot_id IS NULL) THEN
            RAISE EXCEPTION 'STORY68_HANDOFF_BRIDGE_RECONCILIATION_FAILED';
        END IF;

        DROP TABLE public.consultation_context_citations;
        DROP TABLE public.consultation_context_shares;
    END IF;

    DROP INDEX IF EXISTS public.uq_consultation_requests_integrity;
    DROP INDEX IF EXISTS public.uq_consent_grants_integrity;
    DROP INDEX IF EXISTS public.uq_intake_handoff_integrity;

    DELETE FROM public.consultation_requests;
    IF EXISTS (SELECT 1 FROM public.consultation_requests) THEN
        RAISE EXCEPTION 'STORY68_REQUEST_EVACUATION_FAILED';
    END IF;
END
$preserve_story68_history$;

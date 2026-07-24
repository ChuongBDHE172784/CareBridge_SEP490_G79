-- Canonical Story 6.8 handoff graph after immutable-history compatibility.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

CREATE SCHEMA IF NOT EXISTS carebridge_migration_bridge;

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

-- Keep the handoff migration independently bootstrappable on a canonical
-- GitHub-cutover schema where the Story 6.7 lifecycle extension was absent.
ALTER TABLE public.triage_sessions
    ADD COLUMN IF NOT EXISTS journey_id uuid,
    ADD COLUMN IF NOT EXISTS origin_dashboard varchar(30),
    ADD COLUMN IF NOT EXISTS origin_reference_id uuid,
    ADD COLUMN IF NOT EXISTS continuation_token uuid,
    ADD COLUMN IF NOT EXISTS continuation_expires_at timestamptz,
    ADD COLUMN IF NOT EXISTS continuation_acknowledged_at timestamptz;

-- Capture any table left by the immutable migration on clean/GitHub histories.
DO $capture_current_story68_tables$
BEGIN
    IF to_regclass('public.consultation_context_shares') IS NULL THEN
        RETURN;
    END IF;
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
END
$capture_current_story68_tables$;

CREATE TABLE IF NOT EXISTS public.expert_consultation_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_user_id uuid NOT NULL REFERENCES public.users(user_id),
    expert_profile_id uuid NOT NULL
        REFERENCES public.professional_profiles(professional_profile_id),
    client_request_id uuid NOT NULL,
    topic varchar(200) NOT NULL,
    description varchar(2000) NOT NULL,
    preferred_window_start timestamptz,
    preferred_window_end timestamptz,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    reject_reason varchar(500),
    direct_conversation_id uuid
        CONSTRAINT expert_consultation_requests_direct_conversation_archive_fk
        REFERENCES public.archived_realtime_records(archive_id),
    responded_at timestamptz,
    responded_by uuid REFERENCES public.users(user_id),
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT expert_consultation_requests_owner_client_uk
        UNIQUE (requester_user_id, client_request_id),
    CONSTRAINT expert_consultation_requests_status_ck CHECK (
        status IN ('PENDING','ACCEPTED','REJECTED','CANCELLED','EXPIRED')),
    CONSTRAINT expert_consultation_requests_window_ck CHECK (
        (preferred_window_start IS NULL AND preferred_window_end IS NULL)
        OR (preferred_window_start IS NOT NULL
            AND preferred_window_end IS NOT NULL
            AND preferred_window_end > preferred_window_start)),
    CONSTRAINT expert_consultation_requests_responded_ck CHECK (
        status = 'PENDING' OR responded_at IS NOT NULL),
    CONSTRAINT expert_consultation_requests_expiry_ck CHECK (
        expires_at > created_at)
);

DO $expert_consultation_request_foreign_keys$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conrelid = 'public.expert_consultation_requests'::regclass
           AND conname = 'expert_consultation_requests_direct_conversation_archive_fk'
    ) THEN
        ALTER TABLE public.expert_consultation_requests
            ADD CONSTRAINT expert_consultation_requests_direct_conversation_archive_fk
            FOREIGN KEY (direct_conversation_id)
            REFERENCES public.archived_realtime_records(archive_id);
    END IF;
END
$expert_consultation_request_foreign_keys$;

DO $expert_request_conversation_reconciliation$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM public.expert_consultation_requests request
          LEFT JOIN public.archived_realtime_records conversation
            ON conversation.archive_id = request.direct_conversation_id
           AND conversation.legacy_table = 'direct_conversations'
         WHERE request.direct_conversation_id IS NOT NULL
           AND conversation.archive_id IS NULL
    ) THEN
        RAISE EXCEPTION 'STORY68_DIRECT_CONVERSATION_SOURCE_MISMATCH';
    END IF;
END
$expert_request_conversation_reconciliation$;

CREATE OR REPLACE FUNCTION public.carebridge_validate_expert_request_conversation()
RETURNS trigger
LANGUAGE plpgsql
AS $validate_expert_request_conversation$
BEGIN
    IF NEW.direct_conversation_id IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
             FROM public.archived_realtime_records conversation
            WHERE conversation.archive_id = NEW.direct_conversation_id
              AND conversation.legacy_table = 'direct_conversations') THEN
        RAISE EXCEPTION 'STORY68_DIRECT_CONVERSATION_SOURCE_MISMATCH';
    END IF;
    RETURN NEW;
END
$validate_expert_request_conversation$;

DROP TRIGGER IF EXISTS expert_consultation_request_conversation_source_trg
    ON public.expert_consultation_requests;
CREATE TRIGGER expert_consultation_request_conversation_source_trg
BEFORE INSERT OR UPDATE OF direct_conversation_id
ON public.expert_consultation_requests
FOR EACH ROW EXECUTE FUNCTION
    public.carebridge_validate_expert_request_conversation();

CREATE INDEX IF NOT EXISTS expert_consultation_requests_expert_status_ix
    ON public.expert_consultation_requests(
        expert_profile_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS expert_consultation_requests_owner_status_ix
    ON public.expert_consultation_requests(
        requester_user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS expert_consultation_requests_expiry_ix
    ON public.expert_consultation_requests(expires_at)
    WHERE status = 'PENDING';
CREATE UNIQUE INDEX IF NOT EXISTS expert_consultation_requests_integrity_uk
    ON public.expert_consultation_requests(
        id, requester_user_id, expert_profile_id, client_request_id);

INSERT INTO public.expert_consultation_requests (
    id, requester_user_id, expert_profile_id, client_request_id, topic,
    description, preferred_window_start, preferred_window_end, status,
    reject_reason, direct_conversation_id, responded_at, responded_by,
    expires_at, created_at, updated_at)
SELECT id, requester_user_id, expert_profile_id, client_request_id, topic,
       description, preferred_window_start, preferred_window_end, status,
       reject_reason, direct_conversation_id, responded_at, responded_by,
       expires_at, created_at, updated_at
  FROM carebridge_migration_bridge.story68_request_bridge
ON CONFLICT (id) DO NOTHING;

CREATE UNIQUE INDEX IF NOT EXISTS data_permissions_handoff_integrity_uk
    ON public.data_permissions(legacy_consent_id, owner_user_id, evidence_key);
CREATE UNIQUE INDEX IF NOT EXISTS triage_sessions_handoff_integrity_uk
    ON public.triage_sessions(
        triage_session_id, user_id, journey_id, origin_dashboard,
        origin_reference_id, stage, risk_level, status);
CREATE UNIQUE INDEX IF NOT EXISTS mother_journeys_handoff_owner_uk
    ON public.mother_journeys(journey_id, owner_user_id);

CREATE TABLE IF NOT EXISTS public.consultation_context_shares (
    context_share_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
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
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT consultation_context_owner_key_uk
        UNIQUE (owner_user_id, idempotency_key),
    CONSTRAINT consultation_context_intake_expert_uk
        UNIQUE (owner_user_id, intake_session_id, expert_profile_id),
    CONSTRAINT consultation_context_yellow_ck CHECK (risk_level = 'YELLOW'),
    CONSTRAINT consultation_context_completed_ck CHECK (
        intake_status = 'COMPLETED'),
    CONSTRAINT consultation_context_origin_ck CHECK (
        origin_dashboard IN ('MOTHER_JOURNEY','BABY_PROFILE')),
    CONSTRAINT consultation_context_stage_ck CHECK (
        triage_stage IN (
            'PRECONCEPTION','PREGNANCY','POSTPARTUM','INFANT','TODDLER')),
    CONSTRAINT consultation_context_summary_ck CHECK (
        length(btrim(risk_summary)) BETWEEN 1 AND 500),
    CONSTRAINT consultation_context_policy_ck CHECK (
        share_policy_version = 'YELLOW_EXPERT_CONTEXT_V1')
);

ALTER TABLE public.consultation_context_shares
    DROP CONSTRAINT IF EXISTS fk_context_request_integrity,
    DROP CONSTRAINT IF EXISTS fk_context_intake_snapshot,
    DROP CONSTRAINT IF EXISTS fk_context_journey_owner,
    DROP CONSTRAINT IF EXISTS fk_context_expert,
    DROP CONSTRAINT IF EXISTS fk_context_consent_integrity;

ALTER TABLE public.consultation_context_shares
    ADD CONSTRAINT fk_context_request_integrity FOREIGN KEY (
        consultation_request_id, owner_user_id, expert_profile_id,
        idempotency_key)
        REFERENCES public.expert_consultation_requests (
            id, requester_user_id, expert_profile_id, client_request_id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_context_intake_snapshot FOREIGN KEY (
        intake_session_id, owner_user_id, journey_id, origin_dashboard,
        origin_reference_id, triage_stage, risk_level, intake_status)
        REFERENCES public.triage_sessions (
            triage_session_id, user_id, journey_id, origin_dashboard,
            origin_reference_id, stage, risk_level, status)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_context_journey_owner FOREIGN KEY (
        journey_id, owner_user_id)
        REFERENCES public.mother_journeys(journey_id, owner_user_id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_context_expert FOREIGN KEY (expert_profile_id)
        REFERENCES public.professional_profiles(professional_profile_id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_context_consent_integrity FOREIGN KEY (
        consent_grant_id, owner_user_id, idempotency_key)
        REFERENCES public.data_permissions(
            legacy_consent_id, owner_user_id, evidence_key)
        ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_context_shares_owner_created
    ON public.consultation_context_shares(owner_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_context_shares_expert_created
    ON public.consultation_context_shares(expert_profile_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_context_shares_participant_request
    ON public.consultation_context_shares(
        consultation_request_id, owner_user_id, expert_profile_id);

INSERT INTO public.consultation_context_shares (
    context_share_id, consultation_request_id, owner_user_id,
    intake_session_id, expert_profile_id, consent_grant_id,
    idempotency_key, journey_id, origin_dashboard, origin_reference_id,
    triage_stage, risk_level, intake_status, risk_summary,
    share_policy_version, created_at)
SELECT context_share_id, consultation_request_id, owner_user_id,
       intake_session_id, expert_profile_id, consent_grant_id,
       idempotency_key, journey_id, origin_dashboard, origin_reference_id,
       triage_stage, risk_level, intake_status, risk_summary,
       share_policy_version, created_at
  FROM carebridge_migration_bridge.story68_context_share_bridge
ON CONFLICT (context_share_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS public.consultation_context_citations (
    citation_snapshot_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    context_share_id uuid NOT NULL,
    evidence_source_id uuid NOT NULL,
    organization varchar(255) NOT NULL,
    source_url varchar(1000) NOT NULL,
    source_status_at_share varchar(30) NOT NULL,
    reviewed_at timestamptz NOT NULL,
    ordinal smallint NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT consultation_context_citation_source_uk
        UNIQUE (context_share_id, evidence_source_id),
    CONSTRAINT consultation_context_citation_approved_ck CHECK (
        source_status_at_share = 'APPROVED'),
    CONSTRAINT consultation_context_citation_https_ck CHECK (
        source_url LIKE 'https://%'),
    CONSTRAINT consultation_context_citation_ordinal_ck CHECK (ordinal >= 0)
);

ALTER TABLE public.consultation_context_citations
    DROP CONSTRAINT IF EXISTS fk_context_citation_share,
    DROP CONSTRAINT IF EXISTS fk_context_citation_source;
ALTER TABLE public.consultation_context_citations
    ADD CONSTRAINT fk_context_citation_share FOREIGN KEY (context_share_id)
        REFERENCES public.consultation_context_shares(context_share_id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_context_citation_source FOREIGN KEY (evidence_source_id)
        REFERENCES public.knowledge_sources(knowledge_source_id)
        ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_context_citations_share_ordinal
    ON public.consultation_context_citations(
        context_share_id, ordinal, citation_snapshot_id);

INSERT INTO public.consultation_context_citations (
    citation_snapshot_id, context_share_id, evidence_source_id,
    organization, source_url, source_status_at_share, reviewed_at,
    ordinal, created_at)
SELECT citation_snapshot_id, context_share_id, evidence_source_id,
       organization, source_url, source_status_at_share, reviewed_at,
       ordinal, created_at
  FROM carebridge_migration_bridge.story68_context_citation_bridge
ON CONFLICT (citation_snapshot_id) DO NOTHING;

DO $verify_story68_canonical_graph$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM carebridge_migration_bridge.story68_request_bridge bridge
          LEFT JOIN public.expert_consultation_requests target
            ON target.id = bridge.id
           AND to_jsonb(target) = (to_jsonb(bridge) - 'captured_at')
         WHERE target.id IS NULL
    ) OR EXISTS (
        SELECT 1
          FROM carebridge_migration_bridge.story68_context_share_bridge bridge
          LEFT JOIN public.consultation_context_shares target
            ON target.context_share_id = bridge.context_share_id
           AND to_jsonb(target) = (to_jsonb(bridge) - 'captured_at')
         WHERE target.context_share_id IS NULL
    ) OR EXISTS (
        SELECT 1
          FROM carebridge_migration_bridge.story68_context_citation_bridge bridge
          LEFT JOIN public.consultation_context_citations target
            ON target.citation_snapshot_id = bridge.citation_snapshot_id
           AND to_jsonb(target) = (to_jsonb(bridge) - 'captured_at')
         WHERE target.citation_snapshot_id IS NULL
    ) THEN
        RAISE EXCEPTION 'STORY68_CANONICAL_RECONCILIATION_FAILED';
    END IF;
END
$verify_story68_canonical_graph$;

CREATE OR REPLACE FUNCTION public.reject_consultation_context_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $reject_consultation_context_mutation$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME;
END
$reject_consultation_context_mutation$;

DROP TRIGGER IF EXISTS trg_consultation_context_shares_append_only
    ON public.consultation_context_shares;
CREATE TRIGGER trg_consultation_context_shares_append_only
BEFORE UPDATE OR DELETE ON public.consultation_context_shares
FOR EACH ROW EXECUTE FUNCTION public.reject_consultation_context_mutation();

DROP TRIGGER IF EXISTS trg_consultation_context_citations_append_only
    ON public.consultation_context_citations;
CREATE TRIGGER trg_consultation_context_citations_append_only
BEFORE UPDATE OR DELETE ON public.consultation_context_citations
FOR EACH ROW EXECUTE FUNCTION public.reject_consultation_context_mutation();

-- Drop only zero-row compatibility parents explicitly created by 31950.
DO $drop_story68_shadow_parents$
DECLARE
    shadow_name text;
    shadow_rows bigint;
BEGIN
    IF to_regclass(
        'carebridge_migration_bridge.story68_shadow_parent_registry') IS NULL THEN
        RETURN;
    END IF;

    FOR shadow_name IN
        SELECT table_name
          FROM carebridge_migration_bridge.story68_shadow_parent_registry
         ORDER BY table_name
    LOOP
        IF to_regclass('public.' || shadow_name) IS NOT NULL THEN
            EXECUTE format('SELECT count(*) FROM public.%I', shadow_name)
               INTO shadow_rows;
            IF shadow_rows <> 0 THEN
                RAISE EXCEPTION
                    'STORY68_SHADOW_PARENT_NOT_EMPTY: % rows=%',
                    shadow_name, shadow_rows;
            END IF;
            EXECUTE format('DROP TABLE public.%I', shadow_name);
        END IF;
    END LOOP;
END
$drop_story68_shadow_parents$;

DROP TABLE IF EXISTS carebridge_migration_bridge.story68_shadow_parent_registry;
DROP TABLE IF EXISTS carebridge_migration_bridge.story68_context_citation_bridge;
DROP TABLE IF EXISTS carebridge_migration_bridge.story68_context_share_bridge;
DROP TABLE IF EXISTS carebridge_migration_bridge.story68_request_bridge;
DROP TABLE IF EXISTS carebridge_migration_bridge.story68_history_state;

DO $drop_empty_migration_bridge_schema$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_namespace
         WHERE nspname = 'carebridge_migration_bridge'
    )
       AND NOT EXISTS (
           SELECT 1 FROM pg_class relation
           JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
           WHERE namespace.nspname = 'carebridge_migration_bridge')
       AND NOT EXISTS (
           SELECT 1 FROM pg_proc routine
           JOIN pg_namespace namespace ON namespace.oid = routine.pronamespace
           WHERE namespace.nspname = 'carebridge_migration_bridge')
       AND NOT EXISTS (
           SELECT 1 FROM pg_type type
           JOIN pg_namespace namespace ON namespace.oid = type.typnamespace
           WHERE namespace.nspname = 'carebridge_migration_bridge') THEN
        DROP SCHEMA carebridge_migration_bridge;
    END IF;
END
$drop_empty_migration_bridge_schema$;

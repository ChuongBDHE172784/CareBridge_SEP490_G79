-- Bootstrap only the empty compatibility state required by versioned migrations
-- that follow the canonical PostgreSQL 18 baseline. Historical V-migration chains
-- already created and populated these bridges and must remain untouched.

DO $baseline_compatibility_bridges$
DECLARE
    canonical_baseline_applied boolean;
    baby_profiles_missing boolean;
    outcome_evidence_missing boolean;
    expert_reference_table text;
BEGIN
    SELECT EXISTS (
        SELECT 1
          FROM public.flyway_schema_history
         WHERE script = 'B20260724111500__canonical_70_table_baseline.sql'
           AND success
    ) INTO canonical_baseline_applied;

    IF NOT canonical_baseline_applied THEN
        RETURN;
    END IF;

    CREATE SCHEMA IF NOT EXISTS carebridge_migration_bridge;

    CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story65_branch_history_state (
        migration_key text PRIMARY KEY,
        synthetic_baby_profiles boolean NOT NULL,
        synthetic_outcome_evidence boolean NOT NULL,
        CONSTRAINT chk_story65_branch_history_state_key
            CHECK (migration_key = 'V20260722019950')
    );

    IF NOT EXISTS (
        SELECT 1
          FROM carebridge_migration_bridge.story65_branch_history_state
         WHERE migration_key = 'V20260722019950'
    ) THEN
        baby_profiles_missing := to_regclass('public.baby_profiles') IS NULL;
        outcome_evidence_missing :=
            to_regclass('public.pregnancy_outcome_evidence') IS NULL;

        IF baby_profiles_missing THEN
            CREATE TABLE public.baby_profiles (
                baby_id uuid PRIMARY KEY,
                owner_user_id uuid NOT NULL,
                related_journey_id uuid
            );
        END IF;

        IF outcome_evidence_missing THEN
            CREATE TABLE public.pregnancy_outcome_evidence (
                evidence_id uuid PRIMARY KEY,
                journey_id uuid NOT NULL,
                owner_user_id uuid NOT NULL,
                outcome_type varchar(50),
                outcome_date date,
                revision_number integer NOT NULL
            );
        END IF;

        INSERT INTO carebridge_migration_bridge.story65_branch_history_state (
            migration_key,
            synthetic_baby_profiles,
            synthetic_outcome_evidence
        ) VALUES (
            'V20260722019950',
            baby_profiles_missing,
            outcome_evidence_missing
        );
    END IF;

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

    -- The baseline skips the pre-cut-off Story 6.8 table creation. Recreate
    -- that empty source shape, including its deployed constraint names, so
    -- the later canonical handoff migration follows the same preserve-and-
    -- reconcile path as an historical V-chain database.
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
        CONSTRAINT uq_context_owner_key
            UNIQUE (owner_user_id, idempotency_key),
        CONSTRAINT uq_context_intake_expert
            UNIQUE (owner_user_id, intake_session_id, expert_profile_id),
        CONSTRAINT chk_context_yellow CHECK (risk_level = 'YELLOW'),
        CONSTRAINT chk_context_completed CHECK (intake_status = 'COMPLETED'),
        CONSTRAINT chk_context_origin CHECK (
            origin_dashboard IN ('MOTHER_JOURNEY', 'BABY_PROFILE')),
        CONSTRAINT chk_context_stage CHECK (
            triage_stage IN (
                'PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM',
                'INFANT', 'TODDLER')),
        CONSTRAINT chk_context_summary CHECK (
            length(btrim(risk_summary)) BETWEEN 1 AND 500),
        CONSTRAINT chk_context_policy CHECK (
            share_policy_version = 'YELLOW_EXPERT_CONTEXT_V1')
    );

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
        CONSTRAINT uq_context_citation_source
            UNIQUE (context_share_id, evidence_source_id),
        CONSTRAINT chk_context_citation_approved CHECK (
            source_status_at_share = 'APPROVED'),
        CONSTRAINT chk_context_citation_https CHECK (
            source_url LIKE 'https://%'),
        CONSTRAINT chk_context_citation_ordinal CHECK (ordinal >= 0)
    );

    CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_records_consultation_request
        ON public.notification_records (
            user_id, reference_id, ((metadata ->> 'eventType')))
        WHERE type = 'CONSULTATION'
          AND reference_type = 'CONSULTATION_REQUEST';

    -- The canonical baseline already stores only professional_profile_id. The
    -- later immutable cut-over migration validates both identifiers before it
    -- drops the legacy one, so recreate a temporary equivalent column only for
    -- the baseline path. Historical chains already carry these columns.
    FOREACH expert_reference_table IN ARRAY ARRAY[
        'expert_credentials',
        'expert_availability',
        'expert_location_shares'
    ]
    LOOP
        EXECUTE format(
            'ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS expert_profile_id uuid',
            expert_reference_table);
        EXECUTE format(
            'UPDATE public.%I SET expert_profile_id = professional_profile_id '
            'WHERE expert_profile_id IS NULL',
            expert_reference_table);
        EXECUTE format(
            'ALTER TABLE public.%I ALTER COLUMN expert_profile_id SET NOT NULL',
            expert_reference_table);
    END LOOP;
END
$baseline_compatibility_bridges$;

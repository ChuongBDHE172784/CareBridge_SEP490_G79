-- Post-Phase-2 compatibility parents for the immutable historical Story 6.8
-- migration. These tables are deliberately empty and explicitly marked so a
-- later canonical migration can retarget the handoff graph and remove only the
-- synthetic objects created here.

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '5min';

CREATE SCHEMA IF NOT EXISTS carebridge_migration_bridge;

CREATE TABLE IF NOT EXISTS carebridge_migration_bridge.story68_shadow_parent_registry (
    table_name text PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_mother_journeys_id_owner
    ON public.mother_journeys(journey_id, owner_user_id);

DO $story68_shadow_parents$
BEGIN
    IF to_regclass('public.consultation_requests') IS NULL THEN
        CREATE TABLE public.consultation_requests (
            id uuid PRIMARY KEY,
            requester_user_id uuid NOT NULL,
            expert_profile_id uuid NOT NULL,
            client_request_id uuid NOT NULL,
            topic varchar(200),
            description varchar(2000),
            preferred_window_start timestamptz,
            preferred_window_end timestamptz,
            status varchar(20),
            reject_reason varchar(500),
            direct_conversation_id uuid,
            responded_at timestamptz,
            responded_by uuid,
            expires_at timestamptz,
            created_at timestamptz,
            updated_at timestamptz
        );
        INSERT INTO carebridge_migration_bridge.story68_shadow_parent_registry(table_name)
        VALUES ('consultation_requests') ON CONFLICT DO NOTHING;
    END IF;

    IF to_regclass('public.consent_grants') IS NULL THEN
        CREATE TABLE public.consent_grants (
            id bigint PRIMARY KEY,
            user_id uuid NOT NULL,
            evidence_key uuid NOT NULL
        );
        INSERT INTO carebridge_migration_bridge.story68_shadow_parent_registry(table_name)
        VALUES ('consent_grants') ON CONFLICT DO NOTHING;
    END IF;

    IF to_regclass('public.intake_sessions') IS NULL THEN
        CREATE TABLE public.intake_sessions (
            id uuid PRIMARY KEY,
            user_id uuid NOT NULL,
            journey_id uuid,
            origin_dashboard varchar(30),
            origin_reference_id uuid,
            stage varchar(30),
            risk_level varchar(20),
            status varchar(30)
        );
        INSERT INTO carebridge_migration_bridge.story68_shadow_parent_registry(table_name)
        VALUES ('intake_sessions') ON CONFLICT DO NOTHING;
    END IF;

    IF to_regclass('public.expert_profiles') IS NULL THEN
        CREATE TABLE public.expert_profiles (
            expert_profile_id uuid PRIMARY KEY
        );
        INSERT INTO carebridge_migration_bridge.story68_shadow_parent_registry(table_name)
        VALUES ('expert_profiles') ON CONFLICT DO NOTHING;
    END IF;

    IF to_regclass('public.evidence_sources') IS NULL THEN
        CREATE TABLE public.evidence_sources (
            id uuid PRIMARY KEY
        );
        INSERT INTO carebridge_migration_bridge.story68_shadow_parent_registry(table_name)
        VALUES ('evidence_sources') ON CONFLICT DO NOTHING;
    END IF;
END
$story68_shadow_parents$;

-- Let the immutable Story 6.5 migrations be discovered out of order after a
-- GitHub Phase 2 cutover. Their legacy repair is a no-op against these empty
-- compatibility tables; V20260724212500 re-expresses the repair against the
-- canonical data model before the shadows are retired.
CREATE SCHEMA IF NOT EXISTS carebridge_migration_bridge;

CREATE TABLE carebridge_migration_bridge.story65_branch_history_state (
    migration_key text PRIMARY KEY,
    synthetic_baby_profiles boolean NOT NULL,
    synthetic_outcome_evidence boolean NOT NULL,
    CONSTRAINT chk_story65_branch_history_state_key
        CHECK (migration_key = 'V20260722019950')
);

DO $$
DECLARE
    baby_profiles_missing boolean;
    outcome_evidence_missing boolean;
BEGIN
    IF EXISTS (SELECT 1 FROM carebridge_migration_bridge.story65_branch_history_state) THEN
        RAISE EXCEPTION 'Story 6.5 branch-history bridge state already exists';
    END IF;

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
        migration_key, synthetic_baby_profiles, synthetic_outcome_evidence
    ) VALUES (
        'V20260722019950', baby_profiles_missing, outcome_evidence_missing
    );
END $$;

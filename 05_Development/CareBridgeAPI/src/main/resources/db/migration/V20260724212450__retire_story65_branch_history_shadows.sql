-- Remove only the zero-row legacy tables created by V20260722019950. Real
-- legacy tables must already have been reconciled and removed by Phase 2.
DO $$
DECLARE
    state_count bigint;
    synthetic_baby_profiles boolean;
    synthetic_outcome_evidence boolean;
    relation_count bigint;
BEGIN
    IF to_regclass('carebridge_migration_bridge.story65_branch_history_state') IS NULL THEN
        RAISE EXCEPTION 'Story 6.5 branch-history bridge state is missing';
    END IF;

    SELECT count(*) INTO state_count
      FROM carebridge_migration_bridge.story65_branch_history_state;
    IF state_count <> 1 THEN
        RAISE EXCEPTION 'expected one Story 6.5 bridge state row, found %', state_count;
    END IF;

    SELECT state.synthetic_baby_profiles, state.synthetic_outcome_evidence
      INTO synthetic_baby_profiles, synthetic_outcome_evidence
      FROM carebridge_migration_bridge.story65_branch_history_state state
     WHERE state.migration_key = 'V20260722019950';
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Story 6.5 branch-history bridge key is missing';
    END IF;

    IF synthetic_baby_profiles THEN
        IF to_regclass('public.baby_profiles') IS NULL THEN
            RAISE EXCEPTION 'synthetic baby_profiles disappeared before retirement';
        END IF;
        SELECT count(*) INTO relation_count FROM public.baby_profiles;
        IF relation_count <> 0 THEN
            RAISE EXCEPTION 'synthetic baby_profiles unexpectedly contains % rows', relation_count;
        END IF;
        DROP TABLE public.baby_profiles;
    ELSIF to_regclass('public.baby_profiles') IS NOT NULL THEN
        RAISE EXCEPTION 'real baby_profiles remains after Phase 2 reconciliation';
    END IF;

    IF synthetic_outcome_evidence THEN
        IF to_regclass('public.pregnancy_outcome_evidence') IS NULL THEN
            RAISE EXCEPTION
                'synthetic pregnancy_outcome_evidence disappeared before retirement';
        END IF;
        SELECT count(*) INTO relation_count FROM public.pregnancy_outcome_evidence;
        IF relation_count <> 0 THEN
            RAISE EXCEPTION
                'synthetic pregnancy_outcome_evidence unexpectedly contains % rows',
                relation_count;
        END IF;
        DROP TABLE public.pregnancy_outcome_evidence;
    ELSIF to_regclass('public.pregnancy_outcome_evidence') IS NOT NULL THEN
        RAISE EXCEPTION
            'real pregnancy_outcome_evidence remains after Phase 2 reconciliation';
    END IF;
END $$;

DROP TABLE carebridge_migration_bridge.story65_branch_history_state;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_class relation
          JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
         WHERE namespace.nspname = 'carebridge_migration_bridge'
    ) AND NOT EXISTS (
        SELECT 1
          FROM pg_proc procedure
          JOIN pg_namespace namespace ON namespace.oid = procedure.pronamespace
         WHERE namespace.nspname = 'carebridge_migration_bridge'
    ) AND NOT EXISTS (
        SELECT 1
          FROM pg_type type
          JOIN pg_namespace namespace ON namespace.oid = type.typnamespace
         WHERE namespace.nspname = 'carebridge_migration_bridge'
    ) THEN
        DROP SCHEMA carebridge_migration_bridge;
    END IF;
END $$;

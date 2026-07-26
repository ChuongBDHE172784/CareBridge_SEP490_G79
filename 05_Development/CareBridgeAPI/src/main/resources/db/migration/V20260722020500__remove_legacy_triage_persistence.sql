SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

DO $$
DECLARE
    legacy_answer_rows bigint;
    legacy_assessment_rows bigint;
    canonical_session_rows bigint;
    canonical_structured_rows bigint;
    blockers text;
    owned_object record;
BEGIN
    IF to_regclass('public.triage_answers') IS NULL
       OR to_regclass('public.triage_assessments') IS NULL THEN
        RAISE EXCEPTION 'BLOCKED_PARTIAL_TRIAGE_MIGRATION: both legacy triage tables must exist';
    END IF;
    IF to_regclass('public.intake_sessions') IS NULL
       OR to_regclass('public.structured_intake_data') IS NULL THEN
        RAISE EXCEPTION 'BLOCKED_PARTIAL_TRIAGE_MIGRATION: canonical triage persistence is missing';
    END IF;

    LOCK TABLE public.triage_answers, public.triage_assessments IN ACCESS EXCLUSIVE MODE;
    LOCK TABLE public.intake_sessions, public.structured_intake_data IN SHARE MODE;

    SELECT count(*) INTO legacy_answer_rows FROM public.triage_answers;
    SELECT count(*) INTO legacy_assessment_rows FROM public.triage_assessments;
    IF legacy_answer_rows <> 0 OR legacy_assessment_rows <> 0 THEN
        RAISE EXCEPTION
            'BLOCKED_PARTIAL_TRIAGE_MIGRATION: legacy rows exist (triage_answers=%, triage_assessments=%)',
            legacy_answer_rows, legacy_assessment_rows;
    END IF;

    SELECT count(*) INTO canonical_session_rows FROM public.intake_sessions;
    SELECT count(*) INTO canonical_structured_rows FROM public.structured_intake_data;

    WITH legacy AS (
        SELECT 'public.triage_answers'::regclass::oid AS oid
        UNION ALL
        SELECT 'public.triage_assessments'::regclass::oid
    ), dependency AS (
        SELECT format('foreign key %I.%I', ns.nspname, constraint_relation.relname) AS description
          FROM pg_constraint constraint_definition
          JOIN legacy ON legacy.oid = constraint_definition.confrelid
          JOIN pg_class constraint_relation ON constraint_relation.oid = constraint_definition.conrelid
          JOIN pg_namespace ns ON ns.oid = constraint_relation.relnamespace
         WHERE constraint_definition.contype = 'f'
           AND constraint_definition.conrelid NOT IN (SELECT oid FROM legacy)
        UNION ALL
        SELECT format('%s %I.%I', dependent_relation.relkind, ns.nspname, dependent_relation.relname)
          FROM pg_depend dependency_definition
          JOIN legacy ON legacy.oid = dependency_definition.refobjid
          JOIN pg_rewrite rewrite_definition ON rewrite_definition.oid = dependency_definition.objid
          JOIN pg_class dependent_relation ON dependent_relation.oid = rewrite_definition.ev_class
          JOIN pg_namespace ns ON ns.oid = dependent_relation.relnamespace
         WHERE dependency_definition.classid = 'pg_rewrite'::regclass
           AND dependent_relation.oid NOT IN (SELECT oid FROM legacy)
           AND ns.nspname NOT IN ('pg_catalog', 'information_schema')
        UNION ALL
        SELECT format('routine %I.%I', ns.nspname, procedure_definition.proname)
          FROM pg_depend dependency_definition
          JOIN legacy ON legacy.oid = dependency_definition.refobjid
          JOIN pg_proc procedure_definition ON procedure_definition.oid = dependency_definition.objid
          JOIN pg_namespace ns ON ns.oid = procedure_definition.pronamespace
         WHERE dependency_definition.classid = 'pg_proc'::regclass
           AND ns.nspname NOT IN ('pg_catalog', 'information_schema')
        UNION ALL
        SELECT format('routine text %I.%I', ns.nspname, procedure_definition.proname)
          FROM pg_proc procedure_definition
          JOIN pg_namespace ns ON ns.oid = procedure_definition.pronamespace
         WHERE procedure_definition.prokind IN ('f', 'p')
           AND ns.nspname NOT LIKE 'pg\_%' ESCAPE '\'
           AND ns.nspname <> 'information_schema'
           AND pg_get_functiondef(procedure_definition.oid) ~*
               '(^|[^a-z0-9_])("?public"?[.])?"?(triage_answers|triage_assessments)"?([^a-z0-9_]|$)'
        UNION ALL
        SELECT format('trigger %I.%I', ns.nspname, trigger_definition.tgname)
          FROM pg_depend dependency_definition
          JOIN legacy ON legacy.oid = dependency_definition.refobjid
          JOIN pg_trigger trigger_definition ON trigger_definition.oid = dependency_definition.objid
          JOIN pg_class trigger_relation ON trigger_relation.oid = trigger_definition.tgrelid
          JOIN pg_namespace ns ON ns.oid = trigger_relation.relnamespace
         WHERE dependency_definition.classid = 'pg_trigger'::regclass
           AND trigger_definition.tgrelid NOT IN (SELECT oid FROM legacy)
           AND NOT trigger_definition.tgisinternal
        UNION ALL
        SELECT format('policy %I.%I', ns.nspname, policy_definition.polname)
          FROM pg_depend dependency_definition
          JOIN legacy ON legacy.oid = dependency_definition.refobjid
          JOIN pg_policy policy_definition ON policy_definition.oid = dependency_definition.objid
          JOIN pg_class policy_relation ON policy_relation.oid = policy_definition.polrelid
          JOIN pg_namespace ns ON ns.oid = policy_relation.relnamespace
         WHERE dependency_definition.classid = 'pg_policy'::regclass
           AND policy_definition.polrelid NOT IN (SELECT oid FROM legacy)
    )
    SELECT string_agg(DISTINCT description, ', ' ORDER BY description)
      INTO blockers
      FROM dependency;

    IF blockers IS NOT NULL THEN
        RAISE EXCEPTION 'BLOCKED_PARTIAL_TRIAGE_MIGRATION: retained catalog dependencies: %', blockers;
    END IF;

    FOR owned_object IN
        SELECT relation.relname AS table_name, constraint_definition.conname AS object_name
          FROM pg_constraint constraint_definition
          JOIN pg_class relation ON relation.oid = constraint_definition.conrelid
         WHERE constraint_definition.conrelid IN (
             'public.triage_answers'::regclass,
             'public.triage_assessments'::regclass
         )
         ORDER BY CASE relation.relname WHEN 'triage_answers' THEN 0 ELSE 1 END,
                  constraint_definition.conname
    LOOP
        EXECUTE format(
            'ALTER TABLE public.%I DROP CONSTRAINT %I',
            owned_object.table_name,
            owned_object.object_name
        );
    END LOOP;

    FOR owned_object IN
        SELECT index_relation.relname AS object_name
          FROM pg_index index_definition
          JOIN pg_class index_relation ON index_relation.oid = index_definition.indexrelid
         WHERE index_definition.indrelid IN (
             'public.triage_answers'::regclass,
             'public.triage_assessments'::regclass
         )
         ORDER BY index_relation.relname
    LOOP
        EXECUTE format('DROP INDEX public.%I', owned_object.object_name);
    END LOOP;

    DROP TABLE public.triage_answers;
    DROP TABLE public.triage_assessments;

    IF to_regclass('public.triage_answers') IS NOT NULL
       OR to_regclass('public.triage_assessments') IS NOT NULL THEN
        RAISE EXCEPTION 'BLOCKED_PARTIAL_TRIAGE_MIGRATION: legacy tables still exist after drop';
    END IF;

    IF (SELECT count(*) FROM public.intake_sessions) <> canonical_session_rows
       OR (SELECT count(*) FROM public.structured_intake_data) <> canonical_structured_rows THEN
        RAISE EXCEPTION 'BLOCKED_PARTIAL_TRIAGE_MIGRATION: canonical triage rows changed';
    END IF;
END
$$;

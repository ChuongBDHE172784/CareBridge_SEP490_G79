-- Final Cleanup wave 3: remove audited-empty dormant/removed schema. Three
-- candidates are known live-only drift and are legitimately absent on a clean bootstrap.

SET LOCAL lock_timeout = '3s';
SET LOCAL statement_timeout = '30s';

DO $cleanup$
DECLARE
    candidate text;
    candidate_oid oid;
    candidate_kind "char";
    candidate_rows bigint;
    dependency_count bigint;
    candidate_tables constant text[] := ARRAY[
        'contribution_attachments',
        'expert_identity_verifications',
        'expert_verification_documents',
        'impact_assessment_ratings',
        'medical_contributions'
    ];
    known_clean_absences constant text[] := ARRAY[
        'contribution_attachments',
        'expert_identity_verifications',
        'medical_contributions'
    ];
BEGIN
    FOREACH candidate IN ARRAY candidate_tables LOOP
        candidate_oid := NULL;
        candidate_kind := NULL;
        SELECT relation.oid, relation.relkind
          INTO candidate_oid, candidate_kind
          FROM pg_class relation
          JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
         WHERE namespace.nspname = 'public'
           AND relation.relname = candidate;

        IF candidate_oid IS NULL THEN
            IF candidate = ANY(known_clean_absences) THEN
                CONTINUE;
            END IF;
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: required base table public.% is missing', candidate;
        END IF;
        IF candidate_kind <> 'r' THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% has an unexpected relation kind', candidate;
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
             WHERE conrelid = candidate_oid AND contype = 'p'
        ) THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% has no approved primary-key shape', candidate;
        END IF;

        EXECUTE format('LOCK TABLE %I.%I IN ACCESS EXCLUSIVE MODE', 'public', candidate);
        EXECUTE format('SELECT count(*) FROM %I.%I', 'public', candidate)
           INTO candidate_rows;
        IF candidate_rows <> 0 THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% contains % row(s)', candidate, candidate_rows;
        END IF;

        SELECT count(*) INTO dependency_count
          FROM pg_constraint foreign_key
          JOIN pg_class source_relation ON source_relation.oid = foreign_key.conrelid
          JOIN pg_namespace source_namespace ON source_namespace.oid = source_relation.relnamespace
         WHERE foreign_key.contype = 'f'
           AND foreign_key.confrelid = candidate_oid
           AND NOT (
               source_namespace.nspname = 'public'
               AND source_relation.relname = ANY(candidate_tables)
           );
        IF dependency_count <> 0 THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% has retained inbound foreign keys', candidate;
        END IF;

        SELECT count(DISTINCT dependent_relation.oid) INTO dependency_count
          FROM pg_depend dependency
          JOIN pg_rewrite rewrite_rule ON rewrite_rule.oid = dependency.objid
          JOIN pg_class dependent_relation ON dependent_relation.oid = rewrite_rule.ev_class
         WHERE dependency.refobjid = candidate_oid
           AND dependent_relation.oid <> candidate_oid
           AND dependent_relation.relkind IN ('r', 'p', 'v', 'm');
        IF dependency_count <> 0 THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% has dependent views or rewrite rules', candidate;
        END IF;

        IF EXISTS (
            SELECT 1 FROM pg_trigger
             WHERE tgrelid = candidate_oid AND NOT tgisinternal
        ) OR EXISTS (
            SELECT 1 FROM pg_policy WHERE polrelid = candidate_oid
        ) THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% has trigger or RLS dependencies', candidate;
        END IF;

        SELECT count(DISTINCT routine.oid) INTO dependency_count
          FROM pg_proc routine
          JOIN pg_namespace namespace ON namespace.oid = routine.pronamespace
         WHERE routine.prokind IN ('f', 'p')
           AND namespace.nspname NOT IN ('pg_catalog', 'information_schema')
           AND namespace.nspname NOT LIKE 'pg_toast%'
           AND pg_get_functiondef(routine.oid)
               ~* format('(^|[^a-zA-Z0-9_])("?public"?[.])?"?%s"?([^a-zA-Z0-9_]|$)', candidate);
        IF dependency_count <> 0 THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% has function/procedure references', candidate;
        END IF;
    END LOOP;

    IF EXISTS (
        SELECT 1 FROM public.audit_logs
         WHERE lower(coalesce(entity_type, '')) IN (
             'impactassessmentrating', 'medicalcontribution',
             'contributionattachment', 'expertidentityverification',
             'expertverificationdocument'
         )
    ) THEN
        RAISE EXCEPTION
            'BLOCKED_FINAL_CLEANUP: audit history retains dormant-schema references';
    END IF;
END
$cleanup$;

-- Explicit child-first order; no CASCADE.
DROP TABLE IF EXISTS public.contribution_attachments;
DROP TABLE IF EXISTS public.expert_identity_verifications;
DROP TABLE IF EXISTS public.medical_contributions;
DROP TABLE public.expert_verification_documents;
DROP TABLE public.impact_assessment_ratings;

DO $reconcile$
DECLARE
    relation_name text;
BEGIN
    FOREACH relation_name IN ARRAY ARRAY[
        'contribution_attachments',
        'expert_identity_verifications',
        'expert_verification_documents',
        'impact_assessment_ratings',
        'medical_contributions'
    ] LOOP
        IF to_regclass(format('%I.%I', 'public', relation_name)) IS NOT NULL THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: reconciliation retained public.%', relation_name;
        END IF;
    END LOOP;

    IF to_regclass('public.expert_credentials') IS NULL
       OR to_regclass('public.contribution_points') IS NULL THEN
        RAISE EXCEPTION
            'BLOCKED_FINAL_CLEANUP: reconciliation lost canonical credential/contribution tables';
    END IF;
END
$reconcile$;

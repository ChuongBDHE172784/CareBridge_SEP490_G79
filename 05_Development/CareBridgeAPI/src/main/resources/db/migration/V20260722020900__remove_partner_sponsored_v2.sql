-- Final Cleanup wave 2: remove audited-empty MF-16 child persistence while
-- retaining partner_organizations and the care_facilities partner relationship.

SET LOCAL lock_timeout = '3s';
SET LOCAL statement_timeout = '30s';

DO $cleanup$
DECLARE
    candidate text;
    candidate_oid oid;
    candidate_rows bigint;
    dependency_count bigint;
    expected_primary_key text;
    candidate_tables constant text[] := ARRAY[
        'partner_expert_links',
        'partner_services',
        'sponsored_campaigns'
    ];
BEGIN
    FOREACH candidate IN ARRAY candidate_tables LOOP
        SELECT relation.oid
          INTO candidate_oid
          FROM pg_class relation
          JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
         WHERE namespace.nspname = 'public'
           AND relation.relname = candidate
           AND relation.relkind = 'r';

        IF candidate_oid IS NULL THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: required base table public.% is missing or has an unexpected shape',
                candidate;
        END IF;

        expected_primary_key := candidate || '_pkey';
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
             WHERE conrelid = candidate_oid
               AND contype = 'p'
               AND conname = expected_primary_key
        ) THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% does not match the approved primary-key shape',
                candidate;
        END IF;

        EXECUTE format('LOCK TABLE %I.%I IN ACCESS EXCLUSIVE MODE', 'public', candidate);
        EXECUTE format('SELECT count(*) FROM %I.%I', 'public', candidate)
           INTO candidate_rows;
        IF candidate_rows <> 0 THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% contains % row(s)',
                candidate, candidate_rows;
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

    IF to_regclass('public.partner_organizations') IS NULL
       OR to_regclass('public.care_facilities') IS NULL THEN
        RAISE EXCEPTION
            'BLOCKED_FINAL_CLEANUP: retained partner/facility persistence is missing';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.audit_logs
         WHERE lower(coalesce(entity_type, '')) IN
               ('partnerservice', 'sponsoredcampaign', 'partnerexpertlink')
    ) THEN
        RAISE EXCEPTION
            'BLOCKED_FINAL_CLEANUP: audit history retains partner child references';
    END IF;
END
$cleanup$;

DROP TABLE public.partner_expert_links;
DROP TABLE public.partner_services;
DROP TABLE public.sponsored_campaigns;

DO $reconcile$
DECLARE
    relation_name text;
BEGIN
    FOREACH relation_name IN ARRAY ARRAY[
        'partner_expert_links', 'partner_services', 'sponsored_campaigns'
    ] LOOP
        IF to_regclass(format('%I.%I', 'public', relation_name)) IS NOT NULL THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: reconciliation retained public.%', relation_name;
        END IF;
    END LOOP;

    IF to_regclass('public.partner_organizations') IS NULL
       OR to_regclass('public.care_facilities') IS NULL THEN
        RAISE EXCEPTION
            'BLOCKED_FINAL_CLEANUP: reconciliation lost retained partner/facility persistence';
    END IF;
END
$reconcile$;

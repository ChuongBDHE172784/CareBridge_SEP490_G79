-- Final Cleanup wave 1: remove only the audited-empty MF-15 consultation-request
-- and payment persistence. PostgreSQL runs this migration transactionally, so any
-- fail-closed preflight or reconciliation failure rolls back the complete wave.

SET LOCAL lock_timeout = '3s';
SET LOCAL statement_timeout = '30s';

DO $cleanup$
DECLARE
    candidate text;
    candidate_oid oid;
    candidate_kind "char";
    candidate_rows bigint;
    dependency_count bigint;
    expected_primary_key text;
    candidate_tables constant text[] := ARRAY[
        'commission_config',
        'commission_records',
        'consultation_disputes',
        'consultation_messages',
        'consultation_requests',
        'expert_reviews',
        'payment_transactions',
        'refund_records',
        'settlement_records'
    ];
BEGIN
    FOREACH candidate IN ARRAY candidate_tables LOOP
        SELECT relation.oid, relation.relkind
          INTO candidate_oid, candidate_kind
          FROM pg_class relation
          JOIN pg_namespace relation_namespace
            ON relation_namespace.oid = relation.relnamespace
         WHERE relation_namespace.nspname = 'public'
           AND relation.relname = candidate;

        IF candidate_oid IS NULL OR candidate_kind <> 'r' THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: required base table public.% is missing or has an unexpected relation kind',
                candidate;
        END IF;

        expected_primary_key := CASE candidate
            WHEN 'consultation_requests' THEN 'consultation_requests_pkey'
            WHEN 'consultation_messages' THEN 'consultation_messages_pkey'
            WHEN 'consultation_disputes' THEN 'consultation_disputes_pkey'
            WHEN 'payment_transactions' THEN 'payment_transactions_pkey'
            WHEN 'refund_records' THEN 'refund_records_pkey'
            WHEN 'commission_config' THEN 'commission_config_pkey'
            WHEN 'commission_records' THEN 'commission_records_pkey'
            WHEN 'settlement_records' THEN 'settlement_records_pkey'
            WHEN 'expert_reviews' THEN 'expert_reviews_pkey'
        END;

        IF NOT EXISTS (
            SELECT 1
              FROM pg_constraint primary_key
             WHERE primary_key.conrelid = candidate_oid
               AND primary_key.contype = 'p'
               AND primary_key.conname = expected_primary_key
        ) THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% does not match the approved primary-key shape',
                candidate;
        END IF;

        -- Take every candidate lock before destructive DDL. ACCESS EXCLUSIVE also
        -- prevents a row from appearing after its zero-row check.
        EXECUTE format('LOCK TABLE %I.%I IN ACCESS EXCLUSIVE MODE', 'public', candidate);
        EXECUTE format('SELECT count(*) FROM %I.%I', 'public', candidate)
           INTO candidate_rows;
        IF candidate_rows <> 0 THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% contains % row(s)',
                candidate, candidate_rows;
        END IF;

        -- Candidate-to-candidate foreign keys are released by the explicit leaf-first
        -- order below. Any inbound FK from a retained relation blocks the entire wave.
        SELECT count(*)
          INTO dependency_count
          FROM pg_constraint foreign_key
          JOIN pg_class source_relation
            ON source_relation.oid = foreign_key.conrelid
          JOIN pg_namespace source_namespace
            ON source_namespace.oid = source_relation.relnamespace
         WHERE foreign_key.contype = 'f'
           AND foreign_key.confrelid = candidate_oid
           AND NOT (
               source_namespace.nspname = 'public'
               AND source_relation.relname = ANY(candidate_tables)
           );
        IF dependency_count <> 0 THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% has % retained inbound foreign key(s)',
                candidate, dependency_count;
        END IF;

        SELECT count(DISTINCT dependent_relation.oid)
          INTO dependency_count
          FROM pg_depend dependency
          JOIN pg_rewrite rewrite_rule ON rewrite_rule.oid = dependency.objid
          JOIN pg_class dependent_relation ON dependent_relation.oid = rewrite_rule.ev_class
         WHERE dependency.refobjid = candidate_oid
           AND dependent_relation.oid <> candidate_oid
           AND dependent_relation.relkind IN ('r', 'p', 'v', 'm');
        IF dependency_count <> 0 THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% has % dependent view or rewrite rule(s)',
                candidate, dependency_count;
        END IF;

        SELECT count(*)
          INTO dependency_count
          FROM pg_trigger trigger_row
         WHERE trigger_row.tgrelid = candidate_oid
           AND NOT trigger_row.tgisinternal;
        IF dependency_count <> 0 THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% has % user trigger(s)',
                candidate, dependency_count;
        END IF;

        SELECT count(*)
          INTO dependency_count
          FROM pg_policy policy_row
         WHERE policy_row.polrelid = candidate_oid;
        IF dependency_count <> 0 THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% has % RLS policy/policies',
                candidate, dependency_count;
        END IF;

        SELECT count(*)
          INTO dependency_count
          FROM information_schema.role_table_grants grant_row
         WHERE grant_row.table_schema = 'public'
           AND grant_row.table_name = candidate
           AND grant_row.grantee <> current_user;
        IF dependency_count <> 0 THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% has % external grant(s)',
                candidate, dependency_count;
        END IF;

        -- Catalog dependencies do not expose dynamic SQL in routine bodies. Scan all
        -- non-system functions/procedures conservatively before allowing the drop.
        SELECT count(DISTINCT routine.oid)
          INTO dependency_count
          FROM pg_proc routine
          JOIN pg_namespace routine_namespace
            ON routine_namespace.oid = routine.pronamespace
         WHERE routine.prokind IN ('f', 'p')
           AND routine_namespace.nspname NOT IN ('pg_catalog', 'information_schema')
           AND routine_namespace.nspname NOT LIKE 'pg_toast%'
           AND pg_get_functiondef(routine.oid)
               ~* format(
                   '(^|[^a-zA-Z0-9_])("?public"?[.])?"?%s"?([^a-zA-Z0-9_]|$)',
                   candidate
               );
        IF dependency_count <> 0 THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% has % function/procedure reference(s)',
                candidate, dependency_count;
        END IF;
    END LOOP;

    IF to_regclass('public.uq_notification_records_consultation_request') IS NULL THEN
        RAISE EXCEPTION
            'BLOCKED_FINAL_CLEANUP: consultation-request notification index has an unexpected shape';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.notification_records
         WHERE reference_type = 'CONSULTATION_REQUEST'
    ) THEN
        RAISE EXCEPTION
            'BLOCKED_FINAL_CLEANUP: notification_records retains consultation-request history';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.audit_logs
         WHERE upper(coalesce(entity_type, '')) = 'CONSULTATION_REQUEST'
    ) THEN
        RAISE EXCEPTION
            'BLOCKED_FINAL_CLEANUP: audit_logs retains consultation-request history';
    END IF;
END
$cleanup$;

DROP INDEX public.uq_notification_records_consultation_request;

-- Explicit leaf-first dependency order; no CASCADE.
DROP TABLE public.consultation_requests;
DROP TABLE public.consultation_messages;
DROP TABLE public.expert_reviews;
DROP TABLE public.refund_records;
DROP TABLE public.settlement_records;
DROP TABLE public.commission_config;
DROP TABLE public.consultation_disputes;
DROP TABLE public.commission_records;
DROP TABLE public.payment_transactions;

DO $reconcile$
DECLARE
    relation_name text;
BEGIN
    FOREACH relation_name IN ARRAY ARRAY[
        'consultation_requests',
        'consultation_messages',
        'consultation_disputes',
        'payment_transactions',
        'refund_records',
        'commission_config',
        'commission_records',
        'settlement_records',
        'expert_reviews'
    ] LOOP
        IF to_regclass(format('%I.%I', 'public', relation_name)) IS NOT NULL THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: reconciliation found retained candidate public.%',
                relation_name;
        END IF;
    END LOOP;

    FOREACH relation_name IN ARRAY ARRAY[
        'consultation_bookings',
        'consultation_sessions',
        'consultation_price_bands',
        'expert_consultation_prices',
        'direct_conversations',
        'direct_messages',
        'conversation_calls',
        'partner_organizations'
    ] LOOP
        IF to_regclass(format('%I.%I', 'public', relation_name)) IS NULL THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: reconciliation lost blocked table public.%',
                relation_name;
        END IF;
    END LOOP;

    IF to_regclass('public.uq_notification_records_consultation_request') IS NOT NULL THEN
        RAISE EXCEPTION
            'BLOCKED_FINAL_CLEANUP: consultation-request notification index was not removed';
    END IF;
END
$reconcile$;

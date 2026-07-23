-- Final Cleanup wave 1: remove only the audited-empty MF-15 consultation-request
-- and payment persistence. PostgreSQL runs this migration transactionally, so any
-- fail-closed preflight or reconciliation failure rolls back the complete wave.

SET LOCAL lock_timeout = '3s';
SET LOCAL statement_timeout = '30s';

DO $cleanup$
DECLARE
    candidate text;
    candidate_oid oid;
    locked_candidate_oid oid;
    candidate_kind "char";
    candidate_rows bigint;
    dependency_count bigint;
    expected_primary_key text;
    expected_primary_key_definition text;
    expected_column_signatures text[];
    actual_column_signature text;
    expected_catalog_signatures text[];
    actual_catalog_signature text;
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

        -- Lock the resolved object before fingerprinting. Re-resolving the OID
        -- prevents a concurrent DROP/CREATE of the same relation name from
        -- substituting an unapproved table between discovery and validation.
        EXECUTE format('LOCK TABLE %I.%I IN ACCESS EXCLUSIVE MODE', 'public', candidate);
        SELECT relation.oid INTO locked_candidate_oid
          FROM pg_class relation
          JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
         WHERE namespace.nspname = 'public'
           AND relation.relname = candidate
           AND relation.relkind = 'r';
        IF locked_candidate_oid IS DISTINCT FROM candidate_oid THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% changed during preflight', candidate;
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
        expected_primary_key_definition := CASE candidate
            WHEN 'consultation_requests' THEN 'PRIMARY KEY (id)'
            WHEN 'consultation_messages' THEN 'PRIMARY KEY (message_id)'
            WHEN 'consultation_disputes' THEN 'PRIMARY KEY (dispute_id)'
            WHEN 'payment_transactions' THEN 'PRIMARY KEY (payment_id)'
            WHEN 'refund_records' THEN 'PRIMARY KEY (refund_id)'
            WHEN 'commission_config' THEN 'PRIMARY KEY (id)'
            WHEN 'commission_records' THEN 'PRIMARY KEY (commission_id)'
            WHEN 'settlement_records' THEN 'PRIMARY KEY (settlement_id)'
            WHEN 'expert_reviews' THEN 'PRIMARY KEY (review_id)'
        END;
        expected_column_signatures := CASE candidate
            WHEN 'consultation_requests' THEN ARRAY['2ed13ca0b263204f55d8f69b5a7a9073', '3323ecf65b5f62149f245429350254f6']
            WHEN 'consultation_messages' THEN ARRAY['a16144283b838f69c3cf78516495d613', 'f88384e1fe9e143e0b5a8ada22f7c355']
            WHEN 'consultation_disputes' THEN ARRAY['444edf7b6602abb17e5590b075b84d31']
            WHEN 'payment_transactions' THEN ARRAY['2a9b0fceac505f7dc4ec6a7cb3cd01dc']
            WHEN 'refund_records' THEN ARRAY['453a1597cfb21fda978d2f3f150ee083']
            WHEN 'commission_config' THEN ARRAY['5752f4c861e49c6e8d880782f261955b']
            WHEN 'commission_records' THEN ARRAY['3c220aaf15cd323e8d089de54b44c8e1']
            WHEN 'settlement_records' THEN ARRAY['a08e380c5824f48500ac428e6932eccd']
            WHEN 'expert_reviews' THEN ARRAY['c1c9c39de11b74a83b26f7d4e76fbd68']
        END;
        expected_catalog_signatures := CASE candidate
            WHEN 'consultation_requests' THEN ARRAY['720fab3fc280ca261c74c120160ac1b0', 'aa255d3807b8119b46795cb2a4f19f16']
            WHEN 'consultation_messages' THEN ARRAY['326127dcd9930c899db6f3f7ef89c875', '69a82c8d2f27d4ae6c77d112234fb379']
            WHEN 'consultation_disputes' THEN ARRAY['1d236d406dc3707765a0ec99f5ee0fa5']
            WHEN 'payment_transactions' THEN ARRAY['521d682a5ba2f5573d828c749fa8dd82']
            WHEN 'refund_records' THEN ARRAY['1c45d240e31cdbbdceefe2feab89d9d3']
            WHEN 'commission_config' THEN ARRAY['b90913a228adae19d1be401ab05ca4ec']
            WHEN 'commission_records' THEN ARRAY['956742a67c22adb61b6c788f8ca18de5', 'fe5a4ab3cd6b6c666ffbe3bb6458e460']
            WHEN 'settlement_records' THEN ARRAY['20cffe292c89e4b9debfa9495fc6736c', '8cf7e4262028b6f756905851128f2ea3']
            WHEN 'expert_reviews' THEN ARRAY['38b76eaca0a04dee9902ec7ccf7187b6', 'fe7716db3724279d151f1dda6ca82b1a']
        END;
        SELECT md5(string_agg(
                   format('%s:%s:%s', column_name, udt_name, is_nullable),
                   ',' ORDER BY ordinal_position))
          INTO actual_column_signature
          FROM information_schema.columns
         WHERE table_schema = 'public' AND table_name = candidate;
        SELECT md5(
                   coalesce((SELECT string_agg(format('%s:%s:%s:%s', column_name, udt_name, is_nullable, coalesce(column_default, '')), ',' ORDER BY ordinal_position) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = candidate), '') || '|' ||
                   coalesce((SELECT string_agg(format('%s:%s:%s', conname, contype, pg_get_constraintdef(oid, true)), ',' ORDER BY conname) FROM pg_constraint WHERE conrelid = candidate_oid), '') || '|' ||
                   coalesce((SELECT string_agg(indexname || ':' || indexdef, ',' ORDER BY indexname) FROM pg_indexes WHERE schemaname = 'public' AND tablename = candidate), '')
               )
          INTO actual_catalog_signature;

        IF NOT (actual_column_signature = ANY(expected_column_signatures))
           OR NOT (actual_catalog_signature = ANY(expected_catalog_signatures))
           OR NOT EXISTS (
            SELECT 1
              FROM pg_constraint primary_key
             WHERE primary_key.conrelid = candidate_oid
               AND primary_key.contype = 'p'
               AND primary_key.conname = expected_primary_key
               AND pg_get_constraintdef(primary_key.oid, true) = expected_primary_key_definition
        ) THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% does not match the approved catalog shape',
                candidate;
        END IF;

        -- ACCESS EXCLUSIVE prevents a row from appearing after its zero-row check.
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

        IF EXISTS (
            SELECT 1
              FROM pg_class relation
              CROSS JOIN LATERAL aclexplode(
                  coalesce(relation.relacl, acldefault('r', relation.relowner))) acl
             WHERE relation.oid = candidate_oid
               AND acl.grantee NOT IN (relation.relowner, to_regrole(current_user))
        ) OR EXISTS (
            SELECT 1
              FROM pg_attribute attribute
              CROSS JOIN LATERAL aclexplode(attribute.attacl) acl
             WHERE attribute.attrelid = candidate_oid
               AND attribute.attacl IS NOT NULL
               AND acl.grantee NOT IN (
                   (SELECT relowner FROM pg_class WHERE oid = candidate_oid),
                   to_regrole(current_user))
        ) THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% has external table or column grants', candidate;
        END IF;
        IF EXISTS (
            SELECT 1 FROM pg_publication_tables
             WHERE schemaname = 'public' AND tablename = candidate
        )
           OR EXISTS (
               SELECT 1 FROM pg_inherits
                WHERE inhrelid = candidate_oid OR inhparent = candidate_oid
           ) THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% participates in publication or partitioning', candidate;
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM pg_class
             WHERE oid = candidate_oid AND relowner = to_regrole(current_user)
        ) THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% is not owned by the migration role', candidate;
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

    LOCK TABLE public.notification_records, public.audit_logs
        IN SHARE ROW EXCLUSIVE MODE;

    IF NOT EXISTS (
        SELECT 1
          FROM pg_class index_relation
          JOIN pg_namespace index_namespace
            ON index_namespace.oid = index_relation.relnamespace
          JOIN pg_index index_metadata
            ON index_metadata.indexrelid = index_relation.oid
         WHERE index_namespace.nspname = 'public'
           AND index_relation.relname = 'uq_notification_records_consultation_request'
           AND index_metadata.indrelid = 'public.notification_records'::regclass
           AND index_metadata.indisunique
           AND index_metadata.indnkeyatts = 3
           AND pg_get_indexdef(index_relation.oid) =
               'CREATE UNIQUE INDEX uq_notification_records_consultation_request ON public.notification_records USING btree (user_id, reference_id, ((metadata ->> ''eventType''::text))) WHERE (((type)::text = ''CONSULTATION''::text) AND ((reference_type)::text = ''CONSULTATION_REQUEST''::text))'
    ) THEN
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

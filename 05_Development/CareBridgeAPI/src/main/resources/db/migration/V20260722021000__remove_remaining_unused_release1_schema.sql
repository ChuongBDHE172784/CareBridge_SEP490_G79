-- Final Cleanup wave 3: remove audited-empty dormant/removed schema. Three
-- candidates are known live-only drift and are legitimately absent on a clean bootstrap.

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
    expected_signatures text[];
    actual_signature text;
    expected_primary_key text;
    expected_catalog_signatures text[];
    actual_catalog_signature text;
    validated_present text[] := ARRAY[]::text[];
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
        expected_signatures := CASE candidate
            WHEN 'contribution_attachments' THEN ARRAY['attachment_id:uuid:NO,access_mode:varchar:NO,contribution_id:uuid:NO,created_at:timestamptz:NO,display_order:int4:NO,file_id:uuid:NO,kind:varchar:NO,owner_user_id:uuid:NO,purpose:varchar:NO']
            WHEN 'expert_identity_verifications' THEN ARRAY['identity_verification_id:uuid:NO,created_at:timestamptz:NO,expert_profile_id:uuid:NO,face_provider:varchar:NO,face_similarity:numeric:YES,face_status:varchar:NO,face_threshold:numeric:YES,identity_back_file_id:uuid:NO,identity_front_file_id:uuid:NO,provider_error_code:varchar:YES,review_reason:text:YES,review_status:varchar:NO,reviewed_at:timestamptz:YES,reviewed_by:uuid:YES,selfie_file_id:uuid:NO,updated_at:timestamptz:NO,detection_id_card_status:varchar:YES,detection_selfie_status:varchar:YES,id_card_crop_file_id:uuid:YES,pipeline_error_code:varchar:YES,pipeline_status:varchar:YES,processed_at:timestamptz:YES,selfie_crop_file_id:uuid:YES']
            WHEN 'expert_verification_documents' THEN ARRAY[
                'id:uuid:NO,expert_profile_id:uuid:NO,doc_type:varchar:NO,storage_key:varchar:NO,original_name:varchar:NO,mime_type:varchar:NO,size_bytes:int8:NO,status:varchar:NO,reject_reason:text:YES,uploaded_at:timestamptz:NO,reviewed_at:timestamptz:YES,reviewed_by:uuid:YES',
                'id:uuid:NO,expert_profile_id:uuid:NO,doc_type:varchar:NO,storage_key:varchar:NO,original_name:varchar:NO,mime_type:varchar:NO,size_bytes:int8:NO,status:varchar:NO,reject_reason:text:YES,uploaded_at:timestamptz:NO,reviewed_at:timestamptz:YES,reviewed_by:uuid:YES,expert_id:uuid:NO'
            ]
            WHEN 'impact_assessment_ratings' THEN ARRAY['rating_id:uuid:NO,user_id:uuid:NO,content_id:uuid:YES,rating_value:numeric:YES,feedback_text:text:YES,created_at:timestamptz:NO,updated_at:timestamptz:NO']
            WHEN 'medical_contributions' THEN ARRAY['contribution_id:uuid:NO,content:text:NO,created_at:timestamptz:NO,expert_user_id:uuid:NO,hospital_id:varchar:YES,rejection_reason:varchar:YES,specialty_id:varchar:YES,status:varchar:NO,title:varchar:NO,updated_at:timestamptz:NO,version:int4:NO']
        END;
        expected_primary_key := CASE candidate
            WHEN 'contribution_attachments' THEN 'PRIMARY KEY (attachment_id)'
            WHEN 'expert_identity_verifications' THEN 'PRIMARY KEY (identity_verification_id)'
            WHEN 'expert_verification_documents' THEN 'PRIMARY KEY (id)'
            WHEN 'impact_assessment_ratings' THEN 'PRIMARY KEY (rating_id)'
            WHEN 'medical_contributions' THEN 'PRIMARY KEY (contribution_id)'
        END;
        expected_catalog_signatures := CASE candidate
            WHEN 'contribution_attachments' THEN ARRAY['3c4c248675152e9351996233b85cdd04']
            WHEN 'expert_identity_verifications' THEN ARRAY['90921f4a084e36edef12c416122ec488']
            WHEN 'expert_verification_documents' THEN ARRAY['c180fef376e1e66666f9827b54675d3f', '4a724640a47a73cb353d7dae7ee17470']
            WHEN 'impact_assessment_ratings' THEN ARRAY['67911971f498c52fc868144586969d18']
            WHEN 'medical_contributions' THEN ARRAY['ebfb5e157e68948c15107403620769f9']
        END;
        SELECT string_agg(
                   format('%s:%s:%s', column_name, udt_name, is_nullable),
                   ',' ORDER BY ordinal_position)
          INTO actual_signature
         FROM information_schema.columns
         WHERE table_schema = 'public' AND table_name = candidate;
        SELECT md5(
                   coalesce((SELECT string_agg(format('%s:%s:%s:%s', column_name, udt_name, is_nullable, coalesce(column_default, '')), ',' ORDER BY ordinal_position) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = candidate), '') || '|' ||
                   coalesce((SELECT string_agg(format('%s:%s:%s', conname, contype, pg_get_constraintdef(oid, true)), ',' ORDER BY conname) FROM pg_constraint WHERE conrelid = candidate_oid), '') || '|' ||
                   coalesce((SELECT string_agg(indexname || ':' || indexdef, ',' ORDER BY indexname) FROM pg_indexes WHERE schemaname = 'public' AND tablename = candidate), '')
               )
          INTO actual_catalog_signature;
        IF NOT (actual_signature = ANY(expected_signatures))
           OR NOT (actual_catalog_signature = ANY(expected_catalog_signatures))
           OR NOT EXISTS (
            SELECT 1 FROM pg_constraint
             WHERE conrelid = candidate_oid
               AND contype = 'p'
               AND pg_get_constraintdef(oid, true) = expected_primary_key
        ) THEN
            RAISE EXCEPTION
                'BLOCKED_FINAL_CLEANUP: public.% has an unapproved catalog shape', candidate;
        END IF;

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
        validated_present := array_append(validated_present, candidate);
    END LOOP;

    LOCK TABLE public.audit_logs IN SHARE ROW EXCLUSIVE MODE;

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

    -- Drop only relations that were present, locked, and fingerprinted above.
    -- A known-clean-absent relation that appears concurrently is never touched;
    -- reconciliation below detects it and rolls this migration back.
    IF 'contribution_attachments' = ANY(validated_present) THEN
        EXECUTE 'DROP TABLE public.contribution_attachments';
    END IF;
    IF 'expert_identity_verifications' = ANY(validated_present) THEN
        EXECUTE 'DROP TABLE public.expert_identity_verifications';
    END IF;
    IF 'medical_contributions' = ANY(validated_present) THEN
        EXECUTE 'DROP TABLE public.medical_contributions';
    END IF;
    IF 'expert_verification_documents' = ANY(validated_present) THEN
        EXECUTE 'DROP TABLE public.expert_verification_documents';
    END IF;
    IF 'impact_assessment_ratings' = ANY(validated_present) THEN
        EXECUTE 'DROP TABLE public.impact_assessment_ratings';
    END IF;
END
$cleanup$;

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

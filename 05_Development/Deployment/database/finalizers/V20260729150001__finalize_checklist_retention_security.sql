-- CHK-039 privileged post-Flyway finalizer, version 20260729150001.
-- Run exactly through the checked deployment runner after all Flyway migrations
-- and before application startup. The entire finalization is transactional.

BEGIN;

DO $$
DECLARE
    role_name text;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_roles
        WHERE rolname = current_user AND rolsuper = true
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_FINALIZER_REQUIRES_SUPERUSER'
            USING ERRCODE = '42501';
    END IF;

    FOREACH role_name IN ARRAY ARRAY[
        'carebridge_checklist_retention_owner',
        'carebridge_checklist_schema_owner'
    ] LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_catalog.pg_roles
            WHERE rolname = role_name
              AND rolcanlogin = false AND rolsuper = false
              AND rolcreatedb = false AND rolcreaterole = false
              AND rolinherit = false AND rolreplication = false
              AND rolbypassrls = false
        ) THEN
            RAISE EXCEPTION 'CHECKLIST_RETENTION_NOLOGIN_ROLE_INVALID: %', role_name
                USING ERRCODE = '42501';
        END IF;
    END LOOP;

    FOREACH role_name IN ARRAY ARRAY['checklist_operations', 'carebridge_application'] LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_catalog.pg_roles
            WHERE rolname = role_name
              AND rolcanlogin = true AND rolsuper = false
              AND rolcreatedb = false AND rolcreaterole = false
              AND rolinherit = false AND rolreplication = false
              AND rolbypassrls = false
        ) THEN
            RAISE EXCEPTION 'CHECKLIST_RETENTION_LOGIN_ROLE_INVALID: %', role_name
                USING ERRCODE = '42501';
        END IF;
    END LOOP;

    IF EXISTS (
        SELECT 1
        FROM pg_catalog.pg_auth_members membership
        JOIN pg_catalog.pg_roles granted_role ON granted_role.oid = membership.roleid
        JOIN pg_catalog.pg_roles member_role ON member_role.oid = membership.member
        WHERE granted_role.rolname IN (
                  'carebridge_checklist_retention_owner',
                  'carebridge_checklist_schema_owner',
                  'checklist_operations',
                  'carebridge_application')
           OR member_role.rolname IN (
                  'carebridge_checklist_retention_owner',
                  'carebridge_checklist_schema_owner',
                  'checklist_operations',
                  'carebridge_application')
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_ROLE_MEMBERSHIPS_MUST_BE_EMPTY'
            USING ERRCODE = '42501';
    END IF;

    IF to_regprocedure('public.checklist_purge_retained_records(uuid)') IS NULL THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_PURGE_FUNCTION_REQUIRED'
            USING ERRCODE = '42501';
    END IF;
END $$;

-- Preflight semantic attestation runs before any ownership or ACL mutation.
-- pg_get_functiondef is canonicalized by PostgreSQL; hashing it catches body,
-- language, volatility and SET-clause drift even when the SQL signature remains
-- unchanged. The expected digests are PostgreSQL 18.1 canonical values.
DO $$
DECLARE
    schema_owner_oid oid := (
        SELECT oid FROM pg_catalog.pg_roles
        WHERE rolname = 'carebridge_checklist_schema_owner');
    retention_owner_oid oid := (
        SELECT oid FROM pg_catalog.pg_roles
        WHERE rolname = 'carebridge_checklist_retention_owner');
    public_schema_owner name := (
        SELECT owner_role.rolname
        FROM pg_catalog.pg_namespace namespace
        JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = namespace.nspowner
        WHERE namespace.nspname = 'public');
    database_owner_oid oid := (
        SELECT database_entry.datdba
        FROM pg_catalog.pg_database database_entry
        WHERE database_entry.datname = current_database());
    verifier_oid oid := to_regprocedure(
        'public.checklist_assert_retention_security()');
BEGIN
    -- A first finalization may transfer ownership from the Flyway LOGIN role.
    -- Once the callable exists, any owner other than the trusted NOLOGIN role
    -- is drift and must not be silently repaired by rerunning the finalizer.
    IF database_owner_oid <> schema_owner_oid
       AND (verifier_oid IS NOT NULL OR NOT EXISTS (
           SELECT 1
           FROM pg_catalog.pg_roles database_owner
           WHERE database_owner.oid = database_owner_oid
             AND database_owner.rolcanlogin = true)) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_DATABASE_OWNER_INVALID';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM (VALUES
            ('public.carebridge_reject_mutation()'::regprocedure,
            'ea7dbbec7ff2edeab8e24861ea89f4b981750245f26d6806d16258d62449d585'),
            ('public.checklist_action_command_retention_guard()'::regprocedure,
             'e6fd633447f50210f53226a59333893ac2b2eafcde54f6ab2638ec7ee8c16677'),
            ('public.checklist_quarantine_forensic_guard()'::regprocedure,
             '2af5d1644dc6108bc875865f6b472713ece01f51417e9640c9a2b01afbf8f299'),
            ('public.checklist_purge_retained_records(uuid)'::regprocedure,
             'cbb5b155860edef8cd0b637726e3beb991d4fb019715df5b22ee78324ab36546')
        ) expected(function_oid, definition_sha256)
        JOIN pg_catalog.pg_proc routine ON routine.oid = expected.function_oid
        JOIN pg_catalog.pg_language language_entry ON language_entry.oid = routine.prolang
        WHERE language_entry.lanname <> 'plpgsql'
           OR routine.prokind <> 'f'
           OR routine.provolatile <> 'v'
           OR routine.proparallel <> 'u'
           OR routine.proisstrict <> false
           OR routine.proleakproof <> false
           OR encode(sha256(convert_to(pg_get_functiondef(routine.oid), 'UTF8')), 'hex')
                  <> expected.definition_sha256
           OR (routine.oid = 'public.checklist_purge_retained_records(uuid)'::regprocedure
               AND (routine.prosecdef <> true
                    OR routine.proconfig <> ARRAY['search_path=pg_catalog, public']::text[]))
           OR (routine.oid <> 'public.checklist_purge_retained_records(uuid)'::regprocedure
               AND (routine.prosecdef <> false OR routine.proconfig IS NOT NULL))
    ) OR (SELECT count(*) FROM pg_catalog.pg_proc routine
          WHERE routine.oid IN (
              'public.carebridge_reject_mutation()'::regprocedure,
              'public.checklist_action_command_retention_guard()'::regprocedure,
              'public.checklist_quarantine_forensic_guard()'::regprocedure,
              'public.checklist_purge_retained_records(uuid)'::regprocedure)) <> 4 THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_PRIVILEGED_FUNCTION_INTEGRITY_FAILED';
    END IF;

    -- Before handoff the schema and function owners are the same Flyway login;
    -- an already finalized database must retain pg_database_owner instead.
    IF public_schema_owner <> 'pg_database_owner'
       AND EXISTS (
           SELECT 1
           FROM pg_catalog.pg_proc routine
           WHERE routine.oid =
                     'public.checklist_purge_retained_records(uuid)'::regprocedure
             AND routine.proowner = retention_owner_oid) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_PUBLIC_SCHEMA_OWNER_INVALID';
    END IF;

    IF (SELECT count(*)
        FROM (VALUES
            ('audit_events_immutable_trg',
             'public.audit_events'::regclass,
             'public.carebridge_reject_mutation()'::regprocedure,
             27::smallint,
             'CREATE TRIGGER audit_events_immutable_trg BEFORE DELETE OR UPDATE ON public.audit_events FOR EACH ROW EXECUTE FUNCTION carebridge_reject_mutation()'),
            ('checklist_quarantine_forensic_guard_trg',
             'public.checklist_migration_quarantine'::regclass,
             'public.checklist_quarantine_forensic_guard()'::regprocedure,
             27::smallint,
             'CREATE TRIGGER checklist_quarantine_forensic_guard_trg BEFORE DELETE OR UPDATE ON public.checklist_migration_quarantine FOR EACH ROW EXECUTE FUNCTION checklist_quarantine_forensic_guard()'),
            ('checklist_action_command_retention_guard_trg',
             'public.checklist_action_commands'::regclass,
             'public.checklist_action_command_retention_guard()'::regprocedure,
             11::smallint,
             'CREATE TRIGGER checklist_action_command_retention_guard_trg BEFORE DELETE ON public.checklist_action_commands FOR EACH ROW EXECUTE FUNCTION checklist_action_command_retention_guard()'),
            ('checklist_validate_action_command_target_trg',
             'public.checklist_action_commands'::regclass,
             'public.checklist_validate_action_command_target()'::regprocedure,
             7::smallint,
             'CREATE TRIGGER checklist_validate_action_command_target_trg BEFORE INSERT ON public.checklist_action_commands FOR EACH ROW EXECUTE FUNCTION checklist_validate_action_command_target()')
        ) expected(trigger_name, table_oid, function_oid, trigger_type,
                   trigger_definition)
        JOIN pg_catalog.pg_trigger trigger_entry
          ON trigger_entry.tgname = expected.trigger_name
         AND trigger_entry.tgrelid = expected.table_oid
         AND trigger_entry.tgfoid = expected.function_oid
         AND trigger_entry.tgtype = expected.trigger_type
        WHERE trigger_entry.tgenabled = 'O'
          AND trigger_entry.tgisinternal = false
          AND pg_get_triggerdef(trigger_entry.oid, false) = expected.trigger_definition
          AND trigger_entry.tgqual IS NULL
          AND trigger_entry.tgattr = ''::int2vector
          AND octet_length(trigger_entry.tgargs) = 0) <> 4
       OR EXISTS (
           SELECT 1
           FROM pg_catalog.pg_trigger trigger_entry
           WHERE trigger_entry.tgrelid IN (
                     'public.audit_events'::regclass,
                     'public.checklist_migration_quarantine'::regclass,
                     'public.checklist_action_commands'::regclass)
             AND trigger_entry.tgisinternal = false
             AND (trigger_entry.tgname, trigger_entry.tgrelid) NOT IN (
                 ('audit_events_immutable_trg',
                  'public.audit_events'::regclass),
                 ('checklist_quarantine_forensic_guard_trg',
                  'public.checklist_migration_quarantine'::regclass),
                 ('checklist_action_command_retention_guard_trg',
                  'public.checklist_action_commands'::regclass),
                 ('checklist_validate_action_command_target_trg',
                  'public.checklist_action_commands'::regclass))) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_TRIGGER_INVARIANT_FAILED'
            USING DETAIL = (
                SELECT jsonb_pretty(jsonb_agg(jsonb_build_object(
                    'name', trigger_entry.tgname,
                    'relation', trigger_entry.tgrelid::regclass::text,
                    'function', trigger_entry.tgfoid::regprocedure::text,
                    'type', trigger_entry.tgtype,
                    'enabled', trigger_entry.tgenabled,
                    'internal', trigger_entry.tgisinternal,
                    'definition', pg_get_triggerdef(trigger_entry.oid, false),
                    'qual', pg_get_expr(trigger_entry.tgqual, trigger_entry.tgrelid),
                    'attr', trigger_entry.tgattr::text,
                    'args', encode(trigger_entry.tgargs, 'hex'))
                    ORDER BY trigger_entry.tgname))::text
                FROM pg_catalog.pg_trigger trigger_entry
                WHERE trigger_entry.tgrelid IN (
                    'public.audit_events'::regclass,
                    'public.checklist_migration_quarantine'::regclass,
                    'public.checklist_action_commands'::regclass)
                  AND trigger_entry.tgisinternal = false);
    END IF;
END $$;

-- Transfer protected schema objects away from the Flyway login. Only the purge
-- function is owned by the retention SECURITY DEFINER owner.
GRANT USAGE, CREATE ON SCHEMA public TO carebridge_checklist_schema_owner;
GRANT USAGE, CREATE ON SCHEMA public TO carebridge_checklist_retention_owner;

ALTER TABLE public.audit_events OWNER TO carebridge_checklist_schema_owner;
ALTER TABLE public.checklist_migration_quarantine OWNER TO carebridge_checklist_schema_owner;
ALTER TABLE public.checklist_action_commands OWNER TO carebridge_checklist_schema_owner;
ALTER FUNCTION public.checklist_quarantine_forensic_guard()
    OWNER TO carebridge_checklist_schema_owner;
ALTER FUNCTION public.checklist_action_command_retention_guard()
    OWNER TO carebridge_checklist_schema_owner;
ALTER FUNCTION public.carebridge_reject_mutation()
    OWNER TO carebridge_checklist_schema_owner;
ALTER FUNCTION public.checklist_purge_retained_records(uuid)
    OWNER TO carebridge_checklist_retention_owner;
DO $$
BEGIN
    EXECUTE format(
        'ALTER DATABASE %I OWNER TO carebridge_checklist_schema_owner',
        current_database());
END $$;
ALTER SCHEMA public OWNER TO pg_database_owner;

CREATE OR REPLACE FUNCTION public.checklist_assert_retention_security()
RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
VOLATILE
PARALLEL UNSAFE
SET search_path = pg_catalog, public
AS $retention_verifier$
DECLARE
    actual_fingerprint text;
    actual_digest text;
BEGIN
    WITH function_acl AS (
        SELECT routine.oid,
               string_agg(
                   COALESCE(grantee_role.rolname,
                            CASE WHEN acl.grantee = 0 THEN 'PUBLIC' END,
                            acl.grantee::text)
                   || ':' || acl.privilege_type || ':' || acl.is_grantable::text,
                   ',' ORDER BY COALESCE(grantee_role.rolname,
                       CASE WHEN acl.grantee = 0 THEN 'PUBLIC' END,
                       acl.grantee::text), acl.privilege_type)
                   AS acl_text
        FROM pg_catalog.pg_proc routine
        CROSS JOIN LATERAL aclexplode(COALESCE(
            routine.proacl, acldefault('f', routine.proowner))) acl
        LEFT JOIN pg_catalog.pg_roles grantee_role ON grantee_role.oid = acl.grantee
        WHERE routine.oid IN (
            'public.carebridge_reject_mutation()'::regprocedure,
            'public.checklist_action_command_retention_guard()'::regprocedure,
            'public.checklist_quarantine_forensic_guard()'::regprocedure,
            'public.checklist_purge_retained_records(uuid)'::regprocedure)
        GROUP BY routine.oid
    ), table_acl AS (
        SELECT protected.oid,
               string_agg(
                   COALESCE(grantee_role.rolname,
                            CASE WHEN acl.grantee = 0 THEN 'PUBLIC' END,
                            acl.grantee::text)
                   || ':' || acl.privilege_type || ':' || acl.is_grantable::text,
                   ',' ORDER BY COALESCE(grantee_role.rolname,
                       CASE WHEN acl.grantee = 0 THEN 'PUBLIC' END,
                       acl.grantee::text), acl.privilege_type)
                   AS acl_text
        FROM pg_catalog.pg_class protected
        CROSS JOIN LATERAL aclexplode(COALESCE(
            protected.relacl, acldefault('r', protected.relowner))) acl
        LEFT JOIN pg_catalog.pg_roles grantee_role ON grantee_role.oid = acl.grantee
        WHERE protected.oid IN (
            'public.audit_events'::regclass,
            'public.checklist_migration_quarantine'::regclass,
            'public.checklist_action_commands'::regclass)
        GROUP BY protected.oid
    )
    SELECT concat_ws('|',
        'database=' || (
            SELECT owner_role.rolname
            FROM pg_catalog.pg_database database_entry
            JOIN pg_catalog.pg_roles owner_role
              ON owner_role.oid = database_entry.datdba
            WHERE database_entry.datname = current_database()),
        'schema=' || (
            SELECT owner_role.rolname || ':' || COALESCE(string_agg(
                COALESCE(grantee_role.rolname,
                         CASE WHEN acl.grantee = 0 THEN 'PUBLIC' END,
                         acl.grantee::text)
                || ':' || acl.privilege_type || ':' || acl.is_grantable::text,
                ',' ORDER BY COALESCE(grantee_role.rolname,
                    CASE WHEN acl.grantee = 0 THEN 'PUBLIC' END,
                    acl.grantee::text), acl.privilege_type), '')
            FROM pg_catalog.pg_namespace namespace
            JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = namespace.nspowner
            LEFT JOIN LATERAL aclexplode(COALESCE(
                namespace.nspacl, acldefault('n', namespace.nspowner))) acl ON true
            LEFT JOIN pg_catalog.pg_roles grantee_role ON grantee_role.oid = acl.grantee
            WHERE namespace.nspname = 'public'
            GROUP BY owner_role.rolname),
        'roles=' || COALESCE((
            SELECT string_agg(
                role_entry.rolname || ':' || role_entry.rolcanlogin::text || ':'
                || role_entry.rolsuper::text || ':' || role_entry.rolcreatedb::text || ':'
                || role_entry.rolcreaterole::text || ':' || role_entry.rolinherit::text || ':'
                || role_entry.rolreplication::text || ':' || role_entry.rolbypassrls::text,
                ',' ORDER BY role_entry.rolname)
            FROM pg_catalog.pg_roles role_entry
            WHERE role_entry.rolname IN (
                'carebridge_checklist_retention_owner',
                'carebridge_checklist_schema_owner',
                'checklist_operations', 'carebridge_application')), ''),
        'memberships=' || COALESCE((
            SELECT string_agg(granted_role.rolname || '>' || member_role.rolname,
                              ',' ORDER BY granted_role.rolname, member_role.rolname)
            FROM pg_catalog.pg_auth_members membership
            JOIN pg_catalog.pg_roles granted_role ON granted_role.oid = membership.roleid
            JOIN pg_catalog.pg_roles member_role ON member_role.oid = membership.member
            WHERE granted_role.rolname IN (
                      'carebridge_checklist_retention_owner',
                      'carebridge_checklist_schema_owner',
                      'checklist_operations', 'carebridge_application')
               OR member_role.rolname IN (
                      'carebridge_checklist_retention_owner',
                      'carebridge_checklist_schema_owner',
                      'checklist_operations', 'carebridge_application')), ''),
        'functions=' || COALESCE((
            SELECT string_agg(
                routine.oid::regprocedure::text || ':' || owner_role.rolname || ':'
                || language_entry.lanname || ':' || routine.provolatile::text || ':'
                || routine.proparallel::text || ':' || routine.prosecdef::text || ':'
                || routine.proisstrict::text || ':' || routine.proleakproof::text || ':'
                || COALESCE(array_to_string(routine.proconfig, ','), '') || ':'
                || encode(sha256(convert_to(pg_get_functiondef(routine.oid), 'UTF8')), 'hex')
                || ':' || COALESCE(function_acl.acl_text, ''),
                ',' ORDER BY routine.oid::regprocedure::text)
            FROM pg_catalog.pg_proc routine
            JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = routine.proowner
            JOIN pg_catalog.pg_language language_entry ON language_entry.oid = routine.prolang
            LEFT JOIN function_acl ON function_acl.oid = routine.oid
            WHERE routine.oid IN (
                'public.carebridge_reject_mutation()'::regprocedure,
                'public.checklist_action_command_retention_guard()'::regprocedure,
                'public.checklist_quarantine_forensic_guard()'::regprocedure,
                'public.checklist_purge_retained_records(uuid)'::regprocedure)), ''),
        'tables=' || COALESCE((
            SELECT string_agg(
                protected.oid::regclass::text || ':' || owner_role.rolname || ':'
                || protected.relrowsecurity::text || ':'
                || protected.relforcerowsecurity::text || ':'
                || COALESCE(table_acl.acl_text, ''),
                ',' ORDER BY protected.oid::regclass::text)
            FROM pg_catalog.pg_class protected
            JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = protected.relowner
            LEFT JOIN table_acl ON table_acl.oid = protected.oid
            WHERE protected.oid IN (
                'public.audit_events'::regclass,
                'public.checklist_migration_quarantine'::regclass,
                'public.checklist_action_commands'::regclass)), ''),
        'policies=' || COALESCE((
            SELECT string_agg(
                policy.polname || ':' || policy.polcmd::text || ':'
                || policy.polpermissive::text || ':'
                || (SELECT string_agg(
                        COALESCE(role_entry.rolname,
                                 CASE WHEN role_oid = 0 THEN 'PUBLIC' END,
                                 role_oid::text), ',' ORDER BY
                        COALESCE(role_entry.rolname,
                                 CASE WHEN role_oid = 0 THEN 'PUBLIC' END,
                                 role_oid::text))
                    FROM unnest(policy.polroles) role_oid
                    LEFT JOIN pg_catalog.pg_roles role_entry ON role_entry.oid = role_oid)
                || ':' || regexp_replace(lower(COALESCE(
                    pg_get_expr(policy.polqual, policy.polrelid), 'NULL')),
                    '[[:space:]()]', '', 'g')
                || ':' || regexp_replace(lower(COALESCE(
                    pg_get_expr(policy.polwithcheck, policy.polrelid), 'NULL')),
                    '[[:space:]()]', '', 'g'),
                ',' ORDER BY policy.polname)
            FROM pg_catalog.pg_policy policy
            WHERE policy.polrelid =
                'public.checklist_migration_quarantine'::regclass), ''),
        'triggers=' || COALESCE((
            SELECT string_agg(
                trigger_entry.tgname || ':' || trigger_entry.tgrelid::regclass::text
                || ':' || trigger_entry.tgfoid::regprocedure::text
                || ':' || trigger_entry.tgtype::text || ':'
                || trigger_entry.tgenabled::text || ':' || trigger_entry.tgisinternal::text
                || ':' || pg_get_triggerdef(trigger_entry.oid, false)
                || ':' || COALESCE(pg_get_expr(
                    trigger_entry.tgqual, trigger_entry.tgrelid), 'NULL')
                || ':' || trigger_entry.tgattr::text
                || ':' || encode(trigger_entry.tgargs, 'hex'),
                ',' ORDER BY trigger_entry.tgname, trigger_entry.tgrelid::regclass::text)
            FROM pg_catalog.pg_trigger trigger_entry
            WHERE trigger_entry.tgrelid IN (
                'public.audit_events'::regclass,
                'public.checklist_migration_quarantine'::regclass,
                'public.checklist_action_commands'::regclass)
              AND trigger_entry.tgisinternal = false), '')
    ) INTO actual_fingerprint;

    actual_digest := encode(sha256(convert_to(actual_fingerprint, 'UTF8')), 'hex');
    IF actual_digest <> '8360624e83fc7a2ee3dcb1ba77159085ee5965b038ef2d4138ea2db93e627cd7' THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_CATALOG_FINGERPRINT_MISMATCH'
            USING DETAIL = actual_digest;
    END IF;
    RETURN 'VERIFIED:20260729150001';
END
$retention_verifier$;

ALTER FUNCTION public.checklist_assert_retention_security()
    OWNER TO carebridge_checklist_schema_owner;
REVOKE ALL ON FUNCTION public.checklist_assert_retention_security()
    FROM PUBLIC, carebridge_application, checklist_operations;
GRANT EXECUTE ON FUNCTION public.checklist_assert_retention_security()
    TO checklist_operations;

REVOKE CREATE ON SCHEMA public FROM carebridge_checklist_schema_owner;
REVOKE CREATE ON SCHEMA public FROM carebridge_checklist_retention_owner;

-- Normalize table ACLs. Runtime may read/write but never delete retained rows;
-- operations receives no raw table privilege; retention owner receives only the
-- privileges required by the audited purge function.
REVOKE ALL ON public.audit_events,
              public.checklist_migration_quarantine,
              public.checklist_action_commands
    FROM PUBLIC, carebridge_application, checklist_operations,
         carebridge_checklist_retention_owner;
GRANT SELECT, INSERT ON public.audit_events TO carebridge_application;
GRANT SELECT, UPDATE ON public.checklist_migration_quarantine TO carebridge_application;
GRANT SELECT, INSERT, UPDATE ON public.checklist_action_commands TO carebridge_application;
GRANT SELECT, INSERT, DELETE ON public.audit_events
    TO carebridge_checklist_retention_owner;
GRANT SELECT, DELETE ON public.checklist_migration_quarantine,
                        public.checklist_action_commands
    TO carebridge_checklist_retention_owner;
GRANT SELECT ON public.users, public.care_tasks, public.checklist_task_instances
    TO carebridge_checklist_retention_owner;

-- Normalize function ACL to owner + dedicated operations only.
REVOKE ALL ON FUNCTION public.checklist_purge_retained_records(uuid)
    FROM PUBLIC, carebridge_application, checklist_operations;
DO $$
DECLARE
    grantee_name name;
BEGIN
    FOR grantee_name IN
        SELECT DISTINCT role_entry.rolname
        FROM pg_catalog.pg_proc routine
        CROSS JOIN LATERAL aclexplode(
            COALESCE(routine.proacl, acldefault('f', routine.proowner))) acl
        JOIN pg_catalog.pg_roles role_entry ON role_entry.oid = acl.grantee
        WHERE routine.oid = to_regprocedure(
                  'public.checklist_purge_retained_records(uuid)')
          AND role_entry.rolname NOT IN (
                  'carebridge_checklist_retention_owner', 'checklist_operations')
    LOOP
        EXECUTE format(
            'REVOKE ALL ON FUNCTION public.checklist_purge_retained_records(uuid) FROM %I',
            grantee_name);
    END LOOP;
END $$;

-- Grant runtime execution last, after all ownership/ACL mutations.
GRANT USAGE ON SCHEMA public TO checklist_operations, carebridge_application;
GRANT EXECUTE ON FUNCTION public.checklist_purge_retained_records(uuid)
    TO checklist_operations;

-- Exact postconditions. Any mismatch aborts and rolls back every mutation above.
DO $$
DECLARE
    retention_owner_oid oid := (
        SELECT oid FROM pg_catalog.pg_roles
        WHERE rolname = 'carebridge_checklist_retention_owner');
    schema_owner_oid oid := (
        SELECT oid FROM pg_catalog.pg_roles
        WHERE rolname = 'carebridge_checklist_schema_owner');
    operations_oid oid := (
        SELECT oid FROM pg_catalog.pg_roles
        WHERE rolname = 'checklist_operations');
    application_oid oid := (
        SELECT oid FROM pg_catalog.pg_roles
        WHERE rolname = 'carebridge_application');
    purge_oid oid := to_regprocedure(
        'public.checklist_purge_retained_records(uuid)');
    verifier_oid oid := to_regprocedure(
        'public.checklist_assert_retention_security()');
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_catalog.pg_auth_members membership
        WHERE membership.roleid IN (
                  retention_owner_oid, schema_owner_oid, operations_oid, application_oid)
           OR membership.member IN (
                  retention_owner_oid, schema_owner_oid, operations_oid, application_oid)
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_ROLE_MEMBERSHIPS_MUST_BE_EMPTY';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_proc routine
        WHERE routine.oid = purge_oid
          AND routine.proowner = retention_owner_oid
          AND routine.prosecdef = true
          AND routine.proconfig = ARRAY['search_path=pg_catalog, public']::text[]
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_PURGE_FUNCTION_INVARIANT_FAILED';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_catalog.pg_database database_entry
        WHERE database_entry.datname = current_database()
          AND database_entry.datdba = schema_owner_oid
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_DATABASE_OWNER_INVALID';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_catalog.pg_namespace namespace
        WHERE namespace.nspname = 'public'
          AND namespace.nspowner = to_regrole('pg_database_owner')
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_PUBLIC_SCHEMA_OWNER_INVALID';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_catalog.pg_proc routine
        JOIN pg_catalog.pg_language language_entry ON language_entry.oid = routine.prolang
        WHERE routine.oid = verifier_oid
          AND routine.proowner = schema_owner_oid
          AND language_entry.lanname = 'plpgsql'
          AND routine.prokind = 'f'
          AND routine.provolatile = 'v'
          AND routine.proparallel = 'u'
          AND routine.prosecdef = true
          AND routine.proisstrict = false
          AND routine.proleakproof = false
          AND routine.proconfig = ARRAY['search_path=pg_catalog, public']::text[]
          AND encode(sha256(convert_to(
              pg_get_functiondef(routine.oid), 'UTF8')), 'hex') =
              '82acbc14c2af861dcfc543597ef58937f77b2bd26d5b1c4cd36730816f949cf1'
    ) OR EXISTS (
        SELECT 1
        FROM pg_catalog.pg_proc routine
        CROSS JOIN LATERAL aclexplode(COALESCE(
            routine.proacl, acldefault('f', routine.proowner))) acl
        WHERE routine.oid = verifier_oid
          AND (acl.grantee NOT IN (schema_owner_oid, operations_oid)
               OR acl.privilege_type <> 'EXECUTE'
               OR acl.is_grantable = true)
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_VERIFIER_FUNCTION_INVARIANT_FAILED'
            USING DETAIL = COALESCE((
                SELECT encode(sha256(convert_to(
                    pg_get_functiondef(routine.oid), 'UTF8')), 'hex')
                FROM pg_catalog.pg_proc routine
                WHERE routine.oid = verifier_oid), 'missing');
    END IF;

    IF (SELECT count(*)
        FROM (VALUES
            ('public.checklist_quarantine_forensic_guard()'::regprocedure),
            ('public.checklist_action_command_retention_guard()'::regprocedure),
            ('public.carebridge_reject_mutation()'::regprocedure)
        ) expected(function_oid)
        JOIN pg_catalog.pg_proc routine ON routine.oid = expected.function_oid
        WHERE routine.proowner = schema_owner_oid
          AND routine.pronamespace = 'public'::regnamespace
          AND routine.pronargs = 0
          AND routine.prorettype = 'pg_catalog.trigger'::regtype
          AND routine.prokind = 'f') <> 3 THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_GUARD_OWNER_INVARIANT_FAILED';
    END IF;

    IF EXISTS (
        SELECT 1 FROM pg_catalog.pg_class protected
        WHERE protected.oid IN (
            'public.audit_events'::regclass,
            'public.checklist_migration_quarantine'::regclass,
            'public.checklist_action_commands'::regclass)
          AND protected.relowner <> schema_owner_oid
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_TABLE_OWNER_INVARIANT_FAILED';
    END IF;

    IF has_schema_privilege(
           'carebridge_checklist_retention_owner', 'public', 'CREATE')
       OR NOT has_schema_privilege(
           'carebridge_checklist_schema_owner', 'public', 'CREATE') THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_OWNER_CREATE_MUST_BE_REVOKED';
    END IF;

    IF has_table_privilege('carebridge_application', 'public.audit_events', 'DELETE')
       OR has_table_privilege('carebridge_application',
           'public.checklist_migration_quarantine', 'DELETE')
       OR has_table_privilege('carebridge_application',
           'public.checklist_action_commands', 'DELETE')
       OR has_table_privilege('checklist_operations', 'public.audit_events', 'DELETE')
       OR has_table_privilege('checklist_operations',
           'public.checklist_migration_quarantine', 'DELETE')
       OR has_table_privilege('checklist_operations',
           'public.checklist_action_commands', 'DELETE') THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_RAW_DELETE_ACL_INVARIANT_FAILED';
    END IF;

    IF EXISTS (
        WITH expected_acl(table_oid, grantee, privilege_type) AS (
            VALUES
                ('public.audit_events'::regclass, application_oid, 'SELECT'),
                ('public.audit_events'::regclass, application_oid, 'INSERT'),
                ('public.checklist_migration_quarantine'::regclass,
                 application_oid, 'SELECT'),
                ('public.checklist_migration_quarantine'::regclass,
                 application_oid, 'UPDATE'),
                ('public.checklist_action_commands'::regclass,
                 application_oid, 'SELECT'),
                ('public.checklist_action_commands'::regclass,
                 application_oid, 'INSERT'),
                ('public.checklist_action_commands'::regclass,
                 application_oid, 'UPDATE'),
                ('public.audit_events'::regclass, retention_owner_oid, 'SELECT'),
                ('public.audit_events'::regclass, retention_owner_oid, 'INSERT'),
                ('public.audit_events'::regclass, retention_owner_oid, 'DELETE'),
                ('public.checklist_migration_quarantine'::regclass,
                 retention_owner_oid, 'SELECT'),
                ('public.checklist_migration_quarantine'::regclass,
                 retention_owner_oid, 'DELETE'),
                ('public.checklist_action_commands'::regclass,
                 retention_owner_oid, 'SELECT'),
                ('public.checklist_action_commands'::regclass,
                 retention_owner_oid, 'DELETE')
        )
        SELECT 1
        FROM expected_acl expected
        WHERE NOT EXISTS (
            SELECT 1
            FROM pg_catalog.pg_class protected
            CROSS JOIN LATERAL aclexplode(COALESCE(
                protected.relacl, acldefault('r', protected.relowner))) acl
            WHERE protected.oid = expected.table_oid
              AND acl.grantee = expected.grantee
              AND acl.privilege_type = expected.privilege_type)
        UNION ALL
        SELECT 1
        FROM pg_catalog.pg_class protected
        CROSS JOIN LATERAL aclexplode(COALESCE(
            protected.relacl, acldefault('r', protected.relowner))) acl
        WHERE protected.oid IN (
                  'public.audit_events'::regclass,
                  'public.checklist_migration_quarantine'::regclass,
                  'public.checklist_action_commands'::regclass)
          AND acl.grantee <> protected.relowner
          AND NOT EXISTS (
              SELECT 1
              FROM expected_acl expected
              WHERE expected.table_oid = protected.oid
                AND expected.grantee = acl.grantee
                AND expected.privilege_type = acl.privilege_type)
        UNION ALL
        SELECT 1
        FROM pg_catalog.pg_namespace namespace
        CROSS JOIN LATERAL aclexplode(COALESCE(
            namespace.nspacl, acldefault('n', namespace.nspowner))) acl
        WHERE namespace.oid = 'public'::regnamespace
          AND acl.privilege_type = 'CREATE'
          AND acl.grantee NOT IN (
              namespace.nspowner,
              to_regrole('pg_database_owner'))
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_OBJECT_ACL_INVARIANT_FAILED';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_class quarantine
        WHERE quarantine.oid = 'public.checklist_migration_quarantine'::regclass
          AND quarantine.relrowsecurity = true
          AND quarantine.relforcerowsecurity = true
    ) OR (SELECT count(*) FROM pg_catalog.pg_policy policy
          WHERE policy.polrelid = 'public.checklist_migration_quarantine'::regclass) <> 7
      OR (SELECT count(*)
          FROM pg_catalog.pg_policy policy
          JOIN (VALUES
              ('checklist_migration_quarantine_insert_policy',
               'a', ARRAY[0]::oid[], NULL::text, 'true'),
              ('checklist_migration_quarantine_operations_select',
               'r', ARRAY[operations_oid]::oid[], 'true', NULL::text),
              ('checklist_migration_quarantine_operations_update',
               'w', ARRAY[operations_oid]::oid[],
               'resolved_atisnull',
               'resolved_atisnotnullandresolved_byisnotnullandresolution_codeisnotnull'),
              ('checklist_migration_quarantine_application_select',
               'r', ARRAY[application_oid]::oid[], 'true', NULL::text),
              ('checklist_migration_quarantine_application_resolve',
               'w', ARRAY[application_oid]::oid[],
               'resolved_atisnull',
               'resolved_atisnotnullandresolved_byisnotnullandresolution_codeisnotnull'),
              ('checklist_migration_quarantine_retention_owner_delete',
               'd', ARRAY[retention_owner_oid]::oid[],
               'legal_hold=falseandcreated_at<clock_timestamp-''7years''::intervalandretain_until<=clock_timestamp',
               NULL::text),
              ('checklist_migration_quarantine_retention_owner_select',
               'r', ARRAY[retention_owner_oid]::oid[],
               'legal_hold=falseandcreated_at<clock_timestamp-''7years''::intervalandretain_until<=clock_timestamp',
               NULL::text)
          ) expected(policy_name, command, role_oids, qual, with_check)
            ON expected.policy_name = policy.polname
          WHERE policy.polrelid = 'public.checklist_migration_quarantine'::regclass
            AND policy.polcmd::text = expected.command
            AND policy.polroles = expected.role_oids
            AND policy.polpermissive = true
            AND regexp_replace(lower(pg_get_expr(policy.polqual, policy.polrelid)),
                '[[:space:]()]', '', 'g') IS NOT DISTINCT FROM expected.qual
            AND regexp_replace(lower(pg_get_expr(policy.polwithcheck, policy.polrelid)),
                '[[:space:]()]', '', 'g') IS NOT DISTINCT FROM expected.with_check) <> 7 THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_RLS_INVARIANT_FAILED'
            USING DETAIL = (
                SELECT jsonb_pretty(jsonb_agg(jsonb_build_object(
                    'name', policy.polname,
                    'command', policy.polcmd,
                    'roleOids', policy.polroles,
                    'roleNames', (
                        SELECT array_agg(COALESCE(role_entry.rolname, role_oid::text))
                        FROM unnest(policy.polroles) role_oid
                        LEFT JOIN pg_catalog.pg_roles role_entry
                          ON role_entry.oid = role_oid),
                    'qualNormalized', regexp_replace(lower(COALESCE(
                        pg_get_expr(policy.polqual, policy.polrelid), 'NULL')),
                        '[[:space:]()]', '', 'g'),
                    'withCheckNormalized', regexp_replace(lower(COALESCE(
                        pg_get_expr(policy.polwithcheck, policy.polrelid), 'NULL')),
                        '[[:space:]()]', '', 'g'))
                    ORDER BY policy.polname))::text
                FROM pg_catalog.pg_policy policy
                WHERE policy.polrelid =
                    'public.checklist_migration_quarantine'::regclass);
    END IF;

    IF (SELECT count(*)
        FROM (VALUES
            ('audit_events_immutable_trg',
             'public.audit_events'::regclass,
             'public.carebridge_reject_mutation()'::regprocedure,
             27::smallint,
             'CREATE TRIGGER audit_events_immutable_trg BEFORE DELETE OR UPDATE ON public.audit_events FOR EACH ROW EXECUTE FUNCTION carebridge_reject_mutation()'),
            ('checklist_quarantine_forensic_guard_trg',
             'public.checklist_migration_quarantine'::regclass,
             'public.checklist_quarantine_forensic_guard()'::regprocedure,
             27::smallint,
             'CREATE TRIGGER checklist_quarantine_forensic_guard_trg BEFORE DELETE OR UPDATE ON public.checklist_migration_quarantine FOR EACH ROW EXECUTE FUNCTION checklist_quarantine_forensic_guard()'),
            ('checklist_action_command_retention_guard_trg',
             'public.checklist_action_commands'::regclass,
             'public.checklist_action_command_retention_guard()'::regprocedure,
             11::smallint,
             'CREATE TRIGGER checklist_action_command_retention_guard_trg BEFORE DELETE ON public.checklist_action_commands FOR EACH ROW EXECUTE FUNCTION checklist_action_command_retention_guard()'),
            ('checklist_validate_action_command_target_trg',
             'public.checklist_action_commands'::regclass,
             'public.checklist_validate_action_command_target()'::regprocedure,
             7::smallint,
             'CREATE TRIGGER checklist_validate_action_command_target_trg BEFORE INSERT ON public.checklist_action_commands FOR EACH ROW EXECUTE FUNCTION checklist_validate_action_command_target()')
        ) expected(trigger_name, table_oid, function_oid, trigger_type,
                   trigger_definition)
        JOIN pg_catalog.pg_trigger trigger_entry
          ON trigger_entry.tgname = expected.trigger_name
         AND trigger_entry.tgrelid = expected.table_oid
         AND trigger_entry.tgfoid = expected.function_oid
         AND trigger_entry.tgtype = expected.trigger_type
        WHERE trigger_entry.tgenabled = 'O'
          AND trigger_entry.tgisinternal = false
          AND pg_get_triggerdef(trigger_entry.oid, false) = expected.trigger_definition
          AND trigger_entry.tgqual IS NULL
          AND trigger_entry.tgattr = ''::int2vector
          AND octet_length(trigger_entry.tgargs) = 0) <> 4
       OR EXISTS (
           SELECT 1
           FROM pg_catalog.pg_trigger trigger_entry
           WHERE trigger_entry.tgrelid IN (
                     'public.audit_events'::regclass,
                     'public.checklist_migration_quarantine'::regclass,
                     'public.checklist_action_commands'::regclass)
             AND trigger_entry.tgisinternal = false
             AND (trigger_entry.tgname, trigger_entry.tgrelid) NOT IN (
                 ('audit_events_immutable_trg',
                  'public.audit_events'::regclass),
                 ('checklist_quarantine_forensic_guard_trg',
                  'public.checklist_migration_quarantine'::regclass),
                 ('checklist_action_command_retention_guard_trg',
                  'public.checklist_action_commands'::regclass),
                 ('checklist_validate_action_command_target_trg',
                  'public.checklist_action_commands'::regclass))) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_TRIGGER_INVARIANT_FAILED';
    END IF;

    IF NOT has_function_privilege('checklist_operations', purge_oid, 'EXECUTE')
       OR has_function_privilege('carebridge_application', purge_oid, 'EXECUTE')
       OR EXISTS (
           SELECT 1
           FROM pg_catalog.pg_proc routine
           CROSS JOIN LATERAL aclexplode(
               COALESCE(routine.proacl, acldefault('f', routine.proowner))) acl
           WHERE routine.oid = purge_oid
             AND (acl.grantee NOT IN (retention_owner_oid, operations_oid)
                  OR acl.privilege_type <> 'EXECUTE')) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_FUNCTION_ACL_INVARIANT_FAILED';
    END IF;

    IF public.checklist_assert_retention_security()
           <> 'VERIFIED:20260729150001' THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_VERIFIER_RESULT_INVALID';
    END IF;
END $$;

COMMIT;

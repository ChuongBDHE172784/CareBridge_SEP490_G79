package com.carebridge.backend.checklist.operations;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;

public final class ChecklistRetentionOwnerIsolationVerifier implements ApplicationRunner {

    private final JdbcTemplate dedicatedJdbcTemplate;

    ChecklistRetentionOwnerIsolationVerifier(JdbcTemplate dedicatedJdbcTemplate) {
        this.dedicatedJdbcTemplate = dedicatedJdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        Boolean isolated = dedicatedJdbcTemplate.queryForObject(
                """
                WITH expected_roles(role_name, can_login) AS (
                    VALUES
                        ('carebridge_checklist_retention_owner', false),
                        ('carebridge_checklist_schema_owner', false),
                        ('checklist_operations', true),
                        ('carebridge_application', true)
                ), secure_roles AS (
                    SELECT role_entry.oid, role_entry.rolname
                    FROM pg_catalog.pg_roles role_entry
                    JOIN expected_roles expected ON expected.role_name = role_entry.rolname
                    WHERE role_entry.rolcanlogin = expected.can_login
                      AND role_entry.rolsuper = false
                      AND role_entry.rolcreatedb = false
                      AND role_entry.rolcreaterole = false
                      AND role_entry.rolinherit = false
                      AND role_entry.rolreplication = false
                      AND role_entry.rolbypassrls = false
                ), expected_table_acl(table_oid, grantee, privilege_type) AS (
                    SELECT expected.table_name::regclass, role_entry.oid,
                           expected.privilege_type
                    FROM (VALUES
                        ('public.audit_events', 'carebridge_application', 'SELECT'),
                        ('public.audit_events', 'carebridge_application', 'INSERT'),
                        ('public.checklist_action_commands',
                         'carebridge_application', 'SELECT'),
                        ('public.checklist_action_commands',
                         'carebridge_application', 'INSERT'),
                        ('public.checklist_action_commands',
                         'carebridge_application', 'UPDATE'),
                        ('public.audit_events',
                         'carebridge_checklist_retention_owner', 'SELECT'),
                        ('public.audit_events',
                         'carebridge_checklist_retention_owner', 'INSERT'),
                        ('public.audit_events',
                         'carebridge_checklist_retention_owner', 'DELETE'),
                        ('public.checklist_action_commands',
                         'carebridge_checklist_retention_owner', 'SELECT'),
                        ('public.checklist_action_commands',
                         'carebridge_checklist_retention_owner', 'DELETE')
                    ) expected(table_name, role_name, privilege_type)
                    JOIN secure_roles role_entry ON role_entry.rolname = expected.role_name
                ), expected_triggers(
                    trigger_name, table_oid, function_oid, trigger_type, trigger_definition
                ) AS (
                    VALUES
                        ('audit_events_immutable_trg',
                         'public.audit_events'::regclass,
                         'public.carebridge_reject_mutation()'::regprocedure,
                         27::smallint,
                         'CREATE TRIGGER audit_events_immutable_trg BEFORE DELETE OR UPDATE ON public.audit_events FOR EACH ROW EXECUTE FUNCTION carebridge_reject_mutation()'),
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
                ), purge_function AS (
                    SELECT routine.oid, routine.proowner, routine.proacl,
                           routine.prosecdef, routine.proconfig,
                           pg_get_functiondef(routine.oid) AS definition
                    FROM pg_catalog.pg_proc routine
                    WHERE routine.oid = to_regprocedure(
                        'public.checklist_purge_retained_records(uuid)')
                )
                SELECT session_user = 'checklist_operations'
                   AND current_user = 'checklist_operations'
                   AND (SELECT count(*) FROM secure_roles) = 4
                   AND to_regclass('public.checklist_migration_quarantine') IS NULL
                   AND NOT EXISTS (
                       SELECT 1 FROM pg_catalog.pg_auth_members membership
                       WHERE (membership.roleid IN (SELECT oid FROM secure_roles)
                          OR membership.member IN (SELECT oid FROM secure_roles))
                         AND (membership.inherit_option OR membership.set_option))
                   AND EXISTS (
                       SELECT 1 FROM purge_function
                       WHERE purge_function.proowner = (
                                 SELECT oid FROM secure_roles
                                 WHERE rolname = 'carebridge_checklist_retention_owner')
                         AND purge_function.prosecdef = true
                         AND purge_function.proconfig =
                             ARRAY['search_path=pg_catalog, public']::text[]
                         AND purge_function.definition LIKE
                             '%CHECKLIST_RETIREMENT_ACTION_LEDGER_ONLY_V1%'
                         AND purge_function.definition NOT LIKE
                             '%DELETE FROM public.checklist_migration_quarantine%')
                   AND EXISTS (
                       SELECT 1 FROM pg_catalog.pg_database database_entry
                       WHERE database_entry.datname = current_database()
                         AND database_entry.datdba = (
                             SELECT oid FROM secure_roles
                             WHERE rolname = 'carebridge_checklist_schema_owner'))
                   AND EXISTS (
                       SELECT 1 FROM pg_catalog.pg_namespace namespace
                       WHERE namespace.nspname = 'public'
                         AND namespace.nspowner = to_regrole('pg_database_owner'))
                   AND (SELECT count(*)
                        FROM pg_catalog.pg_class protected
                        WHERE protected.oid IN (
                            'public.audit_events'::regclass,
                            'public.checklist_action_commands'::regclass)
                          AND protected.relowner = (
                              SELECT oid FROM secure_roles
                              WHERE rolname = 'carebridge_checklist_schema_owner')) = 2
                   AND (SELECT count(*)
                        FROM pg_catalog.pg_proc routine
                        WHERE routine.proname IN (
                            'checklist_action_command_retention_guard',
                            'carebridge_reject_mutation')
                          AND routine.proowner = (
                              SELECT oid FROM secure_roles
                              WHERE rolname = 'carebridge_checklist_schema_owner')) = 2
                   AND NOT has_schema_privilege(
                       'carebridge_checklist_retention_owner', 'public', 'CREATE')
                   AND NOT has_table_privilege(
                       'carebridge_application', 'public.audit_events', 'DELETE')
                   AND NOT has_table_privilege(
                       'carebridge_application',
                       'public.checklist_action_commands', 'DELETE')
                   AND NOT has_table_privilege(
                       'checklist_operations', 'public.audit_events', 'DELETE')
                   AND NOT has_table_privilege(
                       'checklist_operations',
                       'public.checklist_action_commands', 'DELETE')
                   AND NOT EXISTS (
                       SELECT 1 FROM expected_table_acl expected
                       WHERE NOT EXISTS (
                           SELECT 1
                           FROM pg_catalog.pg_class protected
                           CROSS JOIN LATERAL aclexplode(COALESCE(
                               protected.relacl,
                               acldefault('r', protected.relowner))) acl
                           WHERE protected.oid = expected.table_oid
                             AND acl.grantee = expected.grantee
                             AND acl.privilege_type = expected.privilege_type))
                   AND NOT EXISTS (
                       SELECT 1
                       FROM pg_catalog.pg_class protected
                       CROSS JOIN LATERAL aclexplode(COALESCE(
                           protected.relacl,
                           acldefault('r', protected.relowner))) acl
                       WHERE protected.oid IN (
                           'public.audit_events'::regclass,
                           'public.checklist_action_commands'::regclass)
                         AND acl.grantee <> protected.relowner
                         AND NOT EXISTS (
                             SELECT 1 FROM expected_table_acl expected
                             WHERE expected.table_oid = protected.oid
                               AND expected.grantee = acl.grantee
                               AND expected.privilege_type = acl.privilege_type))
                   AND (SELECT count(*)
                        FROM expected_triggers expected
                        JOIN pg_catalog.pg_trigger trigger_entry
                          ON trigger_entry.tgname = expected.trigger_name
                         AND trigger_entry.tgrelid = expected.table_oid
                         AND trigger_entry.tgfoid = expected.function_oid
                         AND trigger_entry.tgtype = expected.trigger_type
                        WHERE trigger_entry.tgenabled = 'O'
                          AND trigger_entry.tgisinternal = false
                          AND pg_get_triggerdef(trigger_entry.oid, false) =
                              expected.trigger_definition
                          AND trigger_entry.tgqual IS NULL
                          AND trigger_entry.tgattr = ''::int2vector
                          AND octet_length(trigger_entry.tgargs) = 0) = 3
                   AND NOT EXISTS (
                       SELECT 1 FROM pg_catalog.pg_trigger trigger_entry
                       WHERE trigger_entry.tgrelid IN (
                                 'public.audit_events'::regclass,
                                 'public.checklist_action_commands'::regclass)
                         AND trigger_entry.tgisinternal = false
                         AND NOT EXISTS (
                             SELECT 1 FROM expected_triggers expected
                             WHERE expected.trigger_name = trigger_entry.tgname
                               AND expected.table_oid = trigger_entry.tgrelid))
                   AND has_function_privilege(
                       'checklist_operations',
                       to_regprocedure('public.checklist_purge_retained_records(uuid)'),
                       'EXECUTE')
                   AND NOT has_function_privilege(
                       'carebridge_application',
                       to_regprocedure('public.checklist_purge_retained_records(uuid)'),
                       'EXECUTE')
                   AND NOT EXISTS (
                       SELECT 1
                       FROM purge_function
                       CROSS JOIN LATERAL aclexplode(COALESCE(
                           purge_function.proacl,
                           acldefault('f', purge_function.proowner))) acl
                       WHERE acl.privilege_type <> 'EXECUTE'
                          OR acl.grantee NOT IN (
                              SELECT oid FROM secure_roles
                              WHERE rolname IN (
                                  'carebridge_checklist_retention_owner',
                                  'checklist_operations')))
                """,
                Boolean.class);
        if (!Boolean.TRUE.equals(isolated)) {
            throw new IllegalStateException("CHECKLIST_RETENTION_OWNER_ROLE_REACHABLE");
        }
    }
}

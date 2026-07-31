\set ON_ERROR_STOP on
-- Privileged post-Flyway attestation and action-ledger ownership restoration.
BEGIN;

DO $$
DECLARE
    retired_present text;
    purge_definition text;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = current_user AND rolsuper) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_POST_FINALIZER_REQUIRES_SUPERUSER';
    END IF;
    SELECT string_agg(retired.name, ', ' ORDER BY retired.name)
      INTO retired_present
      FROM (VALUES
          ('checklist_reconciliation_candidates'), ('checklist_reconciliation_runs'),
          ('checklist_distribution_outbox'), ('checklist_migration_quarantine'),
          ('checklist_care_group_contexts'), ('checklist_context_authorities'),
          ('checklist_template_version_items'), ('checklist_template_recipient_roles'),
          ('checklist_substages')) AS retired(name)
     WHERE to_regclass('public.' || retired.name) IS NOT NULL;
    IF retired_present IS NOT NULL THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_TABLE_STILL_PRESENT:%', retired_present;
    END IF;
    IF to_regclass('public.checklist_instances') IS NULL
       OR to_regclass('public.checklist_task_instances') IS NULL
       OR to_regclass('public.checklist_action_commands') IS NULL THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_CORE_TABLE_MISSING';
    END IF;
    SELECT pg_get_functiondef('public.checklist_purge_retained_records(uuid)'::regprocedure)
      INTO purge_definition;
    IF purge_definition NOT LIKE '%CHECKLIST_RETIREMENT_ACTION_LEDGER_ONLY_V1%'
       OR purge_definition LIKE '%DELETE FROM public.checklist_migration_quarantine%' THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_PURGE_BODY_INVALID';
    END IF;
    IF (SELECT count(*)
          FROM (VALUES
              ('checklist_action_command_retention_guard_trg',
               'public.checklist_action_command_retention_guard()', 11::smallint),
              ('checklist_validate_action_command_target_trg',
               'public.checklist_validate_action_command_target()', 7::smallint)
          ) AS expected(trigger_name, function_signature, trigger_type)
          JOIN pg_catalog.pg_trigger trigger_entry
            ON trigger_entry.tgname = expected.trigger_name
           AND trigger_entry.tgrelid = 'public.checklist_action_commands'::regclass
           AND trigger_entry.tgfoid = to_regprocedure(expected.function_signature)
           AND trigger_entry.tgtype = expected.trigger_type
           AND trigger_entry.tgenabled = 'O'
           AND trigger_entry.tgisinternal = false
           AND trigger_entry.tgqual IS NULL) <> 2 THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_ACTION_LEDGER_GUARD_MISSING';
    END IF;
END $$;

ALTER FUNCTION public.checklist_purge_retained_records(uuid)
    OWNER TO carebridge_checklist_retention_owner;
REVOKE ALL ON FUNCTION public.checklist_purge_retained_records(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.checklist_purge_retained_records(uuid) TO checklist_operations;
REVOKE CREATE ON SCHEMA public FROM carebridge_checklist_retention_owner;

DO $$
BEGIN
    IF (SELECT owner_role.rolname
          FROM pg_catalog.pg_proc routine
          JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = routine.proowner
         WHERE routine.oid = 'public.checklist_purge_retained_records(uuid)'::regprocedure)
       <> 'carebridge_checklist_retention_owner' THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_PURGE_OWNER_INVALID';
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_catalog.pg_auth_members membership
        JOIN pg_catalog.pg_roles role ON role.oid = membership.roleid
        WHERE role.rolname = 'carebridge_checklist_retention_owner') THEN
        RAISE EXCEPTION 'PURGE_OWNER_ROLE_REACHABLE';
    END IF;
END $$;

COMMIT;
SELECT 'CHECKLIST_RETIREMENT_POST_FINALIZER_COMPLETE' AS status;

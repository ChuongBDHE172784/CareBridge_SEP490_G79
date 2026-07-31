\set ON_ERROR_STOP on
-- Privileged pre-Flyway ownership handoff. The runner must pass
-- -v flyway_role=<role> and -v operator_attestation=<exact token>.
BEGIN;
SELECT set_config('carebridge.retirement_flyway_role', :'flyway_role', false);
SELECT set_config('carebridge.retirement_operator_attestation', :'operator_attestation', false);

DO $$
DECLARE
    requested_role name := current_setting('carebridge.retirement_flyway_role');
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = requested_role) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_FLYWAY_ROLE_MISSING';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = current_user AND rolsuper) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_PRE_FINALIZER_REQUIRES_SUPERUSER';
    END IF;
    IF requested_role IN ('carebridge_application', 'checklist_operations',
            'carebridge_checklist_retention_owner', 'carebridge_checklist_schema_owner') THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_FLYWAY_ROLE_NOT_SEPARATE';
    END IF;
    IF current_setting('carebridge.retirement_operator_attestation') <>
       'REQUEST_100_PARITY_WRITERS_FROZEN_CATALOG_AND_LEDGER_CAPTURED_V1' THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_OPERATOR_ATTESTATION_REQUIRED';
    END IF;
    IF EXISTS (SELECT 1 FROM public.checklist_migration_quarantine WHERE legal_hold) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_LEGAL_HOLD_PRESENT';
    END IF;
END $$;

-- Superuser may replace the owner-protected routine without making the NOLOGIN
-- owner reachable. The signature stays stable; the retired quarantine count is zero.
CREATE OR REPLACE FUNCTION public.checklist_purge_retained_records(p_actor_user_id uuid)
RETURNS TABLE (
    audit_events_purged bigint,
    quarantines_purged bigint,
    action_commands_purged bigint)
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = pg_catalog, public
AS $body$
DECLARE
    retirement_marker constant text := 'CHECKLIST_RETIREMENT_ACTION_LEDGER_ONLY_V1';
BEGIN
    IF session_user <> 'checklist_operations' THEN
        RAISE EXCEPTION 'PURGE_DATABASE_CALLER_NOT_TRUSTED' USING ERRCODE = '42501';
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_catalog.pg_auth_members membership
        JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = membership.roleid
        WHERE owner_role.rolname = 'carebridge_checklist_retention_owner') THEN
        RAISE EXCEPTION 'PURGE_OWNER_ROLE_REACHABLE' USING ERRCODE = '42501';
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM public.users actor
        WHERE actor.user_id = p_actor_user_id
          AND actor.role IN ('SYSTEM_ADMIN', 'OPERATIONS')
          AND actor.account_status = 'ACTIVE' AND actor.enabled AND NOT actor.locked) THEN
        RAISE EXCEPTION 'PURGE_ACTOR_NOT_TRUSTED' USING ERRCODE = '42501';
    END IF;
    WITH deleted AS (
        DELETE FROM public.audit_events event
        WHERE event.event_category LIKE 'CHECKLIST\_%' ESCAPE '\'
          AND event.created_at < clock_timestamp() - interval '7 years'
          AND event.legal_hold = false RETURNING 1)
    SELECT count(*) INTO audit_events_purged FROM deleted;
    quarantines_purged := 0;
    WITH deleted AS (
        DELETE FROM public.checklist_action_commands command
        WHERE command.created_at < clock_timestamp() - interval '7 years'
          AND command.retain_until <= clock_timestamp()
          AND command.legal_hold = false
          AND (
              (command.task_kind = 'CHECKLIST' AND EXISTS (
                  SELECT 1 FROM public.checklist_task_instances task
                  WHERE task.checklist_task_instance_id = command.task_id
                    AND task.status IN ('COMPLETED','SKIPPED','CANCELLED')))
              OR (command.task_kind = 'CARE_TASK' AND EXISTS (
                  SELECT 1 FROM public.care_tasks task
                  WHERE task.task_id = command.task_id AND task.status IN ('DONE','CANCELLED')))
              OR (command.task_kind = 'REMINDER' AND EXISTS (
                  SELECT 1 FROM public.care_tasks task
                  WHERE task.task_id = command.reminder_definition_id
                    AND task.task_type = 'SCHEDULED_REMINDER'
                    AND task.status IN ('COMPLETED','SKIPPED','CANCELLED')))
              OR (command.task_kind = 'CHECKLIST' AND NOT EXISTS (
                  SELECT 1 FROM public.checklist_task_instances task
                  WHERE task.checklist_task_instance_id = command.task_id))
              OR (command.task_kind = 'CARE_TASK' AND NOT EXISTS (
                  SELECT 1 FROM public.care_tasks task WHERE task.task_id = command.task_id))
              OR (command.task_kind = 'REMINDER' AND NOT EXISTS (
                  SELECT 1 FROM public.care_tasks task
                  WHERE task.task_id = command.reminder_definition_id))) RETURNING 1)
    SELECT count(*) INTO action_commands_purged FROM deleted;
    INSERT INTO public.audit_events (
        actor_user_id, actor_type, event_category, resource_type, reason_code,
        correlation_id, after_payload_jsonb, occurred_at, created_at, event_origin, legal_hold)
    VALUES (
        p_actor_user_id, 'USER', 'CHECKLIST_RETENTION_PURGED', 'ChecklistRetention',
        'SEVEN_YEAR_RETENTION', gen_random_uuid(),
        jsonb_build_object('auditEventsPurged', audit_events_purged,
                           'quarantinesPurged', 0,
                           'actionCommandsPurged', action_commands_purged,
                           'retirementMarker', retirement_marker),
        clock_timestamp(), clock_timestamp(), 'AUDIT_LOG', false);
    RETURN QUERY SELECT audit_events_purged, quarantines_purged, action_commands_purged;
END
$body$;

REVOKE ALL ON FUNCTION public.checklist_purge_retained_records(uuid) FROM PUBLIC;

DO $$
BEGIN
    EXECUTE format('ALTER TABLE public.checklist_migration_quarantine OWNER TO %I',
        current_setting('carebridge.retirement_flyway_role'));
    EXECUTE format('ALTER FUNCTION public.checklist_quarantine_forensic_guard() OWNER TO %I',
        current_setting('carebridge.retirement_flyway_role'));
END $$;

COMMIT;
SELECT 'CHECKLIST_RETIREMENT_PRE_FINALIZER_COMPLETE' AS status;

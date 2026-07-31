-- Hosted-Supabase remediation for the roll-forward retirement guard.
--
-- The managed postgres role is not SUPERUSER, so the privileged finalizer cannot
-- run there. This script creates only the non-login role names and guard objects
-- that V20260731070000 requires. It grants no data privileges and no passwords.
-- Run through `supabase db query --linked --file ...` immediately before the
-- application starts with the supabase-roll-forward profile.

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'carebridge_application') THEN
        CREATE ROLE carebridge_application NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
            NOINHERIT NOREPLICATION NOBYPASSRLS;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'checklist_operations') THEN
        CREATE ROLE checklist_operations NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
            NOINHERIT NOREPLICATION NOBYPASSRLS;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'carebridge_checklist_retention_owner') THEN
        CREATE ROLE carebridge_checklist_retention_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
            NOINHERIT NOREPLICATION NOBYPASSRLS;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'carebridge_checklist_schema_owner') THEN
        CREATE ROLE carebridge_checklist_schema_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
            NOINHERIT NOREPLICATION NOBYPASSRLS;
    END IF;
END $$;

-- Canonical action-command retention guard from the clean migration chain.
CREATE OR REPLACE FUNCTION public.checklist_action_command_retention_guard()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF current_user = 'carebridge_checklist_retention_owner'
       AND OLD.legal_hold = false
       AND OLD.created_at < clock_timestamp() - interval '7 years'
       AND OLD.retain_until <= clock_timestamp()
       AND (
           (OLD.task_kind = 'CHECKLIST' AND EXISTS (
               SELECT 1 FROM public.checklist_task_instances task
               WHERE task.checklist_task_instance_id = OLD.task_id
                 AND task.status IN ('COMPLETED', 'SKIPPED', 'CANCELLED')))
           OR (OLD.task_kind = 'CARE_TASK' AND EXISTS (
               SELECT 1 FROM public.care_tasks task
               WHERE task.task_id = OLD.task_id
                 AND task.status IN ('DONE', 'CANCELLED')))
           OR (OLD.task_kind = 'REMINDER' AND EXISTS (
               SELECT 1 FROM public.care_tasks task
               WHERE task.task_id = OLD.reminder_definition_id
                 AND task.task_type = 'SCHEDULED_REMINDER'
                 AND task.status IN ('COMPLETED', 'SKIPPED', 'CANCELLED')))
           OR (OLD.task_kind = 'CHECKLIST' AND NOT EXISTS (
               SELECT 1 FROM public.checklist_task_instances task
               WHERE task.checklist_task_instance_id = OLD.task_id))
           OR (OLD.task_kind = 'CARE_TASK' AND NOT EXISTS (
               SELECT 1 FROM public.care_tasks task
               WHERE task.task_id = OLD.task_id))
           OR (OLD.task_kind = 'REMINDER' AND NOT EXISTS (
               SELECT 1 FROM public.care_tasks task
               WHERE task.task_id = OLD.reminder_definition_id))
       ) THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION
        'RETENTION_DELETE_NOT_AUTHORIZED: checklist action command is not eligible or caller is not the retention owner'
        USING ERRCODE = '42501';
END
$$;

DROP TRIGGER IF EXISTS checklist_action_command_retention_guard_trg
    ON public.checklist_action_commands;
CREATE TRIGGER checklist_action_command_retention_guard_trg
    BEFORE DELETE ON public.checklist_action_commands
    FOR EACH ROW EXECUTE FUNCTION public.checklist_action_command_retention_guard();

-- The canonical quarantine function already exists on this database; recreate
-- its expected trigger so the roll-forward can retire it deterministically.
DROP TRIGGER IF EXISTS checklist_quarantine_forensic_guard_trg
    ON public.checklist_migration_quarantine;
CREATE TRIGGER checklist_quarantine_forensic_guard_trg
    BEFORE UPDATE OR DELETE ON public.checklist_migration_quarantine
    FOR EACH ROW EXECUTE FUNCTION public.checklist_quarantine_forensic_guard();

-- Restore the canonical retention-owner policies so the roll-forward can drop
-- them by name without weakening the existing application policies.
DROP POLICY IF EXISTS checklist_migration_quarantine_retention_owner_delete
    ON public.checklist_migration_quarantine;
CREATE POLICY checklist_migration_quarantine_retention_owner_delete
    ON public.checklist_migration_quarantine FOR DELETE
    TO carebridge_checklist_retention_owner
    USING (legal_hold = false
           AND created_at < clock_timestamp() - interval '7 years'
           AND retain_until <= clock_timestamp());

DROP POLICY IF EXISTS checklist_migration_quarantine_retention_owner_select
    ON public.checklist_migration_quarantine;
CREATE POLICY checklist_migration_quarantine_retention_owner_select
    ON public.checklist_migration_quarantine FOR SELECT
    TO carebridge_checklist_retention_owner
    USING (legal_hold = false
           AND created_at < clock_timestamp() - interval '7 years'
           AND retain_until <= clock_timestamp());

COMMIT;

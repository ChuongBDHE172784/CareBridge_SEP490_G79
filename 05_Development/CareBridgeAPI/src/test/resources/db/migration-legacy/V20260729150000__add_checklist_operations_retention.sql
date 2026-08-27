-- CHK-039: operations-only quarantine surface and audited seven-year retention.
-- Application authorization is enforced at the controller boundary. Database
-- policies expose quarantine rows only to the migration/application role.

ALTER TABLE public.audit_events
    ADD COLUMN legal_hold boolean DEFAULT false NOT NULL;

CREATE INDEX audit_events_checklist_retention_ix
    ON public.audit_events(created_at)
    WHERE event_category LIKE 'CHECKLIST\_%' ESCAPE '\' AND legal_hold = false;

CREATE INDEX checklist_migration_quarantine_retention_ix
    ON public.checklist_migration_quarantine(created_at, retain_until)
    WHERE legal_hold = false;

CREATE INDEX checklist_action_commands_retention_ix
    ON public.checklist_action_commands(created_at, retain_until, task_kind, task_id)
    WHERE legal_hold = false;

-- Deployment must pre-provision both roles. Flyway deliberately has no
-- CREATEROLE dependency and never receives membership in the NOLOGIN owner.
-- A separate privileged finalizer transfers only the purge function after this
-- migration and grants checklist_operations last.
DO $$
DECLARE
    owner_role_oid oid;
BEGIN
    SELECT oid INTO owner_role_oid
    FROM pg_catalog.pg_roles
    WHERE rolname = 'carebridge_checklist_retention_owner'
      AND rolcanlogin = false
      AND rolsuper = false
      AND rolcreatedb = false
      AND rolcreaterole = false
      AND rolinherit = false
      AND rolreplication = false
      AND rolbypassrls = false;
    IF owner_role_oid IS NULL THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_OWNER_ROLE_REQUIRED'
            USING ERRCODE = '42501';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_roles
        WHERE rolname = 'checklist_operations'
          AND rolcanlogin = true
          AND rolsuper = false
          AND rolcreatedb = false
          AND rolcreaterole = false
          AND rolinherit = false
          AND rolreplication = false
          AND rolbypassrls = false
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_OPERATIONS_ROLE_REQUIRED'
            USING ERRCODE = '42501';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_roles
        WHERE rolname = 'carebridge_checklist_schema_owner'
          AND rolcanlogin = false
          AND rolsuper = false
          AND rolcreatedb = false
          AND rolcreaterole = false
          AND rolinherit = false
          AND rolreplication = false
          AND rolbypassrls = false
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_SCHEMA_OWNER_ROLE_REQUIRED'
            USING ERRCODE = '42501';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_roles
        WHERE rolname = 'carebridge_application'
          AND rolcanlogin = true
          AND rolsuper = false
          AND rolcreatedb = false
          AND rolcreaterole = false
          AND rolinherit = false
          AND rolreplication = false
          AND rolbypassrls = false
    ) THEN
        RAISE EXCEPTION 'CAREBRIDGE_APPLICATION_ROLE_REQUIRED'
            USING ERRCODE = '42501';
    END IF;

    IF current_user IN (
        'carebridge_application',
        'checklist_operations',
        'carebridge_checklist_retention_owner',
        'carebridge_checklist_schema_owner'
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_FLYWAY_ROLE_MUST_BE_SEPARATE'
            USING ERRCODE = '42501';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_catalog.pg_auth_members membership
        WHERE membership.roleid = owner_role_oid
           OR membership.member = owner_role_oid
    ) THEN
        RAISE EXCEPTION 'CHECKLIST_RETENTION_OWNER_ROLE_MEMBERSHIP_MUST_BE_EMPTY'
            USING ERRCODE = '42501';
    END IF;
END $$;

GRANT USAGE ON SCHEMA public TO carebridge_checklist_retention_owner;
GRANT SELECT ON public.users,
                public.care_tasks,
                public.checklist_task_instances
    TO carebridge_checklist_retention_owner;
GRANT SELECT, INSERT, DELETE ON public.audit_events
    TO carebridge_checklist_retention_owner;
GRANT SELECT, DELETE ON public.checklist_migration_quarantine,
                        public.checklist_action_commands
    TO carebridge_checklist_retention_owner;

-- Runtime DML is granted to the explicit application login, never to the
-- Flyway current_user. Retained records remain non-deletable at table ACL level.
REVOKE ALL ON public.audit_events,
              public.checklist_migration_quarantine,
              public.checklist_action_commands
    FROM carebridge_application;
GRANT SELECT, INSERT ON public.audit_events TO carebridge_application;
GRANT SELECT, UPDATE ON public.checklist_migration_quarantine TO carebridge_application;
GRANT SELECT, INSERT, UPDATE ON public.checklist_action_commands TO carebridge_application;

-- FORCE ROW LEVEL SECURITY was enabled when the quarantine table was created.
-- Application connections retain read/resolve access only. A dedicated
-- checklist_operations database login invokes the audited SECURITY DEFINER
-- function below; it never receives raw DELETE privileges.
CREATE POLICY checklist_migration_quarantine_application_select
    ON public.checklist_migration_quarantine FOR SELECT
    TO carebridge_application USING (true);
CREATE POLICY checklist_migration_quarantine_application_resolve
    ON public.checklist_migration_quarantine FOR UPDATE
    TO carebridge_application
    USING (resolved_at IS NULL)
    WITH CHECK (resolved_at IS NOT NULL AND resolved_by IS NOT NULL
                AND resolution_code IS NOT NULL);

CREATE POLICY checklist_migration_quarantine_retention_owner_delete
    ON public.checklist_migration_quarantine FOR DELETE
    TO carebridge_checklist_retention_owner
    USING (legal_hold = false
           AND created_at < clock_timestamp() - interval '7 years'
           AND retain_until <= clock_timestamp());

-- PostgreSQL evaluates SELECT visibility for DELETE predicates/RETURNING under
-- FORCE RLS. Keep that visibility owner-only and limited to the same eligible
-- rows; without it an authorized purge is silently filtered to zero rows.
CREATE POLICY checklist_migration_quarantine_retention_owner_select
    ON public.checklist_migration_quarantine FOR SELECT
    TO carebridge_checklist_retention_owner
    USING (legal_hold = false
           AND created_at < clock_timestamp() - interval '7 years'
           AND retain_until <= clock_timestamp());

REVOKE DELETE ON public.audit_events,
                 public.checklist_migration_quarantine,
                 public.checklist_action_commands
    FROM PUBLIC, carebridge_application, checklist_operations;

-- Forensic quarantine fields are immutable. Resolution is the only permitted
-- update and may change only resolved_at/resolved_by/resolution_code.
CREATE OR REPLACE FUNCTION public.checklist_quarantine_forensic_guard() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF current_user = 'carebridge_checklist_retention_owner'
           AND OLD.legal_hold = false
           AND OLD.created_at < clock_timestamp() - interval '7 years'
           AND OLD.retain_until <= clock_timestamp() THEN
            RETURN OLD;
        END IF;
        RAISE EXCEPTION
            'RETENTION_DELETE_NOT_AUTHORIZED: checklist quarantine is not eligible or caller is not the retention owner'
            USING ERRCODE = '42501';
    END IF;

    IF OLD.quarantine_id IS DISTINCT FROM NEW.quarantine_id
       OR OLD.source_table IS DISTINCT FROM NEW.source_table
       OR OLD.source_id IS DISTINCT FROM NEW.source_id
       OR OLD.reason_code IS DISTINCT FROM NEW.reason_code
       OR OLD.payload_ciphertext IS DISTINCT FROM NEW.payload_ciphertext
       OR OLD.payload_hash IS DISTINCT FROM NEW.payload_hash
       OR OLD.encryption_key_version IS DISTINCT FROM NEW.encryption_key_version
       OR OLD.correlation_id IS DISTINCT FROM NEW.correlation_id
       OR OLD.legal_hold IS DISTINCT FROM NEW.legal_hold
       OR OLD.retain_until IS DISTINCT FROM NEW.retain_until
       OR OLD.created_at IS DISTINCT FROM NEW.created_at
       OR OLD.care_context_type IS DISTINCT FROM NEW.care_context_type
       OR OLD.care_context_id IS DISTINCT FROM NEW.care_context_id THEN
        RAISE EXCEPTION 'IMMUTABLE_FORENSIC_FIELDS: checklist quarantine metadata is append-only';
    END IF;
    RETURN NEW;
END
$$;

REVOKE ALL ON FUNCTION public.checklist_quarantine_forensic_guard() FROM PUBLIC;

DROP TRIGGER IF EXISTS checklist_quarantine_forensic_guard_trg
    ON public.checklist_migration_quarantine;
CREATE TRIGGER checklist_quarantine_forensic_guard_trg
    BEFORE UPDATE OR DELETE ON public.checklist_migration_quarantine
    FOR EACH ROW EXECUTE FUNCTION public.checklist_quarantine_forensic_guard();

-- Action-command rows are also forensic/idempotency records. A raw DELETE must
-- be impossible for the shared application role; the owner-only purge path is
-- allowed only for expired, non-held rows whose task is terminal or orphaned.
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
               SELECT 1
               FROM public.checklist_task_instances task
               WHERE task.checklist_task_instance_id = OLD.task_id
                 AND task.status IN ('COMPLETED', 'SKIPPED', 'CANCELLED')
           ))
           OR
           (OLD.task_kind = 'CARE_TASK' AND EXISTS (
               SELECT 1
               FROM public.care_tasks task
               WHERE task.task_id = OLD.task_id
                 AND task.status IN ('DONE', 'CANCELLED')
           ))
           OR
           (OLD.task_kind = 'REMINDER' AND EXISTS (
               SELECT 1
               FROM public.care_tasks task
               WHERE task.task_id = OLD.reminder_definition_id
                 AND task.task_type = 'SCHEDULED_REMINDER'
                 AND task.status IN ('COMPLETED', 'SKIPPED', 'CANCELLED')
           ))
           OR (OLD.task_kind = 'CHECKLIST' AND NOT EXISTS (
               SELECT 1 FROM public.checklist_task_instances task
               WHERE task.checklist_task_instance_id = OLD.task_id
           ))
           OR (OLD.task_kind = 'CARE_TASK' AND NOT EXISTS (
               SELECT 1 FROM public.care_tasks task
               WHERE task.task_id = OLD.task_id
           ))
           OR (OLD.task_kind = 'REMINDER' AND NOT EXISTS (
               SELECT 1 FROM public.care_tasks task
               WHERE task.task_id = OLD.reminder_definition_id
           ))
       ) THEN
        RETURN OLD;
    END IF;

    RAISE EXCEPTION
        'RETENTION_DELETE_NOT_AUTHORIZED: checklist action command is not eligible or caller is not the retention owner'
        USING ERRCODE = '42501';
END
$$;

REVOKE ALL ON FUNCTION public.checklist_action_command_retention_guard() FROM PUBLIC;

DROP TRIGGER IF EXISTS checklist_action_command_retention_guard_trg
    ON public.checklist_action_commands;
CREATE TRIGGER checklist_action_command_retention_guard_trg
    BEFORE DELETE ON public.checklist_action_commands
    FOR EACH ROW EXECUTE FUNCTION public.checklist_action_command_retention_guard();

-- Keep audit_events immutable for every ordinary UPDATE/DELETE. Only statements
-- executing as the non-login retention owner may delete an eligible row.
CREATE OR REPLACE FUNCTION public.carebridge_reject_mutation() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_TABLE_NAME = 'audit_events'
       AND TG_OP = 'DELETE'
       AND current_user = 'carebridge_checklist_retention_owner'
       AND OLD.event_category LIKE 'CHECKLIST\_%' ESCAPE '\'
       AND COALESCE(OLD.legal_hold, false) = false
       AND OLD.created_at < clock_timestamp() - interval '7 years' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'IMMUTABLE_TABLE: %.% does not allow UPDATE or DELETE',
        TG_TABLE_SCHEMA, TG_TABLE_NAME;
END
$$;

REVOKE ALL ON FUNCTION public.carebridge_reject_mutation() FROM PUBLIC;

CREATE OR REPLACE FUNCTION public.checklist_purge_retained_records(p_actor_user_id uuid)
RETURNS TABLE (
    audit_events_purged bigint,
    quarantines_purged bigint,
    action_commands_purged bigint
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
BEGIN
    IF session_user <> 'checklist_operations' THEN
        RAISE EXCEPTION 'PURGE_DATABASE_CALLER_NOT_TRUSTED'
            USING ERRCODE = '42501';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_catalog.pg_auth_members membership
        JOIN pg_catalog.pg_roles owner_role
          ON owner_role.oid = membership.roleid
        WHERE owner_role.rolname = 'carebridge_checklist_retention_owner'
    ) THEN
        RAISE EXCEPTION 'PURGE_OWNER_ROLE_REACHABLE'
            USING ERRCODE = '42501';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM public.users actor
        WHERE actor.user_id = p_actor_user_id
          AND actor.role IN ('SYSTEM_ADMIN', 'OPERATIONS')
          AND actor.account_status = 'ACTIVE'
          AND actor.enabled = true
          AND actor.locked = false
    ) THEN
        RAISE EXCEPTION 'PURGE_ACTOR_NOT_TRUSTED' USING ERRCODE = '42501';
    END IF;

    WITH deleted AS (
        DELETE FROM public.audit_events event
        WHERE event.event_category LIKE 'CHECKLIST\_%' ESCAPE '\'
          AND event.created_at < clock_timestamp() - interval '7 years'
          AND event.legal_hold = false
        RETURNING 1
    )
    SELECT count(*) INTO audit_events_purged FROM deleted;

    WITH deleted AS (
        DELETE FROM public.checklist_migration_quarantine quarantine
        WHERE quarantine.created_at < clock_timestamp() - interval '7 years'
          AND quarantine.retain_until <= clock_timestamp()
          AND quarantine.legal_hold = false
        RETURNING 1
    )
    SELECT count(*) INTO quarantines_purged FROM deleted;

    WITH deleted AS (
        DELETE FROM public.checklist_action_commands command
        WHERE command.created_at < clock_timestamp() - interval '7 years'
          AND command.retain_until <= clock_timestamp()
          AND command.legal_hold = false
          AND (
              (command.task_kind = 'CHECKLIST' AND EXISTS (
                  SELECT 1
                  FROM public.checklist_task_instances task
                  WHERE task.checklist_task_instance_id = command.task_id
                    AND task.status IN ('COMPLETED', 'SKIPPED', 'CANCELLED')
              ))
              OR
              (command.task_kind = 'CARE_TASK' AND EXISTS (
                  SELECT 1
                  FROM public.care_tasks task
                  WHERE task.task_id = command.task_id
                    AND task.status IN ('DONE', 'CANCELLED')
              ))
              OR
              (command.task_kind = 'REMINDER' AND EXISTS (
                  SELECT 1
                  FROM public.care_tasks task
                  WHERE task.task_id = command.reminder_definition_id
                    AND task.task_type = 'SCHEDULED_REMINDER'
                    AND task.status IN ('COMPLETED', 'SKIPPED', 'CANCELLED')
              ))
              OR (command.task_kind = 'CHECKLIST' AND NOT EXISTS (
                  SELECT 1
                  FROM public.checklist_task_instances checklist_task
                  WHERE checklist_task.checklist_task_instance_id = command.task_id
              ))
              OR (command.task_kind = 'CARE_TASK' AND NOT EXISTS (
                  SELECT 1
                  FROM public.care_tasks care_task
                  WHERE care_task.task_id = command.task_id
              ))
              OR (command.task_kind = 'REMINDER' AND NOT EXISTS (
                  SELECT 1
                  FROM public.care_tasks reminder_definition
                  WHERE reminder_definition.task_id = command.reminder_definition_id
              ))
          )
        RETURNING 1
    )
    SELECT count(*) INTO action_commands_purged FROM deleted;

    -- This insert is in the same transaction as all deletes. Any failure rolls
    -- the purge back, making the operations endpoint fail closed.
    INSERT INTO public.audit_events (
        actor_user_id,
        actor_type,
        event_category,
        resource_type,
        reason_code,
        correlation_id,
        after_payload_jsonb,
        occurred_at,
        created_at,
        event_origin,
        legal_hold
    ) VALUES (
        p_actor_user_id,
        'USER',
        'CHECKLIST_RETENTION_PURGED',
        'ChecklistRetention',
        'SEVEN_YEAR_RETENTION',
        gen_random_uuid(),
        jsonb_build_object(
            'auditEventsPurged', audit_events_purged,
            'quarantinesPurged', quarantines_purged,
            'actionCommandsPurged', action_commands_purged),
        clock_timestamp(),
        clock_timestamp(),
        'AUDIT_LOG',
        false
    );

    RETURN QUERY SELECT
        audit_events_purged,
        quarantines_purged,
        action_commands_purged;
END
$$;

REVOKE ALL ON FUNCTION public.checklist_purge_retained_records(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.checklist_purge_retained_records(uuid) FROM CURRENT_USER;

-- No runtime role can execute the intermediate function. A separate privileged
-- versioned deployment finalizer transfers ONLY this function to the NOLOGIN
-- owner, validates catalog invariants, and grants checklist_operations last.

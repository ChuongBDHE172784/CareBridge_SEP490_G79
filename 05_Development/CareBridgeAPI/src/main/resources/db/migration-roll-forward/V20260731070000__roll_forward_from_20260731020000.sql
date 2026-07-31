-- Forward-only compatibility migration for databases whose Flyway history already ends at
-- 20260731020000. Do not run this migration on a clean baseline: use
-- V20260731060000__canonical_post_20260719180000_schema.sql instead.
--
-- This migration is guarded to reject clean or otherwise unsupported history
-- before any source segment executes. On the legacy path, the retirement segment
-- still requires the privileged pre-finalizer/operator attestation before Flyway
-- is started.

DO $rollforward_history_guard$
DECLARE
    current_version text;
BEGIN
    -- Flyway versions are stored as text, so lexical max() is not the
    -- installed migration. Use installed_rank to identify the actual tail.
    SELECT version
      INTO current_version
      FROM public.flyway_schema_history
     WHERE success
       AND installed_rank = (
           SELECT max(installed_rank)
             FROM public.flyway_schema_history
            WHERE success);
    IF current_version IS DISTINCT FROM '20260731020000'
       -- The disposable fixture has 29 rows; real Supabase histories may
       -- contain additional earlier migrations. Require the known minimum.
       OR (SELECT count(*) FROM public.flyway_schema_history WHERE success) < 29
       OR EXISTS (
           SELECT 1
             FROM public.flyway_schema_history
            WHERE success = false)
       OR NOT EXISTS (
           SELECT 1
             FROM public.flyway_schema_history
            WHERE version = '20260731020000'
              AND success = true
              AND script = 'V20260731020000__prepare_checklist_schema_simplification.sql') THEN
        RAISE EXCEPTION 'CHECKLIST_ROLL_FORWARD_HISTORY_REQUIRED:20260731020000';
    END IF;
END
$rollforward_history_guard$;

DO $rollforward_retirement$
BEGIN
    IF to_regclass('public.checklist_substages') IS NOT NULL THEN
        EXECUTE $rollforward_retirement_sql$
-- Checklist three-table retirement. This migration is intentionally non-reversible.
-- Privileged finalized targets must run checklist_retirement_pre_finalizer.sql first.

DO $$
DECLARE
    missing_tables text;
BEGIN
    SELECT string_agg(required.name, ', ' ORDER BY required.name)
      INTO missing_tables
      FROM (VALUES
          ('checklist_reconciliation_candidates'),
          ('checklist_reconciliation_runs'),
          ('checklist_distribution_outbox'),
          ('checklist_migration_quarantine'),
          ('checklist_care_group_contexts'),
          ('checklist_context_authorities'),
          ('checklist_template_version_items'),
          ('checklist_template_recipient_roles'),
          ('checklist_substages')) AS required(name)
     WHERE to_regclass('public.' || required.name) IS NULL;
    IF missing_tables IS NOT NULL THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_REQUIRED_TABLE_MISSING:%', missing_tables;
    END IF;
END $$;

-- Writers are frozen by operator attestation before a finalized live upgrade.
-- Acquire every retirement lock in one fixed order before inspecting mutable state;
-- Flyway's PostgreSQL transaction holds these locks through the final DROP TABLE.
SET LOCAL lock_timeout = '30s';

LOCK TABLE public.checklist_reconciliation_runs IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_reconciliation_candidates IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_distribution_outbox IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_migration_quarantine IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_care_group_contexts IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_context_authorities IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_template_version_items IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_template_recipient_roles IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_substages IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.care_item_templates IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.mother_journeys IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.care_subjects IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.care_groups IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_instances IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_task_instances IN ACCESS EXCLUSIVE MODE;

LOCK TABLE public.checklist_action_commands IN ACCESS EXCLUSIVE MODE;

DO $$
DECLARE
    purge_owner name;
    purge_definition text;
BEGIN

    IF EXISTS (SELECT 1 FROM public.checklist_migration_quarantine WHERE legal_hold) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_LEGAL_HOLD_PRESENT';
    END IF;

    IF (SELECT count(*) FROM public.checklist_migration_quarantine WHERE NOT legal_hold) > 6 THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_QUARANTINE_LIMIT_EXCEEDED';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.checklist_distribution_outbox
        WHERE processed_at IS NULL) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_OUTBOX_NOT_DRAINED';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.checklist_reconciliation_runs
        WHERE status = 'RUNNING') THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_RECONCILIATION_RUN_ACTIVE';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.checklist_reconciliation_candidates
        WHERE outcome = 'PENDING') THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_RECONCILIATION_CANDIDATE_PENDING';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.care_item_templates root
         WHERE root.entry_type = 'TEMPLATE_ROOT'
           AND root.recipient_scope IS DISTINCT FROM (
               SELECT CASE
                   WHEN bool_or(role.recipient_role = 'MOTHER')
                        AND bool_or(role.recipient_role = 'FAMILY') THEN 'BOTH'
                   WHEN bool_or(role.recipient_role = 'MOTHER') THEN 'MOTHER'
                   WHEN bool_or(role.recipient_role = 'FAMILY') THEN 'FAMILY'
                   ELSE NULL
               END
               FROM public.checklist_template_recipient_roles role
               WHERE role.template_version_id = root.template_version_id)) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_RECIPIENT_SCOPE_MISMATCH';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.care_item_templates root
          JOIN public.checklist_substages substage ON substage.substage_id = root.substage_id
         WHERE root.entry_type = 'TEMPLATE_ROOT'
           AND (root.eligibility_anchor_type, root.eligibility_range_unit,
                root.eligibility_start_inclusive, root.eligibility_end_inclusive)
               IS DISTINCT FROM
               (substage.anchor_type, substage.range_unit,
                substage.start_inclusive, substage.end_inclusive)) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_ELIGIBILITY_MISMATCH';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.checklist_task_instances task
          JOIN public.checklist_instances parent
            ON parent.checklist_instance_id = task.checklist_instance_id
          LEFT JOIN public.care_item_templates root
            ON root.template_version_id = task.template_version_id
           AND root.entry_type = 'TEMPLATE_ROOT'
          LEFT JOIN public.care_item_templates item
            ON item.template_id = task.template_item_version_id
           AND item.entry_type = 'CHECKLIST_ENTRY'
         WHERE parent.origin = 'SYSTEM_TEMPLATE'
           AND (root.template_id IS NULL OR item.template_id IS NULL
                OR item.parent_template_id IS DISTINCT FROM root.template_id
                OR parent.template_version_id IS DISTINCT FROM task.template_version_id)) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_TEMPLATE_ITEM_MISMATCH';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM public.checklist_instances instance
         WHERE (instance.care_context_type = 'JOURNEY' AND NOT EXISTS (
                   SELECT 1 FROM public.mother_journeys journey
                    WHERE journey.journey_id = instance.care_context_id
                      AND journey.owner_user_id = instance.context_owner_user_id))
            OR (instance.care_context_type = 'BABY' AND NOT EXISTS (
                   SELECT 1 FROM public.care_subjects baby
                    WHERE baby.care_subject_id = instance.care_context_id
                      AND baby.owner_user_id = instance.context_owner_user_id
                      AND baby.subject_type = 'BABY'))) THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_DIRECT_CONTEXT_MISMATCH';
    END IF;

    SELECT owner_role.rolname, pg_get_functiondef(routine.oid)
      INTO purge_owner, purge_definition
      FROM pg_catalog.pg_proc routine
      JOIN pg_catalog.pg_namespace namespace ON namespace.oid = routine.pronamespace
      JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = routine.proowner
     WHERE namespace.nspname = 'public'
       AND routine.proname = 'checklist_purge_retained_records'
       AND pg_get_function_identity_arguments(routine.oid) = 'p_actor_user_id uuid';
    IF purge_owner IS NULL THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_PURGE_FUNCTION_MISSING';
    END IF;
    IF purge_owner <> current_user
       AND purge_definition NOT LIKE '%CHECKLIST_RETIREMENT_ACTION_LEDGER_ONLY_V1%' THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_PRE_FINALIZER_REQUIRED';
    END IF;
    IF (SELECT owner_role.rolname
          FROM pg_catalog.pg_class relation
          JOIN pg_catalog.pg_namespace namespace ON namespace.oid = relation.relnamespace
          JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = relation.relowner
         WHERE namespace.nspname = 'public'
           AND relation.relname = 'checklist_migration_quarantine') <> current_user THEN
        RAISE EXCEPTION 'CHECKLIST_RETIREMENT_PRE_FINALIZER_REQUIRED';
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

-- On a clean Flyway chain the migration role still owns the routine. Finalized
-- targets arrive with the action-only body already installed by the privileged pre-finalizer.
DO $retirement$
DECLARE
    purge_owner name;
BEGIN
    SELECT owner_role.rolname INTO purge_owner
      FROM pg_catalog.pg_proc routine
      JOIN pg_catalog.pg_namespace namespace ON namespace.oid = routine.pronamespace
      JOIN pg_catalog.pg_roles owner_role ON owner_role.oid = routine.proowner
     WHERE namespace.nspname = 'public'
       AND routine.proname = 'checklist_purge_retained_records'
       AND pg_get_function_identity_arguments(routine.oid) = 'p_actor_user_id uuid';
    IF purge_owner = current_user THEN
        EXECUTE $function$
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
                              WHERE task.task_id = command.task_id
                                AND task.status IN ('DONE','CANCELLED')))
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
                              WHERE task.task_id = command.reminder_definition_id)))
                    RETURNING 1)
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
            $body$
        $function$;
        REVOKE ALL ON FUNCTION public.checklist_purge_retained_records(uuid) FROM PUBLIC;
        REVOKE ALL ON FUNCTION public.checklist_purge_retained_records(uuid) FROM CURRENT_USER;
    END IF;
END
$retirement$;

-- Replace mirrored authorization with canonical-domain constraints and validation.
ALTER TABLE public.care_groups
    ADD COLUMN linked_baby_subject_type varchar(30)
        GENERATED ALWAYS AS (CASE WHEN linked_baby_profile_id IS NULL THEN NULL ELSE 'BABY' END) STORED,
    ADD CONSTRAINT care_groups_linked_journey_owner_fk
        FOREIGN KEY (linked_journey_id, owner_user_id)
        REFERENCES public.mother_journeys(journey_id, owner_user_id) ON DELETE RESTRICT,
    ADD CONSTRAINT care_groups_linked_baby_owner_fk
        FOREIGN KEY (linked_baby_profile_id, owner_user_id, linked_baby_subject_type)
        REFERENCES public.care_subjects(care_subject_id, owner_user_id, subject_type) ON DELETE RESTRICT;

ALTER TABLE public.checklist_instances
    ADD COLUMN journey_context_id uuid GENERATED ALWAYS AS
        (CASE WHEN care_context_type = 'JOURNEY' THEN care_context_id END) STORED,
    ADD COLUMN baby_context_id uuid GENERATED ALWAYS AS
        (CASE WHEN care_context_type = 'BABY' THEN care_context_id END) STORED,
    ADD COLUMN baby_context_subject_type varchar(30) GENERATED ALWAYS AS
        (CASE WHEN care_context_type = 'BABY' THEN 'BABY' END) STORED,
    DROP CONSTRAINT checklist_instances_template_recipient_role_fk,
    DROP CONSTRAINT checklist_instances_context_authority_fk,
    DROP CONSTRAINT checklist_instances_personal_context_authority_fk,
    ADD CONSTRAINT checklist_instances_journey_owner_fk
        FOREIGN KEY (journey_context_id, context_owner_user_id)
        REFERENCES public.mother_journeys(journey_id, owner_user_id) ON DELETE RESTRICT,
    ADD CONSTRAINT checklist_instances_baby_owner_fk
        FOREIGN KEY (baby_context_id, context_owner_user_id, baby_context_subject_type)
        REFERENCES public.care_subjects(care_subject_id, owner_user_id, subject_type) ON DELETE RESTRICT;

CREATE OR REPLACE FUNCTION public.checklist_validate_instance_recipient()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.recipient_role = 'MOTHER' THEN
        IF NEW.care_group_id IS NOT NULL
           OR NEW.recipient_user_id IS DISTINCT FROM NEW.context_owner_user_id THEN
            RAISE EXCEPTION 'CHECKLIST_MOTHER_RECIPIENT_NOT_AUTHORIZED';
        END IF;
    ELSE
        PERFORM 1
          FROM public.care_groups care_group
          JOIN public.care_group_members member
            ON member.care_group_id = care_group.care_group_id
         WHERE care_group.care_group_id = NEW.care_group_id
           AND care_group.owner_user_id = NEW.context_owner_user_id
           AND care_group.status = 'ACTIVE'
           AND ((NEW.care_context_type = 'JOURNEY'
                 AND care_group.linked_journey_id = NEW.care_context_id)
                OR (NEW.care_context_type = 'BABY'
                    AND care_group.linked_baby_profile_id = NEW.care_context_id))
           AND member.user_id = NEW.recipient_user_id
           AND member.member_role <> 'OWNER'
           AND member.invitation_status = 'ACCEPTED'
           AND jsonb_typeof(member.permission_json) = 'object'
           AND jsonb_typeof(member.permission_json->'CHECKLIST_VIEW') = 'boolean'
           AND member.permission_json->>'CHECKLIST_VIEW' = 'true'
         FOR KEY SHARE OF care_group, member;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'CHECKLIST_FAMILY_RECIPIENT_NOT_AUTHORIZED';
        END IF;
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER checklist_validate_instance_recipient_trg ON public.checklist_instances;
CREATE TRIGGER checklist_validate_instance_recipient_trg
BEFORE INSERT OR UPDATE OF recipient_role, recipient_user_id, care_group_id,
    care_context_type, care_context_id, context_owner_user_id
ON public.checklist_instances
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_instance_recipient();

ALTER TABLE public.checklist_task_instances
    DROP CONSTRAINT checklist_task_instances_version_item_fk,
    ADD CONSTRAINT checklist_task_instances_template_version_fk
        FOREIGN KEY (template_version_id)
        REFERENCES public.care_item_templates(template_version_id) ON DELETE RESTRICT,
    ADD CONSTRAINT checklist_task_instances_template_item_fk
        FOREIGN KEY (template_item_version_id)
        REFERENCES public.care_item_templates(template_id) ON DELETE RESTRICT;

CREATE OR REPLACE FUNCTION public.checklist_validate_task_template()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    parent_origin varchar(20);
    parent_version_id uuid;
    root_id uuid;
    item_parent_id uuid;
BEGIN
    SELECT parent.origin, parent.template_version_id
      INTO parent_origin, parent_version_id
      FROM public.checklist_instances parent
     WHERE parent.checklist_instance_id = NEW.checklist_instance_id
     FOR KEY SHARE;
    IF parent_origin = 'SYSTEM_TEMPLATE' THEN
        SELECT root.template_id INTO root_id
          FROM public.care_item_templates root
         WHERE root.template_version_id = NEW.template_version_id
           AND root.entry_type = 'TEMPLATE_ROOT';
        SELECT item.parent_template_id INTO item_parent_id
          FROM public.care_item_templates item
         WHERE item.template_id = NEW.template_item_version_id
           AND item.entry_type = 'CHECKLIST_ENTRY';
        IF NEW.template_version_id IS NULL OR NEW.template_item_version_id IS NULL
           OR NEW.template_version_id IS DISTINCT FROM parent_version_id
           OR root_id IS NULL OR item_parent_id IS DISTINCT FROM root_id THEN
            RAISE EXCEPTION 'CHECKLIST_SYSTEM_TASK_TEMPLATE_REQUIRED';
        END IF;
    ELSIF NEW.template_version_id IS NOT NULL OR NEW.template_item_version_id IS NOT NULL THEN
        RAISE EXCEPTION 'CHECKLIST_USER_TASK_TEMPLATE_FORBIDDEN';
    END IF;
    RETURN NEW;
END $$;

-- Inline metadata is now the sole template authority.
ALTER TABLE public.care_item_templates DROP CONSTRAINT care_item_templates_substage_fk;
DROP TRIGGER checklist_sync_inline_recipient_scope_trg ON public.checklist_template_recipient_roles;
DROP TRIGGER checklist_sync_inline_eligibility_trg ON public.care_item_templates;
DROP TRIGGER checklist_sync_inline_eligibility_from_substage_trg ON public.checklist_substages;
DROP TRIGGER checklist_guard_referenced_substage_mutation_trg ON public.checklist_substages;
DROP TRIGGER checklist_guard_approved_role_mutation_trg ON public.checklist_template_recipient_roles;
DROP TRIGGER checklist_guard_version_item_authority_mutation_trg ON public.checklist_template_version_items;
DROP TRIGGER checklist_sync_template_version_item_write_trg ON public.care_item_templates;
DROP TRIGGER checklist_sync_template_version_item_delete_trg ON public.care_item_templates;

DROP TRIGGER checklist_validate_inline_template_shape_trg ON public.care_item_templates;
CREATE OR REPLACE FUNCTION public.checklist_validate_inline_template_shape()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.entry_type <> 'TEMPLATE_ROOT' THEN
        IF NEW.recipient_scope IS NOT NULL OR NEW.eligibility_anchor_type IS NOT NULL
           OR NEW.eligibility_range_unit IS NOT NULL
           OR NEW.eligibility_start_inclusive IS NOT NULL
           OR NEW.eligibility_end_inclusive IS NOT NULL THEN
            RAISE EXCEPTION 'INLINE_TEMPLATE_SHAPE_INVALID';
        END IF;
        RETURN NULL;
    END IF;
    IF NEW.recipient_scope IS NULL THEN RAISE EXCEPTION 'TEMPLATE_ROLE_REQUIRED'; END IF;
    IF NEW.recipient_scope = 'FAMILY' THEN
        IF NEW.stage IS NOT NULL OR NEW.eligibility_anchor_type IS NOT NULL
           OR NEW.eligibility_range_unit IS NOT NULL
           OR NEW.eligibility_start_inclusive IS NOT NULL
           OR NEW.eligibility_end_inclusive IS NOT NULL THEN
            RAISE EXCEPTION 'INLINE_TEMPLATE_SHAPE_INVALID';
        END IF;
        RETURN NULL;
    END IF;
    IF NEW.stage IS NULL OR NEW.eligibility_anchor_type IS NULL
       OR NEW.eligibility_range_unit IS NULL OR NEW.eligibility_start_inclusive IS NULL
       OR NEW.eligibility_end_inclusive IS NULL
       OR NEW.eligibility_start_inclusive < 0
       OR NEW.eligibility_end_inclusive < NEW.eligibility_start_inclusive
       OR NOT (
           (NEW.stage = 'PRE_PREGNANCY' AND NEW.eligibility_anchor_type = 'NONE'
            AND NEW.eligibility_range_unit = 'DAY'
            AND NEW.eligibility_start_inclusive = 0 AND NEW.eligibility_end_inclusive = 0)
           OR (NEW.migration_review_required AND NEW.eligibility_anchor_type = 'NONE'
               AND NEW.eligibility_range_unit = 'DAY'
               AND NEW.eligibility_start_inclusive = 0
               AND NEW.eligibility_end_inclusive = 2147483647)
           OR (NEW.stage = 'PREGNANCY' AND NEW.eligibility_anchor_type IN ('LMP','EDD'))
           OR (NEW.stage = 'POSTPARTUM' AND NEW.eligibility_anchor_type = 'DELIVERY_DATE')
           OR (NEW.stage = 'BABY_CARE' AND NEW.eligibility_anchor_type = 'BIRTH_DATE')) THEN
        RAISE EXCEPTION 'INLINE_TEMPLATE_SHAPE_INVALID';
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER checklist_validate_inline_template_shape_trg
AFTER INSERT OR UPDATE OF entry_type, stage, content_status, distribution_enabled,
    migration_review_required, recipient_scope, eligibility_anchor_type,
    eligibility_range_unit, eligibility_start_inclusive, eligibility_end_inclusive
ON public.care_item_templates DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_inline_template_shape();

DROP TRIGGER checklist_validate_template_approval_trg ON public.care_item_templates;
CREATE OR REPLACE FUNCTION public.checklist_validate_template_approval()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.entry_type <> 'TEMPLATE_ROOT' THEN RETURN NEW; END IF;
    IF (NEW.distribution_enabled OR NEW.content_status = 'APPROVED')
       AND NEW.migration_review_required THEN RAISE EXCEPTION 'MIGRATION_REVIEW_REQUIRED'; END IF;
    IF NEW.distribution_enabled OR NEW.content_status = 'APPROVED' THEN
        IF NEW.recipient_scope IS NULL THEN RAISE EXCEPTION 'TEMPLATE_ROLE_REQUIRED'; END IF;
        IF EXISTS (
            SELECT 1 FROM public.care_item_templates item
             WHERE item.parent_template_id = NEW.template_id
               AND item.entry_type = 'CHECKLIST_ENTRY' AND item.is_active
               AND item.target_subject IS NULL) THEN
            RAISE EXCEPTION 'ITEM_TARGET_REQUIRED';
        END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER checklist_validate_template_approval_trg
BEFORE INSERT OR UPDATE OF content_status, distribution_enabled, migration_review_required,
    stage, recipient_scope, eligibility_anchor_type, eligibility_range_unit,
    eligibility_start_inclusive, eligibility_end_inclusive, template_version_id
ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_template_approval();

-- Stop every writer/maintainer for retired mirrors.
DROP TRIGGER checklist_sync_journey_context_authority_trg ON public.mother_journeys;
DROP TRIGGER checklist_sync_baby_context_authority_trg ON public.care_subjects;
DROP TRIGGER checklist_sync_reviewed_care_group_contexts_trg ON public.care_groups;
DROP TRIGGER checklist_block_previous_care_group_context_trg ON public.care_groups;

-- Quarantine security dependencies are named explicitly before table retirement.
DROP TRIGGER checklist_quarantine_forensic_guard_trg ON public.checklist_migration_quarantine;
DROP POLICY checklist_migration_quarantine_insert_policy ON public.checklist_migration_quarantine;
DROP POLICY IF EXISTS checklist_migration_quarantine_operations_select ON public.checklist_migration_quarantine;
DROP POLICY IF EXISTS checklist_migration_quarantine_operations_update ON public.checklist_migration_quarantine;
DROP POLICY checklist_migration_quarantine_application_select ON public.checklist_migration_quarantine;
DROP POLICY checklist_migration_quarantine_application_resolve ON public.checklist_migration_quarantine;
DROP POLICY checklist_migration_quarantine_retention_owner_delete ON public.checklist_migration_quarantine;
DROP POLICY checklist_migration_quarantine_retention_owner_select ON public.checklist_migration_quarantine;
REVOKE ALL ON public.checklist_migration_quarantine FROM PUBLIC, carebridge_application,
    checklist_operations, carebridge_checklist_retention_owner;

DROP INDEX public.checklist_migration_quarantine_correlation_ix;
DROP INDEX public.checklist_quarantine_open_group_context_uk;
DROP INDEX public.checklist_migration_quarantine_retention_ix;
DROP INDEX public.checklist_distribution_outbox_pending_ix;
DROP INDEX public.checklist_distribution_outbox_correlation_ix;
DROP INDEX public.checklist_distribution_outbox_retry_ix;
DROP INDEX public.checklist_distribution_outbox_exhausted_ix;
DROP INDEX public.checklist_reconciliation_success_watermark_ix;
DROP INDEX public.checklist_care_group_contexts_single_active_type_ux;

-- Retired-table functions are dropped only after all calling triggers/functions are replaced.
-- The CHK-039 verifier embeds quarantine regclass references in its PL/pgSQL body;
-- retire it explicitly before the table disappears so no broken callable remains.
DROP FUNCTION IF EXISTS public.checklist_assert_retention_security();
DROP FUNCTION public.checklist_quarantine_forensic_guard();
DROP FUNCTION public.checklist_quarantine_invalid_group_context(uuid, varchar, uuid, varchar);
DROP FUNCTION public.checklist_sync_reviewed_care_group_contexts();
DROP FUNCTION public.checklist_block_previous_care_group_context();
DROP FUNCTION public.checklist_sync_context_authority();
DROP FUNCTION public.checklist_sync_template_version_item();
DROP FUNCTION public.checklist_sync_inline_recipient_scope();
DROP FUNCTION public.checklist_sync_inline_eligibility();
DROP FUNCTION public.checklist_sync_inline_eligibility_from_substage();
DROP FUNCTION public.checklist_guard_referenced_substage_mutation();
DROP FUNCTION public.checklist_guard_approved_role_mutation();
DROP FUNCTION public.checklist_guard_version_item_authority_mutation();

-- Exact approved dependency order. Never add CASCADE here.
DROP TABLE public.checklist_reconciliation_candidates;
DROP TABLE public.checklist_reconciliation_runs;
DROP TABLE public.checklist_distribution_outbox;
DROP TABLE public.checklist_migration_quarantine;
DROP TABLE public.checklist_care_group_contexts;
DROP TABLE public.checklist_context_authorities;
DROP TABLE public.checklist_template_version_items;
DROP TABLE public.checklist_template_recipient_roles;
DROP TABLE public.checklist_substages;

        $rollforward_retirement_sql$;
    END IF;
END
$rollforward_retirement$;

DO $rollforward_health$
BEGIN
    IF to_regclass('public.health_metric_definitions') IS NULL THEN
        EXECUTE $rollforward_health_sql$
CREATE TABLE public.health_metric_definitions (
    metric_definition_id uuid DEFAULT gen_random_uuid() NOT NULL,
    metric_code varchar(60) NOT NULL,
    version integer NOT NULL,
    display_name varchar(120) NOT NULL,
    observation_shape varchar(30) NOT NULL,
    subject_type varchar(30) DEFAULT 'MOTHER' NOT NULL,
    manual_entry_supported boolean DEFAULT false NOT NULL,
    device_import_supported boolean DEFAULT false NOT NULL,
    canonical_unit varchar(30),
    accepted_input_units_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    precision_scale smallint,
    required_context_schema_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    plausibility_policy_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    aggregation_policy_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    chart_policy_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    quality_policy_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    safety_policy_version varchar(40),
    allowed_journey_stages_jsonb jsonb DEFAULT '[]'::jsonb NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    effective_from timestamptz DEFAULT now() NOT NULL,
    effective_until timestamptz,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT health_metric_definitions_pk PRIMARY KEY (metric_definition_id),
    CONSTRAINT health_metric_definitions_code_version_uk UNIQUE (metric_code, version),
    CONSTRAINT health_metric_definitions_code_ck CHECK (btrim(metric_code) <> ''),
    CONSTRAINT health_metric_definitions_display_name_ck CHECK (btrim(display_name) <> ''),
    CONSTRAINT health_metric_definitions_version_ck CHECK (version > 0),
    CONSTRAINT health_metric_definitions_shape_ck CHECK (observation_shape IN (
        'POINT', 'PAIRED_POINT', 'SESSION', 'INTERVAL_AGGREGATE'
    )),
    CONSTRAINT health_metric_definitions_subject_ck CHECK (subject_type = 'MOTHER'),
    CONSTRAINT health_metric_definitions_precision_ck CHECK (
        precision_scale IS NULL OR precision_scale >= 0
    ),
    CONSTRAINT health_metric_definitions_effective_period_ck CHECK (
        effective_until IS NULL OR effective_until > effective_from
    ),
    CONSTRAINT health_metric_definitions_units_json_ck CHECK (
        jsonb_typeof(accepted_input_units_jsonb) = 'array'
    ),
    CONSTRAINT health_metric_definitions_context_json_ck CHECK (
        jsonb_typeof(required_context_schema_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_plausibility_json_ck CHECK (
        jsonb_typeof(plausibility_policy_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_aggregation_json_ck CHECK (
        jsonb_typeof(aggregation_policy_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_chart_json_ck CHECK (
        jsonb_typeof(chart_policy_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_quality_json_ck CHECK (
        jsonb_typeof(quality_policy_jsonb) = 'object'
    ),
    CONSTRAINT health_metric_definitions_stages_json_ck CHECK (
        jsonb_typeof(allowed_journey_stages_jsonb) = 'array'
    )
);

CREATE UNIQUE INDEX health_metric_definitions_active_code_uk
    ON public.health_metric_definitions(metric_code)
    WHERE is_active = true;

CREATE INDEX health_metric_definitions_active_display_ix
    ON public.health_metric_definitions(is_active, display_name);

CREATE INDEX health_metric_definitions_effective_period_ix
    ON public.health_metric_definitions(effective_from, effective_until);

INSERT INTO public.health_metric_definitions (
    metric_code,
    version,
    display_name,
    observation_shape,
    manual_entry_supported,
    device_import_supported,
    canonical_unit,
    accepted_input_units_jsonb,
    precision_scale,
    required_context_schema_jsonb,
    aggregation_policy_jsonb,
    chart_policy_jsonb,
    quality_policy_jsonb,
    allowed_journey_stages_jsonb
)
VALUES
    (
        'WEIGHT', 1, 'Cân nặng', 'POINT', true, false, 'kg',
        '["kg", "lb"]'::jsonb, 2, '{}'::jsonb,
        '{"method":"REPRESENTATIVE_DAILY_POINT","baselineAware":true}'::jsonb,
        '{"type":"LINE","xAxis":"GESTATIONAL_WEEK_OR_DATE"}'::jsonb,
        '{"required":false}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'BLOOD_PRESSURE', 1, 'Huyết áp', 'PAIRED_POINT', true, true, 'mmHg',
        '["mmHg"]'::jsonb, 0,
        '{"required":["systolic","diastolic"]}'::jsonb,
        '{"method":"NONE"}'::jsonb,
        '{"type":"DUAL_LINE","series":["systolic","diastolic"]}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'BLOOD_GLUCOSE', 1, 'Đường huyết', 'POINT', true, false, 'mg/dL',
        '["mg/dL", "mmol/L"]'::jsonb, 2,
        '{"required":["measurementContext"],"measurementContextValues":["FASTING","PRE_MEAL","POST_MEAL_1H","POST_MEAL_2H","RANDOM","OTHER_APPROVED"]}'::jsonb,
        '{"method":"PARTITION_BY_CONTEXT"}'::jsonb,
        '{"type":"CONTEXT_SERIES","partitionBy":"measurementContext"}'::jsonb,
        '{"required":false}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'FETAL_MOVEMENT_SESSION', 1, 'Cử động thai', 'SESSION', true, false, 'count',
        '["count"]'::jsonb, 0,
        '{"required":["periodStart","periodEnd","protocolCode","completionStatus","gestationalAgeSnapshot"]}'::jsonb,
        '{"method":"SESSION_HISTORY"}'::jsonb,
        '{"type":"SESSION_TIMELINE"}'::jsonb,
        '{"required":false}'::jsonb,
        '["PREGNANCY"]'::jsonb
    ),
    (
        'MATERNAL_HEART_RATE', 1, 'Nhịp tim mẹ', 'POINT', true, true, 'bpm',
        '["bpm"]'::jsonb, 0,
        '{"required":["measurementState"],"measurementStateValues":["RESTING","ACTIVE","POST_EXERCISE","UNKNOWN"]}'::jsonb,
        '{"method":"PARTITION_BY_CONTEXT","partitionBy":"measurementState"}'::jsonb,
        '{"type":"CONTEXT_SERIES","partitionBy":"measurementState"}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'SLEEP_SESSION', 1, 'Giấc ngủ', 'INTERVAL_AGGREGATE', false, true, 'min',
        '["min", "h"]'::jsonb, 2,
        '{"required":["periodStart","periodEnd","timeZone","sleepType"]}'::jsonb,
        '{"method":"MERGE_PROVIDER_INTERVALS","requiresCompleteness":true}'::jsonb,
        '{"type":"INTERVAL_TIMELINE","showDataGaps":true}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'STEPS', 1, 'Số bước', 'INTERVAL_AGGREGATE', false, true, 'count',
        '["count"]'::jsonb, 0,
        '{"required":["periodStart","periodEnd","timeZone","aggregationLevel"]}'::jsonb,
        '{"method":"DAILY_TOTAL_AFTER_DEDUPLICATION","requiresCompleteness":true}'::jsonb,
        '{"type":"DAILY_BAR","showPartialDay":true}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'SPO2', 1, 'SpO2', 'POINT', false, true, '%',
        '["%"]'::jsonb, 2,
        '{"required":[]}'::jsonb,
        '{"method":"QUALITY_FILTERED_SERIES"}'::jsonb,
        '{"type":"LINE","showDataGaps":true,"showQuality":true}'::jsonb,
        '{"requiredForDevice":true,"allowedLabels":["VALID","LOW_QUALITY","INCOMPLETE","UNKNOWN","REJECTED"]}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    ),
    (
        'TEMPERATURE', 1, 'Nhiệt độ', 'POINT', true, true, 'Cel',
        '["Cel", "°C", "°F"]'::jsonb, 2,
        '{"required":["measurementSite"]}'::jsonb,
        '{"method":"SAME_METHOD_TIME_SERIES"}'::jsonb,
        '{"type":"METHOD_SERIES","partitionBy":"measurementSite"}'::jsonb,
        '{"requiredForDevice":true}'::jsonb,
        '["PRE_PREGNANCY", "PREGNANCY", "POSTPARTUM"]'::jsonb
    );


ALTER TABLE public.health_observations
    ADD COLUMN IF NOT EXISTS period_start timestamptz,
    ADD COLUMN IF NOT EXISTS period_end timestamptz,
    ADD COLUMN IF NOT EXISTS context_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    ADD COLUMN IF NOT EXISTS original_unit varchar(30),
    ADD COLUMN IF NOT EXISTS definition_version integer,
    ADD COLUMN IF NOT EXISTS observation_shape varchar(30);

ALTER TABLE public.health_observations
    ADD CONSTRAINT health_observations_p0_period_ck CHECK (
        period_end IS NULL OR (period_start IS NOT NULL AND period_end > period_start)
    );

ALTER TABLE public.health_observations
    ADD CONSTRAINT health_observations_p0_context_json_ck CHECK (
        jsonb_typeof(context_jsonb) = 'object'
    );

CREATE INDEX IF NOT EXISTS health_observations_p0_subject_metric_time_ix
    ON public.health_observations(care_subject_id, observation_type, observed_at)
    WHERE legacy_source = 'maternal_health_observations';

        $rollforward_health_sql$;
    END IF;
END
$rollforward_health$;

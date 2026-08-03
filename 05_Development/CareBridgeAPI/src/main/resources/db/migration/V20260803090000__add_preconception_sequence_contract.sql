-- Sequential PRE_PREGNANCY checklist contract (forward-only).
-- display_order already exists on care_item_templates and remains zero for legacy rows.

-- History is still immutable; allow the explicit sequence transition reason in addition
-- to the existing lifecycle-obsolete marker.
ALTER TABLE public.checklist_instances
    DROP CONSTRAINT IF EXISTS checklist_instances_history_reason_ck;

ALTER TABLE public.checklist_instances
    ADD CONSTRAINT checklist_instances_history_reason_ck CHECK (
        history_reason_code IS NULL
        OR (length(history_reason_code) <= 80
            AND (history_reason_code LIKE 'LIFECYCLE_STAGE_OBSOLETE%'
                 OR history_reason_code = 'SEQUENCE_STEP_COMPLETED'))
    );

ALTER TABLE public.checklist_action_commands
    DROP CONSTRAINT IF EXISTS checklist_action_commands_task_kind_ck,
    DROP CONSTRAINT IF EXISTS checklist_action_commands_action_ck;

ALTER TABLE public.checklist_action_commands
    ADD CONSTRAINT checklist_action_commands_task_kind_ck
        CHECK (task_kind IN ('CHECKLIST', 'CARE_TASK', 'REMINDER', 'CHECKLIST_SEQUENCE')),
    ADD CONSTRAINT checklist_action_commands_action_ck
        CHECK (action_type IN ('COMPLETE', 'SKIP', 'REOPEN', 'ADVANCE')),
    ADD CONSTRAINT checklist_action_commands_sequence_action_ck
        CHECK ((task_kind = 'CHECKLIST_SEQUENCE' AND action_type = 'ADVANCE')
            OR (task_kind <> 'CHECKLIST_SEQUENCE' AND action_type <> 'ADVANCE'));

ALTER TABLE public.care_item_templates
    ADD CONSTRAINT care_item_templates_sequence_position_ck CHECK (
        entry_type <> 'TEMPLATE_ROOT'
        OR (display_order >= 0 AND display_order <= 1000)
    ),
    ADD CONSTRAINT care_item_templates_sequence_scope_ck CHECK (
        entry_type <> 'TEMPLATE_ROOT'
        OR display_order <= 0
        OR (stage = 'PRE_PREGNANCY' AND template_type = 'MANDATORY'
            AND recipient_scope = 'MOTHER')
    );

CREATE OR REPLACE FUNCTION public.checklist_validate_action_command_target()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.task_kind = 'CHECKLIST' THEN
        PERFORM 1 FROM public.checklist_task_instances task
        WHERE task.checklist_task_instance_id = NEW.task_id;
    ELSIF NEW.task_kind = 'REMINDER' THEN
        PERFORM 1 FROM public.care_tasks task
        WHERE task.task_id = COALESCE(NEW.reminder_definition_id, NEW.task_id)
          AND task.task_type = 'SCHEDULED_REMINDER';
    ELSIF NEW.task_kind = 'CHECKLIST_SEQUENCE' THEN
        PERFORM 1 FROM public.checklist_instances instance
        WHERE instance.checklist_instance_id = NEW.task_id;
    ELSE
        PERFORM 1 FROM public.care_tasks task WHERE task.task_id = NEW.task_id;
    END IF;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'CHECKLIST_ACTION_TARGET_NOT_FOUND';
    END IF;
    RETURN NEW;
END $$;

-- Sequence advance commands are forensic idempotency rows too.  Permit the same
-- owner-only retention path used for task commands once the predecessor is in
-- immutable History (or has been removed by an approved retention process).
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
                 AND task.status IN ('COMPLETED', 'SKIPPED', 'CANCELLED')
           ))
           OR (OLD.task_kind = 'CARE_TASK' AND EXISTS (
               SELECT 1 FROM public.care_tasks task
               WHERE task.task_id = OLD.task_id
                 AND task.status IN ('DONE', 'CANCELLED')
           ))
           OR (OLD.task_kind = 'REMINDER' AND EXISTS (
               SELECT 1 FROM public.care_tasks task
               WHERE task.task_id = OLD.reminder_definition_id
                 AND task.task_type = 'SCHEDULED_REMINDER'
                 AND task.status IN ('COMPLETED', 'SKIPPED', 'CANCELLED')
           ))
           OR (OLD.task_kind = 'CHECKLIST_SEQUENCE' AND EXISTS (
               SELECT 1 FROM public.checklist_instances instance
               WHERE instance.checklist_instance_id = OLD.task_id
                 AND instance.historical_at IS NOT NULL
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
           OR (OLD.task_kind = 'CHECKLIST_SEQUENCE' AND NOT EXISTS (
               SELECT 1 FROM public.checklist_instances instance
               WHERE instance.checklist_instance_id = OLD.task_id
           ))
       ) THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION
        'RETENTION_DELETE_NOT_AUTHORIZED: checklist action command is not eligible or caller is not the retention owner'
        USING ERRCODE = '42501';
END
$$;

-- Protect the reused root display_order column from direct SQL reordering.  Once a
-- lineage has an instance, even a draft version cannot change its sequence position.
CREATE OR REPLACE FUNCTION public.checklist_guard_sequence_position_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.entry_type = 'TEMPLATE_ROOT'
       AND NEW.display_order IS DISTINCT FROM OLD.display_order
       AND (
           OLD.content_status IN ('APPROVED', 'ARCHIVED')
           OR EXISTS (
               SELECT 1 FROM public.checklist_instances instance
               WHERE instance.template_lineage_id = OLD.template_lineage_id
           )
       ) THEN
        RAISE EXCEPTION 'VERSION_IMMUTABLE';
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS checklist_guard_sequence_position_mutation_trg
    ON public.care_item_templates;
CREATE TRIGGER checklist_guard_sequence_position_mutation_trg
BEFORE UPDATE OF display_order ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_guard_sequence_position_mutation();

-- Only approved, distribution-enabled roots may occupy a positive sequence position.
-- Drafts and archived versions can share a position so version-pinned instances remain
-- resolvable; the partial index prevents two active candidates from being selected.
CREATE UNIQUE INDEX IF NOT EXISTS care_item_templates_preconception_sequence_position_uk
    ON public.care_item_templates (display_order)
    WHERE entry_type = 'TEMPLATE_ROOT'
      AND stage = 'PRE_PREGNANCY'
      AND template_type = 'MANDATORY'
      AND recipient_scope = 'MOTHER'
      AND distribution_enabled = true
      AND content_status = 'APPROVED'
      AND display_order > 0;

CREATE INDEX IF NOT EXISTS care_item_templates_preconception_sequence_lookup_ix
    ON public.care_item_templates (stage, template_type, recipient_scope,
                                   display_order, content_status, distribution_enabled,
                                   template_lineage_id, template_version_id)
    WHERE entry_type = 'TEMPLATE_ROOT' AND display_order > 0;

CREATE INDEX IF NOT EXISTS checklist_instances_sequence_scope_ix
    ON public.checklist_instances (context_owner_user_id, recipient_user_id,
                                   recipient_role, care_context_type, care_context_id,
                                   historical_at, template_version_id);

-- Preserve a reminder occurrence as the idempotency task identity while retaining
-- the canonical reminder definition needed for database-level target validation.
ALTER TABLE public.checklist_action_commands
    ADD COLUMN reminder_definition_id uuid;

UPDATE public.checklist_action_commands command
SET reminder_definition_id = command.task_id
WHERE command.task_kind = 'REMINDER'
  AND command.reminder_definition_id IS NULL
  AND EXISTS (
      SELECT 1
      FROM public.care_tasks task
      WHERE task.task_id = command.task_id
        AND task.task_type = 'SCHEDULED_REMINDER'
  );

ALTER TABLE public.checklist_action_commands
    ADD CONSTRAINT checklist_action_commands_reminder_definition_fk
        FOREIGN KEY (reminder_definition_id)
        REFERENCES public.care_tasks(task_id) ON DELETE RESTRICT,
    ADD CONSTRAINT checklist_action_commands_reminder_definition_ck CHECK (
        (task_kind = 'REMINDER' AND reminder_definition_id IS NOT NULL)
        OR (task_kind <> 'REMINDER' AND reminder_definition_id IS NULL)
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
    ELSE
        PERFORM 1 FROM public.care_tasks task WHERE task.task_id = NEW.task_id;
    END IF;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'CHECKLIST_ACTION_TARGET_NOT_FOUND';
    END IF;
    RETURN NEW;
END $$;

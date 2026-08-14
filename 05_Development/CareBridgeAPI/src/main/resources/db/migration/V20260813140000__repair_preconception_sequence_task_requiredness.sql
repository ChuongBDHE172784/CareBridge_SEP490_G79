-- Repair semantic requiredness drift in current PRE_PREGNANCY sequence snapshots.
-- The leaf catalog item is authoritative; root type is used only to scope the
-- sequence cohort. Historical and quarantined snapshots remain immutable.

UPDATE public.checklist_task_instances AS task
SET is_required = item.is_required,
    lock_version = task.lock_version + 1
FROM public.checklist_instances AS parent
JOIN public.care_item_templates AS item
  ON item.entry_type = 'CHECKLIST_ENTRY'
JOIN public.care_item_templates AS root
  ON root.template_id = item.parent_template_id
 AND root.entry_type = 'TEMPLATE_ROOT'
WHERE task.checklist_instance_id = parent.checklist_instance_id
  AND task.template_item_version_id = item.template_id
  AND task.template_version_id = root.template_version_id
  AND task.is_required IS DISTINCT FROM item.is_required
  AND item.is_required IS NOT NULL
  AND task.checklist_quarantine_reason_code IS NULL
  AND parent.checklist_quarantine_reason_code IS NULL
  AND item.checklist_quarantine_reason_code IS NULL
  AND root.checklist_quarantine_reason_code IS NULL
  AND parent.historical_at IS NULL
  AND parent.status <> 'CANCELLED'
  AND parent.origin = 'SYSTEM_TEMPLATE'
  AND parent.recipient_role = 'MOTHER'
  AND parent.care_context_type = 'JOURNEY'
  AND root.stage = 'PRE_PREGNANCY'
  AND root.template_type = 'MANDATORY'
  AND root.recipient_scope = 'MOTHER'
  AND root.display_order > 0;

DO $$
DECLARE
    remaining_drift bigint;
BEGIN
    SELECT count(*)
      INTO remaining_drift
      FROM public.checklist_task_instances AS task
      JOIN public.checklist_instances AS parent
        ON parent.checklist_instance_id = task.checklist_instance_id
      JOIN public.care_item_templates AS item
        ON item.template_id = task.template_item_version_id
       AND item.entry_type = 'CHECKLIST_ENTRY'
      JOIN public.care_item_templates AS root
        ON root.template_id = item.parent_template_id
       AND root.entry_type = 'TEMPLATE_ROOT'
     WHERE task.template_version_id = root.template_version_id
       AND task.is_required IS DISTINCT FROM item.is_required
       AND item.is_required IS NOT NULL
       AND task.checklist_quarantine_reason_code IS NULL
       AND parent.checklist_quarantine_reason_code IS NULL
       AND item.checklist_quarantine_reason_code IS NULL
       AND root.checklist_quarantine_reason_code IS NULL
       AND parent.historical_at IS NULL
       AND parent.status <> 'CANCELLED'
       AND parent.origin = 'SYSTEM_TEMPLATE'
       AND parent.recipient_role = 'MOTHER'
       AND parent.care_context_type = 'JOURNEY'
       AND root.stage = 'PRE_PREGNANCY'
       AND root.template_type = 'MANDATORY'
       AND root.recipient_scope = 'MOTHER'
       AND root.display_order > 0;
    IF remaining_drift > 0 THEN
        RAISE EXCEPTION
            'CHECKLIST_PRECONCEPTION_REQUIREDNESS_DRIFT_REMAINS: count=%',
            remaining_drift;
    END IF;
END $$;

-- Keep the repaired invariant true for new/updated current system sequence tasks.
-- Historical, cancelled, quarantined, and user-created rows remain untouched.
CREATE OR REPLACE FUNCTION public.checklist_guard_preconception_requiredness()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    expected_required boolean;
BEGIN
    SELECT item.is_required
      INTO expected_required
      FROM public.checklist_instances parent
      JOIN public.care_item_templates item
        ON item.template_id = NEW.template_item_version_id
       AND item.entry_type = 'CHECKLIST_ENTRY'
      JOIN public.care_item_templates root
        ON root.template_id = item.parent_template_id
       AND root.entry_type = 'TEMPLATE_ROOT'
     WHERE parent.checklist_instance_id = NEW.checklist_instance_id
       AND NEW.template_version_id = root.template_version_id
       AND item.is_required IS NOT NULL
       AND NEW.checklist_quarantine_reason_code IS NULL
       AND parent.checklist_quarantine_reason_code IS NULL
       AND item.checklist_quarantine_reason_code IS NULL
       AND root.checklist_quarantine_reason_code IS NULL
       AND parent.historical_at IS NULL
       AND parent.status <> 'CANCELLED'
       AND parent.origin = 'SYSTEM_TEMPLATE'
       AND parent.recipient_role = 'MOTHER'
       AND parent.care_context_type = 'JOURNEY'
       AND root.stage = 'PRE_PREGNANCY'
       AND root.template_type = 'MANDATORY'
       AND root.recipient_scope = 'MOTHER'
       AND root.display_order > 0;
    IF FOUND AND NEW.is_required IS DISTINCT FROM expected_required THEN
        RAISE EXCEPTION
            'CHECKLIST_PRECONCEPTION_REQUIREDNESS_MISMATCH: expected=% actual=%',
            expected_required, NEW.is_required;
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS checklist_guard_preconception_requiredness_trg
    ON public.checklist_task_instances;

CREATE TRIGGER checklist_guard_preconception_requiredness_trg
BEFORE INSERT OR UPDATE OF is_required, template_item_version_id, template_version_id,
    checklist_instance_id, checklist_quarantine_reason_code
ON public.checklist_task_instances
FOR EACH ROW EXECUTE FUNCTION public.checklist_guard_preconception_requiredness();

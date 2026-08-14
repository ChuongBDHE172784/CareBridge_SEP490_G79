-- V2 requiredness contract.
--
-- Pregnancy V2 leaves remain targetless, but requiredness is now an explicit
-- boolean just like the retained V1 contract.  Existing null V2 leaves are
-- resolved from their owning root template type: MANDATORY means true and
-- OPTIONAL means false.  This is a content-template policy only; no medical
-- applicability or conditional copy is inferred from item text.
UPDATE public.care_item_templates AS item
SET
    is_required = CASE root.template_type
        WHEN 'MANDATORY' THEN true
        WHEN 'OPTIONAL' THEN false
        ELSE NULL
    END
FROM public.care_item_templates AS root
WHERE
    item.entry_type = 'CHECKLIST_ENTRY'
    AND item.checklist_contract_version = 2
    AND item.is_required IS NULL
    AND root.template_id = item.parent_template_id
    AND root.entry_type = 'TEMPLATE_ROOT';

-- Materialized V2 tasks already have a non-null physical column, but keep the
-- forward migration deterministic for any nullable legacy projection that was
-- introduced before the canonical task shape was finalized.
UPDATE public.checklist_task_instances AS task
SET
    is_required = CASE
        WHEN root.template_id IS NULL THEN false -- user-created V2 task
        WHEN root.template_type = 'MANDATORY' THEN true
        WHEN root.template_type = 'OPTIONAL' THEN false
        ELSE NULL
    END
FROM public.care_item_templates AS root
WHERE
    task.checklist_contract_version = 2
    AND task.is_required IS NULL
    AND root.template_id = task.template_version_id
    AND root.entry_type = 'TEMPLATE_ROOT';

-- User-created V2 tasks have no template root.  They still need an explicit
-- boolean, and the distribution service uses the optional/default semantics
-- for such manually authored tasks.
UPDATE public.checklist_task_instances AS task
SET
    is_required = false
WHERE
    task.checklist_contract_version = 2
    AND task.is_required IS NULL
    AND NOT EXISTS (
        SELECT 1
        FROM public.care_item_templates AS root
        WHERE
            root.template_id = task.template_version_id
            AND root.entry_type = 'TEMPLATE_ROOT'
    );

DO $$
DECLARE
    unresolved_templates bigint;
    unresolved_tasks bigint;
BEGIN
    SELECT count(*)
      INTO unresolved_templates
      FROM public.care_item_templates
     WHERE entry_type = 'CHECKLIST_ENTRY'
       AND checklist_contract_version = 2
       AND is_required IS NULL
       AND checklist_quarantine_reason_code IS NULL;
    SELECT count(*)
      INTO unresolved_tasks
      FROM public.checklist_task_instances
     WHERE checklist_contract_version = 2
       AND is_required IS NULL
       AND checklist_quarantine_reason_code IS NULL;
    IF unresolved_templates > 0 OR unresolved_tasks > 0 THEN
        RAISE EXCEPTION
            'CHECKLIST_V2_REQUIREDNESS_UNRESOLVED: templates=%, tasks=%',
            unresolved_templates, unresolved_tasks;
    END IF;
END $$;

ALTER TABLE public.care_item_templates
DROP CONSTRAINT IF EXISTS checklist_template_v2_requiredness_ck,
ADD CONSTRAINT checklist_template_v2_requiredness_ck CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR entry_type <> 'CHECKLIST_ENTRY'
    OR coalesce(checklist_contract_version, 1) <> 2
    OR is_required IS NOT NULL
) NOT VALID;

ALTER TABLE public.checklist_task_instances
DROP CONSTRAINT IF EXISTS checklist_task_v2_requiredness_ck,
ADD CONSTRAINT checklist_task_v2_requiredness_ck CHECK (
    checklist_quarantine_reason_code IS NOT NULL
    OR coalesce(checklist_contract_version, 1) <> 2
    OR is_required IS NOT NULL
) NOT VALID;

ALTER TABLE public.care_item_templates VALIDATE CONSTRAINT checklist_template_v2_requiredness_ck;

ALTER TABLE public.checklist_task_instances VALIDATE CONSTRAINT checklist_task_v2_requiredness_ck;

-- Keep approval fail-closed even if a future schema change relaxes the row
-- CHECK.  This trigger is intentionally separate from the target-shape trigger
-- so the V1 target compatibility path remains unchanged.
CREATE OR REPLACE FUNCTION public.checklist_validate_v2_requiredness()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.entry_type <> 'TEMPLATE_ROOT'
       OR (NOT NEW.distribution_enabled AND NEW.content_status <> 'APPROVED') THEN
        RETURN NEW;
    END IF;
    IF EXISTS (
        SELECT 1
          FROM public.care_item_templates item
         WHERE item.parent_template_id = NEW.template_id
           AND item.entry_type = 'CHECKLIST_ENTRY'
           AND item.is_active
           AND coalesce(item.checklist_contract_version, 1) = 2
           AND item.is_required IS NULL
           AND item.checklist_quarantine_reason_code IS NULL) THEN
        RAISE EXCEPTION 'ITEM_REQUIREDNESS_REQUIRED';
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS checklist_validate_v2_requiredness_trg ON public.care_item_templates;

CREATE TRIGGER checklist_validate_v2_requiredness_trg
BEFORE INSERT OR UPDATE OF content_status, distribution_enabled, template_id
ON public.care_item_templates
FOR EACH ROW EXECUTE FUNCTION public.checklist_validate_v2_requiredness();
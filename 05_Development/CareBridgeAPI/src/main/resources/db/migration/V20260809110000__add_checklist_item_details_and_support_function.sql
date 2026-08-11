-- Checklist item detail content reuses the existing care_item_templates.description column.
-- Support functions are first-party enum codes resolved to native client routes.
ALTER TABLE public.care_item_templates
    ADD COLUMN support_function_code varchar(40);

ALTER TABLE public.checklist_task_instances
    ADD COLUMN description_snapshot text,
    ADD COLUMN support_function_code varchar(40);

-- Existing distributed tasks must retain the item description that produced them.
-- Direct assignment intentionally preserves NULL and blank source descriptions.
UPDATE public.checklist_task_instances AS task
SET description_snapshot = item.description
FROM public.care_item_templates AS item
WHERE task.template_item_version_id = item.template_id
  AND task.description_snapshot IS NULL;

ALTER TABLE public.care_item_templates
    ADD CONSTRAINT care_item_templates_support_function_ck
    CHECK (support_function_code IS NULL OR support_function_code IN (
        'HEALTH_RECORDS', 'APPOINTMENTS', 'REMINDERS', 'JOURNEY',
        'BABY_CARE', 'EXPERT_CONSULTATION', 'CONTENT_LIBRARY', 'AI_TRIAGE'
    ));

ALTER TABLE public.checklist_task_instances
    ADD CONSTRAINT checklist_task_instances_support_function_ck
    CHECK (support_function_code IS NULL OR support_function_code IN (
        'HEALTH_RECORDS', 'APPOINTMENTS', 'REMINDERS', 'JOURNEY',
        'BABY_CARE', 'EXPERT_CONSULTATION', 'CONTENT_LIBRARY', 'AI_TRIAGE'
    ));

-- Add the maternal exercise route while preserving all existing
-- checklist support-function codes and nullable legacy rows.
ALTER TABLE public.care_item_templates
    DROP CONSTRAINT IF EXISTS care_item_templates_support_function_ck,
    ADD CONSTRAINT care_item_templates_support_function_ck
    CHECK (support_function_code IS NULL OR support_function_code IN (
        'HEALTH_RECORDS', 'MATERNAL_HEALTH_METRICS', 'MATERNAL_EXERCISES',
        'APPOINTMENTS', 'REMINDERS', 'JOURNEY', 'BABY_CARE',
        'EXPERT_CONSULTATION', 'CONTENT_LIBRARY', 'AI_TRIAGE'
    ));

ALTER TABLE public.checklist_task_instances
    DROP CONSTRAINT IF EXISTS checklist_task_instances_support_function_ck,
    ADD CONSTRAINT checklist_task_instances_support_function_ck
    CHECK (support_function_code IS NULL OR support_function_code IN (
        'HEALTH_RECORDS', 'MATERNAL_HEALTH_METRICS', 'MATERNAL_EXERCISES',
        'APPOINTMENTS', 'REMINDERS', 'JOURNEY', 'BABY_CARE',
        'EXPERT_CONSULTATION', 'CONTENT_LIBRARY', 'AI_TRIAGE'
    ));

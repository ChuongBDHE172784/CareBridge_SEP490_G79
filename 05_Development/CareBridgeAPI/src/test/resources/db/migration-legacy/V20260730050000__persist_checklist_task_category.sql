ALTER TABLE public.checklist_task_instances
    ADD COLUMN category varchar(20) DEFAULT 'GENERAL' NOT NULL;

ALTER TABLE public.checklist_task_instances
    ADD CONSTRAINT checklist_task_instances_category_ck CHECK
        (category IN ('DELIVERY','PAPERWORK','BABY_CARE','GENERAL'));

-- V20260729070000 preserves legacy checklist_item_id as the V2 task id. Restore the
-- persisted category before the compatibility GET starts preferring canonical V2 rows.
UPDATE public.checklist_task_instances task
SET category = legacy.category
FROM public.preparation_checklist_items legacy
WHERE task.checklist_task_instance_id = legacy.checklist_item_id
  AND legacy.category IN ('DELIVERY','PAPERWORK','BABY_CARE','GENERAL')
  AND task.category IS DISTINCT FROM legacy.category;

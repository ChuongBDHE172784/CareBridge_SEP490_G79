-- Unified Today task metadata. Additive migration; legacy writes remain available
-- only through compatibility adapters until the V2 cutover.
ALTER TABLE public.care_tasks
    ADD COLUMN origin varchar(20),
    ADD COLUMN target_subject varchar(10);

UPDATE public.care_tasks
SET origin = CASE
        WHEN task_type = 'MANUAL_TASK' THEN 'USER_CREATED'
        WHEN source_reference_id IS NOT NULL OR source_reference_type IS NOT NULL THEN 'SYSTEM_TEMPLATE'
        ELSE 'USER_CREATED'
    END
WHERE origin IS NULL;

WITH resolved_targets AS (
    SELECT task.task_id,
           CASE
               WHEN subject.subject_type = 'BABY' THEN 'BABY'
               WHEN subject.subject_type = 'MOTHER' THEN 'MOTHER'
               WHEN task.baby_id IS NOT NULL THEN 'BABY'
               ELSE 'MOTHER'
           END AS target_subject
    FROM public.care_tasks task
    LEFT JOIN public.care_subjects subject
        ON subject.care_subject_id = task.care_subject_id
)
UPDATE public.care_tasks task
SET target_subject = resolved.target_subject
FROM resolved_targets resolved
WHERE task.task_id = resolved.task_id
  AND task.target_subject IS NULL;

ALTER TABLE public.care_tasks
    ALTER COLUMN origin SET DEFAULT 'USER_CREATED',
    ALTER COLUMN target_subject SET DEFAULT 'MOTHER',
    ALTER COLUMN origin SET NOT NULL,
    ALTER COLUMN target_subject SET NOT NULL,
    ADD CONSTRAINT care_tasks_origin_ck CHECK (origin IN ('SYSTEM_TEMPLATE','USER_CREATED')),
    ADD CONSTRAINT care_tasks_target_subject_ck CHECK (target_subject IN ('MOTHER','BABY'));

CREATE INDEX care_tasks_today_scope_ix
    ON public.care_tasks(assignee_user_id, status, scheduled_at, care_group_id);

-- Stage-scoped checklist history marker.
-- Existing checklist tables stay intact; children retain their original status evidence.
-- Preserves checklist_instances, checklist_task_instances, and checklist_action_commands.

ALTER TABLE public.checklist_instances
    ADD COLUMN historical_at timestamp with time zone;

ALTER TABLE public.checklist_instances
    ADD COLUMN history_reason_code varchar(80);

ALTER TABLE public.checklist_instances
    ADD CONSTRAINT checklist_instances_history_pair_ck CHECK (
        (historical_at IS NULL AND history_reason_code IS NULL)
        OR (historical_at IS NOT NULL AND history_reason_code IS NOT NULL)
    ),
    ADD CONSTRAINT checklist_instances_history_reason_ck CHECK (
        history_reason_code IS NULL
        OR (length(history_reason_code) <= 80
            AND history_reason_code LIKE 'LIFECYCLE_STAGE_OBSOLETE%')
    );

CREATE INDEX checklist_instances_owner_history_ix
    ON public.checklist_instances (context_owner_user_id, historical_at DESC)
    WHERE historical_at IS NOT NULL;

CREATE INDEX checklist_instances_owner_current_ix
    ON public.checklist_instances (context_owner_user_id, updated_at DESC)
    WHERE historical_at IS NULL;

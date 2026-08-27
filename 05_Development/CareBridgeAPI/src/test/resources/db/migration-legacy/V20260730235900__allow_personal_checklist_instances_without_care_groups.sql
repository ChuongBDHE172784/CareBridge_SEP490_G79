-- Reconciliation candidates mirror the recipient context. Personal recipients
-- have a canonical context but intentionally have no group.
ALTER TABLE public.checklist_reconciliation_candidates
    DROP CONSTRAINT IF EXISTS checklist_reconciliation_candidates_context_ck,
    ADD CONSTRAINT checklist_reconciliation_candidates_context_ck CHECK (
        (recipient_user_id IS NULL AND care_group_id IS NULL
            AND care_context_type IS NULL AND care_context_id IS NULL) OR
        (recipient_user_id IS NOT NULL
            AND care_context_type IN ('JOURNEY','BABY') AND care_context_id IS NOT NULL)
    );

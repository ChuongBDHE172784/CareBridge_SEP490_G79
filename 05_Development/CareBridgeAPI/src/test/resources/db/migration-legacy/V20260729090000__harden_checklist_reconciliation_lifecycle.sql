-- Reconciliation lifecycle hardening. This feature remains disabled until the migration chain completes.
ALTER TABLE public.checklist_reconciliation_runs
    DROP CONSTRAINT checklist_reconciliation_runs_trigger_ck,
    ADD CONSTRAINT checklist_reconciliation_runs_trigger_ck
        CHECK (trigger_type IN ('EVENT','HOURLY','STARTUP','MANUAL','REPLAY'));

ALTER TABLE public.checklist_reconciliation_candidates
    ADD COLUMN template_version_id uuid,
    ADD COLUMN source_correlation_id uuid;

-- Earlier phase candidates predate source/template identity. The run correlation is
-- recoverable; template identity is intentionally left NULL instead of fabricated.
UPDATE public.checklist_reconciliation_candidates AS candidate
SET source_correlation_id = run.correlation_id
FROM public.checklist_reconciliation_runs AS run
WHERE run.reconciliation_run_id = candidate.reconciliation_run_id
  AND candidate.source_correlation_id IS NULL;

ALTER TABLE public.checklist_reconciliation_candidates
    DROP CONSTRAINT checklist_reconciliation_candidates_outcome_ck,
    ADD CONSTRAINT checklist_reconciliation_candidates_outcome_ck CHECK
        (outcome IN ('PENDING','CREATED','EXISTING','CANCELLED','INELIGIBLE','FAILED','QUARANTINED')),
    DROP CONSTRAINT checklist_reconciliation_candidates_identity_uk,
    ADD CONSTRAINT checklist_reconciliation_candidates_identity_uk UNIQUE NULLS NOT DISTINCT
        (reconciliation_run_id, template_version_id, source_correlation_id,
         recipient_user_id, care_group_id, care_context_type, care_context_id, window_start, window_end);

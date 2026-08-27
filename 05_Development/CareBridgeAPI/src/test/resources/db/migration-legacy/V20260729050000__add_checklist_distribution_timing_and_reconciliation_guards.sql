-- Checklist Distribution V2 Phase 3: versioned item timing and durable reconciliation guards.

ALTER TABLE public.care_item_templates
    ADD COLUMN due_anchor_type varchar(30),
    ADD COLUMN due_offset_start integer,
    ADD COLUMN due_offset_end integer,
    ADD COLUMN due_offset_unit varchar(10);

ALTER TABLE public.care_item_templates
    ADD CONSTRAINT care_item_templates_due_timing_ck CHECK (
        entry_type <> 'CHECKLIST_ENTRY'
        OR (
            due_anchor_type IS NULL
            AND due_offset_start IS NULL
            AND due_offset_end IS NULL
            AND due_offset_unit IS NULL
        )
        OR (
            due_anchor_type IS NOT NULL
            AND due_offset_start IS NOT NULL
            AND due_offset_end IS NOT NULL
            AND due_offset_unit IS NOT NULL
            AND due_anchor_type IN ('LMP', 'EDD', 'DELIVERY_DATE', 'BIRTH_DATE')
            AND due_offset_start >= 0
            AND due_offset_end >= due_offset_start
            AND due_offset_unit IN ('DAY', 'WEEK', 'MONTH')
        )
    );

ALTER TABLE public.checklist_distribution_outbox
    ADD CONSTRAINT checklist_distribution_outbox_event_uk UNIQUE
        (correlation_id, event_type, aggregate_type, aggregate_id),
    ADD CONSTRAINT checklist_distribution_outbox_retry_ck CHECK (
        attempt_count BETWEEN 0 AND 5
        AND (processed_at IS NULL OR last_error_code IS NULL)
    );

CREATE INDEX checklist_distribution_outbox_retry_ix
    ON public.checklist_distribution_outbox(next_attempt_at, occurred_at, outbox_event_id)
    WHERE processed_at IS NULL AND attempt_count < 5;

CREATE INDEX checklist_distribution_outbox_exhausted_ix
    ON public.checklist_distribution_outbox(occurred_at, correlation_id)
    WHERE processed_at IS NULL AND attempt_count >= 5;

CREATE INDEX checklist_reconciliation_success_watermark_ix
    ON public.checklist_reconciliation_runs(completed_at DESC, reconciliation_run_id)
    WHERE status = 'SUCCEEDED';

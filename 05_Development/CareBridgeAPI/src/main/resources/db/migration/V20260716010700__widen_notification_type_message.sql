-- CB-EXPCHAT-IMP-001 / ADR-MEDI-004 mục 1 — add NotificationType.MESSAGE for direct-chat alerts.
-- Same widen pattern used repeatedly for audit_logs_action_check in prior migrations.
ALTER TABLE public.notification_records
    DROP CONSTRAINT IF EXISTS notification_records_type_check;

ALTER TABLE public.notification_records
    ADD CONSTRAINT notification_records_type_check CHECK (
        (type)::text = ANY ((ARRAY[
            'REMINDER', 'COMMUNITY_REPLY', 'CONSULTATION', 'EMERGENCY', 'MESSAGE'
        ])::text[])
    );

ALTER TABLE public.notification_records
    DROP CONSTRAINT IF EXISTS notification_records_status_check;

ALTER TABLE public.notification_records
    ADD CONSTRAINT notification_records_status_check CHECK (
        (status)::text = ANY ((ARRAY[
            'PENDING', 'PROCESSING', 'SENT', 'DELIVERED', 'FAILED'
        ])::text[])
    );

-- Lease timestamp for the durable direct-message notification outbox. This must be
-- separate from created_at: an old PENDING row may be claimed for the first time
-- long after creation and must still receive a fresh, exclusive processing lease.
ALTER TABLE public.notification_records
    ADD COLUMN processing_started_at timestamptz NULL;

-- CB-EPDS-IMP-001 — allow EPDS_RESULT notifications (EPDS screening results to care-group family).
-- Widening a CHECK constraint is backward compatible: an older binary simply never writes the value.
ALTER TABLE public.notification_records
    DROP CONSTRAINT IF EXISTS notification_records_type_check;

ALTER TABLE public.notification_records
    ADD CONSTRAINT notification_records_type_check CHECK (type IN (
        'REMINDER',
        'COMMUNITY_REPLY',
        'CONSULTATION',
        'EMERGENCY',
        'LOCATION_SHARE',
        'MESSAGE',
        'GROUP_INVITE',
        'CONTENT_REVIEW',
        'EPDS_RESULT'
    ));

-- CB-CONREQ-IMP-001 / ADR-CONREQ-007.
-- A consultation request can produce multiple lifecycle notifications for the same recipient.
-- Include metadata.eventType in the uniqueness grain so redelivery of one event is deduplicated
-- without suppressing a different event for the same request.
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_records_consultation_request
    ON public.notification_records (user_id, reference_id, ((metadata ->> 'eventType')))
    WHERE type = 'CONSULTATION' AND reference_type = 'CONSULTATION_REQUEST';

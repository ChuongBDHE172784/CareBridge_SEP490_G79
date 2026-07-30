ALTER TABLE public.safety_events
    ALTER COLUMN alert_generation SET DEFAULT 0,
    ALTER COLUMN alert_successful_recipient_count SET DEFAULT 0,
    ALTER COLUMN alert_failed_recipient_count SET DEFAULT 0;

ALTER TABLE public.safety_events
    DROP CONSTRAINT IF EXISTS safety_events_action_type_ck;

ALTER TABLE public.safety_events
    ADD CONSTRAINT safety_events_action_type_ck
    CHECK (
        action_type IS NULL
        OR action_type IN (
            'RESPONSE',
            'DELIVERY',
            'FAMILY_ALERT',
            'ALERT_ATTEMPT',
            'MAP_HANDOFF',
            'LOCATION_SNAPSHOT',
            'TRIAGE_ESCALATION'
        )
    );

DROP INDEX IF EXISTS public.safety_events_attempt_event_uk;
CREATE UNIQUE INDEX safety_events_attempt_event_uk
    ON public.safety_events (parent_event_id, alert_generation, action_phase)
    WHERE action_type = 'ALERT_ATTEMPT';

DROP INDEX IF EXISTS public.safety_events_delivery_token_uk;
CREATE UNIQUE INDEX safety_events_delivery_token_uk
    ON public.safety_events (
        parent_event_id,
        alert_generation,
        device_token_id,
        action_phase
    )
    WHERE action_type = 'DELIVERY';

DROP INDEX IF EXISTS public.safety_events_family_alert_uk;
CREATE UNIQUE INDEX safety_events_family_alert_uk
    ON public.safety_events (parent_event_id, alert_generation)
    WHERE action_type = 'FAMILY_ALERT';

ALTER TABLE public.mother_journey_transitions
    DROP CONSTRAINT chk_mother_journey_transition_event_type;

ALTER TABLE public.mother_journey_transitions
    ADD CONSTRAINT chk_mother_journey_transition_event_type
        CHECK (event_type IN (
            'CREATED', 'STAGE_CHANGED', 'DATES_CHANGED', 'DETAILS_CHANGED',
            'STATUS_CHANGED', 'MIGRATED'
        ));

CREATE OR REPLACE FUNCTION public.reject_mother_journey_transition_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF current_setting('carebridge.allow_transition_mutation', true) = 'on' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'mother_journey_transitions is append-only';
END;
$$;

CREATE TRIGGER trg_mother_journey_transitions_append_only
BEFORE UPDATE OR DELETE ON public.mother_journey_transitions
FOR EACH ROW
EXECUTE FUNCTION public.reject_mother_journey_transition_mutation();

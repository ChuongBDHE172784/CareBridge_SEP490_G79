ALTER TABLE public.mother_journeys
    ADD COLUMN version bigint NOT NULL DEFAULT 0,
    ADD COLUMN date_source varchar(30),
    ADD COLUMN date_confidence varchar(20);

ALTER TABLE public.mother_journeys
    ADD CONSTRAINT chk_mother_journeys_date_source
        CHECK (date_source IS NULL OR date_source IN (
            'SELF_REPORTED', 'CLINICIAN_CONFIRMED', 'ULTRASOUND',
            'SYSTEM_DERIVED', 'MIGRATION', 'UNKNOWN'
        )),
    ADD CONSTRAINT chk_mother_journeys_date_confidence
        CHECK (date_confidence IS NULL OR date_confidence IN (
            'CONFIRMED', 'ESTIMATED', 'UNKNOWN'
        ));

DO $$
BEGIN
  IF EXISTS (
    SELECT owner_user_id
    FROM public.mother_journeys
    WHERE status = 'ACTIVE'
      AND journey_type IN ('PRE_PREGNANCY', 'PREGNANCY', 'POSTPARTUM')
    GROUP BY owner_user_id
    HAVING count(*) > 1
  ) THEN
    RAISE EXCEPTION 'Canonical journey migration blocked: duplicate ACTIVE lifecycle rows';
  END IF;
END $$;

CREATE UNIQUE INDEX uq_mother_journeys_one_canonical_active
ON public.mother_journeys(owner_user_id)
WHERE status = 'ACTIVE'
  AND journey_type IN ('PRE_PREGNANCY', 'PREGNANCY', 'POSTPARTUM');

CREATE TABLE public.mother_journey_transitions (
    transition_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    journey_id uuid NOT NULL REFERENCES public.mother_journeys(journey_id),
    event_type varchar(30) NOT NULL,
    from_stage varchar(20),
    to_stage varchar(20),
    changes_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    source varchar(30) NOT NULL,
    confidence varchar(20),
    reason varchar(500),
    actor_user_id uuid REFERENCES public.users(user_id),
    effective_at timestamptz NOT NULL,
    recorded_at timestamptz NOT NULL DEFAULT now(),
    journey_version bigint NOT NULL,
    CONSTRAINT chk_mother_journey_transition_event_type
        CHECK (event_type IN (
            'CREATED', 'STAGE_CHANGED', 'DATES_CHANGED',
            'STATUS_CHANGED', 'MIGRATED'
        )),
    CONSTRAINT chk_mother_journey_transition_source
        CHECK (source IN (
            'SELF_REPORTED', 'CLINICIAN_CONFIRMED', 'ULTRASOUND',
            'SYSTEM_DERIVED', 'MIGRATION', 'UNKNOWN'
        )),
    CONSTRAINT chk_mother_journey_transition_confidence
        CHECK (confidence IS NULL OR confidence IN (
            'CONFIRMED', 'ESTIMATED', 'UNKNOWN'
        )),
    CONSTRAINT uq_mother_journey_transition_version
        UNIQUE (journey_id, journey_version)
);

CREATE INDEX idx_mother_journey_transitions_journey_time
ON public.mother_journey_transitions(journey_id, recorded_at DESC);

INSERT INTO public.mother_journey_transitions (
    journey_id,
    event_type,
    from_stage,
    to_stage,
    changes_json,
    source,
    confidence,
    reason,
    actor_user_id,
    effective_at,
    journey_version
)
SELECT
    journey_id,
    'MIGRATED',
    NULL,
    journey_type,
    '{}'::jsonb,
    'MIGRATION',
    'UNKNOWN',
    'STORY_6_1_BASELINE',
    NULL,
    created_at,
    version
FROM public.mother_journeys
WHERE journey_type IN ('PRE_PREGNANCY', 'PREGNANCY', 'POSTPARTUM');

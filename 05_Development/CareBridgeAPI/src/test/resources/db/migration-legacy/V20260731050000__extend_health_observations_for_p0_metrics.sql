ALTER TABLE public.health_observations
    ADD COLUMN IF NOT EXISTS period_start timestamptz,
    ADD COLUMN IF NOT EXISTS period_end timestamptz,
    ADD COLUMN IF NOT EXISTS context_jsonb jsonb DEFAULT '{}'::jsonb NOT NULL,
    ADD COLUMN IF NOT EXISTS original_unit varchar(30),
    ADD COLUMN IF NOT EXISTS definition_version integer,
    ADD COLUMN IF NOT EXISTS observation_shape varchar(30);

ALTER TABLE public.health_observations
    ADD CONSTRAINT health_observations_p0_period_ck CHECK (
        period_end IS NULL OR (period_start IS NOT NULL AND period_end > period_start)
    );

ALTER TABLE public.health_observations
    ADD CONSTRAINT health_observations_p0_context_json_ck CHECK (
        jsonb_typeof(context_jsonb) = 'object'
    );

CREATE INDEX IF NOT EXISTS health_observations_p0_subject_metric_time_ix
    ON public.health_observations(care_subject_id, observation_type, observed_at)
    WHERE legacy_source = 'maternal_health_observations';

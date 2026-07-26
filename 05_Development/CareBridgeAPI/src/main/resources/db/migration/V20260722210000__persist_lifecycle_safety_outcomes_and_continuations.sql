-- Story 6.7: durable lifecycle safety projections and restart-safe continuations.

CREATE UNIQUE INDEX IF NOT EXISTS uq_mother_journeys_id_owner
    ON mother_journeys (journey_id, owner_user_id);

ALTER TABLE intake_sessions
    ADD COLUMN IF NOT EXISTS journey_id UUID,
    ADD COLUMN IF NOT EXISTS origin_dashboard VARCHAR(30),
    ADD COLUMN IF NOT EXISTS origin_reference_id UUID,
    ADD COLUMN IF NOT EXISTS continuation_token UUID,
    ADD COLUMN IF NOT EXISTS continuation_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS continuation_acknowledged_at TIMESTAMPTZ;

ALTER TABLE intake_sessions DROP CONSTRAINT IF EXISTS chk_intake_stage;
ALTER TABLE intake_sessions
    ADD CONSTRAINT chk_intake_stage CHECK (
        stage IN ('PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM', 'INFANT', 'TODDLER')),
    ADD CONSTRAINT chk_intake_lifecycle_binding CHECK (
        (journey_id IS NULL AND origin_dashboard IS NULL AND origin_reference_id IS NULL
            AND continuation_token IS NULL AND continuation_expires_at IS NULL
            AND continuation_acknowledged_at IS NULL)
        OR
        (journey_id IS NOT NULL AND origin_dashboard IS NOT NULL AND origin_reference_id IS NOT NULL
            AND continuation_token IS NOT NULL AND continuation_expires_at IS NOT NULL)),
    ADD CONSTRAINT chk_intake_origin_dashboard CHECK (
        origin_dashboard IS NULL OR origin_dashboard IN ('MOTHER_JOURNEY', 'BABY_PROFILE')),
    ADD CONSTRAINT chk_intake_origin_stage CHECK (
        origin_dashboard IS NULL
        OR (origin_dashboard = 'MOTHER_JOURNEY'
            AND origin_reference_id = journey_id
            AND stage IN ('PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM'))
        OR (origin_dashboard = 'BABY_PROFILE' AND stage IN ('INFANT', 'TODDLER')));

ALTER TABLE intake_sessions
    ADD CONSTRAINT fk_intake_journey_owner
    FOREIGN KEY (journey_id, user_id)
    REFERENCES mother_journeys (journey_id, owner_user_id)
    ON DELETE RESTRICT;

CREATE UNIQUE INDEX IF NOT EXISTS uq_intake_sessions_continuation_token
    ON intake_sessions (continuation_token)
    WHERE continuation_token IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_intake_sessions_journey
    ON intake_sessions (journey_id, completed_at DESC)
    WHERE journey_id IS NOT NULL;

CREATE TABLE lifecycle_safety_outcomes (
    outcome_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL,
    journey_id UUID NOT NULL,
    intake_session_id UUID NOT NULL UNIQUE,
    emergency_session_id UUID,
    risk_level VARCHAR(10) NOT NULL,
    stage VARCHAR(20) NOT NULL,
    origin_dashboard VARCHAR(30) NOT NULL,
    origin_reference_id UUID NOT NULL,
    origin_action VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_safety_journey_owner FOREIGN KEY (journey_id, owner_user_id)
        REFERENCES mother_journeys (journey_id, owner_user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_safety_intake_owner FOREIGN KEY (intake_session_id, owner_user_id)
        REFERENCES intake_sessions (id, user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_safety_emergency_owner FOREIGN KEY (emergency_session_id, owner_user_id)
        REFERENCES emergency_sessions (id, user_id) ON DELETE RESTRICT,
    CONSTRAINT chk_safety_risk CHECK (risk_level IN ('GREEN', 'YELLOW', 'RED')),
    CONSTRAINT chk_safety_stage CHECK (
        stage IN ('PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM', 'INFANT', 'TODDLER')),
    CONSTRAINT chk_safety_origin CHECK (
        origin_dashboard IN ('MOTHER_JOURNEY', 'BABY_PROFILE')),
    CONSTRAINT chk_safety_action CHECK (
        origin_action IN ('RETURN_TO_MOTHER_JOURNEY', 'RETURN_TO_BABY_PROFILE')),
    CONSTRAINT chk_safety_origin_action CHECK (
        (origin_dashboard = 'MOTHER_JOURNEY'
            AND origin_action = 'RETURN_TO_MOTHER_JOURNEY'
            AND origin_reference_id = journey_id
            AND stage IN ('PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM'))
        OR
        (origin_dashboard = 'BABY_PROFILE'
            AND origin_action = 'RETURN_TO_BABY_PROFILE'
            AND stage IN ('INFANT', 'TODDLER'))),
    CONSTRAINT chk_safety_emergency_risk CHECK (
        emergency_session_id IS NULL OR risk_level = 'RED')
);

CREATE INDEX idx_safety_journey_timeline
    ON lifecycle_safety_outcomes
        (journey_id, occurred_at DESC, recorded_at DESC, outcome_id DESC);

CREATE OR REPLACE FUNCTION reject_lifecycle_safety_outcome_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'lifecycle_safety_outcomes is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_lifecycle_safety_outcomes_append_only
BEFORE UPDATE OR DELETE ON lifecycle_safety_outcomes
FOR EACH ROW EXECUTE FUNCTION reject_lifecycle_safety_outcome_mutation();

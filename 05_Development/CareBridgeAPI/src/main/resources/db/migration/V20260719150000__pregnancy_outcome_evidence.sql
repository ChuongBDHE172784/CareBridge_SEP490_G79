ALTER TABLE public.mother_journeys
    ADD COLUMN pregnancy_outcome VARCHAR(30),
    ADD COLUMN pregnancy_outcome_date DATE,
    ADD CONSTRAINT ck_mother_journey_pregnancy_outcome CHECK (
        pregnancy_outcome IS NULL OR pregnancy_outcome IN (
            'ONGOING', 'UNKNOWN', 'LIVE_BIRTH', 'PREGNANCY_LOSS', 'STILLBIRTH')),
    ADD CONSTRAINT ck_mother_journey_live_birth_date CHECK (
        pregnancy_outcome <> 'LIVE_BIRTH' OR pregnancy_outcome_date IS NOT NULL);

ALTER TABLE public.mother_journey_transitions
    DROP CONSTRAINT chk_mother_journey_transition_event_type;

ALTER TABLE public.mother_journey_transitions
    ADD CONSTRAINT chk_mother_journey_transition_event_type CHECK (
        event_type IN (
            'CREATED', 'STAGE_CHANGED', 'DATES_CHANGED', 'DETAILS_CHANGED',
            'STATUS_CHANGED', 'OUTCOME_RECORDED', 'OUTCOME_CORRECTED', 'MIGRATED'));

CREATE TABLE public.pregnancy_outcome_evidence (
    evidence_id UUID PRIMARY KEY,
    journey_id UUID NOT NULL REFERENCES mother_journeys(journey_id),
    owner_user_id UUID NOT NULL,
    submission_id UUID NOT NULL,
    outcome_type VARCHAR(30) NOT NULL,
    outcome_date DATE,
    source VARCHAR(30) NOT NULL,
    actor_user_id UUID NOT NULL,
    reason VARCHAR(500) NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revision_number INTEGER NOT NULL,
    supersedes_evidence_id UUID REFERENCES pregnancy_outcome_evidence(evidence_id),
    journey_version BIGINT NOT NULL,
    semantic_hash VARCHAR(500) NOT NULL,
    correction BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_pregnancy_outcome_submission UNIQUE (journey_id, submission_id),
    CONSTRAINT uq_pregnancy_outcome_revision UNIQUE (journey_id, revision_number),
    CONSTRAINT ck_pregnancy_outcome_type CHECK (
        outcome_type IN ('ONGOING', 'UNKNOWN', 'LIVE_BIRTH', 'PREGNANCY_LOSS', 'STILLBIRTH')),
    CONSTRAINT ck_live_birth_outcome_date CHECK (
        outcome_type <> 'LIVE_BIRTH' OR outcome_date IS NOT NULL),
    CONSTRAINT ck_pregnancy_outcome_revision_positive CHECK (revision_number > 0),
    CONSTRAINT ck_pregnancy_outcome_reason CHECK (length(trim(reason)) > 0)
);

CREATE INDEX idx_pregnancy_outcome_journey_revision
    ON public.pregnancy_outcome_evidence (journey_id, revision_number DESC);

CREATE INDEX idx_pregnancy_outcome_owner
    ON public.pregnancy_outcome_evidence (owner_user_id);

CREATE OR REPLACE FUNCTION reject_pregnancy_outcome_evidence_mutation()
RETURNS trigger AS $$
BEGIN
    IF current_setting('carebridge.allow_outcome_mutation', true) = 'on' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'pregnancy outcome evidence is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_pregnancy_outcome_evidence_no_update
BEFORE UPDATE OR DELETE ON public.pregnancy_outcome_evidence
FOR EACH ROW EXECUTE FUNCTION reject_pregnancy_outcome_evidence_mutation();

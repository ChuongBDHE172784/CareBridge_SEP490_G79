-- Story 6.8: immutable, consented YELLOW triage context shared with a verified expert.

CREATE UNIQUE INDEX uq_consultation_requests_integrity
    ON consultation_requests (id, requester_user_id, expert_profile_id, client_request_id);

CREATE UNIQUE INDEX uq_consent_grants_integrity
    ON consent_grants (id, user_id, evidence_key);

CREATE UNIQUE INDEX uq_intake_handoff_integrity
    ON intake_sessions
        (id, user_id, journey_id, origin_dashboard, origin_reference_id,
         stage, risk_level, status);

CREATE TABLE consultation_context_shares (
    context_share_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_request_id UUID NOT NULL UNIQUE,
    owner_user_id UUID NOT NULL,
    intake_session_id UUID NOT NULL,
    expert_profile_id UUID NOT NULL,
    consent_grant_id BIGINT NOT NULL UNIQUE,
    idempotency_key UUID NOT NULL,
    journey_id UUID NOT NULL,
    origin_dashboard VARCHAR(30) NOT NULL,
    origin_reference_id UUID NOT NULL,
    triage_stage VARCHAR(20) NOT NULL,
    risk_level VARCHAR(10) NOT NULL,
    intake_status VARCHAR(20) NOT NULL,
    risk_summary VARCHAR(500) NOT NULL,
    share_policy_version VARCHAR(60) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_context_owner_key UNIQUE (owner_user_id, idempotency_key),
    CONSTRAINT uq_context_intake_expert
        UNIQUE (owner_user_id, intake_session_id, expert_profile_id),
    CONSTRAINT fk_context_request_integrity FOREIGN KEY
        (consultation_request_id, owner_user_id, expert_profile_id, idempotency_key)
        REFERENCES consultation_requests
            (id, requester_user_id, expert_profile_id, client_request_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_context_intake_snapshot FOREIGN KEY
        (intake_session_id, owner_user_id, journey_id, origin_dashboard,
         origin_reference_id, triage_stage, risk_level, intake_status)
        REFERENCES intake_sessions
            (id, user_id, journey_id, origin_dashboard, origin_reference_id,
             stage, risk_level, status)
        ON DELETE RESTRICT,
    CONSTRAINT fk_context_journey_owner FOREIGN KEY (journey_id, owner_user_id)
        REFERENCES mother_journeys (journey_id, owner_user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_context_expert FOREIGN KEY (expert_profile_id)
        REFERENCES expert_profiles (expert_profile_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_context_consent_integrity FOREIGN KEY
        (consent_grant_id, owner_user_id, idempotency_key)
        REFERENCES consent_grants (id, user_id, evidence_key)
        ON DELETE RESTRICT,
    CONSTRAINT chk_context_yellow CHECK (risk_level = 'YELLOW'),
    CONSTRAINT chk_context_completed CHECK (intake_status = 'COMPLETED'),
    CONSTRAINT chk_context_origin
        CHECK (origin_dashboard IN ('MOTHER_JOURNEY', 'BABY_PROFILE')),
    CONSTRAINT chk_context_stage CHECK
        (triage_stage IN
            ('PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM', 'INFANT', 'TODDLER')),
    CONSTRAINT chk_context_summary
        CHECK (length(btrim(risk_summary)) BETWEEN 1 AND 500),
    CONSTRAINT chk_context_policy
        CHECK (share_policy_version = 'YELLOW_EXPERT_CONTEXT_V1')
);

CREATE INDEX idx_context_shares_owner_created
    ON consultation_context_shares (owner_user_id, created_at DESC);

CREATE INDEX idx_context_shares_expert_created
    ON consultation_context_shares (expert_profile_id, created_at DESC);

CREATE INDEX idx_context_shares_participant_request
    ON consultation_context_shares
        (consultation_request_id, owner_user_id, expert_profile_id);

CREATE TABLE consultation_context_citations (
    citation_snapshot_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    context_share_id UUID NOT NULL,
    evidence_source_id UUID NOT NULL,
    organization VARCHAR(255) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    source_status_at_share VARCHAR(30) NOT NULL,
    reviewed_at TIMESTAMPTZ NOT NULL,
    ordinal SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_context_citation_share FOREIGN KEY (context_share_id)
        REFERENCES consultation_context_shares (context_share_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_context_citation_source FOREIGN KEY (evidence_source_id)
        REFERENCES evidence_sources (id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_context_citation_source
        UNIQUE (context_share_id, evidence_source_id),
    CONSTRAINT chk_context_citation_approved
        CHECK (source_status_at_share = 'APPROVED'),
    CONSTRAINT chk_context_citation_https CHECK (source_url LIKE 'https://%'),
    CONSTRAINT chk_context_citation_ordinal CHECK (ordinal >= 0)
);

CREATE INDEX idx_context_citations_share_ordinal
    ON consultation_context_citations (context_share_id, ordinal, citation_snapshot_id);

CREATE OR REPLACE FUNCTION reject_consultation_context_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_consultation_context_shares_append_only
BEFORE UPDATE OR DELETE ON consultation_context_shares
FOR EACH ROW EXECUTE FUNCTION reject_consultation_context_mutation();

CREATE TRIGGER trg_consultation_context_citations_append_only
BEFORE UPDATE OR DELETE ON consultation_context_citations
FOR EACH ROW EXECUTE FUNCTION reject_consultation_context_mutation();

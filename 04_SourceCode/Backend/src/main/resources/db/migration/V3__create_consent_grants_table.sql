CREATE TABLE consent_grants (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    data_type VARCHAR(60) NOT NULL,
    purpose VARCHAR(60) NOT NULL,
    recipient VARCHAR(120),
    scope_text TEXT,
    consent_given_at TIMESTAMPTZ NOT NULL,
    expiry_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revoked_by BIGINT,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_consent_grants_user_id ON consent_grants (user_id);
CREATE INDEX idx_consent_grants_valid_lookup ON consent_grants (user_id, data_type, purpose, expiry_at);
CREATE INDEX idx_consent_grants_revoked_at ON consent_grants (revoked_at);

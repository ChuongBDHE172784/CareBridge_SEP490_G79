CREATE TABLE security_events (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    event_type VARCHAR(80) NOT NULL,
    user_id BIGINT,
    ip_address VARCHAR(80),
    details TEXT
);

CREATE INDEX idx_security_events_timestamp ON security_events (timestamp);
CREATE INDEX idx_security_events_event_type ON security_events (event_type);
CREATE INDEX idx_security_events_user_id ON security_events (user_id);

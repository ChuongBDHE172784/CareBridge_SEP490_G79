CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id BIGINT,
    action VARCHAR(80) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(120),
    details TEXT,
    ip_address VARCHAR(80),
    user_agent VARCHAR(500)
);

CREATE INDEX idx_audit_logs_timestamp ON audit_logs (timestamp);
CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_resource ON audit_logs (resource_type, resource_id);

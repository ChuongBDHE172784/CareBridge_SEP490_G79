CREATE TABLE system_configurations (
    system_configuration_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    api_rate_limit INTEGER NOT NULL CHECK (api_rate_limit BETWEEN 1 AND 100000),
    connection_timeout_ms INTEGER NOT NULL CHECK (connection_timeout_ms BETWEEN 1000 AND 300000),
    max_upload_size_mb INTEGER NOT NULL CHECK (max_upload_size_mb BETWEEN 1 AND 1024),
    administrator_email VARCHAR(254) NOT NULL,
    email_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    sms_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    webhook_alerts BOOLEAN NOT NULL DEFAULT FALSE,
    ai_moderation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    maintenance_mode_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by UUID NOT NULL REFERENCES users(user_id),
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE system_configurations IS 'Single active, SYSTEM_ADMIN-managed configuration record for platform operational settings.';

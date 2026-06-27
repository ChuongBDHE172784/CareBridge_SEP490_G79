CREATE TABLE safety_monitoring_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    fall_detection_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    sensitivity_level VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    emergency_auto_alert BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT chk_sensitivity_level CHECK (sensitivity_level IN ('LOW', 'MEDIUM', 'HIGH'))
);

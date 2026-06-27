CREATE TABLE imu_monitoring_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    sensitivity_level VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMPTZ,
    created_by UUID,
    CONSTRAINT chk_imu_status CHECK (status IN ('ACTIVE', 'STOPPED'))
);

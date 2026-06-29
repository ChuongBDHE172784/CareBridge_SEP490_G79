CREATE TABLE IF NOT EXISTS emergency_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    trigger_source VARCHAR(50) NOT NULL,
    user_latitude DECIMAL(10,7),
    user_longitude DECIMAL(10,7),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    created_by UUID,
    CONSTRAINT chk_emergency_status CHECK (status IN ('ACTIVE', 'RESOLVED', 'CANCELLED')),
    CONSTRAINT chk_trigger_source CHECK (trigger_source IN ('MANUAL', 'AUTO_TRIAGE', 'FALL_DETECTION'))
);

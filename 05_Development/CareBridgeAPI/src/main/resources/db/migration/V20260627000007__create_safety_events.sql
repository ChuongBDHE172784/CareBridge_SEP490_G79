CREATE TABLE imu_safety_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    imu_session_id UUID NOT NULL REFERENCES imu_monitoring_sessions(id),
    event_type VARCHAR(20) NOT NULL,
    magnitude NUMERIC(10,4) NOT NULL,
    user_latitude NUMERIC(10,7),
    user_longitude NUMERIC(10,7),
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notes TEXT,
    created_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT chk_event_type CHECK (event_type IN ('SUSPECTED_FALL', 'SUSPECTED_IMPACT', 'FALSE_ALARM'))
);
REVOKE UPDATE, DELETE ON imu_safety_events FROM PUBLIC;

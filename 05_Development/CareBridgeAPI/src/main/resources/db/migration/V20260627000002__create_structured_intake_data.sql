CREATE TABLE IF NOT EXISTS structured_intake_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL UNIQUE REFERENCES intake_sessions(id),
    symptom_list JSONB NOT NULL,
    duration_days INT,
    intensity VARCHAR(20),
    emergency_flag BOOLEAN NOT NULL DEFAULT FALSE,
    extracted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT chk_intensity CHECK (intensity IN ('LOW', 'MEDIUM', 'HIGH'))
);

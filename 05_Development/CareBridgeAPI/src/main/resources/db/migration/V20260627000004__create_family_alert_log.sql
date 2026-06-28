CREATE TABLE IF NOT EXISTS family_alert_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL UNIQUE REFERENCES emergency_sessions(id),
    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    recipient_count INT NOT NULL DEFAULT 0,
    location_included BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM'
);

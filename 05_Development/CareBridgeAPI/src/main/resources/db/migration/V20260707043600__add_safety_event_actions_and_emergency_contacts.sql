ALTER TABLE imu_safety_events
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_imu_safety_events_user_detected_at
    ON imu_safety_events(user_id, detected_at DESC);

CREATE TABLE IF NOT EXISTS emergency_contacts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    relationship VARCHAR(80),
    primary_contact BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_emergency_contacts_user_id
    ON emergency_contacts(user_id);

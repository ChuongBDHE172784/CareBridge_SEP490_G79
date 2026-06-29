CREATE TABLE IF NOT EXISTS intake_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    baby_profile_id UUID,
    symptoms TEXT NOT NULL,
    raw_ai_response TEXT,
    risk_level VARCHAR(10),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    disclaimer TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    created_by UUID NOT NULL,
    CONSTRAINT chk_risk_level CHECK (risk_level IN ('GREEN', 'YELLOW', 'RED')),
    CONSTRAINT chk_intake_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

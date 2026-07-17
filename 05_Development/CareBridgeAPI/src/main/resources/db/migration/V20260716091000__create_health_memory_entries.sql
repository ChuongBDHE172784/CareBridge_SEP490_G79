CREATE TABLE IF NOT EXISTS health_memory_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    mother_profile_id UUID,
    baby_profile_id UUID,
    related_stage VARCHAR(20) NOT NULL,
    summary_text TEXT NOT NULL,
    source_session_id UUID REFERENCES intake_sessions(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_health_memory_stage CHECK (related_stage IN ('PRECONCEPTION', 'PREGNANCY', 'INFANT', 'TODDLER')),
    CONSTRAINT chk_health_memory_profile CHECK (
        (related_stage IN ('PRECONCEPTION', 'PREGNANCY') AND mother_profile_id IS NOT NULL AND baby_profile_id IS NULL)
        OR
        (related_stage IN ('INFANT', 'TODDLER') AND baby_profile_id IS NOT NULL AND mother_profile_id IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_health_memory_mother_profile ON health_memory_entries(mother_profile_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_health_memory_baby_profile ON health_memory_entries(baby_profile_id, created_at DESC) WHERE deleted_at IS NULL;

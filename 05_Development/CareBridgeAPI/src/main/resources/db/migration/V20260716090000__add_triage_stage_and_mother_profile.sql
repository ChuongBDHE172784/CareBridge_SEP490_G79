ALTER TABLE intake_sessions
    ADD COLUMN IF NOT EXISTS stage VARCHAR(20) NOT NULL DEFAULT 'INFANT',
    ADD COLUMN IF NOT EXISTS mother_profile_id UUID;

ALTER TABLE intake_sessions DROP CONSTRAINT IF EXISTS chk_intake_stage;
ALTER TABLE intake_sessions
    ADD CONSTRAINT chk_intake_stage
    CHECK (stage IN ('PRECONCEPTION', 'PREGNANCY', 'INFANT', 'TODDLER'));

CREATE INDEX IF NOT EXISTS idx_intake_sessions_stage ON intake_sessions(stage);
CREATE INDEX IF NOT EXISTS idx_intake_sessions_mother_profile ON intake_sessions(mother_profile_id);

ALTER TABLE intake_sessions DROP CONSTRAINT IF EXISTS chk_intake_stage;
ALTER TABLE intake_sessions
    ADD CONSTRAINT chk_intake_stage
    CHECK (stage IN ('PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM', 'INFANT', 'TODDLER'));

ALTER TABLE health_memory_entries DROP CONSTRAINT IF EXISTS chk_health_memory_stage;
ALTER TABLE health_memory_entries
    ADD CONSTRAINT chk_health_memory_stage
    CHECK (related_stage IN ('PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM', 'INFANT', 'TODDLER'));

ALTER TABLE health_memory_entries DROP CONSTRAINT IF EXISTS chk_health_memory_profile;
ALTER TABLE health_memory_entries
    ADD CONSTRAINT chk_health_memory_profile
    CHECK (
        (
            related_stage IN ('PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM')
            AND mother_profile_id IS NOT NULL
            AND baby_profile_id IS NULL
        )
        OR
        (
            related_stage IN ('INFANT', 'TODDLER')
            AND baby_profile_id IS NOT NULL
            AND mother_profile_id IS NULL
        )
    );

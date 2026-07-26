-- Expert onboarding identity evidence. Files stay private in uploaded_files;
-- only immutable file references are persisted here.
CREATE TABLE IF NOT EXISTS expert_identity_verifications (
    identity_verification_id UUID NOT NULL DEFAULT gen_random_uuid(),
    expert_profile_id UUID NOT NULL,
    selfie_file_id UUID NOT NULL,
    identity_front_file_id UUID NOT NULL,
    identity_back_file_id UUID NOT NULL,
    face_provider VARCHAR(30) NOT NULL DEFAULT 'COMPREFACE',
    face_status VARCHAR(40) NOT NULL,
    face_similarity NUMERIC(7,6),
    face_threshold NUMERIC(7,6),
    provider_error_code VARCHAR(100),
    review_status VARCHAR(40) NOT NULL,
    review_reason TEXT,
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_expert_identity_verifications PRIMARY KEY (identity_verification_id),
    CONSTRAINT fk_expert_identity_profile FOREIGN KEY (expert_profile_id)
        REFERENCES expert_profiles(expert_profile_id) ON DELETE CASCADE,
    CONSTRAINT fk_expert_identity_selfie_file FOREIGN KEY (selfie_file_id)
        REFERENCES uploaded_files(file_id),
    CONSTRAINT fk_expert_identity_front_file FOREIGN KEY (identity_front_file_id)
        REFERENCES uploaded_files(file_id),
    CONSTRAINT fk_expert_identity_back_file FOREIGN KEY (identity_back_file_id)
        REFERENCES uploaded_files(file_id),
    CONSTRAINT fk_expert_identity_reviewer FOREIGN KEY (reviewed_by)
        REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT ck_expert_identity_distinct_files CHECK (
        selfie_file_id <> identity_front_file_id
        AND selfie_file_id <> identity_back_file_id
        AND identity_front_file_id <> identity_back_file_id
    )
);

CREATE INDEX IF NOT EXISTS idx_expert_identity_profile_created
    ON expert_identity_verifications(expert_profile_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_expert_identity_review_status
    ON expert_identity_verifications(review_status, created_at);

ALTER TABLE expert_credentials
    ADD COLUMN IF NOT EXISTS file_id UUID;
ALTER TABLE expert_credentials
    ADD CONSTRAINT fk_expert_credentials_file
        FOREIGN KEY (file_id) REFERENCES uploaded_files(file_id);
CREATE INDEX IF NOT EXISTS idx_expert_credentials_file_id
    ON expert_credentials(file_id);

ALTER TABLE expert_profiles
    ADD COLUMN IF NOT EXISTS verification_rejection_reason TEXT;

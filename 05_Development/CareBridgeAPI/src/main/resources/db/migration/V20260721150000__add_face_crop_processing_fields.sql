-- Add face crop and processing fields for Detection -> Crop -> Verification pipeline
ALTER TABLE expert_identity_verifications
    ADD COLUMN IF NOT EXISTS selfie_crop_file_id UUID,
    ADD COLUMN IF NOT EXISTS id_card_crop_file_id UUID,
    ADD COLUMN IF NOT EXISTS detection_selfie_status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS detection_id_card_status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS pipeline_error_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS pipeline_status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMPTZ;

-- Add foreign keys for cropped face files (optional, may fail if uploaded_files doesn't have the IDs yet)
-- These are added as nullable so they can be populated after upload
ALTER TABLE expert_identity_verifications
    ADD CONSTRAINT fk_expert_identity_selfie_crop_file
        FOREIGN KEY (selfie_crop_file_id) REFERENCES uploaded_files(file_id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_expert_identity_id_card_crop_file
        FOREIGN KEY (id_card_crop_file_id) REFERENCES uploaded_files(file_id) ON DELETE SET NULL;

-- Create indexes for the new pipeline fields
CREATE INDEX IF NOT EXISTS idx_expert_identity_pipeline_status
    ON expert_identity_verifications(pipeline_status, created_at);
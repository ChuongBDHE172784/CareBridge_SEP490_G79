-- Add kind, purpose, access_mode, checksum columns to uploaded_files
ALTER TABLE uploaded_files
    ADD COLUMN IF NOT EXISTS kind VARCHAR(20) NOT NULL DEFAULT 'IMAGE';

ALTER TABLE uploaded_files
    ADD COLUMN IF NOT EXISTS purpose VARCHAR(50);

ALTER TABLE uploaded_files
    ADD COLUMN IF NOT EXISTS access_mode VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';

ALTER TABLE uploaded_files
    ADD COLUMN IF NOT EXISTS checksum VARCHAR(64);

-- Backfill existing records based on MIME type and storage_provider
UPDATE uploaded_files
SET kind = CASE
    WHEN mime_type LIKE 'image/%' THEN 'IMAGE'
    ELSE 'DOCUMENT'
END
WHERE kind IS NULL OR kind = 'IMAGE';

UPDATE uploaded_files
SET purpose = CASE
    WHEN storage_provider = 'cloudinary' AND mime_type LIKE 'image/%' THEN 'PUBLIC_CONTENT_IMAGE'
    WHEN storage_provider = 'r2' AND mime_type = 'application/pdf' THEN 'MEDICAL_CONTRIBUTION_DOCUMENT'
    ELSE 'PUBLIC_CONTENT_IMAGE'
END
WHERE purpose IS NULL;

-- Add constraint for kind
ALTER TABLE uploaded_files
    ADD CONSTRAINT chk_uploaded_files_kind CHECK (kind IN ('IMAGE', 'DOCUMENT'));

-- Add constraint for access_mode
ALTER TABLE uploaded_files
    ADD CONSTRAINT chk_uploaded_files_access_mode CHECK (access_mode IN ('PRIVATE', 'AUTHENTICATED', 'PUBLIC'));
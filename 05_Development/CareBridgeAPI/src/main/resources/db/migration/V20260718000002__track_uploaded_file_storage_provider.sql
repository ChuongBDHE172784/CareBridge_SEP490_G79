ALTER TABLE uploaded_files
    ADD COLUMN IF NOT EXISTS storage_provider VARCHAR(20);

UPDATE uploaded_files
SET storage_provider = CASE
    WHEN storage_key LIKE 'http://%' OR storage_key LIKE 'https://%' THEN 'cloudinary'
    ELSE 'r2'
END
WHERE storage_provider IS NULL;

ALTER TABLE uploaded_files
    ALTER COLUMN storage_provider SET NOT NULL;

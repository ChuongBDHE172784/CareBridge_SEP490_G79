-- UC-167: UploadFile — file storage records
CREATE TABLE uploaded_files (
    file_id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    owner_user_id    UUID         NOT NULL,
    storage_key      VARCHAR(500) NOT NULL,
    original_name    VARCHAR(255) NOT NULL,
    mime_type        VARCHAR(100) NOT NULL,
    file_size_bytes  BIGINT       NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_uploaded_files PRIMARY KEY (file_id),
    CONSTRAINT uq_uploaded_files_storage_key UNIQUE (storage_key)
);

CREATE INDEX idx_uploaded_files_owner ON uploaded_files(owner_user_id);
CREATE INDEX idx_uploaded_files_status ON uploaded_files(status);

-- UC-39: AddHealthRecord — join table linking health records to uploaded files
CREATE TABLE IF NOT EXISTS health_record_files (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    health_record_id UUID        NOT NULL,
    file_id          UUID        NOT NULL,
    display_order    INT         NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_health_record_files PRIMARY KEY (id),
    CONSTRAINT fk_hrf_record FOREIGN KEY (health_record_id) REFERENCES health_records(health_record_id) ON DELETE CASCADE,
    CONSTRAINT fk_hrf_file   FOREIGN KEY (file_id)          REFERENCES uploaded_files(file_id),
    CONSTRAINT uq_hrf        UNIQUE (health_record_id, file_id)
);

CREATE INDEX IF NOT EXISTS idx_hrf_record_id ON health_record_files(health_record_id);
CREATE INDEX IF NOT EXISTS idx_hrf_file_id   ON health_record_files(file_id);

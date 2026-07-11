-- UC189/UC190/UC191: PostpartumLog soft-delete support.
ALTER TABLE postpartum_logs
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_postpartum_logs_status
    ON postpartum_logs(status);

CREATE INDEX IF NOT EXISTS idx_postpartum_logs_journey_status_log_date
    ON postpartum_logs(journey_id, status, log_date DESC, created_at DESC);

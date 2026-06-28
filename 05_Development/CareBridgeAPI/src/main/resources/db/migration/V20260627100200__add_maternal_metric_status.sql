-- UC-187: ViewMaternalHealthMetricDetail — soft-delete support
ALTER TABLE maternal_health_metrics
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_mhm_status ON maternal_health_metrics(status);

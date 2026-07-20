ALTER TABLE postpartum_logs
    ADD COLUMN IF NOT EXISTS submission_id UUID;

UPDATE postpartum_logs
SET submission_id = postpartum_log_id
WHERE submission_id IS NULL;

ALTER TABLE postpartum_logs
    ALTER COLUMN submission_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_postpartum_logs_journey_submission
    ON postpartum_logs (journey_id, submission_id);

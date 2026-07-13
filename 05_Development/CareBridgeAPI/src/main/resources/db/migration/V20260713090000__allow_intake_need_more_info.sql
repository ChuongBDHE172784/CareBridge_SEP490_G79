ALTER TABLE intake_sessions DROP CONSTRAINT IF EXISTS chk_intake_status;
ALTER TABLE intake_sessions DROP CONSTRAINT IF EXISTS intake_sessions_status_check;

ALTER TABLE intake_sessions
    ADD CONSTRAINT chk_intake_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'NEED_MORE_INFO', 'COMPLETED', 'FAILED'));

ALTER TABLE intake_sessions
    ADD COLUMN IF NOT EXISTS client_request_id VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uq_intake_sessions_owner_client_request
    ON intake_sessions (user_id, client_request_id)
    WHERE client_request_id IS NOT NULL;

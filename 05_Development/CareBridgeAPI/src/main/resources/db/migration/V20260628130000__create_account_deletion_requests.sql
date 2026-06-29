-- UC156: Account Deletion Requests table
CREATE TABLE account_deletion_requests (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reason          TEXT,
    requested_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    scheduled_for   TIMESTAMPTZ,
    processed_at    TIMESTAMPTZ,
    processed_by    UUID,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_account_deletion_requests_user_id ON account_deletion_requests (user_id);
CREATE INDEX idx_account_deletion_requests_status ON account_deletion_requests (status);
CREATE INDEX idx_account_deletion_requests_scheduled_for ON account_deletion_requests (scheduled_for) WHERE status = 'PENDING';

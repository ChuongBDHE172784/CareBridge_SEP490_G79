-- Story 6.6: make RED-to-emergency escalation durable and concurrency-safe.

DO $$
DECLARE
    reconciled_count INTEGER;
BEGIN
    WITH ranked_active AS (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY user_id
                   ORDER BY created_at ASC, id ASC
               ) AS active_rank
        FROM emergency_sessions
        WHERE status = 'ACTIVE'
    ), reconciled AS (
        UPDATE emergency_sessions AS emergency
        SET status = 'CANCELLED',
            resolved_at = COALESCE(emergency.resolved_at, emergency.created_at)
        FROM ranked_active AS ranked
        WHERE emergency.id = ranked.id
          AND ranked.active_rank > 1
          AND emergency.status = 'ACTIVE'
        RETURNING emergency.id
    )
    SELECT COUNT(*) INTO reconciled_count FROM reconciled;

    RAISE NOTICE 'story_6_6_emergency_reconciliation cancelled_duplicates=%', reconciled_count;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_emergency_sessions_one_active_per_user
    ON emergency_sessions (user_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS uq_intake_sessions_id_user
    ON intake_sessions (id, user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_emergency_sessions_id_user
    ON emergency_sessions (id, user_id);

CREATE TABLE IF NOT EXISTS triage_emergency_escalations (
    intake_session_id UUID PRIMARY KEY
        REFERENCES intake_sessions(id),
    emergency_session_id UUID NOT NULL
        REFERENCES emergency_sessions(id),
    user_id UUID NOT NULL
        REFERENCES users(user_id),
    triggered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_triage_escalation_intake_owner
        FOREIGN KEY (intake_session_id, user_id)
        REFERENCES intake_sessions(id, user_id),
    CONSTRAINT fk_triage_escalation_emergency_owner
        FOREIGN KEY (emergency_session_id, user_id)
        REFERENCES emergency_sessions(id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_triage_emergency_escalations_emergency
    ON triage_emergency_escalations (emergency_session_id);

CREATE INDEX IF NOT EXISTS idx_triage_emergency_escalations_user
    ON triage_emergency_escalations (user_id, triggered_at DESC);

CREATE TABLE IF NOT EXISTS emergency_notification_outbox (
    emergency_session_id UUID PRIMARY KEY
        REFERENCES emergency_sessions(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_error_code VARCHAR(120),
    claim_token UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    delivered_at TIMESTAMPTZ,
    terminal_at TIMESTAMPTZ,
    CONSTRAINT chk_emergency_notification_outbox_status
        CHECK (status IN ('PENDING', 'DELIVERED', 'SUPPRESSED')),
    CONSTRAINT chk_emergency_notification_outbox_attempts
        CHECK (attempt_count >= 0),
    CONSTRAINT chk_emergency_notification_outbox_terminal_state
        CHECK (
            (status = 'PENDING' AND terminal_at IS NULL AND delivered_at IS NULL)
            OR (status = 'DELIVERED' AND terminal_at IS NOT NULL AND delivered_at IS NOT NULL)
            OR (status = 'SUPPRESSED' AND terminal_at IS NOT NULL AND delivered_at IS NULL)
        ),
    CONSTRAINT chk_emergency_notification_outbox_claim_state
        CHECK (claim_token IS NULL OR status = 'PENDING')
);

CREATE INDEX IF NOT EXISTS idx_emergency_notification_outbox_pending
    ON emergency_notification_outbox (next_attempt_at, created_at)
    WHERE status = 'PENDING';

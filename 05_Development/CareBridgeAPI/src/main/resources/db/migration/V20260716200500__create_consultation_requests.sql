-- === EXPERT CONSULTATION REQUESTS SCHEMA (CB-CONREQ-IMP-001) ===
-- Independent of consultation_bookings/consultation_sessions: a lightweight request is not a
-- paid or scheduled booking.

CREATE TABLE consultation_requests (
    id                      UUID          NOT NULL DEFAULT gen_random_uuid(),
    requester_user_id       UUID          NOT NULL,
    expert_profile_id       UUID          NOT NULL,
    client_request_id       UUID          NOT NULL,
    topic                   VARCHAR(200)  NOT NULL,
    description             VARCHAR(2000) NOT NULL,
    preferred_window_start  TIMESTAMPTZ,
    preferred_window_end    TIMESTAMPTZ,
    status                  VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    reject_reason           VARCHAR(500),
    direct_conversation_id  UUID,
    responded_at            TIMESTAMPTZ,
    responded_by            UUID,
    expires_at              TIMESTAMPTZ   NOT NULL,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT consultation_requests_pkey PRIMARY KEY (id),
    CONSTRAINT consultation_requests_requester_user_id_fkey
        FOREIGN KEY (requester_user_id) REFERENCES users(user_id),
    CONSTRAINT consultation_requests_expert_profile_id_fkey
        FOREIGN KEY (expert_profile_id) REFERENCES expert_profiles(expert_profile_id),
    CONSTRAINT consultation_requests_direct_conversation_id_fkey
        FOREIGN KEY (direct_conversation_id) REFERENCES direct_conversations(conversation_id),
    CONSTRAINT consultation_requests_responded_by_fkey
        FOREIGN KEY (responded_by) REFERENCES users(user_id),
    CONSTRAINT consultation_requests_client_request_id_key
        UNIQUE (requester_user_id, client_request_id),
    CONSTRAINT chk_consultation_requests_status
        CHECK (status = ANY (ARRAY['PENDING','ACCEPTED','REJECTED','CANCELLED','EXPIRED']::varchar[])),
    CONSTRAINT chk_consultation_requests_window
        CHECK ((preferred_window_start IS NULL AND preferred_window_end IS NULL)
                OR (preferred_window_start IS NOT NULL AND preferred_window_end IS NOT NULL
                    AND preferred_window_end > preferred_window_start)),
    CONSTRAINT chk_consultation_requests_responded_fields
        CHECK (status = 'PENDING' OR responded_at IS NOT NULL),
    CONSTRAINT chk_consultation_requests_expires_after_created
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_consultation_requests_expert_status_created
    ON consultation_requests (expert_profile_id, status, created_at DESC);

CREATE INDEX idx_consultation_requests_requester_status_created
    ON consultation_requests (requester_user_id, status, created_at DESC);

CREATE INDEX idx_consultation_requests_expiry
    ON consultation_requests (expires_at) WHERE status = 'PENDING';

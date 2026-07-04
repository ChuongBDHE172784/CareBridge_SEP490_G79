-- TV4: Nearby care support tables (UC-81, UC-82)
-- Migration: V20260703000003

-- ============================================================
-- nearby_support_requests: Mother creates a time-limited nearby
-- support request visible to eligible verified experts.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.nearby_support_requests (
    request_id          uuid NOT NULL DEFAULT gen_random_uuid(),
    requester_user_id   uuid NOT NULL,
    support_type        varchar(50) NOT NULL,
    description         text,
    latitude            numeric(10,8) NOT NULL,
    longitude           numeric(11,8) NOT NULL,
    consent_status      varchar(20) NOT NULL DEFAULT 'PENDING',
    status              varchar(20) NOT NULL DEFAULT 'OPEN',
    responded_at        timestamp with time zone,
    completed_at        timestamp with time zone,
    created_at          timestamp with time zone NOT NULL DEFAULT now(),
    updated_at          timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT nearby_support_requests_pkey PRIMARY KEY (request_id),
    CONSTRAINT nearby_support_requests_status_check
        CHECK (status IN ('OPEN','ACCEPTED','CANCELLED','COMPLETED'))
);

-- ============================================================
-- nearby_support_responses: Expert accepts / declines / stops
-- responding to a nearby support request.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.nearby_support_responses (
    response_id     uuid NOT NULL DEFAULT gen_random_uuid(),
    request_id      uuid NOT NULL,
    expert_profile_id uuid NOT NULL,
    action          varchar(20) NOT NULL,
    note            text,
    responded_at    timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT nearby_support_responses_pkey PRIMARY KEY (response_id),
    CONSTRAINT nearby_support_responses_action_check
        CHECK (action IN ('ACCEPT','DECLINE','STOP')),
    CONSTRAINT fk_nearby_support_responses_request
        FOREIGN KEY (request_id)
        REFERENCES public.nearby_support_requests (request_id)
        ON DELETE CASCADE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_nearby_support_requests_status
    ON public.nearby_support_requests (status, created_at);
CREATE INDEX IF NOT EXISTS idx_nearby_support_responses_request
    ON public.nearby_support_responses (request_id);
CREATE INDEX IF NOT EXISTS idx_nearby_support_responses_expert
    ON public.nearby_support_responses (expert_profile_id);

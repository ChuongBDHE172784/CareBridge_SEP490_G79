-- Restore UC-10/UC-72 after the workflow was intentionally retired by
-- V20260806110000. This forward migration works for both upgraded and fresh
-- databases because the earlier migration always drops the table first.

CREATE TABLE public.account_lock_appeals (
    appeal_id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    lock_episode_id uuid NOT NULL,
    reason character varying(1000) NOT NULL,
    status character varying(30) DEFAULT 'PENDING' NOT NULL,
    submitted_at timestamp with time zone DEFAULT now() NOT NULL,
    reviewed_by uuid,
    reviewed_at timestamp with time zone,
    review_note character varying(1000),
    CONSTRAINT account_lock_appeals_pkey PRIMARY KEY (appeal_id),
    CONSTRAINT account_lock_appeals_user_fk FOREIGN KEY (user_id)
        REFERENCES public.users(user_id) ON DELETE CASCADE,
    CONSTRAINT account_lock_appeals_reviewer_fk FOREIGN KEY (reviewed_by)
        REFERENCES public.users(user_id) ON DELETE SET NULL,
    CONSTRAINT account_lock_appeals_status_ck
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT account_lock_appeals_reason_ck CHECK (length(btrim(reason)) > 0),
    CONSTRAINT account_lock_appeals_review_ck CHECK (
        (status = 'PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL)
        OR
        (status <> 'PENDING' AND reviewed_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX account_lock_appeals_pending_episode_uq
    ON public.account_lock_appeals (user_id, lock_episode_id)
    WHERE status = 'PENDING';

CREATE INDEX account_lock_appeals_queue_ix
    ON public.account_lock_appeals (status, submitted_at DESC);

CREATE INDEX account_lock_appeals_user_ix
    ON public.account_lock_appeals (user_id, submitted_at DESC);

-- === DIRECT CONVERSATION & CALL SCHEMA (UC-144 redesign — CB-CHAT-IMP-144D v1.2) ===
-- Independent of consultation_bookings/consultation_sessions/consultation_messages —
-- no FK to those tables, per ADR-DCC-001.

CREATE TABLE IF NOT EXISTS public.direct_conversations (
    conversation_id  uuid         NOT NULL DEFAULT gen_random_uuid(),
    mother_user_id   uuid         NOT NULL,
    expert_user_id   uuid         NOT NULL,
    status           varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       timestamptz  NOT NULL DEFAULT now(),
    last_activity_at timestamptz,
    CONSTRAINT direct_conversations_pkey PRIMARY KEY (conversation_id),
    CONSTRAINT direct_conversations_mother_user_id_fkey
        FOREIGN KEY (mother_user_id) REFERENCES public.users(user_id),
    CONSTRAINT direct_conversations_expert_user_id_fkey
        FOREIGN KEY (expert_user_id) REFERENCES public.users(user_id),
    CONSTRAINT uq_direct_conversations_pair UNIQUE (mother_user_id, expert_user_id),
    CONSTRAINT chk_direct_conversations_status
        CHECK (status = ANY (ARRAY['ACTIVE']::varchar[])),
    CONSTRAINT chk_direct_conversations_activity_after_created
        CHECK (last_activity_at IS NULL OR last_activity_at >= created_at)
);

-- Reconcile environments where Hibernate ddl-auto created the tables before this
-- Flyway migration was applied. Fresh databases already have these definitions;
-- the DROP/ADD sequence below keeps both paths structurally identical.
ALTER TABLE public.direct_conversations
    ALTER COLUMN conversation_id SET DEFAULT gen_random_uuid(),
    ALTER COLUMN status SET DEFAULT 'ACTIVE',
    ALTER COLUMN created_at SET DEFAULT now(),
    DROP CONSTRAINT IF EXISTS direct_conversations_mother_user_id_fkey,
    DROP CONSTRAINT IF EXISTS direct_conversations_expert_user_id_fkey,
    DROP CONSTRAINT IF EXISTS uq_direct_conversations_pair,
    DROP CONSTRAINT IF EXISTS chk_direct_conversations_status,
    DROP CONSTRAINT IF EXISTS chk_direct_conversations_activity_after_created,
    ADD CONSTRAINT direct_conversations_mother_user_id_fkey
        FOREIGN KEY (mother_user_id) REFERENCES public.users(user_id),
    ADD CONSTRAINT direct_conversations_expert_user_id_fkey
        FOREIGN KEY (expert_user_id) REFERENCES public.users(user_id),
    ADD CONSTRAINT uq_direct_conversations_pair UNIQUE (mother_user_id, expert_user_id),
    ADD CONSTRAINT chk_direct_conversations_status
        CHECK (status = ANY (ARRAY['ACTIVE']::varchar[])),
    ADD CONSTRAINT chk_direct_conversations_activity_after_created
        CHECK (last_activity_at IS NULL OR last_activity_at >= created_at);

CREATE TABLE IF NOT EXISTS public.direct_messages (
    message_id          uuid         NOT NULL DEFAULT gen_random_uuid(),
    conversation_id      uuid         NOT NULL,
    sender_user_id        uuid         NOT NULL,
    client_message_id    uuid         NOT NULL,
    message_type          varchar(30)  NOT NULL DEFAULT 'TEXT',
    message_body           text         NOT NULL,
    created_at              timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT direct_messages_pkey PRIMARY KEY (message_id),
    CONSTRAINT direct_messages_conversation_id_fkey
        FOREIGN KEY (conversation_id) REFERENCES public.direct_conversations(conversation_id),
    CONSTRAINT direct_messages_sender_user_id_fkey
        FOREIGN KEY (sender_user_id) REFERENCES public.users(user_id),
    CONSTRAINT uq_direct_messages_client_id
        UNIQUE (conversation_id, sender_user_id, client_message_id),
    CONSTRAINT chk_direct_messages_type
        CHECK (message_type = ANY (ARRAY['TEXT']::varchar[])),
    CONSTRAINT chk_direct_messages_body_length
        CHECK (length(btrim(message_body)) > 0 AND length(message_body) <= 2000)
);

ALTER TABLE public.direct_messages
    ALTER COLUMN message_id SET DEFAULT gen_random_uuid(),
    ALTER COLUMN message_type SET DEFAULT 'TEXT',
    ALTER COLUMN created_at SET DEFAULT now(),
    DROP CONSTRAINT IF EXISTS direct_messages_conversation_id_fkey,
    DROP CONSTRAINT IF EXISTS direct_messages_sender_user_id_fkey,
    DROP CONSTRAINT IF EXISTS uq_direct_messages_client_id,
    DROP CONSTRAINT IF EXISTS chk_direct_messages_type,
    DROP CONSTRAINT IF EXISTS chk_direct_messages_body_length,
    ADD CONSTRAINT direct_messages_conversation_id_fkey
        FOREIGN KEY (conversation_id) REFERENCES public.direct_conversations(conversation_id),
    ADD CONSTRAINT direct_messages_sender_user_id_fkey
        FOREIGN KEY (sender_user_id) REFERENCES public.users(user_id),
    ADD CONSTRAINT uq_direct_messages_client_id
        UNIQUE (conversation_id, sender_user_id, client_message_id),
    ADD CONSTRAINT chk_direct_messages_type
        CHECK (message_type = ANY (ARRAY['TEXT']::varchar[])),
    ADD CONSTRAINT chk_direct_messages_body_length
        CHECK (length(btrim(message_body)) > 0 AND length(message_body) <= 2000);

CREATE INDEX IF NOT EXISTS idx_direct_messages_conversation_id
    ON public.direct_messages (conversation_id, created_at DESC);

CREATE TABLE IF NOT EXISTS public.conversation_calls (
    call_id                uuid         NOT NULL DEFAULT gen_random_uuid(),
    conversation_id         uuid         NOT NULL,
    initiated_by_user_id    uuid         NOT NULL,
    call_type               varchar(10)  NOT NULL,
    call_status             varchar(20)  NOT NULL DEFAULT 'INITIATED',
    zego_room_id            varchar(255) NOT NULL,
    initiated_at             timestamptz  NOT NULL DEFAULT now(),
    answered_at               timestamptz,
    ended_at                  timestamptz,
    duration_seconds          integer,
    created_at                 timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT conversation_calls_pkey PRIMARY KEY (call_id),
    CONSTRAINT conversation_calls_conversation_id_fkey
        FOREIGN KEY (conversation_id) REFERENCES public.direct_conversations(conversation_id),
    CONSTRAINT conversation_calls_initiated_by_user_id_fkey
        FOREIGN KEY (initiated_by_user_id) REFERENCES public.users(user_id),
    CONSTRAINT chk_conversation_calls_type
        CHECK (call_type = ANY (ARRAY['VOICE','VIDEO']::varchar[])),
    CONSTRAINT chk_conversation_calls_status
        CHECK (call_status = ANY (ARRAY['INITIATED','RINGING','ANSWERED','DECLINED','MISSED','CANCELLED','ENDED','FAILED']::varchar[])),
    CONSTRAINT chk_conversation_calls_duration_non_negative
        CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    CONSTRAINT chk_conversation_calls_answered_after_initiated
        CHECK (answered_at IS NULL OR answered_at >= initiated_at),
    CONSTRAINT chk_conversation_calls_ended_after_initiated
        CHECK (ended_at IS NULL OR ended_at >= initiated_at),
    CONSTRAINT chk_conversation_calls_ended_requires_answered
        CHECK (call_status <> 'ENDED' OR answered_at IS NOT NULL)
);

ALTER TABLE public.conversation_calls
    ALTER COLUMN call_id SET DEFAULT gen_random_uuid(),
    ALTER COLUMN call_status SET DEFAULT 'INITIATED',
    ALTER COLUMN initiated_at SET DEFAULT now(),
    ALTER COLUMN created_at SET DEFAULT now(),
    DROP CONSTRAINT IF EXISTS conversation_calls_conversation_id_fkey,
    DROP CONSTRAINT IF EXISTS conversation_calls_initiated_by_user_id_fkey,
    DROP CONSTRAINT IF EXISTS chk_conversation_calls_type,
    DROP CONSTRAINT IF EXISTS chk_conversation_calls_status,
    DROP CONSTRAINT IF EXISTS chk_conversation_calls_duration_non_negative,
    DROP CONSTRAINT IF EXISTS chk_conversation_calls_answered_after_initiated,
    DROP CONSTRAINT IF EXISTS chk_conversation_calls_ended_after_initiated,
    DROP CONSTRAINT IF EXISTS chk_conversation_calls_ended_requires_answered,
    ADD CONSTRAINT conversation_calls_conversation_id_fkey
        FOREIGN KEY (conversation_id) REFERENCES public.direct_conversations(conversation_id),
    ADD CONSTRAINT conversation_calls_initiated_by_user_id_fkey
        FOREIGN KEY (initiated_by_user_id) REFERENCES public.users(user_id),
    ADD CONSTRAINT chk_conversation_calls_type
        CHECK (call_type = ANY (ARRAY['VOICE','VIDEO']::varchar[])),
    ADD CONSTRAINT chk_conversation_calls_status
        CHECK (call_status = ANY (ARRAY['INITIATED','RINGING','ANSWERED','DECLINED','MISSED','CANCELLED','ENDED','FAILED']::varchar[])),
    ADD CONSTRAINT chk_conversation_calls_duration_non_negative
        CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    ADD CONSTRAINT chk_conversation_calls_answered_after_initiated
        CHECK (answered_at IS NULL OR answered_at >= initiated_at),
    ADD CONSTRAINT chk_conversation_calls_ended_after_initiated
        CHECK (ended_at IS NULL OR ended_at >= initiated_at),
    ADD CONSTRAINT chk_conversation_calls_ended_requires_answered
        CHECK (call_status <> 'ENDED' OR answered_at IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_conversation_calls_conversation_id
    ON public.conversation_calls (conversation_id, initiated_at DESC);

CREATE INDEX IF NOT EXISTS idx_conversation_calls_ringing_timeout
    ON public.conversation_calls (initiated_at) WHERE call_status = 'RINGING';

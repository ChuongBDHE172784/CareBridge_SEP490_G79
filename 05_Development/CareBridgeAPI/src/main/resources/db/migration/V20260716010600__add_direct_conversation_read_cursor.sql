-- CB-EXPCHAT-IMP-001 / ADR-MEDI-003 — per-participant read cursor on direct_conversations.
-- Additive only: 2 nullable columns + 1 supporting index. No existing column/constraint touched.
ALTER TABLE public.direct_conversations
    ADD COLUMN IF NOT EXISTS mother_last_read_at timestamptz NULL,
    ADD COLUMN IF NOT EXISTS mother_last_read_message_id uuid NULL,
    ADD COLUMN IF NOT EXISTS expert_last_read_at timestamptz NULL,
    ADD COLUMN IF NOT EXISTS expert_last_read_message_id uuid NULL;

-- Supports the unread aggregate query (ADR-MEDI-002 §6.2): exclude sender, compare created_at
-- against the caller's cursor for a batch of conversation ids.
-- The existing idx_direct_messages_conversation_created index already supports the timestamp
-- range. This unique suffix gives all timeline/read operations a deterministic total order.
CREATE INDEX IF NOT EXISTS idx_direct_messages_conversation_created_id
    ON public.direct_messages (conversation_id, created_at, message_id);

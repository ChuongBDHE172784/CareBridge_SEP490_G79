ALTER TABLE direct_messages
    DROP CONSTRAINT IF EXISTS direct_messages_type_ck,
    DROP CONSTRAINT IF EXISTS direct_messages_body_ck,
    DROP CONSTRAINT IF EXISTS direct_messages_payload_check;

ALTER TABLE direct_messages
    ADD CONSTRAINT direct_messages_payload_check
    CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE')
      AND ((message_type = 'TEXT' AND message_body IS NOT NULL AND recalled_at IS NULL)
        OR (message_type IN ('IMAGE', 'FILE') AND attachment_id IS NOT NULL AND recalled_at IS NULL)
        OR recalled_at IS NOT NULL));

CREATE TABLE direct_conversation_read_cursors (
    conversation_id uuid NOT NULL REFERENCES direct_conversations(conversation_id) ON DELETE CASCADE,
    reader_user_id uuid NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    last_read_at timestamp with time zone NOT NULL,
    last_read_message_id uuid NOT NULL REFERENCES direct_messages(message_id),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    PRIMARY KEY (conversation_id, reader_user_id)
);

INSERT INTO direct_conversation_read_cursors (conversation_id, reader_user_id, last_read_at, last_read_message_id)
SELECT conversation_id, mother_user_id, mother_last_read_at, mother_last_read_message_id
  FROM direct_conversations
 WHERE mother_last_read_at IS NOT NULL AND mother_last_read_message_id IS NOT NULL
ON CONFLICT (conversation_id, reader_user_id) DO NOTHING;

INSERT INTO direct_conversation_read_cursors (conversation_id, reader_user_id, last_read_at, last_read_message_id)
SELECT conversation_id, expert_user_id, expert_last_read_at, expert_last_read_message_id
  FROM direct_conversations
 WHERE expert_last_read_at IS NOT NULL AND expert_last_read_message_id IS NOT NULL
ON CONFLICT (conversation_id, reader_user_id) DO NOTHING;

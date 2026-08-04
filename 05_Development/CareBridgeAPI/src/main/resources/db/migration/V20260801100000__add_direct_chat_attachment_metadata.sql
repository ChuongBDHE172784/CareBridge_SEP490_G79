ALTER TABLE direct_messages
    ADD COLUMN attachment_id uuid NULL REFERENCES attachments(attachment_id),
    ADD COLUMN recalled_at timestamp with time zone NULL,
    ADD COLUMN recalled_by_user_id uuid NULL;

ALTER TABLE direct_messages
    ALTER COLUMN message_body DROP NOT NULL;

ALTER TABLE direct_messages
    ADD CONSTRAINT direct_messages_payload_check
    CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE')
      AND ((message_type = 'TEXT' AND message_body IS NOT NULL)
        OR (message_type IN ('IMAGE', 'FILE') AND attachment_id IS NOT NULL)));

CREATE INDEX idx_direct_messages_attachment_id ON direct_messages(attachment_id);

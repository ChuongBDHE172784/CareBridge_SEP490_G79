package com.carebridge.backend.directchat.service.impl;

import com.carebridge.backend.directchat.entity.DirectMessage;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Atomic idempotent insert which remains part of the caller's transaction. */
@Component
class DirectMessageWriter {

    private final JdbcTemplate jdbcTemplate;

    DirectMessageWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    boolean insertIfAbsent(DirectMessage message) {
        return jdbcTemplate.update("""
                INSERT INTO archived_realtime_records
                    (archive_id, legacy_table, legacy_id, owner_user_id, conversation_id,
                     sender_user_id, client_message_id, message_type, message_body,
                     original_created_at)
                VALUES (?, 'direct_messages', ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (conversation_id, sender_user_id, client_message_id)
                  WHERE legacy_table='direct_messages' DO NOTHING
                """,
                message.getId(), message.getId().toString(), message.getSenderUserId(),
                message.getConversationId(), message.getSenderUserId(),
                message.getClientMessageId(), message.getMessageType().name(),
                message.getMessageBody(), Timestamp.from(message.getCreatedAt())) == 1;
    }
}

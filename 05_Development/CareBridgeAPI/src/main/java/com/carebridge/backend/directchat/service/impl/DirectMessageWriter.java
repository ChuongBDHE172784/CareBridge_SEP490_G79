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
                INSERT INTO direct_messages
                    (message_id, conversation_id, sender_user_id, client_message_id,
                     message_type, message_body, attachment_id,
                     location_latitude, location_longitude, location_label, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (conversation_id, sender_user_id, client_message_id) DO NOTHING
                """,
                message.getId(), message.getConversationId(), message.getSenderUserId(),
                message.getClientMessageId(), message.getMessageType().name(),
                message.getMessageBody(), message.getAttachmentId(),
                message.getLocationLatitude(), message.getLocationLongitude(), message.getLocationLabel(),
                Timestamp.from(message.getCreatedAt())) == 1;
    }
}

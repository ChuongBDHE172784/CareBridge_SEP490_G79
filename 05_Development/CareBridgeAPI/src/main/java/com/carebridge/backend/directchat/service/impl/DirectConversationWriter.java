package com.carebridge.backend.directchat.service.impl;

import com.carebridge.backend.directchat.entity.DirectConversation;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Atomic find-or-create insert which remains part of the caller's transaction. */
@Component
class DirectConversationWriter {

    private final JdbcTemplate jdbcTemplate;

    DirectConversationWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    boolean insertIfAbsent(DirectConversation conversation) {
        return jdbcTemplate.update("""
                INSERT INTO archived_realtime_records
                    (archive_id, legacy_table, legacy_id, owner_user_id,
                     mother_user_id, expert_user_id, status, original_created_at)
                VALUES (?, 'direct_conversations', ?, ?, ?, ?, ?, ?)
                ON CONFLICT (mother_user_id, expert_user_id)
                  WHERE legacy_table='direct_conversations' DO NOTHING
                """,
                conversation.getId(), conversation.getId().toString(), conversation.getMotherUserId(),
                conversation.getMotherUserId(), conversation.getExpertUserId(),
                conversation.getStatus(), Timestamp.from(conversation.getCreatedAt())) == 1;
    }
}

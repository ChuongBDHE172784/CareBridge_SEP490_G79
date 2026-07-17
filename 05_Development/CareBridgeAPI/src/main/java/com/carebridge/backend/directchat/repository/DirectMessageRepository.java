package com.carebridge.backend.directchat.repository;

import com.carebridge.backend.directchat.entity.DirectMessage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    // BR-DCC-005 idempotency oracle — combined with the unique constraint
    // uq_direct_messages_client_id (conversation_id, sender_user_id, client_message_id).
    Optional<DirectMessage> findByConversationIdAndSenderUserIdAndClientMessageId(
            UUID conversationId, UUID senderUserId, UUID clientMessageId);

    // ADR-MEDI-003 mục 3 (C9) — validates lastSeenMessageId belongs to conversationId in 1 query,
    // before it is ever used to advance a read cursor.
    Optional<DirectMessage> findByIdAndConversationId(UUID messageId, UUID conversationId);
}

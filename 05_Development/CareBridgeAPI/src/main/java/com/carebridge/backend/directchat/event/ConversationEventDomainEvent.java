package com.carebridge.backend.directchat.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published within the write transaction, consumed by
 * {@code IConversationEventPublisher.publishAfterCommit} via
 * {@code @TransactionalEventListener(AFTER_COMMIT)}. Not the Firebase wire payload itself —
 * {@code actorUserId} is used to resolve the recipient (BR-DCC-010) and is never sent to Firebase.
 */
public record ConversationEventDomainEvent(
        String eventType,
        UUID conversationId,
        UUID actorUserId,
        UUID resourceId,
        Instant occurredAt) {
}

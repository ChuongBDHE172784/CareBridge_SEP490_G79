package com.carebridge.backend.integration.firebase;

import com.carebridge.backend.directchat.event.ConversationEventDomainEvent;

/**
 * ADR-DCC-004: writes a minimal signal to the RECIPIENT's Firebase RTDB inbox
 * (/user-conversation-events/{recipientUserId}/{eventId}) — never the actor's own inbox
 * (BR-DCC-010). Best-effort: failures are logged, never thrown, never roll back the
 * already-committed write that triggered them (BR-DCC-007).
 */
public interface IConversationEventPublisher {

    void publishAfterCommit(ConversationEventDomainEvent event);
}

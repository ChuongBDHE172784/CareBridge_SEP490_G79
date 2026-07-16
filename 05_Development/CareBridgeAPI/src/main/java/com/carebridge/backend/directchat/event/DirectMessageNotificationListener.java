package com.carebridge.backend.directchat.event;

import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.notification.service.IDirectMessageNotificationService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * ADR-MEDI-004 mục 2 — second, independent consumer of the same {@link ConversationEventDomainEvent}
 * that {@code ConversationEventPublisherImpl} (UC144, unchanged) already consumes for the Firestore
 * signal. Runs AFTER_COMMIT + @Async, same mechanism as that existing consumer — not a new one —
 * so a notification/FCM failure can never roll back the message that was already committed
 * (constraint C3; see TDS ADR-MEDI-004 for why the original synchronous design was wrong).
 */
@Component
public class DirectMessageNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(DirectMessageNotificationListener.class);
    private static final String MESSAGE_SENT = "MESSAGE_SENT";

    private final DirectConversationRepository conversationRepository;
    private final IDirectMessageNotificationService notificationService;

    @Autowired
    public DirectMessageNotificationListener(
            DirectConversationRepository conversationRepository,
            IDirectMessageNotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.notificationService = notificationService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConversationEvent(ConversationEventDomainEvent event) {
        // ADR-MEDI-004 mục 8 — scope is TEXT messages only this pass, not call events.
        if (!MESSAGE_SENT.equals(event.eventType())) {
            return;
        }
        try {
            DirectConversation conversation = conversationRepository.findById(event.conversationId()).orElse(null);
            if (conversation == null) {
                log.warn("Cannot resolve recipient — conversation {} not found for event {}",
                        event.conversationId(), event.eventType());
                return;
            }
            if (!event.actorUserId().equals(conversation.getMotherUserId())
                    && !event.actorUserId().equals(conversation.getExpertUserId())) {
                log.error("Ignoring malformed MESSAGE_SENT event: actor {} is not in conversation {}",
                        event.actorUserId(), event.conversationId());
                return;
            }
            UUID recipientUserId = resolveRecipient(conversation, event.actorUserId());
            notificationService.notifyNewMessage(recipientUserId, event.actorUserId(), event.conversationId(), event.resourceId());
        } catch (Exception ex) {
            // Nothing above this to roll back (already AFTER_COMMIT) — log only, matching
            // ConversationEventPublisherImpl's own best-effort error handling.
            log.error("Failed to process MESSAGE_SENT notification for conversation {}", event.conversationId(), ex);
        }
    }

    // BR-DCC-010: recipient is always the participant who is NOT the actor — same rule as
    // ConversationEventPublisherImpl.resolveRecipient (private there, so re-implemented here rather
    // than touching the UC144-approved class).
    private static UUID resolveRecipient(DirectConversation conversation, UUID actorUserId) {
        return actorUserId.equals(conversation.getMotherUserId())
                ? conversation.getExpertUserId()
                : conversation.getMotherUserId();
    }
}

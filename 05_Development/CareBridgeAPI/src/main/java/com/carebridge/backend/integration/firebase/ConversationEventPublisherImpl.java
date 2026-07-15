package com.carebridge.backend.integration.firebase;

import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.event.ConversationEventDomainEvent;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ConversationEventPublisherImpl implements IConversationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ConversationEventPublisherImpl.class);

    private final DirectConversationRepository conversationRepository;
    private final IFirebaseRealtimeGateway gateway;
    private final Clock clock;

    @Value("${carebridge.firebase.realtime.enabled:false}")
    private boolean realtimeEnabled;

    @Autowired
    public ConversationEventPublisherImpl(DirectConversationRepository conversationRepository, IFirebaseRealtimeGateway gateway) {
        this(conversationRepository, gateway, Clock.systemDefaultZone());
    }

    /** Test constructor. */
    public ConversationEventPublisherImpl(DirectConversationRepository conversationRepository,
            IFirebaseRealtimeGateway gateway, Clock clock) {
        this.conversationRepository = conversationRepository;
        this.gateway = gateway;
        this.clock = clock;
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(ConversationEventDomainEvent event) {
        if (!realtimeEnabled) {
            log.debug("Firebase realtime disabled — skipping publish for {}", event.eventType());
            return;
        }
        try {
            Optional<DirectConversation> conversation = conversationRepository.findById(event.conversationId());
            if (conversation.isEmpty()) {
                log.warn("Cannot resolve recipient — conversation {} not found for event {}", event.conversationId(), event.eventType());
                return;
            }
            UUID recipientUserId = resolveRecipient(conversation.get(), event.actorUserId());

            String eventId = UUID.randomUUID().toString();
            String path = "/user-conversation-events/" + recipientUserId + "/" + eventId;
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("eventId", eventId);
            payload.put("eventType", event.eventType());
            payload.put("conversationId", event.conversationId().toString());
            payload.put("resourceId", event.resourceId().toString());
            payload.put("occurredAt", event.occurredAt().toEpochMilli());

            gateway.write(path, payload);
        } catch (Exception ex) {
            // BR-DCC-007: best-effort — never propagate, the write that triggered this already committed.
            log.error("Failed to publish Firebase realtime event {} for conversation {}", event.eventType(), event.conversationId(), ex);
        }
    }

    // BR-DCC-010: recipient is always the participant who is NOT the actor.
    private static UUID resolveRecipient(DirectConversation conversation, UUID actorUserId) {
        return actorUserId.equals(conversation.getMotherUserId())
                ? conversation.getExpertUserId()
                : conversation.getMotherUserId();
    }
}

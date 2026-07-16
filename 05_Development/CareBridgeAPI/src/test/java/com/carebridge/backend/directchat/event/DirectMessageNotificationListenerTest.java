package com.carebridge.backend.directchat.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.notification.service.IDirectMessageNotificationService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// MEDI-TC-014a — listener invoked directly (Mockito), independent of transaction/event-bus wiring
@ExtendWith(MockitoExtension.class)
class DirectMessageNotificationListenerTest {

    @Mock private DirectConversationRepository conversationRepository;
    @Mock private IDirectMessageNotificationService notificationService;

    private DirectMessageNotificationListener listener;

    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID MOTHER_ID = UUID.randomUUID();
    private static final UUID EXPERT_ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listener = new DirectMessageNotificationListener(conversationRepository, notificationService);
    }

    private static DirectConversation conversation() {
        return DirectConversation.builder().id(CONVERSATION_ID).motherUserId(MOTHER_ID).expertUserId(EXPERT_ID)
                .status("ACTIVE").build();
    }

    // C12 — event constructed with the REAL field order: (eventType, conversationId, actorUserId, resourceId, occurredAt)
    @Test
    void onConversationEvent_messageSent_resolvesRecipientAsCounterpartOfActor() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        ConversationEventDomainEvent event = new ConversationEventDomainEvent(
                "MESSAGE_SENT", CONVERSATION_ID, /* actorUserId */ MOTHER_ID, /* resourceId */ MESSAGE_ID, Instant.now());

        listener.onConversationEvent(event);

        verify(notificationService).notifyNewMessage(EXPERT_ID, MOTHER_ID, CONVERSATION_ID, MESSAGE_ID);
    }

    @Test
    void onConversationEvent_actorIsExpert_recipientIsMother() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        ConversationEventDomainEvent event = new ConversationEventDomainEvent(
                "MESSAGE_SENT", CONVERSATION_ID, EXPERT_ID, MESSAGE_ID, Instant.now());

        listener.onConversationEvent(event);

        verify(notificationService).notifyNewMessage(MOTHER_ID, EXPERT_ID, CONVERSATION_ID, MESSAGE_ID);
    }

    // ADR-MEDI-004 mục 8 — call events are out of scope, never trigger a MESSAGE notification
    @Test
    void onConversationEvent_callInitiated_ignoredEntirely() {
        ConversationEventDomainEvent event = new ConversationEventDomainEvent(
                "CALL_INITIATED", CONVERSATION_ID, MOTHER_ID, UUID.randomUUID(), Instant.now());

        listener.onConversationEvent(event);

        verify(notificationService, never()).notifyNewMessage(any(), any(), any(), any());
        verify(conversationRepository, never()).findById(any());
    }

    @Test
    void onConversationEvent_conversationNotFound_doesNotThrow_andSkipsNotification() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.empty());
        ConversationEventDomainEvent event = new ConversationEventDomainEvent(
                "MESSAGE_SENT", CONVERSATION_ID, MOTHER_ID, MESSAGE_ID, Instant.now());

        listener.onConversationEvent(event); // must not throw — AFTER_COMMIT, nothing to roll back

        verify(notificationService, never()).notifyNewMessage(any(), any(), any(), any());
    }

    @Test
    void onConversationEvent_notificationServiceThrows_swallowedNotPropagated() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                .when(notificationService).notifyNewMessage(any(), any(), any(), any());
        ConversationEventDomainEvent event = new ConversationEventDomainEvent(
                "MESSAGE_SENT", CONVERSATION_ID, MOTHER_ID, MESSAGE_ID, Instant.now());

        listener.onConversationEvent(event); // must not throw
    }
}

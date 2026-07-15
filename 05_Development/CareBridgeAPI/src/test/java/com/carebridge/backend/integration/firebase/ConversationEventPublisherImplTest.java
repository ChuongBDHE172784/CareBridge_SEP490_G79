package com.carebridge.backend.integration.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.event.ConversationEventDomainEvent;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationEventPublisherImplTest {

    @Mock private DirectConversationRepository conversationRepository;
    @Mock private IFirebaseRealtimeGateway gateway;

    private ConversationEventPublisherImpl publisher;
    private final Instant fixedNow = Instant.parse("2026-07-15T08:00:00Z");

    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID MOTHER_ID = UUID.randomUUID();
    private static final UUID EXPERT_ID = UUID.randomUUID();
    private static final UUID RESOURCE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        publisher = new ConversationEventPublisherImpl(conversationRepository, gateway, Clock.fixed(fixedNow, ZoneOffset.UTC));
    }

    private void setRealtimeEnabled(boolean enabled) throws Exception {
        Field field = ConversationEventPublisherImpl.class.getDeclaredField("realtimeEnabled");
        field.setAccessible(true);
        field.set(publisher, enabled);
    }

    private static DirectConversation conversation() {
        return DirectConversation.builder().id(CONVERSATION_ID).motherUserId(MOTHER_ID).expertUserId(EXPERT_ID).build();
    }

    // DCC-TC-021 — recipient is the counterpart of the actor, never the actor's own inbox.
    @Test
    void publish_actorIsMother_writesToExpertInbox() throws Exception {
        setRealtimeEnabled(true);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        ConversationEventDomainEvent event = new ConversationEventDomainEvent("MESSAGE_SENT", CONVERSATION_ID, MOTHER_ID, RESOURCE_ID, fixedNow);

        publisher.publishAfterCommit(event);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(gateway).write(pathCaptor.capture(), any());
        assertThat(pathCaptor.getValue()).startsWith("/user-conversation-events/" + EXPERT_ID + "/");
    }

    @Test
    void publish_actorIsExpert_writesToMotherInbox() throws Exception {
        setRealtimeEnabled(true);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        ConversationEventDomainEvent event = new ConversationEventDomainEvent("MESSAGE_SENT", CONVERSATION_ID, EXPERT_ID, RESOURCE_ID, fixedNow);

        publisher.publishAfterCommit(event);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(gateway).write(pathCaptor.capture(), any());
        assertThat(pathCaptor.getValue()).startsWith("/user-conversation-events/" + MOTHER_ID + "/");
    }

    // DCC-TC-011 — Firebase payload never contains messageBody or any field beyond the 5 fixed ones.
    @Test
    void publish_payloadContainsOnlyFiveFixedFields() throws Exception {
        setRealtimeEnabled(true);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        ConversationEventDomainEvent event = new ConversationEventDomainEvent("MESSAGE_SENT", CONVERSATION_ID, MOTHER_ID, RESOURCE_ID, fixedNow);

        publisher.publishAfterCommit(event);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gateway).write(anyString(), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.keySet()).containsExactlyInAnyOrder(
                "eventId", "eventType", "conversationId", "resourceId", "occurredAt");
        assertThat(payload.get("eventType")).isEqualTo("MESSAGE_SENT");
        assertThat(payload.get("conversationId")).isEqualTo(CONVERSATION_ID.toString());
        assertThat(payload.get("resourceId")).isEqualTo(RESOURCE_ID.toString());
        assertThat(payload).doesNotContainKey("messageBody");
        assertThat(payload).doesNotContainKey("actorUserId");
    }

    @Test
    void publish_realtimeDisabled_neverCallsGateway() throws Exception {
        setRealtimeEnabled(false);
        ConversationEventDomainEvent event = new ConversationEventDomainEvent("MESSAGE_SENT", CONVERSATION_ID, MOTHER_ID, RESOURCE_ID, fixedNow);

        publisher.publishAfterCommit(event);

        verify(gateway, never()).write(anyString(), any());
        verify(conversationRepository, never()).findById(any());
    }

    // BR-DCC-007 — gateway failure is swallowed (logged), never rethrown.
    @Test
    void publish_gatewayThrows_doesNotPropagate() throws Exception {
        setRealtimeEnabled(true);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        org.mockito.Mockito.doThrow(new RuntimeException("RTDB unreachable")).when(gateway).write(anyString(), any());
        ConversationEventDomainEvent event = new ConversationEventDomainEvent("MESSAGE_SENT", CONVERSATION_ID, MOTHER_ID, RESOURCE_ID, fixedNow);

        publisher.publishAfterCommit(event); // must not throw
    }
}

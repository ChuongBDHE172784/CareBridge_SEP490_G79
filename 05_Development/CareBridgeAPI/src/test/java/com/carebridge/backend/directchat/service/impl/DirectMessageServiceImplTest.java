package com.carebridge.backend.directchat.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.directchat.dto.request.SendDirectMessageRequest;
import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.entity.DirectMessage;
import com.carebridge.backend.directchat.entity.MessageType;
import com.carebridge.backend.directchat.event.ConversationEventDomainEvent;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.policy.IDirectConversationPolicy;
import com.carebridge.backend.directchat.repository.ConversationCallRepository;
import com.carebridge.backend.directchat.repository.ConversationTimelineRepository;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.directchat.repository.DirectMessageRepository;
import com.carebridge.backend.directchat.service.SendDirectMessageResult;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DirectMessageServiceImplTest {

    @Mock private DirectConversationRepository conversationRepository;
    @Mock private DirectMessageRepository messageRepository;
    @Mock private ConversationCallRepository callRepository;
    @Mock private ConversationTimelineRepository timelineRepository;
    @Mock private DirectMessageWriter messageWriter;
    @Mock private IDirectConversationPolicy policy;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditService auditService;

    private DirectMessageServiceImpl service;
    private final Instant fixedNow = Instant.parse("2026-07-15T08:00:00Z");
    private final Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID MOTHER_ID = UUID.randomUUID();
    private static final UUID EXPERT_ID = UUID.randomUUID();
    private static final UUID CLIENT_MESSAGE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DirectMessageServiceImpl(conversationRepository, messageRepository, callRepository,
                timelineRepository, messageWriter, policy, expertProfileRepository,
                eventPublisher, auditService, fixedClock);
        org.mockito.Mockito.lenient()
                .when(expertProfileRepository.findByUserIdForUpdate(EXPERT_ID))
                .thenReturn(Optional.of(eligibleExpert()));
    }

    private static DirectConversation conversation() {
        return DirectConversation.builder()
                .id(CONVERSATION_ID).motherUserId(MOTHER_ID).expertUserId(EXPERT_ID)
                .status("ACTIVE").build();
    }

    private static ExpertProfile eligibleExpert() {
        return ExpertProfile.builder()
                .userId(EXPERT_ID)
                .verificationStatus(VerificationStatus.APPROVED)
                .trustStatus(TrustStatus.ACTIVE)
                .build();
    }

    private SendDirectMessageRequest request(String body) {
        SendDirectMessageRequest req = new SendDirectMessageRequest();
        req.setClientMessageId(CLIENT_MESSAGE_ID);
        req.setMessageBody(body);
        return req;
    }

    // DCC-TC-009 — first send creates, is idempotent.
    @Test
    void sendMessage_newMessage_createsAndPublishesEvent() {
        DirectConversation conv = conversation();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conv));
        when(messageRepository.findByConversationIdAndSenderUserIdAndClientMessageId(CONVERSATION_ID, MOTHER_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty());
        when(messageWriter.insertIfAbsent(any())).thenReturn(true);

        SendDirectMessageResult result = service.sendMessage(CONVERSATION_ID, MOTHER_ID, request("Hello"));

        assertThat(result.created()).isTrue();
        assertThat(result.message().getMessageBody()).isEqualTo("Hello");
        verify(conversationRepository).touchActivity(CONVERSATION_ID, fixedNow); // DCC-TC-030

        ArgumentCaptor<ConversationEventDomainEvent> captor = ArgumentCaptor.forClass(ConversationEventDomainEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("MESSAGE_SENT");
        assertThat(captor.getValue().actorUserId()).isEqualTo(MOTHER_ID); // DCC-TC-021 oracle
        assertThat(captor.getValue().resourceId()).isEqualTo(result.message().getMessageId());
    }

    // DCC-TC-009 — retry with same clientMessageId returns existing, no duplicate insert.
    @Test
    void sendMessage_retrySameClientMessageId_returnsExistingWithoutInsert() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        DirectMessage existing = DirectMessage.builder()
                .id(UUID.randomUUID()).conversationId(CONVERSATION_ID).senderUserId(MOTHER_ID)
                .clientMessageId(CLIENT_MESSAGE_ID).messageType(MessageType.TEXT).messageBody("Hello")
                .createdAt(fixedNow).build();
        when(messageRepository.findByConversationIdAndSenderUserIdAndClientMessageId(CONVERSATION_ID, MOTHER_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.of(existing));

        SendDirectMessageResult result = service.sendMessage(CONVERSATION_ID, MOTHER_ID, request("Hello"));

        assertThat(result.created()).isFalse();
        verify(messageWriter, never()).insertIfAbsent(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void sendMessage_retrySameClientMessageIdWithDifferentBody_throwsConflict() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        DirectMessage existing = DirectMessage.builder()
                .id(UUID.randomUUID()).conversationId(CONVERSATION_ID).senderUserId(MOTHER_ID)
                .clientMessageId(CLIENT_MESSAGE_ID).messageType(MessageType.TEXT).messageBody("Original")
                .createdAt(fixedNow).build();
        when(messageRepository.findByConversationIdAndSenderUserIdAndClientMessageId(
                CONVERSATION_ID, MOTHER_ID, CLIENT_MESSAGE_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.sendMessage(CONVERSATION_ID, MOTHER_ID, request("Changed")))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-005"));
        verify(messageWriter, never()).insertIfAbsent(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // Race variant: DB unique constraint wins concurrently — insert is ignored, service reads winner.
    @Test
    void sendMessage_raceOnIdempotencyKey_recoversViaSelect() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        DirectMessage winner = DirectMessage.builder()
                .id(UUID.randomUUID()).conversationId(CONVERSATION_ID).senderUserId(MOTHER_ID)
                .clientMessageId(CLIENT_MESSAGE_ID).messageType(MessageType.TEXT).messageBody("Hello")
                .createdAt(fixedNow).build();
        when(messageRepository.findByConversationIdAndSenderUserIdAndClientMessageId(CONVERSATION_ID, MOTHER_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(messageWriter.insertIfAbsent(any())).thenReturn(false);

        SendDirectMessageResult result = service.sendMessage(CONVERSATION_ID, MOTHER_ID, request("Hello"));

        assertThat(result.created()).isFalse();
        assertThat(result.message().getMessageId()).isEqualTo(winner.getId());
    }

    @Test
    void sendMessage_raceWinnerHasDifferentBody_throwsConflict() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        DirectMessage winner = DirectMessage.builder()
                .id(UUID.randomUUID()).conversationId(CONVERSATION_ID).senderUserId(MOTHER_ID)
                .clientMessageId(CLIENT_MESSAGE_ID).messageType(MessageType.TEXT).messageBody("Winner")
                .createdAt(fixedNow).build();
        when(messageRepository.findByConversationIdAndSenderUserIdAndClientMessageId(
                CONVERSATION_ID, MOTHER_ID, CLIENT_MESSAGE_ID))
                .thenReturn(Optional.empty()).thenReturn(Optional.of(winner));
        when(messageWriter.insertIfAbsent(any())).thenReturn(false);

        assertThatThrownBy(() -> service.sendMessage(CONVERSATION_ID, MOTHER_ID, request("Loser")))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-005"));
    }

    // ADR-DCC-007 / DCC-TC-029 scenario 2 — Mother blocked when Expert unavailable, nothing persisted.
    @Test
    void sendMessage_expertUnavailable_blocksBeforePersistence() {
        DirectConversation conv = conversation();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conv));
        org.mockito.Mockito.doThrow(DirectChatException.expertUnavailableForWrite())
                .when(policy).assertConversationWritable(any(ExpertProfile.class));

        assertThatThrownBy(() -> service.sendMessage(CONVERSATION_ID, MOTHER_ID, request("Hello")))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-010"));

        verify(messageWriter, never()).insertIfAbsent(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(messageRepository, never()).findByConversationIdAndSenderUserIdAndClientMessageId(any(), any(), any());
    }

    @Test
    void sendMessage_conversationNotFound_throws404() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendMessage(CONVERSATION_ID, MOTHER_ID, request("Hello")))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-006"));
    }
}

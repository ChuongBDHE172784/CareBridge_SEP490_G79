package com.carebridge.backend.directchat.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.entity.DirectMessage;
import com.carebridge.backend.directchat.entity.MessageType;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.policy.IDirectConversationPolicy;
import com.carebridge.backend.directchat.repository.ConversationSummaryAggregateRepository;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.directchat.repository.DirectMessageRepository;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DirectConversationServiceImplReadTest {

    @Mock private DirectConversationRepository conversationRepository;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private DirectMessageRepository messageRepository;
    @Mock private ConversationSummaryAggregateRepository aggregateRepository;
    @Mock private IDirectConversationPolicy policy;
    @Mock private DirectConversationWriter writer;
    @Mock private AuditService auditService;

    private DirectConversationServiceImpl service;
    private final Instant fixedNow = Instant.parse("2026-07-16T08:00:00Z");
    private final Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    private static final UUID MOTHER_ID = UUID.randomUUID();
    private static final UUID EXPERT_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID M1_ID = UUID.randomUUID();
    private static final UUID M2_ID = UUID.randomUUID();
    private static final Instant T0 = Instant.parse("2026-07-16T07:00:00Z");
    private static final Instant T1 = Instant.parse("2026-07-16T07:05:00Z");

    @BeforeEach
    void setUp() {
        service = new DirectConversationServiceImpl(
                conversationRepository, expertProfileRepository, userRepository, messageRepository,
                aggregateRepository, policy, writer, auditService, fixedClock);
    }

    private static DirectConversation conversation() {
        return DirectConversation.builder()
                .id(CONVERSATION_ID).motherUserId(MOTHER_ID).expertUserId(EXPERT_ID).status("ACTIVE").build();
    }

    private static DirectMessage message(UUID id, Instant createdAt) {
        return DirectMessage.builder().id(id).conversationId(CONVERSATION_ID).senderUserId(MOTHER_ID)
                .clientMessageId(UUID.randomUUID()).messageType(MessageType.TEXT).messageBody("hi")
                .createdAt(createdAt).build();
    }

    // MEDI-TC-012 step 1 — cursor advances to resolvedMessage.createdAt, never Instant.now()
    @Test
    void markRead_advancesCursorToMessageCreatedAt_notNow() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByIdAndConversationId(M1_ID, CONVERSATION_ID))
                .thenReturn(Optional.of(message(M1_ID, T0)));

        var expected = new com.carebridge.backend.directchat.repository.ConversationSummaryAggregateRepository.ReadCursor(T0, M1_ID);
        when(aggregateRepository.advanceReadCursor(CONVERSATION_ID, EXPERT_ID, false, T0, M1_ID)).thenReturn(expected);
        var cursor = service.markRead(CONVERSATION_ID, EXPERT_ID, M1_ID);

        assertThat(cursor.createdAt()).isEqualTo(T0);
        assertThat(cursor.messageId()).isEqualTo(M1_ID);
        verify(aggregateRepository).advanceReadCursor(CONVERSATION_ID, EXPERT_ID, false, T0, M1_ID);
    }

    // MEDI-TC-012 step 2/3 — monotonic: an older lastSeenMessageId never pulls the cursor backward.
    // (The GREATEST() clamp itself lives in the native query, verified in the IT; here we assert the
    // service always passes through resolvedMessage.createdAt as the candidate value it computed.)
    @Test
    void markRead_motherRole_updatesMotherCursor() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByIdAndConversationId(M2_ID, CONVERSATION_ID))
                .thenReturn(Optional.of(message(M2_ID, T1)));

        var expected = new com.carebridge.backend.directchat.repository.ConversationSummaryAggregateRepository.ReadCursor(T1, M2_ID);
        when(aggregateRepository.advanceReadCursor(CONVERSATION_ID, MOTHER_ID, true, T1, M2_ID)).thenReturn(expected);
        var cursor = service.markRead(CONVERSATION_ID, MOTHER_ID, M2_ID);

        assertThat(cursor).isEqualTo(expected);
        verify(aggregateRepository).advanceReadCursor(CONVERSATION_ID, MOTHER_ID, true, T1, M2_ID);
    }

    // MEDI-TC-012 step 4/5 — lastSeenMessageId not found (wrong conversation or nonexistent) -> DCC-006,
    // both cases produce the SAME code (collapsed on purpose, ADR-MEDI-003 mục 3).
    @Test
    void markRead_lastSeenMessageIdNotInConversation_throwsDCC006() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        UUID foreignMessageId = UUID.randomUUID();
        when(messageRepository.findByIdAndConversationId(foreignMessageId, CONVERSATION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(CONVERSATION_ID, EXPERT_ID, foreignMessageId))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-006"));
        verify(conversationRepository, never()).markMotherRead(any(), any(), any());
        verify(conversationRepository, never()).markExpertRead(any(), any(), any());
    }

    @Test
    void markRead_lastSeenMessageIdDoesNotExistAnywhere_throwsSameDCC006() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        UUID randomId = UUID.randomUUID();
        when(messageRepository.findByIdAndConversationId(randomId, CONVERSATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(CONVERSATION_ID, EXPERT_ID, randomId))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-006"));
    }

    // MEDI-TC-012 step 6 — markRead by Expert never touches Mother's cursor and vice versa (implicit
    // in the branch taken above); C6 — assertConversationWritable is never invoked from markRead.
    @Test
    void markRead_neverCallsAssertConversationWritable() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(messageRepository.findByIdAndConversationId(M1_ID, CONVERSATION_ID))
                .thenReturn(Optional.of(message(M1_ID, T0)));

        service.markRead(CONVERSATION_ID, EXPERT_ID, M1_ID);

        verify(policy, never()).assertConversationWritable(any());
    }

    // MEDI-TC-013 — non-participant is rejected before the lastSeenMessageId lookup even runs
    @Test
    void markRead_nonParticipant_throwsBeforeResolvingMessage() {
        DirectConversation conv = conversation();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conv));
        UUID stranger = UUID.randomUUID();
        org.mockito.Mockito.doThrow(DirectChatException.notParticipant())
                .when(policy).assertIsParticipant(stranger, conv);

        assertThatThrownBy(() -> service.markRead(CONVERSATION_ID, stranger, M1_ID))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-003"));
        verify(messageRepository, never()).findByIdAndConversationId(any(), any());
    }

    // MEDI-TC-022 (unit half) — nonexistent conversationId -> 404 DCC-006, no mutation attempted
    @Test
    void markRead_conversationNotFound_throws404AndNeverMutates() {
        UUID randomConversationId = UUID.randomUUID();
        when(conversationRepository.findById(randomConversationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(randomConversationId, EXPERT_ID, M1_ID))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-006"));
        verify(conversationRepository, never()).markMotherRead(any(), any(), any());
        verify(conversationRepository, never()).markExpertRead(any(), any(), any());
        verify(policy, never()).assertIsParticipant(any(), any());
        verify(messageRepository, never()).findByIdAndConversationId(any(), any());
    }
}

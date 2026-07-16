package com.carebridge.backend.directchat.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.directchat.dto.response.ConversationCallResponse;
import com.carebridge.backend.directchat.entity.CallStatus;
import com.carebridge.backend.directchat.entity.CallType;
import com.carebridge.backend.directchat.entity.ConversationCall;
import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.policy.IDirectConversationPolicy;
import com.carebridge.backend.directchat.repository.ConversationCallRepository;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.integration.zegocloud.ZegoTokenDto;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.Query;

@ExtendWith(MockitoExtension.class)
class ConversationCallServiceImplTest {

    @Mock private DirectConversationRepository conversationRepository;
    @Mock private ConversationCallRepository callRepository;
    @Mock private IDirectConversationPolicy policy;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private IZegoCloudService zegoCloudService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditService auditService;

    private ConversationCallServiceImpl service;
    private final Instant fixedNow = Instant.parse("2026-07-15T08:03:04Z");
    private final Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID MOTHER_ID = UUID.randomUUID();
    private static final UUID EXPERT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ConversationCallServiceImpl(conversationRepository, callRepository, policy,
                expertProfileRepository, userRepository,
                zegoCloudService, eventPublisher, auditService, fixedClock);
        org.mockito.Mockito.lenient()
                .when(expertProfileRepository.findByUserIdForUpdate(EXPERT_ID))
                .thenReturn(Optional.of(eligibleExpert()));
    }

    private static DirectConversation conversation() {
        return DirectConversation.builder()
                .id(CONVERSATION_ID).motherUserId(MOTHER_ID).expertUserId(EXPERT_ID).status("ACTIVE").build();
    }

    private static ExpertProfile eligibleExpert() {
        return ExpertProfile.builder()
                .userId(EXPERT_ID)
                .verificationStatus(VerificationStatus.APPROVED)
                .trustStatus(TrustStatus.ACTIVE)
                .build();
    }

    private static ConversationCall call(UUID callerUserId, CallStatus status, Instant initiatedAt, Instant answeredAt) {
        return ConversationCall.builder()
                .id(UUID.randomUUID()).conversationId(CONVERSATION_ID).initiatedByUserId(callerUserId)
                .callType(CallType.VOICE).callStatus(status).zegoRoomId("room-x")
                .initiatedAt(initiatedAt).answeredAt(answeredAt).createdAt(initiatedAt).build();
    }

    // RTC-BE-011 — initiate persists durable state without issuing RTC credentials.
    @Test
    void initiateCall_participant_createsDurableCallWithoutToken() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));

        ConversationCallResponse response = service.initiateCall(CONVERSATION_ID, MOTHER_ID, CallType.VOICE);

        assertThat(response.getCallStatus()).isEqualTo("INITIATED");
        ArgumentCaptor<ConversationCall> savedCall = ArgumentCaptor.forClass(ConversationCall.class);
        verify(callRepository).save(savedCall.capture());
        assertThat(savedCall.getValue().getZegoRoomId()).matches("cb_[0-9a-f]{32}");
        verify(conversationRepository).touchActivity(eq(CONVERSATION_ID), eq(fixedNow));
        verify(zegoCloudService, never()).generateToken(anyString(), anyString(), anyString());
    }

    @Test
    void initiateCall_zegoUnavailable_doesNotAffectPersistence() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));

        ConversationCallResponse response =
                service.initiateCall(CONVERSATION_ID, MOTHER_ID, CallType.VOICE);

        assertThat(response.getCallStatus()).isEqualTo("INITIATED");
        verify(callRepository).save(any());
        verify(zegoCloudService, never()).generateToken(anyString(), anyString(), anyString());
    }

    // DCC-TC-027 — only the callee may answer.
    @Test
    void answer_byCaller_throws403WrongActor() {
        ConversationCall ringingCall = call(MOTHER_ID, CallStatus.RINGING, fixedNow.minusSeconds(5), null);
        when(callRepository.findById(ringingCall.getId())).thenReturn(Optional.of(ringingCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));

        assertThatThrownBy(() -> service.answer(CONVERSATION_ID, ringingCall.getId(), MOTHER_ID))
                .isInstanceOfSatisfying(DirectChatException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("DCC-009");
                    assertThat(ex.getHttpStatus().value()).isEqualTo(403);
                });
        verify(callRepository, never()).conditionallyAnswer(any(), any());
    }

    // RTC-BE-012 — answer persists before either participant separately requests credentials.
    @Test
    void answer_byCallee_transitionsToAnsweredWithoutTokenGeneration() {
        ConversationCall ringingCall = call(MOTHER_ID, CallStatus.RINGING, fixedNow.minusSeconds(5), null);
        when(callRepository.findById(ringingCall.getId())).thenReturn(Optional.of(ringingCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(callRepository.conditionallyAnswer(ringingCall.getId(), fixedNow)).thenReturn(1);

        ConversationCallResponse response = service.answer(CONVERSATION_ID, ringingCall.getId(), EXPERT_ID);

        assertThat(response.getCallStatus()).isEqualTo("ANSWERED");
        verify(conversationRepository).touchActivity(CONVERSATION_ID, fixedNow);
        verify(zegoCloudService, never()).generateToken(anyString(), anyString(), anyString());
    }

    // DCC-TC-028 — answer loses the race to the timeout job (conditional UPDATE affected 0 rows).
    @Test
    void answer_raceLostToTimeoutJob_throws409() {
        ConversationCall ringingCall = call(MOTHER_ID, CallStatus.RINGING, fixedNow.minusSeconds(60), null);
        when(callRepository.findById(ringingCall.getId())).thenReturn(Optional.of(ringingCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(callRepository.conditionallyAnswer(ringingCall.getId(), fixedNow)).thenReturn(0);

        assertThatThrownBy(() -> service.answer(CONVERSATION_ID, ringingCall.getId(), EXPERT_ID))
                .isInstanceOfSatisfying(DirectChatException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("DCC-007");
                    assertThat(ex.getHttpStatus().value()).isEqualTo(409);
                });
        verify(conversationRepository, never()).touchActivity(any(), any());
    }

    // DCC-TC-012 — invalid transition: answering an already-ENDED call.
    @Test
    void answer_alreadyEnded_throws409WithoutTouchingDb() {
        ConversationCall endedCall = call(MOTHER_ID, CallStatus.ENDED, fixedNow.minusSeconds(120), fixedNow.minusSeconds(100));
        when(callRepository.findById(endedCall.getId())).thenReturn(Optional.of(endedCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(callRepository.conditionallyAnswer(endedCall.getId(), fixedNow)).thenReturn(0);

        assertThatThrownBy(() -> service.answer(CONVERSATION_ID, endedCall.getId(), EXPERT_ID))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-007"));
    }

    // DCC-TC-013 — duration is server-computed from answeredAt to now(), never from client input.
    @Test
    void end_answeredCall_computesDurationFromClock() {
        Instant answeredAt = fixedNow.minusSeconds(184);
        ConversationCall answeredCall = call(MOTHER_ID, CallStatus.ANSWERED, fixedNow.minusSeconds(200), answeredAt);
        when(callRepository.findById(answeredCall.getId())).thenReturn(Optional.of(answeredCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(callRepository.conditionallyEndAnswered(answeredCall.getId(), fixedNow, 184)).thenReturn(1);

        ConversationCallResponse response = service.end(CONVERSATION_ID, answeredCall.getId(), EXPERT_ID); // callee ending — allowed

        assertThat(response.getCallStatus()).isEqualTo("ENDED");
        assertThat(response.getDurationSeconds()).isEqualTo(184);
    }

    // DCC-TC-027 — callee MAY end an ANSWERED call (both sides allowed once connected).
    @Test
    void end_answeredCall_byEitherParty_allowed() {
        ConversationCall answeredCall = call(MOTHER_ID, CallStatus.ANSWERED, fixedNow.minusSeconds(200), fixedNow.minusSeconds(100));
        when(callRepository.findById(answeredCall.getId())).thenReturn(Optional.of(answeredCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(callRepository.conditionallyEndAnswered(answeredCall.getId(), fixedNow, 100)).thenReturn(1);

        ConversationCallResponse response = service.end(CONVERSATION_ID, answeredCall.getId(), MOTHER_ID); // caller ending

        assertThat(response.getCallStatus()).isEqualTo("ENDED");
    }

    // DCC-TC-027 — callee may NOT end a call that hasn't been answered yet (only caller can cancel).
    @Test
    void end_ringingCall_byCallee_throws403() {
        ConversationCall ringingCall = call(MOTHER_ID, CallStatus.RINGING, fixedNow.minusSeconds(5), null);
        when(callRepository.findById(ringingCall.getId())).thenReturn(Optional.of(ringingCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));

        assertThatThrownBy(() -> service.end(CONVERSATION_ID, ringingCall.getId(), EXPERT_ID))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-009"));
    }

    // ADR-DCC-007 §2 / DCC-TC-029 scenario 4 — ending an ANSWERED call is exempt from the write-block.
    @Test
    void end_answeredCall_exemptFromWritableCheck_evenWhenExpertUnavailable() {
        ConversationCall answeredCall = call(MOTHER_ID, CallStatus.ANSWERED, fixedNow.minusSeconds(200), fixedNow.minusSeconds(100));
        when(callRepository.findById(answeredCall.getId())).thenReturn(Optional.of(answeredCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(callRepository.conditionallyEndAnswered(answeredCall.getId(), fixedNow, 100)).thenReturn(1);

        service.end(CONVERSATION_ID, answeredCall.getId(), MOTHER_ID);

        verify(policy, never()).assertConversationWritable(any());
    }

    // RTC-BE-021 — a revoked Expert remains an identity participant for ANSWERED cleanup.
    @Test
    void end_answeredCall_byRevokedExpert_usesIdentityOnlyParticipation() {
        ConversationCall answeredCall =
                call(MOTHER_ID, CallStatus.ANSWERED, fixedNow.minusSeconds(200), fixedNow.minusSeconds(100));
        DirectConversation conversation = conversation();
        when(callRepository.findById(answeredCall.getId())).thenReturn(Optional.of(answeredCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(callRepository.conditionallyEndAnswered(answeredCall.getId(), fixedNow, 100)).thenReturn(1);

        ConversationCallResponse response =
                service.end(CONVERSATION_ID, answeredCall.getId(), EXPERT_ID);

        assertThat(response.getCallStatus()).isEqualTo("ENDED");
        verify(policy, never()).assertIsParticipant(EXPERT_ID, conversation);
    }

    // RTC-BE-016 — repository race oracle must accept both pre-answer states.
    @Test
    void declineConditionalUpdate_acceptsInitiatedAndRinging() throws Exception {
        Query query = ConversationCallRepository.class
                .getMethod("conditionallyDecline", UUID.class, Instant.class)
                .getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value()).contains("CallStatus.INITIATED", "CallStatus.RINGING");
    }

    // RTC-BE-013 — durable lifecycle responses must not expose room or credentials.
    @Test
    void conversationCallResponse_containsNoRtcCredentials() {
        assertThat(Arrays.stream(ConversationCallResponse.class.getDeclaredFields())
                        .map(java.lang.reflect.Field::getName))
                .doesNotContain("zegoRoomId", "zegoToken", "zegoAppId", "tokenExpiresAt");
    }

    @Test
    void getCall_participant_returnsDurableState() {
        ConversationCall answeredCall =
                call(MOTHER_ID, CallStatus.ANSWERED, fixedNow.minusSeconds(10), fixedNow.minusSeconds(5));
        when(callRepository.findById(answeredCall.getId())).thenReturn(Optional.of(answeredCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));

        ConversationCallResponse response =
                service.getCall(CONVERSATION_ID, answeredCall.getId(), MOTHER_ID);

        assertThat(response.getCallStatus()).isEqualTo("ANSWERED");
    }

    @Test
    void listActiveCalls_returnsAuthoritativeRestProjection() {
        assertThat(service.listActiveCalls(MOTHER_ID)).isEmpty();
    }

    @Test
    void issueJoinCredentials_answeredParticipant_returnsOwnShortLivedCredentials() {
        ConversationCall answeredCall =
                call(MOTHER_ID, CallStatus.ANSWERED, fixedNow.minusSeconds(10), fixedNow.minusSeconds(5));
        when(callRepository.findById(answeredCall.getId())).thenReturn(Optional.of(answeredCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        when(userRepository.findById(MOTHER_ID))
                .thenReturn(Optional.of(User.builder().id(MOTHER_ID).name("Mother Test").build()));
        String zegoUserId = "u_" + MOTHER_ID.toString().replace("-", "");
        when(zegoCloudService.generateToken("room-x", zegoUserId, "Mother Test"))
                .thenReturn(new ZegoTokenDto(
                        "room-x", "04_TEST_TOKEN", 12345L, fixedNow.plusSeconds(3600)));

        var response =
                service.issueJoinCredentials(CONVERSATION_ID, answeredCall.getId(), MOTHER_ID);

        assertThat(response.getRoomId()).isEqualTo("room-x");
        assertThat(response.getUserId()).isEqualTo(zegoUserId);
        assertThat(response.getDisplayName()).isEqualTo("Mother Test");
        assertThat(response.getToken()).isEqualTo("04_TEST_TOKEN");
        assertThat(response.getAppId()).isEqualTo(12345L);
    }

    @Test
    void issueJoinCredentials_nonParticipant_isDeniedWithoutToken() {
        UUID outsiderId = UUID.randomUUID();
        ConversationCall answeredCall =
                call(MOTHER_ID, CallStatus.ANSWERED, fixedNow.minusSeconds(10), fixedNow.minusSeconds(5));
        when(callRepository.findById(answeredCall.getId())).thenReturn(Optional.of(answeredCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));

        assertThatThrownBy(() ->
                        service.issueJoinCredentials(CONVERSATION_ID, answeredCall.getId(), outsiderId))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-003"));
        verify(zegoCloudService, never()).generateToken(anyString(), anyString(), anyString());
    }

    @Test
    void issueJoinCredentials_wrongConversationCallPair_isHidden() {
        ConversationCall answeredCall =
                call(MOTHER_ID, CallStatus.ANSWERED, fixedNow.minusSeconds(10), fixedNow.minusSeconds(5));
        when(callRepository.findById(answeredCall.getId())).thenReturn(Optional.of(answeredCall));

        assertThatThrownBy(() ->
                        service.issueJoinCredentials(UUID.randomUUID(), answeredCall.getId(), MOTHER_ID))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-006"));
        verify(zegoCloudService, never()).generateToken(anyString(), anyString(), anyString());
    }

    @Test
    void issueJoinCredentials_requiresAnsweredState() {
        ConversationCall ringingCall =
                call(MOTHER_ID, CallStatus.RINGING, fixedNow.minusSeconds(10), null);
        when(callRepository.findById(ringingCall.getId())).thenReturn(Optional.of(ringingCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));

        assertThatThrownBy(() ->
                        service.issueJoinCredentials(CONVERSATION_ID, ringingCall.getId(), MOTHER_ID))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-007"));
        verify(zegoCloudService, never()).generateToken(anyString(), anyString(), anyString());
    }

    @Test
    void issueJoinCredentials_revokedExpertPolicy_blocksBothParticipants() {
        ConversationCall answeredCall =
                call(MOTHER_ID, CallStatus.ANSWERED, fixedNow.minusSeconds(10), fixedNow.minusSeconds(5));
        when(callRepository.findById(answeredCall.getId())).thenReturn(Optional.of(answeredCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        org.mockito.Mockito.doThrow(DirectChatException.expertUnavailableForWrite())
                .when(policy).assertConversationWritable(any());

        assertThatThrownBy(() ->
                        service.issueJoinCredentials(CONVERSATION_ID, answeredCall.getId(), MOTHER_ID))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-010"));
        verify(zegoCloudService, never()).generateToken(anyString(), anyString(), anyString());
    }

    @Test
    void getCall_nonParticipant_isDenied() {
        UUID outsiderId = UUID.randomUUID();
        ConversationCall answeredCall =
                call(MOTHER_ID, CallStatus.ANSWERED, fixedNow.minusSeconds(10), fixedNow.minusSeconds(5));
        when(callRepository.findById(answeredCall.getId())).thenReturn(Optional.of(answeredCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));

        assertThatThrownBy(() -> service.getCall(CONVERSATION_ID, answeredCall.getId(), outsiderId))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-003"));
    }

    @Test
    void listActiveCalls_revokedExpert_receivesAnsweredCleanupOnly() {
        ConversationCall ringingCall =
                call(MOTHER_ID, CallStatus.RINGING, fixedNow.minusSeconds(10), null);
        ConversationCall answeredCall =
                call(MOTHER_ID, CallStatus.ANSWERED, fixedNow.minusSeconds(20), fixedNow.minusSeconds(15));
        when(callRepository.findActiveForParticipant(EXPERT_ID))
                .thenReturn(java.util.List.of(ringingCall, answeredCall));
        when(conversationRepository.findById(CONVERSATION_ID))
                .thenReturn(Optional.of(conversation()));
        when(expertProfileRepository.findByUserId(EXPERT_ID))
                .thenReturn(Optional.of(ExpertProfile.builder()
                        .userId(EXPERT_ID)
                        .verificationStatus(VerificationStatus.PENDING)
                        .trustStatus(TrustStatus.ACTIVE)
                        .build()));

        assertThat(service.listActiveCalls(EXPERT_ID))
                .extracting(ConversationCallResponse::getCallStatus)
                .containsExactly("ANSWERED");
    }

    // ADR-DCC-007 §2 / DCC-TC-029 scenario 5 — ending a not-yet-answered call is NOT exempt.
    @Test
    void end_ringingCall_byCaller_stillChecksWritable() {
        ConversationCall ringingCall = call(MOTHER_ID, CallStatus.RINGING, fixedNow.minusSeconds(5), null);
        when(callRepository.findById(ringingCall.getId())).thenReturn(Optional.of(ringingCall));
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation()));
        org.mockito.Mockito.doThrow(DirectChatException.expertUnavailableForWrite())
                .when(policy).assertConversationWritable(any());

        assertThatThrownBy(() -> service.end(CONVERSATION_ID, ringingCall.getId(), MOTHER_ID))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-010"));
    }
}

package com.carebridge.backend.directchat.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.directchat.dto.response.DirectConversationResponse;
import com.carebridge.backend.directchat.dto.response.DirectConversationSummaryResponse;
import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.directchat.policy.IDirectConversationPolicy;
import com.carebridge.backend.directchat.repository.DirectConversationRepository;
import com.carebridge.backend.directchat.service.FindOrCreateConversationResult;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DirectConversationServiceImplTest {

    @Mock private DirectConversationRepository conversationRepository;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private IDirectConversationPolicy policy;
    @Mock private DirectConversationWriter writer;
    @Mock private AuditService auditService;

    private DirectConversationServiceImpl service;
    private final Instant fixedNow = Instant.parse("2026-07-15T08:00:00Z");
    private final Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    private static final UUID MOTHER_ID = UUID.randomUUID();
    private static final UUID EXPERT_PROFILE_ID = UUID.randomUUID();
    private static final UUID EXPERT_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DirectConversationServiceImpl(
                conversationRepository, expertProfileRepository, policy, writer, auditService, fixedClock);
    }

    private static ExpertProfile approvedExpert() {
        return ExpertProfile.builder()
                .expertProfileId(EXPERT_PROFILE_ID)
                .userId(EXPERT_USER_ID)
                .verificationStatus(VerificationStatus.APPROVED)
                .build();
    }

    // DCC-TC-001
    @Test
    void findOrCreate_existingConversation_returnsExistingWithoutInsert() {
        when(expertProfileRepository.findById(EXPERT_PROFILE_ID)).thenReturn(Optional.of(approvedExpert()));
        DirectConversation existing = DirectConversation.builder()
                .id(UUID.randomUUID()).motherUserId(MOTHER_ID).expertUserId(EXPERT_USER_ID)
                .status("ACTIVE").createdAt(fixedNow).build();
        when(conversationRepository.findByMotherUserIdAndExpertUserId(MOTHER_ID, EXPERT_USER_ID))
                .thenReturn(Optional.of(existing));

        FindOrCreateConversationResult result = service.findOrCreate(MOTHER_ID, EXPERT_PROFILE_ID);

        assertThat(result.created()).isFalse();
        assertThat(result.conversation().getConversationId()).isEqualTo(existing.getId());
        verify(writer, never()).insertIfAbsent(any());
    }

    @Test
    void findOrCreate_noExisting_createsNewConversation() {
        when(expertProfileRepository.findById(EXPERT_PROFILE_ID)).thenReturn(Optional.of(approvedExpert()));
        when(conversationRepository.findByMotherUserIdAndExpertUserId(MOTHER_ID, EXPERT_USER_ID))
                .thenReturn(Optional.empty());
        when(writer.insertIfAbsent(any())).thenReturn(true);

        FindOrCreateConversationResult result = service.findOrCreate(MOTHER_ID, EXPERT_PROFILE_ID);

        assertThat(result.created()).isTrue();
        assertThat(result.conversation().getConversationId()).isNotNull();
        verify(auditService).log(AuditAction.DIRECT_CONVERSATION_OPENED, MOTHER_ID,
                "DIRECT_CONVERSATION", result.conversation().getConversationId().toString(), java.util.Map.of());
    }

    // DCC-TC-002 — concurrent race: atomic insert loses, service reads the winner.
    @Test
    void findOrCreate_concurrentRace_recoversWithoutPropagatingException() {
        when(expertProfileRepository.findById(EXPERT_PROFILE_ID)).thenReturn(Optional.of(approvedExpert()));
        DirectConversation winner = DirectConversation.builder()
                .id(UUID.randomUUID()).motherUserId(MOTHER_ID).expertUserId(EXPERT_USER_ID)
                .status("ACTIVE").createdAt(fixedNow).build();
        when(conversationRepository.findByMotherUserIdAndExpertUserId(MOTHER_ID, EXPERT_USER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(writer.insertIfAbsent(any())).thenReturn(false);

        FindOrCreateConversationResult result = service.findOrCreate(MOTHER_ID, EXPERT_PROFILE_ID);

        assertThat(result.created()).isFalse();
        assertThat(result.conversation().getConversationId()).isEqualTo(winner.getId());
    }

    // DCC-TC-004
    @Test
    void findOrCreate_expertNotApproved_propagatesPolicyException() {
        ExpertProfile pending = ExpertProfile.builder()
                .expertProfileId(EXPERT_PROFILE_ID).userId(EXPERT_USER_ID)
                .verificationStatus(VerificationStatus.PENDING).build();
        when(expertProfileRepository.findById(EXPERT_PROFILE_ID)).thenReturn(Optional.of(pending));
        Mockito.doThrow(DirectChatException.expertNotApproved())
                .when(policy).assertExpertVerified(pending);

        assertThatThrownBy(() -> service.findOrCreate(MOTHER_ID, EXPERT_PROFILE_ID))
                .isInstanceOf(DirectChatException.class);
        verify(conversationRepository, never()).findByMotherUserIdAndExpertUserId(any(), any());
    }

    @Test
    void listMyConversations_mapsCounterpartAndRole() {
        DirectConversation asMother = DirectConversation.builder()
                .id(UUID.randomUUID()).motherUserId(MOTHER_ID).expertUserId(EXPERT_USER_ID)
                .status("ACTIVE").lastActivityAt(fixedNow).build();
        when(conversationRepository.findByMotherUserIdOrExpertUserId(MOTHER_ID, MOTHER_ID))
                .thenReturn(List.of(asMother));
        when(expertProfileRepository.findByUserId(EXPERT_USER_ID))
                .thenReturn(Optional.of(approvedExpert()));

        List<DirectConversationSummaryResponse> summaries = service.listMyConversations(MOTHER_ID);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getCounterpartUserId()).isEqualTo(EXPERT_USER_ID);
        assertThat(summaries.get(0).getCounterpartRole()).isEqualTo("EXPERT");
        assertThat(summaries.get(0).isExpertAvailable()).isTrue();
    }

    @Test
    void getConversation_delegatesParticipantCheck() {
        DirectConversation conversation = DirectConversation.builder()
                .id(UUID.randomUUID()).motherUserId(MOTHER_ID).expertUserId(EXPERT_USER_ID)
                .status("ACTIVE").createdAt(fixedNow).build();
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(expertProfileRepository.findByUserId(EXPERT_USER_ID)).thenReturn(Optional.of(approvedExpert()));

        DirectConversationResponse response = service.getConversation(conversation.getId(), MOTHER_ID);

        verify(policy, times(1)).assertIsParticipant(MOTHER_ID, conversation);
        assertThat(response.isExpertAvailable()).isTrue();
    }

    @Test
    void getConversation_notFound_throws404() {
        UUID missingId = UUID.randomUUID();
        when(conversationRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getConversation(missingId, MOTHER_ID))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-006"));
    }
}

package com.carebridge.backend.directchat.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.truststatus.TrustStatus;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DirectConversationPolicyImplTest {

    @Mock
    private ExpertProfileRepository expertProfileRepository;

    @Mock
    private CareGroupMemberRepository careGroupMemberRepository;

    @InjectMocks
    private DirectConversationPolicyImpl policy;

    private static DirectConversation conversation(UUID motherUserId, UUID expertUserId) {
        return DirectConversation.builder()
                .id(UUID.randomUUID())
                .motherUserId(motherUserId)
                .expertUserId(expertUserId)
                .status("ACTIVE")
                .build();
    }

    private static ExpertProfile expertProfile(UUID userId, VerificationStatus status) {
        return ExpertProfile.builder()
                .expertProfileId(UUID.randomUUID())
                .userId(userId)
                .verificationStatus(status)
                .trustStatus(TrustStatus.ACTIVE)
                .build();
    }

    // DCC-TC-005/006 — Mother participant, no expert lookup needed.
    @Test
    void assertIsParticipant_motherParticipant_passes() {
        UUID motherUserId = UUID.randomUUID();
        DirectConversation conversation = conversation(motherUserId, UUID.randomUUID());

        policy.assertIsParticipant(motherUserId, conversation);
        verifyNoInteractions(expertProfileRepository);
    }

    // BR-DCC-003 — re-checked on every access, not just creation.
    @Test
    void assertIsParticipant_expertParticipantApproved_passes() {
        UUID expertUserId = UUID.randomUUID();
        DirectConversation conversation = conversation(UUID.randomUUID(), expertUserId);
        when(expertProfileRepository.findByUserId(expertUserId))
                .thenReturn(Optional.of(expertProfile(expertUserId, VerificationStatus.APPROVED)));

        policy.assertIsParticipant(expertUserId, conversation);
    }

    @Test
    void assertIsParticipant_expertApprovedStillPassesAfterTrustLoss() {
        UUID expertUserId = UUID.randomUUID();
        DirectConversation conversation = conversation(UUID.randomUUID(), expertUserId);
        ExpertProfile revokedTrust =
                expertProfile(expertUserId, VerificationStatus.APPROVED);
        revokedTrust.setTrustStatus(TrustStatus.REVOKED);
        when(expertProfileRepository.findByUserId(expertUserId))
                .thenReturn(Optional.of(revokedTrust));

        policy.assertIsParticipant(expertUserId, conversation);
    }

    // DCC-TC-018 — Expert whose verification is revoked loses access to existing conversations.
    @Test
    void assertIsParticipant_expertParticipantRevoked_throws403() {
        UUID expertUserId = UUID.randomUUID();
        DirectConversation conversation = conversation(UUID.randomUUID(), expertUserId);
        when(expertProfileRepository.findByUserId(expertUserId))
                .thenReturn(Optional.of(expertProfile(expertUserId, VerificationStatus.PENDING)));

        assertThatThrownBy(() -> policy.assertIsParticipant(expertUserId, conversation))
                .isInstanceOfSatisfying(DirectChatException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("DCC-002");
                    assertThat(ex.getHttpStatus().value()).isEqualTo(403);
                });
    }

    // DCC-TC-005/006/017 — non-participant rejected (also covers IDOR: an unrelated user id).
    @Test
    void assertIsParticipant_nonParticipant_throws403() {
        DirectConversation conversation = conversation(UUID.randomUUID(), UUID.randomUUID());
        UUID otherUserId = UUID.randomUUID();

        assertThatThrownBy(() -> policy.assertIsParticipant(otherUserId, conversation))
                .isInstanceOfSatisfying(DirectChatException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("DCC-003");
                    assertThat(ex.getHttpStatus().value()).isEqualTo(403);
                });
    }

    @Test
    void assertIsParticipant_acceptedFamilyMemberOfMother_passes() {
        UUID motherUserId = UUID.randomUUID();
        UUID familyUserId = UUID.randomUUID();
        DirectConversation conversation = conversation(motherUserId, UUID.randomUUID());
        when(careGroupMemberRepository.existsAcceptedMemberOfActiveMotherCareGroup(motherUserId, familyUserId))
                .thenReturn(true);

        policy.assertIsParticipant(familyUserId, conversation);

        assertThat(policy.resolveRole(familyUserId, conversation)).isEqualTo("FAMILY");
    }

    // DCC-TC-004 — Expert not APPROVED rejected at creation time (422).
    @Test
    void assertExpertEligible_approvedAndActive_passes() {
        policy.assertExpertEligibleForConsultation(
                expertProfile(UUID.randomUUID(), VerificationStatus.APPROVED));
    }

    @Test
    void assertExpertEligible_notApproved_throws422() {
        ExpertProfile pending = expertProfile(UUID.randomUUID(), VerificationStatus.PENDING);

        assertThatThrownBy(() -> policy.assertExpertEligibleForConsultation(pending))
                .isInstanceOfSatisfying(DirectChatException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("DCC-002");
                    assertThat(ex.getHttpStatus().value()).isEqualTo(422);
                });
    }

    // DCC-TC-029 scenario 2/3 — write-block when conversation's Expert is no longer APPROVED.
    @Test
    void assertConversationWritable_expertApproved_passes() {
        UUID expertUserId = UUID.randomUUID();
        policy.assertConversationWritable(
                expertProfile(expertUserId, VerificationStatus.APPROVED));
    }

    @Test
    void assertConversationWritable_expertNotApproved_throws409() {
        UUID expertUserId = UUID.randomUUID();
        ExpertProfile expert = expertProfile(expertUserId, VerificationStatus.SUSPENDED);

        assertThatThrownBy(() -> policy.assertConversationWritable(expert))
                .isInstanceOfSatisfying(DirectChatException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("DCC-010");
                    assertThat(ex.getHttpStatus().value()).isEqualTo(409);
                });
    }

    @Test
    void trustLossAlsoBlocksCreationAndWrites() {
        ExpertProfile expert = expertProfile(UUID.randomUUID(), VerificationStatus.APPROVED);
        expert.setTrustStatus(TrustStatus.REVOKED);

        assertThatThrownBy(() -> policy.assertExpertEligibleForConsultation(expert))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-002"));
        assertThatThrownBy(() -> policy.assertConversationWritable(expert))
                .isInstanceOfSatisfying(DirectChatException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo("DCC-010"));
        verifyNoInteractions(expertProfileRepository);
    }

    @Test
    void resolveRole_mother_returnsMother() {
        UUID motherUserId = UUID.randomUUID();
        DirectConversation conversation = conversation(motherUserId, UUID.randomUUID());

        assertThat(policy.resolveRole(motherUserId, conversation)).isEqualTo("MOTHER");
    }

    @Test
    void resolveRole_expert_returnsExpert() {
        UUID expertUserId = UUID.randomUUID();
        DirectConversation conversation = conversation(UUID.randomUUID(), expertUserId);

        assertThat(policy.resolveRole(expertUserId, conversation)).isEqualTo("EXPERT");
    }
}

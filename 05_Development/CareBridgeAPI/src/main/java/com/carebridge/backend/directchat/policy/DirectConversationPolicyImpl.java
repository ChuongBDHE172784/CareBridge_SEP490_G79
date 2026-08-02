package com.carebridge.backend.directchat.policy;

import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DirectConversationPolicyImpl implements IDirectConversationPolicy {

    private final ExpertProfileRepository expertProfileRepository;
    private final CareGroupMemberRepository careGroupMemberRepository;

    @Override
    public void assertIsParticipant(UUID currentUserId, DirectConversation conversation) {
        if (currentUserId.equals(conversation.getMotherUserId())) {
            return;
        }
        if (currentUserId.equals(conversation.getExpertUserId())) {
            // BR-DCC-003: re-checked on every access, not just at conversation creation.
            ExpertProfile expertProfile = expertProfileRepository.findByUserId(currentUserId)
                    .orElseThrow(DirectChatException::notParticipant);
            if (expertProfile.getVerificationStatus() != VerificationStatus.APPROVED) {
                throw DirectChatException.expertNoLongerApproved();
            }
            return;
        }
        if (careGroupMemberRepository.existsAcceptedMemberOfActiveMotherCareGroup(
                conversation.getMotherUserId(), currentUserId)) {
            return;
        }
        throw DirectChatException.notParticipant();
    }

    @Override
    public void assertExpertEligibleForConsultation(ExpertProfile expertProfile) {
        if (!expertProfile.isEligibleForConsultation()) {
            throw DirectChatException.expertNotEligibleForConsultation();
        }
    }

    @Override
    public void assertConversationWritable(ExpertProfile lockedExpertProfile) {
        if (!lockedExpertProfile.isEligibleForConsultation()) {
            throw DirectChatException.expertUnavailableForWrite();
        }
    }

    @Override
    public String resolveRole(UUID currentUserId, DirectConversation conversation) {
        if (currentUserId.equals(conversation.getMotherUserId())) return "MOTHER";
        if (currentUserId.equals(conversation.getExpertUserId())) return "EXPERT";
        if (careGroupMemberRepository.existsAcceptedMemberOfActiveMotherCareGroup(
                conversation.getMotherUserId(), currentUserId)) return "FAMILY";
        throw DirectChatException.notParticipant();
    }
}

package com.carebridge.backend.directchat.policy;

import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.directchat.exception.DirectChatException;
import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DirectConversationPolicyImpl implements IDirectConversationPolicy {

    private final ExpertProfileRepository expertProfileRepository;

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
        throw DirectChatException.notParticipant();
    }

    @Override
    public void assertExpertVerified(ExpertProfile expertProfile) {
        if (expertProfile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw DirectChatException.expertNotApproved();
        }
    }

    @Override
    public void assertConversationWritable(DirectConversation conversation) {
        // ADR-DCC-007 / BR-DCC-015: blocks BOTH participants' writes, not just the Expert's own.
        ExpertProfile expertProfile = expertProfileRepository.findByUserId(conversation.getExpertUserId())
                .orElseThrow(DirectChatException::expertUnavailableForWrite);
        if (expertProfile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw DirectChatException.expertUnavailableForWrite();
        }
    }

    @Override
    public String resolveRole(UUID currentUserId, DirectConversation conversation) {
        return currentUserId.equals(conversation.getMotherUserId()) ? "MOTHER" : "EXPERT";
    }
}

package com.carebridge.backend.directchat.policy;

import com.carebridge.backend.directchat.entity.DirectConversation;
import com.carebridge.backend.expert.entity.ExpertProfile;
import java.util.UUID;

public interface IDirectConversationPolicy {

    /** BR-DCC-009 / BR-DCC-003: participant-only, re-checking Expert APPROVED on every call. */
    void assertIsParticipant(UUID currentUserId, DirectConversation conversation);

    /** BR-DCC-003: creation-time gate for find-or-create. */
    void assertExpertEligibleForConsultation(ExpertProfile expertProfile);

    /** Caller must pass an ExpertProfile locked in the current transaction. */
    void assertConversationWritable(ExpertProfile lockedExpertProfile);

    /** "MOTHER" or "EXPERT" — currentUserId's role within this specific conversation. */
    String resolveRole(UUID currentUserId, DirectConversation conversation);
}

package com.carebridge.backend.directchat.service;

import com.carebridge.backend.directchat.dto.response.ConversationCallResponse;
import com.carebridge.backend.directchat.dto.response.ZegoJoinCredentialsResponse;
import com.carebridge.backend.directchat.entity.CallType;
import java.util.List;
import java.util.UUID;

public interface IConversationCallService {

    ConversationCallResponse initiateCall(UUID conversationId, UUID callerUserId, CallType type);

    ConversationCallResponse getCall(UUID conversationId, UUID callId, UUID currentUserId);

    List<ConversationCallResponse> listActiveCalls(UUID currentUserId);

    /** Callee only. */
    ConversationCallResponse markRinging(UUID conversationId, UUID callId, UUID currentUserId);

    /** Callee only; conditional UPDATE races against CallTimeoutReconciliationJob (ADR-DCC-005). */
    ConversationCallResponse answer(UUID conversationId, UUID callId, UUID currentUserId);

    /** Callee only. */
    ConversationCallResponse decline(UUID conversationId, UUID callId, UUID currentUserId);

    /**
     * Caller may end from any non-terminal state; callee may only end an ANSWERED call.
     * ADR-DCC-007 §2: skips assertConversationWritable() only when the call is ANSWERED.
     */
    ConversationCallResponse end(UUID conversationId, UUID callId, UUID currentUserId);

    ZegoJoinCredentialsResponse issueJoinCredentials(
            UUID conversationId, UUID callId, UUID currentUserId);
}

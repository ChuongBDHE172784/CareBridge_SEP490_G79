package com.carebridge.backend.directchat.service;

import com.carebridge.backend.directchat.dto.response.DirectConversationResponse;
import com.carebridge.backend.directchat.dto.response.DirectConversationSummaryResponse;
import java.util.List;
import java.util.UUID;

public interface IDirectConversationService {

    /** BR-DCC-002: find-or-create, idempotent under concurrent requests. Mother only. */
    FindOrCreateConversationResult findOrCreate(UUID motherUserId, UUID expertProfileId);

    List<DirectConversationSummaryResponse> listMyConversations(UUID currentUserId);

    DirectConversationResponse getConversation(UUID conversationId, UUID currentUserId);
}

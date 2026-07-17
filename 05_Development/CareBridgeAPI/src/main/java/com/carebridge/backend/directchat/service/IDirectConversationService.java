package com.carebridge.backend.directchat.service;

import com.carebridge.backend.directchat.dto.response.DirectConversationResponse;
import com.carebridge.backend.directchat.dto.response.DirectConversationSummaryResponse;
import com.carebridge.backend.directchat.dto.response.UnreadSummaryResponse;
import com.carebridge.backend.directchat.repository.ConversationSummaryAggregateRepository;
import java.util.List;
import java.util.UUID;

public interface IDirectConversationService {

    /** BR-DCC-002: find-or-create, idempotent under concurrent requests. Mother only. */
    FindOrCreateConversationResult findOrCreate(UUID motherUserId, UUID expertProfileId);

    List<DirectConversationSummaryResponse> listMyConversations(UUID currentUserId);

    DirectConversationResponse getConversation(UUID conversationId, UUID currentUserId);

    /**
     * ADR-MEDI-003: lastSeenMessageId is validated to belong to conversationId, then the caller's
     * read cursor advances (monotonically) to that message's createdAt — never Instant.now().
     * Returns the cursor value actually applied.
     */
    ConversationSummaryAggregateRepository.ReadCursor markRead(
            UUID conversationId, UUID currentUserId, UUID lastSeenMessageId);

    UnreadSummaryResponse getUnreadSummary(UUID currentUserId);
}

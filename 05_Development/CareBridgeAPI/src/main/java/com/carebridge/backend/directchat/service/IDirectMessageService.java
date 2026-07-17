package com.carebridge.backend.directchat.service;

import com.carebridge.backend.directchat.dto.request.SendDirectMessageRequest;
import com.carebridge.backend.directchat.dto.response.TimelinePageResponse;
import java.util.UUID;

public interface IDirectMessageService {

    /** ADR-DCC-007: assertConversationWritable() runs before any persistence. */
    SendDirectMessageResult sendMessage(UUID conversationId, UUID senderUserId, SendDirectMessageRequest request);

    TimelinePageResponse getTimeline(UUID conversationId, UUID currentUserId, String after, String before, int limit);
}

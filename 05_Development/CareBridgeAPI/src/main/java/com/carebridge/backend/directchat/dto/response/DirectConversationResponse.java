package com.carebridge.backend.directchat.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DirectConversationResponse {
    private final UUID conversationId;
    private final UUID motherUserId;
    private final UUID expertUserId;
    private final String status;
    private final Instant createdAt;
    private final Instant lastActivityAt;
    // ADR-DCC-007 §4: computed at response time, not persisted — drives the client's
    // read-only "Expert is no longer available" banner. Server-side DCC-010 is the real gate.
    private final boolean expertAvailable;
    private final String counterpartDisplayName;
    private final String counterpartAvatarUrl;
    private final String counterpartRole;

    public DirectConversationResponse(
            UUID conversationId,
            UUID motherUserId,
            UUID expertUserId,
            String status,
            Instant createdAt,
            Instant lastActivityAt,
            boolean expertAvailable) {
        this(
                conversationId,
                motherUserId,
                expertUserId,
                status,
                createdAt,
                lastActivityAt,
                expertAvailable,
                null,
                null,
                null);
    }
}

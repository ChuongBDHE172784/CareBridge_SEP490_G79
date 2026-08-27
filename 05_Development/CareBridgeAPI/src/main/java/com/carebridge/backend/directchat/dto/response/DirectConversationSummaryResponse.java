package com.carebridge.backend.directchat.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// ADR-MEDI-002: extended with counterpart display data, last-message preview, and unread count so
// the mobile client never needs a second call per row.
@Getter
@Builder
@AllArgsConstructor
public class DirectConversationSummaryResponse {
    private final UUID conversationId;
    private final UUID counterpartUserId;
    private final String counterpartRole; // "MOTHER" or "EXPERT" — the OTHER participant, from viewer's perspective
    private final Instant lastActivityAt;
    private final boolean expertAvailable;
    private final String counterpartDisplayName;
    private final String counterpartAvatarUrl;
    private final String counterpartSpecialty; // null unless counterpartRole == EXPERT
    private final String lastMessagePreview; // null if no message yet, truncated to 120 chars
    private final Instant lastMessageAt;
    private final int unreadCount;
    private final String conversationStatus;
}

package com.carebridge.backend.directchat.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DirectConversationSummaryResponse {
    private final UUID conversationId;
    private final UUID counterpartUserId;
    private final String counterpartRole; // "MOTHER" or "EXPERT" — the OTHER participant, from viewer's perspective
    private final Instant lastActivityAt;
    private final boolean expertAvailable;
}

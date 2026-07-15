package com.carebridge.backend.directchat.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ConversationCallResponse {
    private final UUID callId;
    private final UUID conversationId;
    private final UUID initiatedByUserId;
    private final String callType;
    private final String callStatus;
    private final Instant initiatedAt;
    private final Instant answeredAt;
    private final Instant endedAt;
    private final Integer durationSeconds;

    // Populated only on initiate() (for the caller) and answer() (for the callee) —
    // ADR-DCC-003: token/room provisioning is in scope, live RTC UI is not (TDS §1.1).
    private final String zegoRoomId;
    private final String zegoToken;
    private final Long zegoAppId;
    private final Instant tokenExpiresAt;
}

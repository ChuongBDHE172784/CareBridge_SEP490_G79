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
    private final UUID recordingFileId;
    private final Boolean consentAttested;
    private final String recordingStatus;
    private final Integer recordedDurationSeconds;
}

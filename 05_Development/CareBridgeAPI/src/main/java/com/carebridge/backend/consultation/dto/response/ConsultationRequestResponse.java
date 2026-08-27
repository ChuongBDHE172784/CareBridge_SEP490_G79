package com.carebridge.backend.consultation.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ConsultationRequestResponse {
    private final UUID id;
    private final UUID expertProfileId;
    private final String counterpartDisplayName;
    private final String counterpartAvatarUrl;
    private final String topic;
    private final String description;
    private final Instant preferredWindowStart;
    private final Instant preferredWindowEnd;
    private final String status;
    private final String rejectReason;
    private final UUID directConversationId;
    private final Instant respondedAt;
    private final Instant expiresAt;
    private final Instant createdAt;
}

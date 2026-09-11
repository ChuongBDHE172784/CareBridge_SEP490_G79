package com.carebridge.backend.consultation.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ConsultationRequestSummaryResponse {
    private final UUID id;
    private final String counterpartDisplayName;
    private final String topic;
    private final String status;
    private final Instant createdAt;
    private final UUID directConversationId;

    public ConsultationRequestSummaryResponse(
            UUID id,
            String counterpartDisplayName,
            String topic,
            String status,
            Instant createdAt) {
        this(id, counterpartDisplayName, topic, status, createdAt, null);
    }
}

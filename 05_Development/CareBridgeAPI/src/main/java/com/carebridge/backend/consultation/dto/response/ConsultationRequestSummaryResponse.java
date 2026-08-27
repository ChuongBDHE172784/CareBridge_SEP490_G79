package com.carebridge.backend.consultation.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConsultationRequestSummaryResponse {
    private final UUID id;
    private final String counterpartDisplayName;
    private final String topic;
    private final String status;
    private final Instant createdAt;
}

package com.carebridge.backend.triage.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IntakeSessionResponse {
    private UUID sessionId;
    private String stage;
    private String status;
    private String riskLevel;
    private String disclaimer;
    private Instant createdAt;
    private Instant completedAt;
    private UUID journeyId;
    private String originDashboard;
    private UUID originReferenceId;
    private UUID continuationToken;
    private Instant continuationExpiresAt;
}

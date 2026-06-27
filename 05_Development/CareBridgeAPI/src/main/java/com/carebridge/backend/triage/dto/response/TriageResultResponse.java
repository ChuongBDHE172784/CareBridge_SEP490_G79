package com.carebridge.backend.triage.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TriageResultResponse {
    private UUID sessionId;
    private String riskLevel;
    private String summary;
    private String disclaimer;
    private String status;
    private Instant createdAt;
    private Instant completedAt;
}

package com.carebridge.backend.safety.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImuMonitoringSessionResponse {
    private UUID sessionId;
    private UUID userId;
    private String status;
    private String sensitivityLevel;
    private Instant startedAt;
    private Instant endedAt;
}

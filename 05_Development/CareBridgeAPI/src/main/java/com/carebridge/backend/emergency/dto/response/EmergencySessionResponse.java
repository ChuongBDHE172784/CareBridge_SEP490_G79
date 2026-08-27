package com.carebridge.backend.emergency.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencySessionResponse {

    private UUID sessionId;
    private UUID userId;
    private String status;
    private String triggerSource;
    private BigDecimal userLatitude;
    private BigDecimal userLongitude;
    private Instant createdAt;
    private Instant resolvedAt;
}

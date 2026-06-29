package com.carebridge.backend.safety.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyEventResponse {
    private UUID id;
    private UUID userId;
    private String eventType;
    private double magnitude;
    private BigDecimal userLatitude;
    private BigDecimal userLongitude;
    private Instant detectedAt;
}

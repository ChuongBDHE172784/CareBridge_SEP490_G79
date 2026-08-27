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
public class EmergencyHandoffResponse {
    private UUID handoffId;
    private UUID userId;
    private UUID triageHandoffId;
    private String riskLevel;
    private BigDecimal userLatitude;
    private BigDecimal userLongitude;
    private UUID selectedFacilityId;
    private String summary;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}

package com.carebridge.backend.emergency.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEmergencyHandoffRequest {
    @NotNull
    private UUID triageHandoffId;

    @NotNull
    private String riskLevel;

    private BigDecimal userLatitude;
    private BigDecimal userLongitude;
    private String symptomSummary;
    private UUID selectedFacilityId;
}

package com.carebridge.backend.nearbycare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNearbySupportRequest {

    @NotBlank(message = "supportType is required")
    private String supportType;

    private String description;

    @NotNull(message = "latitude is required")
    private BigDecimal latitude;

    @NotNull(message = "longitude is required")
    private BigDecimal longitude;

    @NotBlank(message = "consentStatus is required")
    private String consentStatus;
}

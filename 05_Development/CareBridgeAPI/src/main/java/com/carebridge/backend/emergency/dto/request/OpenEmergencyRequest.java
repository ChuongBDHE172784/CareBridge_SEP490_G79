package com.carebridge.backend.emergency.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenEmergencyRequest {

    @NotBlank(message = "triggerSource must not be blank")
    private String triggerSource;

    private BigDecimal userLatitude;

    private BigDecimal userLongitude;
}

package com.carebridge.backend.safety.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImuDataRequest {

    @NotNull
    private Double accelerometerX;

    @NotNull
    private Double accelerometerY;

    @NotNull
    private Double accelerometerZ;

    @NotNull
    private Double gyroscopeX;

    @NotNull
    private Double gyroscopeY;

    @NotNull
    private Double gyroscopeZ;

    @NotNull
    private Instant timestamp;

    @Size(max = 160)
    private String signalId;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal longitude;

    /**
     * True only after the mobile detector has observed its complete
     * free-fall, impact, and post-impact immobility sequence.
     */
    private boolean onDeviceFallConfirmed;
}

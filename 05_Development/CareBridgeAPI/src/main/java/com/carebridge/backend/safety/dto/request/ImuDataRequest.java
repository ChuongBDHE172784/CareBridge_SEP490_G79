package com.carebridge.backend.safety.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.Instant;

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
}

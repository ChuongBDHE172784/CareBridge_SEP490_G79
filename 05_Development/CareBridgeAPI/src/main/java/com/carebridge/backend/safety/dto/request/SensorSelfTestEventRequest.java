package com.carebridge.backend.safety.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorSelfTestEventRequest {

    @NotBlank
    @Size(max = 140)
    private String testId;

    @NotNull
    private Instant detectedAt;

    @NotNull
    @PositiveOrZero
    private Double accelerationMagnitude;

    @NotNull
    @PositiveOrZero
    private Double gyroscopeMagnitude;
}

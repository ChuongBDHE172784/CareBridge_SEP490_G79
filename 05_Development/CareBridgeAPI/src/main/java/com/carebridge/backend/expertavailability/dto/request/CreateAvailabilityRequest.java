package com.carebridge.backend.expertavailability.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAvailabilityRequest {
    @NotNull
    private Instant startAt;

    @NotNull
    private Instant endAt;

    @NotNull
    private String channelType;

    private String status;
}

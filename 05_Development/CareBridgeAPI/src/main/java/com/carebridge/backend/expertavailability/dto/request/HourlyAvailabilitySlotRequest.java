package com.carebridge.backend.expertavailability.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
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
public class HourlyAvailabilitySlotRequest {

    @NotNull
    private LocalTime startTime;
}

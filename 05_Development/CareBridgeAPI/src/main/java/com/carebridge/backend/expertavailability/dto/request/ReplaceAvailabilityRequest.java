package com.carebridge.backend.expertavailability.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
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
public class ReplaceAvailabilityRequest {

    @NotEmpty
    private List<@NotNull LocalDate> targetDates;

    @NotNull
    private String timeZone;

    @NotNull
    private String channelType;

    @NotNull
    @Valid
    private List<HourlyAvailabilitySlotRequest> slots;
}

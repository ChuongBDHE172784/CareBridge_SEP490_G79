package com.carebridge.backend.family.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RelinkCareGroupJourneyRequest {

    @NotNull
    private UUID journeyId;
}

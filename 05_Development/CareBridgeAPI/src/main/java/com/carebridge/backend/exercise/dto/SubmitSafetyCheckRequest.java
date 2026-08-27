package com.carebridge.backend.exercise.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
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
public class SubmitSafetyCheckRequest {

    @NotNull
    private Boolean q1NoDizziness;

    @NotNull
    private Boolean q2NoContractions;

    @NotNull
    private Boolean q3NoBleeding;

    @NotNull
    private Boolean q4HydratedAndFed;

    private UUID journeyId;

    @Size(max = 500)
    private String notes;
}

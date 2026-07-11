package com.carebridge.backend.vaccination.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PostponeVaccinationRequest {
    @NotBlank
    @Size(max = 200)
    private String vaccineName;

    @NotNull
    @Min(1)
    private Short doseNumber;

    @NotNull
    private LocalDate newScheduledDate;

    @NotBlank
    private String reason;
}

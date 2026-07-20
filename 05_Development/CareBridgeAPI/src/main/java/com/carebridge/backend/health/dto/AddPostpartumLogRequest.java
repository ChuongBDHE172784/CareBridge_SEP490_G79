package com.carebridge.backend.health.dto;

import com.carebridge.backend.health.entity.BleedingLevel;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class AddPostpartumLogRequest {

    @NotNull
    private UUID submissionId;

    @NotNull
    @PastOrPresent
    private LocalDate logDate;

    @Min(0) @Max(10)
    private Short painLevel;

    private BleedingLevel bleedingLevel;

    @Min(0) @Max(10)
    private Short moodLevel;

    @DecimalMin("0.0") @DecimalMax("24.0")
    private BigDecimal sleepHours;

    @Size(max = 1000)
    private String breastfeedingNote;

    @Size(max = 2000)
    private String symptomNote;
}

package com.carebridge.backend.health.dto;

import com.carebridge.backend.health.entity.BleedingLevel;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AddPostpartumLogRequest {

    @NotNull
    private LocalDate logDate;

    @Min(0) @Max(10)
    private Integer painLevel;

    private BleedingLevel bleedingLevel;

    @Min(0) @Max(10)
    private Integer moodLevel;

    @DecimalMin("0.0") @DecimalMax("24.0")
    private BigDecimal sleepHours;

    @Size(max = 1000)
    private String breastfeedingNote;

    @Size(max = 2000)
    private String symptomNote;
}

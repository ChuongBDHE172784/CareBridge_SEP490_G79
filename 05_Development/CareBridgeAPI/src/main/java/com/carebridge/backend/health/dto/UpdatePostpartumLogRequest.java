package com.carebridge.backend.health.dto;

import com.carebridge.backend.health.entity.BleedingLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePostpartumLogRequest {

    @Min(0)
    @Max(10)
    private Short painLevel;

    private BleedingLevel bleedingLevel;

    @Min(0)
    @Max(10)
    private Short moodLevel;

    @DecimalMin("0.0")
    @DecimalMax("24.0")
    private BigDecimal sleepHours;

    @Size(max = 1000)
    private String breastfeedingNote;

    @Size(max = 2000)
    private String symptomNote;
}

package com.carebridge.backend.carejourney.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateGrowthMeasurementRequest {
    private LocalDate measuredDate;

    @DecimalMin("0")
    private BigDecimal weightKg;

    @DecimalMin("0")
    private BigDecimal heightCm;

    @DecimalMin("0")
    private BigDecimal headCircumferenceCm;

    @Size(max = 30)
    private String sourceType;

    private String note;
}

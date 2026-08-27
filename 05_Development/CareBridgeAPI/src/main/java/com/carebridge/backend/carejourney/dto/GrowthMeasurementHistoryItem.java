package com.carebridge.backend.carejourney.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class GrowthMeasurementHistoryItem {
    private UUID growthMeasurementId;
    private LocalDate measuredDate;
    private BigDecimal weightKg;
    private BigDecimal heightCm;
    private BigDecimal headCircumferenceCm;
    private String sourceType;
    private String note;
    private Instant createdAt;
}

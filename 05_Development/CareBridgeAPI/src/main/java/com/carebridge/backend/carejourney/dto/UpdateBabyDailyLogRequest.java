package com.carebridge.backend.carejourney.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class UpdateBabyDailyLogRequest {

    private Instant startedAt;

    private Instant endedAt;

    @DecimalMin(value = "0", message = "quantity must be non-negative")
    private BigDecimal quantity;

    @Size(max = 20, message = "unit must not exceed 20 characters")
    private String unit;

    @Size(max = 500, message = "note must not exceed 500 characters")
    private String note;
}

package com.carebridge.backend.carejourney.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class AddBabyDailyLogRequest {

    @Pattern(
        regexp = "FEEDING|SLEEP|DIAPER|FEVER|VOMITING|MEDICINE",
        message = "log_type must be one of: FEEDING, SLEEP, DIAPER, FEVER, VOMITING, MEDICINE"
    )
    private String logType;

    private Instant startedAt;

    private Instant endedAt;

    @DecimalMin(value = "0.00", message = "quantity must be non-negative")
    private BigDecimal quantity;

    @Size(max = 20, message = "unit must not exceed 20 characters")
    private String unit;

    @Size(max = 2000, message = "note must not exceed 2000 characters")
    private String note;
}

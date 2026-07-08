package com.carebridge.backend.carejourney.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BabyDailyLogResponse {

    private UUID babyLogId;
    private UUID babyId;
    private String logType;
    private Instant startedAt;
    private Instant endedAt;
    private BigDecimal quantity;
    private String unit;
    private String note;
    private UUID recordedBy;
    private Instant createdAt;
    private Instant updatedAt;
}

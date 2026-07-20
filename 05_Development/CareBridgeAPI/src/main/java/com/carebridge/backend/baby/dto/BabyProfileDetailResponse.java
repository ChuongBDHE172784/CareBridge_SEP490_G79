package com.carebridge.backend.baby.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class BabyProfileDetailResponse {

    private UUID id;
    private String nickname;
    private LocalDate birthDate;
    private String gender;
    private BigDecimal birthWeightKg;
    private BigDecimal birthLengthCm;
    private String status;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID relatedJourneyId;
}

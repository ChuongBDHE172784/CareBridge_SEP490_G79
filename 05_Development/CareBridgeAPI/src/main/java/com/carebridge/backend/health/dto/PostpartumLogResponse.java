package com.carebridge.backend.health.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class PostpartumLogResponse {

    private UUID postpartumLogId;
    private UUID journeyId;
    private UUID submissionId;
    private LocalDate logDate;
    private Short painLevel;
    private String bleedingLevel;
    private Short moodLevel;
    private BigDecimal sleepHours;
    private String breastfeedingNote;
    private String symptomNote;
    private Instant createdAt;
    private String aiInsight;
    private boolean redFlagAlert;
}

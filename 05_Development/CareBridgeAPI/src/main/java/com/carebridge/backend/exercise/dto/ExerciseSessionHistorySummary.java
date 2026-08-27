package com.carebridge.backend.exercise.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSessionHistorySummary {

    private UUID exerciseSessionId;
    private UUID exerciseId;
    private String exerciseTitle;
    private String sessionStatus;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private Long actualDurationSeconds;
    private BigDecimal completionPercent;
    private BigDecimal postureScore;
    private Integer warningCount;
}

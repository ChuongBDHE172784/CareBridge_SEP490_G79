package com.carebridge.backend.exercise.dto;

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
public class ExerciseSummaryResponse {

    private UUID exerciseId;
    private String title;
    private String description;
    private String trimesterScope;
    private String difficultyLevel;
    private Short durationMinutes;
    private String mediaUrl;
    private String safetyWarning;
    private Boolean supportsPostureAnalysis;
}

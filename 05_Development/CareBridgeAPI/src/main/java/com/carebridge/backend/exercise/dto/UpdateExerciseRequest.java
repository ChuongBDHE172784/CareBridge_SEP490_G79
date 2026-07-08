package com.carebridge.backend.exercise.dto;

import com.carebridge.backend.exercise.entity.DifficultyLevel;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateExerciseRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    @NotNull
    private TrimesterScope trimesterScope;

    @NotNull
    private DifficultyLevel difficultyLevel;

    @NotNull
    @Min(1)
    @Max(180)
    private Short durationMinutes;

    private String instructionContent;

    private String mediaUrl;

    // NULLABLE by design (ADR-EXERCISE-ADMIN-004):
    // null = unchanged, "" = rejected (EX-ADMIN-002), non-blank = applied.
    private String safetyWarning;

    @NotNull
    private Boolean supportsPostureAnalysis;
}

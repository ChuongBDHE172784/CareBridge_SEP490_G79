package com.carebridge.backend.exercise.mapper;

import com.carebridge.backend.exercise.dto.ExerciseDetailResponse;
import com.carebridge.backend.exercise.dto.ExerciseSummaryResponse;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import org.springframework.stereotype.Component;

@Component
public class ExerciseMapper {

    public ExerciseSummaryResponse toSummaryResponse(PregnancyExercise entity) {
        return ExerciseSummaryResponse.builder()
                .exerciseId(entity.getExerciseId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .trimesterScope(entity.getTrimesterScope() != null
                        ? entity.getTrimesterScope().name() : null)
                .difficultyLevel(entity.getDifficultyLevel() != null
                        ? entity.getDifficultyLevel().name() : null)
                .durationMinutes(entity.getDurationMinutes())
                .mediaUrl(entity.getMediaUrl())
                .safetyWarning(entity.getSafetyWarning() != null
                        ? entity.getSafetyWarning() : "")
                .supportsPostureAnalysis(entity.getSupportsPostureAnalysis())
                .build();
    }

    public ExerciseDetailResponse toDetailResponse(PregnancyExercise entity) {
        return ExerciseDetailResponse.builder()
                .exerciseId(entity.getExerciseId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .trimesterScope(entity.getTrimesterScope() != null
                        ? entity.getTrimesterScope().name() : null)
                .difficultyLevel(entity.getDifficultyLevel() != null
                        ? entity.getDifficultyLevel().name() : null)
                .durationMinutes(entity.getDurationMinutes())
                .instructionContent(entity.getInstructionContent())
                .mediaUrl(entity.getMediaUrl())
                .safetyWarning(entity.getSafetyWarning() != null
                        ? entity.getSafetyWarning() : "")
                .supportsPostureAnalysis(entity.getSupportsPostureAnalysis())
                .versionNo(entity.getVersionNo())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

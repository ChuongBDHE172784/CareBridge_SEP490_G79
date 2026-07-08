package com.carebridge.backend.exercise.mapper;

import com.carebridge.backend.exercise.dto.AdminExerciseResponse;
import com.carebridge.backend.exercise.dto.CreateExerciseRequest;
import com.carebridge.backend.exercise.dto.ExerciseDetailResponse;
import com.carebridge.backend.exercise.dto.ExerciseSummaryResponse;
import com.carebridge.backend.exercise.dto.UpdateExerciseRequest;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import java.time.OffsetDateTime;
import java.util.UUID;
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

    // === NEW (UC185 admin write side) ===

    public AdminExerciseResponse toAdminResponse(PregnancyExercise entity) {
        return AdminExerciseResponse.builder()
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
                .safetyWarning(entity.getSafetyWarning())
                .supportsPostureAnalysis(entity.getSupportsPostureAnalysis())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .versionNo(entity.getVersionNo())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /** Builds a new DRAFT PregnancyExercise from an admin create request. */
    public PregnancyExercise toEntity(CreateExerciseRequest request, UUID adminUserId) {
        OffsetDateTime now = OffsetDateTime.now();
        PregnancyExercise entity = new PregnancyExercise();
        entity.setExerciseId(UUID.randomUUID());
        entity.setCreatedBy(adminUserId);
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setTrimesterScope(request.getTrimesterScope());
        entity.setDifficultyLevel(request.getDifficultyLevel());
        entity.setDurationMinutes(request.getDurationMinutes());
        entity.setInstructionContent(request.getInstructionContent());
        entity.setMediaUrl(request.getMediaUrl());
        entity.setSafetyWarning(request.getSafetyWarning());
        entity.setSupportsPostureAnalysis(request.getSupportsPostureAnalysis());
        entity.setStatus(ExerciseStatus.DRAFT);
        entity.setVersionNo(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    /**
     * Applies an admin update in place. Does NOT change status/versionNo (caller bumps
     * versionNo). safetyWarning uses "null = unchanged" semantics — the blank-string
     * rejection (ADR-EXERCISE-ADMIN-004, EX-ADMIN-002) is validated by the caller
     * BEFORE this method is invoked; this method only decides whether to apply it.
     */
    public void applyUpdate(PregnancyExercise entity, UpdateExerciseRequest request) {
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setTrimesterScope(request.getTrimesterScope());
        entity.setDifficultyLevel(request.getDifficultyLevel());
        entity.setDurationMinutes(request.getDurationMinutes());
        entity.setInstructionContent(request.getInstructionContent());
        entity.setMediaUrl(request.getMediaUrl());
        if (request.getSafetyWarning() != null) {
            entity.setSafetyWarning(request.getSafetyWarning());
        }
        entity.setSupportsPostureAnalysis(request.getSupportsPostureAnalysis());
        entity.setUpdatedAt(OffsetDateTime.now());
    }
}

package com.carebridge.backend.exercise.service;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.ExerciseDetailResponse;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.exception.ExerciseNotFoundException;
import com.carebridge.backend.exercise.mapper.ExerciseMapper;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseDetailQueryServiceImpl implements IExerciseDetailQueryService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseMapper exerciseMapper;

    @Override
    public ApiResponse<ExerciseDetailResponse> getExerciseDetail(UUID exerciseId) {
        // C1: status filter MUST be in repository query — never fetch-all and filter in Java
        PregnancyExercise exercise = exerciseRepository
                .findByExerciseIdAndStatus(exerciseId, ExerciseStatus.PUBLISHED)
                .orElseThrow(ExerciseNotFoundException::notFound);
        // C2: DRAFT/ARCHIVED → 404 with EX-001 (ADR-VPED-001) — handled by orElseThrow above

        // C3: safetyWarning null → "" handled in mapper.toDetailResponse()
        // C4: instructionContent included in ExerciseDetailResponse (ADR-VPED-002)
        ExerciseDetailResponse response = exerciseMapper.toDetailResponse(exercise);

        return ApiResponse.success(response);
    }
}

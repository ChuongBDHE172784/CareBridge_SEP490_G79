package com.carebridge.backend.exercise.service;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.exercise.dto.ExerciseSummaryResponse;
import com.carebridge.backend.exercise.entity.DifficultyLevel;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import com.carebridge.backend.exercise.mapper.ExerciseMapper;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseQueryServiceImpl implements IExerciseQueryService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseMapper exerciseMapper;

    @Override
    public PaginatedResponse<ExerciseSummaryResponse> listPublishedExercises(
            TrimesterScope trimester, DifficultyLevel difficulty, int page, int size) {
        int clampedSize = Math.min(Math.max(size, 1), 50);

        return PaginatedResponse.of(
                exerciseRepository.findPublishedByFilters(
                                ExerciseStatus.PUBLISHED, trimester, difficulty,
                                PageRequest.of(page, clampedSize))
                        .map(exerciseMapper::toSummaryResponse));
    }
}

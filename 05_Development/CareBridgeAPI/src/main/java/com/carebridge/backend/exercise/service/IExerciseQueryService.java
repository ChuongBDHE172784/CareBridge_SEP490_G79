package com.carebridge.backend.exercise.service;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.exercise.dto.ExerciseSummaryResponse;
import com.carebridge.backend.exercise.entity.DifficultyLevel;
import com.carebridge.backend.exercise.entity.TrimesterScope;

public interface IExerciseQueryService {

    PaginatedResponse<ExerciseSummaryResponse> listPublishedExercises(
            TrimesterScope trimester, DifficultyLevel difficulty, int page, int size);
}

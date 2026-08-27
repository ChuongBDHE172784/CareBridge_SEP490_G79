package com.carebridge.backend.exercise.service;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.ExerciseDetailResponse;
import java.util.UUID;

public interface IExerciseDetailQueryService {

    ApiResponse<ExerciseDetailResponse> getExerciseDetail(UUID exerciseId);
}

package com.carebridge.backend.exercise.service;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.SafetyCheckResponse;
import com.carebridge.backend.exercise.dto.SubmitSafetyCheckRequest;
import java.util.UUID;

public interface IExerciseSafetyCheckService {

    ApiResponse<SafetyCheckResponse> submitSafetyCheck(
            UUID exerciseId, SubmitSafetyCheckRequest request, UUID userId);

    ApiResponse<SafetyCheckResponse> getLatestSafetyCheck(UUID exerciseId, UUID userId);
}

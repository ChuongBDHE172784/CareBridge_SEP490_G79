package com.carebridge.backend.exercise.service;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.SessionResultResponse;
import java.util.UUID;

public interface IExerciseSessionResultService {

    ApiResponse<SessionResultResponse> getSessionResult(UUID sessionId, UUID userId);
}

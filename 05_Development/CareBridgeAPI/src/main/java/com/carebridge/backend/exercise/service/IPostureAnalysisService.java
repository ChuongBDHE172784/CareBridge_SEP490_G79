package com.carebridge.backend.exercise.service;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.PostureEventRequest;
import com.carebridge.backend.exercise.dto.PostureFeedbackResponse;
import java.util.UUID;

public interface IPostureAnalysisService {

    /**
     * Analyzes posture from keypoint data and returns feedback. Uses the exercise's
     * active posture_analysis_config to determine analysis rules; falls back to a
     * RULE_BASED default (no error) when no active config exists (Logic Issue L2).
     *
     * @throws com.carebridge.backend.common.exception.SessionNotFoundException when sessionId does not exist
     * @throws com.carebridge.backend.exercise.exception.SessionOwnershipException when the session does not belong to userId
     * @throws com.carebridge.backend.exercise.exception.InvalidSessionStateException (EXSESS-009) when session is not IN_PROGRESS
     */
    ApiResponse<PostureFeedbackResponse> analyzePosture(
            UUID sessionId, UUID userId, PostureEventRequest request);
}

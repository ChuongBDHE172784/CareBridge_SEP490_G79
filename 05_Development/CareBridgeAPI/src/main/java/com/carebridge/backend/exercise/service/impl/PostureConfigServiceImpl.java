package com.carebridge.backend.exercise.service.impl;

import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.PostureConfigResponse;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PostureAnalysisConfig;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.exception.ExerciseNotFoundException;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.repository.PostureAnalysisConfigRepository;
import com.carebridge.backend.exercise.service.IPostureConfigService;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostureConfigServiceImpl implements IPostureConfigService {

    private final ExerciseRepository exerciseRepository;
    private final PostureAnalysisConfigRepository postureConfigRepository;

    @Override
    public ApiResponse<PostureConfigResponse> getActiveConfig(UUID exerciseId) {
        PregnancyExercise exercise = exerciseRepository
                .findByExerciseIdAndStatus(exerciseId, ExerciseStatus.PUBLISHED)
                .orElseThrow(ExerciseNotFoundException::notFound);

        if (!Boolean.TRUE.equals(exercise.getSupportsPostureAnalysis())) {
            throw ExerciseNotFoundException.notFound();
        }

        PostureAnalysisConfig config = postureConfigRepository
                .findActiveConfigByExerciseId(exerciseId, OffsetDateTime.now())
                .orElseThrow(() ->
                        new ResourceNotFoundException("No active posture analysis configuration found"));

        PostureConfigResponse response = PostureConfigResponse.builder()
                .postureConfigId(config.getPostureConfigId())
                .exerciseId(config.getExerciseId())
                .analysisMode(config.getAnalysisMode())
                .ruleOrModelVersion(config.getRuleOrModelVersion())
                .confidenceThreshold(config.getConfidenceThreshold())
                .feedbackLevel(config.getFeedbackLevel())
                .effectiveFrom(config.getEffectiveFrom())
                .build();

        return ApiResponse.success(response);
    }
}

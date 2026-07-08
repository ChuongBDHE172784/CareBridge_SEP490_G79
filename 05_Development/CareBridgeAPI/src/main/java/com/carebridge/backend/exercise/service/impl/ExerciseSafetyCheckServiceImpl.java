package com.carebridge.backend.exercise.service.impl;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.SafetyCheckResponse;
import com.carebridge.backend.exercise.dto.SubmitSafetyCheckRequest;
import com.carebridge.backend.exercise.entity.ExerciseSafetyCheck;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.SafetyCheckStatus;
import com.carebridge.backend.exercise.entity.SafetyQuestion;
import com.carebridge.backend.exercise.exception.ExerciseNotFoundException;
import com.carebridge.backend.exercise.exception.SafetyCheckNotFoundException;
import com.carebridge.backend.exercise.mapper.SafetyCheckMapper;
import com.carebridge.backend.exercise.policy.EvaluationResult;
import com.carebridge.backend.exercise.policy.SafetyCheckPolicy;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.repository.ExerciseSafetyCheckRepository;
import com.carebridge.backend.exercise.service.IExerciseSafetyCheckService;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExerciseSafetyCheckServiceImpl implements IExerciseSafetyCheckService {

    private final ExerciseSafetyCheckRepository safetyCheckRepository;
    private final ExerciseRepository exerciseRepository;
    private final SafetyCheckMapper safetyCheckMapper;
    private final SafetyCheckPolicy safetyCheckPolicy;

    @Override
    @Transactional
    public ApiResponse<SafetyCheckResponse> submitSafetyCheck(
            UUID exerciseId, SubmitSafetyCheckRequest request, UUID userId) {
        // 1. Exercise must exist and be PUBLISHED (status filter in repository query).
        exerciseRepository
                .findByExerciseIdAndStatus(exerciseId, ExerciseStatus.PUBLISHED)
                .orElseThrow(ExerciseNotFoundException::notFound);

        // 2. Evaluate answers via the policy (the only red-flag evaluator).
        // PDPA: never log the answer values.
        Map<SafetyQuestion, Boolean> answers = safetyCheckMapper.toQuestionMap(request);
        EvaluationResult evaluation = safetyCheckPolicy.evaluate(answers);

        // 3. Build the entity and resolve outcome fields.
        ExerciseSafetyCheck check =
                safetyCheckMapper.buildEntity(exerciseId, userId, request, evaluation);

        if (evaluation.isCleared()) {
            check.setRedFlagDetected(false);
            check.setResultStatus(SafetyCheckStatus.CLEARED);
            check.setCompletedAt(OffsetDateTime.now());
            check.setBlockedReason(null);
        } else {
            check.setRedFlagDetected(true);
            check.setResultStatus(SafetyCheckStatus.BLOCKED);
            check.setCompletedAt(null);
            check.setBlockedReason(
                    safetyCheckPolicy.buildBlockedReason(evaluation.getFlaggedQuestions()));
        }

        // 4. Persist and return.
        ExerciseSafetyCheck saved = safetyCheckRepository.save(check);
        return ApiResponse.success(safetyCheckMapper.toResponse(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<SafetyCheckResponse> getLatestSafetyCheck(UUID exerciseId, UUID userId) {
        ExerciseSafetyCheck check = safetyCheckRepository
                .findTopByExerciseIdAndUserIdOrderByCreatedAtDesc(exerciseId, userId)
                .orElseThrow(SafetyCheckNotFoundException::new);
        return ApiResponse.success(safetyCheckMapper.toResponse(check));
    }
}

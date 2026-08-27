package com.carebridge.backend.exercise.mapper;

import com.carebridge.backend.exercise.dto.SafetyCheckResponse;
import com.carebridge.backend.exercise.dto.SubmitSafetyCheckRequest;
import com.carebridge.backend.exercise.entity.ExerciseSafetyCheck;
import com.carebridge.backend.exercise.entity.SafetyCheckStatus;
import com.carebridge.backend.exercise.entity.SafetyQuestion;
import com.carebridge.backend.exercise.policy.EvaluationResult;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SafetyCheckMapper {

    public SafetyCheckResponse toResponse(ExerciseSafetyCheck entity) {
        return SafetyCheckResponse.builder()
                .safetyCheckId(entity.getSafetyCheckId())
                .exerciseId(entity.getExerciseId())
                .resultStatus(entity.getResultStatus() != null ? entity.getResultStatus().name() : null)
                .redFlagDetected(entity.getRedFlagDetected())
                .blockedReason(entity.getBlockedReason())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Builds a new {@link ExerciseSafetyCheck} entity from the submitted request and the
     * policy evaluation result. The caller is responsible for setting the resolved fields
     * ({@code resultStatus}, {@code redFlagDetected}, {@code blockedReason}, {@code completedAt}).
     */
    public ExerciseSafetyCheck buildEntity(
            UUID exerciseId,
            UUID userId,
            SubmitSafetyCheckRequest request,
            EvaluationResult evaluationResult) {
        OffsetDateTime now = OffsetDateTime.now();
        return ExerciseSafetyCheck.builder()
                .exerciseId(exerciseId)
                .userId(userId)
                .journeyId(request.getJourneyId())
                .answerJson(toAnswerMap(request))
                .createdAt(now)
                .build();
    }

    public Map<SafetyQuestion, Boolean> toQuestionMap(SubmitSafetyCheckRequest request) {
        Map<SafetyQuestion, Boolean> answers = new LinkedHashMap<>();
        answers.put(SafetyQuestion.Q1_NO_DIZZINESS, request.getQ1NoDizziness());
        answers.put(SafetyQuestion.Q2_NO_CONTRACTIONS, request.getQ2NoContractions());
        answers.put(SafetyQuestion.Q3_NO_BLEEDING, request.getQ3NoBleeding());
        answers.put(SafetyQuestion.Q4_HYDRATED_AND_FED, request.getQ4HydratedAndFed());
        return answers;
    }

    private Map<String, Boolean> toAnswerMap(SubmitSafetyCheckRequest request) {
        Map<String, Boolean> answers = new LinkedHashMap<>();
        answers.put(SafetyQuestion.Q1_NO_DIZZINESS.name(), request.getQ1NoDizziness());
        answers.put(SafetyQuestion.Q2_NO_CONTRACTIONS.name(), request.getQ2NoContractions());
        answers.put(SafetyQuestion.Q3_NO_BLEEDING.name(), request.getQ3NoBleeding());
        answers.put(SafetyQuestion.Q4_HYDRATED_AND_FED.name(), request.getQ4HydratedAndFed());
        return answers;
    }
}

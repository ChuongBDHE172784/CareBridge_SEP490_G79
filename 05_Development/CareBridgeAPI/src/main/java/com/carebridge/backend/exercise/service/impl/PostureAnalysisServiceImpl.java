package com.carebridge.backend.exercise.service.impl;

import com.carebridge.backend.common.exception.SessionNotFoundException;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.PostureEventRequest;
import com.carebridge.backend.exercise.dto.PostureFeedbackResponse;
import com.carebridge.backend.exercise.entity.AnalysisMode;
import com.carebridge.backend.exercise.entity.ExerciseSession;
import com.carebridge.backend.exercise.entity.PostureAnalysisConfig;
import com.carebridge.backend.exercise.entity.SessionStatus;
import com.carebridge.backend.exercise.exception.InvalidSessionStateException;
import com.carebridge.backend.exercise.exception.SessionOwnershipException;
import com.carebridge.backend.exercise.inference.PostureInferenceConfigResolver;
import com.carebridge.backend.exercise.inference.PostureInferenceConfigResolver.ResolvedInferenceConfig;
import com.carebridge.backend.exercise.inference.PostureInferencePort;
import com.carebridge.backend.exercise.inference.PostureInferencePort.InferenceFeedback;
import com.carebridge.backend.exercise.inference.PostureInferencePort.InferenceRequest;
import com.carebridge.backend.exercise.inference.PostureInferencePort.InferenceResult;
import com.carebridge.backend.exercise.inference.PostureInferenceUnavailableException;
import com.carebridge.backend.exercise.repository.ExerciseSessionRepository;
import com.carebridge.backend.exercise.repository.PostureAnalysisConfigRepository;
import com.carebridge.backend.exercise.repository.PostureFeedbackEventRepository;
import com.carebridge.backend.exercise.service.ExerciseCareContextResolver;
import com.carebridge.backend.exercise.service.IPostureAnalysisService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostureAnalysisServiceImpl implements IPostureAnalysisService {

    private final ExerciseSessionRepository sessionRepository;
    private final PostureAnalysisConfigRepository postureConfigRepository;
    private final PostureFeedbackEventRepository postureFeedbackEventRepository;
    private final PostureInferencePort postureInferencePort;
    private final PostureInferenceConfigResolver inferenceConfigResolver;
    private final ExerciseCareContextResolver careContextResolver;

    @Override
    @Transactional
    public ApiResponse<PostureFeedbackResponse> analyzePosture(
            UUID sessionId, UUID userId, PostureEventRequest request) {
        ExerciseSession session = sessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Exercise session not found"));

        if (!userId.equals(session.getUserId())) {
            throw new SessionOwnershipException();
        }
        if (session.getSessionStatus() != SessionStatus.IN_PROGRESS) {
            throw InvalidSessionStateException.notInProgress();
        }

        ExerciseCareContextResolver.CareContext careContext = careContextResolver.resolve(
                session.getUserId(), session.getJourneyId());

        // Logic Issue L2 — a missing active config is NOT an error; fall back to a
        // RULE_BASED default and log a warning, rather than throwing PAC-004/EX-014.
        Optional<PostureAnalysisConfig> configOpt = postureConfigRepository
                .findActiveConfigByExerciseId(session.getExerciseId(), OffsetDateTime.now());
        if (configOpt.isEmpty()) {
            log.warn(
                    "No active posture template config for exercise {} — falling back to RULE_BASED default",
                    session.getExerciseId());
        }
        PostureAnalysisConfig config = configOpt.orElse(null);
        String feedbackLevel = config != null && config.getFeedbackLevel() != null
                ? config.getFeedbackLevel() : "DETAILED";

        Analysis analysis = dispatchAnalysis(config, request, feedbackLevel);

        com.carebridge.backend.exercise.entity.PostureFeedbackEvent event =
                com.carebridge.backend.exercise.entity.PostureFeedbackEvent.builder()
                        .exerciseSessionId(sessionId)
                        .journeyId(careContext.careSubjectId())
                        .postureConfigId(config != null ? config.getPostureConfigId() : null)
                        .eventTimeMs(request.getEventTimeMs())
                        .postureCode(analysis.postureCode())
                        .confidenceScore(analysis.confidenceScore())
                        .severity(analysis.severity())
                        .feedbackText(analysis.feedbackText())
                        .canonicalPayload(analysis.metadata())
                        .build();
        postureFeedbackEventRepository.save(event);

        if ("CRITICAL".equals(analysis.severity())) {
            int warningCount = session.getWarningCount() != null ? session.getWarningCount() : 0;
            session.setWarningCount(
                    warningCount == Integer.MAX_VALUE ? Integer.MAX_VALUE : warningCount + 1);
            sessionRepository.save(session);
        }

        PostureFeedbackResponse response = PostureFeedbackResponse.builder()
                .postureCode(analysis.postureCode())
                .confidenceScore(analysis.confidenceScore())
                .severity(analysis.severity())
                .feedbackText(analysis.feedbackText())
                .build();

        return ApiResponse.success(response);
    }

    private Analysis dispatchAnalysis(
            PostureAnalysisConfig config, PostureEventRequest request, String feedbackLevel) {
        AnalysisMode mode = resolveMode(config);
        if (mode == AnalysisMode.RULE_BASED) {
            return analyzeKeypoints(request.getKeypointSummaryJson(), feedbackLevel);
        }

        boolean allowRuleFallback = mode == AnalysisMode.HYBRID;
        try {
            ResolvedInferenceConfig inferenceConfig = inferenceConfigResolver.resolve(config);
            long sequenceNumber = request.getEventTimeMs() == null
                    ? 0L : request.getEventTimeMs();
            Map<String, Object> landmarks = request.getKeypointSummaryJson() == null
                    ? Map.of() : new LinkedHashMap<>(request.getKeypointSummaryJson());

            InferenceResult result = postureInferencePort.infer(new InferenceRequest(
                    inferenceConfig.modelVersion(),
                    inferenceConfig.exerciseKey(),
                    sequenceNumber,
                    landmarks));

            if (belowThreshold(result.confidence(), config.getConfidenceThreshold())
                    || providerReportedLowConfidence(result)) {
                return allowRuleFallback
                        ? degradedRuleFallback(
                                "MODEL_LOW_CONFIDENCE",
                                request.getKeypointSummaryJson(),
                                feedbackLevel)
                        : lowConfidenceAnalysis(result, feedbackLevel);
            }
            return modelAnalysis(result, feedbackLevel);
        } catch (PostureInferenceUnavailableException exception) {
            log.warn("Posture inference unavailable: {}", exception.getReasonCode());
            return allowRuleFallback
                    ? degradedRuleFallback(
                            "MODEL_UNAVAILABLE",
                            request.getKeypointSummaryJson(),
                            feedbackLevel)
                    : unavailableAnalysis(exception.getReasonCode(), feedbackLevel);
        }
    }

    private AnalysisMode resolveMode(PostureAnalysisConfig config) {
        if (config == null || config.getAnalysisMode() == null) {
            return AnalysisMode.RULE_BASED;
        }
        try {
            return AnalysisMode.valueOf(config.getAnalysisMode());
        } catch (IllegalArgumentException exception) {
            log.warn("Unknown posture analysis mode; falling back to RULE_BASED");
            return AnalysisMode.RULE_BASED;
        }
    }

    private boolean belowThreshold(BigDecimal confidence, BigDecimal threshold) {
        return threshold != null && confidence.compareTo(threshold) < 0;
    }

    private boolean providerReportedLowConfidence(InferenceResult result) {
        return result.feedback() != null
                && result.feedback().stream()
                        .anyMatch(item -> "low_confidence".equals(item.code()));
    }

    private Analysis modelAnalysis(InferenceResult result, String feedbackLevel) {
        String severity = highestSeverity(result.feedback(), result.correct());
        String detailedText = result.feedback().stream()
                .map(InferenceFeedback::message)
                .filter(message -> message != null && !message.isBlank())
                .distinct()
                .reduce((left, right) -> left + " " + right)
                .orElse(result.predictedClass());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("analysisStatus", "MODEL_BASED");
        metadata.put("provider", "EXERCISE_CORRECTION");
        metadata.put("modelVersion", result.modelVersion());
        metadata.put("exerciseKey", result.exerciseKey());
        metadata.put("sequenceNumber", result.sequenceNumber());
        metadata.put("score", result.score());
        metadata.put("correct", result.correct());

        return new Analysis(
                result.predictedClass(),
                result.confidence(),
                severity,
                applyFeedbackLevel(feedbackLevel, result.predictedClass(), detailedText),
                metadata);
    }

    private String highestSeverity(List<InferenceFeedback> feedback, boolean correct) {
        if (feedback.stream().anyMatch(item -> "CRITICAL".equals(item.severity()))) {
            return "CRITICAL";
        }
        if (feedback.stream().anyMatch(item -> "WARNING".equals(item.severity()))) {
            return "WARNING";
        }
        return correct ? "INFO" : "WARNING";
    }

    private Analysis lowConfidenceAnalysis(InferenceResult result, String feedbackLevel) {
        String postureCode = "MODEL_LOW_CONFIDENCE";
        return new Analysis(
                postureCode,
                result.confidence(),
                "WARNING",
                applyFeedbackLevel(
                        feedbackLevel,
                        postureCode,
                        "The posture model could not assess this frame confidently."),
                Map.of(
                        "analysisStatus", "MODEL_LOW_CONFIDENCE",
                        "provider", "EXERCISE_CORRECTION",
                        "modelVersion", result.modelVersion(),
                        "exerciseKey", result.exerciseKey(),
                        "sequenceNumber", result.sequenceNumber()));
    }

    private Analysis unavailableAnalysis(String reasonCode, String feedbackLevel) {
        String postureCode = "MODEL_UNAVAILABLE";
        return new Analysis(
                postureCode,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                "WARNING",
                applyFeedbackLevel(
                        feedbackLevel,
                        postureCode,
                        "Posture analysis is temporarily unavailable. Please try again."),
                Map.of(
                        "analysisStatus", "MODEL_UNAVAILABLE",
                        "provider", "EXERCISE_CORRECTION",
                        "failureCode", reasonCode));
    }

    private Analysis degradedRuleFallback(
            String reasonCode, Map<String, Object> keypoints, String feedbackLevel) {
        Analysis fallback = analyzeKeypoints(keypoints, "DETAILED");
        String postureCode = reasonCode + "_RULE_FALLBACK_" + fallback.postureCode();
        BigDecimal confidence = fallback.confidenceScore().min(new BigDecimal("0.70"));
        String severity = "CRITICAL".equals(fallback.severity()) ? "CRITICAL" : "WARNING";
        String detailedText = "Model result unavailable; rule-based fallback: "
                + (fallback.feedbackText() == null ? fallback.postureCode() : fallback.feedbackText());

        return new Analysis(
                postureCode,
                confidence,
                severity,
                applyFeedbackLevel(feedbackLevel, postureCode, detailedText),
                Map.of(
                        "analysisStatus", "DEGRADED",
                        "provider", "RULE_BASED_FALLBACK",
                        "failureCode", reasonCode,
                        "fallbackPostureCode", fallback.postureCode()));
    }

    private String applyFeedbackLevel(
            String feedbackLevel, String postureCode, String detailedText) {
        return switch (feedbackLevel) {
            case "SILENT" -> null;
            case "BASIC" -> postureCode;
            default -> detailedText;
        };
    }

    /**
     * RULE_BASED posture heuristic keyed off a "backAngle" (degrees) keypoint field.
     * This is a deliberately simple, deterministic rule set (no ML dependency) —
     * sufficient to satisfy BR-POSTURE-002/003 (config-driven severity + CRITICAL
     * gating) without diagnosing or delaying emergency routing (AI provides
     * guidance only, per CLAUDE.md).
     */
    private Analysis analyzeKeypoints(java.util.Map<String, Object> keypoints, String feedbackLevel) {
        Object backAngleRaw = keypoints != null ? keypoints.get("backAngle") : null;
        Double backAngle = toDouble(backAngleRaw);

        String postureCode;
        BigDecimal confidenceScore;
        String severity;
        String fullText;

        if (backAngle == null) {
            postureCode = "UNKNOWN";
            confidenceScore = new BigDecimal("0.50");
            severity = "INFO";
            fullText = "Insufficient landmark data to assess posture.";
        } else if (backAngle <= 15.0) {
            postureCode = "GOOD_FORM";
            confidenceScore = new BigDecimal("0.95");
            severity = "INFO";
            fullText = "Great form! Keep your back straight.";
        } else if (backAngle <= 30.0) {
            postureCode = "MILD_ROUNDING";
            confidenceScore = new BigDecimal("0.70");
            severity = "WARNING";
            fullText = "Try to straighten your back slightly.";
        } else {
            postureCode = "ROUND_BACK";
            confidenceScore = new BigDecimal("0.40");
            severity = "CRITICAL";
            fullText = "Stop and correct your posture — your back is rounding significantly.";
        }

        String feedbackText = applyFeedbackLevel(feedbackLevel, postureCode, fullText);

        return new Analysis(
                postureCode,
                confidenceScore,
                severity,
                feedbackText,
                Map.of("analysisStatus", "RULE_BASED"));
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            double parsed = number.doubleValue();
            return Double.isFinite(parsed) ? parsed : null;
        }
        if (value instanceof String str) {
            try {
                double parsed = Double.parseDouble(str);
                return Double.isFinite(parsed) ? parsed : null;
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private record Analysis(
            String postureCode,
            BigDecimal confidenceScore,
            String severity,
            String feedbackText,
            Map<String, Object> metadata) {
    }
}

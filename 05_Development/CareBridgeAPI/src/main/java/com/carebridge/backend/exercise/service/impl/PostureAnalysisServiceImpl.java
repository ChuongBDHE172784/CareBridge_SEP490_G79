package com.carebridge.backend.exercise.service.impl;

import com.carebridge.backend.common.exception.SessionNotFoundException;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.PostureEventRequest;
import com.carebridge.backend.exercise.dto.PostureFeedbackResponse;
import com.carebridge.backend.exercise.entity.ExerciseSession;
import com.carebridge.backend.exercise.entity.PostureAnalysisConfig;
import com.carebridge.backend.exercise.entity.SessionStatus;
import com.carebridge.backend.exercise.exception.InvalidSessionStateException;
import com.carebridge.backend.exercise.exception.SessionOwnershipException;
import com.carebridge.backend.exercise.repository.ExerciseSessionRepository;
import com.carebridge.backend.exercise.repository.PostureAnalysisConfigRepository;
import com.carebridge.backend.exercise.repository.PostureFeedbackEventRepository;
import com.carebridge.backend.exercise.service.IPostureAnalysisService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
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

        Analysis analysis = analyzeKeypoints(request.getKeypointSummaryJson(), feedbackLevel);

        com.carebridge.backend.exercise.entity.PostureFeedbackEvent event =
                com.carebridge.backend.exercise.entity.PostureFeedbackEvent.builder()
                        .feedbackEventId(UUID.randomUUID())
                        .exerciseSessionId(sessionId)
                        .postureConfigId(config != null ? config.getPostureConfigId() : null)
                        .eventTimeMs(request.getEventTimeMs())
                        .postureCode(analysis.postureCode())
                        .confidenceScore(analysis.confidenceScore())
                        .severity(analysis.severity())
                        .feedbackText(analysis.feedbackText())
                        .build();
        postureFeedbackEventRepository.save(event);

        if ("CRITICAL".equals(analysis.severity())) {
            session.setWarningCount(
                    (session.getWarningCount() != null ? session.getWarningCount() : 0) + 1);
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

        String feedbackText = switch (feedbackLevel) {
            case "SILENT" -> null;
            case "BASIC" -> postureCode;
            default -> fullText;
        };

        return new Analysis(postureCode, confidenceScore, severity, feedbackText);
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private record Analysis(
            String postureCode, BigDecimal confidenceScore, String severity, String feedbackText) {
    }
}

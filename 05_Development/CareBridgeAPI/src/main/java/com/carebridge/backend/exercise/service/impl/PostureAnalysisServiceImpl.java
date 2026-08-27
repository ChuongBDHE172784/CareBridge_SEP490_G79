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
import com.carebridge.backend.exercise.policy.GeometricPostureRules;
import com.carebridge.backend.exercise.policy.GeometricPostureRules.RuleFinding;
import com.carebridge.backend.exercise.policy.PostureSessionTracker;
import com.carebridge.backend.exercise.policy.PostureSessionTracker.TrackedFrame;
import com.carebridge.backend.exercise.repository.ExerciseSessionRepository;
import com.carebridge.backend.exercise.repository.PostureAnalysisConfigRepository;
import com.carebridge.backend.exercise.repository.PostureFeedbackEventRepository;
import com.carebridge.backend.exercise.service.ExerciseCareContextResolver;
import com.carebridge.backend.exercise.service.IPostureAnalysisService;
import com.carebridge.backend.exercise.service.PostureFeedbackMessages;
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

    /** A landmark below this MediaPipe visibility is treated as absent. */
    private static final double MIN_LANDMARK_VISIBILITY = 0.5;

    /** Shoulder and hip midpoints closer than this carry no usable orientation. */
    private static final double MIN_TRUNK_SPAN = 0.02;

    private final ExerciseSessionRepository sessionRepository;
    private final PostureAnalysisConfigRepository postureConfigRepository;
    private final PostureFeedbackEventRepository postureFeedbackEventRepository;
    private final PostureInferencePort postureInferencePort;
    private final PostureInferenceConfigResolver inferenceConfigResolver;
    private final ExerciseCareContextResolver careContextResolver;
    private final PostureSessionTracker sessionTracker;

    @Override
    @Transactional
    public ApiResponse<PostureFeedbackResponse> analyzePosture(
            UUID sessionId, UUID userId, PostureEventRequest request) {
        // =========================================================================
        // [BƯỚC 1 & 2: Validate nghiệp vụ & Tiếp nhận Request]
        // =========================================================================

        // [2.1] Truy vấn phiên tập luyện (ExerciseSession) theo ID từ Database
        ExerciseSession session = sessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Exercise session not found"));

        // [2.2] Kiểm tra quyền sở hữu: Người gửi request phải chính là người tạo phiên tập
        if (!userId.equals(session.getUserId())) {
            throw new SessionOwnershipException();
        }

        // [2.3] Kiểm tra trạng thái phiên tập: Bắt buộc phải đang ở trạng thái IN_PROGRESS mới phân tích tư thế
        if (session.getSessionStatus() != SessionStatus.IN_PROGRESS) {
            throw InvalidSessionStateException.notInProgress();
        }

        // [2.4] Phân giải ngữ cảnh chăm sóc (CareContext / Care Journey) của mẹ bầu
        ExerciseCareContextResolver.CareContext careContext = careContextResolver.resolve(
                session.getUserId(), session.getJourneyId());

        // [2.5] Lấy cấu hình phân tích tư thế (PostureAnalysisConfig) đang ACTIVE của bài tập
        // Nếu không có cấu hình riêng, hệ thống tự động fallback về RULE_BASED mặc định mà không làm gián đoạn bài tập
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

        // =========================================================================
        // [BƯỚC 3: Xử lý nghiệp vụ & Thuật toán AI phân tích tư thế]
        // =========================================================================

        // [3.1] Điều phối phân tích khung hình (Frame-level Analysis):
        // Chạy qua AI Model (Sidecar MediaPipe/ML) hoặc Thuật toán hình học (Geometric Rule-based / Fallback)
        Analysis analysis = dispatchAnalysis(config, request, feedbackLevel);

        // [3.2] Áp dụng ngữ cảnh lịch sử phiên tập (Session History Tracking):
        // Kiểm tra chuyển động toàn chu kỳ (ví dụ: độ gập tối đa trong bicep curl, chu kỳ squat/lunge)
        analysis = applySessionHistory(sessionId, config, request, analysis, feedbackLevel);

        // [3.3] Lọc chống spam bản ghi (Dedup Gate / Occurrence Gate):
        // Chỉ lưu Database khi phát hiện lỗi mới xuất hiện lần đầu trong chuỗi frame (tránh ghi hàng chục frame trùng lặp mỗi giây)
        if (!sessionTracker.isNewOccurrence(sessionId, analysis.postureCode())) {
            return ApiResponse.success(PostureFeedbackResponse.builder()
                    .postureCode(analysis.postureCode())
                    .confidenceScore(analysis.confidenceScore())
                    .severity(analysis.severity())
                    .feedbackText(analysis.feedbackText())
                    .build());
        }

        // =========================================================================
        // [BƯỚC 4: Lưu trữ & Thay đổi trạng thái Database]
        // =========================================================================

        // [4.1] Khởi tạo và lưu sự kiện phản hồi tư thế vào bảng posture_feedback_events
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

        // [4.2] Nếu phát hiện lỗi tư thế nghiêm trọng (CRITICAL), tăng bộ đếm cảnh báo warningCount trong ExerciseSession
        if ("CRITICAL".equals(analysis.severity())) {
            int warningCount = session.getWarningCount() != null ? session.getWarningCount() : 0;
            session.setWarningCount(
                    warningCount == Integer.MAX_VALUE ? Integer.MAX_VALUE : warningCount + 1);
            sessionRepository.save(session);
        }

        // =========================================================================
        // [BƯỚC 5: Đóng gói phản hồi trả về Frontend]
        // =========================================================================

        // [5.1] Đóng gói DTO phản hồi chứa mã tư thế, điểm tin cậy, mức độ nghiêm trọng và câu hướng dẫn bằng giọng nói/UI
        PostureFeedbackResponse response = PostureFeedbackResponse.builder()
                .postureCode(analysis.postureCode())
                .confidenceScore(analysis.confidenceScore())
                .severity(analysis.severity())
                .feedbackText(analysis.feedbackText())
                .build();

        return ApiResponse.success(response);
    }

    /**
     * Phân phối luồng phân tích khung hình tư thế dựa trên chế độ cấu hình (AnalysisMode).
     *
     * @param config        Cấu hình phân tích tư thế bài tập (PostureAnalysisConfig)
     * @param request       Dữ liệu khung hình gửi lên từ client chứa các điểm mốc cơ thể (Keypoint landmarks)
     * @param feedbackLevel Mức độ chi tiết phản hồi (DETAILED, BASIC, SILENT)
     * @return Kết quả phân tích (Analysis) gồm mã tư thế, điểm tin cậy, độ nghiêm trọng, text hướng dẫn
     */
    private Analysis dispatchAnalysis(
            PostureAnalysisConfig config, PostureEventRequest request, String feedbackLevel) {
        AnalysisMode mode = resolveMode(config);
        // Nếu cấu hình là thuần RULE_BASED (hình học dựa trên luật)
        if (mode == AnalysisMode.RULE_BASED) {
            return analyzeKeypoints(request.getKeypointSummaryJson(), feedbackLevel);
        }

        // Chế độ HYBRID: Kết hợp cả 2 nguồn: Model ML phân loại pha/lớp tư thế + Luật hình học kiểm tra lỗi tư thế
        // Chế độ MODEL_BASED: Chỉ chạy qua Model AI sidecar
        boolean allowRuleFallback = mode == AnalysisMode.HYBRID;
        String exerciseKey = null;
        try {
            // Phân giải cấu hình inference (tên model, key bài tập)
            ResolvedInferenceConfig inferenceConfig = inferenceConfigResolver.resolve(config);
            exerciseKey = inferenceConfig.exerciseKey();
            long sequenceNumber = request.getEventTimeMs() == null
                    ? 0L : request.getEventTimeMs();
            Map<String, Object> landmarks = request.getKeypointSummaryJson() == null
                    ? Map.of() : new LinkedHashMap<>(request.getKeypointSummaryJson());

            // Gọi sang Python Sidecar (MediaPipe Posture Correction Service) qua HTTP Port
            InferenceResult result = postureInferencePort.infer(new InferenceRequest(
                    inferenceConfig.modelVersion(),
                    inferenceConfig.exerciseKey(),
                    sequenceNumber,
                    landmarks));

            // Kiểm tra ngưỡng tin cậy (Confidence Threshold): nếu AI model không chắc chắn -> fallback về luật hình học
            if (belowThreshold(result.confidence(), config.getConfidenceThreshold())
                    || providerReportedLowConfidence(result)) {
                return allowRuleFallback
                        ? degradedRuleFallback(
                                "MODEL_LOW_CONFIDENCE",
                                exerciseKey,
                                request.getKeypointSummaryJson(),
                                feedbackLevel)
                        : lowConfidenceAnalysis(result, feedbackLevel);
            }
            Analysis modelAnalysis = modelAnalysis(result, feedbackLevel);
            return allowRuleFallback
                    ? mergeRuleFindings(
                            modelAnalysis, result, request.getKeypointSummaryJson(), feedbackLevel)
                    : modelAnalysis;
        } catch (PostureInferenceUnavailableException exception) {
            // Khi AI sidecar không khả dụng hoặc timeout -> Fallback linh hoạt sang bộ luật hình học để bảo đảm trải nghiệm mẹ bầu không bị đứt quãng
            log.warn("Posture inference unavailable: {}", exception.getReasonCode());
            return allowRuleFallback
                    ? degradedRuleFallback(
                            "MODEL_UNAVAILABLE",
                            exerciseKey,
                            request.getKeypointSummaryJson(),
                            feedbackLevel)
                    : unavailableAnalysis(exception.getReasonCode(), feedbackLevel);
        }
    }

    /**
     * Tích hợp lịch sử chuyển động của toàn phiên tập (Session Tracker) vào kết quả phân tích.
     * Mục đích nghiệp vụ: Phát hiện các lỗi tư thế cần theo dõi cả chu kỳ chuyển động
     * (ví dụ: gập tay bicep curl không đủ độ co thắt cơ - WEAK_PEAK_CONTRACTION).
     */
    private Analysis applySessionHistory(
            UUID sessionId,
            PostureAnalysisConfig config,
            PostureEventRequest request,
            Analysis analysis,
            String feedbackLevel) {
        if (resolveMode(config) != AnalysisMode.HYBRID) {
            return analysis;
        }
        String exerciseKey = exerciseKeyOrNull(config);
        Object modelStage = analysis.metadata().get("modelStage");
        TrackedFrame tracked = sessionTracker.track(
                sessionId,
                exerciseKey,
                request.getKeypointSummaryJson(),
                modelStage instanceof String stage ? stage : null);
        if (tracked.findings().isEmpty()) {
            return analysis;
        }
        return withFindings(analysis, tracked.findings(), feedbackLevel);
    }

    /** Resolving may fail for an invalid config; that is not a reason to reject the frame. */
    private String exerciseKeyOrNull(PostureAnalysisConfig config) {
        try {
            return inferenceConfigResolver.resolve(config).exerciseKey();
        } catch (PostureInferenceUnavailableException exception) {
            return null;
        }
    }

    /**
     * Kết hợp kết quả phân loại từ Model AI với các quy tắc hình học (Geometric Rules) cho cùng 1 khung hình.
     * Quy tắc nghiệp vụ y tế: Nếu AI Model báo tư thế đúng nhưng bộ luật hình học phát hiện lỗi cơ học
     * (ví dụ: góc đầu gối quá sâu gây áp lực lên ổ khớp của mẹ bầu), lỗi hình học sẽ được ưu tiên cảnh báo
     * để đảm bảo an toàn vận động tuyệt đối cho thai phụ.
     */
    private Analysis mergeRuleFindings(
            Analysis modelAnalysis,
            InferenceResult result,
            Map<String, Object> keypoints,
            String feedbackLevel) {
        List<RuleFinding> findings = GeometricPostureRules.evaluate(
                result.exerciseKey(), keypoints, squatStage(result));
        return findings.isEmpty()
                ? modelAnalysis : withFindings(modelAnalysis, findings, feedbackLevel);
    }

    /**
     * Layers rule findings onto an existing verdict. Used both for the frame-local
     * geometry and for findings that only the session history could produce.
     */
    private Analysis withFindings(
            Analysis base, List<RuleFinding> findings, String feedbackLevel) {
        List<String> codes = findings.stream().map(RuleFinding::code).distinct().toList();
        String ruleText = codes.stream()
                .map(PostureFeedbackMessages::forRuleFinding)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
        boolean baseFoundError = !"INFO".equals(base.severity());
        String postureCode = baseFoundError ? base.postureCode() : codes.get(0);
        String detailedText = baseFoundError && base.feedbackText() != null
                ? base.feedbackText() + " " + ruleText : ruleText;

        Map<String, Object> metadata = new LinkedHashMap<>(base.metadata());
        metadata.put("analysisStatus", "HYBRID");
        @SuppressWarnings("unchecked")
        List<String> existing = metadata.get("ruleFindings") instanceof List<?> list
                ? (List<String>) list : List.of();
        metadata.put(
                "ruleFindings",
                java.util.stream.Stream.concat(existing.stream(), codes.stream())
                        .distinct()
                        .toList());

        return new Analysis(
                postureCode,
                base.confidenceScore(),
                escalate(base.severity(), findings),
                applyFeedbackLevel(feedbackLevel, postureCode, detailedText),
                metadata);
    }

    /** The phase the knee-placement band is keyed on; squats are the only user. */
    private String squatStage(InferenceResult result) {
        if (!"squat".equals(result.exerciseKey())) {
            return null;
        }
        return result.stage() != null ? result.stage() : result.predictedClass();
    }

    private String escalate(String severity, List<RuleFinding> findings) {
        if ("CRITICAL".equals(severity)) {
            return "CRITICAL";
        }
        return findings.stream().anyMatch(finding -> "CRITICAL".equals(finding.severity()))
                ? "CRITICAL" : "WARNING";
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
        // The sidecar's own message is English developer text, and a correct pose
        // carries no feedback item at all — falling back to result.predictedClass()
        // put the raw classifier label ("C", "up") in front of the user.
        String detailedText = result.feedback().stream()
                .map(InferenceFeedback::code)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .map(PostureFeedbackMessages::forModelFeedback)
                .reduce((left, right) -> left + " " + right)
                .orElseGet(() -> PostureFeedbackMessages.forModelClass(
                        result.exerciseKey(), result.predictedClass()));

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("analysisStatus", "MODEL_BASED");
        metadata.put("provider", "EXERCISE_CORRECTION");
        metadata.put("modelVersion", result.modelVersion());
        metadata.put("exerciseKey", result.exerciseKey());
        metadata.put("sequenceNumber", result.sequenceNumber());
        metadata.put("score", result.score());
        metadata.put("correct", result.correct());
        // Kept separately from postureCode, which a rule finding may take over. The
        // provider reports the phase explicitly; older providers that do not are
        // covered by falling back to the class, which is the phase for squats.
        metadata.put(
                "modelStage",
                result.stage() != null ? result.stage() : result.predictedClass());

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
                        PostureFeedbackMessages.forPostureCode(postureCode)),
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
                        PostureFeedbackMessages.forPostureCode(postureCode)),
                Map.of(
                        "analysisStatus", "MODEL_UNAVAILABLE",
                        "provider", "EXERCISE_CORRECTION",
                        "failureCode", reasonCode));
    }

    /**
     * The model half is unusable for this frame, so the rule half carries it alone:
     * the generic trunk heuristic plus, when the exercise is known, the same
     * exercise-specific geometry the hybrid path uses. Confidence is capped so a
     * degraded verdict is never mistaken for a model-grade one.
     */
    private Analysis degradedRuleFallback(
            String reasonCode,
            String exerciseKey,
            Map<String, Object> keypoints,
            String feedbackLevel) {
        Analysis fallback = analyzeKeypoints(keypoints, "DETAILED");
        // Stage-dependent checks are skipped here: without inference there is no phase.
        List<RuleFinding> findings =
                GeometricPostureRules.evaluate(exerciseKey, keypoints, null);

        String postureCode = reasonCode + "_RULE_FALLBACK_"
                + (findings.isEmpty() ? fallback.postureCode() : findings.get(0).code());
        BigDecimal confidence = fallback.confidenceScore().min(new BigDecimal("0.70"));
        String severity = "CRITICAL".equals(fallback.severity()) ? "CRITICAL" : "WARNING";
        String ruleDetail = findings.stream()
                .map(RuleFinding::code)
                .distinct()
                .map(PostureFeedbackMessages::forRuleFinding)
                .reduce((left, right) -> left + " " + right)
                .orElse(null);
        String baseDetail = fallback.feedbackText() == null
                ? PostureFeedbackMessages.forPostureCode(fallback.postureCode())
                : fallback.feedbackText();
        String detailedText = PostureFeedbackMessages.degraded(
                ruleDetail == null ? baseDetail : baseDetail + " " + ruleDetail);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("analysisStatus", "DEGRADED");
        metadata.put("provider", "RULE_BASED_FALLBACK");
        metadata.put("failureCode", reasonCode);
        metadata.put("fallbackPostureCode", fallback.postureCode());
        if (!findings.isEmpty()) {
            metadata.put(
                    "ruleFindings", findings.stream().map(RuleFinding::code).distinct().toList());
        }

        return new Analysis(
                postureCode,
                confidence,
                severity,
                applyFeedbackLevel(feedbackLevel, postureCode, detailedText),
                metadata);
    }

    private String applyFeedbackLevel(
            String feedbackLevel, String postureCode, String detailedText) {
        return switch (feedbackLevel) {
            case "SILENT" -> null;
            // BASIC used to echo the raw postureCode, which is a machine identifier.
            case "BASIC" -> PostureFeedbackMessages.shortLabel(postureCode);
            default -> detailedText;
        };
    }

    /**
     * Phân tích tư thế dựa trên tập luật hình học thuần (RULE_BASED Heuristic).
     *
     * <p>Nghiệp vụ Y tế: Đánh giá góc nghiêng cột sống lưng (backAngle) để bảo vệ cột sống và vùng thắt lưng của thai phụ:
     * - backAngle <= 15°: Tư thế lưng thẳng, chuẩn (GOOD_FORM - INFO).
     * - 15° < backAngle <= 30°: Lưng hơi cong nhẹ (MILD_ROUNDING - WARNING).
     * - backAngle > 30°: Lưng gù/cong gập quá mức, có nguy cơ gây chèn ép cột sống & áp lực lên thai nhi (ROUND_BACK - CRITICAL).
     */
    private Analysis analyzeKeypoints(java.util.Map<String, Object> keypoints, String feedbackLevel) {
        Object backAngleRaw = keypoints != null ? keypoints.get("backAngle") : null;
        Double backAngle = toDouble(backAngleRaw);
        // Nếu client không gửi trường backAngle dựng sẵn, tự động tính toán góc nghiêng thân từ các mốc vai và hông
        if (backAngle == null) {
            backAngle = trunkLeanFromLandmarks(keypoints);
        }

        String postureCode;
        BigDecimal confidenceScore;
        String severity;

        if (backAngle == null) {
            postureCode = "UNKNOWN";
            confidenceScore = new BigDecimal("0.50");
            severity = "INFO";
        } else if (backAngle <= 15.0) {
            postureCode = "GOOD_FORM";
            confidenceScore = new BigDecimal("0.95");
            severity = "INFO";
        } else if (backAngle <= 30.0) {
            postureCode = "MILD_ROUNDING";
            confidenceScore = new BigDecimal("0.70");
            severity = "WARNING";
        } else {
            postureCode = "ROUND_BACK";
            confidenceScore = new BigDecimal("0.40");
            severity = "CRITICAL";
        }

        String feedbackText = applyFeedbackLevel(
                feedbackLevel, postureCode, PostureFeedbackMessages.forPostureCode(postureCode));

        return new Analysis(
                postureCode,
                confidenceScore,
                severity,
                feedbackText,
                Map.of("analysisStatus", "RULE_BASED"));
    }

    /**
     * Ước lượng góc nghiêng thân người (Trunk Lean tính theo độ lệch so với phương thẳng đứng)
     * từ trung điểm của 2 vai (left_shoulder, right_shoulder) và trung điểm của 2 hông (left_hip, right_hip).
     * Sử dụng hàm lượng giác atan2(horizontal, vertical) trên tọa độ chuẩn hóa MediaPipe.
     */
    private Double trunkLeanFromLandmarks(Map<String, Object> keypoints) {
        if (keypoints == null) {
            return null;
        }
        Double shoulderX = midpoint(keypoints, "left_shoulder", "right_shoulder", "x");
        Double shoulderY = midpoint(keypoints, "left_shoulder", "right_shoulder", "y");
        Double hipX = midpoint(keypoints, "left_hip", "right_hip", "x");
        Double hipY = midpoint(keypoints, "left_hip", "right_hip", "y");
        if (shoulderX == null || shoulderY == null || hipX == null || hipY == null) {
            return null;
        }

        double horizontal = Math.abs(shoulderX - hipX);
        double vertical = Math.abs(hipY - shoulderY);
        if (horizontal < MIN_TRUNK_SPAN && vertical < MIN_TRUNK_SPAN) {
            return null;
        }
        return Math.toDegrees(Math.atan2(horizontal, vertical));
    }

    private Double midpoint(
            Map<String, Object> keypoints, String leftName, String rightName, String axis) {
        Double left = landmarkAxis(keypoints.get(leftName), axis);
        Double right = landmarkAxis(keypoints.get(rightName), axis);
        if (left == null || right == null) {
            return null;
        }
        return (left + right) / 2.0;
    }

    private Double landmarkAxis(Object landmark, String axis) {
        if (!(landmark instanceof Map<?, ?> point)) {
            return null;
        }
        Double visibility = toDouble(point.get("visibility"));
        if (visibility == null || visibility < MIN_LANDMARK_VISIBILITY) {
            return null;
        }
        return toDouble(point.get(axis));
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

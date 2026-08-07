package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.exception.SessionNotFoundException;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.PostureEventRequest;
import com.carebridge.backend.exercise.dto.PostureFeedbackResponse;
import com.carebridge.backend.exercise.entity.ExerciseSession;
import com.carebridge.backend.exercise.entity.PostureAnalysisConfig;
import com.carebridge.backend.exercise.entity.PostureFeedbackEvent;
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
import com.carebridge.backend.exercise.policy.PostureSessionTracker;
import com.carebridge.backend.exercise.repository.ExerciseSessionRepository;
import com.carebridge.backend.exercise.repository.PostureAnalysisConfigRepository;
import com.carebridge.backend.exercise.repository.PostureFeedbackEventRepository;
import com.carebridge.backend.exercise.service.ExerciseCareContextResolver;
import com.carebridge.backend.exercise.service.impl.PostureAnalysisServiceImpl;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostureAnalysisServiceTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID EXERCISE_ID = UUID.randomUUID();
    private static final UUID MOTHER_USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID JOURNEY_ID = UUID.randomUUID();
    private static final UUID CARE_SUBJECT_ID = UUID.randomUUID();

    @Mock
    private ExerciseSessionRepository sessionRepository;

    @Mock
    private PostureAnalysisConfigRepository postureConfigRepository;

    @Mock
    private PostureFeedbackEventRepository postureFeedbackEventRepository;

    @Mock
    private PostureInferencePort postureInferencePort;

    @Mock
    private PostureInferenceConfigResolver inferenceConfigResolver;

    @Mock
    private ExerciseCareContextResolver careContextResolver;

    /** Real, not mocked: the transition gate and repetition counters are under test. */
    @Spy
    private PostureSessionTracker sessionTracker = new PostureSessionTracker();

    @InjectMocks
    private PostureAnalysisServiceImpl service;

    private ExerciseSession inProgressSession() {
        return ExerciseSession.builder()
                .exerciseSessionId(SESSION_ID)
                .exerciseId(EXERCISE_ID)
                .userId(MOTHER_USER_ID)
                .journeyId(JOURNEY_ID)
                .startedAt(OffsetDateTime.now())
                .sessionStatus(SessionStatus.IN_PROGRESS)
                .warningCount(0)
                .build();
    }

    private void stubCareContext() {
        when(careContextResolver.resolve(MOTHER_USER_ID, JOURNEY_ID))
                .thenReturn(new ExerciseCareContextResolver.CareContext(JOURNEY_ID, CARE_SUBJECT_ID));
    }

    private PostureAnalysisConfig activeConfig(String feedbackLevel) {
        return PostureAnalysisConfig.builder()
                .postureConfigId(UUID.randomUUID())
                .exerciseId(EXERCISE_ID)
                .analysisMode("RULE_BASED")
                .confidenceThreshold(new BigDecimal("0.5"))
                .feedbackLevel(feedbackLevel)
                .status("ACTIVE")
                .build();
    }

    private PostureEventRequest requestWithBackAngle(double backAngle) {
        PostureEventRequest request = new PostureEventRequest();
        request.setEventTimeMs(1000L);
        request.setKeypointSummaryJson(Map.of("backAngle", backAngle));
        return request;
    }

    /**
     * Builds the landmark-shaped payload a realtime camera client actually posts:
     * named MediaPipe points, with no precomputed "backAngle" field.
     */
    private PostureEventRequest requestWithTrunkLandmarks(
            double shoulderX, double shoulderY, double hipX, double hipY) {
        PostureEventRequest request = new PostureEventRequest();
        request.setEventTimeMs(1000L);
        request.setKeypointSummaryJson(Map.of(
                "left_shoulder", landmark(shoulderX, shoulderY, 0.95),
                "right_shoulder", landmark(shoulderX, shoulderY, 0.95),
                "left_hip", landmark(hipX, hipY, 0.95),
                "right_hip", landmark(hipX, hipY, 0.95)));
        return request;
    }

    private Map<String, Object> landmark(double x, double y, double visibility) {
        return Map.of("x", x, "y", y, "z", 0.0, "visibility", visibility);
    }

    private PostureAnalysisConfig modelConfig(String analysisMode, BigDecimal threshold) {
        return PostureAnalysisConfig.builder()
                .postureConfigId(UUID.randomUUID())
                .exerciseId(EXERCISE_ID)
                .analysisMode(analysisMode)
                .ruleOrModelVersion(PostureInferenceConfigResolver.PINNED_MODEL_VERSION)
                .confidenceThreshold(threshold)
                .feedbackLevel("DETAILED")
                .configJson("{\"exerciseKey\":\"squat\"}")
                .status("ACTIVE")
                .build();
    }

    /**
     * Shaped like a real sidecar reply: a raw classifier label plus a stable
     * feedback code, with English developer text that must never reach the user.
     */
    private InferenceResult inferenceResult(BigDecimal confidence) {
        return new InferenceResult(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION,
                "lunge",
                1000L,
                "L",
                confidence,
                false,
                new BigDecimal("0.62"),
                List.of(new InferenceFeedback(
                        "knee_over_toe",
                        "WARNING",
                        "Front knee is classified as moving too far over the toes.")),
                // Down phase: the class became the error verdict, the phase rides along.
                "D");
    }

    /** A correct pose: the sidecar sends a bare classifier label and no feedback. */
    private InferenceResult correctInferenceResult(String exerciseKey, String predictedClass) {
        return correctInferenceResult(exerciseKey, predictedClass, null);
    }

    private InferenceResult correctInferenceResult(
            String exerciseKey, String predictedClass, String stage) {
        return new InferenceResult(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION,
                exerciseKey,
                1000L,
                predictedClass,
                new BigDecimal("0.99"),
                true,
                new BigDecimal("99"),
                List.of(),
                stage);
    }

    // EX-TC-030-005 — happy path with feedback + severity
    @Test
    @DisplayName("EX-TC-030-005: submit posture event returns feedback with severity, saves event")
    void analyzePosture_happyPath_returnsFeedbackAndSavesEvent() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(activeConfig("DETAILED")));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(5.0));

        assertThat(response.getData().getPostureCode()).isNotNull();
        assertThat(response.getData().getConfidenceScore())
                .isGreaterThanOrEqualTo(BigDecimal.ZERO)
                .isLessThanOrEqualTo(BigDecimal.ONE);
        assertThat(response.getData().getSeverity()).isIn("INFO", "WARNING", "CRITICAL");
        assertThat(response.getData().getFeedbackText()).isNotNull();

        verify(postureFeedbackEventRepository).save(any(PostureFeedbackEvent.class));
    }

    // EX-TC-030-005 — CRITICAL severity increments session warningCount
    @Test
    @DisplayName("EX-TC-030-005: CRITICAL severity (large backAngle) increments session warningCount")
    void analyzePosture_criticalSeverity_incrementsWarningCount() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(activeConfig("DETAILED")));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(45.0));

        assertThat(response.getData().getSeverity()).isEqualTo("CRITICAL");

        ArgumentCaptor<ExerciseSession> captor = ArgumentCaptor.forClass(ExerciseSession.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getWarningCount()).isEqualTo(1);
    }

    // EX-TC-030-005-B — no posture config → fallback RULE_BASED, no exception
    @Test
    @DisplayName("EX-TC-030-005-B: no posture config falls back to RULE_BASED default, no exception")
    void analyzePosture_noConfig_fallsBackToRuleBased() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.empty());

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(5.0));

        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getPostureCode()).isEqualTo("GOOD_FORM");
        verify(postureFeedbackEventRepository).save(any(PostureFeedbackEvent.class));
    }

    // Session not found
    @Test
    @DisplayName("analyzePosture: session not found throws SessionNotFoundException")
    void analyzePosture_sessionNotFound_throws() {
        UUID unknownId = UUID.randomUUID();
        when(sessionRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.analyzePosture(unknownId, MOTHER_USER_ID, requestWithBackAngle(5.0)))
                .isInstanceOf(SessionNotFoundException.class);

        verify(postureFeedbackEventRepository, never()).save(any());
    }

    // TC-COND-011 — session ownership check
    @Test
    @DisplayName("EX-TC-030-SEC-001: session belongs to a different user throws SessionOwnershipException")
    void analyzePosture_wrongOwner_throws() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));

        assertThatThrownBy(() ->
                service.analyzePosture(SESSION_ID, OTHER_USER_ID, requestWithBackAngle(5.0)))
                .isInstanceOf(SessionOwnershipException.class);

        verify(postureFeedbackEventRepository, never()).save(any());
    }

    // Session not IN_PROGRESS (e.g. COMPLETED)
    @Test
    @DisplayName("analyzePosture: session not IN_PROGRESS throws InvalidSessionStateException EXSESS-009")
    void analyzePosture_sessionCompleted_throws() {
        ExerciseSession completed = inProgressSession();
        completed.setSessionStatus(SessionStatus.COMPLETED);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() ->
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(5.0)))
                .isInstanceOf(InvalidSessionStateException.class)
                .satisfies(ex -> assertThat(((InvalidSessionStateException) ex).getCode())
                        .isEqualTo("EXSESS-009"));

        verify(postureFeedbackEventRepository, never()).save(any());
    }

    // SILENT feedback level suppresses feedbackText
    @Test
    @DisplayName("analyzePosture: SILENT feedback level produces null feedbackText")
    void analyzePosture_silentFeedbackLevel_nullFeedbackText() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(activeConfig("SILENT")));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(5.0));

        assertThat(response.getData().getFeedbackText()).isNull();
    }

    @Test
    @DisplayName("MODEL_BASED dispatch uses server-owned config and maps normalized inference")
    void analyzePosture_modelBased_dispatchesAndMapsInference() {
        PostureAnalysisConfig config = modelConfig("MODEL_BASED", new BigDecimal("0.60"));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config)).thenReturn(new ResolvedInferenceConfig(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION, "squat"));
        when(postureInferencePort.infer(any(InferenceRequest.class)))
                .thenReturn(inferenceResult(new BigDecimal("0.82")));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(5.0));

        // postureCode stays the machine label; only the user-facing text is localized.
        assertThat(response.getData().getPostureCode()).isEqualTo("L");
        assertThat(response.getData().getConfidenceScore()).isEqualByComparingTo("0.82");
        assertThat(response.getData().getSeverity()).isEqualTo("WARNING");
        assertThat(response.getData().getFeedbackText())
                .isEqualTo("Gối trước đang vượt quá mũi chân — dồn trọng tâm về sau.");

        ArgumentCaptor<InferenceRequest> requestCaptor =
                ArgumentCaptor.forClass(InferenceRequest.class);
        verify(postureInferencePort).infer(requestCaptor.capture());
        assertThat(requestCaptor.getValue().modelVersion())
                .isEqualTo(PostureInferenceConfigResolver.PINNED_MODEL_VERSION);
        assertThat(requestCaptor.getValue().exerciseKey()).isEqualTo("squat");
        assertThat(requestCaptor.getValue().sequenceNumber()).isEqualTo(1000L);
        assertThat(requestCaptor.getValue().landmarks()).containsEntry("backAngle", 5.0);
    }

    @Test
    @DisplayName("MODEL_BASED sidecar failure returns explicit unavailable result")
    void analyzePosture_modelBasedSidecarFailure_returnsUnavailable() {
        PostureAnalysisConfig config = modelConfig("MODEL_BASED", new BigDecimal("0.60"));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config)).thenReturn(new ResolvedInferenceConfig(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION, "squat"));
        when(postureInferencePort.infer(any(InferenceRequest.class)))
                .thenThrow(new PostureInferenceUnavailableException("SIDECAR_TIMEOUT_OR_UNREACHABLE"));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(5.0));

        assertThat(response.getData().getPostureCode()).isEqualTo("MODEL_UNAVAILABLE");
        assertThat(response.getData().getConfidenceScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getData().getSeverity()).isEqualTo("WARNING");

        ArgumentCaptor<PostureFeedbackEvent> eventCaptor =
                ArgumentCaptor.forClass(PostureFeedbackEvent.class);
        verify(postureFeedbackEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getCanonicalPayload())
                .containsEntry("analysisStatus", "MODEL_UNAVAILABLE")
                .containsEntry("failureCode", "SIDECAR_TIMEOUT_OR_UNREACHABLE");
    }

    @Test
    @DisplayName("HYBRID sidecar failure explicitly degrades to bounded rule fallback")
    void analyzePosture_hybridSidecarFailure_usesExplicitRuleFallback() {
        PostureAnalysisConfig config = modelConfig("HYBRID", new BigDecimal("0.60"));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config)).thenReturn(new ResolvedInferenceConfig(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION, "squat"));
        when(postureInferencePort.infer(any(InferenceRequest.class)))
                .thenThrow(new PostureInferenceUnavailableException("SIDECAR_UNAVAILABLE"));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(5.0));

        assertThat(response.getData().getPostureCode())
                .isEqualTo("MODEL_UNAVAILABLE_RULE_FALLBACK_GOOD_FORM");
        assertThat(response.getData().getConfidenceScore()).isEqualByComparingTo("0.70");
        assertThat(response.getData().getSeverity()).isEqualTo("WARNING");
        assertThat(response.getData().getFeedbackText())
                .isEqualTo("Đang dùng phân tích dự phòng: Tư thế tốt! Giữ lưng thẳng.");
    }

    @Test
    @DisplayName("A correct pose is narrated in Vietnamese, never as the raw model label")
    void analyzePosture_correctModelClass_neverExposesRawLabel() {
        PostureAnalysisConfig config = modelConfig("MODEL_BASED", new BigDecimal("0.60"));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config)).thenReturn(new ResolvedInferenceConfig(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION, "bicep_curl"));
        when(postureInferencePort.infer(any(InferenceRequest.class)))
                .thenReturn(correctInferenceResult("bicep_curl", "C"));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(5.0));

        assertThat(response.getData().getPostureCode()).isEqualTo("C");
        assertThat(response.getData().getSeverity()).isEqualTo("INFO");
        assertThat(response.getData().getFeedbackText())
                .isEqualTo("Tư thế đứng tốt — giữ khuỷu tay sát thân.");
    }

    /** A squat pose the model calls correct but whose stance is narrower than 1.2x shoulders. */
    private PostureEventRequest squatWithNarrowStance() {
        PostureEventRequest request = new PostureEventRequest();
        request.setEventTimeMs(1000L);
        request.setKeypointSummaryJson(Map.of(
                "left_shoulder", landmark(0.40, 0.30, 0.95),
                "right_shoulder", landmark(0.60, 0.30, 0.95),
                "left_foot_index", landmark(0.45, 0.90, 0.95),
                "right_foot_index", landmark(0.55, 0.90, 0.95),
                "left_knee", landmark(0.455, 0.70, 0.95),
                "right_knee", landmark(0.545, 0.70, 0.95)));
        return request;
    }

    @Test
    @DisplayName("HYBRID surfaces a rule error the squat model cannot detect")
    void analyzePosture_hybridRuleFindsWhatModelCannot() {
        PostureAnalysisConfig config = modelConfig("HYBRID", new BigDecimal("0.60"));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config)).thenReturn(new ResolvedInferenceConfig(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION, "squat"));
        // The squat model only classifies the phase, and calls this frame correct.
        when(postureInferencePort.infer(any(InferenceRequest.class)))
                .thenReturn(correctInferenceResult("squat", "down"));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, squatWithNarrowStance());

        assertThat(response.getData().getPostureCode()).isEqualTo("FOOT_PLACEMENT_TOO_TIGHT");
        assertThat(response.getData().getSeverity()).isEqualTo("WARNING");
        assertThat(response.getData().getFeedbackText())
                .isEqualTo("Hai bàn chân đang quá hẹp — mở rộng gần bằng vai.");

        ArgumentCaptor<PostureFeedbackEvent> eventCaptor =
                ArgumentCaptor.forClass(PostureFeedbackEvent.class);
        verify(postureFeedbackEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getCanonicalPayload())
                .containsEntry("analysisStatus", "HYBRID")
                .containsEntry("ruleFindings", List.of("FOOT_PLACEMENT_TOO_TIGHT"));
    }

    @Test
    @DisplayName("MODEL_BASED stays model-only on the very same frame")
    void analyzePosture_modelBasedIgnoresGeometricRules() {
        PostureAnalysisConfig config = modelConfig("MODEL_BASED", new BigDecimal("0.60"));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config)).thenReturn(new ResolvedInferenceConfig(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION, "squat"));
        when(postureInferencePort.infer(any(InferenceRequest.class)))
                .thenReturn(correctInferenceResult("squat", "down"));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, squatWithNarrowStance());

        assertThat(response.getData().getPostureCode()).isEqualTo("down");
        assertThat(response.getData().getSeverity()).isEqualTo("INFO");
        assertThat(response.getData().getFeedbackText())
                .isEqualTo("Nhịp hạ người — giữ gối theo hướng mũi chân.");
    }

    @Test
    @DisplayName("HYBRID keeps the model verdict and appends the rule advice")
    void analyzePosture_hybridCombinesModelAndRuleText() {
        PostureAnalysisConfig config = modelConfig("HYBRID", new BigDecimal("0.60"));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config)).thenReturn(new ResolvedInferenceConfig(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION, "squat"));
        when(postureInferencePort.infer(any(InferenceRequest.class)))
                .thenReturn(new InferenceResult(
                        PostureInferenceConfigResolver.PINNED_MODEL_VERSION,
                        "squat",
                        1000L,
                        "down",
                        new BigDecimal("0.90"),
                        false,
                        new BigDecimal("40"),
                        List.of(new InferenceFeedback(
                                "knee_over_toe", "WARNING", "Front knee too far over the toes.")),
                        "down"));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, squatWithNarrowStance());

        // The model already found an error, so its code stays the headline.
        assertThat(response.getData().getPostureCode()).isEqualTo("down");
        assertThat(response.getData().getSeverity()).isEqualTo("WARNING");
        assertThat(response.getData().getFeedbackText())
                .isEqualTo("Gối trước đang vượt quá mũi chân — dồn trọng tâm về sau."
                        + " Hai bàn chân đang quá hẹp — mở rộng gần bằng vai.");
    }

    @Test
    @DisplayName("A posture held across frames is persisted once, not on every frame")
    void analyzePosture_repeatedIdenticalPosture_persistsOnlyOnEntry() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(activeConfig("DETAILED")));

        // Same good posture on three consecutive frames.
        service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(5.0));
        service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(6.0));
        ApiResponse<PostureFeedbackResponse> third =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(7.0));

        // Realtime feedback still answers every frame...
        assertThat(third.getData().getPostureCode()).isEqualTo("GOOD_FORM");
        // ...but only the frame that entered the posture was recorded.
        verify(postureFeedbackEventRepository, times(1)).save(any(PostureFeedbackEvent.class));

        // Changing posture is a new entry and is recorded again.
        service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(45.0));
        verify(postureFeedbackEventRepository, times(2)).save(any(PostureFeedbackEvent.class));
    }

    @Test
    @DisplayName("A sustained CRITICAL posture increments warningCount once per entry")
    void analyzePosture_sustainedCritical_incrementsWarningCountOnce() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(activeConfig("DETAILED")));

        service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(45.0));
        service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(46.0));
        service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(47.0));

        verify(sessionRepository, times(1)).save(any(ExerciseSession.class));
    }

    @Test
    @DisplayName("HYBRID reports a weak peak contraction once the curl extends again")
    void analyzePosture_weakPeakContraction_surfacesOnExtension() {
        PostureAnalysisConfig config = modelConfig("HYBRID", new BigDecimal("0.60"));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config)).thenReturn(new ResolvedInferenceConfig(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION, "bicep_curl"));
        when(postureInferencePort.infer(any(InferenceRequest.class)))
                .thenReturn(correctInferenceResult("bicep_curl", "C"));

        // Extend, curl only to 90 degrees, then extend again.
        service.analyzePosture(SESSION_ID, MOTHER_USER_ID, curlRequest(150.0));
        service.analyzePosture(SESSION_ID, MOTHER_USER_ID, curlRequest(90.0));
        ApiResponse<PostureFeedbackResponse> extended =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, curlRequest(150.0));

        assertThat(extended.getData().getPostureCode()).isEqualTo("WEAK_PEAK_CONTRACTION");
        assertThat(extended.getData().getSeverity()).isEqualTo("WARNING");
        assertThat(extended.getData().getFeedbackText())
                .isEqualTo("Nhịp vừa rồi chưa cuốn đủ cao — gập tay sâu hơn ở điểm trên.");
    }

    /** A left arm posed at the requested shoulder-elbow-wrist angle. */
    private PostureEventRequest curlRequest(double degrees) {
        double radians = Math.toRadians(degrees);
        PostureEventRequest request = new PostureEventRequest();
        request.setEventTimeMs(1000L);
        request.setKeypointSummaryJson(Map.of(
                "left_shoulder", landmark(0.5, 0.3, 0.95),
                "left_elbow", landmark(0.5, 0.5, 0.95),
                "left_wrist", landmark(
                        0.5 + 0.2 * Math.sin(radians), 0.5 - 0.2 * Math.cos(radians), 0.95)));
        return request;
    }

    @Test
    @DisplayName("BASIC feedback level returns a Vietnamese label, not the machine code")
    void analyzePosture_basicFeedbackLevel_returnsVietnameseLabel() {
        PostureAnalysisConfig config = activeConfig("BASIC");
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(45.0));

        assertThat(response.getData().getPostureCode()).isEqualTo("ROUND_BACK");
        assertThat(response.getData().getFeedbackText()).isEqualTo("Lưng cong nhiều");
    }

    @Test
    @DisplayName("HYBRID fallback derives posture from MediaPipe landmarks, not only backAngle")
    void analyzePosture_hybridFallbackWithLandmarks_derivesTrunkLean() {
        PostureAnalysisConfig config = modelConfig("HYBRID", new BigDecimal("0.60"));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config)).thenReturn(new ResolvedInferenceConfig(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION, "squat"));
        when(postureInferencePort.infer(any(InferenceRequest.class)))
                .thenThrow(new PostureInferenceUnavailableException("SIDECAR_REJECTED_REQUEST"));

        // Shoulders sit almost directly above the hips — an upright trunk.
        ApiResponse<PostureFeedbackResponse> response = service.analyzePosture(
                SESSION_ID, MOTHER_USER_ID, requestWithTrunkLandmarks(0.50, 0.30, 0.50, 0.70));

        assertThat(response.getData().getPostureCode())
                .isEqualTo("MODEL_UNAVAILABLE_RULE_FALLBACK_GOOD_FORM");
        assertThat(response.getData().getSeverity()).isEqualTo("WARNING");
    }

    @Test
    @DisplayName("HYBRID fallback flags a strongly rounded trunk as CRITICAL")
    void analyzePosture_hybridFallbackWithLeaningTrunk_isCritical() {
        PostureAnalysisConfig config = modelConfig("HYBRID", new BigDecimal("0.60"));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config)).thenReturn(new ResolvedInferenceConfig(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION, "squat"));
        when(postureInferencePort.infer(any(InferenceRequest.class)))
                .thenThrow(new PostureInferenceUnavailableException("SIDECAR_REJECTED_REQUEST"));

        // Shoulders far ahead of the hips — roughly 45 degrees off vertical.
        ApiResponse<PostureFeedbackResponse> response = service.analyzePosture(
                SESSION_ID, MOTHER_USER_ID, requestWithTrunkLandmarks(0.30, 0.30, 0.70, 0.70));

        assertThat(response.getData().getPostureCode())
                .isEqualTo("MODEL_UNAVAILABLE_RULE_FALLBACK_ROUND_BACK");
        assertThat(response.getData().getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("Rule fallback stays UNKNOWN when trunk landmarks are not confidently visible")
    void analyzePosture_ruleFallbackWithOccludedTrunk_staysUnknown() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(activeConfig("DETAILED")));

        PostureEventRequest request =
                requestWithTrunkLandmarks(0.30, 0.30, 0.70, 0.70);
        Map<String, Object> occluded = new java.util.LinkedHashMap<>(request.getKeypointSummaryJson());
        occluded.put("left_hip", landmark(0.30, 0.70, 0.10));
        request.setKeypointSummaryJson(occluded);

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, request);

        assertThat(response.getData().getPostureCode()).isEqualTo("UNKNOWN");
        assertThat(response.getData().getSeverity()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("MODEL_BASED confidence below server threshold is not presented as success")
    void analyzePosture_modelBelowThreshold_returnsLowConfidence() {
        PostureAnalysisConfig config = modelConfig("MODEL_BASED", new BigDecimal("0.75"));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config)).thenReturn(new ResolvedInferenceConfig(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION, "squat"));
        when(postureInferencePort.infer(any(InferenceRequest.class)))
                .thenReturn(inferenceResult(new BigDecimal("0.52")));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(5.0));

        assertThat(response.getData().getPostureCode()).isEqualTo("MODEL_LOW_CONFIDENCE");
        assertThat(response.getData().getConfidenceScore()).isEqualByComparingTo("0.52");
        assertThat(response.getData().getSeverity()).isEqualTo("WARNING");
    }

    @Test
    @DisplayName("MODEL_BASED provider low-confidence marker is not presented as a posture class")
    void analyzePosture_providerLowConfidence_returnsLowConfidence() {
        PostureAnalysisConfig config = modelConfig("MODEL_BASED", new BigDecimal("0.50"));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
        stubCareContext();
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(config));
        when(inferenceConfigResolver.resolve(config)).thenReturn(new ResolvedInferenceConfig(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION, "squat"));
        when(postureInferencePort.infer(any(InferenceRequest.class)))
                .thenReturn(new InferenceResult(
                        PostureInferenceConfigResolver.PINNED_MODEL_VERSION,
                        "squat",
                        1000L,
                        "unknown",
                        new BigDecimal("0.60"),
                        false,
                        BigDecimal.ZERO,
                        List.of(new InferenceFeedback(
                                "low_confidence",
                                "INFO",
                                "The demo model is not confident enough to evaluate this sample.")),
                        // Below threshold the sidecar reports no trustworthy phase.
                        null));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(5.0));

        assertThat(response.getData().getPostureCode()).isEqualTo("MODEL_LOW_CONFIDENCE");
        assertThat(response.getData().getSeverity()).isEqualTo("WARNING");
    }
}

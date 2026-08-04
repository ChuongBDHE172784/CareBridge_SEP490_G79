package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

    private InferenceResult inferenceResult(BigDecimal confidence) {
        return new InferenceResult(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION,
                "squat",
                1000L,
                "KNEE_TOO_WIDE",
                confidence,
                false,
                new BigDecimal("0.62"),
                List.of(new InferenceFeedback(
                        "KNEE_TOO_WIDE", "WARNING", "Bring your knees slightly inward.")));
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

        assertThat(response.getData().getPostureCode()).isEqualTo("KNEE_TOO_WIDE");
        assertThat(response.getData().getConfidenceScore()).isEqualByComparingTo("0.82");
        assertThat(response.getData().getSeverity()).isEqualTo("WARNING");
        assertThat(response.getData().getFeedbackText())
                .isEqualTo("Bring your knees slightly inward.");

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
        assertThat(response.getData().getFeedbackText()).contains("rule-based fallback");
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
                                "The demo model is not confident enough to evaluate this sample."))));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(5.0));

        assertThat(response.getData().getPostureCode()).isEqualTo("MODEL_LOW_CONFIDENCE");
        assertThat(response.getData().getSeverity()).isEqualTo("WARNING");
    }
}

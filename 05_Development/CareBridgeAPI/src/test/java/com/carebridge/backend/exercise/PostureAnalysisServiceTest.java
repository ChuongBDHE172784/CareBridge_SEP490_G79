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
import com.carebridge.backend.exercise.repository.ExerciseSessionRepository;
import com.carebridge.backend.exercise.repository.PostureAnalysisConfigRepository;
import com.carebridge.backend.exercise.repository.PostureFeedbackEventRepository;
import com.carebridge.backend.exercise.service.impl.PostureAnalysisServiceImpl;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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

    @Mock
    private ExerciseSessionRepository sessionRepository;

    @Mock
    private PostureAnalysisConfigRepository postureConfigRepository;

    @Mock
    private PostureFeedbackEventRepository postureFeedbackEventRepository;

    @InjectMocks
    private PostureAnalysisServiceImpl service;

    private ExerciseSession inProgressSession() {
        return ExerciseSession.builder()
                .exerciseSessionId(SESSION_ID)
                .exerciseId(EXERCISE_ID)
                .userId(MOTHER_USER_ID)
                .startedAt(OffsetDateTime.now())
                .sessionStatus(SessionStatus.IN_PROGRESS)
                .warningCount(0)
                .build();
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

    // EX-TC-030-005 — happy path with feedback + severity
    @Test
    @DisplayName("EX-TC-030-005: submit posture event returns feedback with severity, saves event")
    void analyzePosture_happyPath_returnsFeedbackAndSavesEvent() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession()));
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
        when(postureConfigRepository.findActiveConfigByExerciseId(eq(EXERCISE_ID), any()))
                .thenReturn(Optional.of(activeConfig("SILENT")));

        ApiResponse<PostureFeedbackResponse> response =
                service.analyzePosture(SESSION_ID, MOTHER_USER_ID, requestWithBackAngle(5.0));

        assertThat(response.getData().getFeedbackText()).isNull();
    }
}

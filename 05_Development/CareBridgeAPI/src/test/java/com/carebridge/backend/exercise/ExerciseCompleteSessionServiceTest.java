package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.exercise.dto.SessionResultResponse;
import com.carebridge.backend.exercise.entity.ExerciseSession;
import com.carebridge.backend.exercise.entity.PostureFeedbackEvent;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.entity.SessionStatus;
import com.carebridge.backend.exercise.exception.InvalidSessionStateException;
import com.carebridge.backend.exercise.exception.SessionOwnershipException;
import com.carebridge.backend.exercise.mapper.ExerciseSessionMapper;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.repository.ExerciseSafetyCheckRepository;
import com.carebridge.backend.exercise.policy.PostureSessionTracker;
import com.carebridge.backend.exercise.repository.ExerciseSessionRepository;
import com.carebridge.backend.exercise.repository.PostureFeedbackEventRepository;
import com.carebridge.backend.exercise.service.impl.ExerciseSessionServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExerciseCompleteSessionServiceTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID EXERCISE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    @Mock
    private ExerciseSessionRepository sessionRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private ExerciseSafetyCheckRepository safetyCheckRepository;

    @Mock
    private PostureFeedbackEventRepository postureFeedbackEventRepository;

    @Spy
    private ExerciseSessionMapper sessionMapper;

    @Spy
    private ObjectMapper objectMapper;

    /** Real: the completion summary reads the repetitions it counted. */
    @Spy
    private PostureSessionTracker sessionTracker = new PostureSessionTracker();

    @InjectMocks
    private ExerciseSessionServiceImpl service;

    private ExerciseSession session(SessionStatus status, UUID owner, long startedMinutesAgo) {
        return ExerciseSession.builder()
                .exerciseSessionId(SESSION_ID)
                .exerciseId(EXERCISE_ID)
                .userId(owner)
                .sessionStatus(status)
                .pausedSeconds(0)
                .warningCount(0)
                .startedAt(OffsetDateTime.now().minusMinutes(startedMinutesAgo))
                .updatedAt(OffsetDateTime.now().minusMinutes(startedMinutesAgo))
                .build();
    }

    private PregnancyExercise exercise(short durationMinutes) {
        PregnancyExercise exercise = new PregnancyExercise();
        exercise.setExerciseId(EXERCISE_ID);
        exercise.setTitle("Pelvic Tilt");
        exercise.setDurationMinutes(durationMinutes);
        return exercise;
    }

    private PostureFeedbackEvent event(String severity, String code, BigDecimal confidence) {
        return PostureFeedbackEvent.builder()
                .feedbackEventId(UUID.randomUUID())
                .exerciseSessionId(SESSION_ID)
                .severity(severity)
                .postureCode(code)
                .confidenceScore(confidence)
                .build();
    }

    @Test
    @DisplayName("COMP-TC-001: IN_PROGRESS → COMPLETED with metrics")
    void complete_inProgress_completesWithMetrics() {
        when(sessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.IN_PROGRESS, USER_ID, 5)));
        when(exerciseRepository.findById(EXERCISE_ID))
                .thenReturn(Optional.of(exercise((short) 10)));
        when(postureFeedbackEventRepository.findByExerciseSessionId(SESSION_ID))
                .thenReturn(List.of());
        when(sessionRepository.save(any(ExerciseSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SessionResultResponse response = service.completeSession(SESSION_ID, USER_ID);

        assertThat(response.getSessionStatus()).isEqualTo(SessionStatus.COMPLETED.name());
        assertThat(response.getEndedAt()).isNotNull();
        assertThat(response.getExerciseTitle()).isEqualTo("Pelvic Tilt");
        assertThat(response.getCompletionPercent()).isNotNull();
        assertThat(response.getActualDurationSeconds()).isNotNull();
    }

    @Test
    @DisplayName("COMP-TC-002: PAUSED → COMPLETED (valid state)")
    void complete_paused_completes() {
        when(sessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.PAUSED, USER_ID, 5)));
        when(exerciseRepository.findById(EXERCISE_ID))
                .thenReturn(Optional.of(exercise((short) 10)));
        when(postureFeedbackEventRepository.findByExerciseSessionId(SESSION_ID))
                .thenReturn(List.of());
        when(sessionRepository.save(any(ExerciseSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SessionResultResponse response = service.completeSession(SESSION_ID, USER_ID);

        assertThat(response.getSessionStatus()).isEqualTo(SessionStatus.COMPLETED.name());
    }

    @Test
    @DisplayName("COMP-TC-003: already COMPLETED → InvalidSessionStateException EXSESS-008")
    void complete_alreadyCompleted_throws() {
        when(sessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.COMPLETED, USER_ID, 5)));

        assertThatThrownBy(() -> service.completeSession(SESSION_ID, USER_ID))
                .isInstanceOf(InvalidSessionStateException.class)
                .extracting("code").isEqualTo("EXSESS-008");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("COMP-TC-004: wrong user → SessionOwnershipException")
    void complete_wrongUser_throws() {
        when(sessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.IN_PROGRESS, OTHER_USER_ID, 5)));

        assertThatThrownBy(() -> service.completeSession(SESSION_ID, USER_ID))
                .isInstanceOf(SessionOwnershipException.class);

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("COMP-TC-005: no posture events → postureScore null")
    void complete_noPostureEvents_postureScoreNull() {
        when(sessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.IN_PROGRESS, USER_ID, 5)));
        when(exerciseRepository.findById(EXERCISE_ID))
                .thenReturn(Optional.of(exercise((short) 10)));
        when(postureFeedbackEventRepository.findByExerciseSessionId(SESSION_ID))
                .thenReturn(List.of());
        when(sessionRepository.save(any(ExerciseSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SessionResultResponse response = service.completeSession(SESSION_ID, USER_ID);

        assertThat(response.getPostureScore()).isNull();
    }

    @Test
    @DisplayName("COMP-TC-006: actual duration > planned → completionPercent capped at 100")
    void complete_overPlanned_completionPercentCapped() {
        // Started 30 minutes ago, planned duration only 5 minutes → > 100% before cap.
        when(sessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.IN_PROGRESS, USER_ID, 30)));
        when(exerciseRepository.findById(EXERCISE_ID))
                .thenReturn(Optional.of(exercise((short) 5)));
        when(postureFeedbackEventRepository.findByExerciseSessionId(SESSION_ID))
                .thenReturn(List.of(event("HIGH", "ROUNDED_BACK", new BigDecimal("0.80"))));
        when(sessionRepository.save(any(ExerciseSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SessionResultResponse response = service.completeSession(SESSION_ID, USER_ID);

        assertThat(response.getCompletionPercent()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.getPostureScore()).isEqualByComparingTo(new BigDecimal("0.80"));
    }
}

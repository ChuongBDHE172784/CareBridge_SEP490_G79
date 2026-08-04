package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.exercise.dto.StartSessionRequest;
import com.carebridge.backend.exercise.dto.StartSessionResponse;
import com.carebridge.backend.exercise.entity.ExerciseSafetyCheck;
import com.carebridge.backend.exercise.entity.ExerciseSession;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.entity.SafetyCheckStatus;
import com.carebridge.backend.exercise.entity.SessionStatus;
import com.carebridge.backend.exercise.exception.DuplicateSessionException;
import com.carebridge.backend.exercise.exception.ExerciseNotFoundException;
import com.carebridge.backend.exercise.exception.SafetyCheckNotClearedException;
import com.carebridge.backend.exercise.mapper.ExerciseSessionMapper;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.repository.ExerciseSafetyCheckRepository;
import com.carebridge.backend.exercise.repository.ExerciseSessionRepository;
import com.carebridge.backend.exercise.repository.PostureFeedbackEventRepository;
import com.carebridge.backend.exercise.service.ExerciseCareContextResolver;
import com.carebridge.backend.exercise.service.impl.ExerciseSessionServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ExerciseSessionServiceTest {

    private static final UUID EXERCISE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID SAFETY_CHECK_ID = UUID.randomUUID();

    @Mock
    private ExerciseSessionRepository sessionRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private ExerciseSafetyCheckRepository safetyCheckRepository;

    @Mock
    private PostureFeedbackEventRepository postureFeedbackEventRepository;

    @Mock
    private ExerciseCareContextResolver careContextResolver;

    @Spy
    private ExerciseSessionMapper sessionMapper;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private ExerciseSessionServiceImpl service;

    private PregnancyExercise publishedExercise() {
        PregnancyExercise exercise = new PregnancyExercise();
        exercise.setExerciseId(EXERCISE_ID);
        exercise.setStatus(ExerciseStatus.PUBLISHED);
        exercise.setSupportsPostureAnalysis(true);
        return exercise;
    }

    private ExerciseSafetyCheck clearedCheck(UUID owner, UUID exerciseId, SafetyCheckStatus status) {
        return ExerciseSafetyCheck.builder()
                .safetyCheckId(SAFETY_CHECK_ID)
                .exerciseId(exerciseId)
                .userId(owner)
                .resultStatus(status)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private StartSessionRequest request() {
        return StartSessionRequest.builder().safetyCheckId(SAFETY_CHECK_ID).build();
    }

    @Test
    @DisplayName("SESS-TC-001: published + cleared + no duplicate → IN_PROGRESS session")
    void startSession_valid_createsInProgressSession() {
        when(exerciseRepository.findByExerciseIdAndStatus(EXERCISE_ID, ExerciseStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedExercise()));
        when(safetyCheckRepository.findById(SAFETY_CHECK_ID))
                .thenReturn(Optional.of(clearedCheck(USER_ID, EXERCISE_ID, SafetyCheckStatus.CLEARED)));
        when(sessionRepository.findActiveSessionToday(eq(EXERCISE_ID), eq(USER_ID), anyList(), any()))
                .thenReturn(Optional.empty());
        when(careContextResolver.resolve(USER_ID, null))
                .thenReturn(new ExerciseCareContextResolver.CareContext(
                        UUID.randomUUID(), UUID.randomUUID()));
        when(sessionRepository.save(any(ExerciseSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StartSessionResponse response = service.startSession(EXERCISE_ID, request(), USER_ID);

        assertThat(response.getSessionStatus()).isEqualTo(SessionStatus.IN_PROGRESS.name());
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getExerciseId()).isEqualTo(EXERCISE_ID);
        assertThat(response.getStartedAt()).isNotNull();
        assertThat(response.getSupportsPostureAnalysis()).isTrue();
    }

    @Test
    @DisplayName("SESS-TC-002: exercise not published → ExerciseNotFoundException")
    void startSession_exerciseNotPublished_throws() {
        when(exerciseRepository.findByExerciseIdAndStatus(EXERCISE_ID, ExerciseStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startSession(EXERCISE_ID, request(), USER_ID))
                .isInstanceOf(ExerciseNotFoundException.class)
                .extracting("code").isEqualTo("EX-001");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("SESS-TC-003: safety check BLOCKED → SafetyCheckNotClearedException")
    void startSession_safetyCheckBlocked_throws() {
        when(exerciseRepository.findByExerciseIdAndStatus(EXERCISE_ID, ExerciseStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedExercise()));
        when(safetyCheckRepository.findById(SAFETY_CHECK_ID))
                .thenReturn(Optional.of(clearedCheck(USER_ID, EXERCISE_ID, SafetyCheckStatus.BLOCKED)));

        assertThatThrownBy(() -> service.startSession(EXERCISE_ID, request(), USER_ID))
                .isInstanceOf(SafetyCheckNotClearedException.class)
                .extracting("code").isEqualTo("EXSESS-003");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("SESS-TC-004: safety check user mismatch → SafetyCheckNotClearedException")
    void startSession_safetyCheckUserMismatch_throws() {
        when(exerciseRepository.findByExerciseIdAndStatus(EXERCISE_ID, ExerciseStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedExercise()));
        when(safetyCheckRepository.findById(SAFETY_CHECK_ID))
                .thenReturn(Optional.of(
                        clearedCheck(OTHER_USER_ID, EXERCISE_ID, SafetyCheckStatus.CLEARED)));

        assertThatThrownBy(() -> service.startSession(EXERCISE_ID, request(), USER_ID))
                .isInstanceOf(SafetyCheckNotClearedException.class);

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("SESS-TC-005: duplicate active session today → DuplicateSessionException")
    void startSession_duplicateActiveSession_throws() {
        when(exerciseRepository.findByExerciseIdAndStatus(EXERCISE_ID, ExerciseStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedExercise()));
        when(safetyCheckRepository.findById(SAFETY_CHECK_ID))
                .thenReturn(Optional.of(clearedCheck(USER_ID, EXERCISE_ID, SafetyCheckStatus.CLEARED)));
        when(sessionRepository.findActiveSessionToday(eq(EXERCISE_ID), eq(USER_ID), anyList(), any()))
                .thenReturn(Optional.of(new ExerciseSession()));

        assertThatThrownBy(() -> service.startSession(EXERCISE_ID, request(), USER_ID))
                .isInstanceOf(DuplicateSessionException.class)
                .extracting("code").isEqualTo("EXSESS-004");

        verify(sessionRepository, never()).save(any());
    }
}

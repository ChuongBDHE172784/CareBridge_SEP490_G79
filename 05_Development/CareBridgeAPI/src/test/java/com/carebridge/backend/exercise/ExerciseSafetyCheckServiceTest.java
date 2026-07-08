package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.SafetyCheckResponse;
import com.carebridge.backend.exercise.dto.SubmitSafetyCheckRequest;
import com.carebridge.backend.exercise.entity.ExerciseSafetyCheck;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.entity.SafetyCheckStatus;
import com.carebridge.backend.exercise.exception.ExerciseNotFoundException;
import com.carebridge.backend.exercise.exception.SafetyCheckNotFoundException;
import com.carebridge.backend.exercise.mapper.SafetyCheckMapper;
import com.carebridge.backend.exercise.policy.SafetyCheckPolicy;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.repository.ExerciseSafetyCheckRepository;
import com.carebridge.backend.exercise.service.impl.ExerciseSafetyCheckServiceImpl;
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
class ExerciseSafetyCheckServiceTest {

    private static final UUID EXERCISE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ExerciseSafetyCheckRepository safetyCheckRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Spy
    private SafetyCheckMapper safetyCheckMapper;

    @Spy
    private SafetyCheckPolicy safetyCheckPolicy;

    @InjectMocks
    private ExerciseSafetyCheckServiceImpl service;

    private PregnancyExercise publishedExercise() {
        PregnancyExercise exercise = new PregnancyExercise();
        exercise.setExerciseId(EXERCISE_ID);
        exercise.setStatus(ExerciseStatus.PUBLISHED);
        exercise.setSupportsPostureAnalysis(true);
        return exercise;
    }

    private SubmitSafetyCheckRequest request(boolean q1, boolean q2, boolean q3, boolean q4) {
        return SubmitSafetyCheckRequest.builder()
                .q1NoDizziness(q1)
                .q2NoContractions(q2)
                .q3NoBleeding(q3)
                .q4HydratedAndFed(q4)
                .build();
    }

    @Test
    @DisplayName("PSC-TC-007: exercise not PUBLISHED → ExerciseNotFoundException EX-001, no save")
    void submitSafetyCheck_exerciseNotPublished_throwsAndDoesNotSave() {
        when(exerciseRepository.findByExerciseIdAndStatus(EXERCISE_ID, ExerciseStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.submitSafetyCheck(EXERCISE_ID, request(true, true, true, true), USER_ID))
                .isInstanceOf(ExerciseNotFoundException.class)
                .extracting("code")
                .isEqualTo("EX-001");

        verify(safetyCheckRepository, never()).save(any());
    }

    @Test
    @DisplayName("PSC-TC-009: no latest safety check → SafetyCheckNotFoundException PSC-002")
    void getLatestSafetyCheck_empty_throwsNotFound() {
        when(safetyCheckRepository.findTopByExerciseIdAndUserIdOrderByCreatedAtDesc(
                EXERCISE_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatestSafetyCheck(EXERCISE_ID, USER_ID))
                .isInstanceOf(SafetyCheckNotFoundException.class)
                .extracting("code")
                .isEqualTo("PSC-002");
    }

    @Test
    @DisplayName("submitSafetyCheck: all true answers → CLEARED response")
    void submitSafetyCheck_allTrue_returnsClearedResponse() {
        when(exerciseRepository.findByExerciseIdAndStatus(EXERCISE_ID, ExerciseStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedExercise()));
        when(safetyCheckRepository.save(any(ExerciseSafetyCheck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<SafetyCheckResponse> response =
                service.submitSafetyCheck(EXERCISE_ID, request(true, true, true, true), USER_ID);

        SafetyCheckResponse data = response.getData();
        assertThat(data.getResultStatus()).isEqualTo(SafetyCheckStatus.CLEARED.name());
        assertThat(data.getRedFlagDetected()).isFalse();
        assertThat(data.getBlockedReason()).isNull();
        assertThat(data.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("submitSafetyCheck: Q1 false → BLOCKED response with reason")
    void submitSafetyCheck_q1False_returnsBlockedResponse() {
        when(exerciseRepository.findByExerciseIdAndStatus(EXERCISE_ID, ExerciseStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedExercise()));
        when(safetyCheckRepository.save(any(ExerciseSafetyCheck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<SafetyCheckResponse> response =
                service.submitSafetyCheck(EXERCISE_ID, request(false, true, true, true), USER_ID);

        SafetyCheckResponse data = response.getData();
        assertThat(data.getResultStatus()).isEqualTo(SafetyCheckStatus.BLOCKED.name());
        assertThat(data.getRedFlagDetected()).isTrue();
        assertThat(data.getCompletedAt()).isNull();
        assertThat(data.getBlockedReason()).contains("doctor").contains("midwife");
    }
}

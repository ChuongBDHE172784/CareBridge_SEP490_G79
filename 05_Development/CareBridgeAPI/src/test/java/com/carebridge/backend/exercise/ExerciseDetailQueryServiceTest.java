package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.exercise.dto.ExerciseDetailResponse;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.exception.ExerciseNotFoundException;
import com.carebridge.backend.exercise.mapper.ExerciseMapper;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.service.ExerciseDetailQueryServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExerciseDetailQueryServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @Spy
    private ExerciseMapper exerciseMapper;

    @InjectMocks
    private ExerciseDetailQueryServiceImpl service;

    // VPED-TC-001 — Happy path: PUBLISHED exercise returns full detail
    @Test
    @DisplayName("VPED-TC-001: PUBLISHED exercise returns full detail with instructionContent")
    void getExerciseDetail_published_returnsFullDetail() {
        PregnancyExercise entity = ExerciseDetailTestFactory.makePublishedExerciseWithFullDetail();

        when(exerciseRepository.findByExerciseIdAndStatus(
                ExerciseDetailTestFactory.UUID_PUBLISHED_1, ExerciseStatus.PUBLISHED))
                .thenReturn(Optional.of(entity));

        ApiResponse<ExerciseDetailResponse> response =
                service.getExerciseDetail(ExerciseDetailTestFactory.UUID_PUBLISHED_1);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();

        ExerciseDetailResponse data = response.getData();
        assertThat(data.getExerciseId()).isEqualTo(ExerciseDetailTestFactory.UUID_PUBLISHED_1);
        assertThat(data.getInstructionContent()).isNotNull().isNotEmpty();
        assertThat(data.getInstructionContent()).startsWith("Step 1:");
        assertThat(data.getSafetyWarning()).isEqualTo(
                "Stop immediately if you feel dizzy or experience pain.");
        assertThat(data.getVersionNo()).isEqualTo(1);
        assertThat(data.getCreatedAt()).isNotNull();
        assertThat(data.getSupportsPostureAnalysis()).isTrue();
        assertThat(data.getTrimesterScope()).isEqualTo("FIRST");
        assertThat(data.getDifficultyLevel()).isEqualTo("EASY");
    }

    // VPED-TC-002 — null safetyWarning maps to empty string
    @Test
    @DisplayName("VPED-TC-002: null safetyWarning in DB maps to empty string in response")
    void getExerciseDetail_nullSafetyWarning_mapsToEmptyString() {
        PregnancyExercise entity = ExerciseDetailTestFactory.makePublishedExerciseWithNullSafetyWarning();

        when(exerciseRepository.findByExerciseIdAndStatus(
                ExerciseDetailTestFactory.UUID_PUBLISHED_2, ExerciseStatus.PUBLISHED))
                .thenReturn(Optional.of(entity));

        ApiResponse<ExerciseDetailResponse> response =
                service.getExerciseDetail(ExerciseDetailTestFactory.UUID_PUBLISHED_2);

        assertThat(response.getData().getSafetyWarning())
                .isNotNull()
                .isEqualTo("");
    }

    // VPED-TC-003 — Non-existent exerciseId returns 404 EX-001
    @Test
    @DisplayName("VPED-TC-003: non-existent exerciseId throws ExerciseNotFoundException EX-001")
    void getExerciseDetail_nonExistent_throwsNotFoundException() {
        when(exerciseRepository.findByExerciseIdAndStatus(
                ExerciseDetailTestFactory.UUID_NOT_EXIST, ExerciseStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExerciseDetail(ExerciseDetailTestFactory.UUID_NOT_EXIST))
                .isInstanceOf(ExerciseNotFoundException.class)
                .hasMessageContaining("Exercise not found")
                .extracting("code")
                .isEqualTo("EX-001");
    }

    // VPED-TC-004 — DRAFT exercise returns 404 EX-001 (not 403) — CRITICAL
    @Test
    @DisplayName("VPED-TC-004: DRAFT exercise returns 404 EX-001 (not 403) — ADR-VPED-001")
    void getExerciseDetail_draftExercise_throwsNotFoundNotForbidden() {
        when(exerciseRepository.findByExerciseIdAndStatus(
                ExerciseDetailTestFactory.UUID_DRAFT, ExerciseStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExerciseDetail(ExerciseDetailTestFactory.UUID_DRAFT))
                .isInstanceOf(ExerciseNotFoundException.class)
                .extracting("code")
                .isEqualTo("EX-001");
    }

    // VPED-TC-005 — ARCHIVED exercise returns 404 EX-001 (not 403) — CRITICAL
    @Test
    @DisplayName("VPED-TC-005: ARCHIVED exercise returns 404 EX-001 (not 403) — ADR-VPED-001")
    void getExerciseDetail_archivedExercise_throwsNotFoundNotForbidden() {
        when(exerciseRepository.findByExerciseIdAndStatus(
                ExerciseDetailTestFactory.UUID_ARCHIVED, ExerciseStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExerciseDetail(ExerciseDetailTestFactory.UUID_ARCHIVED))
                .isInstanceOf(ExerciseNotFoundException.class)
                .extracting("code")
                .isEqualTo("EX-001");
    }
}

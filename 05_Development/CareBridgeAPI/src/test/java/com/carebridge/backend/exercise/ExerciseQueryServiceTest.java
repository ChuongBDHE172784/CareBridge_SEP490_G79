package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.response.PaginatedResponse;
import com.carebridge.backend.exercise.dto.ExerciseSummaryResponse;
import com.carebridge.backend.exercise.entity.DifficultyLevel;
import com.carebridge.backend.exercise.entity.ExerciseStatus;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.entity.TrimesterScope;
import com.carebridge.backend.exercise.mapper.ExerciseMapper;
import com.carebridge.backend.exercise.repository.ExerciseRepository;
import com.carebridge.backend.exercise.service.ExerciseQueryServiceImpl;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ExerciseQueryServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @Spy
    private ExerciseMapper exerciseMapper;

    @InjectMocks
    private ExerciseQueryServiceImpl service;

    // EX-TC-029-001 — List filtered by trimester=FIRST
    @Test
    @DisplayName("EX-TC-029-001: list filtered by trimester=FIRST returns matching exercises")
    void listPublishedExercises_filteredByTrimester_returnsMatching() {
        PregnancyExercise entity = ExerciseDetailTestFactory.makePublishedExerciseWithFullDetail();

        when(exerciseRepository.findPublishedByFilters(
                ExerciseStatus.PUBLISHED, TrimesterScope.FIRST, null, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        PaginatedResponse<ExerciseSummaryResponse> response =
                service.listPublishedExercises(TrimesterScope.FIRST, null, 0, 20);

        assertThat(response.getData()).hasSize(1);
        ExerciseSummaryResponse item = response.getData().get(0);
        assertThat(item.getExerciseId()).isEqualTo(ExerciseDetailTestFactory.UUID_PUBLISHED_1);
        assertThat(item.getTrimesterScope()).isEqualTo("FIRST");
        assertThat(item.getSafetyWarning()).isNotNull();
        assertThat(item.getSupportsPostureAnalysis()).isTrue();
    }

    // EX-TC-029-002 — Filter by difficulty=EASY
    @Test
    @DisplayName("EX-TC-029-002: filter by difficulty=EASY returns only easy exercises")
    void listPublishedExercises_filteredByDifficulty_returnsOnlyEasy() {
        PregnancyExercise entity = ExerciseDetailTestFactory.makePublishedExerciseWithFullDetail();

        when(exerciseRepository.findPublishedByFilters(
                ExerciseStatus.PUBLISHED, null, DifficultyLevel.EASY, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        PaginatedResponse<ExerciseSummaryResponse> response =
                service.listPublishedExercises(null, DifficultyLevel.EASY, 0, 20);

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getDifficultyLevel()).isEqualTo("EASY");
    }

    // EX-TC-029-003 — No filters returns all PUBLISHED
    @Test
    @DisplayName("EX-TC-029-003: no filters returns all PUBLISHED exercises")
    void listPublishedExercises_noFilters_returnsAllPublished() {
        PregnancyExercise e1 = ExerciseDetailTestFactory.makePublishedExerciseWithFullDetail();
        PregnancyExercise e2 = ExerciseDetailTestFactory.makePublishedExerciseWithNullSafetyWarning();
        e2.setTrimesterScope(TrimesterScope.SECOND);
        e2.setDifficultyLevel(DifficultyLevel.MEDIUM);

        when(exerciseRepository.findPublishedByFilters(
                ExerciseStatus.PUBLISHED, null, null, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(e1, e2)));

        PaginatedResponse<ExerciseSummaryResponse> response =
                service.listPublishedExercises(null, null, 0, 20);

        assertThat(response.getData()).hasSize(2);
    }

    // EX-TC-029-004 — DRAFT not visible (CRITICAL)
    @Test
    @DisplayName("EX-TC-029-004: DRAFT exercises filtered at query level — not in list")
    void listPublishedExercises_draftFilteredByQuery_notInResult() {
        PregnancyExercise published = ExerciseDetailTestFactory.makePublishedExerciseWithFullDetail();

        when(exerciseRepository.findPublishedByFilters(
                ExerciseStatus.PUBLISHED, null, null, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(published)));

        PaginatedResponse<ExerciseSummaryResponse> response =
                service.listPublishedExercises(null, null, 0, 20);

        assertThat(response.getData()).hasSize(1);
        response.getData().forEach(item ->
                assertThat(item.getTitle()).doesNotContain("Draft"));
    }
}

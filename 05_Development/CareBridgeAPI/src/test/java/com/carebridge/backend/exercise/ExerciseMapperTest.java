package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.exercise.dto.ExerciseDetailResponse;
import com.carebridge.backend.exercise.dto.ExerciseSummaryResponse;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.mapper.ExerciseMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExerciseMapperTest {

    private final ExerciseMapper mapper = new ExerciseMapper();

    // VPED-TC-002 (mapper-level): null safetyWarning → empty string
    @Test
    @DisplayName("VPED-TC-002-mapper: null safetyWarning in entity maps to empty string in DTO")
    void toDetailResponse_nullSafetyWarning_mapsToEmptyString() {
        PregnancyExercise entity = ExerciseDetailTestFactory.makePublishedExerciseWithNullSafetyWarning();

        ExerciseDetailResponse result = mapper.toDetailResponse(entity);

        assertThat(result.getSafetyWarning())
                .isNotNull()
                .isEqualTo("");
    }

    @Test
    @DisplayName("VPED-TC-002-mapper: populated safetyWarning maps correctly")
    void toDetailResponse_populatedSafetyWarning_mapsCorrectly() {
        PregnancyExercise entity = ExerciseDetailTestFactory.makePublishedExerciseWithFullDetail();

        ExerciseDetailResponse result = mapper.toDetailResponse(entity);

        assertThat(result.getSafetyWarning())
                .isEqualTo("Stop immediately if you feel dizzy or experience pain.");
    }

    // EX-TC-029-006 — null safetyWarning → "" in toSummaryResponse
    @Test
    @DisplayName("EX-TC-029-006: null safetyWarning maps to empty string in toSummaryResponse")
    void toSummaryResponse_nullSafetyWarning_mapsToEmptyString() {
        PregnancyExercise entity = ExerciseDetailTestFactory.makePublishedExerciseWithNullSafetyWarning();

        ExerciseSummaryResponse result = mapper.toSummaryResponse(entity);

        assertThat(result.getSafetyWarning()).isNotNull().isEqualTo("");
        assertThat(result.getExerciseId()).isEqualTo(ExerciseDetailTestFactory.UUID_PUBLISHED_2);
        assertThat(result.getTitle()).isNotNull();
    }

    @Test
    @DisplayName("toDetailResponse maps all fields correctly including instructionContent")
    void toDetailResponse_allFields_mappedCorrectly() {
        PregnancyExercise entity = ExerciseDetailTestFactory.makePublishedExerciseWithFullDetail();

        ExerciseDetailResponse result = mapper.toDetailResponse(entity);

        assertThat(result.getExerciseId()).isEqualTo(ExerciseDetailTestFactory.UUID_PUBLISHED_1);
        assertThat(result.getTitle()).isEqualTo("Prenatal Yoga - First Trimester");
        assertThat(result.getInstructionContent()).isNotNull().startsWith("Step 1:");
        assertThat(result.getTrimesterScope()).isEqualTo("FIRST");
        assertThat(result.getDifficultyLevel()).isEqualTo("EASY");
        assertThat(result.getDurationMinutes()).isEqualTo((short) 20);
        assertThat(result.getVersionNo()).isEqualTo(1);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getSupportsPostureAnalysis()).isTrue();
    }
}

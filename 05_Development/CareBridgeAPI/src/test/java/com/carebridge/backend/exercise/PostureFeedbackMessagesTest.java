package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.exercise.service.PostureFeedbackMessages;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class PostureFeedbackMessagesTest {

    /** Every feedback code the Exercise-Correction sidecar can emit. */
    private static Stream<String> sidecarFeedbackCodes() {
        return Stream.of(
                "low_back", "high_back", "lean_back", "knee_over_toe",
                "lunge_phase_not_evaluated", "low_confidence", "correct");
    }

    /** Every classifier label the pinned models can return, per exercise. */
    private static Stream<org.junit.jupiter.params.provider.Arguments> correctModelClasses() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("plank", "C"),
                org.junit.jupiter.params.provider.Arguments.of("bicep_curl", "C"),
                org.junit.jupiter.params.provider.Arguments.of("squat", "up"),
                org.junit.jupiter.params.provider.Arguments.of("squat", "down"),
                org.junit.jupiter.params.provider.Arguments.of("lunge", "C"));
    }

    @ParameterizedTest
    @MethodSource("sidecarFeedbackCodes")
    @DisplayName("Every sidecar feedback code resolves to Vietnamese copy")
    void everySidecarCodeIsTranslated(String code) {
        String message = PostureFeedbackMessages.forModelFeedback(code);

        assertThat(message).isNotEqualTo(PostureFeedbackMessages.UNRECOGNIZED_DETAIL);
        assertThat(message).isNotBlank().doesNotContain(code);
        assertThat(isVietnamese(message)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("correctModelClasses")
    @DisplayName("Every correct classifier label resolves to Vietnamese copy")
    void everyCorrectClassIsTranslated(String exerciseKey, String predictedClass) {
        String message = PostureFeedbackMessages.forModelClass(exerciseKey, predictedClass);

        assertThat(message).isNotBlank();
        assertThat(isVietnamese(message)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "GOOD_FORM", "MILD_ROUNDING", "ROUND_BACK", "UNKNOWN",
        "MODEL_LOW_CONFIDENCE", "MODEL_UNAVAILABLE"
    })
    @DisplayName("Every backend posture code resolves to Vietnamese copy and a short label")
    void everyPostureCodeIsTranslated(String postureCode) {
        assertThat(PostureFeedbackMessages.forPostureCode(postureCode))
                .isNotEqualTo(PostureFeedbackMessages.UNRECOGNIZED_DETAIL)
                .doesNotContain(postureCode);
        assertThat(PostureFeedbackMessages.shortLabel(postureCode))
                .isNotBlank()
                .doesNotContain(postureCode);
    }

    @ParameterizedTest
    @CsvSource({
        "LOOSE_UPPER_ARM", "FOOT_PLACEMENT_TOO_TIGHT", "FOOT_PLACEMENT_TOO_WIDE",
        "KNEE_PLACEMENT_TOO_TIGHT", "KNEE_PLACEMENT_TOO_WIDE", "KNEE_ANGLE_OUT_OF_RANGE"
    })
    @DisplayName("Every geometric rule finding resolves to Vietnamese copy")
    void everyRuleFindingIsTranslated(String code) {
        String message = PostureFeedbackMessages.forRuleFinding(code);

        assertThat(message)
                .isNotEqualTo(PostureFeedbackMessages.UNRECOGNIZED_DETAIL)
                .doesNotContain(code);
        assertThat(isVietnamese(message)).isTrue();
    }

    @Test
    @DisplayName("Unknown codes fall back to safe copy instead of leaking the identifier")
    void unknownCodesNeverLeak() {
        List<String> leaky = List.of("KNEE_TOO_WIDE", "C", "up", "some_new_model_code", "");

        for (String code : leaky) {
            assertThat(PostureFeedbackMessages.forModelFeedback(code))
                    .isEqualTo(PostureFeedbackMessages.UNRECOGNIZED_DETAIL);
            assertThat(PostureFeedbackMessages.forPostureCode(code))
                    .isEqualTo(PostureFeedbackMessages.UNRECOGNIZED_DETAIL);
            assertThat(PostureFeedbackMessages.shortLabel(code)).isEqualTo("Chưa đánh giá được");
        }
        assertThat(PostureFeedbackMessages.forModelFeedback(null))
                .isEqualTo(PostureFeedbackMessages.UNRECOGNIZED_DETAIL);
        assertThat(PostureFeedbackMessages.forPostureCode(null))
                .isEqualTo(PostureFeedbackMessages.UNRECOGNIZED_DETAIL);
        assertThat(PostureFeedbackMessages.shortLabel(null)).isEqualTo("Chưa đánh giá được");
    }

    @Test
    @DisplayName("An unmapped correct label falls back to generic praise, not the label")
    void unmappedCorrectClassFallsBackToGenericPraise() {
        String message = PostureFeedbackMessages.forModelClass("squat", "sideways");

        assertThat(message).isEqualTo("Tư thế đúng, giữ nguyên nhịp này.");
        assertThat(PostureFeedbackMessages.forModelClass(null, null)).isEqualTo(message);
    }

    @Test
    @DisplayName("Composite degraded codes resolve to the underlying rule label")
    void compositeDegradedCodeResolvesToRuleLabel() {
        assertThat(PostureFeedbackMessages.shortLabel("MODEL_UNAVAILABLE_RULE_FALLBACK_ROUND_BACK"))
                .isEqualTo("Lưng cong nhiều");
        assertThat(PostureFeedbackMessages.shortLabel("MODEL_LOW_CONFIDENCE_RULE_FALLBACK_UNKNOWN"))
                .isEqualTo("Chưa đủ dữ liệu");
    }

    @Test
    @DisplayName("Degraded copy prefixes the rule sentence in Vietnamese")
    void degradedCopyIsVietnamese() {
        assertThat(PostureFeedbackMessages.degraded("Tư thế tốt! Giữ lưng thẳng."))
                .isEqualTo("Đang dùng phân tích dự phòng: Tư thế tốt! Giữ lưng thẳng.");
        assertThat(PostureFeedbackMessages.degraded(null))
                .isEqualTo("Đang dùng phân tích dự phòng: "
                        + PostureFeedbackMessages.UNRECOGNIZED_DETAIL);
    }

    /** Cheap guard that the copy is real Vietnamese rather than leftover English. */
    private boolean isVietnamese(String message) {
        return message.chars().anyMatch(ch -> "ăâđêôơưàảãáạèéẻìíịòóỏùúủýỹ".indexOf(
                Character.toLowerCase(ch)) >= 0);
    }
}

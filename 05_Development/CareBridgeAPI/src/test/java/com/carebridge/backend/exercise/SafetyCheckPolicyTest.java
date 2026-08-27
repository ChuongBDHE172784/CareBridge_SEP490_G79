package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.exercise.entity.SafetyQuestion;
import com.carebridge.backend.exercise.policy.EvaluationResult;
import com.carebridge.backend.exercise.policy.SafetyCheckPolicy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SafetyCheckPolicyTest {

    private final SafetyCheckPolicy policy = new SafetyCheckPolicy();

    private Map<SafetyQuestion, Boolean> allTrue() {
        Map<SafetyQuestion, Boolean> answers = new EnumMap<>(SafetyQuestion.class);
        for (SafetyQuestion q : SafetyQuestion.values()) {
            answers.put(q, true);
        }
        return answers;
    }

    @Test
    @DisplayName("PSC-TC-001: all true answers → cleared, no flagged questions")
    void evaluate_allTrue_cleared() {
        EvaluationResult result = policy.evaluate(allTrue());

        assertThat(result.isCleared()).isTrue();
        assertThat(result.getFlaggedQuestions()).isEmpty();
    }

    @Test
    @DisplayName("PSC-TC-002: Q1 false → not cleared, Q1 flagged")
    void evaluate_q1False_blocked() {
        Map<SafetyQuestion, Boolean> answers = allTrue();
        answers.put(SafetyQuestion.Q1_NO_DIZZINESS, false);

        EvaluationResult result = policy.evaluate(answers);

        assertThat(result.isCleared()).isFalse();
        assertThat(result.getFlaggedQuestions())
                .containsExactly(SafetyQuestion.Q1_NO_DIZZINESS);
    }

    @ParameterizedTest
    @EnumSource(value = SafetyQuestion.class,
            names = {"Q2_NO_CONTRACTIONS", "Q3_NO_BLEEDING", "Q4_HYDRATED_AND_FED"})
    @DisplayName("PSC-TC-003: each of Q2/Q3/Q4 false independently blocks")
    void evaluate_singleQuestionFalse_blocked(SafetyQuestion question) {
        Map<SafetyQuestion, Boolean> answers = allTrue();
        answers.put(question, false);

        EvaluationResult result = policy.evaluate(answers);

        assertThat(result.isCleared()).isFalse();
        assertThat(result.getFlaggedQuestions()).containsExactly(question);
    }

    @Test
    @DisplayName("PSC-TC-004: all false → all four questions flagged")
    void evaluate_allFalse_allFlagged() {
        Map<SafetyQuestion, Boolean> answers = new EnumMap<>(SafetyQuestion.class);
        for (SafetyQuestion q : SafetyQuestion.values()) {
            answers.put(q, false);
        }

        EvaluationResult result = policy.evaluate(answers);

        assertThat(result.isCleared()).isFalse();
        assertThat(result.getFlaggedQuestions()).hasSize(4);
    }

    @Test
    @DisplayName("PSC-TC-005: Q1 blocked reason mentions doctor AND midwife")
    void buildBlockedReason_q1_mentionsDoctorAndMidwife() {
        String reason = policy.buildBlockedReason(List.of(SafetyQuestion.Q1_NO_DIZZINESS));

        assertThat(reason).contains("doctor");
        assertThat(reason).contains("midwife");
    }

    @ParameterizedTest
    @EnumSource(SafetyQuestion.class)
    @DisplayName("PSC-TC-006: blocked reason for each question is non-diagnostic")
    void buildBlockedReason_isNonDiagnostic(SafetyQuestion question) {
        String reason = policy.buildBlockedReason(List.of(question)).toLowerCase();

        assertThat(reason).doesNotContain("you have");
        assertThat(reason).doesNotContain("you are suffering");
        assertThat(reason).doesNotContain("diagnos");
        assertThat(reason).doesNotContain("prescri");
        // And must still direct the user to professionals.
        assertThat(reason).contains("doctor");
        assertThat(reason).contains("midwife");
    }

    @Test
    @DisplayName("buildBlockedReason: multiple flags concatenated with space separator")
    void buildBlockedReason_multipleFlags_concatenated() {
        String reason = policy.buildBlockedReason(
                List.of(SafetyQuestion.Q1_NO_DIZZINESS, SafetyQuestion.Q3_NO_BLEEDING));

        assertThat(reason).contains("Dizziness");
        assertThat(reason).contains("Bleeding");
    }
}

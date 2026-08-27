package com.carebridge.backend.reminder;

import com.carebridge.backend.reminder.policy.TriageFollowUpTitlePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * CB-TYFU-TDD-001 — TYFU-TC-05 / TYFU-TC-06.
 * Oracle: ADR-TYFU-006 priority table (fixed Vietnamese titles, first-match-wins);
 * BR-TYFU-004 (never raw text, never null); DDL title varchar(255).
 */
class TriageFollowUpTitlePolicyTest {

    private static final String FEVER_TITLE     = "Kiểm tra lại thân nhiệt của bé";
    private static final String VOMIT_TITLE     = "Kiểm tra lại tình trạng nôn trớ của bé";
    private static final String HYDRATION_TITLE = "Kiểm tra lại tình trạng đi ngoài và dấu hiệu mất nước của bé";
    private static final String BREATHING_TITLE = "Kiểm tra lại tình trạng ho và nhịp thở của bé";
    private static final String GENERIC_TITLE   = "Theo dõi lại tình trạng sức khỏe của bé sau sàng lọc AI";

    // ── TYFU-TC-05 — decision table rows 1–4 + priority (ADR-TYFU-006) ──────────

    @ParameterizedTest
    @CsvSource({
            "fever,           " + FEVER_TITLE,      // row 1
            "high_fever,      " + FEVER_TITLE,      // row 1
            "vomiting,        " + VOMIT_TITLE,      // row 2
            "cough,           " + BREATHING_TITLE,  // row 4
    })
    void tyfuTc05_singleCanonicalCode_mapsToFixedTitle(String code, String expectedTitle) {
        TriageFollowUpTitlePolicy policy = new TriageFollowUpTitlePolicy();

        assertThat(policy.deriveTitle(List.of(code))).isEqualTo(expectedTitle);
    }

    @Test
    void tyfuTc05_hydrationFamily_mapsToHydrationTitle() {
        TriageFollowUpTitlePolicy policy = new TriageFollowUpTitlePolicy();

        assertThat(policy.deriveTitle(List.of("diarrhea", "mild_dehydration")))
                .isEqualTo(HYDRATION_TITLE); // row 3
    }

    @Test
    void tyfuTc05_priorityOrder_feverWinsOverCough_regardlessOfInputOrder() {
        TriageFollowUpTitlePolicy policy = new TriageFollowUpTitlePolicy();

        // Fixed priority order of the policy, NOT the caller's list order (ADR-TYFU-006)
        assertThat(policy.deriveTitle(List.of("cough", "fever"))).isEqualTo(FEVER_TITLE);
    }

    @Test
    void tyfuTc05_allTitlesFitDdlColumn_titleVarchar255() {
        TriageFollowUpTitlePolicy policy = new TriageFollowUpTitlePolicy();

        for (List<String> input : Arrays.asList(
                List.of("fever"), List.of("vomiting"), List.of("diarrhea"),
                List.of("cough"), List.<String>of())) {
            assertThat(policy.deriveTitle(input)).isNotNull().hasSizeLessThanOrEqualTo(255);
        }
    }

    // ── TYFU-TC-06 — fallback: null / empty / unmapped (BR-TYFU-004) ────────────

    @Test
    void tyfuTc06_nullInput_returnsGenericFallback_withoutThrowing() {
        TriageFollowUpTitlePolicy policy = new TriageFollowUpTitlePolicy();

        assertThatCode(() -> policy.deriveTitle(null)).doesNotThrowAnyException();
        assertThat(policy.deriveTitle(null)).isEqualTo(GENERIC_TITLE);
    }

    @Test
    void tyfuTc06_emptyInput_returnsGenericFallback() {
        TriageFollowUpTitlePolicy policy = new TriageFollowUpTitlePolicy();

        assertThat(policy.deriveTitle(List.of())).isEqualTo(GENERIC_TITLE);
    }

    @Test
    void tyfuTc06_unmappedCanonicalCodes_returnGenericFallback_neverEchoInput() {
        TriageFollowUpTitlePolicy policy = new TriageFollowUpTitlePolicy();

        String title = policy.deriveTitle(List.of("rash", "cyanosis"));

        assertThat(title).isEqualTo(GENERIC_TITLE);
        assertThat(title).doesNotContain("rash").doesNotContain("cyanosis");
    }
}

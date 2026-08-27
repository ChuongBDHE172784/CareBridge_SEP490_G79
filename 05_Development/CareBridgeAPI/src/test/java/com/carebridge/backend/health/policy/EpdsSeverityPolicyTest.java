package com.carebridge.backend.health.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.health.policy.EpdsSeverityPolicy.Band;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * CB-EPDS-TEST-001 — TC-03..TC-08.
 *
 * <p>Band thresholds mirror the mobile oracle {@code epds_screen.dart:17-21}.
 */
class EpdsSeverityPolicyTest {

    /** Wording that must never reach a family-facing message (BR-SAFETY-EPDS-001). */
    private static final List<String> FORBIDDEN = List.of(
            "tự hại", "tự sát", "câu 10", "question 10", "self-harm", "suicid");

    /** The exact Dart guidance strings — none may ever be transmitted (TDS ADR-003). */
    private static final List<String> DART_GUIDANCE = List.of(
            "Cần đánh giá sức khỏe tâm thần ngay và hỗ trợ khẩn nếu có ý nghĩ tự sát.",
            "Nên sắp xếp gặp bác sĩ hoặc chuyên gia tâm lý để được đánh giá chuyên sâu.",
            "Theo dõi sát và thực hiện lại EPDS sau 2–4 tuần.",
            "Tiếp tục theo dõi và thực hiện lại theo lịch thai kỳ hoặc sau sinh.");

    // ---------------------------------------------------------------- TC-03
    @Test
    void score13IsAssessBand() {
        assertThat(EpdsSeverityPolicy.band(13)).isEqualTo(Band.ASSESS);
        assertThat(EpdsSeverityPolicy.band(12)).isNotEqualTo(Band.ASSESS);
        assertThat(EpdsSeverityPolicy.bandLabel(Band.ASSESS)).isEqualTo("Cần được đánh giá chuyên sâu");

        String body = EpdsSeverityPolicy.familyBody(13, 0);
        assertThat(body).contains("Điểm 13/30").contains("Cần được đánh giá chuyên sâu");
    }

    // ---------------------------------------------------------------- TC-04
    @Test
    void score10IsMonitorBandAnd9IsLow() {
        assertThat(EpdsSeverityPolicy.band(10)).isEqualTo(Band.MONITOR);
        assertThat(EpdsSeverityPolicy.band(12)).isEqualTo(Band.MONITOR);
        assertThat(EpdsSeverityPolicy.band(9)).isEqualTo(Band.LOW);
        assertThat(EpdsSeverityPolicy.bandLabel(Band.MONITOR)).isEqualTo("Cần theo dõi và sàng lọc lại");
    }

    // ---------------------------------------------------------------- TC-05
    @Test
    void lowBandCoversZeroAndNine() {
        assertThat(EpdsSeverityPolicy.band(0)).isEqualTo(Band.LOW);
        assertThat(EpdsSeverityPolicy.band(9)).isEqualTo(Band.LOW);
        assertThat(EpdsSeverityPolicy.bandLabel(Band.LOW)).isEqualTo("Nguy cơ hiện tại thấp");
    }

    // ---------------------------------------------------------------- TC-06
    @Test
    void escalationMessageDisclosesNeitherSelfHarmNorAnyScore() {
        String title = EpdsSeverityPolicy.familyTitle(8, 3);
        String body = EpdsSeverityPolicy.familyBody(8, 3);

        // Guard against a vacuous pass against an empty message (AP-01).
        assertThat(title).isNotBlank();
        assertThat(body).isNotBlank();
        assertThat(title).isEqualTo("Kết quả sàng lọc EPDS cần được quan tâm");

        String combined = (title + " " + body).toLowerCase(Locale.ROOT);
        for (String forbidden : FORBIDDEN) {
            assertThat(combined)
                    .as("family escalation message must not contain '%s'", forbidden)
                    .doesNotContain(forbidden.toLowerCase(Locale.ROOT));
        }
        // Neither the Q10 sub-score nor the total may appear (TDS §5.3 hard constraint).
        assertThat(combined).doesNotContain("3").doesNotContain("8");
    }

    // ---------------------------------------------------------------- TC-07
    @Test
    void dartGuidanceTextNeverAppearsInAnyFamilyMessage() {
        for (int total : new int[] {0, 9, 10, 12, 13, 30}) {
            for (int q10 : new int[] {0, 1, 2, 3}) {
                String title = EpdsSeverityPolicy.familyTitle(total, q10);
                String body = EpdsSeverityPolicy.familyBody(total, q10);
                assertThat(title).isNotBlank();
                assertThat(body).isNotBlank();

                String combined = title + " " + body;
                for (String guidance : DART_GUIDANCE) {
                    assertThat(combined)
                            .as("total=%d q10=%d must not carry Dart guidance", total, q10)
                            .doesNotContain(guidance);
                }
            }
        }
    }

    // ---------------------------------------------------------------- TC-08
    @Test
    void positiveQuestion10EscalatesEvenWhenTotalIsLow() {
        assertThat(EpdsSeverityPolicy.requiresEscalation(1)).isTrue();
        assertThat(EpdsSeverityPolicy.requiresEscalation(0)).isFalse();

        // Total 8 sits in the LOW band; escalation must still override it.
        String escalatedTitle = EpdsSeverityPolicy.familyTitle(8, 1);
        String normalTitle = EpdsSeverityPolicy.familyTitle(8, 0);
        assertThat(escalatedTitle).isNotEqualTo(normalTitle);
        assertThat(escalatedTitle).isEqualTo("Kết quả sàng lọc EPDS cần được quan tâm");

        String escalatedBody = EpdsSeverityPolicy.familyBody(8, 1);
        assertThat(escalatedBody).isNotEqualTo(EpdsSeverityPolicy.familyBody(8, 0));
        // The reassuring low-risk label must not be shown when Q10 is positive.
        assertThat(escalatedBody).doesNotContain("Nguy cơ hiện tại thấp");
    }

    @Test
    void normalMessageCarriesScoreAndBandAndDisclaimer() {
        String title = EpdsSeverityPolicy.familyTitle(8, 0);
        String body = EpdsSeverityPolicy.familyBody(8, 0);

        assertThat(title).isEqualTo("Kết quả sàng lọc EPDS");
        assertThat(body)
                .contains("Điểm 8/30")
                .contains("Nguy cơ hiện tại thấp")
                .contains("Đây là dữ liệu theo dõi, không phải chẩn đoán y khoa.");
    }
}

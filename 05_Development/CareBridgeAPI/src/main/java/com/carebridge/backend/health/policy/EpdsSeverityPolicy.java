package com.carebridge.backend.health.policy;

/**
 * EPDS severity banding and family-facing message assembly (CB-EPDS-IMP-001).
 *
 * <p>Thresholds mirror the mobile oracle {@code epds_screen.dart:17-21}. Any change here requires
 * the same change there — see TDS ADR-002 and §14.4.
 *
 * <p><strong>Privacy contract (BR-SAFETY-EPDS-001 / TDS ADR-003).</strong> Question 10 is the
 * self-harm item. It is accepted here only as a boolean escalation predicate and must never appear
 * — as a value, a label, or a paraphrase — in any string returned by this class. Neither may the
 * mobile {@code epdsGuidance()} text, which branches on Question 10 and names suicidal ideation.
 * The escalation branch additionally omits the numeric total, so a recipient cannot pair a low
 * total with an escalation and thereby infer the Question-10 answer.
 */
public final class EpdsSeverityPolicy {

    private static final int ASSESS_THRESHOLD = 13;
    private static final int MONITOR_THRESHOLD = 10;
    private static final int MAX_SCORE = 30;

    /** Reuses the wording of {@code HealthMetricServiceImpl.DISCLAIMER}. */
    private static final String DISCLAIMER =
            "Đây là dữ liệu theo dõi, không phải chẩn đoán y khoa.";

    private static final String NORMAL_TITLE = "Kết quả sàng lọc EPDS";
    private static final String ESCALATION_TITLE = "Kết quả sàng lọc EPDS cần được quan tâm";

    private EpdsSeverityPolicy() {
    }

    public enum Band {
        LOW, MONITOR, ASSESS
    }

    public static Band band(int totalScore) {
        if (totalScore >= ASSESS_THRESHOLD) {
            return Band.ASSESS;
        }
        if (totalScore >= MONITOR_THRESHOLD) {
            return Band.MONITOR;
        }
        return Band.LOW;
    }

    public static String bandLabel(Band band) {
        return switch (band) {
            case ASSESS -> "Cần được đánh giá chuyên sâu";
            case MONITOR -> "Cần theo dõi và sàng lọc lại";
            case LOW -> "Nguy cơ hiện tại thấp";
        };
    }

    /**
     * @param question10Score the self-harm item score; used as a predicate only, never rendered
     */
    public static boolean requiresEscalation(int question10Score) {
        return question10Score > 0;
    }

    public static String familyTitle(int totalScore, int question10Score) {
        return requiresEscalation(question10Score) ? ESCALATION_TITLE : NORMAL_TITLE;
    }

    public static String familyBody(int totalScore, int question10Score) {
        if (requiresEscalation(question10Score)) {
            // No score, no band, no item name — escalation without disclosure (ADR-003).
            return "Mẹ vừa hoàn thành sàng lọc tâm trạng EPDS. "
                    + "Kết quả cần được quan tâm ngay — hãy liên hệ và ở bên mẹ. "
                    + DISCLAIMER;
        }
        return "Mẹ vừa hoàn thành sàng lọc tâm trạng EPDS. "
                + "Điểm " + totalScore + "/" + MAX_SCORE + " — " + bandLabel(band(totalScore)) + ". "
                + DISCLAIMER;
    }
}

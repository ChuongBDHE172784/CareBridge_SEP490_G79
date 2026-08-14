package com.carebridge.backend.exercise.service;

import java.util.Map;

/**
 * Vietnamese user-facing copy for posture feedback.
 *
 * <p>The Exercise-Correction sidecar is language-neutral: it emits stable machine
 * codes ({@code low_back}, {@code knee_over_toe}, …) plus raw classifier labels
 * ({@code C}, {@code L}, {@code up}, {@code down}) with English developer text.
 * None of that is presentable — before this catalog a correctly-held pose showed
 * the literal letter "C" in the app. Every string a mother can read is resolved
 * here, keyed off those stable codes.
 *
 * <p>Machine-readable {@code postureCode} values are deliberately left untouched:
 * they are persisted for analytics and drive the client's stage detection.
 *
 * <p>Copy is guidance only and never diagnostic, per the exercise-feature safety
 * rule that AI output must not diagnose or delay clinical routing.
 */
public final class PostureFeedbackMessages {

    /** Shown when a code is unrecognized, so a raw model label never reaches the UI. */
    public static final String UNRECOGNIZED_DETAIL =
            "Chưa đánh giá được tư thế trong khung hình này.";

    private static final String UNRECOGNIZED_LABEL = "Chưa đánh giá được";

    private static final String DEGRADED_PREFIX = "";

    private static final String RULE_FALLBACK_MARKER = "_RULE_FALLBACK_";

    /** Feedback codes emitted by the sidecar when a pose is classified as incorrect. */
    private static final Map<String, String> MODEL_FEEDBACK = Map.of(
            "low_back", "Lưng dưới đang võng xuống — nâng nhẹ hông lên.",
            "high_back", "Hông đang nâng quá cao — hạ nhẹ hông xuống.",
            "lean_back", "Thân người đang ngả ra sau — đứng thẳng lại.",
            "knee_over_toe", "Gối trước đang vượt quá mũi chân — dồn trọng tâm về sau.",
            "lunge_phase_not_evaluated", "Chỉ đánh giá tư thế ở nhịp hạ người.",
            "low_confidence", "Chưa nhận rõ tư thế trong khung hình này.",
            "correct", "Tư thế đúng, giữ nguyên nhịp này.");

    /**
     * Correct-form classifier labels, keyed {@code exerciseKey/predictedClass}. The
     * sidecar sends no feedback item when a pose is correct, so without this map the
     * only text available would be the raw label.
     */
    private static final Map<String, String> MODEL_CLASSES = Map.of(
            "plank/C", "Giữ tốt — thân người đang thẳng.",
            "bicep_curl/C", "Tư thế đứng tốt — giữ khuỷu tay sát thân.",
            "squat/up", "Nhịp đứng lên — giữ lưng thẳng.",
            "squat/down", "Nhịp hạ người — giữ gối theo hướng mũi chân.",
            "lunge/C", "Gối trước đang đúng vị trí — giữ nhịp này.");

    /**
     * Findings from {@link com.carebridge.backend.exercise.policy.GeometricPostureRules},
     * the rule half of the hybrid approach. These run alongside the model rather than
     * instead of it, so their copy must read as coaching, not as a failure.
     */
    private static final Map<String, String> RULE_FINDINGS = Map.of(
            "LOOSE_UPPER_ARM", "Giữ khuỷu tay sát thân — cánh tay trên đang đưa ra xa.",
            "FOOT_PLACEMENT_TOO_TIGHT", "Hai bàn chân đang quá hẹp — mở rộng gần bằng vai.",
            "FOOT_PLACEMENT_TOO_WIDE", "Hai bàn chân đang quá rộng — thu hẹp lại gần bằng vai.",
            "KNEE_PLACEMENT_TOO_TIGHT", "Hai gối đang khép vào trong — mở gối theo hướng mũi chân.",
            "KNEE_PLACEMENT_TOO_WIDE", "Hai gối đang mở quá rộng — thu gối về theo hướng mũi chân.",
            "KNEE_ANGLE_OUT_OF_RANGE", "Góc gối chưa ở khoảng phù hợp — chỉnh lại độ sâu của nhịp.",
            "WEAK_PEAK_CONTRACTION", "Nhịp vừa rồi chưa cuốn đủ cao — gập tay sâu hơn ở điểm trên.");

    /** Posture codes produced by this backend: the rule engine and degraded states. */
    private static final Map<String, String> POSTURE_CODES = Map.of(
            "GOOD_FORM", "Tư thế tốt! Giữ lưng thẳng.",
            "MILD_ROUNDING", "Hãy duỗi lưng thẳng thêm một chút.",
            "ROUND_BACK", "Dừng lại và chỉnh tư thế — lưng đang cong nhiều.",
            "UNKNOWN", "Chưa đủ dữ liệu điểm mốc để đánh giá tư thế.",
            "MODEL_LOW_CONFIDENCE", "Chưa nhận rõ tư thế trong khung hình này.",
            "MODEL_UNAVAILABLE", "Phân tích tư thế đang tạm gián đoạn. Vui lòng thử lại.");

    /** Short labels for the BASIC feedback level, which previously echoed the raw code. */
    private static final Map<String, String> SHORT_LABELS = Map.of(
            "GOOD_FORM", "Tư thế tốt",
            "MILD_ROUNDING", "Lưng hơi cong",
            "ROUND_BACK", "Lưng cong nhiều",
            "UNKNOWN", "Chưa đủ dữ liệu",
            "MODEL_LOW_CONFIDENCE", "Chưa nhận rõ tư thế",
            "MODEL_UNAVAILABLE", "Tạm gián đoạn");

    private PostureFeedbackMessages() {
    }

    /** Vietnamese sentence for one sidecar feedback code. */
    public static String forModelFeedback(String code) {
        if (code == null) {
            return UNRECOGNIZED_DETAIL;
        }
        return MODEL_FEEDBACK.getOrDefault(code.trim(), UNRECOGNIZED_DETAIL);
    }

    /**
     * Vietnamese sentence for a classifier label the model considered correct.
     * Falls back to generic praise rather than exposing the label itself.
     */
    public static String forModelClass(String exerciseKey, String predictedClass) {
        if (exerciseKey == null || predictedClass == null) {
            return MODEL_FEEDBACK.get("correct");
        }
        return MODEL_CLASSES.getOrDefault(
                exerciseKey.trim() + "/" + predictedClass.trim(), MODEL_FEEDBACK.get("correct"));
    }

    /** Vietnamese sentence for one geometric rule finding. */
    public static String forRuleFinding(String code) {
        if (code == null) {
            return UNRECOGNIZED_DETAIL;
        }
        return RULE_FINDINGS.getOrDefault(code.trim(), UNRECOGNIZED_DETAIL);
    }

    /** Vietnamese sentence for a posture code produced by this backend. */
    public static String forPostureCode(String postureCode) {
        if (postureCode == null) {
            return UNRECOGNIZED_DETAIL;
        }
        return POSTURE_CODES.getOrDefault(postureCode.trim(), UNRECOGNIZED_DETAIL);
    }

    /** Marks a sentence as coming from the degraded rule-based path. */
    public static String degraded(String detail) {
        return DEGRADED_PREFIX + (detail == null || detail.isBlank() ? UNRECOGNIZED_DETAIL : detail);
    }

    /**
     * Short Vietnamese label for the BASIC feedback level. Composite degraded codes
     * such as {@code MODEL_UNAVAILABLE_RULE_FALLBACK_GOOD_FORM} resolve to the label
     * of the rule result that actually produced them.
     */
    public static String shortLabel(String postureCode) {
        if (postureCode == null) {
            return UNRECOGNIZED_LABEL;
        }
        String code = postureCode.trim();
        int marker = code.indexOf(RULE_FALLBACK_MARKER);
        if (marker >= 0) {
            return SHORT_LABELS.getOrDefault(
                    code.substring(marker + RULE_FALLBACK_MARKER.length()), UNRECOGNIZED_LABEL);
        }
        return SHORT_LABELS.getOrDefault(code, UNRECOGNIZED_LABEL);
    }
}

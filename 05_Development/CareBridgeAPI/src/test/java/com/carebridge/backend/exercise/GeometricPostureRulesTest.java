package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.exercise.policy.GeometricPostureRules;
import com.carebridge.backend.exercise.policy.GeometricPostureRules.RuleFinding;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Mirrors the thresholds in upstream Exercise-Correction {@code web/server/detection}
 * at commit {@code 202a0a80}: bicep_curl.py LOOSE_UPPER_ARM_ANGLE_THRESHOLD = 40,
 * squat.py FOOT_SHOULDER_RATIO_THRESHOLDS = [1.2, 2.8] and KNEE_FOOT_RATIO_THRESHOLDS,
 * lunge.py KNEE_ANGLE_THRESHOLD = [60, 125].
 */
class GeometricPostureRulesTest {

    private static Map<String, Object> landmark(double x, double y, double visibility) {
        return Map.of("x", x, "y", y, "z", 0.0, "visibility", visibility);
    }

    private static List<String> codes(List<RuleFinding> findings) {
        return findings.stream().map(RuleFinding::code).toList();
    }

    // --- bicep curl ---------------------------------------------------------

    private Map<String, Object> bicep(double elbowX, double elbowY, double visibility) {
        Map<String, Object> pose = new LinkedHashMap<>();
        pose.put("left_shoulder", landmark(0.5, 0.3, visibility));
        pose.put("left_elbow", landmark(elbowX, elbowY, visibility));
        pose.put("right_shoulder", landmark(0.5, 0.3, visibility));
        pose.put("right_elbow", landmark(0.5, 0.6, visibility));
        return pose;
    }

    @Test
    @DisplayName("Upper arm hanging vertically is accepted")
    void bicepCurl_verticalUpperArm_noFinding() {
        // Elbow straight below the shoulder: 0 degrees off vertical.
        assertThat(GeometricPostureRules.evaluate("bicep_curl", bicep(0.5, 0.6, 0.95), null))
                .isEmpty();
    }

    @Test
    @DisplayName("Upper arm beyond 40 degrees off vertical is flagged")
    void bicepCurl_looseUpperArm_isFlagged() {
        // dx = 0.3, dy = 0.3 → 45 degrees off vertical, past the 40 degree threshold.
        List<RuleFinding> findings =
                GeometricPostureRules.evaluate("bicep_curl", bicep(0.8, 0.6, 0.95), null);

        assertThat(codes(findings)).containsExactly(GeometricPostureRules.LOOSE_UPPER_ARM);
        assertThat(findings.get(0).severity()).isEqualTo("WARNING");
    }

    @Test
    @DisplayName("Landmarks below the 0.65 visibility gate are ignored, not guessed")
    void bicepCurl_lowVisibility_yieldsNoFinding() {
        assertThat(GeometricPostureRules.evaluate("bicep_curl", bicep(0.8, 0.6, 0.5), null))
                .isEmpty();
    }

    // --- squat --------------------------------------------------------------

    private Map<String, Object> squat(double footHalfWidth, double kneeHalfWidth) {
        Map<String, Object> pose = new LinkedHashMap<>();
        // Shoulder width fixed at 0.2 so the foot/shoulder ratio is easy to reason about.
        pose.put("left_shoulder", landmark(0.4, 0.3, 0.95));
        pose.put("right_shoulder", landmark(0.6, 0.3, 0.95));
        pose.put("left_foot_index", landmark(0.5 - footHalfWidth, 0.9, 0.95));
        pose.put("right_foot_index", landmark(0.5 + footHalfWidth, 0.9, 0.95));
        pose.put("left_knee", landmark(0.5 - kneeHalfWidth, 0.7, 0.95));
        pose.put("right_knee", landmark(0.5 + kneeHalfWidth, 0.7, 0.95));
        return pose;
    }

    @Test
    @DisplayName("Feet about shoulder width with knees tracking are accepted")
    void squat_acceptablePlacement_noFinding() {
        // foot width 0.3 / shoulder 0.2 = 1.5; knee 0.27 / foot 0.3 = 0.9, inside down[0.7,1.1]
        assertThat(GeometricPostureRules.evaluate("squat", squat(0.15, 0.135), "down")).isEmpty();
    }

    @Test
    @DisplayName("Stance narrower than 1.2x shoulder width is flagged")
    void squat_feetTooTight_isFlagged() {
        // foot width 0.2 / shoulder 0.2 = 1.0, below the 1.2 minimum
        assertThat(codes(GeometricPostureRules.evaluate("squat", squat(0.1, 0.09), "down")))
                .contains(GeometricPostureRules.FOOT_PLACEMENT_TOO_TIGHT);
    }

    @Test
    @DisplayName("Stance wider than 2.8x shoulder width is flagged")
    void squat_feetTooWide_isFlagged() {
        // foot width 0.6 / shoulder 0.2 = 3.0, above the 2.8 maximum
        assertThat(codes(GeometricPostureRules.evaluate("squat", squat(0.3, 0.27), "down")))
                .contains(GeometricPostureRules.FOOT_PLACEMENT_TOO_WIDE);
    }

    @Test
    @DisplayName("Knees caving inward during the down phase are flagged")
    void squat_kneesCaveIn_isFlagged() {
        // foot width 0.3 (ratio 1.5, fine); knee 0.15 / foot 0.3 = 0.5, below down[0.7]
        assertThat(codes(GeometricPostureRules.evaluate("squat", squat(0.15, 0.075), "down")))
                .containsExactly(GeometricPostureRules.KNEE_PLACEMENT_TOO_TIGHT);
    }

    @Test
    @DisplayName("The knee band is stage-dependent: 0.6 passes standing, fails at the bottom")
    void squat_kneeBandDependsOnStage() {
        // knee 0.09 / foot 0.3 = 0.3 → below both bands; use 0.6 to separate them.
        Map<String, Object> pose = squat(0.15, 0.09);
        assertThat(codes(GeometricPostureRules.evaluate("squat", pose, "up")))
                .doesNotContain(GeometricPostureRules.KNEE_PLACEMENT_TOO_TIGHT);
        assertThat(codes(GeometricPostureRules.evaluate("squat", pose, "down")))
                .contains(GeometricPostureRules.KNEE_PLACEMENT_TOO_TIGHT);
    }

    @Test
    @DisplayName("Without a stage the knee band is skipped, but feet are still checked")
    void squat_withoutStage_skipsKneeBandOnly() {
        List<String> found = codes(GeometricPostureRules.evaluate("squat", squat(0.1, 0.075), null));

        assertThat(found).containsExactly(GeometricPostureRules.FOOT_PLACEMENT_TOO_TIGHT);
    }

    // --- lunge --------------------------------------------------------------

    private Map<String, Object> lunge(double kneeX, double kneeY) {
        Map<String, Object> pose = new LinkedHashMap<>();
        pose.put("left_hip", landmark(0.5, 0.5, 0.95));
        pose.put("left_knee", landmark(kneeX, kneeY, 0.95));
        pose.put("left_ankle", landmark(0.5, 0.9, 0.95));
        pose.put("right_hip", landmark(0.5, 0.5, 0.95));
        pose.put("right_knee", landmark(0.5, 0.7, 0.95));
        pose.put("right_ankle", landmark(0.5, 0.9, 0.95));
        return pose;
    }

    @Test
    @DisplayName("A straight leg (180 degrees) is outside the [60,125] band")
    void lunge_straightLeg_isFlagged() {
        assertThat(codes(GeometricPostureRules.evaluate("lunge", lunge(0.5, 0.7), null)))
                .containsExactly(GeometricPostureRules.KNEE_ANGLE_OUT_OF_RANGE);
    }

    @Test
    @DisplayName("A knee bent to roughly 90 degrees sits inside the band")
    void lunge_bentKnee_noFinding() {
        // hip straight above the knee, ankle straight to the side → 90 degrees.
        Map<String, Object> pose = new LinkedHashMap<>();
        pose.put("left_hip", landmark(0.5, 0.5, 0.95));
        pose.put("left_knee", landmark(0.5, 0.7, 0.95));
        pose.put("left_ankle", landmark(0.7, 0.7, 0.95));
        pose.put("right_hip", landmark(0.5, 0.5, 0.95));
        pose.put("right_knee", landmark(0.5, 0.7, 0.95));
        pose.put("right_ankle", landmark(0.7, 0.7, 0.95));

        assertThat(GeometricPostureRules.evaluate("lunge", pose, null)).isEmpty();
    }

    // --- guards -------------------------------------------------------------

    @Test
    @DisplayName("Plank and unknown keys carry no geometric rule, matching upstream")
    void unsupportedExercises_yieldNoFinding() {
        assertThat(GeometricPostureRules.evaluate("plank", bicep(0.8, 0.6, 0.95), null)).isEmpty();
        assertThat(GeometricPostureRules.evaluate("burpee", bicep(0.8, 0.6, 0.95), null)).isEmpty();
        assertThat(GeometricPostureRules.evaluate(null, bicep(0.8, 0.6, 0.95), null)).isEmpty();
        assertThat(GeometricPostureRules.evaluate("squat", Map.of(), "down")).isEmpty();
        assertThat(GeometricPostureRules.evaluate("squat", null, "down")).isEmpty();
    }
}

package com.carebridge.backend.exercise.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deterministic geometric posture checks over MediaPipe landmarks.
 *
 * <p>This is the rule half of the hybrid approach used by the upstream
 * Exercise-Correction project (commit {@code 202a0a80}, {@code web/server/detection}).
 * CareBridge originally vendored only the model artifacts, so every check upstream
 * performs in plain geometry was missing — most visibly for squats, where upstream
 * detects form errors entirely through rules and the model only classifies the
 * up/down phase.
 *
 * <p>Thresholds and formulas are kept identical to upstream, including its
 * coordinate convention: angles come from {@code atan2} and widths from Euclidean
 * distance over the <em>normalized</em> x/y landmarks, with no aspect-ratio
 * correction. Introducing that correction here would silently invalidate every
 * threshold below, so it is deliberately not applied.
 *
 * <p>Only frame-local checks live here. Upstream's stateful checks — repetition
 * counting, peak-contraction tracking across a curl, and "record the error only on
 * the transition into it" — need per-stream history that this stateless backend
 * path does not carry.
 *
 * <p>Findings are guidance only and never diagnostic.
 */
public final class GeometricPostureRules {

    /** One rule violation, keyed by a stable code resolved to copy at presentation time. */
    public record RuleFinding(String code, String severity) {
    }

    // --- Upstream thresholds (web/server/detection) -------------------------

    /** bicep_curl.py: VISIBILITY_THRESHOLD. */
    private static final double BICEP_VISIBILITY = 0.65;

    /** bicep_curl.py: LOOSE_UPPER_ARM_ANGLE_THRESHOLD, degrees off vertical. */
    private static final double LOOSE_UPPER_ARM_DEGREES = 40.0;

    /** squat.py: VISIBILITY_THRESHOLD. */
    private static final double SQUAT_VISIBILITY = 0.6;

    /** squat.py: FOOT_SHOULDER_RATIO_THRESHOLDS. */
    private static final double FOOT_SHOULDER_MIN = 1.2;
    private static final double FOOT_SHOULDER_MAX = 2.8;

    /** squat.py: KNEE_FOOT_RATIO_THRESHOLDS, keyed by squat stage. */
    private static final Map<String, double[]> KNEE_FOOT_RATIO = Map.of(
            "up", new double[] {0.5, 1.0},
            "middle", new double[] {0.7, 1.0},
            "down", new double[] {0.7, 1.1});

    /** lunge.py: KNEE_ANGLE_THRESHOLD. */
    private static final double LUNGE_KNEE_ANGLE_MIN = 60.0;
    private static final double LUNGE_KNEE_ANGLE_MAX = 125.0;

    /** Upstream lunge.py gates on visibility without naming a constant; mirror the squat gate. */
    private static final double LUNGE_VISIBILITY = 0.6;

    // --- Finding codes ------------------------------------------------------

    public static final String LOOSE_UPPER_ARM = "LOOSE_UPPER_ARM";
    public static final String FOOT_PLACEMENT_TOO_TIGHT = "FOOT_PLACEMENT_TOO_TIGHT";
    public static final String FOOT_PLACEMENT_TOO_WIDE = "FOOT_PLACEMENT_TOO_WIDE";
    public static final String KNEE_PLACEMENT_TOO_TIGHT = "KNEE_PLACEMENT_TOO_TIGHT";
    public static final String KNEE_PLACEMENT_TOO_WIDE = "KNEE_PLACEMENT_TOO_WIDE";
    public static final String KNEE_ANGLE_OUT_OF_RANGE = "KNEE_ANGLE_OUT_OF_RANGE";

    /** Raised by {@link PostureSessionTracker}; the check spans a whole repetition. */
    public static final String WEAK_PEAK_CONTRACTION = "WEAK_PEAK_CONTRACTION";

    static final String WARNING = "WARNING";

    private GeometricPostureRules() {
    }

    /**
     * Evaluates the frame-local rules for one exercise.
     *
     * @param exerciseKey server-owned exercise key; unknown keys yield no findings
     * @param landmarks   named MediaPipe landmarks as posted by the client
     * @param stage       model-reported phase ({@code up}/{@code down}) or {@code null}
     *                    when inference did not run; stage-dependent checks are then
     *                    skipped rather than evaluated against a guessed band
     * @return findings in a stable order, empty when the pose is acceptable or the
     *         required landmarks are missing or not confidently visible
     */
    public static List<RuleFinding> evaluate(
            String exerciseKey, Map<String, Object> landmarks, String stage) {
        if (exerciseKey == null || landmarks == null || landmarks.isEmpty()) {
            return List.of();
        }
        return switch (exerciseKey) {
            case "bicep_curl" -> evaluateBicepCurl(landmarks);
            case "squat" -> evaluateSquat(landmarks, stage);
            case "lunge" -> evaluateLunge(landmarks);
            // plank: upstream applies no geometric rule beyond its model threshold.
            default -> List.of();
        };
    }

    /**
     * Upstream's loose-upper-arm check: the angle between the upper arm and vertical,
     * measured at the shoulder. A curl should keep the upper arm pinned to the torso.
     */
    private static List<RuleFinding> evaluateBicepCurl(Map<String, Object> landmarks) {
        for (String side : List.of("left", "right")) {
            Point shoulder = point(landmarks, side + "_shoulder", BICEP_VISIBILITY);
            Point elbow = point(landmarks, side + "_elbow", BICEP_VISIBILITY);
            if (shoulder == null || elbow == null) {
                continue;
            }
            // Upstream projects the shoulder straight down the frame and measures the
            // angle at the shoulder between that projection and the elbow.
            Point projection = new Point(shoulder.x(), shoulder.y() + 1.0);
            double angle = angleAt(shoulder, elbow, projection);
            if (angle > LOOSE_UPPER_ARM_DEGREES) {
                return List.of(new RuleFinding(LOOSE_UPPER_ARM, WARNING));
            }
        }
        return List.of();
    }

    /**
     * Upstream's foot- and knee-placement analysis. Both are width ratios, so they
     * survive the missing aspect-ratio correction far better than a raw angle would:
     * the segments compared are all roughly horizontal.
     */
    private static List<RuleFinding> evaluateSquat(Map<String, Object> landmarks, String stage) {
        List<RuleFinding> findings = new ArrayList<>();

        Double shoulderWidth = width(landmarks, "left_shoulder", "right_shoulder", SQUAT_VISIBILITY);
        Double footWidth = width(landmarks, "left_foot_index", "right_foot_index", SQUAT_VISIBILITY);
        Double kneeWidth = width(landmarks, "left_knee", "right_knee", SQUAT_VISIBILITY);

        if (shoulderWidth != null && footWidth != null && shoulderWidth > 0) {
            double footShoulderRatio = round1(footWidth / shoulderWidth);
            if (footShoulderRatio < FOOT_SHOULDER_MIN) {
                findings.add(new RuleFinding(FOOT_PLACEMENT_TOO_TIGHT, WARNING));
            } else if (footShoulderRatio > FOOT_SHOULDER_MAX) {
                findings.add(new RuleFinding(FOOT_PLACEMENT_TOO_WIDE, WARNING));
            }
        }

        double[] band = stage == null ? null : KNEE_FOOT_RATIO.get(stage);
        if (band != null && footWidth != null && kneeWidth != null && footWidth > 0) {
            double kneeFootRatio = round1(kneeWidth / footWidth);
            if (kneeFootRatio < band[0]) {
                findings.add(new RuleFinding(KNEE_PLACEMENT_TOO_TIGHT, WARNING));
            } else if (kneeFootRatio > band[1]) {
                findings.add(new RuleFinding(KNEE_PLACEMENT_TOO_WIDE, WARNING));
            }
        }
        return List.copyOf(findings);
    }

    /** Upstream's knee-angle band, evaluated per leg on the hip-knee-ankle triplet. */
    private static List<RuleFinding> evaluateLunge(Map<String, Object> landmarks) {
        for (String side : List.of("left", "right")) {
            Point hip = point(landmarks, side + "_hip", LUNGE_VISIBILITY);
            Point knee = point(landmarks, side + "_knee", LUNGE_VISIBILITY);
            Point ankle = point(landmarks, side + "_ankle", LUNGE_VISIBILITY);
            if (hip == null || knee == null || ankle == null) {
                continue;
            }
            double angle = angleAt(knee, hip, ankle);
            if (angle < LUNGE_KNEE_ANGLE_MIN || angle > LUNGE_KNEE_ANGLE_MAX) {
                return List.of(new RuleFinding(KNEE_ANGLE_OUT_OF_RANGE, WARNING));
            }
        }
        return List.of();
    }

    /**
     * Elbow angle for one side, measured on the shoulder-elbow-wrist triplet — the
     * same value upstream drives its curl stage machine and peak-contraction check
     * from. Returns {@code null} when the arm is not confidently visible.
     *
     * @param side {@code "left"} or {@code "right"}
     */
    public static Double elbowAngle(Map<String, Object> landmarks, String side) {
        if (landmarks == null || side == null) {
            return null;
        }
        Point shoulder = point(landmarks, side + "_shoulder", BICEP_VISIBILITY);
        Point elbow = point(landmarks, side + "_elbow", BICEP_VISIBILITY);
        Point wrist = point(landmarks, side + "_wrist", BICEP_VISIBILITY);
        if (shoulder == null || elbow == null || wrist == null) {
            return null;
        }
        return angleAt(elbow, shoulder, wrist);
    }

    // --- Geometry, matching upstream detection/utils.py ----------------------

    /** Angle at {@code vertex} between {@code first} and {@code second}, in [0, 180]. */
    static double angleAt(Point vertex, Point first, Point second) {
        double radians = Math.atan2(second.y() - vertex.y(), second.x() - vertex.x())
                - Math.atan2(first.y() - vertex.y(), first.x() - vertex.x());
        double degrees = Math.abs(Math.toDegrees(radians));
        return degrees <= 180.0 ? degrees : 360.0 - degrees;
    }

    private static Double width(
            Map<String, Object> landmarks, String left, String right, double visibility) {
        Point a = point(landmarks, left, visibility);
        Point b = point(landmarks, right, visibility);
        if (a == null || b == null) {
            return null;
        }
        return Math.hypot(b.x() - a.x(), b.y() - a.y());
    }

    /** Upstream rounds every ratio to one decimal before comparing. */
    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static Point point(Map<String, Object> landmarks, String name, double visibility) {
        if (!(landmarks.get(name) instanceof Map<?, ?> raw)) {
            return null;
        }
        Double confidence = number(raw.get("visibility"));
        if (confidence == null || confidence < visibility) {
            return null;
        }
        Double x = number(raw.get("x"));
        Double y = number(raw.get("y"));
        return x == null || y == null ? null : new Point(x, y);
    }

    private static Double number(Object value) {
        if (value instanceof Number number) {
            double parsed = number.doubleValue();
            return Double.isFinite(parsed) ? parsed : null;
        }
        return null;
    }

    record Point(double x, double y) {
    }
}

package com.carebridge.backend.exercise.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.exercise.policy.PostureSessionTracker.TrackedFrame;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the cross-frame behaviours ported from upstream Exercise-Correction
 * {@code detection/bicep_curl.py}: the 120/100 degree curl stage machine, its
 * repetition counter, and the 60 degree peak-contraction check evaluated when the
 * arm extends again.
 */
class PostureSessionTrackerTest {

    private static final UUID SESSION = UUID.randomUUID();

    private final PostureSessionTracker tracker = new PostureSessionTracker();

    /**
     * Builds a left arm whose shoulder-elbow-wrist angle is {@code degrees}. The elbow
     * sits at the origin of the angle with the shoulder straight above it.
     */
    private Map<String, Object> arm(double degrees) {
        double radians = Math.toRadians(degrees);
        Map<String, Object> pose = new LinkedHashMap<>();
        pose.put("left_shoulder", landmark(0.5, 0.3));
        pose.put("left_elbow", landmark(0.5, 0.5));
        pose.put("left_wrist", landmark(0.5 + 0.2 * Math.sin(radians), 0.5 - 0.2 * Math.cos(radians)));
        return pose;
    }

    private Map<String, Object> landmark(double x, double y) {
        return Map.of("x", x, "y", y, "z", 0.0, "visibility", 0.95);
    }

    private TrackedFrame track(Map<String, Object> pose) {
        return tracker.track(SESSION, "bicep_curl", pose, null);
    }

    @Test
    @DisplayName("The elbow angle fixture matches GeometricPostureRules")
    void armFixtureProducesTheIntendedAngle() {
        assertThat(GeometricPostureRules.elbowAngle(arm(150.0), "left")).isCloseTo(150.0, within());
        assertThat(GeometricPostureRules.elbowAngle(arm(40.0), "left")).isCloseTo(40.0, within());
    }

    @Test
    @DisplayName("Extending past 120 then curling below 100 counts one repetition")
    void fullCurlCountsOneRepetition() {
        assertThat(track(arm(150.0)).repetitions()).isZero();
        assertThat(track(arm(110.0)).repetitions()).isZero();
        assertThat(track(arm(40.0)).repetitions()).isEqualTo(1);
    }

    @Test
    @DisplayName("Staying curled does not keep incrementing the counter")
    void repeatedCurledFramesCountOnce() {
        track(arm(150.0));
        track(arm(40.0));
        track(arm(35.0));
        assertThat(track(arm(30.0)).repetitions()).isEqualTo(1);
    }

    @Test
    @DisplayName("A curl tighter than 60 degrees raises no peak-contraction finding")
    void strongContractionIsAccepted() {
        track(arm(150.0));
        track(arm(40.0));

        TrackedFrame extended = track(arm(150.0));

        assertThat(extended.findings()).isEmpty();
        assertThat(extended.repetitions()).isEqualTo(1);
    }

    @Test
    @DisplayName("A curl that never got tighter than 60 degrees is flagged on the way down")
    void weakContractionIsFlaggedWhenTheArmExtends() {
        track(arm(150.0));
        track(arm(90.0));  // counts the rep, but only reaches 90 degrees
        track(arm(80.0));

        TrackedFrame extended = track(arm(150.0));

        assertThat(extended.findings())
                .singleElement()
                .satisfies(finding -> {
                    assertThat(finding.code())
                            .isEqualTo(GeometricPostureRules.WEAK_PEAK_CONTRACTION);
                    assertThat(finding.severity()).isEqualTo("WARNING");
                });
    }

    @Test
    @DisplayName("The peak resets, so the next weak curl is reported again")
    void peakResetsBetweenRepetitions() {
        track(arm(150.0));
        track(arm(90.0));
        assertThat(track(arm(150.0)).findings()).hasSize(1);

        track(arm(90.0));
        assertThat(track(arm(150.0)).findings()).hasSize(1);
    }

    @Test
    @DisplayName("Squat repetitions close on the model's down to up transition")
    void squatRepetitionsFollowTheModelStage() {
        assertThat(tracker.track(SESSION, "squat", Map.of(), "up").repetitions()).isZero();
        assertThat(tracker.track(SESSION, "squat", Map.of(), "down").repetitions()).isZero();
        assertThat(tracker.track(SESSION, "squat", Map.of(), "down").repetitions()).isZero();
        assertThat(tracker.track(SESSION, "squat", Map.of(), "up").repetitions()).isEqualTo(1);
        assertThat(tracker.track(SESSION, "squat", Map.of(), "up").repetitions()).isEqualTo(1);
    }

    @Test
    @DisplayName("Lunge repetitions close on entering the down phase, the opposite of squats")
    void lungeRepetitionsFollowTheStageLabels() {
        assertThat(tracker.track(SESSION, "lunge", Map.of(), "I").repetitions()).isZero();
        assertThat(tracker.track(SESSION, "lunge", Map.of(), "M").repetitions()).isZero();
        assertThat(tracker.track(SESSION, "lunge", Map.of(), "D").repetitions()).isEqualTo(1);
        // Holding the down phase does not keep counting.
        assertThat(tracker.track(SESSION, "lunge", Map.of(), "D").repetitions()).isEqualTo(1);
        assertThat(tracker.track(SESSION, "lunge", Map.of(), "I").repetitions()).isEqualTo(1);
        assertThat(tracker.track(SESSION, "lunge", Map.of(), "D").repetitions()).isEqualTo(2);
    }

    @Test
    @DisplayName("A frame the lunge model was unsure about leaves the phase untouched")
    void lungeIgnoresMissingAndUnknownStages() {
        tracker.track(SESSION, "lunge", Map.of(), "I");
        // Below its threshold the sidecar reports no stage at all.
        tracker.track(SESSION, "lunge", Map.of(), null);
        tracker.track(SESSION, "lunge", Map.of(), "C");

        assertThat(tracker.track(SESSION, "lunge", Map.of(), "D").repetitions()).isEqualTo(1);
    }

    @Test
    @DisplayName("A posture is a new occurrence only when it changes")
    void transitionGateFiresOnlyOnChange() {
        assertThat(tracker.isNewOccurrence(SESSION, "GOOD_FORM")).isTrue();
        assertThat(tracker.isNewOccurrence(SESSION, "GOOD_FORM")).isFalse();
        assertThat(tracker.isNewOccurrence(SESSION, "ROUND_BACK")).isTrue();
        assertThat(tracker.isNewOccurrence(SESSION, "ROUND_BACK")).isFalse();
        // Returning to a posture already seen earlier is still a fresh entry into it.
        assertThat(tracker.isNewOccurrence(SESSION, "GOOD_FORM")).isTrue();
    }

    @Test
    @DisplayName("Clearing a session drops its counters")
    void clearResetsTheSession() {
        track(arm(150.0));
        track(arm(40.0));
        assertThat(tracker.repetitions(SESSION)).isEqualTo(1);

        tracker.clear(SESSION);

        assertThat(tracker.repetitions(SESSION)).isZero();
    }

    @Test
    @DisplayName("Unknown sessions and exercises are inert")
    void unknownInputsAreInert() {
        assertThat(tracker.repetitions(UUID.randomUUID())).isZero();
        assertThat(tracker.repetitions(null)).isZero();
        assertThat(tracker.track(null, "bicep_curl", arm(40.0), null).repetitions()).isZero();
        assertThat(tracker.track(SESSION, null, arm(40.0), null).findings()).isEmpty();
        assertThat(tracker.track(SESSION, "plank", arm(40.0), null).findings()).isEmpty();
        assertThat(tracker.isNewOccurrence(null, "GOOD_FORM")).isTrue();
    }

    @Test
    @DisplayName("Idle sessions are evicted rather than held for the process lifetime")
    void idleSessionsAreEvicted() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-07T10:00:00Z"));
        PostureSessionTracker ticking = new PostureSessionTracker(clock);

        ticking.track(SESSION, "bicep_curl", arm(150.0), null);
        ticking.track(SESSION, "bicep_curl", arm(40.0), null);
        assertThat(ticking.repetitions(SESSION)).isEqualTo(1);

        clock.advance(Duration.ofMinutes(31));
        // Any call sweeps expired entries first.
        ticking.track(UUID.randomUUID(), "bicep_curl", arm(150.0), null);

        assertThat(ticking.repetitions(SESSION)).isZero();
    }

    private static org.assertj.core.data.Offset<Double> within() {
        return org.assertj.core.data.Offset.offset(0.5);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    @DisplayName("Findings are returned as an immutable list")
    void findingsAreImmutable() {
        track(arm(150.0));
        track(arm(90.0));
        List<?> findings = track(arm(150.0)).findings();

        assertThat(findings).isNotEmpty();
        assertThat(findings.getClass().getName()).contains("Immutable");
    }
}

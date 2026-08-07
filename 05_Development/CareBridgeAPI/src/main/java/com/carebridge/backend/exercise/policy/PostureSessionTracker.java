package com.carebridge.backend.exercise.policy;

import com.carebridge.backend.exercise.policy.GeometricPostureRules.RuleFinding;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Cross-frame posture state for an exercise session.
 *
 * <p>The frame-local checks in {@link GeometricPostureRules} cannot see a whole
 * repetition, but three of upstream Exercise-Correction's behaviours need exactly
 * that: counting repetitions, judging peak contraction across a curl, and recording
 * an error only when the pose <em>enters</em> that error rather than on every frame
 * of it.
 *
 * <p>State is per session and in memory, mirroring the existing
 * {@link com.carebridge.backend.security.policy.RateLimitPolicy} idiom. That is a
 * deliberate trade: posture frames arrive up to ten times a second, so persisting
 * this would multiply database writes for data that is worthless once the session
 * ends. A restart or a second instance therefore restarts the counters — the session
 * keeps working, it just resumes counting from zero.
 */
@Component
public class PostureSessionTracker {

    /** bicep_curl.py: STAGE_UP_THRESHOLD — below this the arm counts as curled. */
    private static final double CURL_UP_DEGREES = 100.0;

    /** bicep_curl.py: STAGE_DOWN_THRESHOLD — above this the arm counts as extended. */
    private static final double CURL_DOWN_DEGREES = 120.0;

    /** bicep_curl.py: PEAK_CONTRACTION_THRESHOLD — the curl must reach tighter than this. */
    private static final double PEAK_CONTRACTION_DEGREES = 60.0;

    /** bicep_curl.py sentinel meaning "no peak recorded for this repetition yet". */
    private static final double NO_PEAK = 1000.0;

    /** Abandoned sessions are dropped rather than held for the process lifetime. */
    private static final Duration IDLE_TTL = Duration.ofMinutes(30);

    private static final String STAGE_UP = "up";
    private static final String STAGE_DOWN = "down";

    private final Clock clock;
    private final Map<UUID, SessionState> states = new ConcurrentHashMap<>();

    public PostureSessionTracker() {
        this(Clock.systemUTC());
    }

    PostureSessionTracker(Clock clock) {
        this.clock = clock;
    }

    /** What one frame contributed once session history is taken into account. */
    public record TrackedFrame(int repetitions, List<RuleFinding> findings) {
    }

    /**
     * Folds one frame into the session's history.
     *
     * @param modelStage the phase reported by the model for squats and lunges;
     *                   ignored for curls, whose stage upstream derives from the
     *                   elbow angle rather than from a model
     * @return the running repetition count plus any finding that only a full
     *         repetition could reveal
     */
    public TrackedFrame track(
            UUID sessionId, String exerciseKey, Map<String, Object> landmarks, String modelStage) {
        if (sessionId == null || exerciseKey == null) {
            return new TrackedFrame(0, List.of());
        }
        evictIdleSessions();
        SessionState state = states.computeIfAbsent(sessionId, key -> new SessionState());

        synchronized (state) {
            state.lastSeen = clock.instant();
            List<RuleFinding> findings = switch (exerciseKey) {
                case "bicep_curl" -> trackCurl(state, landmarks);
                case "squat" -> {
                    trackSquatStage(state, modelStage);
                    yield List.of();
                }
                case "lunge" -> {
                    trackLungeStage(state, modelStage);
                    yield List.of();
                }
                default -> List.of();
            };
            return new TrackedFrame(state.repetitions(), findings);
        }
    }

    /**
     * Reports whether this posture code differs from the one last seen, so callers
     * can persist an event when a posture is entered instead of on every frame that
     * holds it. Upstream applies the same gate before recording an error.
     *
     * <p>Calling this advances the session's last-seen posture, so it must be called
     * once per frame and its answer acted on.
     */
    public boolean isNewOccurrence(UUID sessionId, String postureCode) {
        if (sessionId == null) {
            return true;
        }
        evictIdleSessions();
        SessionState state = states.computeIfAbsent(sessionId, key -> new SessionState());
        synchronized (state) {
            state.lastSeen = clock.instant();
            boolean changed = postureCode == null
                    ? state.lastPostureCode != null
                    : !postureCode.equals(state.lastPostureCode);
            state.lastPostureCode = postureCode;
            return changed;
        }
    }

    /** Repetitions counted so far, or zero for an unknown session. */
    public int repetitions(UUID sessionId) {
        SessionState state = sessionId == null ? null : states.get(sessionId);
        if (state == null) {
            return 0;
        }
        synchronized (state) {
            return state.repetitions();
        }
    }

    /** Releases the session's state; call once the session is no longer running. */
    public void clear(UUID sessionId) {
        if (sessionId != null) {
            states.remove(sessionId);
        }
    }

    /**
     * Upstream's curl state machine, run independently per arm: the arm is "down"
     * past 120 degrees, and crossing below 100 from there completes a repetition.
     * While curled, the tightest angle reached is remembered; when the arm extends
     * again, a peak that never got tighter than 60 degrees is a weak contraction.
     */
    private List<RuleFinding> trackCurl(SessionState state, Map<String, Object> landmarks) {
        List<RuleFinding> findings = new ArrayList<>();
        for (ArmState arm : List.of(state.leftArm, state.rightArm)) {
            Double angle = GeometricPostureRules.elbowAngle(landmarks, arm.side);
            if (angle == null) {
                continue;
            }
            if (angle > CURL_DOWN_DEGREES) {
                if (STAGE_UP.equals(arm.stage)) {
                    if (arm.peakAngle != NO_PEAK && arm.peakAngle >= PEAK_CONTRACTION_DEGREES) {
                        findings.add(new RuleFinding(
                                GeometricPostureRules.WEAK_PEAK_CONTRACTION,
                                GeometricPostureRules.WARNING));
                    }
                    arm.peakAngle = NO_PEAK;
                }
                arm.stage = STAGE_DOWN;
            } else if (angle < CURL_UP_DEGREES && STAGE_DOWN.equals(arm.stage)) {
                arm.stage = STAGE_UP;
                arm.repetitions++;
            }
            if (STAGE_UP.equals(arm.stage)) {
                arm.peakAngle = Math.min(arm.peakAngle, angle);
            }
        }
        return List.copyOf(findings);
    }

    /** squat.py counts a repetition when the phase returns from down to up. */
    private void trackSquatStage(SessionState state, String modelStage) {
        if (modelStage == null) {
            return;
        }
        if (STAGE_UP.equals(modelStage) && STAGE_DOWN.equals(state.legStage)) {
            state.legRepetitions++;
        }
        if (STAGE_UP.equals(modelStage) || STAGE_DOWN.equals(modelStage)) {
            state.legStage = modelStage;
        }
    }

    /**
     * lunge.py counts in the other direction: the repetition closes on <em>entering</em>
     * the down phase from init or mid. Stages arrive as the raw model labels
     * {@code I}/{@code M}/{@code D}, and a stage the model was not confident about
     * never arrives at all, so the machine holds its last known phase.
     */
    private void trackLungeStage(SessionState state, String modelStage) {
        if (modelStage == null) {
            return;
        }
        switch (modelStage) {
            case "I", "M" -> state.legStage = modelStage;
            case "D" -> {
                if ("I".equals(state.legStage) || "M".equals(state.legStage)) {
                    state.legRepetitions++;
                }
                state.legStage = modelStage;
            }
            default -> {
                // An unrecognised label leaves the phase untouched.
            }
        }
    }

    private void evictIdleSessions() {
        Instant cutoff = clock.instant().minus(IDLE_TTL);
        states.values().removeIf(state -> {
            synchronized (state) {
                return state.lastSeen.isBefore(cutoff);
            }
        });
    }

    private final class SessionState {
        private final ArmState leftArm = new ArmState("left");
        private final ArmState rightArm = new ArmState("right");
        private String legStage;
        private int legRepetitions;
        private String lastPostureCode;
        private Instant lastSeen = clock.instant();

        /**
         * A single number for the session summary. Curls are counted per arm and only
         * one arm may be in frame, so the better-observed arm stands for the set.
         */
        private int repetitions() {
            return Math.max(legRepetitions, Math.max(leftArm.repetitions, rightArm.repetitions));
        }
    }

    private static final class ArmState {
        private final String side;
        private String stage;
        private double peakAngle = NO_PEAK;
        private int repetitions;

        private ArmState(String side) {
            this.side = side;
        }
    }
}

package com.carebridge.backend.exercise.policy;

import com.carebridge.backend.exercise.entity.SafetyQuestion;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Domain policy for evaluating pre-exercise safety check answers.
 *
 * <p>This is the ONLY class that performs red-flag evaluation for safety checks.
 * An answer of {@code false} for any question means the protective condition was
 * NOT met (i.e. a symptom is present), which is treated as a red flag.
 *
 * <p>Blocked-reason texts are intentionally non-diagnostic: they never assert that
 * the user "has" a condition, is "suffering", is "diagnosed", or is "prescribed"
 * anything. They always direct the user to consult a doctor and a midwife.
 */
@Component
public class SafetyCheckPolicy {

    private static final Map<SafetyQuestion, String> BLOCKED_REASONS = buildBlockedReasons();

    public EvaluationResult evaluate(Map<SafetyQuestion, Boolean> answers) {
        List<SafetyQuestion> flagged = new ArrayList<>();
        for (SafetyQuestion question : SafetyQuestion.values()) {
            Boolean answer = answers.get(question);
            // A false answer means the protective condition was NOT met → red flag.
            if (Boolean.FALSE.equals(answer)) {
                flagged.add(question);
            }
        }
        boolean cleared = flagged.isEmpty();
        return new EvaluationResult(cleared, flagged);
    }

    public String buildBlockedReason(List<SafetyQuestion> flaggedQuestions) {
        List<String> parts = new ArrayList<>();
        for (SafetyQuestion question : flaggedQuestions) {
            String reason = BLOCKED_REASONS.get(question);
            if (reason != null) {
                parts.add(reason);
            }
        }
        return String.join(" ", parts);
    }

    private static Map<SafetyQuestion, String> buildBlockedReasons() {
        Map<SafetyQuestion, String> reasons = new EnumMap<>(SafetyQuestion.class);
        reasons.put(
                SafetyQuestion.Q1_NO_DIZZINESS,
                "Dizziness or faintness was reported. It is not recommended to exercise while "
                        + "experiencing dizziness as it may indicate reduced blood flow. Please rest "
                        + "and consult your doctor or midwife before proceeding with physical activity.");
        reasons.put(
                SafetyQuestion.Q2_NO_CONTRACTIONS,
                "Abnormal uterine contractions or abdominal pain was reported. Exercising with "
                        + "these symptoms is not recommended as it may pose risks. Please rest and "
                        + "consult your doctor or midwife before proceeding with physical activity.");
        reasons.put(
                SafetyQuestion.Q3_NO_BLEEDING,
                "Bleeding or suspected amniotic fluid leakage was reported. Exercise is not "
                        + "recommended in this situation. Please seek medical attention and consult "
                        + "your doctor or midwife immediately before any physical activity.");
        reasons.put(
                SafetyQuestion.Q4_HYDRATED_AND_FED,
                "Adequate hydration and nutrition were not confirmed. Exercising without proper "
                        + "hydration and a light snack may be unsafe. Please hydrate, have a light "
                        + "snack, and consult your doctor or midwife if you are unsure.");
        return reasons;
    }
}

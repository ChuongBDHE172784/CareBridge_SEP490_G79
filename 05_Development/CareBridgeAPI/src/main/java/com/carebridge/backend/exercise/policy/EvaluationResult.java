package com.carebridge.backend.exercise.policy;

import com.carebridge.backend.exercise.entity.SafetyQuestion;
import java.util.List;

public class EvaluationResult {

    private final boolean cleared;
    private final List<SafetyQuestion> flaggedQuestions;

    public EvaluationResult(boolean cleared, List<SafetyQuestion> flaggedQuestions) {
        this.cleared = cleared;
        this.flaggedQuestions = flaggedQuestions;
    }

    public boolean isCleared() {
        return cleared;
    }

    public List<SafetyQuestion> getFlaggedQuestions() {
        return flaggedQuestions;
    }
}

package com.carebridge.backend.checklist.distribution;

import java.time.LocalDate;

public record ChecklistEligibilityDecision(
        boolean eligible,
        LocalDate anchorDate,
        LocalDate windowStart,
        LocalDate windowEnd,
        String failureCode) {

    static ChecklistEligibilityDecision eligible(LocalDate anchor, LocalDate start, LocalDate end) {
        return new ChecklistEligibilityDecision(true, anchor, start, end, null);
    }

    static ChecklistEligibilityDecision outside(LocalDate anchor, LocalDate start, LocalDate end) {
        return new ChecklistEligibilityDecision(false, anchor, start, end, null);
    }

    static ChecklistEligibilityDecision failure(String code) {
        return new ChecklistEligibilityDecision(false, null, null, null, code);
    }

    static ChecklistEligibilityDecision neutral() {
        return new ChecklistEligibilityDecision(true, null, null, null, null);
    }
}

package com.carebridge.backend.recommendation.service;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.recommendation.dto.RecommendationEnums.WeekEligibilityMode;

public record RecommendationContext(
        ContentStage stage,
        Integer pregnancyWeek,
        WeekState weekState,
        WeekEligibilityMode weekEligibilityMode) {

    public enum WeekState { NOT_APPLICABLE, KNOWN, MISSING, OUT_OF_RANGE }
}

package com.carebridge.backend.recommendation.dto;

public final class RecommendationEnums {
    private RecommendationEnums() {}

    public enum WeekEligibilityMode { NOT_APPLICABLE, BOUNDED_AND_STAGE_WIDE, STAGE_WIDE_ONLY_MISSING, STAGE_WIDE_ONLY_OUT_OF_RANGE }
    public enum SelectionMode { TARGETED_ONLY, TARGETED_WITH_FALLBACK, FALLBACK_ONLY, EMPTY }
    public enum CoverageStatus { COMPLETE, PARTIAL, EMPTY }
    public enum SelectionType { TARGETED, FALLBACK }
    public enum ReasonCode { PERSONALIZED_CONTEXT, LIFECYCLE_FALLBACK }
}

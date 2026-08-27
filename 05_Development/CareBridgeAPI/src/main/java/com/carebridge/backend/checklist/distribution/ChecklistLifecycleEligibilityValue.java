package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import lombok.Builder;

/** Immutable table-independent lifecycle eligibility value. */
@Builder
public record ChecklistLifecycleEligibilityValue(
        String stage,
        ChecklistAnchorType anchorType,
        ChecklistRangeUnit rangeUnit,
        Integer startInclusive,
        Integer endInclusive,
        Boolean active) implements ChecklistLifecycleEligibility {

    @Override public String getStage() { return stage; }
    @Override public ChecklistAnchorType getAnchorType() { return anchorType; }
    @Override public ChecklistRangeUnit getRangeUnit() { return rangeUnit; }
    @Override public Integer getStartInclusive() { return startInclusive; }
    @Override public Integer getEndInclusive() { return endInclusive; }
    @Override public Boolean getActive() { return active; }
}

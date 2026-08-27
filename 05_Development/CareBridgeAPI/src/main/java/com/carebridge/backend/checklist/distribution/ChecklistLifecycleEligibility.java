package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;

/** Table-independent lifecycle eligibility used by request materialization. */
public interface ChecklistLifecycleEligibility {

    String getStage();

    ChecklistAnchorType getAnchorType();

    ChecklistRangeUnit getRangeUnit();

    Integer getStartInclusive();

    Integer getEndInclusive();

    Boolean getActive();
}

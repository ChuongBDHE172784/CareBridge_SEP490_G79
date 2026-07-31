package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;

/** Structured lifecycle substage returned by checklist authoring APIs. */
public record ChecklistSubstageResponse(
        String code,
        ChecklistAnchorType anchor,
        Integer startInclusive,
        Integer endInclusive,
        ChecklistRangeUnit unit
) {}

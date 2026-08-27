package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Structured lifecycle substage supplied when authoring a checklist template. */
public record ChecklistSubstageRequest(
        @NotBlank @Size(max = 80) String code,
        @NotNull ChecklistAnchorType anchor,
        @NotNull @PositiveOrZero Integer startInclusive,
        @NotNull @PositiveOrZero Integer endInclusive,
        @NotNull ChecklistRangeUnit unit
) {}

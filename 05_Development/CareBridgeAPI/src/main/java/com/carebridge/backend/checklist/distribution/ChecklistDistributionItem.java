package com.carebridge.backend.checklist.distribution;

import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import java.util.UUID;

public record ChecklistDistributionItem(
        UUID templateItemVersionId,
        String title,
        int displayOrder,
        boolean required,
        ChecklistTargetSubject targetSubject,
        ChecklistAnchorType dueAnchor,
        Integer dueOffsetDays,
        ChecklistRangeUnit dueOffsetUnit) {

    public ChecklistDistributionItem(
            UUID templateItemVersionId,
            String title,
            int displayOrder,
            boolean required,
            ChecklistTargetSubject targetSubject,
            ChecklistAnchorType dueAnchor,
            Integer dueOffsetDays) {
        this(templateItemVersionId, title, displayOrder, required, targetSubject,
                dueAnchor, dueOffsetDays, ChecklistRangeUnit.DAY);
    }
}

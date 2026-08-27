package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import java.time.Instant;
import java.util.UUID;

// §14 addendum — mirrors ContentDecisionResponse but without versionNo (ChecklistTemplate has none)
public record ChecklistTemplateDecisionResponse(
        UUID id,
        ChecklistTemplateStatus previousStatus,
        ChecklistTemplateStatus newStatus,
        UUID decidedByAdminId,
        String reason,
        Instant decidedAt
) {}

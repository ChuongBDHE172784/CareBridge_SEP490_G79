package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ContentStatus;
import java.time.Instant;
import java.util.UUID;

// §14 addendum — mirrors ContentDecisionResponse but without versionNo (ChecklistTemplate has none)
public record ChecklistTemplateDecisionResponse(
        UUID id,
        ContentStatus previousStatus,
        ContentStatus newStatus,
        UUID decidedByAdminId,
        String reason,
        Instant decidedAt
) {}

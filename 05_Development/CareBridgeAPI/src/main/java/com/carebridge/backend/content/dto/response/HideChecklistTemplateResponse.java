package com.carebridge.backend.content.dto.response;

import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import java.time.Instant;
import java.util.UUID;

public record HideChecklistTemplateResponse(
        UUID id,
        ChecklistTemplateStatus previousStatus,
        ChecklistTemplateStatus newStatus,
        String reason,
        UUID hiddenByAdminId,
        Instant hiddenAt
) {}

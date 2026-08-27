package com.carebridge.backend.content.dto.response;
import com.carebridge.backend.content.entity.ReportTargetType;
import java.time.Instant;
import java.util.UUID;
public record EscalationItemResponse(UUID actionId, UUID reportId, UUID targetUserId,
        ReportTargetType targetType, UUID moderatorUserId, String reason, Instant escalatedAt) { }

package com.carebridge.backend.identity.admin.dto.response;

import com.carebridge.backend.identity.admin.entity.AccountLockAppealStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AccountLockAppealResponse(
        UUID id,
        UUID userId,
        String userName,
        String userEmail,
        UUID lockEpisodeId,
        String lockReason,
        String reason,
        AccountLockAppealStatus status,
        Instant submittedAt,
        UUID reviewedBy,
        Instant reviewedAt,
        String reviewNote) {
}

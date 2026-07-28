package com.carebridge.backend.identity.admin.dto.response;

import com.carebridge.backend.audit.entity.AuditAction;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Identity-governance audit item for a specific target user. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserActivityResponse {
    private UUID id;
    private UUID actorUserId;
    private AuditAction action;
    private Instant timestamp;
    private String details;
}

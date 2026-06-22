package com.carebridge.backend.audit.dto.response;

import com.carebridge.backend.audit.entity.AuditAction;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private UUID id;
    private Instant timestamp;
    private UUID userId;
    private AuditAction action;
    private String resourceType;
    private String resourceId;
    private String details;
}

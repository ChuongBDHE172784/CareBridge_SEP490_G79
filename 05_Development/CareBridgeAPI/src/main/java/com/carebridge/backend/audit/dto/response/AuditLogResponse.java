package com.carebridge.backend.audit.dto.response;

import com.carebridge.backend.audit.entity.AuditAction;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private java.util.UUID id;
    private Instant timestamp;
    private java.util.UUID userId;
    private String actorName;
    private String actorEmail;
    private AuditAction action;
    private String resourceType;
    private java.util.UUID resourceId;
    private String details;
}

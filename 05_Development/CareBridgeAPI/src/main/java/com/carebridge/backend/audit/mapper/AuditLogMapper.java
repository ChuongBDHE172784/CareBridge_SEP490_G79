package com.carebridge.backend.audit.mapper;

import com.carebridge.backend.audit.dto.response.AuditLogResponse;
import com.carebridge.backend.audit.entity.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLog log) {
        return toResponse(log, null, null);
    }

    public AuditLogResponse toResponse(AuditLog log, String actorName, String actorEmail) {
        if (log == null) {
            return null;
        }
        return AuditLogResponse.builder()
                .id(log.getAuditLogId())
                .timestamp(log.getCreatedAt())
                .userId(log.getActorUserId())
                .actorName(actorName)
                .actorEmail(actorEmail)
                .action(log.getAction())
                .resourceType(log.getEntityType())
                .resourceId(log.getEntityId())
                .details(log.getNewValueJson())
                .build();
    }
}

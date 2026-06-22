package com.carebridge.backend.audit.mapper;

import com.carebridge.backend.audit.dto.response.AuditLogResponse;
import com.carebridge.backend.audit.entity.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogResponse toResponse(AuditLog log) {
        if (log == null) {
            return null;
        }
        return AuditLogResponse.builder()
                .id(log.getId())
                .timestamp(log.getTimestamp())
                .userId(log.getUserId())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .details(log.getDetails())
                .build();
    }
}

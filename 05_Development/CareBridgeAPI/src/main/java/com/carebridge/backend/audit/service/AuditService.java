package com.carebridge.backend.audit.service;

import com.carebridge.backend.audit.dto.response.AuditLogResponse;
import com.carebridge.backend.audit.entity.AuditAction;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {

    void log(AuditAction action, java.util.UUID userId, String resourceType, String resourceId, Object details);

    void log(AuditAction action, java.util.UUID userId, String resourceType, String resourceId,
             Object details, String reasonCode, java.util.UUID correlationId);

    void log(AuditAction action, String userId, String resourceId, Object details);

    /** Writes a typed mutation audit or throws before the business transaction can commit. */
    void logRequired(RequiredAuditEvent event);

    Page<AuditLogResponse> search(java.util.UUID userId, AuditAction action, Instant fromDate, Instant toDate, Pageable pageable);
}

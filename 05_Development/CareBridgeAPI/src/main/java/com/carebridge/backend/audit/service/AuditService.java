package com.carebridge.backend.audit.service;

import com.carebridge.backend.audit.dto.response.AuditLogResponse;
import com.carebridge.backend.audit.entity.AuditAction;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {

    void log(AuditAction action, java.util.UUID userId, String resourceType, String resourceId, Object details);

    void log(AuditAction action, String userId, String resourceId, Object details);

    Page<AuditLogResponse> search(java.util.UUID userId, AuditAction action, Instant fromDate, Instant toDate, Pageable pageable);
}

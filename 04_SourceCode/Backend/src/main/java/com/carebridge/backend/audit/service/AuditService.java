package com.carebridge.backend.audit.service;

import com.carebridge.backend.audit.dto.response.AuditLogResponse;
import com.carebridge.backend.audit.entity.AuditAction;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {

    void log(AuditAction action, Long userId, String resourceType, String resourceId, Object details);

    void log(AuditAction action, String userId, String resourceId, Object details);

    Page<AuditLogResponse> search(Long userId, AuditAction action, Instant fromDate, Instant toDate, Pageable pageable);
}

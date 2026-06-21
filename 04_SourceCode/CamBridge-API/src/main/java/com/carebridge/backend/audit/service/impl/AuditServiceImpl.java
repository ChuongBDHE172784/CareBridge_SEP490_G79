package com.carebridge.backend.audit.service.impl;

import com.carebridge.backend.audit.dto.response.AuditLogResponse;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.mapper.AuditLogMapper;
import com.carebridge.backend.audit.policy.AuditEligibilityPolicy;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.audit.service.AuditService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final AuditEligibilityPolicy auditEligibilityPolicy;

    @Override
    public void log(AuditAction action, UUID userId, String resourceType, String resourceId, Object details) {
        if (!auditEligibilityPolicy.shouldAudit(action)) {
            return;
        }
        AuditLog log = AuditLog.builder()
                .createdAt(Instant.now())
                .actorUserId(userId)
                .action(action)
                .entityType(resourceType)
                .entityId(parseUuid(resourceId))
                .newValueJson(toJson(details))
                .build();
        auditLogRepository.save(log);
    }

    @Override
    public void log(AuditAction action, String userId, String resourceId, Object details) {
        UUID parsedUserId = userId == null ? null : UUID.fromString(userId);
        log(action, parsedUserId, null, resourceId, details);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
            UUID userId,
            AuditAction action,
            Instant fromDate,
            Instant toDate,
            Pageable pageable) {
        return auditLogRepository.search(userId, action, fromDate, toDate, pageable)
                .map(auditLogMapper::toResponse);
    }

    private String toJson(Object details) {
        if (details == null) {
            return null;
        }
        return String.valueOf(details);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

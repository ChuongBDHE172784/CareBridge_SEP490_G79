package com.carebridge.backend.audit.service.impl;

import com.carebridge.backend.audit.dto.response.AuditLogResponse;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.AuditLog;
import com.carebridge.backend.audit.mapper.AuditLogMapper;
import com.carebridge.backend.audit.policy.AuditEligibilityPolicy;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.audit.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final AuditEligibilityPolicy auditEligibilityPolicy;
    private final ObjectMapper objectMapper;

    @Override
    public void log(AuditAction action, java.util.UUID userId, String resourceType, String resourceId, Object details) {
        if (!auditEligibilityPolicy.shouldAudit(action)) {
            return;
        }
        AuditLog log = AuditLog.builder()
                .createdAt(Instant.now())
                .actorUserId(userId)
                .action(action)
                .entityType(resourceType)
                .entityId(resourceId == null ? null : java.util.UUID.fromString(resourceId))
                .newValueJson(toJson(details))
                .build();
        auditLogRepository.save(log);
    }

    @Override
    public void log(AuditAction action, String userId, String resourceId, Object details) {
        java.util.UUID parsedUserId = userId == null ? null : java.util.UUID.fromString(userId);
        log(action, parsedUserId, null, resourceId, details);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
            java.util.UUID userId,
            AuditAction action,
            Instant fromDate,
            Instant toDate,
            Pageable pageable) {
        return auditLogRepository.search(userId, action, fromDate, toDate, pageable)
                .map(auditLogMapper::toResponse);
    }

    private String toJson(Object details) {
        if (details == null) return null;
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            log.warn("AuditService: could not serialize details to JSON — storing as null: {}", e.getMessage());
            return null;
        }
    }
}

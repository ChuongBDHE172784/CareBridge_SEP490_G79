package com.carebridge.backend.audit.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.entity.SecurityEvent;
import com.carebridge.backend.audit.entity.SecurityEventType;
import com.carebridge.backend.audit.repository.SecurityEventRepository;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.audit.service.SecurityEventService;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SecurityEventServiceImpl implements SecurityEventService {

    private final SecurityEventRepository securityEventRepository;
    private final AuditService auditService;

    @Override
    public void log(SecurityEventType eventType, Long userId, String ipAddress, Object details) {
        SecurityEvent event = SecurityEvent.builder()
                .timestamp(Instant.now())
                .eventType(eventType)
                .userId(userId)
                .ipAddress(ipAddress)
                .details(toJson(details))
                .build();
        SecurityEvent saved = securityEventRepository.save(event);
        auditService.log(
                AuditAction.SECURITY_EVENT,
                userId,
                "SecurityEvent",
                saved.getId().toString(),
                Map.of("eventType", eventType.name()));
    }

    private String toJson(Object details) {
        if (details == null) {
            return null;
        }
        return String.valueOf(details);
    }
}

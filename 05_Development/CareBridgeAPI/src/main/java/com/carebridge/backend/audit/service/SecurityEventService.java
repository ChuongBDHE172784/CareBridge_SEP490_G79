package com.carebridge.backend.audit.service;

import com.carebridge.backend.audit.entity.SecurityEventType;

public interface SecurityEventService {

    void log(SecurityEventType eventType, java.util.UUID userId, String ipAddress, Object details);
}

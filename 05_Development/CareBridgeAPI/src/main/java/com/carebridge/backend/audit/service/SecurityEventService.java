package com.carebridge.backend.audit.service;

import com.carebridge.backend.audit.entity.SecurityEventType;

public interface SecurityEventService {

    void log(SecurityEventType eventType, Long userId, String ipAddress, Object details);
}

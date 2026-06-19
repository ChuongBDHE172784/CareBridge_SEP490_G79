package com.carebridge.backend.audit.service;

import com.carebridge.backend.audit.entity.SecurityEventType;
import java.util.UUID;

public interface SecurityEventService {

    void log(SecurityEventType eventType, UUID userId, String ipAddress, Object details);
}

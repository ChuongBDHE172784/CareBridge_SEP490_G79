package com.carebridge.backend.emergency.service;

import java.util.UUID;

public interface SmsFallbackPort {

    void sendFallback(UUID userId, UUID sessionId, String message);
}

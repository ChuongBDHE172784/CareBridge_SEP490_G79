package com.carebridge.backend.identity.service;

import com.carebridge.backend.identity.dto.SessionInfo;
import java.util.List;
import java.util.UUID;

public interface SessionService {
    List<SessionInfo> getActiveSessions(UUID userId);
    SessionInfo getCurrentSession();
    void revokeSession(UUID sessionId, UUID requestingUserId, String ipAddress);
    void logout(String refreshToken, UUID userId, String ipAddress);
    void updateLastActivity(String token, String ipAddress);
}

package com.carebridge.backend.identity.service;

import com.carebridge.backend.identity.dto.SessionInfo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SessionService {
    List<SessionInfo> getActiveSessions(UUID userId);
    Page<SessionInfo> getActiveSessionsPaged(UUID userId, Pageable pageable);
    SessionInfo getCurrentSession();
    void revokeSession(UUID sessionId, UUID requestingUserId, String ipAddress);
    int revokeAllExceptCurrent(UUID userId, String ipAddress);
    void logout(String refreshToken, UUID userId, String ipAddress);
    void updateLastActivity(String token, String ipAddress);
}

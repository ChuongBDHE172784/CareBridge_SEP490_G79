package com.carebridge.backend.identity.service.impl;

import com.carebridge.backend.identity.dto.SessionInfo;
import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.identity.service.SessionService;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements SessionService {

    private final UserSessionRepository sessionRepository;
    private final JwtTokenProvider tokenProvider;

    private static final long INACTIVE_THRESHOLD_DAYS = 30;
    private static final long INACTIVE_THRESHOLD_SECONDS = INACTIVE_THRESHOLD_DAYS * 24 * 60 * 60;

    @Override
    @Transactional(readOnly = true)
    public List<SessionInfo> getActiveSessions(UUID userId) {
        List<UserSession> sessions = sessionRepository.findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(userId);
        Instant now = Instant.now();
        String currentToken = extractCurrentToken();

        return sessions.stream().map(session -> {
            SessionInfo.SessionInfoBuilder builder = SessionInfo.builder()
                .sessionId(session.getSessionId())
                .deviceName(session.getDeviceName())
                .browser(session.getBrowser())
                .ipAddress(session.getIpAddress())
                .location(session.getLocation())
                .lastActivityAt(session.getLastActivityAt())
                .isCurrent(currentToken != null && session.getRefreshTokenHash() != null &&
                          tokenProvider.validateToken(currentToken) &&
                          session.getRefreshTokenHash().equals(currentToken));

            // Determine status
            if (session.isRevoked()) {
                builder.status("revoked");
            } else if (session.getLastActivityAt() != null &&
                       session.getLastActivityAt().isBefore(now.minusSeconds(INACTIVE_THRESHOLD_SECONDS))) {
                builder.status("inactive");
            } else if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
                builder.status("expired");
            } else {
                builder.status("active");
            }

            return builder.build();
        }).toList();
    }

    @Override
    public SessionInfo getCurrentSession() {
        String token = extractCurrentToken();
        if (token == null) {
            return null;
        }
        UserSession session = sessionRepository.findByRefreshTokenHashAndRevokedFalse(token)
            .orElse(null);
        if (session == null) {
            return null;
        }
        return mapToSessionInfo(session, true);
    }

    @Override
    @Transactional
    public void revokeSession(UUID sessionId, UUID requestingUserId, String ipAddress) {
        UserSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!session.getUserId().equals(requestingUserId)) {
            throw new IllegalArgumentException("Cannot revoke another user's session");
        }

        int updated = sessionRepository.revokeSession(sessionId, requestingUserId, Instant.now());
        if (updated == 0) {
            throw new IllegalStateException("Failed to revoke session");
        }

        log.info("Session revoked: sessionId={}, userId={}, ip={}", sessionId, requestingUserId, ipAddress);
    }

    @Override
    @Transactional
    public void updateLastActivity(String token, String ipAddress) {
        UserSession session = sessionRepository.findByRefreshTokenHashAndRevokedFalse(token)
            .orElse(null);
        if (session != null) {
            Instant now = Instant.now();
            sessionRepository.updateActivity(now, ipAddress, token);
            session.setUpdatedAt(now);
            log.debug("Updated last activity for session: tokenHash={}, ip={}",
                     token.substring(0, Math.min(token.length(), 16)), ipAddress);
        }
    }

    private String extractCurrentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object credentials = authentication.getCredentials();
        if (credentials instanceof String) {
            return (String) credentials;
        }
        // Try to extract from principal if credentials is not token
        return null;
    }

    private SessionInfo mapToSessionInfo(UserSession session, boolean isCurrent) {
        Instant now = Instant.now();
        String status;
        if (session.isRevoked()) {
            status = "revoked";
        } else if (session.getLastActivityAt() != null &&
                   session.getLastActivityAt().isBefore(now.minusSeconds(INACTIVE_THRESHOLD_SECONDS))) {
            status = "inactive";
        } else if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
            status = "expired";
        } else {
            status = "active";
        }

        return SessionInfo.builder()
            .sessionId(session.getSessionId())
            .deviceName(session.getDeviceName())
            .browser(session.getBrowser())
            .ipAddress(session.getIpAddress())
            .location(session.getLocation())
            .lastActivityAt(session.getLastActivityAt())
            .isCurrent(isCurrent)
            .status(status)
            .build();
    }
}

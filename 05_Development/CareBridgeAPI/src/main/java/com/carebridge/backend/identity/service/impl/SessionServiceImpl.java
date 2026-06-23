package com.carebridge.backend.identity.service.impl;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.identity.dto.SessionInfo;
import com.carebridge.backend.identity.entity.TokenBlacklist;
import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.TokenBlacklistRepository;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.identity.service.SessionService;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
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
    private final AuditService auditService;
    private final TokenBlacklistRepository tokenBlacklistRepository;

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

        // Check if already revoked (race condition protection)
        if (session.isRevoked()) {
            throw new IllegalArgumentException("Session is already revoked");
        }

        // Check if trying to revoke current session
        String currentToken = extractCurrentToken();
        if (currentToken != null && session.getRefreshTokenHash() != null) {
            String currentTokenHash = hashToken(currentToken);
            if (currentTokenHash.equals(session.getRefreshTokenHash())) {
                throw new IllegalArgumentException("Please use Logout to sign out from this device");
            }
        }

        int updated = sessionRepository.revokeSession(sessionId, requestingUserId, Instant.now());
        if (updated == 0) {
            // Race condition: session was revoked by another thread between our check and update
            // Re-fetch to verify current state
            UserSession currentState = sessionRepository.findById(sessionId).orElse(null);
            if (currentState != null && currentState.isRevoked()) {
                throw new IllegalArgumentException("Session is already revoked");
            }
            throw new IllegalStateException("Failed to revoke session - please try again");
        }

        // Add token to blacklist
        if (session.getRefreshTokenHash() != null && session.getExpiresAt() != null) {
            TokenBlacklist blacklistEntry = TokenBlacklist.builder()
                    .tokenHash(session.getRefreshTokenHash())
                    .expiresAt(session.getExpiresAt())
                    .revokedAt(Instant.now())
                    .reason("session_revoke")
                    .build();
            tokenBlacklistRepository.save(blacklistEntry);
        } else {
            if (session.getRefreshTokenHash() == null) {
                log.warn("Session has null refreshTokenHash - cannot blacklist: sessionId={}", sessionId);
            }
        }

        auditService.log(
            com.carebridge.backend.audit.entity.AuditAction.SESSION_REVOKED,
            requestingUserId,
            "UserSession",
            sessionId.toString(),
            "Session revoked by user from IP: " + ipAddress
        );

        log.info("Session revoked: sessionId={}, userId={}, ip={}", sessionId, requestingUserId, ipAddress);
    }

    @Override
    @Transactional
    public void logout(String refreshToken, UUID userId, String ipAddress) {
        // If no token provided, treat as already logged out (idempotent)
        if (refreshToken == null) {
            log.debug("Logout called with null token - treating as already logged out");
            return;
        }

        // Find the active session by token hash
        UserSession session = sessionRepository.findByRefreshTokenHashAndRevokedFalse(refreshToken)
            .orElse(null);

        // If session not found (already revoked/expired), treat as success - idempotent
        if (session == null) {
            log.debug("Logout called for non-existent or already revoked session: tokenHashPrefix={}",
                      refreshToken.substring(0, Math.min(refreshToken.length(), 16)));
            return;
        }

        // Check if already revoked (handles race condition where another thread revoked between find and update)
        if (session.isRevoked()) {
            log.debug("Session already revoked (concurrent logout): sessionId={}", session.getSessionId());
            return;
        }

        // Mark session as revoked using existing revokeSession method
        int updated = sessionRepository.revokeSession(session.getSessionId(), userId, Instant.now());
        if (updated == 0) {
            // Race condition: session was revoked by another thread after our check
            log.debug("Session already revoked by concurrent request: sessionId={}", session.getSessionId());
            return;
        }

        // Add to token blacklist with reason="logout"
        if (session.getRefreshTokenHash() != null && session.getExpiresAt() != null) {
            TokenBlacklist blacklistEntry = TokenBlacklist.builder()
                    .tokenHash(session.getRefreshTokenHash())
                    .expiresAt(session.getExpiresAt())
                    .revokedAt(Instant.now())
                    .reason("logout")
                    .build();
            tokenBlacklistRepository.save(blacklistEntry);
        } else {
            if (session.getRefreshTokenHash() == null) {
                log.warn("Session has null refreshTokenHash - cannot blacklist: sessionId={}", session.getSessionId());
            }
        }

        // Audit log
        auditService.log(
            com.carebridge.backend.audit.entity.AuditAction.LOGOUT,
            userId,
            "UserSession",
            session.getSessionId().toString(),
            "User logged out from IP: " + ipAddress
        );

        log.info("User logged out: userId={}, sessionId={}, ip={}", userId, session.getSessionId(), ipAddress);

        // Clear security context to ensure no token remains
        SecurityContextHolder.clearContext();
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

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
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

package com.carebridge.backend.identity.service.impl;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.identity.dto.SessionInfo;
import com.carebridge.backend.identity.entity.TokenBlacklist;
import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.TokenBlacklistRepository;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.security.jwt.JwtAuthenticationToken;
import com.carebridge.backend.security.repository.RefreshTokenRepository;
import com.carebridge.backend.security.util.TokenUtils;
import com.carebridge.backend.identity.service.SessionService;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final RefreshTokenRepository refreshTokenRepository;

    private static final long INACTIVE_THRESHOLD_DAYS = 30;
    private static final long INACTIVE_THRESHOLD_SECONDS = INACTIVE_THRESHOLD_DAYS * 24 * 60 * 60;

    @Override
    @Transactional(readOnly = true)
    public List<SessionInfo> getActiveSessions(UUID userId) {
        List<UserSession> sessions = sessionRepository.findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(userId);
        Instant now = Instant.now();

        // Get current session ID from JWT sid claim
        UUID currentSessionId = extractCurrentSessionId();

        return sessions.stream().map(session -> {
            SessionInfo.SessionInfoBuilder builder = SessionInfo.builder()
                .sessionId(session.getSessionId())
                .deviceName(session.getDeviceName())
                .browser(session.getBrowser())
                .ipAddress(session.getIpAddress())
                .location(session.getLocation())
                .lastActivityAt(session.getLastActivityAt())
                .isCurrent(currentSessionId != null && currentSessionId.equals(session.getSessionId()));

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
    @Transactional(readOnly = true)
    public Page<SessionInfo> getActiveSessionsPaged(UUID userId, Pageable pageable) {
        UUID currentSessionId = extractCurrentSessionId();
        Instant now = Instant.now();
        return sessionRepository.findByUserIdAndRevokedFalse(userId, pageable)
                .map(session -> {
                    SessionInfo.SessionInfoBuilder builder = SessionInfo.builder()
                            .sessionId(session.getSessionId())
                            .deviceName(session.getDeviceName())
                            .browser(session.getBrowser())
                            .ipAddress(session.getIpAddress())
                            .location(session.getLocation())
                            .lastActivityAt(session.getLastActivityAt())
                            .isCurrent(currentSessionId != null && currentSessionId.equals(session.getSessionId()));

                    if (session.getLastActivityAt() != null &&
                            session.getLastActivityAt().isBefore(now.minusSeconds(INACTIVE_THRESHOLD_SECONDS))) {
                        builder.status("inactive");
                    } else if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
                        builder.status("expired");
                    } else {
                        builder.status("active");
                    }
                    return builder.build();
                });
    }

    @Override
    @Transactional
    public int revokeAllExceptCurrent(UUID userId, String ipAddress) {
        UUID currentSessionId = extractCurrentSessionId();
        if (currentSessionId == null) {
            throw new IllegalStateException("No current session found");
        }
        int revoked = sessionRepository.revokeAllExceptSession(userId, currentSessionId, Instant.now());
        if (revoked > 0) {
            auditService.log(
                    com.carebridge.backend.audit.entity.AuditAction.SESSION_REVOKED,
                    userId,
                    "UserSession",
                    "all_except_current",
                    "Revoked " + revoked + " session(s) from IP: " + ipAddress
            );
            log.info("Revoked {} session(s) for userId={}, kept sessionId={}", revoked, userId, currentSessionId);
        }
        return revoked;
    }

    private UUID extractCurrentSessionId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        // If using JwtAuthenticationToken, get session ID directly
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getSessionId();
        }
        // Fallback: extract from credentials if it's a JWT token
        if (auth.getCredentials() instanceof String token) {
            return tokenProvider.getSessionId(token);
        }
        return null;
    }

    @Override
    public SessionInfo getCurrentSession() {
        String token = extractCurrentToken();
        if (token == null) {
            return null;
        }
        String tokenHash = TokenUtils.hashSha256(token);
        UserSession session = sessionRepository.findByRefreshTokenHashAndRevokedFalse(tokenHash)
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

        // Check if trying to revoke current session using sid claim
        UUID currentSessionId = extractCurrentSessionId();
        if (currentSessionId != null && currentSessionId.equals(session.getSessionId())) {
            throw new IllegalArgumentException("Please use Logout to sign out from this device");
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
        // If no refresh token provided, try to logout current session via sid claim
        if (refreshToken == null || refreshToken.isBlank()) {
            UUID currentSessionId = extractCurrentSessionId();
            if (currentSessionId != null) {
                UserSession session = sessionRepository.findById(currentSessionId).orElse(null);
                if (session != null && !session.isRevoked() && session.getUserId().equals(userId)) {
                    // Revoke the session
                    int updated = sessionRepository.revokeSession(currentSessionId, userId, Instant.now());
                    if (updated > 0) {
                        // Blacklist the refresh token hash if available
                        if (session.getRefreshTokenHash() != null && session.getExpiresAt() != null) {
                            TokenBlacklist blacklistEntry = TokenBlacklist.builder()
                                    .tokenHash(session.getRefreshTokenHash())
                                    .expiresAt(session.getExpiresAt())
                                    .revokedAt(Instant.now())
                                    .reason("logout_sid")
                                    .build();
                            tokenBlacklistRepository.save(blacklistEntry);
                        }
                        // Revoke any matching refresh token row
                        if (session.getRefreshTokenHash() != null) {
                            refreshTokenRepository.revokeByTokenHashAndUserId(session.getRefreshTokenHash(), userId);
                        }
                        auditService.log(
                            com.carebridge.backend.audit.entity.AuditAction.LOGOUT,
                            userId,
                            "UserSession",
                            currentSessionId.toString(),
                            "User logged out current session via SID from IP: " + ipAddress
                        );
                        log.info("User logged out via SID: userId={}, sessionId={}, ip={}", userId, currentSessionId, ipAddress);
                    }
                }
            }
            // Always clear security context
            SecurityContextHolder.clearContext();
            return;
        }

        // Hash the raw refresh token for database lookup
        String tokenHash = TokenUtils.hashSha256(refreshToken);

        // Find the active session by token hash
        UserSession session = sessionRepository.findByRefreshTokenHashAndRevokedFalse(tokenHash)
            .orElse(null);

        // If session not found (already revoked/expired), treat as success - idempotent
        if (session == null) {
            log.debug("Logout called for non-existent or already revoked session: tokenHashPrefix={}",
                      tokenHash.substring(0, Math.min(tokenHash.length(), 16)));
            // Still try to revoke any matching refresh token row if present
            revokeRefreshTokenByHash(tokenHash, userId);
            return;
        }

        // Verify ownership: session must belong to the authenticated user
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Cannot logout from another user's session");
        }

        // Check if already revoked (handles race condition where another thread revoked between find and update)
        if (session.isRevoked()) {
            log.debug("Session already revoked (concurrent logout): sessionId={}", session.getSessionId());
            revokeRefreshTokenByHash(tokenHash, userId);
            return;
        }

        // Mark session as revoked
        int updated = sessionRepository.revokeSession(session.getSessionId(), userId, Instant.now());
        if (updated == 0) {
            // Race condition: session was revoked by another thread after our check
            log.debug("Session already revoked by concurrent request: sessionId={}", session.getSessionId());
            revokeRefreshTokenByHash(tokenHash, userId);
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
            log.warn("Session has null refreshTokenHash - cannot blacklist: sessionId={}", session.getSessionId());
        }

        // Revoke the matching refresh token row
        revokeRefreshTokenByHash(tokenHash, userId);

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

    private void revokeRefreshTokenByHash(String tokenHash, UUID userId) {
        // Find and revoke any refresh token with this hash belonging to the user
        int revokedCount = refreshTokenRepository.revokeByTokenHashAndUserId(tokenHash, userId);
        if (revokedCount > 0) {
            log.debug("Revoked {} refresh token(s) for userId={}", revokedCount, userId);
        }
    }

    @Override
    @Transactional
    public void updateLastActivity(String token, String ipAddress) {
        String tokenHash = TokenUtils.hashSha256(token);
        UserSession session = sessionRepository.findByRefreshTokenHashAndRevokedFalse(tokenHash)
            .orElse(null);
        if (session != null) {
            Instant now = Instant.now();
            sessionRepository.updateActivity(now, ipAddress, tokenHash);
            session.setUpdatedAt(now);
            log.debug("Updated last activity for session: tokenHashPrefix={}, ip={}",
                     tokenHash.substring(0, Math.min(tokenHash.length(), 16)), ipAddress);
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

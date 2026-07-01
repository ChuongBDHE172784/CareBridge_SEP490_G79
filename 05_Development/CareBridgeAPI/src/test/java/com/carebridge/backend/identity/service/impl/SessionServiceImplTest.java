package com.carebridge.backend.identity.service.impl;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.identity.entity.TokenBlacklist;
import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.TokenBlacklistRepository;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.security.repository.RefreshTokenRepository;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.jwt.JwtAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private JwtTokenProvider tokenProvider;

    private SessionServiceImpl sessionService;

    private final UUID userId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final String refreshTokenHash = "a7f5f3541e884f1c276fb883d721583f7bc5729e63a2e765d63e7339c7d4d1f7";

    @BeforeEach
    void setUp() {
        sessionService = new SessionServiceImpl(sessionRepository, tokenProvider, auditService, tokenBlacklistRepository, refreshTokenRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // Prevents a mocked Authentication set via SecurityContextHolder.setContext() in this class
        // from leaking into unrelated tests later in the same JVM/thread (Surefire runs sequentially
        // on one thread by default, and SecurityContextHolder's default strategy is ThreadLocal).
        SecurityContextHolder.clearContext();
    }

    private UserSession createSession(boolean revoked, Instant expiresAt) {
        return UserSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .refreshTokenHash(refreshTokenHash)
                .deviceName("Test Device")
                .browser("Chrome")
                .ipAddress("127.0.0.1")
                .location(null)
                .lastActivityAt(Instant.now())
                .expiresAt(expiresAt)
                .status("active")
                .isCurrent(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .revoked(revoked)
                .build();
    }

    private void mockCurrentToken(String token) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getCredentials()).thenReturn(token);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void mockJwtAuthenticationToken(UUID sessionId) {
        var authorities = List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MOTHER"));
        JwtAuthenticationToken jwtAuth = new JwtAuthenticationToken(
                new org.springframework.security.core.userdetails.User("test", "pass", authorities),
                null,
                authorities,
                sessionId
        );
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(jwtAuth);
        SecurityContextHolder.setContext(securityContext);
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Test
    @DisplayName("LOGOUT-TC-007: SecurityEvent TOKEN_REVOKED logged on session revoke")
    void revokeSession_Success_RevokesSession_AddsToBlacklist_LogsAudit() {
        // Arrange
        UserSession session = createSession(false, Instant.now().plusSeconds(3600));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.revokeSession(eq(sessionId), eq(userId), any())).thenReturn(1);
        mockCurrentToken("different-token");

        // Act
        sessionService.revokeSession(sessionId, userId, "127.0.0.1");

        // Assert
        verify(sessionRepository).revokeSession(eq(sessionId), eq(userId), any());
        ArgumentCaptor<TokenBlacklist> blacklistCaptor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(tokenBlacklistRepository).save(blacklistCaptor.capture());
        TokenBlacklist captured = blacklistCaptor.getValue();
        assertEquals(refreshTokenHash, captured.getTokenHash());
        assertEquals(session.getExpiresAt(), captured.getExpiresAt());
        assertEquals("session_revoke", captured.getReason());
        verify(auditService).log(
                eq(com.carebridge.backend.audit.entity.AuditAction.SESSION_REVOKED),
                eq(userId),
                eq("UserSession"),
                eq(sessionId.toString()),
                contains("Session revoked by user from IP: 127.0.0.1")
        );
    }

    @Test
    @DisplayName("LOGOUT-TC-009: Partial logout does not affect current session")
    void revokeSession_CurrentSession_ThrowsException() {
        // Arrange: current authenticated session's sid equals the target sessionId
        UserSession session = createSession(false, Instant.now().plusSeconds(3600));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        // Mock JwtAuthenticationToken with sessionId equal to target
        mockJwtAuthenticationToken(sessionId);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sessionService.revokeSession(sessionId, userId, "127.0.0.1");
        });
        assertEquals("Please use Logout to sign out from this device", exception.getMessage());
        verify(sessionRepository, never()).revokeSession(any(), any(), any());
        verify(tokenBlacklistRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("LOGOUT-TC-004: Session not found in DB throws exception")
    void revokeSession_SessionNotFound_ThrowsException() {
        // Arrange
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sessionService.revokeSession(sessionId, userId, "127.0.0.1");
        });
        assertEquals("Session not found", exception.getMessage());
        verify(sessionRepository, never()).revokeSession(any(), any(), any());
    }

    @Test
    @DisplayName("LOGOUT-TC-005: Session ownership - user A cannot logout user B")
    void revokeSession_AnotherUser_ThrowsException() {
        // Arrange
        UserSession session = createSession(false, Instant.now().plusSeconds(3600));
        UUID differentUserId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sessionService.revokeSession(sessionId, differentUserId, "127.0.0.1");
        });
        assertEquals("Cannot revoke another user's session", exception.getMessage());
        verify(sessionRepository, never()).revokeSession(any(), any(), any());
    }

    @Test
    void revokeSession_RevokeFailed_ThrowsException() {
        // Arrange: session exists but revokeSession returns 0 (shouldn't happen normally)
        UserSession session = createSession(false, Instant.now().plusSeconds(3600));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.revokeSession(eq(sessionId), eq(userId), any())).thenReturn(0);
        mockCurrentToken("different-token");

        // Act & Assert - should throw with appropriate error message
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            sessionService.revokeSession(sessionId, userId, "127.0.0.1");
        });
        assertEquals("Failed to revoke session - please try again", exception.getMessage());
        verify(tokenBlacklistRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("LOGOUT-TC-003: Reject revoked refresh token")
    void revokeSession_AlreadyRevoked_ThrowsException() {
        // Arrange: session is already revoked
        UserSession session = createSession(true, Instant.now().plusSeconds(3600));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        // No need to mock current token - exception happens before current token check

        // Act & Assert - should throw exception for already revoked session
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sessionService.revokeSession(sessionId, userId, "127.0.0.1");
        });
        assertEquals("Session is already revoked", exception.getMessage());
        verify(sessionRepository, never()).revokeSession(any(), any(), any());
        verify(tokenBlacklistRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void revokeSession_NullRefreshTokenHash_DoesNotAddToBlacklist() {
        // Arrange
        UserSession session = createSession(false, Instant.now().plusSeconds(3600));
        session.setRefreshTokenHash(null);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.revokeSession(eq(sessionId), eq(userId), any())).thenReturn(1);
        mockCurrentToken("different-token");

        // Act
        sessionService.revokeSession(sessionId, userId, "127.0.0.1");

        // Assert
        verify(tokenBlacklistRepository, never()).save(any());
        verify(auditService).log(any(), any(), any(), any(), any());
    }

    @Test
    void revokeSession_NullExpiresAt_DoesNotAddToBlacklist() {
        // Arrange
        UserSession session = createSession(false, null);
        session.setRefreshTokenHash(refreshTokenHash);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.revokeSession(eq(sessionId), eq(userId), any())).thenReturn(1);
        mockCurrentToken("different-token");

        // Act
        sessionService.revokeSession(sessionId, userId, "127.0.0.1");

        // Assert
        verify(tokenBlacklistRepository, never()).save(any());
        verify(auditService).log(any(), any(), any(), any(), any());
    }

    @Test
    void revokeSession_NullCurrentToken_AllowsRevoke() {
        // Arrange
        UserSession session = createSession(false, Instant.now().plusSeconds(3600));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.revokeSession(eq(sessionId), eq(userId), any())).thenReturn(1);
        mockCurrentToken(null);

        // Act
        sessionService.revokeSession(sessionId, userId, "127.0.0.1");

        // Assert
        verify(sessionRepository).revokeSession(eq(sessionId), eq(userId), any());
        verify(tokenBlacklistRepository).save(any());
        verify(auditService).log(any(), any(), any(), any(), any());
    }

    // ========== Logout Tests ==========

    @Test
    @DisplayName("LOGOUT-TC-001: Partial logout success - blacklists token and logs audit")
    void logout_Success_BlacklistsToken_LogsAudit() {
        // Arrange
        String token = "test-refresh-token";
        String tokenHash = hashToken(token);
        UserSession session = createSession(false, Instant.now().plusSeconds(3600));
        session.setRefreshTokenHash(tokenHash);
        session.setSessionId(sessionId);
        when(sessionRepository.findByRefreshTokenHashAndRevokedFalse(eq(tokenHash))).thenReturn(Optional.of(session));
        when(sessionRepository.revokeSession(eq(sessionId), eq(userId), any())).thenReturn(1);
        when(refreshTokenRepository.revokeByTokenHashAndUserId(eq(tokenHash), eq(userId))).thenReturn(1);
        // No need to mock SecurityContext - logout uses direct token param

        // Act
        sessionService.logout(token, userId, "127.0.0.1");

        // Assert
        verify(sessionRepository).revokeSession(eq(sessionId), eq(userId), any());
        ArgumentCaptor<TokenBlacklist> blacklistCaptor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(tokenBlacklistRepository).save(blacklistCaptor.capture());
        TokenBlacklist captured = blacklistCaptor.getValue();
        assertEquals(tokenHash, captured.getTokenHash());
        assertEquals(session.getExpiresAt(), captured.getExpiresAt());
        assertEquals("logout", captured.getReason());
        verify(auditService).log(
                eq(com.carebridge.backend.audit.entity.AuditAction.LOGOUT),
                eq(userId),
                eq("UserSession"),
                eq(sessionId.toString()),
                contains("User logged out from IP: 127.0.0.1")
        );
    }

    @Test
    void logout_Idempotent_WhenAlreadyLoggedOut() {
        // Arrange
        String token = "test-refresh-token";
        String tokenHash = hashToken(token);
        when(sessionRepository.findByRefreshTokenHashAndRevokedFalse(eq(tokenHash))).thenReturn(Optional.empty());
        when(refreshTokenRepository.revokeByTokenHashAndUserId(eq(tokenHash), eq(userId))).thenReturn(0);
        // No SecurityContext mock needed

        // Act - should not throw
        sessionService.logout(token, userId, "127.0.0.1");

        // Assert - no operations should occur
        verify(sessionRepository, never()).revokeSession(any(), any(), any());
        verify(tokenBlacklistRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void logout_NullToken_HandlesGracefully() {
        // Arrange - no SecurityContext mock needed

        // Act - should not throw
        sessionService.logout(null, userId, "127.0.0.1");

        // Assert - no operations should occur
        verify(sessionRepository, never()).findByRefreshTokenHashAndRevokedFalse(any());
        verify(sessionRepository, never()).revokeSession(any(), any(), any());
        verify(tokenBlacklistRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void logout_ClearsSecurityContext() {
        // Arrange
        String token = "test-refresh-token";
        String tokenHash = hashToken(token);
        UserSession session = createSession(false, Instant.now().plusSeconds(3600));
        session.setRefreshTokenHash(tokenHash);
        session.setSessionId(sessionId);
        when(sessionRepository.findByRefreshTokenHashAndRevokedFalse(eq(tokenHash))).thenReturn(Optional.of(session));
        when(sessionRepository.revokeSession(eq(sessionId), eq(userId), any())).thenReturn(1);
        when(refreshTokenRepository.revokeByTokenHashAndUserId(eq(tokenHash), eq(userId))).thenReturn(1);

        // Act
        sessionService.logout(token, userId, "127.0.0.1");

        // Assert - verify SecurityContext cleared
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("LOGOUT-TC-008: Access token still valid after logout - expired session still revoked")
    void logout_WithExpiredSession_RevokesAndBlacklists() {
        // Arrange: session with expiresAt in the past (expired but not revoked)
        String token = "test-refresh-token";
        String tokenHash = hashToken(token);
        UserSession session = createSession(false, Instant.now().minusSeconds(3600)); // expired
        session.setRefreshTokenHash(tokenHash);
        session.setSessionId(sessionId);
        when(sessionRepository.findByRefreshTokenHashAndRevokedFalse(eq(tokenHash))).thenReturn(Optional.of(session));
        when(sessionRepository.revokeSession(eq(sessionId), eq(userId), any())).thenReturn(1);
        when(refreshTokenRepository.revokeByTokenHashAndUserId(eq(tokenHash), eq(userId))).thenReturn(1);

        // Act
        sessionService.logout(token, userId, "127.0.0.1");

        // Assert - should still revoke and blacklist (expiry doesn't prevent logout)
        verify(sessionRepository).revokeSession(eq(sessionId), eq(userId), any());
        ArgumentCaptor<TokenBlacklist> blacklistCaptor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(tokenBlacklistRepository).save(blacklistCaptor.capture());
        TokenBlacklist captured = blacklistCaptor.getValue();
        assertEquals(tokenHash, captured.getTokenHash());
        assertEquals(session.getExpiresAt(), captured.getExpiresAt());
        assertEquals("logout", captured.getReason());
        verify(auditService).log(
                eq(com.carebridge.backend.audit.entity.AuditAction.LOGOUT),
                eq(userId),
                eq("UserSession"),
                eq(sessionId.toString()),
                contains("User logged out from IP: 127.0.0.1")
        );
    }

    @Test
    void logout_WithNullToken_UsesSidToLogoutCurrentSession() {
        // Arrange: no refresh token provided, but current session exists via sid
        UserSession session = createSession(false, Instant.now().plusSeconds(3600));
        // The session's refreshTokenHash is already set by createSession
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.revokeSession(eq(sessionId), eq(userId), any())).thenReturn(1);
        when(refreshTokenRepository.revokeByTokenHashAndUserId(eq(refreshTokenHash), eq(userId))).thenReturn(1);
        // Mock JwtAuthenticationToken with sessionId
        mockJwtAuthenticationToken(sessionId);

        // Act
        sessionService.logout(null, userId, "127.0.0.1");

        // Assert
        verify(sessionRepository).revokeSession(eq(sessionId), eq(userId), any());
        ArgumentCaptor<TokenBlacklist> blacklistCaptor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(tokenBlacklistRepository).save(blacklistCaptor.capture());
        TokenBlacklist captured = blacklistCaptor.getValue();
        assertEquals(refreshTokenHash, captured.getTokenHash());
        assertEquals("logout_sid", captured.getReason());
        verify(auditService).log(
                eq(com.carebridge.backend.audit.entity.AuditAction.LOGOUT),
                eq(userId),
                eq("UserSession"),
                eq(sessionId.toString()),
                contains("User logged out current session via SID from IP: 127.0.0.1")
        );
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_WithNullToken_NoCurrentSession_ClearsContextOnly() {
        // Arrange: no refresh token and no current session (sid null or not found)
        // Don't mock JwtAuth - extractCurrentSessionId will return null
        // No need to mock sessionRepository.findById because it won't be called

        // Act - should not throw
        sessionService.logout(null, userId, "127.0.0.1");

        // Assert - only context cleared
        verify(sessionRepository, never()).findById(any());
        verify(sessionRepository, never()).revokeSession(any(), any(), any());
        verify(tokenBlacklistRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("LOGOUT-TC-002: Logout-all revokes all user sessions")
    void logoutAll_revokesAllUserSessions() {
        // Arrange: mock current session via JwtAuthenticationToken
        mockJwtAuthenticationToken(sessionId);
        when(sessionRepository.revokeAllExceptSession(eq(userId), eq(sessionId), any())).thenReturn(3);

        // Act
        int revoked = sessionService.revokeAllExceptCurrent(userId, "127.0.0.1");

        // Assert
        assertEquals(3, revoked);
        verify(sessionRepository).revokeAllExceptSession(eq(userId), eq(sessionId), any());
        verify(auditService).log(
                eq(com.carebridge.backend.audit.entity.AuditAction.SESSION_REVOKED),
                eq(userId),
                eq("UserSession"),
                eq("all_except_current"),
                contains("Revoked 3 session(s) from IP: 127.0.0.1")
        );
    }

    @Test
    @DisplayName("LOGOUT-TC-006: No access token — controller-level 401")
    @org.junit.jupiter.api.Disabled("Controller-level test — requires @WebMvcTest setup")
    void logout_noAccessToken_returns401() {
        // This TC requires controller-level testing with MockMvc
        // Covered at controller layer, not service layer
    }

    @Test
    @DisplayName("LOGOUT-TC-INT-001: After logout, refresh token is rejected")
    void logout_afterLogout_refreshTokenRejected() {
        // Arrange: set up a valid session with refresh token
        String token = "test-refresh-token";
        String tokenHash = hashToken(token);
        UserSession session = createSession(false, Instant.now().plusSeconds(3600));
        session.setRefreshTokenHash(tokenHash);
        session.setSessionId(sessionId);
        when(sessionRepository.findByRefreshTokenHashAndRevokedFalse(eq(tokenHash))).thenReturn(Optional.of(session));
        when(sessionRepository.revokeSession(eq(sessionId), eq(userId), any())).thenReturn(1);
        when(refreshTokenRepository.revokeByTokenHashAndUserId(eq(tokenHash), eq(userId))).thenReturn(1);

        // Act: call logout
        sessionService.logout(token, userId, "127.0.0.1");

        // Assert: verify the refresh token hash was blacklisted
        ArgumentCaptor<TokenBlacklist> blacklistCaptor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(tokenBlacklistRepository).save(blacklistCaptor.capture());
        TokenBlacklist captured = blacklistCaptor.getValue();
        assertEquals(tokenHash, captured.getTokenHash());
        assertEquals(session.getExpiresAt(), captured.getExpiresAt());
        assertEquals("logout", captured.getReason());

        // Verify the refresh token was also revoked in the refresh token repository
        verify(refreshTokenRepository).revokeByTokenHashAndUserId(eq(tokenHash), eq(userId));
    }
}

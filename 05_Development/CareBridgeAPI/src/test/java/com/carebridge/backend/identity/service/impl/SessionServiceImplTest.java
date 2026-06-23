package com.carebridge.backend.identity.service.impl;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.identity.entity.TokenBlacklist;
import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.TokenBlacklistRepository;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
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
    private AuditService auditService;

    @Mock
    private JwtTokenProvider tokenProvider;

    private SessionServiceImpl sessionService;

    private final UUID userId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final String refreshTokenHash = "a7f5f3541e884f1c276fb883d721583f7bc5729e63a2e765d63e7339c7d4d1f7";

    @BeforeEach
    void setUp() {
        sessionService = new SessionServiceImpl(sessionRepository, tokenProvider, auditService, tokenBlacklistRepository);
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
    void revokeSession_CurrentSession_ThrowsException() {
        // Arrange
        String currentToken = "current-refresh-token";
        String currentTokenHash = hashToken(currentToken);
        UserSession session = createSession(false, Instant.now().plusSeconds(3600));
        session.setRefreshTokenHash(currentTokenHash);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        mockCurrentToken(currentToken);

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
    void logout_Success_BlacklistsToken_LogsAudit() {
        // Arrange
        String token = "test-refresh-token";
        String tokenHash = hashToken(token);
        UserSession session = createSession(false, Instant.now().plusSeconds(3600));
        session.setRefreshTokenHash(tokenHash);
        session.setSessionId(sessionId);
        when(sessionRepository.findByRefreshTokenHashAndRevokedFalse(token)).thenReturn(Optional.of(session));
        when(sessionRepository.revokeSession(eq(sessionId), eq(userId), any())).thenReturn(1);
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
        when(sessionRepository.findByRefreshTokenHashAndRevokedFalse(token)).thenReturn(Optional.empty());
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
        when(sessionRepository.findByRefreshTokenHashAndRevokedFalse(token)).thenReturn(Optional.of(session));
        when(sessionRepository.revokeSession(eq(sessionId), eq(userId), any())).thenReturn(1);

        // Act
        sessionService.logout(token, userId, "127.0.0.1");

        // Assert - verify SecurityContext cleared
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_WithExpiredSession_RevokesAndBlacklists() {
        // Arrange: session with expiresAt in the past (expired but not revoked)
        String token = "test-refresh-token";
        String tokenHash = hashToken(token);
        UserSession session = createSession(false, Instant.now().minusSeconds(3600)); // expired
        session.setRefreshTokenHash(tokenHash);
        session.setSessionId(sessionId);
        when(sessionRepository.findByRefreshTokenHashAndRevokedFalse(token)).thenReturn(Optional.of(session));
        when(sessionRepository.revokeSession(eq(sessionId), eq(userId), any())).thenReturn(1);

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
}

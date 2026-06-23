package com.carebridge.backend.identity.service;

import com.carebridge.backend.identity.dto.SessionInfo;
import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.identity.service.impl.SessionServiceImpl;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private com.carebridge.backend.security.jwt.JwtTokenProvider tokenProvider;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private SessionServiceImpl sessionService;

    private UUID userId;
    private UUID sessionId;
    private Instant now;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        now = Instant.now();

        sessionService = new SessionServiceImpl(sessionRepository, tokenProvider);
    }

    @Test
    void getActiveSessions_ShouldReturnSortedSessionsWithStatus() {
        // Arrange
        UserSession activeSession = UserSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .refreshTokenHash("hash1")
                .deviceName("iPhone")
                .browser("Safari")
                .ipAddress("192.168.1.1")
                .location("Ho Chi Minh City, Vietnam")
                .lastActivityAt(now)
                .expiresAt(now.plusSeconds(3600))
                .revoked(false)
                .status("active")
                .isCurrent(false)
                .createdAt(now.minusSeconds(3600))
                .updatedAt(now)
                .build();

        UserSession inactiveSession = UserSession.builder()
                .sessionId(UUID.randomUUID())
                .userId(userId)
                .refreshTokenHash("hash2")
                .deviceName("Windows PC")
                .browser("Chrome")
                .ipAddress("10.0.0.1")
                .location(null)
                .lastActivityAt(now.minusSeconds(60 * 60 * 24 * 35)) // 35 days ago
                .expiresAt(now.plusSeconds(3600))
                .revoked(false)
                .isCurrent(false)
                .createdAt(now.minusSeconds(60 * 60 * 24 * 35))
                .updatedAt(now.minusSeconds(60 * 60 * 24 * 35))
                .build();

        when(sessionRepository.findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(userId))
                .thenReturn(Arrays.asList(activeSession, inactiveSession));

        // Act
        List<SessionInfo> result = sessionService.getActiveSessions(userId);

        // Assert
        assertEquals(2, result.size());
        assertEquals("active", result.get(0).getStatus());
        assertEquals("inactive", result.get(1).getStatus());
        assertFalse(result.get(0).isCurrent());
        assertFalse(result.get(1).isCurrent());
        verify(sessionRepository).findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(userId);
    }

    @Test
    void getActiveSessions_ShouldMarkCurrentSession() {
        // Arrange
        String currentToken = "current-refresh-token";
        UserSession session = UserSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .refreshTokenHash(currentToken)
                .deviceName("Mac")
                .browser("Chrome")
                .ipAddress("127.0.0.1")
                .location(null)
                .lastActivityAt(now)
                .expiresAt(now.plusSeconds(3600))
                .revoked(false)
                .isCurrent(false)
                .createdAt(now.minusSeconds(3600))
                .updatedAt(now)
                .build();

        when(sessionRepository.findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(userId))
                .thenReturn(List.of(session));
        when(tokenProvider.validateToken(currentToken)).thenReturn(true);
        setAuthenticationToken(currentToken);

        // Act
        List<SessionInfo> result = sessionService.getActiveSessions(userId);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.get(0).isCurrent());
    }

    @Test
    void getCurrentSession_ShouldReturnSessionWhenTokenValid() {
        // Arrange
        String currentToken = "valid-token";
        UserSession session = UserSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .refreshTokenHash(currentToken)
                .deviceName("iPhone")
                .browser("Safari")
                .ipAddress("192.168.1.1")
                .location("Hanoi, Vietnam")
                .lastActivityAt(now)
                .expiresAt(now.plusSeconds(3600))
                .revoked(false)
                .isCurrent(true)
                .createdAt(now.minusSeconds(3600))
                .updatedAt(now)
                .build();

        when(sessionRepository.findByRefreshTokenHashAndRevokedFalse(currentToken))
                .thenReturn(Optional.of(session));
        setAuthenticationToken(currentToken);

        // Act
        SessionInfo result = sessionService.getCurrentSession();

        // Assert
        assertNotNull(result);
        assertEquals(sessionId, result.getSessionId());
        assertEquals("iPhone", result.getDeviceName());
    }

    @Test
    void getCurrentSession_ShouldReturnNullWhenNoToken() {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act
        SessionInfo result = sessionService.getCurrentSession();

        // Assert
        assertNull(result);
    }

    @Test
    void getCurrentSession_ShouldReturnNullWhenSessionNotFound() {
        // Arrange
        String token = "unknown-token";
        when(sessionRepository.findByRefreshTokenHashAndRevokedFalse(token))
                .thenReturn(Optional.empty());
        setAuthenticationToken(token);

        // Act
        SessionInfo result = sessionService.getCurrentSession();

        // Assert
        assertNull(result);
    }

    @Test
    void revokeSession_ShouldRevokeWhenSessionExistsAndUserMatches() {
        // Arrange
        UserSession session = UserSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .refreshTokenHash("hash")
                .deviceName("PC")
                .browser("Chrome")
                .ipAddress("127.0.0.1")
                .lastActivityAt(now)
                .expiresAt(now.plusSeconds(3600))
                .revoked(false)
                .status("active")
                .isCurrent(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        doReturn(1).when(sessionRepository).revokeSession(eq(sessionId), eq(userId), any(Instant.class));

        // Act
        sessionService.revokeSession(sessionId, userId, "127.0.0.1");

        // Assert
        verify(sessionRepository).revokeSession(eq(sessionId), eq(userId), any(Instant.class));
    }

    @Test
    void revokeSession_ShouldThrowWhenSessionNotFound() {
        // Arrange
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            sessionService.revokeSession(sessionId, userId, "127.0.0.1");
        });
    }

    @Test
    void revokeSession_ShouldThrowWhenUserDoesNotMatch() {
        // Arrange
        UUID differentUserId = UUID.randomUUID();
        UserSession session = UserSession.builder()
                .sessionId(sessionId)
                .userId(differentUserId)
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            sessionService.revokeSession(sessionId, userId, "127.0.0.1");
        });
    }

    @Test
    void updateLastActivity_ShouldUpdateWhenSessionExists() {
        // Arrange
        String token = "refresh-token";
        UserSession session = UserSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .refreshTokenHash(token)
                .lastActivityAt(now.minusSeconds(600))
                .updatedAt(now.minusSeconds(600))
                .build();

        when(sessionRepository.findByRefreshTokenHashAndRevokedFalse(token))
                .thenReturn(Optional.of(session));
        doNothing().when(sessionRepository).updateActivity(any(Instant.class), anyString(), eq(token));

        // Act
        sessionService.updateLastActivity(token, "192.168.1.2");

        // Assert
        verify(sessionRepository).updateActivity(any(Instant.class), eq("192.168.1.2"), eq(token));
    }

    @Test
    void updateLastActivity_ShouldDoNothingWhenSessionNotFound() {
        // Arrange
        String token = "unknown-token";
        when(sessionRepository.findByRefreshTokenHashAndRevokedFalse(token))
                .thenReturn(Optional.empty());

        // Act - should not throw
        sessionService.updateLastActivity(token, "127.0.0.1");

        // Assert - no interaction
        verify(sessionRepository, never()).updateActivity(any(), anyString(), anyString());
    }

    private void setAuthenticationToken(String token) {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getCredentials()).thenReturn(token);

        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }
}

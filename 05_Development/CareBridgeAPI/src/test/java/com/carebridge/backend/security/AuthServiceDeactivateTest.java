package com.carebridge.backend.security;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AuthenticationException;
import com.carebridge.backend.common.exception.AuthorizationException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.identity.repository.TokenBlacklistRepository;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.OtpVerificationRepository;
import com.carebridge.backend.security.repository.RefreshTokenRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.policy.AuthenticationPolicy;
import com.carebridge.backend.security.policy.PasswordComplexityPolicy;
import com.carebridge.backend.security.policy.RateLimitPolicy;
import com.carebridge.backend.security.service.AuthService;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.security.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceDeactivateTest {

    private AuthService authService;
    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private DeviceTokenRepository deviceTokenRepository;
    private AuditService auditService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        deviceTokenRepository = mock(DeviceTokenRepository.class);
        auditService = mock(AuditService.class);
        passwordEncoder = mock(PasswordEncoder.class);

        authService = new AuthServiceImpl(
                userRepository,
                refreshTokenRepository,
                mock(OtpVerificationRepository.class),
                auditService,
                mock(com.carebridge.backend.security.jwt.JwtTokenProvider.class),
                mock(com.carebridge.backend.security.mapper.UserMapper.class),
                mock(AuthenticationPolicy.class),
                mock(PasswordComplexityPolicy.class),
                mock(RateLimitPolicy.class),
                mock(UserSessionRepository.class),
                mock(TokenBlacklistRepository.class),
                mock(EmailService.class),
                mock(SmsService.class),
                passwordEncoder,
                deviceTokenRepository
        );
    }

    private User buildActiveUser(UUID userId, Role role) {
        return User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("$2a$12$hashedpassword")
                .role(role)
                .enabled(true)
                .accountStatus("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("AUTH-081: Wrong password throws AuthenticationException")
    void deactivate_wrongPassword_throwsAuthenticationException() {
        UUID userId = UUID.randomUUID();
        User user = buildActiveUser(userId, Role.MOTHER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "$2a$12$hashedpassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.deactivate(userId, "wrongpassword"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("AUTH-081");
    }

    @Test
    @DisplayName("AUTH-082: Already deactivated account throws ValidationException")
    void deactivate_alreadyDeactivated_throwsValidationException() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("$2a$12$hashedpassword")
                .role(Role.MOTHER)
                .enabled(false)
                .accountStatus("DEACTIVATED")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.deactivate(userId, "anypassword"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("AUTH-082");
    }

    @Test
    @DisplayName("AUTH-083: SYSTEM_ADMIN cannot self-deactivate")
    void deactivate_systemAdmin_throwsAuthorizationException() {
        UUID userId = UUID.randomUUID();
        User user = buildActiveUser(userId, Role.SYSTEM_ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.deactivate(userId, "anypassword"))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("SYSTEM_ADMIN");
    }

    @Test
    @DisplayName("AUTH-084: Successful deactivation sets account status and revokes tokens")
    void deactivate_validCredentials_deactivatesAndRevokesTokens() {
        UUID userId = UUID.randomUUID();
        User user = buildActiveUser(userId, Role.MOTHER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctpassword", "$2a$12$hashedpassword")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(refreshTokenRepository.revokeAllByUserId(userId)).thenReturn(1);
        when(deviceTokenRepository.deactivateAllForUser(eq(userId), any())).thenReturn(1);

        authService.deactivate(userId, "correctpassword");

        // Verify user state
        verify(userRepository).save(argThat(u ->
                "DEACTIVATED".equals(u.getAccountStatus()) && !u.isEnabled()));

        // Verify token revocation
        verify(refreshTokenRepository).revokeAllByUserId(userId);
        verify(deviceTokenRepository).deactivateAllForUser(eq(userId), any());

        // Verify audit
        verify(auditService).log(eq(AuditAction.SECURITY_EVENT), eq(userId), any(), any(), any());
    }

    @Test
    @DisplayName("AUTH-085: User not found throws ResourceNotFoundException")
    void deactivate_userNotFound_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.deactivate(userId, "anypassword"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("AUTH-086: Order of checks: role check before status check")
    void deactivate_systemAdminAlreadyDeactivated_throwsAuthorizationExceptionFirst() {
        UUID userId = UUID.randomUUID();
        // System admin who is already deactivated - role check should come first
        User user = User.builder()
                .id(userId)
                .email("admin@example.com")
                .passwordHash("$2a$12$hashedpassword")
                .role(Role.SYSTEM_ADMIN)
                .enabled(false)
                .accountStatus("DEACTIVATED")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Should throw AuthorizationException (role check) before ValidationException (status check)
        assertThatThrownBy(() -> authService.deactivate(userId, "anypassword"))
                .isInstanceOf(AuthorizationException.class);
    }
}

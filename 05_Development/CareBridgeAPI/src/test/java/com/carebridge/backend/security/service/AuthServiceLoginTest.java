package com.carebridge.backend.security.service;

import com.carebridge.backend.common.exception.AccountDisabledException;
import com.carebridge.backend.common.exception.AccountLockedException;
import com.carebridge.backend.common.exception.AuthenticationException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.identity.repository.TokenBlacklistRepository;
import com.carebridge.backend.security.dto.request.LoginRequest;
import com.carebridge.backend.security.dto.response.AuthResponse;
import com.carebridge.backend.security.dto.response.UserProfileResponse;
import com.carebridge.backend.security.entity.RefreshToken;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.RefreshTokenRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.policy.AuthenticationPolicy;
import com.carebridge.backend.security.policy.PasswordComplexityPolicy;
import com.carebridge.backend.security.policy.RateLimitPolicy;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceLoginTest {

    private AuthService authService;
    private UserRepository userRepository;
    private RateLimitPolicy rateLimitPolicy;
    private AuthenticationPolicy authenticationPolicy;
    private PasswordComplexityPolicy passwordComplexityPolicy;
    private PasswordEncoder passwordEncoder;
    private com.carebridge.backend.security.jwt.JwtTokenProvider jwtTokenProvider;
    private com.carebridge.backend.security.mapper.UserMapper userMapper;
    private RefreshTokenRepository refreshTokenRepository;
    private com.carebridge.backend.audit.service.AuditService auditService;
    private com.carebridge.backend.identity.repository.UserSessionRepository sessionRepository;
    private TokenBlacklistRepository tokenBlacklistRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        rateLimitPolicy = mock(RateLimitPolicy.class);
        authenticationPolicy = mock(AuthenticationPolicy.class);
        passwordComplexityPolicy = mock(PasswordComplexityPolicy.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(com.carebridge.backend.security.jwt.JwtTokenProvider.class);
        userMapper = mock(com.carebridge.backend.security.mapper.UserMapper.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        auditService = mock(com.carebridge.backend.audit.service.AuditService.class);
        sessionRepository = mock(com.carebridge.backend.identity.repository.UserSessionRepository.class);
        tokenBlacklistRepository = mock(TokenBlacklistRepository.class);

        authService = new AuthServiceImpl(
                userRepository,
                refreshTokenRepository,
                mock(com.carebridge.backend.security.repository.OtpVerificationRepository.class),
                auditService,
                jwtTokenProvider,
                userMapper,
                authenticationPolicy,
                passwordComplexityPolicy,
                rateLimitPolicy,
                sessionRepository,
                tokenBlacklistRepository,
                mock(com.carebridge.backend.security.service.EmailService.class),
                mock(com.carebridge.backend.security.service.SmsService.class),
                passwordEncoder
        );
    }

    @Test
    void login_WithValidPhone_ShouldReturnTokensAndProfile() {
        // Given
        String phone = "+84901234567";
        String password = "MyP@ssw0rd123";
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .phone(phone)
                .email(null)
                .passwordHash("$2a$12$hashedpassword")
                .enabled(true)
                .locked(false)
                .role(Role.MOTHER)
                .build();

        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        doNothing().when(authenticationPolicy).ensureCanAuthenticate(user);
        when(rateLimitPolicy.canAttempt(userId.toString())).thenReturn(true);
        when(passwordEncoder.matches(password, "$2a$12$hashedpassword")).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId(1L);
            }
            return token;
        });
        when(jwtTokenProvider.generateAccessToken(eq(user), any(UUID.class))).thenReturn("access-token");
        when(userMapper.toProfileResponse(user)).thenReturn(new UserProfileResponse());

        LoginRequest request = new LoginRequest();
        request.setPhone(phone);
        request.setEmail(null);
        request.setPassword(password);

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isNotNull();
        assertThat(response.getUser()).isNotNull();

        verify(rateLimitPolicy).reset(userId.toString());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getLastLoginAt()).isNotNull();
        assertThat(savedUser.isLocked()).isFalse();
        assertThat(savedUser.getLockedAt()).isNull();
    }

    @Test
    void login_WithValidEmail_ShouldReturnTokensAndProfile() {
        // Given
        String email = "test@example.com";
        String password = "MyP@ssw0rd123";
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .phone(null)
                .email(email)
                .passwordHash("$2a$12$hashedpassword")
                .enabled(true)
                .locked(false)
                .role(Role.EXPERT)
                .build();

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        doNothing().when(authenticationPolicy).ensureCanAuthenticate(user);
        when(rateLimitPolicy.canAttempt(userId.toString())).thenReturn(true);
        when(passwordEncoder.matches(password, "$2a$12$hashedpassword")).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId(1L);
            }
            return token;
        });
        when(jwtTokenProvider.generateAccessToken(eq(user), any(UUID.class))).thenReturn("access-token");
        when(userMapper.toProfileResponse(user)).thenReturn(new UserProfileResponse());

        LoginRequest request = new LoginRequest();
        request.setPhone(null);
        request.setEmail(email);
        request.setPassword(password);

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isNotNull();
    }

    @Test
    void login_WhenUserNotFound_ShouldThrowInvalidCredentials() {
        // Given
        String phone = "+84999999999";
        when(userRepository.findByPhone(phone)).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setPhone(phone);
        request.setEmail(null);
        request.setPassword("password");

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials");

        verifyNoInteractions(rateLimitPolicy, authenticationPolicy);
    }

    @Test
    void login_WhenWrongPassword_ShouldThrowInvalidCredentials() {
        // Given
        String phone = "+84901234567";
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .phone(phone)
                .passwordHash("$2a$12$correcthash")
                .enabled(true)
                .locked(false)
                .build();

        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        doNothing().when(authenticationPolicy).ensureCanAuthenticate(user);
        when(rateLimitPolicy.canAttempt(userId.toString())).thenReturn(true);
        when(passwordEncoder.matches("wrongpassword", "$2a$12$correcthash")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setPhone(phone);
        request.setPassword("wrongpassword");

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials");

        verify(rateLimitPolicy, never()).reset(userId.toString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_WhenDisabledAccount_ShouldThrowAccountDisabled() {
        // Given
        String phone = "+84901234567";
        User user = User.builder()
                .id(UUID.randomUUID())
                .phone(phone)
                .enabled(false)
                .locked(false)
                .build();

        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        doThrow(new AccountDisabledException("Account is disabled"))
                .when(authenticationPolicy).ensureCanAuthenticate(user);

        LoginRequest request = new LoginRequest();
        request.setPhone(phone);
        request.setPassword("password");

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountDisabledException.class)
                .hasMessage("Account is disabled");

        verifyNoInteractions(rateLimitPolicy, passwordEncoder);
    }

    @Test
    void login_WhenLockedAccount_ShouldThrowAccountLocked() {
        // Given
        String phone = "+84901234567";
        User user = User.builder()
                .id(UUID.randomUUID())
                .phone(phone)
                .enabled(true)
                .locked(true)
                .lockedAt(Instant.now())
                .build();

        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        doThrow(new AccountLockedException("Account is locked"))
                .when(authenticationPolicy).ensureCanAuthenticate(user);

        LoginRequest request = new LoginRequest();
        request.setPhone(phone);
        request.setPassword("password");

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountLockedException.class)
                .hasMessage("Account is locked");

        verifyNoInteractions(rateLimitPolicy, passwordEncoder);
    }

    @Test
    void login_WhenRateLimited_ShouldLockAccountAndThrow() {
        // Given
        String phone = "+84901234567";
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .phone(phone)
                .enabled(true)
                .locked(false)
                .build();

        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        doNothing().when(authenticationPolicy).ensureCanAuthenticate(user);
        when(rateLimitPolicy.canAttempt(userId.toString())).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setPhone(phone);
        request.setPassword("password");

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("Account temporarily locked");

        // Verify account was locked and lockedAt was set
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.isLocked()).isTrue();
        assertThat(savedUser.getLockedAt()).isNotNull();

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void login_WhenNoIdentifier_ShouldThrowValidationException() {
        LoginRequest request = new LoginRequest();
        request.setPhone(null);
        request.setEmail(null);
        request.setPassword("password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Either phone or email must be provided");
    }

    @Test
    void login_WhenBothIdentifiersProvided_ShouldReject() {
        LoginRequest request = new LoginRequest();
        request.setPhone("+84901234567");
        request.setEmail("test@example.com");
        request.setPassword("password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Either phone or email must be provided");
    }

    @Test
    void login_EmailNormalization_ShouldFindUserCaseInsensitively() {
        // Given - user registered with lowercase email
        String emailLower = "test@example.com";
        String emailInput = "Test@Example.com";
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email(emailLower)
                .phone(null)
                .passwordHash("$2a$12$hashedpassword")
                .enabled(true)
                .locked(false)
                .build();

        // Note: AuthServiceImpl normalizes email to lowercase before lookup
        when(userRepository.findByEmailIgnoreCase(emailLower)).thenReturn(Optional.of(user));
        doNothing().when(authenticationPolicy).ensureCanAuthenticate(user);
        when(rateLimitPolicy.canAttempt(userId.toString())).thenReturn(true);
        when(passwordEncoder.matches("password", "$2a$12$hashedpassword")).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId(1L);
            }
            return token;
        });
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("token");
        when(userMapper.toProfileResponse(user)).thenReturn(new UserProfileResponse());

        LoginRequest request = new LoginRequest();
        request.setPhone(null);
        request.setEmail(emailInput);
        request.setPassword("password");

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
    }

    @Test
    void login_WhenPasswordHashNull_ShouldThrowInvalidCredentials() {
        // Given
        String phone = "+84901234567";
        User user = User.builder()
                .id(UUID.randomUUID())
                .phone(phone)
                .passwordHash(null)
                .enabled(true)
                .locked(false)
                .build();

        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        doNothing().when(authenticationPolicy).ensureCanAuthenticate(user);
        when(rateLimitPolicy.canAttempt(user.getId().toString())).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setPhone(phone);
        request.setPassword("password");

        // When/Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_WhenLockedAccountAfterCooldown_ShouldSucceedAndUnlock() {
        // Given - user locked 16 minutes ago (past 15 min cooldown)
        String phone = "+84901234567";
        String password = "MyP@ssw0rd123";
        UUID userId = UUID.randomUUID();
        Instant lockedAt = Instant.now().minus(16, java.time.temporal.ChronoUnit.MINUTES);
        User user = User.builder()
                .id(userId)
                .phone(phone)
                .email(null)
                .passwordHash("$2a$12$hashedpassword")
                .enabled(true)
                .locked(true)
                .lockedAt(lockedAt)
                .role(Role.MOTHER)
                .build();

        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        // Use real AuthenticationPolicy to test actual auto-unlock behavior
        // (Other tests mock it, but this test specifically verifies the policy's unlock logic)
        // We'll reconstruct authService with real policy
        AuthenticationPolicy realPolicy = new AuthenticationPolicy();
        // Set the user field via reflection? Actually policy.ensureCanAuthenticate modifies user directly
        // We can't inject real policy easily because it's @Component but we can instantiate manually
        // We'll just let the mocked service call the real policy method by not stubbing it
        // Actually, since we're using mock(authenticationPolicy) in setUp, we need to override here
        // Easiest: create a new AuthServiceImpl with real policy
        TokenBlacklistRepository tokenBlacklistRepo = mock(TokenBlacklistRepository.class);
        AuthService realAuthService = new AuthServiceImpl(
                userRepository,
                refreshTokenRepository,
                mock(com.carebridge.backend.security.repository.OtpVerificationRepository.class),
                auditService,
                jwtTokenProvider,
                userMapper,
                realPolicy, // real policy
                passwordComplexityPolicy,
                rateLimitPolicy,
                sessionRepository,
                tokenBlacklistRepo,
                mock(com.carebridge.backend.security.service.EmailService.class),
                mock(com.carebridge.backend.security.service.SmsService.class),
                passwordEncoder
        );

        when(rateLimitPolicy.canAttempt(userId.toString())).thenReturn(true);
        when(passwordEncoder.matches(password, "$2a$12$hashedpassword")).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId(1L);
            }
            return token;
        });
        when(jwtTokenProvider.generateAccessToken(eq(user), any(UUID.class))).thenReturn("access-token");
        when(userMapper.toProfileResponse(user)).thenReturn(new UserProfileResponse());

        LoginRequest request = new LoginRequest();
        request.setPhone(phone);
        request.setEmail(null);
        request.setPassword(password);

        // When
        AuthResponse response = realAuthService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");

        // Verify user was unlocked and persisted
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.isLocked()).isFalse();
        assertThat(savedUser.getLockedAt()).isNull();
    }
}

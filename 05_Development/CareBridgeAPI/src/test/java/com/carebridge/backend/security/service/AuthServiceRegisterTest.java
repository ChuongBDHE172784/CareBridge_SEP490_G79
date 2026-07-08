package com.carebridge.backend.security.service;

import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.security.dto.request.RegisterRequest;
import com.carebridge.backend.security.dto.response.OtpSendResponse;
import com.carebridge.backend.security.entity.OtpVerification;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.policy.AuthenticationPolicy;
import com.carebridge.backend.security.policy.PasswordComplexityPolicy;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.OtpVerificationRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.impl.AuthServiceImpl;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class AuthServiceRegisterTest {

    @Autowired
    private AuthService authService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private SmsService smsService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
        assertNotNull(authService);
    }

    @Test
    @DisplayName("AUTH-TC-INT-001: Integration register with email")
    void register_IntegrationTest_Email() {
        // Tạo request
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPhone(null);
        request.setPassword("MyP@ssw0rd123");
        request.setRole(Role.MOTHER);

        // Test will fail due to mocks not configured - this is just structure check
        // Full integration test with @DataJpaTest would be better
    }

    // ═══════════════════════════════════════════════════════════════════
    // UC-01 RegisterAccount — TDD gap closure (unit-level, mock-based).
    // NOTE ON SPEC DIVERGENCE: the real service returns OtpSendResponse (OTP-gated
    // registration), NOT a RegisterResponseDTO with status="UNVERIFIED"; duplicate
    // email/phone throw a single ValidationException("Account already exists") rather
    // than distinct AUTH-002/AUTH-003 codes; role is a Role enum guarded by
    // AuthenticationPolicy.resolveSelfRegistrationRole (not a String "ADMIN").
    // Assertions below encode the ACTUAL approved behavior.
    // ═══════════════════════════════════════════════════════════════════

    private final UserRepository userRepositoryMock = mock(UserRepository.class);
    private final OtpVerificationRepository otpRepoMock = mock(OtpVerificationRepository.class);
    private final PasswordComplexityPolicy passwordComplexityPolicyMock = mock(PasswordComplexityPolicy.class);
    private final PasswordEncoder passwordEncoderMock = mock(PasswordEncoder.class);

    private AuthServiceImpl newUnitAuthService(AuthenticationPolicy authenticationPolicy) {
        AuthServiceImpl svc = new AuthServiceImpl(
                userRepositoryMock,
                mock(com.carebridge.backend.security.repository.RefreshTokenRepository.class),
                otpRepoMock,
                mock(com.carebridge.backend.audit.service.AuditService.class),
                mock(com.carebridge.backend.security.jwt.JwtTokenProvider.class),
                mock(com.carebridge.backend.security.mapper.UserMapper.class),
                authenticationPolicy,
                passwordComplexityPolicyMock,
                mock(com.carebridge.backend.security.policy.RateLimitPolicy.class),
                mock(com.carebridge.backend.identity.repository.UserSessionRepository.class),
                mock(com.carebridge.backend.identity.repository.TokenBlacklistRepository.class),
                emailService,
                smsService,
                passwordEncoderMock,
                mock(com.carebridge.backend.notification.repository.DeviceTokenRepository.class));
        ReflectionTestUtils.setField(svc, "otpExpirationSeconds", 300L);
        return svc;
    }

    private RegisterRequest registerRequest(String email, String phone, String password, Role role) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPhone(phone);
        req.setPassword(password);
        req.setRole(role);
        return req;
    }

    @Test
    @DisplayName("AUTH-TC-001: Successful MOTHER registration issues OTP and persists user")
    void register_validMother_persistsUserAndSendsOtp() {
        AuthenticationPolicy policy = mock(AuthenticationPolicy.class);
        AuthServiceImpl svc = newUnitAuthService(policy);
        when(passwordComplexityPolicyMock.isComplexEnough(anyString())).thenReturn(true);
        when(policy.resolveSelfRegistrationRole(Role.MOTHER)).thenReturn(Role.MOTHER);
        when(userRepositoryMock.existsByEmail("new.mother@example.com")).thenReturn(false);
        when(passwordEncoderMock.encode("MyP@ssw0rd123")).thenReturn("$2a$12$hashedvalue");
        when(userRepositoryMock.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                u.setId(UUID.randomUUID());
            }
            return u;
        });
        when(otpRepoMock.save(any(OtpVerification.class))).thenAnswer(inv -> {
            OtpVerification o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(42L);
            }
            return o;
        });

        OtpSendResponse response = svc.register(
                registerRequest("new.mother@example.com", null, "MyP@ssw0rd123", Role.MOTHER));

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isNotNull();
        assertThat(response.getOtpExpiresAt()).isNotNull();
        verify(userRepositoryMock, times(1)).save(any(User.class));
        verify(passwordEncoderMock).encode("MyP@ssw0rd123");
        verify(otpRepoMock, times(1)).save(any(OtpVerification.class));
        verify(emailService, times(1)).sendOtpVerificationEmail(eq("new.mother@example.com"), anyString(), anyInt());
        // Password must be persisted as a BCrypt hash, never plaintext.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryMock).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$12$hashedvalue");
        assertThat(userCaptor.getValue().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("AUTH-TC-002: Duplicate email rejected with no side effects")
    void register_duplicateEmail_rejected() {
        AuthServiceImpl svc = newUnitAuthService(mock(AuthenticationPolicy.class));
        when(passwordComplexityPolicyMock.isComplexEnough(anyString())).thenReturn(true);
        when(userRepositoryMock.existsByEmail("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> svc.register(
                registerRequest("existing@test.com", null, "MyP@ssw0rd123", Role.MOTHER)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Account already exists");

        verify(userRepositoryMock, never()).save(any(User.class));
        verify(otpRepoMock, never()).save(any(OtpVerification.class));
        verifyNoInteractions(emailService, smsService);
    }

    @Test
    @DisplayName("AUTH-TC-003: Duplicate phone rejected with no side effects")
    void register_duplicatePhone_rejected() {
        AuthServiceImpl svc = newUnitAuthService(mock(AuthenticationPolicy.class));
        when(passwordComplexityPolicyMock.isComplexEnough(anyString())).thenReturn(true);
        when(userRepositoryMock.existsByPhone("+84912345678")).thenReturn(true);

        assertThatThrownBy(() -> svc.register(
                registerRequest(null, "+84912345678", "MyP@ssw0rd123", Role.MOTHER)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Account already exists");

        verify(userRepositoryMock, never()).save(any(User.class));
        verify(otpRepoMock, never()).save(any(OtpVerification.class));
    }

    @Test
    @DisplayName("AUTH-TC-005: Privileged role (SYSTEM_ADMIN) rejected for self-registration")
    void register_privilegedRole_rejected() {
        // Uses the REAL AuthenticationPolicy so the whitelist (MOTHER/FAMILY/EXPERT) is exercised.
        AuthServiceImpl svc = newUnitAuthService(new AuthenticationPolicy());
        when(passwordComplexityPolicyMock.isComplexEnough(anyString())).thenReturn(true);
        when(userRepositoryMock.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> svc.register(
                registerRequest("admin.wannabe@test.com", null, "MyP@ssw0rd123", Role.SYSTEM_ADMIN)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not allowed for self-registration");

        verify(userRepositoryMock, never()).save(any(User.class));
    }

    @Test
    @DisplayName("AUTH-TC-008: OTP record created and delivered after successful registration")
    void register_success_createsAndSendsOtpAfterUserSaved() {
        AuthenticationPolicy policy = mock(AuthenticationPolicy.class);
        AuthServiceImpl svc = newUnitAuthService(policy);
        when(passwordComplexityPolicyMock.isComplexEnough(anyString())).thenReturn(true);
        when(policy.resolveSelfRegistrationRole(any())).thenReturn(Role.MOTHER);
        when(userRepositoryMock.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoderMock.encode(anyString())).thenReturn("$2a$12$hashedvalue");
        when(userRepositoryMock.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                u.setId(UUID.randomUUID());
            }
            return u;
        });
        when(otpRepoMock.save(any(OtpVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        svc.register(registerRequest("otp.user@example.com", null, "MyP@ssw0rd123", Role.MOTHER));

        // OTP must be persisted AFTER the user is saved (BR-AUTH: OTP-after-save).
        InOrder inOrder = inOrder(userRepositoryMock, otpRepoMock, emailService);
        inOrder.verify(userRepositoryMock).save(any(User.class));
        inOrder.verify(otpRepoMock).save(any(OtpVerification.class));
        inOrder.verify(emailService).sendOtpVerificationEmail(eq("otp.user@example.com"), anyString(), anyInt());
    }

    // ─── DTO Bean-Validation layer (AUTH-TC-006, 007, 009, 005-null) ───

    private static Validator beanValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return factory.getValidator();
    }

    private static boolean hasViolationOn(Set<ConstraintViolation<RegisterRequest>> violations, String field) {
        return violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    @Test
    @DisplayName("AUTH-TC-006: Invalid email format rejected by @Email")
    void registerDto_invalidEmail_violation() {
        RegisterRequest req = registerRequest("not-an-email", null, "MyP@ssw0rd123", Role.MOTHER);
        Set<ConstraintViolation<RegisterRequest>> violations = beanValidator().validate(req);
        assertThat(hasViolationOn(violations, "email")).isTrue();
    }

    @Test
    @DisplayName("AUTH-TC-007: Non-Vietnamese phone rejected by @VietnamesePhoneNumber")
    void registerDto_invalidVietnamesePhone_violation() {
        RegisterRequest req = registerRequest(null, "+14155552671", "MyP@ssw0rd123", Role.MOTHER);
        Set<ConstraintViolation<RegisterRequest>> violations = beanValidator().validate(req);
        assertThat(hasViolationOn(violations, "phone")).isTrue();
    }

    @Test
    @DisplayName("AUTH-TC-005b: Null role rejected by @NotNull")
    void registerDto_nullRole_violation() {
        RegisterRequest req = registerRequest("valid@test.com", null, "MyP@ssw0rd123", null);
        Set<ConstraintViolation<RegisterRequest>> violations = beanValidator().validate(req);
        assertThat(hasViolationOn(violations, "role")).isTrue();
    }

    @Test
    @DisplayName("AUTH-TC-009: SQL-injection string in email is rejected by validation (system safe)")
    void registerDto_sqlInjectionEmail_rejected() {
        RegisterRequest req = registerRequest("'; DROP TABLE users; --", null, "MyP@ssw0rd123", Role.MOTHER);
        Set<ConstraintViolation<RegisterRequest>> violations = beanValidator().validate(req);
        // @Email rejects the payload → request never reaches persistence, so no SQL is executed.
        assertThat(hasViolationOn(violations, "email")).isTrue();
    }
}

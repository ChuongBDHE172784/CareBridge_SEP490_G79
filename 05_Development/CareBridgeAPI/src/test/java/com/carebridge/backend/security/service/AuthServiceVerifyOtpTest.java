package com.carebridge.backend.security.service;

import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.identity.repository.TokenBlacklistRepository;
import com.carebridge.backend.security.dto.request.VerifyOtpRequest;
import com.carebridge.backend.security.dto.response.AuthResponse;
import com.carebridge.backend.security.entity.OtpVerification;
import com.carebridge.backend.security.entity.RefreshToken;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.OtpVerificationRepository;
import com.carebridge.backend.security.repository.RefreshTokenRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.service.impl.AuthServiceImpl;
import com.carebridge.backend.security.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UC-02 Verify OTP — TDD gap closure for the verify flow itself.
 *
 * SPEC DIVERGENCE NOTE: the real API is {@code verifyOtp(VerifyOtpRequest)} keyed by
 * phone/email + 6-digit code (not userId + code). OTP lookups exclude already-used
 * records ({@code findTop...UsedAtIsNull...}), expired records are filtered out and
 * surface as ValidationException("Invalid or expired OTP"), and a wrong code decrements
 * {@code attempts} (5 → 0). There is no distinct AUTH-006/007/008/009 code nor a
 * RateLimitExceededException/SecurityEvent on exhaustion in the approved implementation;
 * exhaustion simply marks the record used. Assertions encode the ACTUAL behavior.
 */
class AuthServiceVerifyOtpTest {

    private AuthServiceImpl authService;
    private UserRepository userRepository;
    private OtpVerificationRepository otpVerificationRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private com.carebridge.backend.identity.repository.UserSessionRepository sessionRepository;
    private JwtTokenProvider jwtTokenProvider;

    private static final String PHONE = "+84912345678";

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        otpVerificationRepository = mock(OtpVerificationRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        sessionRepository = mock(com.carebridge.backend.identity.repository.UserSessionRepository.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);

        authService = new AuthServiceImpl(
                userRepository,
                refreshTokenRepository,
                otpVerificationRepository,
                mock(com.carebridge.backend.audit.service.AuditService.class),
                jwtTokenProvider,
                mock(com.carebridge.backend.security.mapper.UserMapper.class),
                mock(com.carebridge.backend.security.policy.AuthenticationPolicy.class),
                mock(com.carebridge.backend.security.policy.PasswordComplexityPolicy.class),
                mock(com.carebridge.backend.security.policy.RateLimitPolicy.class),
                sessionRepository,
                mock(TokenBlacklistRepository.class),
                mock(EmailService.class),
                mock(SmsService.class),
                mock(org.springframework.security.crypto.password.PasswordEncoder.class),
                mock(com.carebridge.backend.notification.repository.DeviceTokenRepository.class));
        ReflectionTestUtils.setField(authService, "otpExpirationSeconds", 300L);
    }

    private User unverifiedUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .phone(PHONE)
                .role(Role.MOTHER)
                .enabled(false)
                .locked(false)
                .accountStatus("PENDING_ACTIVATION")
                .build();
    }

    private OtpVerification registerOtp(User user, String rawCode, Instant expiresAt, int attempts) {
        return OtpVerification.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .user(user)
                .phone(PHONE)
                .codeHash(TokenUtils.hashSha256(rawCode))
                .purpose(OtpVerification.OtpPurpose.REGISTER)
                .expiresAt(expiresAt)
                .attempts(attempts)
                .verified(false)
                .build();
    }

    private VerifyOtpRequest request(String phone, String email, String otp) {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setPhone(phone);
        req.setEmail(email);
        req.setOtp(otp);
        return req;
    }

    @Test
    @DisplayName("OTP-TC-001: Correct OTP activates the account and returns tokens")
    void verifyOtp_correctCode_activatesAccount() {
        User user = unverifiedUser();
        OtpVerification otp = registerOtp(user, "123456", Instant.now().plusSeconds(120), 5);
        when(otpVerificationRepository.findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc(PHONE))
                .thenReturn(Optional.of(otp));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> {
            RefreshToken t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(7L);
            }
            return t;
        });
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(any(User.class), any(UUID.class))).thenReturn("access-token");

        AuthResponse response = authService.verifyOtp(request(PHONE, null, "123456"));

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(otp.isVerified()).isTrue();
        assertThat(otp.getUsedAt()).isNotNull();
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getAccountStatus()).isEqualTo("ACTIVE");
        assertThat(user.getPhoneVerified()).isTrue();
        assertThat(user.getEmailVerified()).isNotEqualTo(true);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("OTP-TC-002: Expired OTP is rejected and the account stays unverified")
    void verifyOtp_expiredCode_rejected() {
        User user = unverifiedUser();
        OtpVerification expired = registerOtp(user, "123456", Instant.now().minusSeconds(60), 5);
        when(otpVerificationRepository.findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc(PHONE))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.verifyOtp(request(PHONE, null, "123456")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid or expired OTP");

        assertThat(user.isEnabled()).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("OTP-TC-003: Wrong OTP decrements the remaining attempts and is rejected")
    void verifyOtp_wrongCode_decrementsAttempts() {
        User user = unverifiedUser();
        OtpVerification otp = registerOtp(user, "123456", Instant.now().plusSeconds(120), 5);
        when(otpVerificationRepository.findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc(PHONE))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> authService.verifyOtp(request(PHONE, null, "000000")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid OTP");

        assertThat(otp.getAttempts()).isEqualTo(4);
        assertThat(otp.getUsedAt()).isNull();
        verify(otpVerificationRepository).save(otp);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("OTP-TC-004: Final wrong attempt exhausts and consumes the OTP record")
    void verifyOtp_lastWrongAttempt_marksRecordUsed() {
        User user = unverifiedUser();
        OtpVerification otp = registerOtp(user, "123456", Instant.now().plusSeconds(120), 1);
        when(otpVerificationRepository.findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc(PHONE))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> authService.verifyOtp(request(PHONE, null, "000000")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid OTP");

        assertThat(otp.getAttempts()).isEqualTo(0);
        assertThat(otp.getUsedAt()).isNotNull(); // exhausted → consumed
        verify(otpVerificationRepository).save(otp);
    }

    @Test
    @DisplayName("OTP-TC-005: Already-used OTP cannot be replayed")
    void verifyOtp_alreadyUsedCode_rejected() {
        // Used records are excluded by findTop...UsedAtIsNull... → empty → rejected.
        when(otpVerificationRepository.findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc(PHONE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyOtp(request(PHONE, null, "123456")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid or expired OTP");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("OTP-TC-006: Missing identifier (no phone/email) is rejected")
    void verifyOtp_noIdentifier_rejected() {
        assertThatThrownBy(() -> authService.verifyOtp(request(null, null, "123456")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Either phone or email must be provided");

        verifyNoInteractions(userRepository);
    }
}

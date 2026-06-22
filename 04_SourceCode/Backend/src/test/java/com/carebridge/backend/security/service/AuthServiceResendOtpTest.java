package com.carebridge.backend.security.service;

import com.carebridge.backend.common.exception.RateLimitExceededException;
import com.carebridge.backend.security.dto.request.ResendOtpRequest;
import com.carebridge.backend.security.entity.OtpVerification;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.OtpVerificationRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.policy.RateLimitPolicy;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.security.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class AuthServiceResendOtpTest {

    private AuthService authService;
    private UserRepository userRepository;
    private OtpVerificationRepository otpVerificationRepository;
    private RateLimitPolicy rateLimitPolicy;
    private EmailService emailService;
    private SmsService smsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        otpVerificationRepository = mock(OtpVerificationRepository.class);
        rateLimitPolicy = mock(RateLimitPolicy.class);
        emailService = mock(EmailService.class);
        smsService = mock(SmsService.class);

        AtomicLong otpIdSequence = new AtomicLong(10L);
        when(otpVerificationRepository.save(any(OtpVerification.class)))
                .thenAnswer(invocation -> {
                    OtpVerification saved = invocation.getArgument(0);
                    if (saved.getId() == null) {
                        saved.setId(otpIdSequence.getAndIncrement());
                    }
                    return saved;
                });

        authService = new AuthServiceImpl(
                userRepository,
                mock(com.carebridge.backend.security.repository.RefreshTokenRepository.class),
                otpVerificationRepository,
                mock(com.carebridge.backend.audit.service.AuditService.class),
                mock(com.carebridge.backend.security.jwt.JwtTokenProvider.class),
                mock(com.carebridge.backend.security.mapper.UserMapper.class),
                mock(com.carebridge.backend.security.policy.AuthenticationPolicy.class),
                mock(com.carebridge.backend.security.policy.PasswordComplexityPolicy.class),
                rateLimitPolicy,
                emailService,
                smsService,
                mock(org.springframework.security.crypto.password.PasswordEncoder.class)
        );
        ReflectionTestUtils.setField(authService, "otpExpirationSeconds", 300L);
    }

    @Test
    void resendOtp_WithPhone_ShouldGenerateNewOtpAndSend() {
        // Given
        String phone = "+84901234567";
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .phone(phone)
                .email(null)
                .build();

        OtpVerification existingOtp = OtpVerification.builder()
                .id(1L)
                .user(user)
                .phone(phone)
                .purpose(OtpVerification.OtpPurpose.REGISTER)
                .codeHash("old_hash")
                .expiresAt(Instant.now().minusSeconds(60))
                .attempts(0)
                .verified(false)
                .build();

        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        when(otpVerificationRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(userId))
                .thenReturn(Optional.of(existingOtp));
        when(rateLimitPolicy.tryConsumeResend(userId.toString())).thenReturn(true);

        ResendOtpRequest request = new ResendOtpRequest();
        request.setPhone(phone);
        request.setEmail(null);

        // When
        var response = authService.resendOtp(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOtpExpiresAt()).isNotNull();
        assertThat(response.getMessage()).contains("successfully");

        verify(rateLimitPolicy).tryConsumeResend(userId.toString());
        verify(rateLimitPolicy, never()).recordResendAttempt(anyString());
        verify(smsService).sendOtpVerificationSms(eq(phone), anyString(), eq(5));
        assertThat(existingOtp.getUsedAt()).isNotNull();
    }

    @Test
    void resendOtp_WithEmail_ShouldWorkWithEmailIdentifier() {
        // Given
        String email = "test@example.com";
        String phone = "+84901234567";
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .phone(phone)
                .email(email)
                .build();

        OtpVerification existingOtp = OtpVerification.builder()
                .user(user)
                .phone(phone)
                .purpose(OtpVerification.OtpPurpose.LOGIN)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(otpVerificationRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(userId))
                .thenReturn(Optional.of(existingOtp));
        when(rateLimitPolicy.tryConsumeResend(userId.toString())).thenReturn(true);

        ResendOtpRequest request = new ResendOtpRequest();
        request.setPhone(null);
        request.setEmail(email);

        // When
        var response = authService.resendOtp(request);

        // Then
        assertThat(response).isNotNull();
        verify(emailService).sendOtpVerificationEmail(eq(email), anyString(), eq(5));
        verify(rateLimitPolicy).tryConsumeResend(userId.toString());
    }

    @Test
    void resendOtp_WhenNoIdentifier_ShouldThrowValidationException() {
        // Given
        ResendOtpRequest request = new ResendOtpRequest();
        request.setPhone(null);
        request.setEmail(null);

        // When/Then
        assertThatThrownBy(() -> authService.resendOtp(request))
                .isInstanceOf(com.carebridge.backend.common.exception.ValidationException.class)
                .hasMessageContaining("Exactly one of phone or email must be provided");
        verifyNoInteractions(userRepository, rateLimitPolicy, emailService, smsService);
    }

    @Test
    void resendOtp_WhenBothIdentifiersProvided_ShouldRejectWithoutSideEffects() {
        ResendOtpRequest request = new ResendOtpRequest();
        request.setPhone("+84901234567");
        request.setEmail("attacker@example.com");

        assertThatThrownBy(() -> authService.resendOtp(request))
                .isInstanceOf(com.carebridge.backend.common.exception.ValidationException.class)
                .hasMessageContaining("Exactly one of phone or email must be provided");

        verifyNoInteractions(userRepository, otpVerificationRepository, rateLimitPolicy, emailService, smsService);
    }

    @Test
    void resendOtp_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        String phone = "+84999999999";
        when(userRepository.findByPhone(phone)).thenReturn(Optional.empty());

        ResendOtpRequest request = new ResendOtpRequest();
        request.setPhone(phone);
        request.setEmail(null);

        // When/Then
        assertThatThrownBy(() -> authService.resendOtp(request))
                .isInstanceOf(com.carebridge.backend.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
        verifyNoInteractions(rateLimitPolicy, emailService, smsService);
    }

    @Test
    void resendOtp_WhenNoPendingOtp_ShouldThrowValidationException() {
        // Given
        String phone = "+84901234567";
        User user = User.builder().id(UUID.randomUUID()).build();

        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        when(otpVerificationRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(user.getId()))
                .thenReturn(Optional.empty());

        ResendOtpRequest request = new ResendOtpRequest();
        request.setPhone(phone);
        request.setEmail(null);

        // When/Then
        assertThatThrownBy(() -> authService.resendOtp(request))
                .isInstanceOf(com.carebridge.backend.common.exception.ValidationException.class)
                .hasMessageContaining("No pending OTP verification found");
        verifyNoInteractions(rateLimitPolicy, emailService, smsService);
    }

    @Test
    void resendOtp_WhenRateLimited_ShouldThrowRateLimitExceededException() {
        // Given
        String phone = "+84901234567";
        User user = User.builder().id(UUID.randomUUID()).phone(phone).build();
        OtpVerification otp = OtpVerification.builder().user(user).build();

        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        when(otpVerificationRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(user.getId()))
                .thenReturn(Optional.of(otp));
        when(rateLimitPolicy.tryConsumeResend(user.getId().toString())).thenReturn(false);
        when(rateLimitPolicy.getTimeUntilResendReset(user.getId().toString())).thenReturn(30L);

        ResendOtpRequest request = new ResendOtpRequest();
        request.setPhone(phone);
        request.setEmail(null);

        // When/Then
        assertThatThrownBy(() -> authService.resendOtp(request))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Please wait before resending");
        // Iteration 3 (EC-E6 fix): DB writes happen BEFORE cooldown consume so a
        // rollback can release the slot. Rate-limit rejection now occurs after
        // existing.setUsedAt + new OTP save; verify both writes were attempted.
        verify(otpVerificationRepository, times(2)).save(any(OtpVerification.class));
        verifyNoInteractions(emailService, smsService);
    }

    @Test
    void resendOtp_SwitchingChannelForSameAccount_ShouldRemainRateLimited() {
        String phone = "+84901234567";
        String email = "test@example.com";
        UUID userId = UUID.randomUUID();
        String accountKey = userId.toString();
        User user = User.builder().id(userId).phone(phone).email(email).build();
        OtpVerification existingOtp = OtpVerification.builder()
                .id(1L)
                .user(user)
                .phone(phone)
                .purpose(OtpVerification.OtpPurpose.REGISTER)
                .build();

        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(otpVerificationRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(userId))
                .thenReturn(Optional.of(existingOtp));
        when(rateLimitPolicy.tryConsumeResend(accountKey)).thenReturn(true, false);
        when(rateLimitPolicy.getTimeUntilResendReset(accountKey)).thenReturn(60L);

        ResendOtpRequest phoneRequest = new ResendOtpRequest();
        phoneRequest.setPhone(phone);
        authService.resendOtp(phoneRequest);

        ResendOtpRequest emailRequest = new ResendOtpRequest();
        emailRequest.setEmail(email);

        assertThatThrownBy(() -> authService.resendOtp(emailRequest))
                .isInstanceOf(RateLimitExceededException.class);

        verify(rateLimitPolicy, times(2)).tryConsumeResend(accountKey);
        // Iteration 3 (EC-E6 fix): each resend attempt now saves existing + new
        // BEFORE the cooldown check, so two channel-switched attempts produce
        // 2 attempts × 2 saves = 4 save() invocations.
        verify(otpVerificationRepository, times(4)).save(any(OtpVerification.class));
        verify(smsService).sendOtpVerificationSms(eq(phone), anyString(), eq(5));
        verifyNoInteractions(emailService);
    }
}

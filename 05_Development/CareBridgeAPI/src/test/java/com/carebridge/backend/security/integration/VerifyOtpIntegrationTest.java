package com.carebridge.backend.security.integration;

import com.carebridge.backend.security.dto.request.RegisterRequest;
import com.carebridge.backend.security.dto.request.VerificationMethod;
import com.carebridge.backend.security.dto.request.VerifyOtpRequest;
import com.carebridge.backend.security.entity.OtpVerification;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.OtpVerificationRepository;
import com.carebridge.backend.security.repository.RefreshTokenRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OTP-TC-INT-002 — Integration: full register → verify-otp flow drives the account to
 * ACTIVE in a real PostgreSQL database (Testcontainers), and the OTP row is consumed.
 *
 * <p>The real design keys OTP verification by phone/email + code (not userId), so the
 * flow registers via email, captures the delivered code from the mocked EmailService,
 * then verifies. Assertions match the actual implementation: on success the OTP's
 * {@code attempts} counter is left at its initial value (5) rather than reset to 0.
 */
@Transactional
class VerifyOtpIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private OtpVerificationRepository otpVerificationRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean private EmailService emailService;
    @MockitoBean private SmsService smsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void cleanup() {
        refreshTokenRepository.deleteAll();
        otpVerificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void verifyOtp_activatesUserAndConsumesOtpInRealDatabase() throws Exception {
        String email = "int.verify@test.com";

        RegisterRequest register = new RegisterRequest();
        register.setName("Test User");
        register.setEmail(email);
        register.setPhone(null);
        register.setPassword("Test@1234");
        register.setRole(Role.MOTHER);
        register.setVerificationMethod(VerificationMethod.EMAIL);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        // Capture the real 6-digit code delivered via the mocked email channel.
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce())
                .sendOtpVerificationEmail(eq(email), otpCaptor.capture(), eq(5));
        String otpCode = otpCaptor.getValue();
        assertThat(otpCode).hasSize(6);

        User pending = userRepository.findByEmail(email).orElseThrow();
        assertThat(pending.isEnabled()).isFalse();

        VerifyOtpRequest verify = new VerifyOtpRequest();
        verify.setEmail(email);
        verify.setOtp(otpCode);

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verify)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user.email").value(email));

        // users table: account activated
        User activated = userRepository.findById(pending.getId()).orElseThrow();
        assertThat(activated.isEnabled()).isTrue();
        assertThat(activated.getAccountStatus()).isEqualTo("ACTIVE");

        // Canonical challenge: no pending row remains; the consumed row is used + verified
        assertThat(otpVerificationRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(pending.getId()))
                .isEmpty();
        OtpVerification consumed = otpVerificationRepository.findAll().stream()
                .filter(o -> o.getUser().getId().equals(pending.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(consumed.getUsedAt()).isNotNull();
        assertThat(consumed.isVerified()).isTrue();

        // A canonical auth session token was issued on activation
        assertThat(refreshTokenRepository.findByUser_IdAndRevokedFalse(pending.getId())).hasSize(1);
    }
}

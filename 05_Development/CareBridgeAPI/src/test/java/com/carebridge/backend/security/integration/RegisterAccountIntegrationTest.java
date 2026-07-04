package com.carebridge.backend.security.integration;

import com.carebridge.backend.security.dto.request.RegisterRequest;
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
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AUTH-TC-INT-001 — Integration: user and OTP record persisted to a real database.
 *
 * <p>Exercises the full HTTP → controller → service → repository → PostgreSQL round-trip
 * (no repository mocks) via Testcontainers. Only the external OTP delivery channels
 * (email/SMS) are mocked, since those are side effects outside the persistence boundary.
 *
 * <p>Assertions reflect the real implementation: account status {@code PENDING_ACTIVATION}
 * with {@code enabled=false}, a BCrypt password hash, and a pending {@code otp_verifications}
 * row with a future {@code expires_at}. (The Test-Spec's idealized names — {@code UNVERIFIED},
 * {@code otp_records} — map to these actual columns.)
 */
@Transactional
class RegisterAccountIntegrationTest extends AbstractPostgresIntegrationTest {

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
    void register_persistsUserAndOtpRecordInRealDatabase() throws Exception {
        String email = "int.register@test.com";

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPhone(null);
        request.setPassword("Test@1234");
        request.setRole(Role.MOTHER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.otpExpiresAt").exists());

        // users table: 1 row, PENDING_ACTIVATION, BCrypt hash
        Optional<User> userOpt = userRepository.findByEmail(email);
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        assertThat(user.getId()).isNotNull();
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getAccountStatus()).isEqualTo("PENDING_ACTIVATION");
        assertThat(user.getRole()).isEqualTo(Role.MOTHER);
        assertThat(user.getPasswordHash()).startsWith("$2");

        // otp_verifications table: 1 pending row, unused, future expiry
        OtpVerification otp = otpVerificationRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(user.getId())
                .orElseThrow();
        assertThat(otp.getUsedAt()).isNull();
        assertThat(otp.isVerified()).isFalse();
        assertThat(otp.getPurpose()).isEqualTo(OtpVerification.OtpPurpose.REGISTER);
        assertThat(otp.getExpiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plusSeconds(700));
    }
}

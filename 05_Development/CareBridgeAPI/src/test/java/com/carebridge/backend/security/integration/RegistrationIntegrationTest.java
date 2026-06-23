package com.carebridge.backend.security.integration;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.repository.AuditLogRepository;
import com.carebridge.backend.security.dto.request.RegisterRequest;
import com.carebridge.backend.security.dto.request.ResendOtpRequest;
import com.carebridge.backend.security.dto.request.VerifyOtpRequest;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.entity.OtpVerification;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.repository.OtpVerificationRepository;
import com.carebridge.backend.security.repository.RefreshTokenRepository;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrationIntegrationTest {

    @TestConfiguration
    static class TestMockMvcConfig {
        @Bean
        MockMvcBuilderCustomizer csrfCustomizer() {
            return builder -> builder.defaultRequest(post("/").with(csrf()));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private SmsService smsService;

    @AfterEach
    void cleanup() {
        // Story 1.2 fix: the new happy-path test creates refresh tokens that
        // FK-link to users. Delete them first to keep the order referential.
        // Audit logs are append-only (AuditLog.@PreRemove throws), and they
        // do not FK-link to users, so they can stay between tests.
        refreshTokenRepository.deleteAll();
        otpVerificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_WithValidEmail_ShouldCreateUserAndReturnSuccess() throws Exception {
        // Given
        String email = "test@example.com";
        String password = "MyP@ssw0rd123";

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPhone(null);
        request.setPassword(password);
        request.setRole(Role.MOTHER);

        // When/Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.message").value("Registration initiated. Please verify your OTP."))
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.otpExpiresAt").exists());

        // Verify user was created in database
        Optional<User> userOpt = userRepository.findByEmail(email);
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getAccountStatus()).isEqualTo("PENDING_ACTIVATION");
        assertThat(user.getRole()).isEqualTo(Role.MOTHER);
        assertThat(user.getPasswordHash()).isNotNull();

        // Verify OTP was created
        assertThat(otpVerificationRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(user.getId())).isPresent();
    }

    @Test
    void register_WithValidPhone_ShouldCreateUserAndReturnSuccess() throws Exception {
        // Given
        String phone = "+84901234567";
        String password = "MyP@ssw0rd123";

        RegisterRequest request = new RegisterRequest();
        request.setEmail(null);
        request.setPhone(phone);
        request.setPassword(password);
        request.setRole(Role.EXPERT);

        // When/Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.message").value("Registration initiated. Please verify your OTP."))
                .andExpect(jsonPath("$.data.userId").exists());

        // Verify user was created
        Optional<User> userOpt = userRepository.findByPhone(phone);
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getRole()).isEqualTo(Role.EXPERT);
    }

    @Test
    void register_WithWeakPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPhone(null);
        request.setPassword("weak");
        request.setRole(Role.MOTHER);

        // When/Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void register_WithDuplicateEmail_ShouldReturnBadRequest() throws Exception {
        // Given - First registration
        String email = "duplicate@example.com";
        RegisterRequest firstRequest = new RegisterRequest();
        firstRequest.setEmail(email);
        firstRequest.setPhone(null);
        firstRequest.setPassword("MyP@ssw0rd123");
        firstRequest.setRole(Role.MOTHER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)));

        // When/Then - Second registration with same email
        RegisterRequest secondRequest = new RegisterRequest();
        secondRequest.setEmail(email);
        secondRequest.setPhone("+84987654321");
        secondRequest.setPassword("AnotherPass123!");
        secondRequest.setRole(Role.MOTHER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithInvalidEmailFormat_ShouldReturnBadRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");
        request.setPhone(null);
        request.setPassword("MyP@ssw0rd123");
        request.setRole(Role.MOTHER);

        // When/Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithInvalidPhoneFormat_ShouldReturnBadRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail(null);
        request.setPhone("0901234567"); // Not E.164
        request.setPassword("MyP@ssw0rd123");
        request.setRole(Role.MOTHER);

        // When/Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithNoEmailOrPhone_ShouldReturnBadRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail(null);
        request.setPhone(null);
        request.setPassword("MyP@ssw0rd123");
        request.setRole(Role.MOTHER);

        // When/Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== OTP Verification Flow Tests ==========

    @Test
    void verifyOtp_WithValidOtp_ShouldActivateUserAndReturnTokens() throws Exception {
        // Given: Register a user via email (covers the Story 1.2 email-channel fix).
        String email = "verify@example.com";
        String password = "MyP@ssw0rd123";
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(email);
        registerRequest.setPhone(null);
        registerRequest.setPassword(password);
        registerRequest.setRole(Role.MOTHER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Capture the OTP delivered via the mocked EmailService.
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce())
                .sendOtpVerificationEmail(eq(email), otpCaptor.capture(), eq(5));
        String capturedOtp = otpCaptor.getValue();
        assertThat(capturedOtp).hasSize(6);

        Optional<User> userOpt = userRepository.findByEmail(email);
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        assertThat(user.isEnabled()).isFalse();

        // When: Verify with the captured OTP via the email field.
        VerifyOtpRequest verifyRequest = new VerifyOtpRequest();
        verifyRequest.setEmail(email);
        verifyRequest.setOtp(capturedOtp);

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andDo(result -> {
                    if (result.getResolvedException() != null) {
                        result.getResolvedException().printStackTrace();
                    }
                })
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user.email").value(email));

        // Then: User is activated, OTP is marked used, and attempts remain 5.
        User activated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(activated.isEnabled()).isTrue();
        assertThat(activated.getAccountStatus()).isEqualTo("ACTIVE");

        Optional<OtpVerification> otpAfter = otpVerificationRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(user.getId());
        assertThat(otpAfter).isEmpty();
        var usedOtp = otpVerificationRepository.findAll().stream()
                .filter(o -> o.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(usedOtp.getUsedAt()).isNotNull();
        assertThat(usedOtp.isVerified()).isTrue();
        assertThat(usedOtp.getAttempts()).isEqualTo(5);
    }

    @Test
    void verifyOtp_WithMixedCaseAndWhitespaceEmail_ShouldStillActivateUser() throws Exception {
        // Story 1.2 fix coverage: verifyOtp normalizes the email so that mixed
        // case and surrounding whitespace resolve to the same canonical form
        // already persisted by register().
        String email = "mixedcase@example.com";
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(email);
        registerRequest.setPhone(null);
        registerRequest.setPassword("MyP@ssw0rd123");
        registerRequest.setRole(Role.MOTHER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce())
                .sendOtpVerificationEmail(eq(email), otpCaptor.capture(), eq(5));
        String capturedOtp = otpCaptor.getValue();

        VerifyOtpRequest verifyRequest = new VerifyOtpRequest();
        verifyRequest.setEmail("  MixedCase@Example.COM  ");
        verifyRequest.setOtp(capturedOtp);

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value(email));
    }

    @Test
    void verifyOtp_WithWrongOtp_ShouldDecrementAttemptsAndReturnError() throws Exception {
        // Given: Register and get OTP record
        String phone = "+84901234567";
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(null);
        registerRequest.setPhone(phone);
        registerRequest.setPassword("MyP@ssw0rd123");
        registerRequest.setRole(Role.EXPERT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        Optional<User> userOpt = userRepository.findByPhone(phone);
        assertThat(userOpt).isPresent();
        User user = userOpt.get();

        var otpOpt = otpVerificationRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(user.getId());
        assertThat(otpOpt).isPresent();

        // When: Submit wrong OTP
        VerifyOtpRequest verifyRequest = new VerifyOtpRequest();
        verifyRequest.setPhone(phone);
        verifyRequest.setOtp("000000"); // Wrong OTP

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid OTP"));

        // Then: Attempts should decrement
        var updatedOtpOpt = otpVerificationRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(user.getId());
        assertThat(updatedOtpOpt).isPresent();
        var updatedOtp = updatedOtpOpt.get();
        assertThat(updatedOtp.getAttempts()).isEqualTo(4); // Started at 5, now 4
    }

    @Test
    void verifyOtp_WithExpiredOtp_ShouldReturnError() throws Exception {
        // Given: Create expired OTP record directly
        String email = "expired@example.com";
        User user = User.builder()
                .email(email)
                .phone(null)
                .role(Role.MOTHER)
                .passwordHash("hashed_password")
                .enabled(false)
                .accountStatus("PENDING_ACTIVATION")
                .build();
        user = userRepository.save(user);

        var expiredOtp = OtpVerification.builder()
                .user(user)
                .codeHash("somehash")
                .phone(null)
                .purpose(OtpVerification.OtpPurpose.REGISTER)
                .expiresAt(Instant.now().minusSeconds(300)) // Expired 5 min ago
                .attempts(5)
                .verified(false)
                .build();
        otpVerificationRepository.save(expiredOtp);

        // When: Try to verify with expired OTP
        VerifyOtpRequest verifyRequest = new VerifyOtpRequest();
        verifyRequest.setEmail(email);
        verifyRequest.setOtp("123456");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP"));
    }

    // ========== OTP Resend Flow Tests ==========

    @Test
    void resendOtp_WithValidRequest_ShouldSendNewOtpAndReturnSuccess() throws Exception {
        // Given: Register a user
        String phone = "+84911234567";
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(null);
        registerRequest.setPhone(phone);
        registerRequest.setPassword("MyP@ssw0rd123");
        registerRequest.setRole(Role.MOTHER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        Optional<User> userOpt = userRepository.findByPhone(phone);
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        OtpVerification originalOtp = otpVerificationRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(user.getId())
                .orElseThrow();
        long auditCountBefore = auditLogRepository.findAll().stream()
                .filter(log -> log.getActorUserId().equals(user.getId()))
                .filter(log -> log.getAction() == AuditAction.OTP_RESENT)
                .count();
        clearInvocations(emailService, smsService);

        // When: Request resend
        ResendOtpRequest resendRequest = new ResendOtpRequest();
        resendRequest.setPhone(phone);
        resendRequest.setEmail(null);

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("OTP resent successfully"))
                .andExpect(jsonPath("$.data.otpExpiresAt").exists())
                .andExpect(jsonPath("$.data.resendCooldownRemaining").isNumber());

        var userOtps = otpVerificationRepository.findAll().stream()
                .filter(otp -> otp.getUser().getId().equals(user.getId()))
                .toList();
        assertThat(userOtps).hasSize(2);
        OtpVerification persistedOriginal = otpVerificationRepository.findById(originalOtp.getId()).orElseThrow();
        assertThat(persistedOriginal.getUsedAt()).isNotNull();
        var pendingOtps = userOtps.stream()
                .filter(otp -> otp.getUsedAt() == null)
                .toList();
        assertThat(pendingOtps).hasSize(1);
        OtpVerification replacement = pendingOtps.get(0);
        assertThat(replacement.getId()).isNotEqualTo(originalOtp.getId());
        assertThat(replacement.getPhone()).isEqualTo(phone);
        assertThat(replacement.getEmail()).isNull();
        verify(smsService).sendOtpVerificationSms(eq(phone), anyString(), eq(5));
        verifyNoInteractions(emailService);

        long auditCountAfter = auditLogRepository.findAll().stream()
                .filter(log -> log.getActorUserId().equals(user.getId()))
                .filter(log -> log.getAction() == AuditAction.OTP_RESENT)
                .count();
        assertThat(auditCountAfter).isEqualTo(auditCountBefore + 1);
    }

    @Test
    void resendOtp_WithPhoneAndEmail_ShouldRejectWithoutSideEffects() throws Exception {
        String phone = "+84922222222";
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setPhone(phone);
        registerRequest.setPassword("MyP@ssw0rd123");
        registerRequest.setRole(Role.MOTHER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        User user = userRepository.findByPhone(phone).orElseThrow();
        OtpVerification originalOtp = otpVerificationRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(user.getId())
                .orElseThrow();
        long auditCountBefore = auditLogRepository.findAll().stream()
                .filter(log -> log.getActorUserId().equals(user.getId()))
                .filter(log -> log.getAction() == AuditAction.OTP_RESENT)
                .count();
        clearInvocations(emailService, smsService);

        ResendOtpRequest mixedRequest = new ResendOtpRequest();
        mixedRequest.setPhone(phone);
        mixedRequest.setEmail("attacker@example.com");

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mixedRequest)))
                .andExpect(status().isBadRequest());

        OtpVerification pendingAfterRejection = otpVerificationRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(user.getId())
                .orElseThrow();
        assertThat(pendingAfterRejection.getId()).isEqualTo(originalOtp.getId());
        assertThat(pendingAfterRejection.getCodeHash()).isEqualTo(originalOtp.getCodeHash());
        assertThat(auditLogRepository.findAll().stream()
                .filter(log -> log.getActorUserId().equals(user.getId()))
                .filter(log -> log.getAction() == AuditAction.OTP_RESENT)
                .count()).isEqualTo(auditCountBefore);
        verifyNoInteractions(emailService, smsService);

        ResendOtpRequest validRequest = new ResendOtpRequest();
        validRequest.setPhone(phone);
        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void resendOtp_WithoutPhoneOrEmail_ShouldReturnBadRequest() throws Exception {
        // Given: Empty request
        ResendOtpRequest resendRequest = new ResendOtpRequest();
        resendRequest.setPhone(null);
        resendRequest.setEmail(null);

        // When/Then
        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendOtp_WithNonExistentUser_ShouldReturnNotFound() throws Exception {
        // Given: Request for user that doesn't exist
        ResendOtpRequest resendRequest = new ResendOtpRequest();
        resendRequest.setPhone("+84999999999");
        resendRequest.setEmail(null);

        // When/Then
        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void resendOtp_WhenNoPendingOtp_ShouldReturnBadRequest() throws Exception {
        // Given: User exists but no pending OTP
        String phone = "+84987654321";
        User user = User.builder()
                .phone(phone)
                .email(null)
                .role(Role.MOTHER)
                .passwordHash("hashed_password")
                .enabled(true) // Already active
                .accountStatus("ACTIVE")
                .build();
        user = userRepository.save(user);

        ResendOtpRequest resendRequest = new ResendOtpRequest();
        resendRequest.setPhone(phone);
        resendRequest.setEmail(null);

        // When/Then
        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No pending OTP verification found. Please request a new OTP."));
    }

    @Test
    void resendOtp_WithMultiplePendingRows_ShouldInvalidateNewestOtp() throws Exception {
        String phone = "+84933333333";
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setPhone(phone);
        registerRequest.setPassword("MyP@ssw0rd123");
        registerRequest.setRole(Role.MOTHER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        User user = userRepository.findByPhone(phone).orElseThrow();
        OtpVerification olderPending = otpVerificationRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(user.getId())
                .orElseThrow();
        OtpVerification newerPending = otpVerificationRepository.saveAndFlush(
                OtpVerification.builder()
                        .user(user)
                        .codeHash("newer_hash")
                        .phone(phone)
                        .purpose(OtpVerification.OtpPurpose.REGISTER)
                        .expiresAt(Instant.now().plusSeconds(300))
                        .attempts(5)
                        .verified(false)
                        .build());

        jdbcTemplate.update(
                "UPDATE otp_verifications SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-06-22T00:00:00Z")),
                olderPending.getId());
        jdbcTemplate.update(
                "UPDATE otp_verifications SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-06-22T00:00:00Z")),
                newerPending.getId());
        clearInvocations(emailService, smsService);

        ResendOtpRequest resendRequest = new ResendOtpRequest();
        resendRequest.setPhone(phone);
        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendRequest)))
                .andExpect(status().isOk());

        assertThat(otpVerificationRepository.findById(olderPending.getId()).orElseThrow().getUsedAt()).isNull();
        assertThat(otpVerificationRepository.findById(newerPending.getId()).orElseThrow().getUsedAt()).isNotNull();
        verify(smsService).sendOtpVerificationSms(eq(phone), anyString(), eq(5));
        verifyNoInteractions(emailService);
    }

    @Test
    void resendOtp_WithinCooldownPeriod_ShouldReturnTooManyRequests() throws Exception {
        // Given: Register a user
        String phone = "+84955555555";
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(null);
        registerRequest.setPhone(phone);
        registerRequest.setPassword("MyP@ssw0rd123");
        registerRequest.setRole(Role.MOTHER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // First resend (should succeed)
        ResendOtpRequest resendRequest = new ResendOtpRequest();
        resendRequest.setPhone(phone);

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendRequest)))
                .andExpect(status().isOk());

        // Second resend immediately (should fail with 429)
        mockMvc.perform(post("/api/v1/auth/resend-otp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendRequest)))
                .andExpect(status().isTooManyRequests())
              .andExpect(jsonPath("$.message").value(
                      org.hamcrest.Matchers.matchesPattern(
                              "Please wait before resending OTP\\. Cooldown: (59|60) seconds")));
    }
}

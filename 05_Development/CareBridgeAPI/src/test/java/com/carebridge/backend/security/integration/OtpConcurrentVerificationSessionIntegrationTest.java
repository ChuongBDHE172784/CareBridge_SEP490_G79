package com.carebridge.backend.security.integration;

import com.carebridge.backend.security.dto.request.VerifyOtpRequest;
import com.carebridge.backend.security.entity.OtpVerification;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.OtpVerificationRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.AuthService;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.security.util.TokenUtils;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ITS-SEC-002 — full Expected Result of the suggested integration test case:
 * "One request wins; the proof is consumed once; no duplicate user/session is created."
 *
 * <p>Created during integration-test execution verification because the existing
 * {@link OtpRaceConditionIntegrationTest} deliberately does NOT assert two of those three
 * clauses: it bounds successes to {@code 1..2} rather than exactly one, and it never counts
 * session rows. This test asserts the clauses as written so the real behaviour is recorded
 * instead of assumed.
 *
 * <p>Production code is untouched. If this test fails, the failure is the finding.
 */
@Isolated
class OtpConcurrentVerificationSessionIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String EMAIL = "its.sec002.session@test.com";
    private static final String OTP_CODE = "654321";

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private OtpVerificationRepository otpVerificationRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private EmailService emailService;
    @MockitoBean private SmsService smsService;

    @Test
    void concurrentVerify_electsOneWinner_consumesProofOnce_andCreatesNoDuplicateSession()
            throws Exception {
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);

        User user = userRepository.save(User.builder()
                .email(EMAIL)
                .role(Role.MOTHER)
                .passwordHash("$2a$10$abcdefghijklmnopqrstuv")
                .enabled(false)
                .locked(false)
                .emailVerified(false)
                .phoneVerified(false)
                .accountStatus("PENDING_ACTIVATION")
                .build());

        otpVerificationRepository.save(OtpVerification.builder()
                .user(user)
                .codeHash(TokenUtils.hashSha256(OTP_CODE))
                .email(EMAIL)
                .purpose(OtpVerification.OtpPurpose.REGISTER)
                .expiresAt(Instant.now().plusSeconds(300))
                .attempts(5)
                .verified(false)
                .build());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> task = () -> {
            RequestContextHolder.setRequestAttributes(
                    new ServletRequestAttributes(new MockHttpServletRequest()));
            try {
                start.await();
                VerifyOtpRequest req = new VerifyOtpRequest();
                req.setEmail(EMAIL);
                req.setOtp(OTP_CODE);
                authService.verifyOtp(req);
                return "SUCCESS";
            } catch (Exception ex) {
                return ex.getClass().getSimpleName();
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        };

        Future<String> f1 = pool.submit(task);
        Future<String> f2 = pool.submit(task);
        start.countDown();
        String r1 = f1.get();
        String r2 = f2.get();
        pool.shutdown();

        long successes = List.of(r1, r2).stream().filter("SUCCESS"::equals).count();

        // Clause 2 — the proof is consumed exactly once.
        List<OtpVerification> otps = otpVerificationRepository.findAll().stream()
                .filter(otp -> otp.getUser() != null && otp.getUser().getId().equals(user.getId()))
                .toList();
        assertThat(otps).as("one OTP row for the user").hasSize(1);
        assertThat(otps.get(0).getUsedAt()).as("OTP consumed").isNotNull();

        // Clause 3a — no duplicate user.
        Long userRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE email = ?", Long.class, EMAIL);
        assertThat(userRows).as("no duplicate user row").isEqualTo(1L);

        // Clause 3b — no duplicate session.
        Long sessionRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM auth_sessions WHERE user_id = ?", Long.class, user.getId());
        assertThat(sessionRows).as("no duplicate auth session for the raced verification")
                .isLessThanOrEqualTo(1L);

        // Clause 1 — exactly one request wins.
        assertThat(successes).as("exactly one verification request wins the race").isEqualTo(1L);
    }
}

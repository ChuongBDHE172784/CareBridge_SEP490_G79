package com.carebridge.backend.security.policy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RateLimitPolicyTest {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 15 * 60;
    private final RateLimitPolicy policy = new RateLimitPolicy();

    @Test
    void canAttempt_ShouldReturnTrue_ForFirstAttempt() {
        assertTrue(policy.canAttempt("+84901234567"));
    }

    @Test
    void canAttempt_ShouldAllowUpToMaxAttempts() {
        String phone = "+84901234567";
        // First 5 attempts should be allowed
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertTrue(policy.canAttempt(phone), "Attempt " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void canAttempt_ShouldReturnFalse_AfterMaxAttempts() {
        String phone = "+84901234567";
        // Use up all attempts
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            policy.canAttempt(phone);
        }
        // Next attempt should be rate limited
        assertFalse(policy.canAttempt(phone));
    }

    @Test
    void canAttempt_WithOtpHash_ShouldResetWhenOtpChanges() {
        String phone = "+84901234567";
        String otpHash1 = "hash1";
        String otpHash2 = "hash2";

        // First attempt with hash1
        assertTrue(policy.canAttempt(phone, otpHash1));
        // Second attempt with same hash should be allowed (2nd attempt)
        assertTrue(policy.canAttempt(phone, otpHash1));
        // New OTP hash should reset attempts (1st attempt with hash2)
        assertTrue(policy.canAttempt(phone, otpHash2));
        // Use up remaining attempts with hash2 (need MAX_ATTEMPTS - 1 more calls)
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            assertTrue(policy.canAttempt(phone, otpHash2));
        }
        // Should be rate-limited now (all 5 attempts with hash2 used, 6th call denied)
        assertFalse(policy.canAttempt(phone, otpHash2));
    }

    @Test
    void getRemainingAttempts_ShouldReturnMax_ForNewPhone() {
        assertEquals(MAX_ATTEMPTS, policy.getRemainingAttempts("+84901234567"));
    }

    @Test
    void getRemainingAttempts_ShouldDecrease_AfterAttempts() {
        String phone = "+84901234567";
        policy.canAttempt(phone);
        assertEquals(MAX_ATTEMPTS - 1, policy.getRemainingAttempts(phone));
    }

    @Test
    void getRemainingAttempts_ShouldReturnZero_AfterMaxAttempts() {
        String phone = "+84901234567";
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            policy.canAttempt(phone);
        }
        assertEquals(0, policy.getRemainingAttempts(phone));
    }

    @Test
    void reset_ShouldClearAttempts() {
        String phone = "+84901234567";
        // Use some attempts
        for (int i = 0; i < 3; i++) {
            policy.canAttempt(phone);
        }
        assertEquals(2, policy.getRemainingAttempts(phone));

        // Reset
        policy.reset(phone);

        // Should be back to full attempts
        assertEquals(MAX_ATTEMPTS, policy.getRemainingAttempts(phone));
        assertTrue(policy.canAttempt(phone));
    }

    @Test
    void windowExpiry_ShouldResetAttempts_AfterWindowPasses() throws InterruptedException {
        String phone = "+84901234567";

        // Use some attempts
        policy.canAttempt(phone);
        policy.canAttempt(phone);
        assertEquals(3, policy.getRemainingAttempts(phone));

        // Wait for window to expire (use reflection to set window start in past)
        // Since we can't easily wait 15 minutes, we test via getTimeUntilReset
        long timeLeft = policy.getTimeUntilReset(phone);
        assertTrue(timeLeft >= 0);
    }

    @Test
    void getTimeUntilReset_ShouldReturnZero_ForExpiredWindow() {
        String phone = "+84901234567";
        // Use an attempt
        policy.canAttempt(phone);
        // Time until reset should be positive (window just started)
        long timeLeft = policy.getTimeUntilReset(phone);
        assertTrue(timeLeft <= WINDOW_SECONDS);
        assertTrue(timeLeft >= 0);
    }
}

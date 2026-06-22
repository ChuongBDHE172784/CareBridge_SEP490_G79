package com.carebridge.backend.security.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitPolicyResendTest {

    private RateLimitPolicy rateLimitPolicy;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-06-22T00:00:00Z"), ZoneOffset.UTC);
        rateLimitPolicy = new RateLimitPolicy(clock);
    }

    @Test
    void canResendOtp_WhenNoPreviousResend_ShouldReturnTrue() {
        // Given/When
        boolean result = rateLimitPolicy.canResendOtp("+84901234567");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void tryConsumeResend_ShouldAtomicallyAllowFirstAndRejectSecondAttempt() {
        String identifier = "+84901111111";

        assertThat(rateLimitPolicy.tryConsumeResend(identifier)).isTrue();
        assertThat(rateLimitPolicy.tryConsumeResend(identifier)).isFalse();
        assertThat(rateLimitPolicy.getTimeUntilResendReset(identifier)).isBetween(59L, 60L);
    }

    @Test
    void tryConsumeResend_WithConcurrentFirstAttempts_ShouldAllowExactlyOne() throws Exception {
        long successfulConsumes = countSuccessfulConcurrentConsumes("account-1", 24);

        assertThat(successfulConsumes).isEqualTo(1);
    }

    @Test
    void tryConsumeResend_AtExpiredBoundaryWithConcurrentAttempts_ShouldAllowExactlyOne() throws Exception {
        String accountKey = "account-2";
        assertThat(rateLimitPolicy.tryConsumeResend(accountKey)).isTrue();
        clock.advance(Duration.ofSeconds(60));

        long successfulConsumes = countSuccessfulConcurrentConsumes(accountKey, 24);

        assertThat(successfulConsumes).isEqualTo(1);
        assertThat(rateLimitPolicy.tryConsumeResend(accountKey)).isFalse();
    }

    @Test
    void tryConsumeResend_RacingLazyCleanup_ShouldKeepRefreshedCooldown() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            for (int i = 0; i < 100; i++) {
                String accountKey = "cleanup-race-" + i;
                rateLimitPolicy.recordResendAttempt(accountKey);
                clock.advance(Duration.ofSeconds(60));
                CountDownLatch start = new CountDownLatch(1);

                Future<Boolean> consume = executor.submit(() -> {
                    start.await();
                    return rateLimitPolicy.tryConsumeResend(accountKey);
                });
                Future<Boolean> inspect = executor.submit(() -> {
                    start.await();
                    return rateLimitPolicy.canResendOtp(accountKey);
                });

                start.countDown();
                assertThat(consume.get(5, TimeUnit.SECONDS)).isTrue();
                inspect.get(5, TimeUnit.SECONDS);
                assertThat(rateLimitPolicy.tryConsumeResend(accountKey)).isFalse();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void canResendOtp_AfterResend_ShouldReturnFalseWithinCooldown() {
        // Given
        String identifier = "+84901234567";
        rateLimitPolicy.recordResendAttempt(identifier);

        // When
        boolean result = rateLimitPolicy.canResendOtp(identifier);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void canResendOtp_AfterCooldownPeriod_ShouldReturnTrue() {
        // Given
        String identifier = "+84901234567";
        rateLimitPolicy.recordResendAttempt(identifier);

        assertThat(rateLimitPolicy.canResendOtp(identifier)).isFalse();

        clock.advance(Duration.ofSeconds(60));

        assertThat(rateLimitPolicy.canResendOtp(identifier)).isTrue();
        assertThat(rateLimitPolicy.getTimeUntilResendReset(identifier)).isZero();
    }

    @Test
    void recordResendAttempt_ShouldUpdateLastResendAt() {
        // Given
        String identifier = "+84901234567";
        Instant before = Instant.now();

        // When
        rateLimitPolicy.recordResendAttempt(identifier);

        // Then
        long cooldown = rateLimitPolicy.getTimeUntilResendReset(identifier);
        assertThat(cooldown).isEqualTo(60);

        // Verify internal state indirectly through canResend
        assertThat(rateLimitPolicy.canResendOtp(identifier)).isFalse();
    }

    @Test
    void getTimeUntilResendReset_WhenNoResend_ShouldReturnZero() {
        // Given/When
        long cooldown = rateLimitPolicy.getTimeUntilResendReset("+84901234567");

        // Then
        assertThat(cooldown).isZero();
    }

    @Test
    void getTimeUntilResendReset_AfterResend_ShouldReturnRemainingSeconds() {
        // Given
        String identifier = "+84901234567";
        rateLimitPolicy.recordResendAttempt(identifier);

        // When
        long cooldown = rateLimitPolicy.getTimeUntilResendReset(identifier);

        // Then
        assertThat(cooldown).isEqualTo(60);
    }

    @Test
    void resetResend_ShouldClearResendRecord() {
        // Given
        String identifier = "+84901234567";
        rateLimitPolicy.recordResendAttempt(identifier);
        assertThat(rateLimitPolicy.canResendOtp(identifier)).isFalse();

        // When
        rateLimitPolicy.resetResend(identifier);

        // Then
        assertThat(rateLimitPolicy.canResendOtp(identifier)).isTrue();
        assertThat(rateLimitPolicy.getTimeUntilResendReset(identifier)).isZero();
    }

    @Test
    void resendWithDifferentIdentifiers_ShouldHaveIndependentCooldowns() {
        // Given
        String phone1 = "+84901234567";
        String phone2 = "+84987654321";

        rateLimitPolicy.recordResendAttempt(phone1);

        // When/Then
        assertThat(rateLimitPolicy.canResendOtp(phone1)).isFalse();
        assertThat(rateLimitPolicy.canResendOtp(phone2)).isTrue(); // phone2 can still resend

        rateLimitPolicy.recordResendAttempt(phone2);

        assertThat(rateLimitPolicy.canResendOtp(phone1)).isFalse();
        assertThat(rateLimitPolicy.canResendOtp(phone2)).isFalse();
    }

    @Test
    void resendWithEmail_ShouldWorkWithEmailAsIdentifier() {
        // Given
        String email = "test@example.com";

        rateLimitPolicy.recordResendAttempt(email);

        // When/Then
        assertThat(rateLimitPolicy.canResendOtp(email)).isFalse();
        assertThat(rateLimitPolicy.getTimeUntilResendReset(email)).isEqualTo(60);

        rateLimitPolicy.resetResend(email);
        assertThat(rateLimitPolicy.canResendOtp(email)).isTrue();
    }

    private long countSuccessfulConcurrentConsumes(String accountKey, int callerCount) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(callerCount);
        CountDownLatch ready = new CountDownLatch(callerCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int i = 0; i < callerCount; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent consume start timed out");
                    }
                    return rateLimitPolicy.tryConsumeResend(accountKey);
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            long successfulConsumes = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5, TimeUnit.SECONDS)) {
                    successfulConsumes++;
                }
            }
            return successfulConsumes;
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private static final class MutableClock extends Clock {
        private volatile Instant currentInstant;
        private final ZoneId zone;

        private MutableClock(Instant currentInstant, ZoneId zone) {
            this.currentInstant = currentInstant;
            this.zone = zone;
        }

        void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {
            return new MutableClock(currentInstant, newZone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}

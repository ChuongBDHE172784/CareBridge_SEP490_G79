package com.carebridge.backend.security.policy;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * Rate limiting policy cho OTP verification attempts và resend.
 *
 * Verification attempts: 5 attempts trong 15 phút cho mỗi phone number.
 * Resend cooldown: 1 resend mỗi 60 giây cho mỗi phone/email.
 */
@Component
public class RateLimitPolicy {

    private static final int MAX_ATTEMPTS = 5;
    private static final long ATTEMPT_WINDOW_SECONDS = 15 * 60; // 15 minutes
    private static final long RESEND_COOLDOWN_SECONDS = 60; // 60 seconds
    private final Clock clock;

    public RateLimitPolicy() {
        this(Clock.systemUTC());
    }

    RateLimitPolicy(Clock clock) {
        this.clock = clock;
    }

    /**
     * Tracking attempts theo phone number.
     * Key: phone number
     * Value: AttemptInfo với attempts count và window start
     */
    private static class AttemptInfo {
        int attempts;
        Instant windowStart;
        String lastOtpHash; // để đảm bảo cùng một OTP trong window

        AttemptInfo(int attempts, Instant windowStart, String lastOtpHash) {
            this.attempts = attempts;
            this.windowStart = windowStart;
            this.lastOtpHash = lastOtpHash;
        }
    }

    private final Map<String, AttemptInfo> attemptMap = new ConcurrentHashMap<>();

    /**
     * Kiểm tra xem phone number có vượt quá giới hạn attempts không.
     * Nếu window đã hết, reset attempts.
     *
     * @param phone phone number
     * @return true nếu vẫn trong giới hạn, false nếu bị rate limited
     */
    public boolean canAttempt(String phone) {
        cleanExpiredWindows();

        Instant now = clock.instant();
        AtomicBoolean allowed = new AtomicBoolean(false);

        attemptMap.compute(phone, (key, existing) -> {
            if (existing == null) {
                allowed.set(true);
                return new AttemptInfo(1, now, null);
            }

            // Check if window has expired - reset
            if (now.isAfter(existing.windowStart.plusSeconds(ATTEMPT_WINDOW_SECONDS))) {
                allowed.set(true);
                return new AttemptInfo(1, now, existing.lastOtpHash);
            }

            // Within same window
            if (existing.attempts < MAX_ATTEMPTS) {
                existing.attempts++;
                allowed.set(true);
                return existing;
            }

            allowed.set(false);
            return existing; // at limit
        });

        return allowed.get();
    }

    /**
     * Lấy số attempts còn lại trong window hiện tại.
     *
     * @param phone phone number
     * @return số attempts còn lại (0-5)
     */
    public int getRemainingAttempts(String phone) {
        cleanExpiredWindows();

        AttemptInfo info = attemptMap.get(phone);
        if (info == null) {
            return MAX_ATTEMPTS;
        }

        Instant now = clock.instant();
        if (now.isAfter(info.windowStart.plusSeconds(ATTEMPT_WINDOW_SECONDS))) {
            return MAX_ATTEMPTS;
        }

        return Math.max(0, MAX_ATTEMPTS - info.attempts);
    }

    /**
     * Reset rate limit cho phone number (dùng cho testing hoặc admin actions).
     *
     * @param phone phone number
     */
    public void reset(String phone) {
        attemptMap.remove(phone);
    }

    /**
     * Lấy thời gian còn lại trước khi window reset (giây).
     *
     * @param phone phone number
     * @return số giây còn lại, hoặc 0 nếu không có attempt nào
     */
    public long getTimeUntilReset(String phone) {
        AttemptInfo info = attemptMap.get(phone);
        if (info == null) {
            return 0;
        }

        Instant now = clock.instant();
        Instant windowEnd = info.windowStart.plusSeconds(ATTEMPT_WINDOW_SECONDS);

        if (now.isAfter(windowEnd)) {
            return 0;
        }

        return windowEnd.getEpochSecond() - now.getEpochSecond();
    }

    // ========== Resend Rate Limiting (60 second cooldown) ==========

    /**
     * Tracking resend attempts theo phone/email identifier.
     * Key: phone number or email
     * Value: ResendInfo với last resend timestamp
     */
    private static class ResendInfo {
        Instant lastResendAt;

        ResendInfo(Instant lastResendAt) {
            this.lastResendAt = lastResendAt;
        }
    }

    private final Map<String, ResendInfo> resendMap = new ConcurrentHashMap<>();

    /**
     * Kiểm tra xem có thể resend OTP không (60 second cooldown).
     *
     * @param identifier phone number or email
     * @return true nếu đã đủ 60 giây từ lần resend cuối, false nếu chưa
     */
    public boolean canResendOtp(String identifier) {
        cleanExpiredResends();

        ResendInfo info = resendMap.get(identifier);
        Instant now = clock.instant();

        if (info == null) {
            // No previous resend, allowed
            return true;
        }

        // Check if cooldown period has passed
        if (!now.isBefore(info.lastResendAt.plusSeconds(RESEND_COOLDOWN_SECONDS))) {
            return true;
        }

        return false; // Still in cooldown
    }

    /**
     * Ghi nhận một resend attempt cho identifier.
     * Update lastResendAt timestamp.
     *
     * @param identifier phone number or email
     */
    public void recordResendAttempt(String identifier) {
        resendMap.put(identifier, new ResendInfo(clock.instant()));
    }

    /**
     * Atomic check-and-record for resend to prevent race conditions.
     * Returns true if resend is allowed and was recorded, false if cooldown not elapsed.
     *
     * @param identifier phone number or email
     * @return true if resend slot consumed, false if still in cooldown
     */
    public boolean tryConsumeResend(String identifier) {
        Instant now = clock.instant();
        AtomicBoolean allowed = new AtomicBoolean(false);

        resendMap.compute(identifier, (key, existing) -> {
            if (existing == null) {
                allowed.set(true);
                return new ResendInfo(now);
            }
            if (!now.isBefore(existing.lastResendAt.plusSeconds(RESEND_COOLDOWN_SECONDS))) {
                allowed.set(true);
                return new ResendInfo(now);
            }
            return existing;
        });
        return allowed.get();
    }

    /**
     * Lấy thời gian còn lại trước khi có thể resend lại (giây).
     *
     * @param identifier phone number or email
     * @return số giây còn lại, hoặc 0 nếu có thể resend ngay
     */
    public long getTimeUntilResendReset(String identifier) {
        ResendInfo info = resendMap.get(identifier);
        if (info == null) {
            return 0;
        }

        Instant now = clock.instant();
        Instant cooldownEnd = info.lastResendAt.plusSeconds(RESEND_COOLDOWN_SECONDS);

        if (!now.isBefore(cooldownEnd)) {
            return 0;
        }

        return cooldownEnd.getEpochSecond() - now.getEpochSecond();
    }

    /**
     * Reset resend rate limit cho identifier (dùng cho testing hoặc admin actions).
     *
     * @param identifier phone number or email
     */
    public void resetResend(String identifier) {
        resendMap.remove(identifier);
    }

    /**
     * Xóa các resend entries đã hết cooldown để tránh memory leak.
     */
    private void cleanExpiredResends() {
        Instant now = clock.instant();
        for (String key : resendMap.keySet()) {
            resendMap.computeIfPresent(key, (ignored, current) ->
                    !now.isBefore(current.lastResendAt.plusSeconds(RESEND_COOLDOWN_SECONDS))
                            ? null
                            : current);
        }
    }

    /**
     * Xóa tất cả rate limits (cleanup method, có thể gọi periodically).
     */
    private void cleanExpiredWindows() {
        Instant now = clock.instant();
        attemptMap.entrySet().removeIf(entry -> {
            AttemptInfo info = entry.getValue();
            return now.isAfter(info.windowStart.plusSeconds(ATTEMPT_WINDOW_SECONDS));
        });
        cleanExpiredResends();
    }
}

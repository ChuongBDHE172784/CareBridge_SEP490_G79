package com.carebridge.backend.security.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Response DTO cho OTP resend operation.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class OtpResendResponse {

    /**
     * Thời gian hết hạn của OTP mới (5 phút từ thời điểm resend).
     */
    private Instant otpExpiresAt;

    /**
     * Thời gian còn lại trước khi có thể resend lại (giây).
     */
    private long resendCooldownRemaining;

    /**
     * Thông báo tùy chọn (ví dụ: "OTP sent successfully").
     */
    private String message;
}

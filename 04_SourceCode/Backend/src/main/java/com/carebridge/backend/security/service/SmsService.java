package com.carebridge.backend.security.service;

import org.springframework.stereotype.Service;

/**
 * Interface cho SMS service.
 */
@Service
public interface SmsService {

    /**
     * Gửi SMS OTP verification.
     *
     * @param to số điện thoại người nhận (định dạng E.164)
     * @param otp mã OTP (plain text để hiển thị)
     * @param expiryMinutes thời gian hết hạn tính bằng phút
     */
    void sendOtpVerificationSms(String to, String otp, int expiryMinutes);
}
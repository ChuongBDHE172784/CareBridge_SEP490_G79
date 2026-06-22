package com.carebridge.backend.security.service;

import org.springframework.stereotype.Service;

/**
 * Interface cho email service.
 */
@Service
public interface EmailService {

    /**
     * Gửi email OTP verification.
     *
     * @param to địa chỉ email người nhận
     * @param otp mã OTP (plain text để hiển thị)
     * @param expiryMinutes thời gian hết hạn tính bằng phút
     */
    void sendOtpVerificationEmail(String to, String otp, int expiryMinutes);

    /**
     * Gửi email thông báo đăng ký thành công.
     *
     * @param to địa chỉ email người nhận
     * @param name tên người dùng
     */
    void sendRegistrationSuccessEmail(String to, String name);
}

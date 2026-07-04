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

    void sendPasswordResetEmail(String to, String token, int expiryMinutes);

    /**
     * UC115 Create Staff Account — one-time delivery of a system-generated temporary
     * password to a newly admin-provisioned staff account. The admin never sees this
     * value (ADR-IAM-005).
     *
     * @param to           staff member's email
     * @param name         staff member's display name
     * @param tempPassword plaintext temporary password (one-time display in this email only)
     */
    void sendStaffAccountCredentialsEmail(String to, String name, String tempPassword);
}

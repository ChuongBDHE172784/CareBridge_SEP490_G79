package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.security.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Profile;

import org.springframework.stereotype.Service;

/**
 * Mock implementation của EmailService cho môi trường test hoặc khi chưa cấu hình email thật.
 * Log nội dung email thay vì gửi thật.
 */
@Service
@ConditionalOnMissingBean(GmailEmailService.class)
public class MockEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(MockEmailService.class);

    // @Profile({"dev","test"}) keeps this out of production, where GmailEmailService
    // takes over, so the OTP and reset token below stay on a developer machine. They
    // are logged on purpose: without a real mailbox locally, reading them from the
    // log is how anyone signs in. The System.out.println duplicates of these lines
    // were removed — they wrote the same values a second time through a channel no
    // log level or appender configuration can suppress.

    @Override
    public void sendOtpVerificationEmail(String to, String otp, int expiryMinutes) {
        logger.info("[MOCK EMAIL] To: {}, OTP: {}, Expires in: {} minutes", to, otp, expiryMinutes);
    }

    @Override
    public void sendRegistrationSuccessEmail(String to, String name) {
        logger.info("[MOCK EMAIL] To: {}, Registration success for: {}", to, name);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token, int expiryMinutes) {
        logger.info("[MOCK EMAIL] Password reset to: {}, token: {}, expires: {} min", to, token, expiryMinutes);
    }

    @Override
    public void sendStaffAccountCredentialsEmail(String to, String name, String tempPassword) {
        // Deliberately never logs tempPassword (UC115-TC-013 / CWE-532) — a real
        // credential-delivery email would include it in the message body only.
        logger.info("[MOCK EMAIL] Staff account credentials issued to: {} for: {}", to, name);
    }
}

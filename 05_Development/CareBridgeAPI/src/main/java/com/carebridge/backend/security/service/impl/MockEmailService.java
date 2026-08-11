package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.security.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;

import org.springframework.stereotype.Service;

/**
 * Mock implementation của EmailService cho môi trường dev.
 * Log nội dung email thay vì gửi thật.
 */
@Service
@Profile({ "dev", "test" })
@Primary
public class MockEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(MockEmailService.class);

    @Override
    public void sendOtpVerificationEmail(String to, String otp, int expiryMinutes) {
        logger.info("[MOCK EMAIL] To: {}, OTP: {}, Expires in: {} minutes", to, otp, expiryMinutes);
        System.out.println("[MOCK EMAIL] To: " + to + ", OTP: " + otp + ", Expires in: " + expiryMinutes + " minutes");
    }

    @Override
    public void sendRegistrationSuccessEmail(String to, String name) {
        logger.info("[MOCK EMAIL] To: {}, Registration success for: {}", to, name);
        System.out.println("[MOCK EMAIL] Registration success email to: " + to + " for user: " + name);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token, int expiryMinutes) {
        logger.info("[MOCK EMAIL] Password reset to: {}, token: {}, expires: {} min", to, token, expiryMinutes);
        System.out.println("[MOCK EMAIL] Password reset to: " + to + ", token: " + token);
    }

    @Override
    public void sendStaffAccountCredentialsEmail(String to, String name, String tempPassword) {
        // Deliberately never logs tempPassword (UC115-TC-013 / CWE-532) — a real
        // credential-delivery email would include it in the message body only.
        logger.info("[MOCK EMAIL] Staff account credentials issued to: {} for: {}", to, name);
        System.out.println("[MOCK EMAIL] Staff account credentials issued to: " + to + " for user: " + name);
    }
}

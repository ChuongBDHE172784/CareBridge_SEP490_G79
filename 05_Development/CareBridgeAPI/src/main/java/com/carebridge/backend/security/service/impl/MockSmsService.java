package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.security.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Mock implementation của SmsService cho môi trường dev.
 * Log nội dung SMS thay vì gửi thật.
 */
@Service
public class MockSmsService implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(MockSmsService.class);

    // This class carries no @Profile, so it is the SmsService the production
    // deployment gets. Printing the OTP and the reset token therefore put both in
    // the container log of a public server, where reading the log is enough to
    // take over any account that can reset by phone. Only the fact that a message
    // was requested is recorded now; the value itself never leaves memory.

    @Override
    public void sendOtpVerificationSms(String to, String otp, int expiryMinutes) {
        logger.info("[MOCK SMS] OTP requested for {}, expires in {} minutes", maskRecipient(to), expiryMinutes);
    }

    @Override
    public void sendPasswordResetSms(String to, String token, int expiryMinutes) {
        logger.info("[MOCK SMS] Password reset requested for {}, expires in {} minutes",
                maskRecipient(to), expiryMinutes);
    }

    /** Keeps the log useful for tracing a report without recording a full phone number. */
    private static String maskRecipient(String to) {
        if (to == null || to.length() < 4) {
            return "***";
        }
        return "***" + to.substring(to.length() - 3);
    }
}

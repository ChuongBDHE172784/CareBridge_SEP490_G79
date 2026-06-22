package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.security.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Mock implementation của SmsService cho môi trường dev.
 * Log nội dung SMS thay vì gửi thật.
 */
@Service
@Profile("dev")
public class MockSmsService implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(MockSmsService.class);

    @Override
    public void sendOtpVerificationSms(String to, String otp, int expiryMinutes) {
        logger.info("[MOCK SMS] To: {}, OTP: {}, Expires in: {} minutes", to, otp, expiryMinutes);
        System.out.println("[MOCK SMS] To: " + to + ", OTP: " + otp + ", Expires in: " + expiryMinutes + " minutes");
    }
}
package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.security.entity.OtpVerification;
import com.carebridge.backend.security.otp.OtpGenerator;
import com.carebridge.backend.security.policy.AuthenticationPolicy;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.OtpVerificationRepository;
import com.carebridge.backend.security.service.OtpService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final AuthenticationPolicy authenticationPolicy;

    @Value("${carebridge.security.otp.expiration-seconds:300}")
    private long otpExpirationSeconds;

    @Value("${carebridge.security.otp.max-attempts:5}")
    private int maxAttempts;

    @Override
    @Transactional
    public OtpVerification createAndSend(
            String phone,
            OtpVerification.OtpPurpose purpose,
            String email,
            Role requestedRole) {
        OtpVerification verification = OtpVerification.builder()
                .phone(phone)
                .otpCode(OtpGenerator.generate())
                .purpose(purpose)
                .email(email)
                .requestedRole(requestedRole)
                .expiresAt(Instant.now().plusSeconds(otpExpirationSeconds))
                .build();
        OtpVerification saved = otpVerificationRepository.save(verification);
        log.info("Mock OTP sent for phoneEnding={}, purpose={}, otp={}", phoneEnding(phone), purpose, saved.getOtpCode());
        return saved;
    }

    @Override
    @Transactional
    public OtpVerification verify(String phone, String otp) {
        OtpVerification verification = otpVerificationRepository
                .findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new ValidationException("OTP not found or already verified"));

        if (verification.getExpiresAt().isBefore(Instant.now())) {
            throw new ValidationException("OTP has expired");
        }
        authenticationPolicy.ensureOtpCanBeAttempted(verification, maxAttempts);
        if (!verification.getOtpCode().equals(otp)) {
            verification.setAttempts(verification.getAttempts() + 1);
            otpVerificationRepository.save(verification);
            throw new ValidationException("Invalid OTP");
        }

        verification.setVerified(true);
        verification.setVerifiedAt(Instant.now());
        return otpVerificationRepository.save(verification);
    }

    private String phoneEnding(String phone) {
        if (phone == null || phone.length() < 4) {
            return "unknown";
        }
        return phone.substring(phone.length() - 4);
    }
}

package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.RateLimitExceededException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.security.dto.request.ForgotPasswordRequest;
import com.carebridge.backend.security.dto.response.ForgotPasswordResponse;
import com.carebridge.backend.security.entity.PasswordResetToken;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.policy.RateLimitPolicy;
import com.carebridge.backend.security.repository.PasswordResetTokenRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.ForgotPasswordService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.security.util.TokenUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private static final int TOKEN_TTL_SECONDS = 900; // 15 minutes
    private static final String RATE_LIMIT_PREFIX = "forgot_pwd:";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RateLimitPolicy rateLimitPolicy;
    private final EmailService emailService;
    private final SmsService smsService;
    private final AuditService auditService;

    @Value("${carebridge.security.password-reset.rate-limit.per-user:3}")
    private int maxPerUser;

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String compact = phone.replaceAll("[\\s().-]", "");
        if (compact.startsWith("0")) return "+84" + compact.substring(1);
        return compact.startsWith("+") ? compact : "+" + compact;
    }

    @Override
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request, String ipAddress) {
        String contact = request.getContact() != null ? request.getContact().trim() : null;
        if (contact == null || contact.isBlank()) {
            throw new ValidationException("Contact (email or phone) is required");
        }

        // Normalize phone number to E.164 format for consistent lookup
        if (!contact.contains("@")) {
            contact = normalizePhone(contact);
        }

        boolean isEmail = contact.contains("@");
        boolean isPhone = contact.startsWith("+");

        String rateLimitKey = RATE_LIMIT_PREFIX + contact.toLowerCase();
        if (!rateLimitPolicy.canAttempt(rateLimitKey)) {
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }

        // Anti-enumeration: lookup user but always return 200
        User user = isEmail
                ? userRepository.findByEmailIgnoreCase(contact).orElse(null)
                : userRepository.findByPhone(contact).orElse(null);

        if (user != null && "ACTIVE".equals(user.getAccountStatus())) {
            String rawToken = UUID.randomUUID().toString();
            String tokenHash = TokenUtils.hashSha256(rawToken);
            Instant expiresAt = Instant.now().plusSeconds(TOKEN_TTL_SECONDS);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(expiresAt)
                    .build();
            tokenRepository.save(resetToken);

            if (isEmail) {
                emailService.sendPasswordResetEmail(contact, rawToken, TOKEN_TTL_SECONDS / 60);
            } else {
                smsService.sendPasswordResetSms(contact, rawToken, TOKEN_TTL_SECONDS / 60);
            }

            auditService.log(
                    AuditAction.PASSWORD_RESET_REQUESTED,
                    user.getId(),
                    "PasswordResetToken",
                    resetToken.getId() != null ? resetToken.getId().toString() : null,
                    Map.of(
                            "contactMethod", isEmail ? "email" : "sms",
                            "ipAddress", ipAddress != null ? ipAddress : ""));
        }

        return ForgotPasswordResponse.builder()
                .message("If the account exists and is active, a reset instruction has been sent to the provided contact.")
                .expiresIn(TOKEN_TTL_SECONDS)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public User validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ValidationException("Token is required");
        }
        String tokenHash = TokenUtils.hashSha256(token);
        Instant now = Instant.now();

        PasswordResetToken resetToken = tokenRepository
                .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(tokenHash, now)
                .orElseThrow(() -> new ValidationException("Invalid or expired reset token. Please request a new password reset link."));

        if (resetToken.getAttemptCount() >= 5) {
            throw new ValidationException("Too many reset attempts for this token. Please request a new password reset link.");
        }

        return resetToken.getUser();
    }

    @Override
    public void consumeToken(String token) {
        String tokenHash = TokenUtils.hashSha256(token);
        Instant now = Instant.now();

        PasswordResetToken resetToken = tokenRepository
                .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(tokenHash, now)
                .orElseThrow(() -> new ValidationException("Invalid or expired reset token."));

        tokenRepository.markAsUsed(resetToken.getId(), now);
    }


}

package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.security.dto.request.ResetPasswordRequest;
import com.carebridge.backend.security.dto.response.ResetPasswordResponse;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.policy.PasswordComplexityPolicy;
import com.carebridge.backend.security.repository.RefreshTokenRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.ForgotPasswordService;
import com.carebridge.backend.security.service.ResetPasswordService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ResetPasswordServiceImpl implements ResetPasswordService {

    private final ForgotPasswordService forgotPasswordService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordComplexityPolicy passwordComplexityPolicy;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match. Please ensure both password fields are identical.");
        }

        if (!passwordComplexityPolicy.isComplexEnough(request.getNewPassword())) {
            throw new ValidationException(passwordComplexityPolicy.getRequirements());
        }

        User user = forgotPasswordService.validateToken(request.getToken());

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        int revoked = refreshTokenRepository.revokeAllByUserId(user.getId());

        forgotPasswordService.consumeToken(request.getToken());

        auditService.log(
                AuditAction.PASSWORD_RESET_COMPLETED,
                user.getId(),
                "User",
                user.getId().toString(),
                Map.of("sessionsRevoked", String.valueOf(revoked)));

        return ResetPasswordResponse.builder()
                .message("Your password has been reset successfully. Please log in with your new password.")
                .build();
    }
}

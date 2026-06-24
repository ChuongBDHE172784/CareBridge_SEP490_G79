package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AuthenticationException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.common.util.StringUtils;
import com.carebridge.backend.security.dto.request.LoginRequest;
import com.carebridge.backend.security.dto.request.RefreshTokenRequest;
import com.carebridge.backend.security.dto.request.RegisterRequest;
import com.carebridge.backend.security.dto.request.UpdateProfileRequest;
import com.carebridge.backend.security.dto.request.VerifyOtpRequest;
import com.carebridge.backend.security.dto.response.AuthResponse;
import com.carebridge.backend.security.dto.response.OtpSendResponse;
import com.carebridge.backend.security.dto.response.UserProfileResponse;
import com.carebridge.backend.security.entity.OtpVerification;
import com.carebridge.backend.security.entity.RefreshToken;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.mapper.UserMapper;
import com.carebridge.backend.security.policy.AuthenticationPolicy;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.RefreshTokenRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.AuthService;
import com.carebridge.backend.security.service.OtpService;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpService otpService;
    private final AuditService auditService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final AuthenticationPolicy authenticationPolicy;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${carebridge.security.otp.expiration-seconds:300}")
    private long otpExpirationSeconds;

    @Value("${carebridge.security.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Override
    public OtpSendResponse register(RegisterRequest request) {
        String phone = normalizePhone(request.getPhone());
        if (userRepository.existsByPhone(phone)) {
            throw new ValidationException("Phone is already registered");
        }
        Role role = authenticationPolicy.resolveSelfRegistrationRole(request.getRole());
        OtpVerification verification = otpService.createAndSend(
                phone,
                OtpVerification.OtpPurpose.REGISTER,
                request.getEmail(),
                role);
        auditService.log(
                AuditAction.OTP_SENT,
                null,
                "OtpVerification",
                verification.getId().toString(),
                Map.of("purpose", verification.getPurpose().name()));
        return OtpSendResponse.builder()
                .message("OTP sent")
                .expiresIn(otpExpirationSeconds)
                .build();
    }

    @Override
    public OtpSendResponse login(LoginRequest request) {
        String phone = normalizePhone(request.getPhone());
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        authenticationPolicy.ensureCanAuthenticate(user);
        OtpVerification verification = otpService.createAndSend(phone, OtpVerification.OtpPurpose.LOGIN, null, null);
        auditService.log(
                AuditAction.OTP_SENT,
                user.getId(),
                "OtpVerification",
                verification.getId().toString(),
                Map.of("purpose", verification.getPurpose().name()));
        return OtpSendResponse.builder()
                .message("OTP sent")
                .expiresIn(otpExpirationSeconds)
                .build();
    }

    @Override
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String phone = normalizePhone(request.getPhone());
        OtpVerification verification = otpService.verify(phone, request.getOtp());
        User user = userRepository.findByPhone(phone).orElseGet(() -> createUserFromOtp(verification));
        authenticationPolicy.ensureCanAuthenticate(user);
        RefreshToken refreshToken = createRefreshToken(user);
        auditService.log(
                AuditAction.OTP_VERIFIED,
                user.getId(),
                "OtpVerification",
                verification.getId().toString(),
                Map.of("purpose", verification.getPurpose().name()));
        auditService.log(AuditAction.LOGIN, user.getId(), "User", user.getId().toString(), null);
        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(user))
                .refreshToken(refreshToken.getToken())
                .user(userMapper.toProfileResponse(user))
                .build();
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken existing = refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new AuthenticationException("Refresh token is invalid"));
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            existing.setRevoked(true);
            throw new AuthenticationException("Refresh token has expired");
        }
        User user = existing.getUser();
        authenticationPolicy.ensureCanAuthenticate(user);
        existing.setRevoked(true);
        RefreshToken rotated = createRefreshToken(user);
        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(user))
                .refreshToken(rotated.getToken())
                .user(userMapper.toProfileResponse(user))
                .build();
    }

    @Override
    public void logout(String refreshToken, java.util.UUID userId) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                    .ifPresent(token -> {
                        token.setRevoked(true);
                        auditService.log(
                                AuditAction.LOGOUT,
                                token.getUser().getId(),
                                "RefreshToken",
                                token.getId().toString(),
                                null);
                    });
            return;
        }
        if (userId != null) {
            refreshTokenRepository.findByUser_IdAndRevokedFalse(userId)
                    .forEach(token -> token.setRevoked(true));
            auditService.log(AuditAction.LOGOUT, userId, "User", userId.toString(), null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(java.util.UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toProfileResponse(user);
    }

    @Override
    public UserProfileResponse updateProfile(java.util.UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setName(StringUtils.sanitizeBasicText(request.getName()));
        user.setAvatarUrl(StringUtils.trimToNull(request.getAvatarUrl()));
        return userMapper.toProfileResponse(userRepository.save(user));
    }

    private User createUserFromOtp(OtpVerification verification) {
        if (verification.getPurpose() != OtpVerification.OtpPurpose.REGISTER) {
            throw new ResourceNotFoundException("User not found");
        }
        Role role = verification.getRequestedRole() == null ? Role.MOTHER : verification.getRequestedRole();
        User user = User.builder()
                .phone(verification.getPhone())
                .email(verification.getEmail())
                .role(role)
                .enabled(true)
                .locked(false)
                .passwordHash(passwordEncoder.encode(generateOpaqueSecret()))
                .build();
        return userRepository.save(user);
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(generateOpaqueSecret())
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private String generateOpaqueSecret() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizePhone(String phone) {
        return StringUtils.trimToNull(phone);
    }
}

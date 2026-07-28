package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AccountLockedException;
import com.carebridge.backend.common.exception.AuthenticationException;
import com.carebridge.backend.common.exception.AuthorizationException;
import com.carebridge.backend.common.exception.InvalidRefreshTokenException;
import com.carebridge.backend.common.exception.RateLimitExceededException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.RevokedSessionException;
import com.carebridge.backend.common.exception.SessionNotFoundException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.common.validation.VietnamesePhoneNumbers;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.common.util.StringUtils;
import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.TokenBlacklistRepository;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.security.dto.request.ChangePasswordRequest;
import com.carebridge.backend.security.dto.request.LoginRequest;
import com.carebridge.backend.security.dto.request.RefreshTokenRequest;
import com.carebridge.backend.security.dto.request.RegisterRequest;
import com.carebridge.backend.security.dto.request.ResendOtpRequest;
import com.carebridge.backend.security.dto.request.SelectRoleRequest;
import com.carebridge.backend.security.dto.request.UpdateProfileRequest;
import com.carebridge.backend.security.dto.request.VerifyOtpRequest;
import com.carebridge.backend.security.dto.response.AuthResponse;
import com.carebridge.backend.security.dto.response.OtpResendResponse;
import com.carebridge.backend.security.dto.response.OtpSendResponse;
import com.carebridge.backend.security.dto.response.UserProfileResponse;
import com.carebridge.backend.security.entity.OtpVerification;
import com.carebridge.backend.security.entity.RefreshToken;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.mapper.UserMapper;
import com.carebridge.backend.security.policy.AuthenticationPolicy;
import com.carebridge.backend.security.policy.PasswordComplexityPolicy;
import com.carebridge.backend.security.policy.RateLimitPolicy;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.OtpVerificationRepository;
import com.carebridge.backend.security.repository.RefreshTokenRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.AuthService;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import com.carebridge.backend.security.util.TokenUtils;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final OtpVerificationRepository otpVerificationRepository;
    private final AuditService auditService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final AuthenticationPolicy authenticationPolicy;
    private final PasswordComplexityPolicy passwordComplexityPolicy;
    private final RateLimitPolicy rateLimitPolicy;
    private final UserSessionRepository sessionRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;
    private final DeviceTokenRepository deviceTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private transient HttpServletRequest request;

    @Value("${carebridge.security.otp.expiration-seconds:300}")
    private long otpExpirationSeconds;

    @Value("${carebridge.security.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    /**
     * Constant-time comparison for hex-encoded hashes to prevent timing attacks.
     */
    private static boolean constantTimeHashEquals(String hash1, String hash2) {
        if (hash1 == null || hash2 == null) {
            return hash1 == hash2;
        }
        try {
            byte[] bytes1 = hash1.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] bytes2 = hash2.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return MessageDigest.isEqual(bytes1, bytes2);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public OtpSendResponse register(RegisterRequest request) {
        // 1. Validate password complexity
        if (!passwordComplexityPolicy.isComplexEnough(request.getPassword())) {
            throw new ValidationException(passwordComplexityPolicy.getRequirements());
        }

        // 2. Determine identifier (email or phone)
        String email = request.getEmail();
        String phone = normalizePhone(request.getPhone());
        String identifier;

        if (email != null && !email.isBlank()) {
            identifier = email.trim().toLowerCase();
            if (!identifier.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                throw new ValidationException("Invalid email format");
            }
        } else if (phone != null && !phone.isBlank()) {
            identifier = phone;
        } else {
            throw new ValidationException("Either email or phone must be provided");
        }

        // 3. Check for duplicate account
        boolean emailExists = email != null && !email.isBlank() && userRepository.existsByEmail(email);
        boolean phoneExists = phone != null && !phone.isBlank() && userRepository.existsByPhone(phone);

        if (emailExists || phoneExists) {
            throw new ValidationException("Account already exists");
        }

        // 4. Resolve optional role through authentication policy.
        // Consumer self-registration intentionally starts without a role when omitted.
        Role role = authenticationPolicy.resolveSelfRegistrationRole(request.getRole());

        // 5. Hash password with BCrypt
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // 6. Create user with enabled=false
        User user = User.builder()
                .name(StringUtils.sanitizeBasicText(request.getName()))
                .email(email != null && !email.isBlank() ? email : null)
                .phone(phone != null && !phone.isBlank() ? phone : null)
                .role(role)
                .passwordHash(passwordHash)
                .enabled(false)
                .locked(false)
                .emailVerified(false)
                .phoneVerified(false)
                .accountStatus("PENDING_ACTIVATION")
                .build();

        user = userRepository.save(user);

        // 7. Generate 6-digit OTP and hash with SHA256
        String otp = generate6DigitOtp();
        String otpHash = TokenUtils.hashSha256(otp);

        // 8. Create OtpVerification
        OtpVerification otpVerification = OtpVerification.builder()
                .user(user)
                .codeHash(otpHash)
                .phone(phone != null && !phone.isBlank() ? phone : null)
                .email(identifier != null && identifier.contains("@") ? identifier : null)
                .purpose(OtpVerification.OtpPurpose.REGISTER)
                .expiresAt(Instant.now().plusSeconds(otpExpirationSeconds))
                .attempts(5)
                .verified(false)
                .build();

        otpVerificationRepository.save(otpVerification);

        // 9. Send OTP
        if (email != null && !email.isBlank()) {
            emailService.sendOtpVerificationEmail(email, otp, (int) (otpExpirationSeconds / 60));
        }
        if (phone != null && !phone.isBlank()) {
            smsService.sendOtpVerificationSms(phone, otp, (int) (otpExpirationSeconds / 60));
        }

        // 10. Audit log
        auditService.log(
                AuditAction.OTP_SENT,
                user.getId(),
                "OtpVerification",
                otpVerification.getId() != null ? otpVerification.getId().toString() : null,
                Map.ofEntries(
                    Map.entry("purpose", "REGISTER"),
                    Map.entry("email", email != null ? email : ""),
                    Map.entry("phone", phone != null ? phone : ""),
                    Map.entry("role", role != null ? role.name() : "UNASSIGNED")));

        return OtpSendResponse.builder()
                .message("Registration initiated. Please verify your OTP.")
                .expiresIn(otpExpirationSeconds)
                .userId(user.getId())
                .otpExpiresAt(otpVerification.getExpiresAt())
                .build();
    }

    private String generate6DigitOtp() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }

    private String extractDeviceName(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown Device";
        }
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "Mobile Device";
        }
        if (userAgent.contains("windows")) {
            return "Windows PC";
        }
        if (userAgent.contains("mac os")) {
            return "Mac";
        }
        if (userAgent.contains("linux")) {
            return "Linux";
        }
        return "Browser/App";
    }

    @Override
    @Transactional(noRollbackFor = {AccountLockedException.class, RateLimitExceededException.class})
    public AuthResponse login(LoginRequest request) {
        // 1. Normalize identifier (phone or email)
        String phone = normalizePhone(request.getPhone());
        String emailRaw = request.getEmail();
        String email = (emailRaw == null || emailRaw.isBlank()) ? null : emailRaw.trim().toLowerCase();

        // Validate exactly one identifier
        boolean hasPhone = phone != null;
        boolean hasEmail = email != null;
        if (hasPhone == hasEmail) {
            throw new ValidationException("Either phone or email must be provided (exactly one)");
        }

        // 2. Fetch user
        User user = hasPhone
                ? userRepository.findByPhone(phone).orElse(null)
                : userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user == null) {
            throw new AuthenticationException("Invalid credentials");
        }

        // Check account status via policy (covers enabled/disabled/locked)
        authenticationPolicy.ensureCanAuthenticate(user);

        // Rate limit check
        String rateLimitKey = getRateLimitKey(user);
        if (!rateLimitPolicy.canAttempt(rateLimitKey)) {
            user.setLocked(true);
            user.setLockedAt(Instant.now());
            userRepository.save(user);
            throw new RateLimitExceededException("Account temporarily locked due to multiple failed attempts");
        }

        // Verify password
        String passwordHash = user.getPasswordHash();
        if (passwordHash == null) {
            throw new AuthenticationException("Invalid credentials");
        }

        if (!passwordEncoder.matches(request.getPassword(), passwordHash)) {
            throw new AuthenticationException("Invalid credentials");
        }

        // Password matches: reset rate limits and locks.
        resetRateLimit(rateLimitKey);
        user.setLocked(false);
        user.setLockedAt(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        RefreshToken refreshToken = createRefreshToken(user);
        String rawRefreshToken = refreshToken.getToken();
        String refreshTokenHash = TokenUtils.hashSha256(rawRefreshToken);

        UUID sessionId = UUID.randomUUID();
        String ipAddress = this.request != null ? this.request.getRemoteAddr() : null;
        String userAgent = this.request != null ? this.request.getHeader("User-Agent") : null;

        UserSession session = UserSession.builder()
                .userId(user.getId())
                .sessionId(sessionId)
                .refreshTokenHash(refreshTokenHash)
                .deviceName(extractDeviceName(userAgent))
                .browser(userAgent != null ? userAgent : "Unknown")
                .ipAddress(ipAddress)
                .location(null)
                .lastActivityAt(Instant.now())
                .expiresAt(refreshToken.getExpiresAt())
                .status("active")
                .isCurrent(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        sessionRepository.save(session);
        sessionRepository.clearCurrentSessions(user.getId(), sessionId);

        String accessToken = jwtTokenProvider.generateAccessToken(user, sessionId);
        Map<String, Object> auditDetails = new HashMap<>();
        if (ipAddress != null) {
            auditDetails.put("ipAddress", ipAddress);
        }
        if (userAgent != null) {
            auditDetails.put("userAgent", userAgent);
        }
        auditService.log(
                AuditAction.LOGIN,
                user.getId(),
                "User",
                user.getId().toString(),
                auditDetails.isEmpty() ? null : auditDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .user(userMapper.toProfileResponse(user))
                .build();
    }

    @Transactional(noRollbackFor = ValidationException.class)
    @Override
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String phone = normalizePhone(request.getPhone());
        String emailRaw = request.getEmail();
        String email = (emailRaw == null || emailRaw.isBlank()) ? null : emailRaw.trim().toLowerCase();
        String otpInput = request.getOtp();

        // Find valid OtpVerification by phone or email
        OtpVerification verification;
        if (phone != null && !phone.isBlank()) {
            verification = otpVerificationRepository
                    .findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc(phone)
                    .filter(v -> v.getExpiresAt().isAfter(Instant.now()))
                    .orElseThrow(() -> new ValidationException("Invalid or expired OTP"));
        } else if (email != null && !email.isBlank()) {
            verification = otpVerificationRepository
                    .findTopByEmailAndUsedAtIsNullOrderByCreatedAtDesc(email)
                    .filter(v -> v.getExpiresAt().isAfter(Instant.now()))
                    .orElseThrow(() -> new ValidationException("Invalid or expired OTP"));
        } else {
            throw new ValidationException("Either phone or email must be provided");
        }

        // Verify purpose-specific logic
        if (verification.getPurpose() == OtpVerification.OtpPurpose.REGISTER) {
            User user = verification.getUser();
            if (user == null) {
                throw new ValidationException("Invalid OTP");
            }
            return completeRegistration(verification, user, otpInput);
        } else if (verification.getPurpose() == OtpVerification.OtpPurpose.LOGIN) {
            return completeLogin(verification, phone, otpInput);
        }

        throw new ValidationException("Unsupported OTP purpose");
    }

    @Override
    @Transactional
    public OtpResendResponse resendOtp(ResendOtpRequest request) {
        String phone = normalizePhone(request.getPhone());
        String email = StringUtils.trimToNull(request.getEmail());

        boolean hasPhone = phone != null;
        boolean hasEmail = email != null;
        if (hasPhone == hasEmail) {
            throw new ValidationException("Exactly one of phone or email must be provided");
        }

        User user = hasPhone
                ? userRepository.findByPhone(phone)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                : userRepository.findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        OtpVerification existingVerification = otpVerificationRepository
                .findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(user.getId())
                .orElseThrow(() -> new ValidationException(
                        "No pending OTP verification found. Please request a new OTP."));

        String deliveryIdentifier = hasPhone
                ? StringUtils.trimToNull(user.getPhone())
                : StringUtils.trimToNull(user.getEmail());
        if (deliveryIdentifier == null) {
            throw new ValidationException("The account does not have the requested delivery identifier");
        }

        String resendAccountKey = user.getId().toString();

        String otp = generate6DigitOtp();
        String otpHash = TokenUtils.hashSha256(otp);
        Instant now = Instant.now();

        existingVerification.setUsedAt(now);
        otpVerificationRepository.save(existingVerification);

        OtpVerification newVerification = OtpVerification.builder()
                .user(user)
                .codeHash(otpHash)
                .phone(hasPhone ? deliveryIdentifier : null)
                .email(hasEmail ? deliveryIdentifier : null)
                .purpose(existingVerification.getPurpose())
                .expiresAt(now.plusSeconds(otpExpirationSeconds))
                .attempts(5)
                .verified(false)
                .build();

        OtpVerification savedVerification = otpVerificationRepository.save(newVerification);

        if (!rateLimitPolicy.tryConsumeResend(resendAccountKey)) {
            long cooldownRemaining = rateLimitPolicy.getTimeUntilResendReset(resendAccountKey);
            throw new RateLimitExceededException(
                    "Please wait before resending OTP. Cooldown: " + cooldownRemaining + " seconds");
        }

        try {
            if (hasEmail) {
                emailService.sendOtpVerificationEmail(deliveryIdentifier, otp, (int) (otpExpirationSeconds / 60));
            } else {
                smsService.sendOtpVerificationSms(deliveryIdentifier, otp, (int) (otpExpirationSeconds / 60));
            }

            auditService.log(
                    AuditAction.OTP_RESENT,
                    user.getId(),
                    "User",
                    user.getId().toString(),
                    Map.of(
                            "purpose", existingVerification.getPurpose().name(),
                            "channel", hasPhone ? "SMS" : "EMAIL",
                            "otpVerificationId", String.valueOf(savedVerification.getId())));
        } catch (RuntimeException ex) {
            rateLimitPolicy.resetResend(resendAccountKey);
            throw ex;
        }

        long cooldownRemaining = rateLimitPolicy.getTimeUntilResendReset(resendAccountKey);

        return OtpResendResponse.builder()
                .otpExpiresAt(now.plusSeconds(otpExpirationSeconds))
                .resendCooldownRemaining(cooldownRemaining)
                .message("OTP resent successfully")
                .build();
    }

    private AuthResponse completeRegistration(OtpVerification verification, User user, String otpInput) {
        String inputHash = TokenUtils.hashSha256(otpInput);
        if (!constantTimeHashEquals(inputHash, verification.getCodeHash())) {
            verification.setAttempts(verification.getAttempts() - 1);
            if (verification.getAttempts() <= 0) {
                verification.setUsedAt(Instant.now());
            }
            otpVerificationRepository.save(verification);
            throw new ValidationException("Invalid OTP");
        }

        verification.setUsedAt(Instant.now());
        verification.setVerified(true);
        otpVerificationRepository.save(verification);

        user.setEnabled(true);
        user.setAccountStatus("ACTIVE");
        userRepository.save(user);

        auditService.log(
                AuditAction.OTP_VERIFIED,
                user.getId(),
                "OtpVerification",
                verification.getId().toString(),
                Map.of("purpose", verification.getPurpose().name()));
        auditService.log(AuditAction.USER_REGISTRATION_COMPLETED, user.getId(), "User", user.getId().toString(), null);

        RefreshToken refreshToken = createRefreshToken(user);
        String rawRefreshToken = refreshToken.getToken();
        String refreshTokenHash = TokenUtils.hashSha256(rawRefreshToken);

        UUID sessionId = UUID.randomUUID();
        String ipAddress = this.request != null ? this.request.getRemoteAddr() : null;
        String userAgent = this.request != null ? this.request.getHeader("User-Agent") : null;
        String deviceName = extractDeviceName(userAgent);
        String browser = userAgent != null ? userAgent : "Unknown";

        UserSession session = UserSession.builder()
                .userId(user.getId())
                .sessionId(sessionId)
                .refreshTokenHash(refreshTokenHash)
                .deviceName(deviceName)
                .browser(browser)
                .ipAddress(ipAddress)
                .location(null)
                .lastActivityAt(Instant.now())
                .expiresAt(refreshToken.getExpiresAt())
                .status("active")
                .isCurrent(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        sessionRepository.save(session);
        sessionRepository.clearCurrentSessions(user.getId(), sessionId);

        String accessToken = jwtTokenProvider.generateAccessToken(user, sessionId);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .user(userMapper.toProfileResponse(user))
                .build();
    }

    private AuthResponse completeLogin(OtpVerification verification, String phone, String otpInput) {
        String inputHash = TokenUtils.hashSha256(otpInput);
        if (!constantTimeHashEquals(inputHash, verification.getCodeHash())) {
            verification.setAttempts(verification.getAttempts() - 1);
            if (verification.getAttempts() <= 0) {
                verification.setUsedAt(Instant.now());
            }
            otpVerificationRepository.save(verification);
            throw new ValidationException("Invalid OTP");
        }

        verification.setUsedAt(Instant.now());
        verification.setVerified(true);
        otpVerificationRepository.save(verification);

        User user = verification.getUser();
        authenticationPolicy.ensureCanAuthenticate(user);

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        auditService.log(
                AuditAction.OTP_VERIFIED,
                user.getId(),
                "OtpVerification",
                verification.getId().toString(),
                Map.of("purpose", verification.getPurpose().name()));
        auditService.log(AuditAction.LOGIN, user.getId(), "User", user.getId().toString(), null);

        RefreshToken refreshToken = createRefreshToken(user);
        String rawRefreshToken = refreshToken.getToken();
        String refreshTokenHash = TokenUtils.hashSha256(rawRefreshToken);

        UUID sessionId = UUID.randomUUID();
        String ipAddress = this.request != null ? this.request.getRemoteAddr() : null;
        String userAgent = this.request != null ? this.request.getHeader("User-Agent") : null;
        String deviceName = extractDeviceName(userAgent);
        String browser = userAgent != null ? userAgent : "Unknown";

        UserSession session = UserSession.builder()
                .userId(user.getId())
                .sessionId(sessionId)
                .refreshTokenHash(refreshTokenHash)
                .deviceName(deviceName)
                .browser(browser)
                .ipAddress(ipAddress)
                .location(null)
                .lastActivityAt(Instant.now())
                .expiresAt(refreshToken.getExpiresAt())
                .status("active")
                .isCurrent(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        sessionRepository.save(session);
        sessionRepository.clearCurrentSessions(user.getId(), sessionId);

        String accessToken = jwtTokenProvider.generateAccessToken(user, sessionId);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .user(userMapper.toProfileResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String rawToken = request.getRefreshToken();
        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthenticationException("Refresh token is required");
        }

        Instant now = Instant.now();
        String tokenHash = TokenUtils.hashSha256(rawToken);

        if (tokenBlacklistRepository.existsByTokenHashAndExpiresAtAfter(tokenHash, now)) {
            throw new AuthenticationException("Token has been blacklisted");
        }

        UserSession session = sessionRepository.findByRefreshTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException(
                        "No active session found for this token"));

        if (session.isRevoked()) {
            throw new RevokedSessionException("Session has been revoked");
        }
        if (session.getStatus() == null || !"ACTIVE".equalsIgnoreCase(session.getStatus())) {
            throw new InvalidRefreshTokenException("Session is not active");
        }
        if (session.getExpiresAt() != null && !session.getExpiresAt().isAfter(now)) {
            throw new InvalidRefreshTokenException("Session has expired");
        }

        RefreshToken existing = refreshTokenRepository.findByTokenAndRevokedFalseForUpdate(rawToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is invalid"));

        if (!tokenHash.equals(session.getRefreshTokenHash())) {
            throw new InvalidRefreshTokenException("Token does not match session");
        }

        if (existing.getExpiresAt().isBefore(now) || existing.getExpiresAt().equals(now)) {
            existing.setRevoked(true);
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        User user = existing.getUser();
        authenticationPolicy.ensureCanAuthenticate(user);

        existing.setRevoked(true);
        RefreshToken rotated = createRefreshToken(user);
        String newTokenHash = TokenUtils.hashSha256(rotated.getToken());

        int sessionUpdated = sessionRepository.updateSessionForRotation(
                session.getSessionId(),
                newTokenHash,
                rotated.getExpiresAt(),
                now);
        if (sessionUpdated == 0) {
            throw new IllegalStateException("Failed to update session during token rotation");
        }

        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(user, session.getSessionId()))
                .refreshToken(rotated.getToken())
                .user(userMapper.toProfileResponse(user))
                .build();
    }

    @Override
    public void logout(String refreshToken, UUID userId) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            String tokenHash = TokenUtils.hashSha256(refreshToken);

            tokenBlacklistRepository.save(com.carebridge.backend.identity.entity.TokenBlacklist.builder()
                    .tokenHash(tokenHash)
                    .expiresAt(Instant.now().plus(7, java.time.temporal.ChronoUnit.DAYS))
                    .build());

            sessionRepository.findByRefreshTokenHashAndRevokedFalse(tokenHash)
                    .ifPresent(session -> {
                        session.setRevoked(true);
                        session.setStatus("REVOKED");
                        session.setUpdatedAt(Instant.now());
                        sessionRepository.save(session);
                    });

            refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                    .ifPresent(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
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
                    .forEach(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                    });
            sessionRepository.findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(userId)
                    .forEach(session -> {
                        session.setRevoked(true);
                        session.setStatus("REVOKED");
                        session.setUpdatedAt(Instant.now());
                        sessionRepository.save(session);
                    });
            auditService.log(AuditAction.LOGOUT, userId, "User", userId.toString(), null);
        }
    }

    @Override
    @Transactional
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.isLocked()) {
            throw new com.carebridge.backend.common.exception.AccountLockedException("Account is locked");
        }
        UserProfileResponse response = userMapper.toProfileResponse(user);
        // ADR-008-002: audit every successful profile view. Must NOT be readOnly — a
        // read-only transaction sets Hibernate's flush mode to MANUAL, so this audit
        // insert is enqueued but never flushed/committed (silently dropped, no error).
        auditService.log(AuditAction.PROFILE_VIEWED, userId, "User", userId.toString(), null);
        return response;
    }

    @Override
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setName(StringUtils.sanitizeBasicText(request.getName()));
        user.setAvatarUrl(StringUtils.trimToNull(request.getAvatarUrl()));
        User saved = userRepository.save(user);
        // ADR-002: audit profile updates in the same transaction (only after a successful save).
        auditService.log(AuditAction.PROFILE_UPDATED, userId, "User", userId.toString(), null);
        return userMapper.toProfileResponse(saved);
    }

    @Override
    public UserProfileResponse selectRole(UUID userId, SelectRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != null) {
            throw new ValidationException("Role has already been assigned");
        }

        Role selectedRole = authenticationPolicy.resolveSelfRegistrationRole(request.getRole());
        if (selectedRole == null) {
            throw new ValidationException("Role is required");
        }

        user.setRole(selectedRole);
        User saved = userRepository.save(user);
        auditService.log(AuditAction.PROFILE_UPDATED, userId, "User", userId.toString(),
                Map.of("role", selectedRole.name()));
        return userMapper.toProfileResponse(saved);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException(
                    "AUTH-072",
                    "Password confirmation does not match the new password");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new ValidationException("AUTH-071", "Current password is incorrect");
        }

        if (!passwordComplexityPolicy.isComplexEnough(request.getNewPassword())) {
            throw new ValidationException("AUTH-073", passwordComplexityPolicy.getRequirements());
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ValidationException(
                    "AUTH-074",
                    "New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.findByUser_IdAndRevokedFalse(userId).forEach(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });

        auditService.log(AuditAction.PASSWORD_CHANGED, userId, "User", userId.toString(), null);
    }

    @Override
    @Transactional
    public void deactivate(UUID userId, String confirmPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // AUTH-083: Block SYSTEM_ADMIN from self-deactivation
        if (user.getRole() == Role.SYSTEM_ADMIN) {
            throw new AuthorizationException("SYSTEM_ADMIN accounts cannot be self-deactivated");
        }

        // AUTH-082: Already deactivated
        if ("DEACTIVATED".equals(user.getAccountStatus())) {
            throw new ValidationException("AUTH-082: Account is already deactivated");
        }

        // AUTH-081: Wrong password
        if (!passwordEncoder.matches(confirmPassword, user.getPasswordHash())) {
            throw new AuthenticationException("AUTH-081: Incorrect password");
        }

        // Deactivate account
        user.setAccountStatus("DEACTIVATED");
        user.setEnabled(false);
        userRepository.save(user);

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllByUserId(userId);

        // Deactivate device tokens
        deviceTokenRepository.deactivateAllForUser(userId, Instant.now());

        // Audit
        auditService.log(AuditAction.SECURITY_EVENT, userId, "User", userId.toString(),
                Map.of("action", "ACCOUNT_DEACTIVATED"));
    }

    private RefreshToken createRefreshToken(User user) {
        String rawToken = generateOpaqueSecret();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(rawToken)
                .tokenHash(TokenUtils.hashSha256(rawToken))
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
        try {
            return VietnamesePhoneNumbers.normalizeToE164(phone);
        } catch (IllegalArgumentException invalidPhone) {
            throw new ValidationException(VietnamesePhoneNumbers.INVALID_FORMAT_MESSAGE);
        }
    }

    private String getRateLimitKey(User user) {
        return user.getId().toString();
    }

    private void resetRateLimit(String identifier) {
        rateLimitPolicy.reset(identifier);
    }
}

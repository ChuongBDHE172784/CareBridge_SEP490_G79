package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.AuthenticationException;
import com.carebridge.backend.common.exception.RateLimitExceededException;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.common.util.StringUtils;
import com.carebridge.backend.security.dto.request.LoginRequest;
import com.carebridge.backend.security.dto.request.RefreshTokenRequest;
import com.carebridge.backend.security.dto.request.RegisterRequest;
import com.carebridge.backend.security.dto.request.ResendOtpRequest;
import com.carebridge.backend.security.dto.request.UpdateProfileRequest;
import com.carebridge.backend.security.dto.request.VerifyOtpRequest;
import com.carebridge.backend.security.dto.response.AuthResponse;
import com.carebridge.backend.security.dto.response.RegisterResponse;
import com.carebridge.backend.security.dto.response.OtpResendResponse;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
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
    private final OtpVerificationRepository otpVerificationRepository;
    private final AuditService auditService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final AuthenticationPolicy authenticationPolicy;
    private final PasswordComplexityPolicy passwordComplexityPolicy;
    private final RateLimitPolicy rateLimitPolicy;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;

    @Value("${carebridge.security.otp.expiration-seconds:300}")
    private long otpExpirationSeconds;

    @Value("${carebridge.security.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Override
    public RegisterResponse register(RegisterRequest request) {
        // 1. Validate password complexity
        if (!passwordComplexityPolicy.isComplexEnough(request.getPassword())) {
            throw new ValidationException(passwordComplexityPolicy.getRequirements());
        }

        // 2. Determine identifier (email or phone)
        String email = request.getEmail();
        String phone = request.getPhone();
        String identifier;

        if (email != null && !email.isBlank()) {
            identifier = email.trim().toLowerCase();
            // Validate email format using RFC 5322 simplified regex
            if (!identifier.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                throw new ValidationException("Invalid email format");
            }
        } else if (phone != null && !phone.isBlank()) {
            identifier = phone;
            // Validate Vietnamese phone format (E.164)
            if (!identifier.matches("^\\+84[1-9][0-9]{8,9}$")) {
                throw new ValidationException("Invalid phone format. Use E.164 format (+84xxxxxxxxx)");
            }
        } else {
            throw new ValidationException("Either email or phone must be provided");
        }

        // 3. Check for duplicate account
        boolean emailExists = email != null && !email.isBlank() && userRepository.existsByEmail(email);
        boolean phoneExists = phone != null && !phone.isBlank() && userRepository.existsByPhone(phone);

        if (emailExists || phoneExists) {
            // Generic message - don't leak which identifier exists
            throw new ValidationException("Account already exists");
        }

        // 4. Resolve role through authentication policy
        Role role = authenticationPolicy.resolveSelfRegistrationRole(request.getRole());

        // 5. Hash password with BCrypt (strength 12 via encoder config)
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // 6. Create user with enabled=false
        User user = User.builder()
                .email(email != null && !email.isBlank() ? email : null)
                .phone(phone != null && !phone.isBlank() ? phone : null)
                .role(role)
                .passwordHash(passwordHash)
                .enabled(false)
                .locked(false)
                .accountStatus("PENDING_ACTIVATION")
                .build();

        user = userRepository.save(user);

        // 7. Generate 6-digit OTP and hash with SHA256
        String otp = generate6DigitOtp();
        String otpHash = hashOtpWithSha256(otp);

        // 8. Create OtpVerification with 5-min expiry, 5 attempts
        // Story 1.2 fix: persist .email(...) symmetric to .phone(...) so that
        // email-channel verify-otp (which looks up by email) can find the row.
        // Without this, email-registered users could never complete registration.
        OtpVerification otpVerification = OtpVerification.builder()
                .user(user)
                .codeHash(otpHash)
                .phone(phone != null && !phone.isBlank() ? phone : null)
                .email(identifier != null && identifier.contains("@") ? identifier : null)
                .purpose(OtpVerification.OtpPurpose.REGISTER)
                .expiresAt(Instant.now().plusSeconds(otpExpirationSeconds))
                .attempts(5) // remaining attempts
                .verified(false)
                .build();

        otpVerificationRepository.save(otpVerification);

        // 9. Send OTP via email or SMS
        if (email != null && !email.isBlank()) {
            emailService.sendOtpVerificationEmail(email, otp, (int) (otpExpirationSeconds / 60));
        }
        if (phone != null && !phone.isBlank()) {
            smsService.sendOtpVerificationSms(phone, otp, (int) (otpExpirationSeconds / 60));
        }

        // 10. Audit log - using OTP_SENT as registration initiation
        auditService.log(
                AuditAction.OTP_SENT,
                user.getId(),
                "OtpVerification",
                otpVerification.getId().toString(),
                Map.ofEntries(
                    Map.entry("purpose", "REGISTER"),
                    Map.entry("email", email != null ? email : ""),
                    Map.entry("phone", phone != null ? phone : ""),
                    Map.entry("role", role.name())));

        // 11. Return response
        return RegisterResponse.builder()
                .userId(user.getId())
                .message("Registration initiated. Please verify your OTP.")
                .otpExpiresAt(Instant.now().plusSeconds(otpExpirationSeconds))
                .build();
    }

    private String generate6DigitOtp() {
        // Secure random 6-digit OTP
        java.security.SecureRandom random = new java.security.SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 100000-999999
        return String.valueOf(otp);
    }

    private String hashOtpWithSha256(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // 1. Normalize identifier (phone or email)
        String phone = normalizePhone(request.getPhone());
        String emailRaw = request.getEmail();
        String email = (emailRaw == null || emailRaw.isBlank()) ? null : emailRaw.trim().toLowerCase();

        // Validate exactly one identifier
        boolean hasPhone = phone != null;
        boolean hasEmail = email != null;
        if (hasPhone == hasEmail) {
            throw new AuthenticationException("Either phone or email must be provided (exactly one)");
        }

        // 2. Rate limiting: check if account is locked due to too many failed attempts
        // Use user ID as rate limit key (will be known after fetching user)
        User user = hasPhone
                ? userRepository.findByPhone(phone).orElse(null)
                : userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user == null) {
            // Generic message - don't leak user existence
            throw new AuthenticationException("Invalid credentials");
        }

        // Check account status via policy (covers enabled/disabled/locked)
        authenticationPolicy.ensureCanAuthenticate(user);

        // Rate limit key based on user ID (not identifier) to prevent account-wide lockout
        String rateLimitKey = getRateLimitKey(user);

        // Check rate limit - if exhausted, lock the account
        if (!rateLimitPolicy.canAttempt(rateLimitKey)) {
            // Lock the account for 15 minutes
            user.setLocked(true);
            user.setLockedAt(Instant.now());
            userRepository.save(user);

            long cooldown = rateLimitPolicy.getTimeUntilReset(rateLimitKey);
            throw new AuthenticationException("Account temporarily locked due to multiple failed attempts. Please try again in " + cooldown + " seconds.");
        }

        // 3. Verify password
        String passwordHash = user.getPasswordHash();
        if (passwordHash == null) {
            throw new AuthenticationException("Invalid credentials");
        }

        if (!passwordEncoder.matches(request.getPassword(), passwordHash)) {
            // Password mismatch - rate limit counter already incremented by canAttempt()
            throw new AuthenticationException("Invalid credentials");
        }

        // 4. Successful login - reset rate limit, unlock account, update last login
        resetRateLimit(rateLimitKey);
        user.setLocked(false);
        user.setLockedAt(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // 5. Generate tokens and return response (reuse completeLogin logic)
        RefreshToken refreshToken = createRefreshToken(user);

        auditService.log(
                AuditAction.LOGIN,
                user.getId(),
                "User",
                user.getId().toString(),
                null);

        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(user))
                .refreshToken(refreshToken.getToken())
                .user(userMapper.toProfileResponse(user))
                .build();
    }

    @Transactional(noRollbackFor = ValidationException.class)
    @Override
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String phone = normalizePhone(request.getPhone());
        // Story 1.2 fix: normalize the email to match the canonical form used in
        // register() and resendOtp(). Without this, "Test@Example.com " does not
        // match the persisted lowercase trimmed value, and verify fails with a
        // generic "Invalid or expired OTP" error.
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
            // For registration: user must exist and be pending activation
            User user = verification.getUser();
            if (user == null) {
                throw new ValidationException("Invalid OTP");
            }
            return completeRegistration(verification, user, otpInput);
        } else if (verification.getPurpose() == OtpVerification.OtpPurpose.LOGIN) {
            // For login: verify OTP and authenticate
            return completeLogin(verification, phone, otpInput);
        }

        throw new ValidationException("Unsupported OTP purpose");
    }

    @Override
    @Transactional
    public OtpResendResponse resendOtp(ResendOtpRequest request) {
        String phone = StringUtils.trimToNull(request.getPhone());
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

        // Reordered (Iteration 3): DB writes before cooldown consumption so that
        // a Transaction rollback (DB-level failure) releases the cooldown slot
        // implicitly by skipping the consume call. If a step AFTER successful
        // consume (audit/delivery) throws, the explicit finally-block releases
        // the in-memory slot. See spec-1-1-fix-backend-tests.md change-log
        // entry for EC-E6.
        String otp = generate6DigitOtp();
        String otpHash = hashOtpWithSha256(otp);
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

        // Consume cooldown AFTER DB writes succeeded (or will succeed at commit).
        // If this throws 429, the @Transactional rollback discards the DB writes.
        if (!rateLimitPolicy.tryConsumeResend(resendAccountKey)) {
            long cooldownRemaining = rateLimitPolicy.getTimeUntilResendReset(resendAccountKey);
            throw new RateLimitExceededException(
                    "Please wait before resending OTP. Cooldown: " + cooldownRemaining + " seconds");
        }

        boolean cooldownConsumed = true;
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
            // Delivery or audit failed after cooldown was consumed. Release the
            // slot so the user can retry without waiting a full window. The
            // @Transactional rollback discards the DB writes.
            rateLimitPolicy.resetResend(resendAccountKey);
            cooldownConsumed = false;
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
        // Hash input OTP and compare
        String inputHash = hashOtpWithSha256(otpInput);
        if (!inputHash.equals(verification.getCodeHash())) {
            // Decrease attempts
            verification.setAttempts(verification.getAttempts() - 1);
            if (verification.getAttempts() <= 0) {
                verification.setUsedAt(Instant.now());
            }
            otpVerificationRepository.save(verification);
            throw new ValidationException("Invalid OTP");
        }

        // Mark OTP as used
        verification.setUsedAt(Instant.now());
        verification.setVerified(true);
        otpVerificationRepository.save(verification);

        // Enable user
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

        // Create refresh token
        RefreshToken refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(user))
                .refreshToken(refreshToken.getToken())
                .user(userMapper.toProfileResponse(user))
                .build();
    }

    private AuthResponse completeLogin(OtpVerification verification, String phone, String otpInput) {
        // Hash input OTP and compare
        String inputHash = hashOtpWithSha256(otpInput);
        if (!inputHash.equals(verification.getCodeHash())) {
            // Decrease attempts
            verification.setAttempts(verification.getAttempts() - 1);
            if (verification.getAttempts() <= 0) {
                verification.setUsedAt(Instant.now());
            }
            otpVerificationRepository.save(verification);
            throw new ValidationException("Invalid OTP");
        }

        // Mark OTP as used
        verification.setUsedAt(Instant.now());
        verification.setVerified(true);
        otpVerificationRepository.save(verification);

        User user = verification.getUser();
        authenticationPolicy.ensureCanAuthenticate(user);

        auditService.log(
                AuditAction.OTP_VERIFIED,
                user.getId(),
                "OtpVerification",
                verification.getId().toString(),
                Map.of("purpose", verification.getPurpose().name()));
        auditService.log(AuditAction.LOGIN, user.getId(), "User", user.getId().toString(), null);

        // Create refresh token
        RefreshToken refreshToken = createRefreshToken(user);

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
    public void logout(String refreshToken, UUID userId) {
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
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toProfileResponse(user);
    }

    @Override
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setName(StringUtils.sanitizeBasicText(request.getName()));
        user.setAvatarUrl(StringUtils.trimToNull(request.getAvatarUrl()));
        return userMapper.toProfileResponse(userRepository.save(user));
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
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizePhone(String phone) {
        return StringUtils.trimToNull(phone);
    }

    // Helper để lấy identifier cho rate limiting (dùng user ID để thống nhất)
    private String getRateLimitKey(User user) {
        return user.getId().toString();
    }

    // Helper reset rate limit counter
    private void resetRateLimit(String identifier) {
        rateLimitPolicy.reset(identifier);
    }
}

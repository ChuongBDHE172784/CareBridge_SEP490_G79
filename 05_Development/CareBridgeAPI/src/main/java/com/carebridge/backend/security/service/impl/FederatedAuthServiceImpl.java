package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.security.dto.request.FederatedAuthRequest;
import com.carebridge.backend.security.dto.request.LinkGoogleIdentityRequest;
import com.carebridge.backend.security.dto.request.PhoneLoginRequest;
import com.carebridge.backend.security.dto.request.PhoneRegisterRequest;
import com.carebridge.backend.security.dto.response.FederatedAuthResponse;
import com.carebridge.backend.security.dto.response.LinkedGoogleIdentityResponse;
import com.carebridge.backend.security.entity.RefreshToken;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.entity.UserIdentity;
import com.carebridge.backend.security.exception.FederatedAuthException;
import com.carebridge.backend.security.federation.FirebaseTokenVerifier;
import com.carebridge.backend.security.federation.FederatedProvider;
import com.carebridge.backend.security.federation.VerifiedFederatedIdentity;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.mapper.UserMapper;
import com.carebridge.backend.security.policy.AuthenticationPolicy;
import com.carebridge.backend.security.policy.PasswordComplexityPolicy;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.RefreshTokenRepository;
import com.carebridge.backend.security.repository.UserIdentityRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.FederatedAuthService;
import com.carebridge.backend.security.util.TokenUtils;
import com.carebridge.backend.common.validation.VietnamesePhoneNumbers;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.common.util.StringUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FederatedAuthServiceImpl implements FederatedAuthService {
    @Value("${carebridge.auth.federated-enabled:true}")
    private boolean federatedEnabled = true;

    private final FirebaseTokenVerifier tokenVerifier;
    private final UserIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository sessionRepository;
    private final AuthenticationPolicy authenticationPolicy;
    private final PasswordComplexityPolicy passwordComplexityPolicy;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public FederatedAuthResponse authenticate(FederatedAuthRequest request) {
        if (!federatedEnabled) {
            throw FederatedAuthException.unavailable();
        }
        VerifiedFederatedIdentity verified = tokenVerifier.verify(request.idToken());
        if (verified.provider() != FederatedProvider.GOOGLE) {
            // Phone authentication has stricter, intent-specific registration and
            // login contracts. Never let a PHONE token fall back to auto-create.
            throw FederatedAuthException.unsupportedProvider();
        }
        if (verified.subject() == null || verified.subject().isBlank()) {
            throw FederatedAuthException.invalidProof();
        }
        identityRepository.lockProviderSubject(verified.provider().name() + ':' + verified.subject());

        UserIdentity identity = identityRepository
                .findByProviderAndProviderSubject(verified.provider(), verified.subject())
                .orElse(null);
        boolean newUser = identity == null;
        User user;
        if (identity == null) {
            String normalizedPhone = normalizePhone(verified.phoneNumber());
            String normalizedEmail = normalizeEmail(verified.email());
            if (normalizedEmail != null) {
                identityRepository.lockProviderSubject("EMAIL:" + normalizedEmail);
            }
            if (normalizedPhone != null) {
                identityRepository.lockProviderSubject("PHONE_NUMBER:" + normalizedPhone);
            }
            rejectUnlinkedContactCollision(verified, normalizedPhone);
            try {
                user = createUser(verified, normalizedPhone);
                identity = UserIdentity.builder()
                        .user(user)
                        .provider(verified.provider())
                        .providerSubject(verified.subject())
                        .providerEmail(normalizedEmail)
                        .providerPhone(normalizedPhone)
                        .lastUsedAt(Instant.now())
                        .build();
                identityRepository.saveAndFlush(identity);
            } catch (DataIntegrityViolationException collision) {
                throw FederatedAuthException.collision();
            }
        } else {
            user = identity.getUser();
            identity.setLastUsedAt(Instant.now());
            identityRepository.save(identity);
        }

        authenticationPolicy.ensureCanAuthenticate(user);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        FederatedAuthResponse response = issueSession(user, request.deviceInfo(), newUser);
        auditService.log(newUser ? AuditAction.FEDERATED_REGISTRATION : AuditAction.FEDERATED_LOGIN,
                user.getId(), "UserIdentity", identity.getId() == null ? verified.provider().name() : identity.getId().toString(),
                java.util.Map.of("provider", verified.provider().name()));
        return response;
    }

    @Override
    @Transactional
    public FederatedAuthResponse registerPhone(PhoneRegisterRequest request) {
        ensureFederatedEnabled();
        VerifiedFederatedIdentity verified = requirePhoneProof(request.idToken());
        String verifiedPhone = normalizeRequiredPhone(verified.phoneNumber());
        String requestedPhone = normalizeRequiredPhone(request.phone());
        if (!verifiedPhone.equals(requestedPhone)) {
            throw FederatedAuthException.invalidProof();
        }
        if (!passwordComplexityPolicy.isComplexEnough(request.password())) {
            throw new ValidationException(passwordComplexityPolicy.getRequirements());
        }
        if (request.role() == Role.EXPERT) {
            throw new ValidationException("Expert registration requires email verification");
        }

        String email = normalizeEmail(request.email());
        if (email != null) {
            // Serialize phone registration against concurrent registrations that
            // use the same email, even when their Firebase phone subjects differ.
            identityRepository.lockProviderSubject("EMAIL:" + email);
        }
        // Contact advisory locks use the same EMAIL -> PHONE_NUMBER order as
        // Google/federated registration to avoid cross-provider deadlocks.
        identityRepository.lockProviderSubject(FederatedProvider.PHONE.name() + ':' + verified.subject());
        identityRepository.lockProviderSubject("PHONE_NUMBER:" + verifiedPhone);
        if (identityRepository.findByProviderAndProviderSubject(
                    FederatedProvider.PHONE, verified.subject()).isPresent()
                || userRepository.findByPhone(verifiedPhone).isPresent()
                || (email != null && userRepository.findByEmailIgnoreCase(email).isPresent())) {
            throw FederatedAuthException.collision();
        }

        User user;
        UserIdentity identity;
        Instant now = Instant.now();
        try {
            user = userRepository.saveAndFlush(User.builder()
                    .name(StringUtils.sanitizeBasicText(request.name()))
                    .email(email)
                    .phone(verifiedPhone)
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .role(authenticationPolicy.resolveSelfRegistrationRole(request.role()))
                    .accountStatus("ACTIVE")
                    .emailVerified(false)
                    .phoneVerified(true)
                    .enabled(true)
                    .locked(false)
                    .build());
            identity = UserIdentity.builder()
                    .user(user)
                    .provider(FederatedProvider.PHONE)
                    .providerSubject(verified.subject())
                    .providerPhone(verifiedPhone)
                    .createdAt(now)
                    .lastUsedAt(now)
                    .build();
            identityRepository.saveAndFlush(identity);
        } catch (DataIntegrityViolationException collision) {
            throw FederatedAuthException.collision();
        }
        user.setLastLoginAt(now);
        userRepository.save(user);

        FederatedAuthResponse response = issueSession(user, request.deviceInfo(), true);
        auditService.log(AuditAction.FEDERATED_REGISTRATION, user.getId(), "UserIdentity",
                identity.getId() == null ? FederatedProvider.PHONE.name() : identity.getId().toString(),
                java.util.Map.of("provider", FederatedProvider.PHONE.name()));
        return response;
    }

    @Override
    @Transactional
    public FederatedAuthResponse loginPhone(PhoneLoginRequest request) {
        ensureFederatedEnabled();
        VerifiedFederatedIdentity verified = requirePhoneProof(request.idToken());
        String verifiedPhone = normalizeRequiredPhone(verified.phoneNumber());
        identityRepository.lockProviderSubject(FederatedProvider.PHONE.name() + ':' + verified.subject());

        UserIdentity identity = identityRepository
                .findByProviderAndProviderSubject(FederatedProvider.PHONE, verified.subject())
                .orElse(null);
        boolean newUser = false;
        User user;
        if (identity != null) {
            user = identity.getUser();
            requireMatchingUserPhone(user, verifiedPhone);
        } else {
            identityRepository.lockProviderSubject("PHONE_NUMBER:" + verifiedPhone);
            user = userRepository.findByPhone(verifiedPhone).orElse(null);
            if (user == null) {
                try {
                    user = createUser(verified, verifiedPhone);
                } catch (DataIntegrityViolationException collision) {
                    throw FederatedAuthException.collision();
                }
                newUser = true;
            } else {
                requireMatchingUserPhone(user, verifiedPhone);
                if (!Boolean.TRUE.equals(user.getPhoneVerified())) {
                    // An optional, unverified profile phone is not an authentication
                    // factor. Linking it requires an authenticated account flow.
                    throw FederatedAuthException.invalidProof();
                }
                authenticationPolicy.ensureCanAuthenticate(user);
                identityRepository.lockUserProvider(user.getId() + ":" + FederatedProvider.PHONE.name());
                if (identityRepository.findByUserIdAndProvider(user.getId(), FederatedProvider.PHONE).isPresent()) {
                    throw FederatedAuthException.invalidProof();
                }
            }
            identity = UserIdentity.builder()
                    .user(user)
                    .provider(FederatedProvider.PHONE)
                    .providerSubject(verified.subject())
                    .providerPhone(verifiedPhone)
                    .createdAt(Instant.now())
                    .lastUsedAt(Instant.now())
                    .build();
            identityRepository.saveAndFlush(identity);
        }

        authenticationPolicy.ensureCanAuthenticate(user);
        identity.setProviderPhone(verifiedPhone);
        identity.setLastUsedAt(Instant.now());
        identityRepository.save(identity);
        user.setPhoneVerified(true);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        FederatedAuthResponse response = issueSession(user, request.deviceInfo(), newUser);
        auditService.log(newUser ? AuditAction.FEDERATED_REGISTRATION : AuditAction.FEDERATED_LOGIN,
                user.getId(), "UserIdentity",
                identity.getId() == null ? FederatedProvider.PHONE.name() : identity.getId().toString(),
                java.util.Map.of("provider", FederatedProvider.PHONE.name()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public LinkedGoogleIdentityResponse getGoogleIdentity(UUID userId) {
        ensureFederatedEnabled();
        User user = requireActiveUser(userId);
        return identityRepository.findByUserIdAndProvider(user.getId(), FederatedProvider.GOOGLE)
                .map(this::toLinkedGoogleIdentityResponse)
                .orElseGet(() -> new LinkedGoogleIdentityResponse(
                        FederatedProvider.GOOGLE, false, null, null));
    }

    @Override
    @Transactional
    public LinkedGoogleIdentityResponse linkGoogleIdentity(
            UUID userId, LinkGoogleIdentityRequest request) {
        ensureFederatedEnabled();
        User user = requireActiveUser(userId);
        VerifiedFederatedIdentity verified = tokenVerifier.verify(request.idToken());
        if (verified.provider() != FederatedProvider.GOOGLE) {
            throw FederatedAuthException.unsupportedProvider();
        }
        if (verified.subject() == null || verified.subject().isBlank()) {
            throw FederatedAuthException.invalidProof();
        }

        identityRepository.lockProviderSubject(FederatedProvider.GOOGLE.name() + ':' + verified.subject());
        identityRepository.lockUserProvider(userId + ":" + FederatedProvider.GOOGLE.name());

        UserIdentity subjectIdentity = identityRepository
                .findByProviderAndProviderSubject(FederatedProvider.GOOGLE, verified.subject())
                .orElse(null);
        UserIdentity userIdentity = identityRepository
                .findByUserIdAndProvider(userId, FederatedProvider.GOOGLE)
                .orElse(null);

        if (subjectIdentity != null && !userId.equals(subjectIdentity.getUser().getId())) {
            throw FederatedAuthException.identityOwnedByAnotherUser();
        }
        if (userIdentity != null && !verified.subject().equals(userIdentity.getProviderSubject())) {
            throw FederatedAuthException.userAlreadyLinkedDifferentIdentity();
        }
        if (subjectIdentity != null) {
            return toLinkedGoogleIdentityResponse(subjectIdentity);
        }

        Instant now = Instant.now();
        UserIdentity identity = UserIdentity.builder()
                .user(user)
                .provider(FederatedProvider.GOOGLE)
                .providerSubject(verified.subject())
                .providerEmail(normalizeEmail(verified.email()))
                .createdAt(now)
                .lastUsedAt(now)
                .build();
        identityRepository.saveAndFlush(identity);
        auditService.log(
                AuditAction.FEDERATED_IDENTITY_LINKED,
                userId,
                "UserIdentity",
                identity.getId() == null ? userId.toString() : identity.getId().toString(),
                java.util.Map.of("provider", FederatedProvider.GOOGLE.name()));
        return toLinkedGoogleIdentityResponse(identity);
    }

    private void ensureFederatedEnabled() {
        if (!federatedEnabled) {
            throw FederatedAuthException.unavailable();
        }
    }

    private VerifiedFederatedIdentity requirePhoneProof(String idToken) {
        VerifiedFederatedIdentity verified = tokenVerifier.verify(idToken);
        if (verified.provider() != FederatedProvider.PHONE) {
            throw FederatedAuthException.unsupportedProvider();
        }
        if (verified.subject() == null || verified.subject().isBlank()
                || !verified.phoneVerified() || verified.phoneNumber() == null) {
            throw FederatedAuthException.invalidProof();
        }
        return verified;
    }

    private void requireMatchingUserPhone(User user, String verifiedPhone) {
        String storedPhone = normalizeRequiredPhone(user.getPhone());
        if (!verifiedPhone.equals(storedPhone)) {
            throw FederatedAuthException.invalidProof();
        }
    }

    private User requireActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(FederatedAuthException::invalidProof);
        authenticationPolicy.ensureCanAuthenticate(user);
        return user;
    }

    private LinkedGoogleIdentityResponse toLinkedGoogleIdentityResponse(UserIdentity identity) {
        return new LinkedGoogleIdentityResponse(
                FederatedProvider.GOOGLE,
                true,
                identity.getProviderEmail(),
                identity.getCreatedAt());
    }

    private User createUser(VerifiedFederatedIdentity verified, String normalizedPhone) {
        // saveAndFlush: UserIdentityRepository.save writes users.social_identities via plain
        // JDBC, so the freshly created user row must already be flushed to the database.
        return userRepository.saveAndFlush(User.builder()

                .email(normalizeEmail(verified.email()))
                .phone(normalizedPhone)
                .name(verified.displayName())
                .accountStatus("ACTIVE")
                .emailVerified(verified.emailVerified())
                .phoneVerified(verified.phoneVerified())
                .enabled(true)
                .locked(false)
                .build());
    }

    private void rejectUnlinkedContactCollision(
            VerifiedFederatedIdentity verified, String normalizedPhone) {
        String email = normalizeEmail(verified.email());
        if ((email != null && userRepository.findByEmailIgnoreCase(email).isPresent())
                || (normalizedPhone != null && userRepository.findByPhone(normalizedPhone).isPresent())) {
            throw FederatedAuthException.collision();
        }
    }

    private FederatedAuthResponse issueSession(User user, String deviceInfo, boolean newUser) {
        String safeDeviceInfo = deviceInfo == null || deviceInfo.isBlank()
                ? "Unknown"
                : deviceInfo.trim();
        if (safeDeviceInfo.length() > 150) {
            safeDeviceInfo = safeDeviceInfo.substring(0, 150);
        }
        String rawRefreshToken = UUID.randomUUID() + "." + UUID.randomUUID();
        String hash = TokenUtils.hashSha256(rawRefreshToken);
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user).token(rawRefreshToken).tokenHash(hash).expiresAt(expiresAt).build());

        UUID sessionId = UUID.randomUUID();
        sessionRepository.save(UserSession.builder()
                .sessionId(sessionId).userId(user.getId()).refreshTokenHash(hash)
                .deviceName(safeDeviceInfo).browser(safeDeviceInfo)
                .lastActivityAt(Instant.now()).expiresAt(expiresAt).status("active")
                .isCurrent(true).createdAt(Instant.now()).updatedAt(Instant.now()).build());
        sessionRepository.clearCurrentSessions(user.getId(), sessionId);

        return FederatedAuthResponse.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(user, sessionId))
                .refreshToken(rawRefreshToken)
                .user(userMapper.toProfileResponse(user))
                .newUser(newUser)
                .profileCompleted(user.getRole() != null)
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        try {
            return VietnamesePhoneNumbers.normalizeToE164(phone);
        } catch (IllegalArgumentException invalidPhone) {
            throw FederatedAuthException.invalidProof();
        }
    }

    private String normalizeRequiredPhone(String phone) {
        String normalized = normalizePhone(phone);
        if (normalized == null) {
            throw FederatedAuthException.invalidProof();
        }
        return normalized;
    }
}

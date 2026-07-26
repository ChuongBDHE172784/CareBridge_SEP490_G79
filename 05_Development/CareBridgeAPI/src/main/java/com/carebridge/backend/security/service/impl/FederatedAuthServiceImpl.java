package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.security.dto.request.FederatedAuthRequest;
import com.carebridge.backend.security.dto.request.LinkGoogleIdentityRequest;
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
import com.carebridge.backend.security.repository.RefreshTokenRepository;
import com.carebridge.backend.security.repository.UserIdentityRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.FederatedAuthService;
import com.carebridge.backend.security.util.TokenUtils;
import com.carebridge.backend.common.validation.VietnamesePhoneNumbers;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
            rejectUnlinkedContactCollision(verified, normalizedPhone);
            user = createUser(verified, normalizedPhone);
            identity = UserIdentity.builder()
                    .user(user)
                    .provider(verified.provider())
                    .providerSubject(verified.subject())
                    .providerEmail(normalizeEmail(verified.email()))
                    .providerPhone(normalizedPhone)
                    .lastUsedAt(Instant.now())
                    .build();
            identityRepository.saveAndFlush(identity);
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
        return userRepository.save(User.builder()
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
        String rawRefreshToken = UUID.randomUUID() + "." + UUID.randomUUID();
        String hash = TokenUtils.hashSha256(rawRefreshToken);
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user).token(rawRefreshToken).tokenHash(hash).expiresAt(expiresAt).build());

        UUID sessionId = UUID.randomUUID();
        sessionRepository.save(UserSession.builder()
                .sessionId(sessionId).userId(user.getId()).refreshTokenHash(hash)
                .deviceName(deviceInfo).browser(deviceInfo == null ? "Unknown" : deviceInfo)
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
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        try {
            return VietnamesePhoneNumbers.normalizeToE164(phone);
        } catch (IllegalArgumentException invalidPhone) {
            throw FederatedAuthException.invalidProof();
        }
    }
}

package com.carebridge.backend.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.security.dto.request.FederatedAuthRequest;
import com.carebridge.backend.security.dto.request.LinkGoogleIdentityRequest;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.entity.UserIdentity;
import com.carebridge.backend.security.exception.FederatedAuthException;
import com.carebridge.backend.security.federation.*;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.mapper.UserMapper;
import com.carebridge.backend.security.policy.AuthenticationPolicy;
import com.carebridge.backend.security.repository.RefreshTokenRepository;
import com.carebridge.backend.security.repository.UserIdentityRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.impl.FederatedAuthServiceImpl;
import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class FederatedAuthServiceTest {
    private final FirebaseTokenVerifier verifier = mock(FirebaseTokenVerifier.class);
    private final UserIdentityRepository identities = mock(UserIdentityRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final UserSessionRepository sessions = mock(UserSessionRepository.class);
    private final JwtTokenProvider jwt = mock(JwtTokenProvider.class);
    private final AuditService audit = mock(AuditService.class);
    private FederatedAuthService service;

    @BeforeEach
    void setUp() {
        reset(verifier, identities, users, refreshTokens, sessions, jwt, audit);
        when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(users.findByPhone(anyString())).thenReturn(Optional.empty());
        when(identities.findByUserIdAndProvider(any(UUID.class), any(FederatedProvider.class)))
                .thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) user.setId(UUID.randomUUID());
            return user;
        });
        when(identities.saveAndFlush(any(UserIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(identities.save(any(UserIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwt.generateAccessToken(any(User.class), any(UUID.class))).thenReturn("carebridge-access-token");
        service = new FederatedAuthServiceImpl(verifier, identities, users, refreshTokens, sessions,
                new AuthenticationPolicy(), jwt, new UserMapper(), audit);
    }

    @Test
    void newGoogleIdentity_createsCareBridgeAccountAndSession() {
        when(verifier.verify("valid-google-token")).thenReturn(google("google-1", "new@example.com"));
        var response = service.authenticate(request("valid-google-token"));
        assertThat(response.newUser()).isTrue();
        assertThat(response.accessToken()).isEqualTo("carebridge-access-token");
        assertThat(response.refreshToken()).isNotBlank();
        verify(identities).saveAndFlush(any(UserIdentity.class));
        verify(sessions).save(any(UserSession.class));
    }

    @Test
    void newPhoneIdentity_normalizesE164AndCreatesAccount() {
        when(verifier.verify("valid-phone-token")).thenReturn(phone("phone-1", "0901111001"));
        var response = service.authenticate(request("valid-phone-token"));
        assertThat(response.user().getPhone()).isEqualTo("+84901111001");
        assertThat(response.user().getPhoneVerified()).isTrue();
    }

    @Test
    void invalidExpiredOrRevokedToken_isRejectedWithoutSession() {
        when(verifier.verify("invalid-token")).thenThrow(FederatedAuthException.invalidProof());
        assertThatThrownBy(() -> service.authenticate(request("invalid-token")))
                .isInstanceOf(FederatedAuthException.class).hasMessage("Unable to authenticate");
        verifyNoInteractions(sessions);
    }

    @Test
    void existingUnlinkedContact_isNotAutomaticallyLinked() {
        when(verifier.verify("colliding-contact-token")).thenReturn(google("google-2", "existing@example.com"));
        when(users.findByEmailIgnoreCase("existing@example.com"))
                .thenReturn(Optional.of(activeUser(UUID.randomUUID(), "existing@example.com", null)));
        assertThatThrownBy(() -> service.authenticate(request("colliding-contact-token")))
                .isInstanceOf(FederatedAuthException.class).hasMessage("Existing account requires verification");
        verify(identities, never()).saveAndFlush(any());
    }

    @Test
    void repeatedProviderSubject_doesNotCreateDuplicateUser() {
        VerifiedFederatedIdentity proof = phone("stable-1", "+84901111002");
        when(verifier.verify("stable-subject-token")).thenReturn(proof);
        AtomicReference<UserIdentity> saved = new AtomicReference<>();
        when(identities.findByProviderAndProviderSubject(FederatedProvider.PHONE, "stable-1"))
                .thenAnswer(ignored -> Optional.ofNullable(saved.get()));
        when(identities.saveAndFlush(any())).thenAnswer(invocation -> {
            UserIdentity identity = invocation.getArgument(0);
            saved.set(identity);
            return identity;
        });
        var first = service.authenticate(request("stable-subject-token"));
        var second = service.authenticate(request("stable-subject-token"));
        assertThat(first.user().getId()).isEqualTo(second.user().getId());
        verify(users, times(3)).save(any(User.class)); // one create plus last-login updates for both requests
        verify(identities, times(1)).saveAndFlush(any());
    }

    @Test
    void providerTimeout_failsClosedWithoutMutation() {
        when(verifier.verify("provider-timeout")).thenThrow(FederatedAuthException.unavailable());
        assertThatThrownBy(() -> service.authenticate(request("provider-timeout")))
                .isInstanceOf(FederatedAuthException.class).hasMessage("Identity provider unavailable");
        verifyNoInteractions(users, sessions, refreshTokens);
    }

    @Test
    void disabledFeatureFlag_failsClosedBeforeProviderVerification() {
        ReflectionTestUtils.setField(service, "federatedEnabled", false);
        assertThatThrownBy(() -> service.authenticate(request("valid-google-token")))
                .isInstanceOf(FederatedAuthException.class).hasMessage("Identity provider unavailable");
        verifyNoInteractions(verifier, users, sessions, refreshTokens);
    }

    @Test
    void lockedDisabledOrSuspendedAccount_cannotCreateSession() {
        User blocked = activeUser(UUID.randomUUID(), null, "+84901111003");
        blocked.setLocked(true);
        blocked.setLockedAt(Instant.now());
        UserIdentity identity = UserIdentity.builder().user(blocked).provider(FederatedProvider.PHONE)
                .providerSubject("blocked-1").lastUsedAt(Instant.now()).build();
        when(verifier.verify("blocked-account-token")).thenReturn(phone("blocked-1", blocked.getPhone()));
        when(identities.findByProviderAndProviderSubject(FederatedProvider.PHONE, "blocked-1"))
                .thenReturn(Optional.of(identity));
        assertThatThrownBy(() -> service.authenticate(request("blocked-account-token")))
                .hasMessageContaining("locked");
        verifyNoInteractions(sessions);
    }

    @Test
    void rolelessAccount_isRoutedToProfileCompletion() {
        when(verifier.verify("roleless-user-token")).thenReturn(phone("roleless-1", "+84901111004"));
        var response = service.authenticate(request("roleless-user-token"));
        assertThat(response.profileCompleted()).isFalse();
        assertThat(response.accessToken()).isNotBlank();
    }

    @Test
    void googleLinkStatus_withoutIdentity_returnsUnlinkedWithoutVerifyingProvider() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId, "existing@example.com", null);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        var response = service.getGoogleIdentity(userId);

        assertThat(response.provider()).isEqualTo(FederatedProvider.GOOGLE);
        assertThat(response.linked()).isFalse();
        assertThat(response.email()).isNull();
        verifyNoInteractions(verifier);
        verifyNoInteractions(sessions, refreshTokens);
    }

    @Test
    void googleLinkStatus_withIdentity_returnsProviderEmailAndLinkedAt() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId, "existing@example.com", null);
        UserIdentity identity = googleIdentity(user, "google-link-1", "google@example.com");
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(identities.findByUserIdAndProvider(userId, FederatedProvider.GOOGLE))
                .thenReturn(Optional.of(identity));

        var response = service.getGoogleIdentity(userId);

        assertThat(response.linked()).isTrue();
        assertThat(response.email()).isEqualTo("google@example.com");
        assertThat(response.linkedAt()).isEqualTo(identity.getCreatedAt());
        verifyNoInteractions(verifier, sessions, refreshTokens);
    }

    @Test
    void linkGoogleIdentity_validProof_linksCurrentUserWithoutCreatingSession() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId, "existing@example.com", null);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(verifier.verify("fresh-google-token"))
                .thenReturn(google("google-link-1", "google@example.com"));

        var response = service.linkGoogleIdentity(
                userId, new LinkGoogleIdentityRequest("fresh-google-token"));

        assertThat(response.linked()).isTrue();
        assertThat(response.email()).isEqualTo("google@example.com");
        verify(identities).lockProviderSubject("GOOGLE:google-link-1");
        verify(identities).lockUserProvider(userId + ":GOOGLE");
        ArgumentCaptor<UserIdentity> identityCaptor = ArgumentCaptor.forClass(UserIdentity.class);
        verify(identities).saveAndFlush(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getUser()).isSameAs(user);
        assertThat(identityCaptor.getValue().getProviderSubject()).isEqualTo("google-link-1");
        verify(audit).log(eq(AuditAction.FEDERATED_IDENTITY_LINKED), eq(userId),
                eq("UserIdentity"), any(), eq(java.util.Map.of("provider", "GOOGLE")));
        verifyNoInteractions(sessions, refreshTokens, jwt);
    }

    @Test
    void linkGoogleIdentity_sameUserAndSubject_isIdempotentWithoutDuplicateAudit() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId, "existing@example.com", null);
        UserIdentity identity = googleIdentity(user, "google-link-1", "google@example.com");
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(verifier.verify("same-google-token"))
                .thenReturn(google("google-link-1", "google@example.com"));
        when(identities.findByProviderAndProviderSubject(FederatedProvider.GOOGLE, "google-link-1"))
                .thenReturn(Optional.of(identity));
        when(identities.findByUserIdAndProvider(userId, FederatedProvider.GOOGLE))
                .thenReturn(Optional.of(identity));

        var response = service.linkGoogleIdentity(
                userId, new LinkGoogleIdentityRequest("same-google-token"));

        assertThat(response.linked()).isTrue();
        verify(identities, never()).saveAndFlush(any());
        verifyNoInteractions(audit, sessions, refreshTokens, jwt);
    }

    @Test
    void linkGoogleIdentity_subjectOwnedByAnotherUser_returnsNeutralConflict() {
        UUID userId = UUID.randomUUID();
        User currentUser = activeUser(userId, "current@example.com", null);
        User otherUser = activeUser(UUID.randomUUID(), "other@example.com", null);
        when(users.findById(userId)).thenReturn(Optional.of(currentUser));
        when(verifier.verify("owned-google-token"))
                .thenReturn(google("owned-subject", "other@example.com"));
        when(identities.findByProviderAndProviderSubject(FederatedProvider.GOOGLE, "owned-subject"))
                .thenReturn(Optional.of(googleIdentity(otherUser, "owned-subject", "other@example.com")));

        assertThatThrownBy(() -> service.linkGoogleIdentity(
                userId, new LinkGoogleIdentityRequest("owned-google-token")))
                .isInstanceOf(FederatedAuthException.class)
                .hasMessage("This Google account cannot be linked");
        verify(identities, never()).saveAndFlush(any());
        verifyNoInteractions(audit, sessions, refreshTokens, jwt);
    }

    @Test
    void linkGoogleIdentity_userAlreadyLinkedDifferentSubject_returnsConflictWithoutReplacement() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId, "existing@example.com", null);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(verifier.verify("different-google-token"))
                .thenReturn(google("google-link-2", "second@example.com"));
        when(identities.findByUserIdAndProvider(userId, FederatedProvider.GOOGLE))
                .thenReturn(Optional.of(googleIdentity(user, "google-link-1", "first@example.com")));

        assertThatThrownBy(() -> service.linkGoogleIdentity(
                userId, new LinkGoogleIdentityRequest("different-google-token")))
                .isInstanceOf(FederatedAuthException.class)
                .hasMessage("A different Google account is already linked");
        verify(identities, never()).saveAndFlush(any());
        verifyNoInteractions(audit, sessions, refreshTokens, jwt);
    }

    @Test
    void linkGoogleIdentity_phoneProof_isRejectedWithoutMutation() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId, "existing@example.com", null);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(verifier.verify("phone-token")).thenReturn(phone("phone-1", "+84901111001"));

        assertThatThrownBy(() -> service.linkGoogleIdentity(
                userId, new LinkGoogleIdentityRequest("phone-token")))
                .isInstanceOf(FederatedAuthException.class)
                .hasMessage("Unsupported identity provider");
        verify(identities, never()).saveAndFlush(any());
        verifyNoInteractions(audit, sessions, refreshTokens, jwt);
    }

    @Test
    void linkGoogleIdentity_invalidOrRevokedProof_isRejectedWithoutMutation() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId, "existing@example.com", null);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(verifier.verify("revoked-google-token"))
                .thenThrow(FederatedAuthException.invalidProof());

        assertThatThrownBy(() -> service.linkGoogleIdentity(
                userId, new LinkGoogleIdentityRequest("revoked-google-token")))
                .isInstanceOf(FederatedAuthException.class)
                .hasMessage("Unable to authenticate");
        verify(identities, never()).saveAndFlush(any());
        verifyNoInteractions(audit, sessions, refreshTokens, jwt);
    }

    @Test
    void linkGoogleIdentity_providerOutage_failsClosedWithoutMutation() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId, "existing@example.com", null);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(verifier.verify("provider-timeout"))
                .thenThrow(FederatedAuthException.unavailable());

        assertThatThrownBy(() -> service.linkGoogleIdentity(
                userId, new LinkGoogleIdentityRequest("provider-timeout")))
                .isInstanceOf(FederatedAuthException.class)
                .hasMessage("Identity provider unavailable");
        verify(identities, never()).saveAndFlush(any());
        verifyNoInteractions(audit, sessions, refreshTokens, jwt);
    }

    @Test
    void linkGoogleIdentity_blockedCurrentUser_isRejectedBeforeMutation() {
        UUID userId = UUID.randomUUID();
        User blocked = activeUser(userId, "blocked@example.com", null);
        blocked.setLocked(true);
        blocked.setLockedAt(Instant.now());
        when(users.findById(userId)).thenReturn(Optional.of(blocked));
        when(verifier.verify("fresh-google-token"))
                .thenReturn(google("google-link-1", "google@example.com"));

        assertThatThrownBy(() -> service.linkGoogleIdentity(
                userId, new LinkGoogleIdentityRequest("fresh-google-token")))
                .hasMessageContaining("locked");
        verify(identities, never()).saveAndFlush(any());
        verifyNoInteractions(audit, sessions, refreshTokens, jwt);
    }

    private User activeUser(UUID id, String email, String phone) {
        return User.builder().id(id).email(email).phone(phone).name("Federated User")
                .accountStatus("ACTIVE").emailVerified(email != null).phoneVerified(phone != null)
                .enabled(true).locked(false).build();
    }

    private VerifiedFederatedIdentity google(String subject, String email) {
        return new VerifiedFederatedIdentity(FederatedProvider.GOOGLE, subject, email, null,
                "Google User", true, false);
    }

    private VerifiedFederatedIdentity phone(String subject, String phone) {
        return new VerifiedFederatedIdentity(FederatedProvider.PHONE, subject, null, phone,
                "Phone User", false, true);
    }

    private UserIdentity googleIdentity(User user, String subject, String email) {
        return UserIdentity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .provider(FederatedProvider.GOOGLE)
                .providerSubject(subject)
                .providerEmail(email)
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .build();
    }

    private FederatedAuthRequest request(String token) {
        return new FederatedAuthRequest(token, "JUnit device");
    }
}

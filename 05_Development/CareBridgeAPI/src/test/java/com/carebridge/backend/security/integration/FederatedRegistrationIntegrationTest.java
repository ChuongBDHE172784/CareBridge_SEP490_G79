package com.carebridge.backend.security.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.security.dto.request.FederatedAuthRequest;
import com.carebridge.backend.security.dto.request.PhoneLoginRequest;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.entity.UserIdentity;
import com.carebridge.backend.security.exception.FederatedAuthException;
import com.carebridge.backend.security.federation.*;
import com.carebridge.backend.security.repository.UserIdentityRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.FederatedAuthService;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** RED PostgreSQL contracts for federated account creation and identity uniqueness. */
class FederatedRegistrationIntegrationTest extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private FederatedAuthService service;
    @Autowired private UserRepository users;
    @Autowired private UserIdentityRepository identities;
    @MockitoBean private FirebaseTokenVerifier verifier;

    @Test
    void userIdentitiesSchema_enforcesUniqueProviderSubject() {
        // Canonical schema: the user_identities table (and its UNIQUE constraint) is gone;
        // identities live in users.social_identities and uniqueness of (provider,
        // providerSubject) across ALL users is enforced by UserIdentityRepository.save
        // (advisory locks + duplicate check -> DataIntegrityViolationException).
        when(verifier.verify("unique-subject-token")).thenReturn(new VerifiedFederatedIdentity(
                FederatedProvider.PHONE, "unique-subject-1", null, "+84901110003",
                "Unique Mother", false, true));
        service.loginPhone(new PhoneLoginRequest("unique-subject-token", "JUnit"));

        User other = users.save(User.builder().email("unique.subject.other@example.com")
                .name("Other Owner").accountStatus("ACTIVE").emailVerified(true)
                .phoneVerified(false).enabled(true).locked(false).build());
        assertThatThrownBy(() -> identities.save(UserIdentity.builder()
                .user(other)
                .provider(FederatedProvider.PHONE)
                .providerSubject("unique-subject-1")
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);

        Integer owners = jdbc.queryForObject("""
                select count(*)
                  from users u
                 cross join lateral jsonb_array_elements(coalesce(u.social_identities, '[]'::jsonb)) identity
                 where identity->>'provider' = 'PHONE'
                   and identity->>'providerSubject' = 'unique-subject-1'
                """, Integer.class);
        assertThat(owners).isEqualTo(1);
    }

    @Test
    void collidingContact_cannotCreateOrAutoLinkIdentity() {
        String email = "existing.federated@example.com";
        users.save(User.builder().email(email).name("Existing User").accountStatus("ACTIVE")
                .emailVerified(true).phoneVerified(false).enabled(true).locked(false).build());
        when(verifier.verify("collision-token")).thenReturn(new VerifiedFederatedIdentity(
                FederatedProvider.GOOGLE, "google-collision-1", email, null, "Existing User", true, false));

        assertThatThrownBy(() -> service.authenticate(new FederatedAuthRequest("collision-token", "JUnit")))
                .isInstanceOf(FederatedAuthException.class)
                .hasMessage("Existing account requires verification");
        Integer identityCount = jdbc.queryForObject("""
                select count(*)
                  from users u
                 cross join lateral jsonb_array_elements(coalesce(u.social_identities, '[]'::jsonb)) identity
                 where identity->>'providerSubject' = 'google-collision-1'
                """, Integer.class);
        assertThat(identityCount).isZero();
    }
}

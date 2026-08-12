package com.carebridge.backend.security.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.AbstractEmbeddedPostgresIntegrationTest;
import com.carebridge.backend.security.dto.request.FederatedAuthRequest;
import com.carebridge.backend.security.dto.request.PhoneLoginRequest;
import com.carebridge.backend.security.federation.*;
import com.carebridge.backend.security.service.FederatedAuthService;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** RED PostgreSQL contracts for federated session persistence and replay safety. */
class FederatedLoginIntegrationTest extends AbstractEmbeddedPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private FederatedAuthService service;
    @MockitoBean private FirebaseTokenVerifier verifier;

    @Test
    void federatedLogin_persistsOnlyHashedRefreshReferences() {
        when(verifier.verify("google-login-token")).thenReturn(googleIdentity("uid-login-1", "login@example.com"));
        service.authenticate(new FederatedAuthRequest("google-login-token", "JUnit device"));
        Integer hashedSessions = jdbc.queryForObject("""
                select count(*) from auth_sessions
                where refresh_token_hash is not null and length(refresh_token_hash) = 64
                """, Integer.class);
        assertThat(hashedSessions).isPositive();
    }

    @Test
    void replayedProviderSubject_doesNotDuplicateIdentity() {
        when(verifier.verify("replay-token")).thenReturn(googleIdentity("uid-replay-1", "replay@example.com"));
        service.authenticate(new FederatedAuthRequest("replay-token", "JUnit device"));
        service.authenticate(new FederatedAuthRequest("replay-token", "JUnit device"));
        // Canonical schema: federated identities live in users.social_identities (jsonb).
        Integer duplicateSubjects = jdbc.queryForObject("""
                select count(*) from (
                    select identity->>'provider' as provider,
                           identity->>'providerSubject' as provider_subject
                      from users u
                     cross join lateral jsonb_array_elements(coalesce(u.social_identities, '[]'::jsonb)) identity
                     group by 1, 2 having count(*) > 1
                ) duplicates
                """, Integer.class);
        assertThat(duplicateSubjects).isZero();
        Integer identities = jdbc.queryForObject("""
                select count(*)
                  from users u
                 cross join lateral jsonb_array_elements(coalesce(u.social_identities, '[]'::jsonb)) identity
                 where identity->>'providerSubject' = 'uid-replay-1'
                """, Integer.class);
        assertThat(identities).isEqualTo(1);
    }

    @Test
    void replayedPhoneSubject_autoCreatesExactlyOneUserAndIdentity() {
        when(verifier.verify("phone-replay-token")).thenReturn(new VerifiedFederatedIdentity(
                FederatedProvider.PHONE, "phone-replay-1", null, "+84901110991",
                null, false, true));

        var first = service.loginPhone(new PhoneLoginRequest("phone-replay-token", "JUnit device"));
        var replay = service.loginPhone(new PhoneLoginRequest("phone-replay-token", "JUnit device"));

        assertThat(first.newUser()).isTrue();
        assertThat(replay.newUser()).isFalse();
        assertThat(replay.user().getId()).isEqualTo(first.user().getId());
        Integer users = jdbc.queryForObject(
                "select count(*) from users where phone = '+84901110991'", Integer.class);
        assertThat(users).isEqualTo(1);
        Integer identities = jdbc.queryForObject("""
                select count(*)
                  from users u
                 cross join lateral jsonb_array_elements(coalesce(u.social_identities, '[]'::jsonb)) identity
                 where identity->>'provider' = 'PHONE'
                   and identity->>'providerSubject' = 'phone-replay-1'
                """, Integer.class);
        assertThat(identities).isEqualTo(1);
    }

    private VerifiedFederatedIdentity googleIdentity(String subject, String email) {
        return new VerifiedFederatedIdentity(FederatedProvider.GOOGLE, subject, email, null,
                "Federated User", true, false);
    }
}

package com.carebridge.backend.security.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.security.dto.request.FederatedAuthRequest;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.exception.FederatedAuthException;
import com.carebridge.backend.security.federation.*;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.FederatedAuthService;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** RED PostgreSQL contracts for federated account creation and identity uniqueness. */
class FederatedRegistrationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private FederatedAuthService service;
    @Autowired private UserRepository users;
    @MockitoBean private FirebaseTokenVerifier verifier;

    @Test
    void userIdentitiesSchema_enforcesUniqueProviderSubject() {
        Integer constraints = jdbc.queryForObject("""
                select count(*) from pg_constraint c
                join pg_class t on t.oid = c.conrelid
                where t.relname = 'user_identities' and c.contype = 'u'
                """, Integer.class);
        assertThat(constraints).isGreaterThanOrEqualTo(1);
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
        Integer identityCount = jdbc.queryForObject(
                "select count(*) from user_identities where provider_subject = 'google-collision-1'", Integer.class);
        assertThat(identityCount).isZero();
    }
}

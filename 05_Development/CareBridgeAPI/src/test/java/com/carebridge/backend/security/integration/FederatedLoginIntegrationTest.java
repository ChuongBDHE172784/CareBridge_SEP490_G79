package com.carebridge.backend.security.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.security.dto.request.FederatedAuthRequest;
import com.carebridge.backend.security.federation.*;
import com.carebridge.backend.security.service.FederatedAuthService;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** RED PostgreSQL contracts for federated session persistence and replay safety. */
class FederatedLoginIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private FederatedAuthService service;
    @MockitoBean private FirebaseTokenVerifier verifier;

    @Test
    void federatedLogin_persistsOnlyHashedRefreshReferences() {
        when(verifier.verify("phone-login-token")).thenReturn(phoneIdentity("uid-login-1", "+84901110001"));
        service.authenticate(new FederatedAuthRequest("phone-login-token", "JUnit device"));
        Integer hashedSessions = jdbc.queryForObject("""
                select count(*) from user_sessions
                where refresh_token_hash is not null and length(refresh_token_hash) = 64
                """, Integer.class);
        assertThat(hashedSessions).isPositive();
    }

    @Test
    void replayedProviderSubject_doesNotDuplicateIdentity() {
        when(verifier.verify("replay-token")).thenReturn(phoneIdentity("uid-replay-1", "+84901110002"));
        service.authenticate(new FederatedAuthRequest("replay-token", "JUnit device"));
        service.authenticate(new FederatedAuthRequest("replay-token", "JUnit device"));
        Integer duplicateSubjects = jdbc.queryForObject("""
                select count(*) from (
                    select provider, provider_subject from user_identities
                    group by provider, provider_subject having count(*) > 1
                ) duplicates
                """, Integer.class);
        assertThat(duplicateSubjects).isZero();
        Integer identities = jdbc.queryForObject(
                "select count(*) from user_identities where provider_subject = 'uid-replay-1'", Integer.class);
        assertThat(identities).isEqualTo(1);
    }

    private VerifiedFederatedIdentity phoneIdentity(String subject, String phone) {
        return new VerifiedFederatedIdentity(FederatedProvider.PHONE, subject, null, phone,
                "Federated Mother", false, true);
    }
}

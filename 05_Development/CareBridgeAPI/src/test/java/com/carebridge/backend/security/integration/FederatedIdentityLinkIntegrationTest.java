package com.carebridge.backend.security.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.security.dto.request.LinkGoogleIdentityRequest;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.federation.FederatedProvider;
import com.carebridge.backend.security.federation.FirebaseTokenVerifier;
import com.carebridge.backend.security.federation.VerifiedFederatedIdentity;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.FederatedAuthService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

class FederatedIdentityLinkIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private FederatedAuthService service;
    @Autowired private UserRepository users;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @MockitoBean private FirebaseTokenVerifier verifier;

    @Test
    void identityEndpoints_requireCareBridgeAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/identities/google"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/identities/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"fresh-google-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void validGoogleProof_persistsOneIdentityAndOneAuditWhileReplayIsIdempotent() {
        User user = users.saveAndFlush(User.builder()
                .email("link.integration@example.com")
                .name("Link Integration")
                .accountStatus("ACTIVE")
                .emailVerified(true)
                .phoneVerified(false)
                .enabled(true)
                .locked(false)
                .build());
        when(verifier.verify("integration-google-token"))
                .thenReturn(new VerifiedFederatedIdentity(
                        FederatedProvider.GOOGLE,
                        "integration-google-subject",
                        "linked.google@example.com",
                        null,
                        "Linked Google",
                        true,
                        false));

        var first = service.linkGoogleIdentity(
                user.getId(), new LinkGoogleIdentityRequest("integration-google-token"));
        var replay = service.linkGoogleIdentity(
                user.getId(), new LinkGoogleIdentityRequest("integration-google-token"));

        assertThat(first.linked()).isTrue();
        assertThat(replay.email()).isEqualTo("linked.google@example.com");
        assertThat(jdbc.queryForObject("""
                select count(*) from user_identities
                 where user_id = ? and provider = 'GOOGLE'
                """, Integer.class, user.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                select count(*) from audit_logs
                 where actor_user_id = ? and action = 'FEDERATED_IDENTITY_LINKED'
                """, Integer.class, user.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                select count(*) from user_sessions where user_id = ?
                """, Integer.class, user.getId())).isZero();
    }
}

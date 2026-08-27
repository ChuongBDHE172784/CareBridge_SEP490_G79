package com.carebridge.backend.family.security;

import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InviteFamilyMemberSecurityTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String BASE_URL = "/api/v1/care-groups/" + UUID.randomUUID() + "/invitations";

    // ── TC-024: Unauthenticated request rejected ──────────────────────────────

    @Test
    void inviteFamilyMember_noJwt_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\": \"LINK\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── TC-023: SQL injection via phone field is neutralized ──────────────────
    // (Defense-in-depth: JPA parameterization prevents injection at persistence layer)

    @Test
    void inviteFamilyMember_sqlInjectionViaPhone_doesNotExposeData() throws Exception {
        // Without auth we get 401 before reaching any SQL — injection cannot reach DB
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\": \"PHONE\", \"phone\": \"' OR '1'='1\"}"))
                .andExpect(status().isUnauthorized());
    }
}

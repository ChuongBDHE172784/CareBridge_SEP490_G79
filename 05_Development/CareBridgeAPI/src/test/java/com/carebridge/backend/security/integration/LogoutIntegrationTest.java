package com.carebridge.backend.security.integration;

import com.carebridge.backend.security.dto.request.LoginRequest;
import com.carebridge.backend.security.dto.request.RefreshTokenRequest;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.util.TokenUtils;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LOGOUT-TC-INT-001 — Integration: login → logout → refresh is rejected, and the session
 * row is revoked in the real database (Testcontainers PostgreSQL).
 *
 * <p>End-to-end HTTP flow: {@code /login-direct} issues tokens, {@code /logout} (authenticated
 * with the access token) blacklists the refresh token and revokes the session, and a
 * subsequent {@code /refresh} with the same token is rejected.
 *
 * <p>Real behavior note: the rejected refresh returns HTTP 401 with error code
 * {@code AUTHENTICATION_FAILED} (the blacklisted-token branch), which is the concrete
 * mapping of the Test-Spec's idealized {@code AUTH-021}.
 */
@Transactional
class LogoutIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String EMAIL = "int.logout@test.com";
    private static final String PASSWORD = "SecureP@ss1";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void logout_revokesSession_andRejectsSubsequentRefresh() throws Exception {
        userRepository.save(User.builder()
                .email(EMAIL)
                .role(Role.MOTHER)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .enabled(true)
                .locked(false)
                .emailVerified(true)
                .phoneVerified(false)
                .accountStatus("ACTIVE")
                .build());

        // 1. Login → capture tokens
        LoginRequest login = new LoginRequest();
        login.setEmail(EMAIL);
        login.setPassword(PASSWORD);
        String loginBody = mockMvc.perform(post("/api/v1/auth/login-direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(loginBody).get("data");
        String accessToken = data.get("accessToken").asText();
        String refreshToken = data.get("refreshToken").asText();

        // 2. Logout (authenticated with the access token) using the refresh token
        RefreshTokenRequest logoutReq = new RefreshTokenRequest();
        logoutReq.setRefreshToken(refreshToken);
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk());

        // 3. Refresh with the now-revoked token → 401 rejected
        RefreshTokenRequest refreshReq = new RefreshTokenRequest();
        refreshReq.setRefreshToken(refreshToken);
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_FAILED"));

        // DB assertion: the session row for this token is revoked
        String tokenHash = TokenUtils.hashSha256(refreshToken);
        Boolean revoked = jdbcTemplate.queryForObject(
                "SELECT revoked FROM user_sessions WHERE refresh_token_hash = ?",
                Boolean.class, tokenHash);
        assertThat(revoked).isTrue();
    }
}

package com.carebridge.backend.security.integration;

import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.security.dto.request.LoginRequest;
import com.carebridge.backend.security.entity.RefreshToken;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.RefreshTokenRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LOGIN-TC-INT-001 — Integration: login issues tokens, persists a session in the real
 * database (Testcontainers PostgreSQL), and the access-token JWT carries valid claims.
 *
 * <p>The production {@code /login} endpoint issues an OTP challenge first; the token-issuing
 * path is {@code /login-direct}. This exercises that path end-to-end against a seeded ACTIVE
 * user and asserts the canonical authentication session plus the
 * JWT subject/authorities. (Role authority is the real {@code ROLE_MOTHER}, not the Test-Spec's
 * idealized bare {@code MOTHER}.)
 */
@Transactional
class LoginIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String EMAIL = "int.login@test.com";
    private static final String PASSWORD = "SecureP@ss1";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserSessionRepository sessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User seedActiveUser() {
        return userRepository.save(User.builder()
                .email(EMAIL)
                .role(Role.MOTHER)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .enabled(true)
                .locked(false)
                .emailVerified(true)
                .phoneVerified(false)
                .accountStatus("ACTIVE")
                .build());
    }

    @Test
    void login_persistsSessionAndIssuesValidJwt() throws Exception {
        User user = seedActiveUser();

        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);

        String body = mockMvc.perform(post("/api/v1/auth/login-direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(body).get("data");
        String accessToken = data.get("accessToken").asText();

        // Canonical session facade: one active token hash (64 chars)
        List<RefreshToken> tokens = refreshTokenRepository.findByUser_IdAndRevokedFalse(user.getId());
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).isRevoked()).isFalse();
        assertThat(tokens.get(0).getTokenHash()).hasSize(64);

        // auth_sessions: one active session row, SHA-256 hex hash (64 chars)
        List<UserSession> sessions =
                sessionRepository.findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(user.getId());
        assertThat(sessions).hasSize(1);
        UserSession session = sessions.get(0);
        assertThat(session.isRevoked()).isFalse();
        assertThat(session.getRefreshTokenHash()).hasSize(64);

        // JWT claims: subject is the user id, authorities include ROLE_MOTHER
        assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
        assertThat(jwtTokenProvider.getSubject(accessToken)).isEqualTo(user.getId().toString());
        assertThat(jwtTokenProvider.getAuthorities(accessToken))
                .contains(new SimpleGrantedAuthority("ROLE_MOTHER"));
    }
}

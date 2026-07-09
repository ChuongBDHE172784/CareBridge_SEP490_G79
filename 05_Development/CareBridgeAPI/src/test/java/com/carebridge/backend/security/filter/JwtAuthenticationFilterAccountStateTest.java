package com.carebridge.backend.security.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.ContentController;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests per-request account-state enforcement in JwtAuthenticationFilter.
 *
 * Scenarios:
 *  F-AS-01  Active user with valid token   → request proceeds (200)
 *  F-AS-02  Disabled user with valid token → 403 ACCOUNT_DISABLED
 *  F-AS-03  Locked user with valid token   → 403 ACCOUNT_LOCKED
 *  F-AS-04  Deleted user (token issued)    → 401 AUTHENTICATION_FAILED
 *  F-AS-05  No Bearer token                → 401 (filter no-ops, entry point fires)
 */
@WebMvcTest(
        value = ContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class JwtAuthenticationFilterAccountStateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentService contentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID SESSION_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String FAKE_TOKEN = "fake.jwt.token";
    private static final String AUTH_HEADER = "Authorization";

    @BeforeEach
    void setupTokenProvider() {
        when(jwtTokenProvider.validateToken(FAKE_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getSubject(FAKE_TOKEN)).thenReturn(USER_ID.toString());
        when(jwtTokenProvider.getSessionId(FAKE_TOKEN)).thenReturn(SESSION_ID);
        when(jwtTokenProvider.getAuthorities(FAKE_TOKEN))
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_MOTHER")));
        when(userSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(buildSession(false, "active", Instant.now().plusSeconds(3600))));
    }

    private User buildUser(boolean enabled, boolean locked) {
        return User.builder()
                .enabled(enabled)
                .locked(locked)
                .role(Role.MOTHER)
                .phone("0900000001")
                .build();
    }

    private UserSession buildSession(boolean revoked, String status, Instant expiresAt) {
        return UserSession.builder()
                .sessionId(SESSION_ID)
                .userId(USER_ID)
                .refreshTokenHash("refresh-token-hash")
                .revoked(revoked)
                .status(status)
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .build();
    }

    // F-AS-01: active user proceeds normally (filter passes, controller is reached)
    @Test
    void activeUser_withValidToken_shouldPassThrough() throws Exception {
        when(contentService.getContents(any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(buildUser(true, false)));

        mockMvc.perform(get("/api/v1/content").header(AUTH_HEADER, "Bearer " + FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    // F-AS-02: disabled user gets 403 ACCOUNT_DISABLED regardless of token validity
    @Test
    void disabledUser_withValidToken_shouldReturn403AccountDisabled() throws Exception {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(buildUser(false, false)));

        mockMvc.perform(get("/api/v1/content").header(AUTH_HEADER, "Bearer " + FAKE_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCOUNT_DISABLED"))
                .andExpect(jsonPath("$.status").value(403));
    }

    // F-AS-03: locked user gets 403 ACCOUNT_LOCKED
    @Test
    void lockedUser_withValidToken_shouldReturn403AccountLocked() throws Exception {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(buildUser(true, true)));

        mockMvc.perform(get("/api/v1/content").header(AUTH_HEADER, "Bearer " + FAKE_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.status").value(403));
    }

    // F-AS-04: user deleted after token issued → 401 (user not in DB)
    @Test
    void deletedUser_withValidToken_shouldReturn401() throws Exception {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/content").header(AUTH_HEADER, "Bearer " + FAKE_TOKEN))
                .andExpect(status().isUnauthorized());
    }

    // F-AS-05: no token → 401 via Spring Security entry point (filter never touches repo)
    @Test
    void noToken_shouldReturn401WithoutCallingRepository() throws Exception {
        mockMvc.perform(get("/api/v1/content"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokedSession_withValidToken_shouldReturn401SessionRevoked() throws Exception {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(buildUser(true, false)));
        when(userSessionRepository.findById(SESSION_ID))
                .thenReturn(Optional.of(buildSession(
                        true, "revoked", Instant.now().plusSeconds(3600))));

        mockMvc.perform(get("/api/v1/content").header(AUTH_HEADER, "Bearer " + FAKE_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("SESSION_REVOKED"))
                .andExpect(jsonPath("$.status").value(401));
    }
}

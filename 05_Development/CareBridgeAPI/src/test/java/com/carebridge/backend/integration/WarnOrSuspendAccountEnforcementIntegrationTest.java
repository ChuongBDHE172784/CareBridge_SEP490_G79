package com.carebridge.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ContentController;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * WSA-TC-INT-004, WSA-TC-INT-005 — ADR-003 enforcement, exercised through the REAL
 * JwtAuthenticationFilter + SecurityFilterChain (not a unit-mocked filter), following the same
 * pattern already established by JwtAuthenticationFilterAccountStateTest for ACCOUNT_DISABLED/
 * ACCOUNT_LOCKED. This codebase has no Testcontainers/real-DB harness (verified, same finding as
 * UC-100/101), so UserRepository is mocked at the bean level — but the filter code under test
 * (doFilterInternal) runs for real, which is what proves the enforcement is not a "ghost action".
 */
@WebMvcTest(
        value = ContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class WarnOrSuspendAccountEnforcementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentService contentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID SUSPENDED_USER_ID = UUID.fromString("bb000000-0000-0000-0000-000000000002");
    private static final UUID LAPSED_USER_ID = UUID.fromString("bb000000-0000-0000-0000-000000000003");
    private static final String FAKE_TOKEN = "fake.jwt.token";

    @BeforeEach
    void setUp() {
        when(jwtTokenProvider.validateToken(FAKE_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getAuthorities(FAKE_TOKEN))
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_MOTHER")));
    }

    private User buildUser(UUID id, Instant suspendedUntil) {
        return User.builder()
                .id(id)
                .enabled(true)
                .locked(false)
                .role(Role.MOTHER)
                .suspendedUntil(suspendedUntil)
                .build();
    }

    // WSA-TC-INT-004 (CRITICAL — ghost-action gate): pre-issued JWT of a just-suspended user is
    // rejected on its very next request.
    @Test
    void suspendedUser_withPreIssuedToken_shouldReturn403AccountSuspended() throws Exception {
        when(jwtTokenProvider.getSubject(FAKE_TOKEN)).thenReturn(SUSPENDED_USER_ID.toString());
        when(userRepository.findById(SUSPENDED_USER_ID))
                .thenReturn(Optional.of(buildUser(SUSPENDED_USER_ID, Instant.now().plus(14, ChronoUnit.DAYS))));

        mockMvc.perform(get("/api/v1/content").header("Authorization", "Bearer " + FAKE_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCOUNT_SUSPENDED"));
    }

    // WSA-TC-INT-005: a lapsed (past) suspension does not block, and is not written back to null.
    @Test
    void lapsedSuspension_shouldAllowRequestThroughWithoutWriteBack() throws Exception {
        when(jwtTokenProvider.getSubject(FAKE_TOKEN)).thenReturn(LAPSED_USER_ID.toString());
        when(userRepository.findById(LAPSED_USER_ID))
                .thenReturn(Optional.of(buildUser(LAPSED_USER_ID, Instant.now().minus(1, ChronoUnit.DAYS))));
        when(contentService.getContents(any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/v1/content").header("Authorization", "Bearer " + FAKE_TOKEN))
                .andExpect(status().isOk());

        verify(userRepository, never()).save(any());
    }
}

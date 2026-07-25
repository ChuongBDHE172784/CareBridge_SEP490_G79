package com.carebridge.backend.integration.gemini;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.integration.gemini.controller.RagController;
import com.carebridge.backend.integration.gemini.service.FallbackRagServiceImpl;
import com.carebridge.backend.integration.gemini.service.RagPolicyServiceImpl;
import com.carebridge.backend.integration.gemini.filter.RagSafetyFilter;
import com.carebridge.backend.integration.gemini.dto.RagSafetyResult;
import com.carebridge.backend.content.policy.LifecycleContentStageResolver;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proves RagController wires and serves requests via FallbackRagServiceImpl
 * when no Gemini-profile bean is present — i.e. Gemini key absent / default profile.
 *
 * RAG-B2D-01  Spring context loads, fallback=true returned
 * RAG-B2D-02  Unauthenticated → 401
 * RAG-B2D-03  PARTNER role → 403
 * RAG-B2D-04  Disabled account → 403 ACCOUNT_DISABLED
 * RAG-B2D-05  Locked account → 403 ACCOUNT_LOCKED
 */
@WebMvcTest(
        value = RagController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, FallbackRagServiceImpl.class, RagPolicyServiceImpl.class,
        MockMvcSecurityBuilderConfig.class})
class RagNoGeminiStartupTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RagSafetyFilter ragSafetyFilter;

    @MockitoBean
    private LifecycleContentStageResolver lifecycleContentStageResolver;

    private static final String VALID_QUERY =
            "{\"query\": \"Phù chân khi mang thai 28 tuần\", \"maxContextChunks\": 3}";

    // RAG-B2D-01: context loads; FallbackRagServiceImpl returns fallback=true
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void contextLoads_withNoGeminiKey_returnsFallbackResponse() throws Exception {
        when(ragSafetyFilter.check(anyString())).thenReturn(RagSafetyResult.safe());
        when(lifecycleContentStageResolver.resolve(any())).thenReturn(ContentStage.PREGNANCY);
        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_QUERY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fallback").value(true))
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty());
    }

    // RAG-B2D-02: unauthenticated → 401
    @Test
    void noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_QUERY))
                .andExpect(status().isUnauthorized());
    }

    // RAG-B2D-03: PARTNER role is excluded from the RAG endpoint → 403
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "PARTNER")
    void partnerRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/rag/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_QUERY))
                .andExpect(status().isForbidden());
    }

    // RAG-B2D-04: valid JWT but account is disabled → 403 ACCOUNT_DISABLED
    @Test
    void disabledAccount_returns403AccountDisabled() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        User disabledUser = User.builder()
                .enabled(false).locked(false).role(Role.MOTHER).phone("0900000003").build();

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getSubject(anyString())).thenReturn(userId.toString());
        when(jwtTokenProvider.getAuthorities(anyString()))
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_MOTHER")));
        when(userRepository.findById(userId)).thenReturn(Optional.of(disabledUser));

        mockMvc.perform(post("/api/v1/rag/answer")
                        .header("Authorization", "Bearer fake.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_QUERY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCOUNT_DISABLED"));
    }

    // RAG-B2D-05: valid JWT but account is locked → 403 ACCOUNT_LOCKED
    @Test
    void lockedAccount_returns403AccountLocked() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        User lockedUser = User.builder()
                .enabled(true).locked(true).role(Role.MOTHER).phone("0900000004").build();

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getSubject(anyString())).thenReturn(userId.toString());
        when(jwtTokenProvider.getAuthorities(anyString()))
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_MOTHER")));
        when(userRepository.findById(userId)).thenReturn(Optional.of(lockedUser));

        mockMvc.perform(post("/api/v1/rag/answer")
                        .header("Authorization", "Bearer fake.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_QUERY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCOUNT_LOCKED"));
    }
}

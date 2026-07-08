package com.carebridge.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.CommunityDashboardController;
import com.carebridge.backend.content.service.CommunityDashboardService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// DASH-TC-111, DASH-TC-112, DASH-TC-113
@WebMvcTest(
        value = CommunityDashboardController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class CommunityDashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommunityDashboardService communityDashboardService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String URL = "/api/v1/admin/community/dashboard";

    // DASH-TC-111: non-SYSTEM_ADMIN (MOTHER) -> 403. Body is empty — URL-matcher denial (SecurityConfig's
    // explicit `.requestMatchers(GET, ".../dashboard").hasRole("SYSTEM_ADMIN")`) precedes DispatcherServlet,
    // same verified finding as UC-100/101/102/106/107/UC-111's own ContentApprovalControllerSecurityTest.
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void getDashboard_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden());

        verify(communityDashboardService, never()).getDashboard(any());
    }

    // DASH-TC-112: MODERATOR -> 403 (no implicit RoleHierarchy) — see DASH-TC-111 note on empty body
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000bb", roles = "MODERATOR")
    void getDashboard_asModeratorRole_shouldReturn403() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden());

        verify(communityDashboardService, never()).getDashboard(any());
    }

    // DASH-TC-113: no JWT -> 401 bodiless
    @Test
    void getDashboard_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }
}

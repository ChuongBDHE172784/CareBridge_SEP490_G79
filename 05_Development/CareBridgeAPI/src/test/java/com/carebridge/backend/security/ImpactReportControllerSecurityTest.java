package com.carebridge.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ImpactReportController;
import com.carebridge.backend.content.service.ImpactReportService;
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

// IMP-TC-110, IMP-TC-111, IMP-TC-112
@WebMvcTest(
        value = ImpactReportController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ImpactReportControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImpactReportService impactReportService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String URL = "/api/v1/admin/impact-report";

    // IMP-TC-110: PARTNER -> 403. Body empty — URL-matcher denial precedes DispatcherServlet
    // (same verified finding as UC-100/101/102/106/107/111).
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000cc", roles = "PARTNER")
    void getImpactReport_asPartnerRole_shouldReturn403() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden());

        verify(impactReportService, never()).getImpactReport(any());
    }

    // IMP-TC-111: MODERATOR -> 403 (no implicit RoleHierarchy)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000bb", roles = "MODERATOR")
    void getImpactReport_asModeratorRole_shouldReturn403() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden());

        verify(impactReportService, never()).getImpactReport(any());
    }

    // IMP-TC-112: no JWT -> 401 bodiless
    @Test
    void getImpactReport_withoutAuthentication_shouldReturn401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }
}

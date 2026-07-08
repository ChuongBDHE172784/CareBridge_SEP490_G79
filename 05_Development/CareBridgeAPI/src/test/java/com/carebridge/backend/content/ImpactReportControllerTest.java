package com.carebridge.backend.content;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ImpactReportController;
import com.carebridge.backend.content.exception.ModerationException;
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

// IMP-TC-107
@WebMvcTest(
        value = ImpactReportController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ImpactReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImpactReportService impactReportService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String URL = "/api/v1/admin/impact-report";

    // IMP-TC-107: invalid range (from > to) -> 400 MOD-022
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000aa", roles = "SYSTEM_ADMIN")
    void getImpactReport_invalidRange_returns400Mod022() throws Exception {
        org.mockito.Mockito.when(impactReportService.getImpactReport(org.mockito.ArgumentMatchers.any()))
                .thenThrow(ModerationException.invalidImpactReportDateRange());

        mockMvc.perform(get(URL).param("from", "2026-06-30").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MOD-022"));
    }
}

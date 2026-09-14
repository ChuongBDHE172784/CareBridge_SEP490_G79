package com.carebridge.backend.carejourney.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.carejourney.dto.GrowthChartResponse;
import com.carebridge.backend.carejourney.service.IGrowthService;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** SBG-TC-005 — role matrix for the growth endpoints (TDS §9.1, §16). */
@WebMvcTest(
        value = {GrowthChartController.class, GrowthMeasurementController.class},
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class GrowthChartControllerSecurityTest {

    private static final UUID BABY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EXPERT_USER = "00000000-0000-0000-0000-0000000000e1";
    private static final String CHART_URL = "/api/v1/babies/" + BABY_ID + "/growth-chart";
    private static final String HISTORY_URL = "/api/v1/babies/" + BABY_ID + "/growth-measurements";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IGrowthService growthService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void stubChart() {
        when(growthService.getGrowthChart(any(), eq(BABY_ID))).thenReturn(
                GrowthChartResponse.builder().babyId(BABY_ID).nickname("Bé Test").measurements(List.of()).build());
    }

    @Test
    void growthChart_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get(CHART_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = EXPERT_USER, roles = "EXPERT")
    void growthChart_asExpert_reachesService() throws Exception {
        mockMvc.perform(get(CHART_URL)).andExpect(status().isOk());

        verify(growthService).getGrowthChart(UUID.fromString(EXPERT_USER), BABY_ID);
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000038", roles = "MOTHER")
    void growthChart_asMother_reachesService() throws Exception {
        mockMvc.perform(get(CHART_URL)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000bb", roles = "MODERATOR")
    void growthChart_asModerator_returns403() throws Exception {
        mockMvc.perform(get(CHART_URL)).andExpect(status().isForbidden());

        verify(growthService, never()).getGrowthChart(any(), any());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000aa", roles = "SYSTEM_ADMIN")
    void growthChart_asSystemAdmin_returns403() throws Exception {
        mockMvc.perform(get(CHART_URL)).andExpect(status().isForbidden());

        verify(growthService, never()).getGrowthChart(any(), any());
    }

    @Test
    @WithMockUser(username = EXPERT_USER, roles = "EXPERT")
    void growthHistory_asExpert_returns403() throws Exception {
        mockMvc.perform(get(HISTORY_URL)).andExpect(status().isForbidden());

        verify(growthService, never()).getGrowthMeasurementHistory(any(), any(), any());
    }
}

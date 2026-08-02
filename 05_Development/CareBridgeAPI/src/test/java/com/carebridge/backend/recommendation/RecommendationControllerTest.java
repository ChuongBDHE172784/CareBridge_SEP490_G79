package com.carebridge.backend.recommendation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.recommendation.controller.RecommendationController;
import com.carebridge.backend.recommendation.dto.RecommendationContentResponse;
import com.carebridge.backend.recommendation.dto.RecommendationEnums.CoverageStatus;
import com.carebridge.backend.recommendation.dto.RecommendationEnums.SelectionMode;
import com.carebridge.backend.recommendation.dto.RecommendationEnums.WeekEligibilityMode;
import com.carebridge.backend.recommendation.dto.RecommendationProfileResponse;
import com.carebridge.backend.recommendation.entity.RecommendationProfileStatus;
import com.carebridge.backend.recommendation.service.RecommendationService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = RecommendationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class RecommendationControllerTest {

    private static final String USER = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = USER, roles = "MOTHER")
    void motherCanReadProfileAndContent() throws Exception {
        when(recommendationService.getProfile(UUID.fromString(USER))).thenReturn(
                new RecommendationProfileResponse(
                        RecommendationProfileStatus.NOT_STARTED, true, false, 0, 0, null,
                        new RecommendationProfileResponse.ConsentSummary("NONE", null, null, null, null),
                        null, null));
        when(recommendationService.getContent(UUID.fromString(USER), 3)).thenReturn(
                new RecommendationContentResponse(
                        "PRE_PREGNANCY", null, WeekEligibilityMode.NOT_APPLICABLE,
                        RecommendationProfileStatus.NOT_STARTED, SelectionMode.EMPTY,
                        CoverageStatus.EMPTY, false, List.of()));

        mockMvc.perform(get("/api/v1/recommendations/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiresAction").value(true));
        mockMvc.perform(get("/api/v1/recommendations/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectionMode").value("EMPTY"));
    }

    @Test
    @WithMockUser(username = USER, roles = "MOTHER")
    void nonNumericLimitIsRejectedWithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/content?limit=not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("RECOMMENDATION_LIMIT_INVALID"));

        verify(recommendationService, never()).getContent(any(UUID.class), anyInt());
    }

    @Test
    @WithMockUser(username = USER, roles = "FAMILY")
    void nonMotherCannotAccessRecommendationEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/profile"))
                .andExpect(status().isForbidden());
        verify(recommendationService, never()).getProfile(any(UUID.class));
    }
}

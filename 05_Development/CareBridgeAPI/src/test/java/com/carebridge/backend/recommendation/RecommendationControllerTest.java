package com.carebridge.backend.recommendation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.common.config.JacksonConfig;
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
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = RecommendationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({JacksonConfig.class, SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
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
        when(recommendationService.getContent(UUID.fromString(USER), null, 3)).thenReturn(
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
    @WithMockUser(username = USER, roles = "FAMILY")
    void familyMemberCanReadContentWithCareGroupId() throws Exception {
        UUID group = UUID.fromString("00000000-0000-0000-0000-000000000002");
        when(recommendationService.getContent(UUID.fromString(USER), group, 3)).thenReturn(
                new RecommendationContentResponse(
                        "PREGNANCY", 12, WeekEligibilityMode.BOUNDED_AND_STAGE_WIDE,
                        RecommendationProfileStatus.NOT_STARTED, SelectionMode.EMPTY,
                        CoverageStatus.EMPTY, false, List.of()));

        mockMvc.perform(get("/api/v1/recommendations/content?careGroupId=" + group))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("PREGNANCY"))
                .andExpect(jsonPath("$.data.pregnancyWeek").value(12));
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
    @WithMockUser(username = USER, roles = "MOTHER")
    void motherCanSubmitJsonProfile() throws Exception {
        when(recommendationService.putProfile(eq(UUID.fromString(USER)), any(JsonNode.class))).thenReturn(
                new RecommendationProfileResponse(
                        RecommendationProfileStatus.ACTIVE, false, true, 1, 1, null,
                        new RecommendationProfileResponse.ConsentSummary("GRANTED", null, null, null, null),
                        null, null));

        mockMvc.perform(put("/api/v1/recommendations/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissionId":"00000000-0000-0000-0000-000000000002",
                                  "schemaVersion":1,
                                  "policyVersion":"MOTHER_PERSONALIZED_CONTENT_V1",
                                  "consentAccepted":true,
                                  "profile":{}
                                }
                                """))
                .andExpect(status().isOk());

        verify(recommendationService).putProfile(eq(UUID.fromString(USER)), any(JsonNode.class));
    }

    @Test
    @WithMockUser(username = USER, roles = "FAMILY")
    void nonMotherCannotAccessRecommendationEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/profile"))
                .andExpect(status().isForbidden());
        verify(recommendationService, never()).getProfile(any(UUID.class));
    }
}

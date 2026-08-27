package com.carebridge.backend.exercise;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.exercise.controller.ExerciseSessionController;
import com.carebridge.backend.exercise.dto.PostureFeedbackResponse;
import com.carebridge.backend.exercise.service.IExerciseSessionHistoryService;
import com.carebridge.backend.exercise.service.IExerciseSessionResultService;
import com.carebridge.backend.exercise.service.IExerciseSessionService;
import com.carebridge.backend.exercise.service.IPostureAnalysisService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// EX-TC-030-008 — UC30 posture-events endpoint security
@WebMvcTest(
        value = ExerciseSessionController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ExerciseSessionPostureEventsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IExerciseSessionService sessionService;

    @MockitoBean
    private IExerciseSessionResultService sessionResultService;

    @MockitoBean
    private IExerciseSessionHistoryService sessionHistoryService;

    @MockitoBean
    private IPostureAnalysisService postureAnalysisService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final String BODY =
            "{\"eventTimeMs\":1000,\"keypointSummaryJson\":{\"backAngle\":5.0}}";

    @Test
    @DisplayName("EX-TC-030-008: no JWT on posture-events returns 401")
    void analyzePosture_noJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/exercises/sessions/" + SESSION_ID + "/posture-events")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "EXPERT")
    @DisplayName("EX-TC-030-SEC: non-MOTHER role (EXPERT) on posture-events returns 403")
    void analyzePosture_expertRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/exercises/sessions/" + SESSION_ID + "/posture-events")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    @DisplayName("EX-TC-030: MOTHER role on posture-events succeeds (positive control)")
    void analyzePosture_motherRole_returns200() throws Exception {
        when(postureAnalysisService.analyzePosture(any(), any(), any()))
                .thenReturn(ApiResponse.success(PostureFeedbackResponse.builder()
                        .postureCode("GOOD_FORM")
                        .severity("INFO")
                        .build()));

        mockMvc.perform(post("/api/v1/exercises/sessions/" + SESSION_ID + "/posture-events")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
    }
}

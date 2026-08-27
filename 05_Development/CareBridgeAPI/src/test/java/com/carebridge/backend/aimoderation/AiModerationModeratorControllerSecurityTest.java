package com.carebridge.backend.aimoderation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.aimoderation.controller.AiModerationModeratorController;
import com.carebridge.backend.aimoderation.dto.response.AiAssessmentResponse;
import com.carebridge.backend.aimoderation.dto.response.AiFeedbackResponse;
import com.carebridge.backend.aimoderation.entity.AiFeedbackVerdict;
import com.carebridge.backend.aimoderation.service.AiAssessmentModeratorService;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Scenario 19: MODERATOR-only access to assessment evidence + feedback. */
@WebMvcTest(
        value = AiModerationModeratorController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class AiModerationModeratorControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiAssessmentModeratorService moderatorService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String MODERATOR_UUID = "22222222-2222-2222-2222-222222222222";
    private static final UUID REPORT_ID = UUID.randomUUID();
    private static final UUID ASSESSMENT_ID = UUID.randomUUID();

    @Test
    @WithMockUser(username = MODERATOR_UUID, roles = "MODERATOR")
    void getAssessment_asModerator_returns200() throws Exception {
        when(moderatorService.getAssessmentForReport(any(), any()))
                .thenReturn(new AiAssessmentResponse(ASSESSMENT_ID, null, null, null, null, null, null,
                        null, null, "GEMINI", "gemini-1.5-flash", "hash", null, null, Instant.now(), null,
                        java.util.List.of(), null, null));
        mockMvc.perform(get("/api/v1/admin/moderation/reports/{id}/assessment", REPORT_ID))
                .andExpect(status().isOk());
    }

    // Ordinary users can never read moderation evidence
    @Test
    @WithMockUser(username = MODERATOR_UUID, roles = "MOTHER")
    void getAssessment_asMother_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/moderation/reports/{id}/assessment", REPORT_ID))
                .andExpect(status().isForbidden());
    }

    // The moderation split is disjoint: SYSTEM_ADMIN does not process cases
    @Test
    @WithMockUser(username = MODERATOR_UUID, roles = "SYSTEM_ADMIN")
    void getAssessment_asSystemAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/moderation/reports/{id}/assessment", REPORT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = MODERATOR_UUID, roles = "MODERATOR")
    void submitFeedback_asModerator_returns201() throws Exception {
        when(moderatorService.submitFeedback(any(), any(), any()))
                .thenReturn(new AiFeedbackResponse(UUID.randomUUID(), ASSESSMENT_ID,
                        AiFeedbackVerdict.DISAGREE, "không đồng ý", Instant.now()));
        mockMvc.perform(post("/api/v1/admin/moderation/assessments/{id}/feedback", ASSESSMENT_ID)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"verdict\":\"DISAGREE\",\"note\":\"không đồng ý\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void submitFeedback_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/moderation/assessments/{id}/feedback", ASSESSMENT_ID)
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"verdict\":\"AGREE\"}"))
                .andExpect(status().isUnauthorized());
    }
}

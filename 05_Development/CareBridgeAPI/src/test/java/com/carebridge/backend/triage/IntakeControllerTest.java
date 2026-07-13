package com.carebridge.backend.triage;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.triage.controller.IntakeController;
import com.carebridge.backend.triage.engine.TriageGraphService;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.ITriageService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = IntakeController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class IntakeControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ITriageService triageService;
    @MockitoBean private ChildTriageAiClient childTriageAiClient;
    @MockitoBean private TriageGraphService triageGraphService;
    @MockitoBean private ObjectMapper objectMapper;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/triage/intake";

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "MOTHER")
    void runIntake_blankSymptoms_shouldReturn400() throws Exception {
        // TRIAGE-TC-002
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "MOTHER")
    void runIntake_symptomsTooLong_shouldReturn400() throws Exception {
        // TRIAGE-TC-003
        String tooLong = "a".repeat(2001);
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void runIntake_noJwt_shouldReturn401() throws Exception {
        // TRIAGE-TC-005
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000020", roles = "PARTNER")
    void runIntake_wrongRole_shouldReturn403() throws Exception {
        // TRIAGE-TC-006
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"test symptoms\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "MOTHER")
    void startConversation_initialTextTooLong_shouldReturn400() throws Exception {
        String tooLong = "a".repeat(2001);
        mockMvc.perform(post(BASE_URL + "/conversation/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialText\":\"" + tooLong + "\",\"currentIntake\":{}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "MOTHER")
    void continueConversation_invalidSessionId_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/conversation/continue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"intakeSessionId\":\"client-id\",\"newAnswers\":{}}"))
                .andExpect(status().isBadRequest());
    }
}

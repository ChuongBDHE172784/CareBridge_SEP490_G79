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
import com.carebridge.backend.triage.service.ITriageContinuationService;
import com.carebridge.backend.triage.dto.request.StartIntakeConversationRequest;
import com.carebridge.backend.triage.dto.response.IntakeConversationResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

@WebMvcTest(
        value = IntakeController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class IntakeControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ITriageService triageService;
    @MockitoBean private ITriageContinuationService continuationService;
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
    void startConversation_postpartumJson_shouldBindTypedMaternalStage() throws Exception {
        when(triageService.startConversation(any(), any())).thenReturn(
                IntakeConversationResponse.builder()
                        .status("ASK_MORE")
                        .intakeSessionId("00000000-0000-0000-0000-000000000064")
                        .stage("POSTPARTUM")
                        .round(1)
                        .build());

        mockMvc.perform(post(BASE_URL + "/conversation/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "initialText": "Tôi cần hỗ trợ sau sinh",
                                  "stage": "POSTPARTUM",
                                  "motherProfileId": "00000000-0000-0000-0000-000000000099",
                                  "currentIntake": {"stage": "POSTPARTUM"}
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<StartIntakeConversationRequest> captor =
                ArgumentCaptor.forClass(StartIntakeConversationRequest.class);
        verify(triageService).startConversation(
                captor.capture(),
                eq(UUID.fromString("00000000-0000-0000-0000-000000000010")));
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getStage())
                .isEqualTo(TriageStage.POSTPARTUM);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getMotherProfileId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000099"));
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getBabyProfileId()).isNull();
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

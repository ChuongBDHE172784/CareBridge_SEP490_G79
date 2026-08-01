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
import com.carebridge.backend.triage.dto.response.IntakeSessionResponse;
import com.carebridge.backend.triage.exception.TriageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
    private static final String FAMILY = "00000000-0000-0000-0000-000000000012";

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
    @WithMockUser(username = FAMILY, roles = "FAMILY")
    void runIntake_familyRole_shouldUseAuthenticatedFamilyAsOwner() throws Exception {
        UUID familyId = UUID.fromString(FAMILY);
        when(triageService.runIntake(any(), eq(familyId))).thenReturn(
                IntakeSessionResponse.builder()
                        .sessionId(UUID.fromString("00000000-0000-0000-0000-000000000061"))
                        .stage("PREGNANCY")
                        .status("COMPLETED")
                        .riskLevel("YELLOW")
                        .build());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptoms\":\"Người thân đang chóng mặt\",\"stage\":\"PREGNANCY\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.stage").value("PREGNANCY"));

        verify(triageService).runIntake(any(), eq(familyId));
    }

    @Test
    @WithMockUser(username = FAMILY, roles = "FAMILY")
    void conversation_familyRole_shouldStartAndContinueOwnSession() throws Exception {
        UUID familyId = UUID.fromString(FAMILY);
        UUID sessionId = UUID.fromString("00000000-0000-4000-8000-000000000062");
        when(triageService.startConversation(any(), eq(familyId))).thenReturn(
                IntakeConversationResponse.builder()
                        .status("ASK_MORE")
                        .intakeSessionId(sessionId.toString())
                        .stage("INFANT")
                        .round(1)
                        .build());
        when(triageService.continueConversation(any(), eq(familyId))).thenReturn(
                IntakeConversationResponse.builder()
                        .status("TRIAGE_COMPLETE")
                        .intakeSessionId(sessionId.toString())
                        .stage("INFANT")
                        .round(2)
                        .build());

        mockMvc.perform(post(BASE_URL + "/conversation/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialText\":\"Bé sốt\",\"stage\":\"INFANT\",\"currentIntake\":{\"stage\":\"INFANT\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intakeSessionId").value(sessionId.toString()));

        mockMvc.perform(post(BASE_URL + "/conversation/continue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"intakeSessionId\":\"" + sessionId + "\",\"newAnswers\":{\"temperatureC\":39.0},\"round\":1}"))
                .andExpect(status().isOk());

        verify(triageService).startConversation(any(), eq(familyId));
        verify(triageService).continueConversation(any(), eq(familyId));
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
    void ov01E2e008_startConversationReturnsTypedMaternalOrigin() throws Exception {
        UUID journeyId = UUID.fromString("00000000-0000-0000-0000-000000000098");
        UUID continuationToken = UUID.fromString("00000000-0000-0000-0000-000000000097");
        when(triageService.startConversation(any(), any())).thenReturn(
                IntakeConversationResponse.builder()
                        .status("ASK_MORE")
                        .intakeSessionId("00000000-0000-0000-0000-000000000064")
                        .stage("POSTPARTUM")
                        .journeyId(journeyId)
                        .originDashboard(OriginDashboard.MOTHER_JOURNEY)
                        .originReferenceId(journeyId)
                        .originAction(OriginAction.RETURN_TO_MOTHER_JOURNEY)
                        .continuationToken(continuationToken)
                        .round(1)
                        .build());

        mockMvc.perform(post(BASE_URL + "/conversation/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "initialText": "Tôi cần hỗ trợ sau sinh",
                                  "stage": "POSTPARTUM",
                                  "journeyId": "00000000-0000-0000-0000-000000000098",
                                  "originDashboard": "MOTHER_JOURNEY",
                                  "originReferenceId": "00000000-0000-0000-0000-000000000098",
                                  "motherProfileId": "00000000-0000-0000-0000-000000000099",
                                  "currentIntake": {"stage": "POSTPARTUM"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.journeyId").value(journeyId.toString()))
                .andExpect(jsonPath("$.data.originDashboard").value("MOTHER_JOURNEY"))
                .andExpect(jsonPath("$.data.originReferenceId").value(journeyId.toString()))
                .andExpect(jsonPath("$.data.originAction").value("RETURN_TO_MOTHER_JOURNEY"))
                .andExpect(jsonPath("$.data.continuationToken")
                        .value(continuationToken.toString()));

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
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getJourneyId())
                .isEqualTo(journeyId);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getOriginDashboard())
                .isEqualTo(OriginDashboard.MOTHER_JOURNEY);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getOriginReferenceId())
                .isEqualTo(journeyId);
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000011", roles = "MOTHER")
    void ov01E2e015_crossAccountContinuationUsesAuthenticatedOwnerAndReturnsNeutral404()
            throws Exception {
        UUID accountB = UUID.fromString("00000000-0000-0000-0000-000000000011");
        String accountAToken = "00000000-0000-0000-0000-000000000096";
        when(continuationService.resolve(accountB, accountAToken))
                .thenThrow(new TriageException(
                        HttpStatus.NOT_FOUND, "TRIAGE-014", "Continuation not found"));
        doThrow(new TriageException(
                HttpStatus.NOT_FOUND, "TRIAGE-014", "Continuation not found"))
                .when(continuationService).acknowledge(accountB, accountAToken);

        String body = "{\"token\":\"" + accountAToken + "\"}";
        mockMvc.perform(post(BASE_URL + "/continuations/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TRIAGE-014"));
        mockMvc.perform(post(BASE_URL + "/continuations/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TRIAGE-014"));

        verify(continuationService).resolve(accountB, accountAToken);
        verify(continuationService).acknowledge(accountB, accountAToken);
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

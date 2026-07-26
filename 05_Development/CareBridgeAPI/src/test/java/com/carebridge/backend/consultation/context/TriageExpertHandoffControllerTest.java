package com.carebridge.backend.consultation.context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.consultation.context.controller.TriageExpertHandoffController;
import com.carebridge.backend.consultation.context.dto.HandoffContextResponse;
import com.carebridge.backend.consultation.context.dto.HandoffCreateResponse;
import com.carebridge.backend.consultation.context.dto.HandoffParticipantResponse;
import com.carebridge.backend.consultation.context.dto.HandoffPreviewResponse;
import com.carebridge.backend.consultation.context.policy.TriageExpertHandoffPolicy;
import com.carebridge.backend.consultation.context.service.ITriageExpertHandoffService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TriageExpertHandoffControllerTest {

    private static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID INTAKE_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final Instant SHARED_AT = Instant.parse("2026-07-23T00:00:00Z");

    @Mock private ITriageExpertHandoffService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TriageExpertHandoffController(service))
                .build();
    }

    @Test
    void previewReturnsExactPublicEnvelope() throws Exception {
        when(service.preview(INTAKE_ID, MOTHER_ID)).thenReturn(new HandoffPreviewResponse(
                INTAKE_ID,
                TriageExpertHandoffPolicy.POLICY_VERSION,
                "YELLOW",
                "POSTPARTUM",
                "Safe summary",
                List.of(),
                new TriageExpertHandoffPolicy().sharedFields(),
                new TriageExpertHandoffPolicy().excludedFields()));

        mockMvc.perform(get(
                                "/api/v1/triage/intake/{intakeSessionId}/expert-handoff-preview",
                                INTAKE_ID)
                        .principal(() -> MOTHER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intakeSessionId").value(INTAKE_ID.toString()))
                .andExpect(jsonPath("$.data.riskLevel").value("YELLOW"))
                .andExpect(jsonPath("$.data.riskSummary").value("Safe summary"))
                .andExpect(jsonPath("$.data.ownerUserId").doesNotExist())
                .andExpect(jsonPath("$.data.journeyId").doesNotExist())
                .andExpect(jsonPath("$.data.continuationToken").doesNotExist());
    }

    @Test
    void createUses201ForNewAnd200ForReplay() throws Exception {
        HandoffContextResponse context = new HandoffContextResponse(
                "YELLOW", "POSTPARTUM", "Safe summary", List.of());
        when(service.create(eq(INTAKE_ID), any(), eq(MOTHER_ID)))
                .thenReturn(
                        new HandoffCreateResponse(
                                REQUEST_ID, "PENDING", false, SHARED_AT, context),
                        new HandoffCreateResponse(
                                REQUEST_ID, "PENDING", true, SHARED_AT, context));

        mockMvc.perform(post(
                                "/api/v1/triage/intake/{intakeSessionId}/expert-handoffs",
                                INTAKE_ID)
                        .principal(() -> MOTHER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.consultationRequestId").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data.replayed").value(false))
                .andExpect(jsonPath("$.data.context.riskLevel").value("YELLOW"))
                .andExpect(jsonPath("$.data.context.recommendedAction").doesNotExist())
                .andExpect(jsonPath("$.data.context.intakeSessionId").doesNotExist());

        mockMvc.perform(post(
                                "/api/v1/triage/intake/{intakeSessionId}/expert-handoffs",
                                INTAKE_ID)
                        .principal(() -> MOTHER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.replayed").value(true));
    }

    @Test
    void participantReadOmitsReplayAndInternalIdentifiers() throws Exception {
        when(service.read(REQUEST_ID, MOTHER_ID)).thenReturn(new HandoffParticipantResponse(
                REQUEST_ID,
                "PENDING",
                SHARED_AT,
                new HandoffContextResponse(
                        "YELLOW", "POSTPARTUM", "Safe summary", List.of())));

        mockMvc.perform(get(
                                "/api/v1/consultation-requests/{requestId}/triage-context",
                                REQUEST_ID)
                        .principal(() -> MOTHER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.consultationRequestId").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data.replayed").doesNotExist())
                .andExpect(jsonPath("$.data.context.ownerUserId").doesNotExist())
                .andExpect(jsonPath("$.data.context.originDashboard").doesNotExist());
    }

    private static String validCreateBody() {
        return """
                {
                  "clientRequestId":"00000000-0000-0000-0000-000000000301",
                  "expertProfileId":"00000000-0000-0000-0000-000000000401",
                  "consentAccepted":true,
                  "consentPolicyVersion":"YELLOW_EXPERT_CONTEXT_V1"
                }
                """;
    }
}

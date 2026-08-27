package com.carebridge.backend.consultation.context;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.carebridge.backend.common.exception.GlobalExceptionHandler;
import com.carebridge.backend.consultation.context.controller.TriageExpertHandoffController;
import com.carebridge.backend.consultation.context.dto.HandoffContextResponse;
import com.carebridge.backend.consultation.context.dto.HandoffCreateResponse;
import com.carebridge.backend.consultation.context.dto.HandoffParticipantResponse;
import com.carebridge.backend.consultation.context.dto.HandoffPreviewResponse;
import com.carebridge.backend.consultation.context.exception.TriageExpertHandoffException;
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

/**
 * Story 6.8 HTTP acceptance contract tests, retained from the accepted RED gate.
 *
 * <p>The original test used raw absent routes and proved the 404 RED signature. It is now bound
 * to the feature controller while preserving the same raw HTTP and JSON assertions.
 */
@ExtendWith(MockitoExtension.class)
class TriageExpertHandoffHttpContractRedTest {

    private static final String MOTHER_ID = "00000000-0000-0000-0000-000000000101";
    private static final String INTAKE_ID = "00000000-0000-0000-0000-000000000201";
    private static final String REQUEST_ID = "00000000-0000-0000-0000-000000000501";

    @Mock private ITriageExpertHandoffService handoffService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new TriageExpertHandoffController(handoffService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void previewContractReturnsExactMinimumAllowlistWithoutInternalLinkage() throws Exception {
        when(handoffService.preview(UUID.fromString(INTAKE_ID), UUID.fromString(MOTHER_ID)))
                .thenReturn(new HandoffPreviewResponse(
                        UUID.fromString(INTAKE_ID),
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
                        .principal(() -> MOTHER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intakeSessionId").value(INTAKE_ID))
                .andExpect(jsonPath("$.data.consentPolicyVersion")
                        .value("YELLOW_EXPERT_CONTEXT_V1"))
                .andExpect(jsonPath("$.data.riskLevel").value("YELLOW"))
                .andExpect(jsonPath("$.data.stage").isNotEmpty())
                .andExpect(jsonPath("$.data.riskSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.citations").isArray())
                .andExpect(jsonPath("$.data.sharedFields").isArray())
                .andExpect(jsonPath("$.data.excludedFields").isArray())
                .andExpect(jsonPath("$.data.ownerUserId").doesNotExist())
                .andExpect(jsonPath("$.data.journeyId").doesNotExist())
                .andExpect(jsonPath("$.data.originDashboard").doesNotExist())
                .andExpect(jsonPath("$.data.originReferenceId").doesNotExist())
                .andExpect(jsonPath("$.data.continuationToken").doesNotExist());
    }

    @Test
    void createContractReturns201WithCommittedImmutableContextIdentity() throws Exception {
        when(handoffService.create(eq(UUID.fromString(INTAKE_ID)), any(), eq(UUID.fromString(MOTHER_ID))))
                .thenReturn(createResponse(false));
        mockMvc.perform(post(
                        "/api/v1/triage/intake/{intakeSessionId}/expert-handoffs",
                        INTAKE_ID)
                        .principal(() -> MOTHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.consultationRequestId").isNotEmpty())
                .andExpect(jsonPath("$.data.requestStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.replayed").value(false))
                .andExpect(jsonPath("$.data.sharedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.context.riskLevel").value("YELLOW"))
                .andExpect(jsonPath("$.data.context.stage").isNotEmpty())
                .andExpect(jsonPath("$.data.context.riskSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.context.citations").isArray())
                .andExpect(jsonPath("$.data.context.recommendedAction").doesNotExist())
                .andExpect(jsonPath("$.data.context.ownerUserId").doesNotExist())
                .andExpect(jsonPath("$.data.context.intakeSessionId").doesNotExist())
                .andExpect(jsonPath("$.data.context.journeyId").doesNotExist())
                .andExpect(jsonPath("$.data.context.continuationToken").doesNotExist());
    }

    @Test
    void sameIntentReplayContractReturns200AndOriginalAggregate() throws Exception {
        when(handoffService.create(eq(UUID.fromString(INTAKE_ID)), any(), eq(UUID.fromString(MOTHER_ID))))
                .thenReturn(createResponse(true));
        mockMvc.perform(post(
                        "/api/v1/triage/intake/{intakeSessionId}/expert-handoffs",
                        INTAKE_ID)
                        .principal(() -> MOTHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.consultationRequestId").isNotEmpty())
                .andExpect(jsonPath("$.data.requestStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.replayed").value(true))
                .andExpect(jsonPath("$.data.sharedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.context.riskLevel").value("YELLOW"))
                .andExpect(jsonPath("$.data.context.citations").isArray());
    }

    @Test
    void participantContextContractReturnsSameAllowlistWithoutReplayOrInternalIds()
            throws Exception {
        when(handoffService.read(UUID.fromString(REQUEST_ID), UUID.fromString(MOTHER_ID)))
                .thenReturn(new HandoffParticipantResponse(
                        UUID.fromString(REQUEST_ID),
                        "PENDING",
                        Instant.parse("2026-07-23T00:00:00Z"),
                        context()));
        mockMvc.perform(get(
                        "/api/v1/consultation-requests/{requestId}/triage-context",
                        REQUEST_ID)
                        .principal(() -> MOTHER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.consultationRequestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.data.requestStatus").isNotEmpty())
                .andExpect(jsonPath("$.data.sharedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.context.riskLevel").value("YELLOW"))
                .andExpect(jsonPath("$.data.context.stage").isNotEmpty())
                .andExpect(jsonPath("$.data.context.riskSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.context.citations").isArray())
                .andExpect(jsonPath("$.data.replayed").doesNotExist())
                .andExpect(jsonPath("$.data.context.ownerUserId").doesNotExist())
                .andExpect(jsonPath("$.data.context.intakeSessionId").doesNotExist())
                .andExpect(jsonPath("$.data.context.journeyId").doesNotExist())
                .andExpect(jsonPath("$.data.context.originDashboard").doesNotExist())
                .andExpect(jsonPath("$.data.context.continuationToken").doesNotExist());
    }

    @Test
    void falseConsentIsRejectedBeforeAnyHandoffBehavior() throws Exception {
        doThrow(TriageExpertHandoffException.invalidRequest())
                .when(handoffService)
                .create(eq(UUID.fromString(INTAKE_ID)), any(), eq(UUID.fromString(MOTHER_ID)));
        mockMvc.perform(post(
                        "/api/v1/triage/intake/{intakeSessionId}/expert-handoffs",
                        INTAKE_ID)
                        .principal(() -> MOTHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientRequestId":"00000000-0000-0000-0000-000000000301",
                                  "expertProfileId":"00000000-0000-0000-0000-000000000401",
                                  "consentAccepted":false,
                                  "consentPolicyVersion":"YELLOW_EXPERT_CONTEXT_V1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
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

    private static HandoffCreateResponse createResponse(boolean replayed) {
        return new HandoffCreateResponse(
                UUID.fromString(REQUEST_ID),
                "PENDING",
                replayed,
                Instant.parse("2026-07-23T00:00:00Z"),
                context());
    }

    private static HandoffContextResponse context() {
        return new HandoffContextResponse(
                "YELLOW", "POSTPARTUM", "Safe summary", List.of());
    }
}

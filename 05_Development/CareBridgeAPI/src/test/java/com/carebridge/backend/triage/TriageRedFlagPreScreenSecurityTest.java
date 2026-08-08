package com.carebridge.backend.triage;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.triage.controller.IntakeController;
import com.carebridge.backend.triage.engine.TriageGraphService;
import com.carebridge.backend.triage.policy.TriageRedFlagPreScreenPolicy;
import com.carebridge.backend.triage.service.ChildTriageAiClient;
import com.carebridge.backend.triage.service.ITriageContinuationService;
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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TRFP-TC-SEC-001 (CRITICAL) — OWASP A01:2021 / CWE-862.
 * Intake endpoints remain limited to MOTHER and FAMILY; the pre-screen is unreachable pre-authorization,
 * so the new safety path cannot be abused to trigger escalations cross-role.
 * Oracle: CB-TRIAGE-IMP-003 §16 Authorization Matrix (unchanged); IntakeController @PreAuthorize.
 *
 * Red Gate note (Test-Spec §5.1): this TC exercises the pre-existing Spring Security chain and is
 * the documented legitimate pre-existing-subject pass at Red Gate (UC-110 precedent).
 */
@WebMvcTest(
        value = IntakeController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class TriageRedFlagPreScreenSecurityTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ITriageService triageService;
    @MockitoBean private ITriageContinuationService continuationService;
    @MockitoBean private ChildTriageAiClient childTriageAiClient;
    @MockitoBean private TriageGraphService triageGraphService;
    @MockitoBean private ObjectMapper objectMapper;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private TriageRedFlagPreScreenPolicy preScreenPolicy;

    private static final String BASE_URL = "/api/v1/triage/intake";
    // FX-008 — RED-matching keyword text; probes whether the new safety path leaks cross-role.
    private static final String MATCHING_ONE_SHOT_BODY =
            "{\"symptoms\":\"bé bị nga dap dau xuống sàn\"}";
    // The stage pair is supplied deliberately. StartIntakeConversationRequest requires a stage
    // that matches currentIntake.stage, and Spring resolves and validates the body before the
    // @PreAuthorize interceptor runs — so a stage-less body returns 400 and the request never
    // reaches the authorization check this test exists to pin. A body that would otherwise be
    // accepted is what proves the 403 comes from the role, not from a malformed payload.
    private static final String MATCHING_START_BODY =
            "{\"initialText\":\"bé bị nga dap dau xuống sàn\",\"stage\":\"INFANT\","
                    + "\"currentIntake\":{\"stage\":\"INFANT\"}}";

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-0000000000e1", roles = "EXPERT")
    void unsupportedRole_intakeEndpointsForbidden_preScreenUnreachable() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MATCHING_ONE_SHOT_BODY))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(BASE_URL + "/conversation/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MATCHING_START_BODY))
                .andExpect(status().isForbidden());

        // No session persisted, no event published, pre-screen never invoked.
        verifyNoInteractions(triageService, preScreenPolicy);
    }
}

package com.carebridge.backend.triage;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.triage.controller.TriageConsentController;
import com.carebridge.backend.triage.dto.response.TriageConsentAcceptOutcome;
import com.carebridge.backend.triage.dto.response.TriageConsentStatusResponse;
import com.carebridge.backend.triage.service.ITriageConsentService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.carebridge.backend.triage.dto.request.AcceptTriageConsentRequest;

import static com.carebridge.backend.triage.TriageConsentTestFactory.V1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CB-TRIAGE-CONSENT-IMP-001-TEST — {@code @WebMvcTest} slice for
 * {@link TriageConsentController} (mocked {@link ITriageConsentService}; real security filter
 * chain — same conventions as {@code IntakeControllerTest}).
 */
@WebMvcTest(
        value = TriageConsentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class TriageConsentControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ITriageConsentService consentService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/triage/consent";
    private static final String MOTHER = "00000000-0000-0000-0000-000000000010";
    private static final String EXPERT = "00000000-0000-0000-0000-000000000099";

    private static TriageConsentStatusResponse acceptedStatus() {
        TriageConsentStatusResponse status = new TriageConsentStatusResponse();
        status.setStatus("ACCEPTED");
        status.setReason(null);
        status.setCurrentVersion(V1);
        status.setAcceptedVersion(V1);
        status.setAcceptedAt(Instant.parse("2026-07-26T08:00:00Z"));
        status.setDisclaimerText("SYNTHETIC DISCLAIMER TEXT V1");
        return status;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-04 (controller part) — created=true → 201; created=false → 200
    // Oracle: TDS §9.1 (accept idempotent → 200 on no-op, 201 on first accept)
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(username = MOTHER, roles = "MOTHER")
    void tc04_accept_created_shouldReturn201() throws Exception {
        when(consentService.accept(any(), any()))
                .thenReturn(new TriageConsentAcceptOutcome(true, acceptedStatus()));

        mockMvc.perform(post(BASE_URL + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyVersion\":\"" + V1 + "\",\"locale\":\"vi\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        verify(consentService, times(1)).accept(any(), any());
    }

    @Test
    @WithMockUser(username = MOTHER, roles = "MOTHER")
    void tc04_accept_idempotentNoOp_shouldReturn200() throws Exception {
        when(consentService.accept(any(), any()))
                .thenReturn(new TriageConsentAcceptOutcome(false, acceptedStatus()));

        mockMvc.perform(post(BASE_URL + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyVersion\":\"" + V1 + "\",\"locale\":\"vi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        verify(consentService, times(1)).accept(any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-14 — No JWT → 401 on all three consent endpoints (CWE-306)
    // Oracle: platform security conventions (IntakeControllerTest 401 pattern)
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void tc14_withoutJwt_allConsentEndpoints_shouldReturn401_serviceNeverInvoked() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE_URL + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyVersion\":\"" + V1 + "\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE_URL + "/revoke"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(consentService);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-15 — Wrong role (ROLE_EXPERT) → 403 (RBAC 403, distinct from gate 409)
    // Oracle: BR-RBAC / TDS §16 Auth Matrix (MOTHER-only) / ADR-TDC-002
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(username = EXPERT, roles = "EXPERT")
    void tc15_expertRole_allConsentEndpoints_shouldReturn403_serviceNeverInvoked() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE_URL + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyVersion\":\"" + V1 + "\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE_URL + "/revoke"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(consentService);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // TDC-TC-19 — Boundary: policyVersion length 80 accepted; 81 → 400; blank → 400
    // Oracle: baseline DDL policy_version varchar(80) → TDS §8.1 @Size(max=80)
    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    @WithMockUser(username = MOTHER, roles = "MOTHER")
    void tc19_policyVersionBoundary_80Accepted_81AndBlankRejected() throws Exception {
        // Step 1 — exactly 80 chars (FX-009): passes Bean Validation, reaches the service.
        when(consentService.accept(any(), any()))
                .thenReturn(new TriageConsentAcceptOutcome(false, acceptedStatus()));
        String v80 = "V".repeat(80);
        mockMvc.perform(post(BASE_URL + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyVersion\":\"" + v80 + "\"}"))
                .andExpect(status().isOk());
        ArgumentCaptor<AcceptTriageConsentRequest> captor =
                ArgumentCaptor.forClass(AcceptTriageConsentRequest.class);
        verify(consentService, times(1)).accept(captor.capture(), any());
        assertThat(captor.getValue().getPolicyVersion()).isEqualTo(v80);

        // Step 2 — 81 chars: 400 with standard validation ErrorResponse naming policyVersion;
        // the service is never reached for the oversize value (still exactly 1 invocation).
        String v81 = "V".repeat(81);
        mockMvc.perform(post(BASE_URL + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyVersion\":\"" + v81 + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(content().string(containsString("policyVersion")));

        // Step 3 — blank: 400 (@NotBlank).
        mockMvc.perform(post(BASE_URL + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyVersion\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(consentService, times(1)).accept(any(), any());
    }
}

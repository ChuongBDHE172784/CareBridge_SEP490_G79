package com.carebridge.backend.journey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.journey.controller.JourneyOnboardingController;
import com.carebridge.backend.journey.dto.JourneyOnboardingStatusResponse;
import com.carebridge.backend.journey.service.IJourneyOnboardingService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = JourneyOnboardingController.class,
        excludeFilters = @Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class JourneyOnboardingControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean IJourneyOnboardingService onboardingService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserRepository userRepository;

    @Test
    void unauthenticatedRequestsFailClosed() throws Exception {
        mockMvc.perform(get("/api/v1/journey-onboarding/status"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/journey-onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(onboardingService);
    }

    @Test
    void nonMotherCannotSubmitBaselineOrConsent() throws Exception {
        mockMvc.perform(post("/api/v1/journey-onboarding")
                        .with(user(JourneyLifecycleTestFactory.EXPERT_ID.toString()).roles("EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(onboardingService);
    }

    @Test
    void motherSubmissionUsesAuthenticatedOwnerAndReturnsMinimumStatus() throws Exception {
        when(onboardingService.submit(eq(JourneyLifecycleTestFactory.MOTHER_ID), any()))
                .thenReturn(JourneyOnboardingStatusResponse.builder()
                        .baselineComplete(true)
                        .consentValid(true)
                        .baselineRevision(1)
                        .build());

        mockMvc.perform(post("/api/v1/journey-onboarding")
                        .with(user(JourneyLifecycleTestFactory.MOTHER_ID.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baselineComplete").value(true))
                .andExpect(jsonPath("$.data.consentValid").value(true))
                .andExpect(jsonPath("$.data.baselineRevision").value(1));
    }

    @Test
    void eligibilityConflictPreservesStableMachineReadableCode() throws Exception {
        when(onboardingService.getStatus(JourneyLifecycleTestFactory.MOTHER_ID))
                .thenThrow(new BusinessException(
                        HttpStatus.CONFLICT,
                        "LIFECYCLE_CONSENT_INVALID",
                        "Your lifecycle consent needs to be reviewed"));

        mockMvc.perform(get("/api/v1/journey-onboarding/status")
                        .with(user(JourneyLifecycleTestFactory.MOTHER_ID.toString())
                                .roles("MOTHER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LIFECYCLE_CONSENT_INVALID"));
    }

    @Test
    void eligibilityStoreFailurePreservesUnavailableCode() throws Exception {
        when(onboardingService.getStatus(JourneyLifecycleTestFactory.MOTHER_ID))
                .thenThrow(new BusinessException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "ONBOARDING_ELIGIBILITY_UNAVAILABLE",
                        "Onboarding eligibility cannot be verified right now"));

        mockMvc.perform(get("/api/v1/journey-onboarding/status")
                        .with(user(JourneyLifecycleTestFactory.MOTHER_ID.toString())
                                .roles("MOTHER")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error")
                        .value("ONBOARDING_ELIGIBILITY_UNAVAILABLE"));
    }

    private String validRequest() {
        return """
                {
                  "submissionId": "00000000-0000-0000-0000-000000006200",
                  "lifecycleGoal": "PREPARING_FOR_PREGNANCY",
                  "locale": "vi-VN",
                  "timeZone": "Asia/Ho_Chi_Minh",
                  "preferences": ["NUTRITION"],
                  "consentAccepted": true,
                  "policyVersion": "MOTHER_LIFECYCLE_V1"
                }
                """;
    }
}

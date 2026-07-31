package com.carebridge.backend.baby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.baby.controller.BabyController;
import com.carebridge.backend.baby.service.IBabyService;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(
        value = BabyController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class BabyRemovedRouteContractTest {

    private static final UUID MOTHER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID BABY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID JOURNEY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000030");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private IBabyService babyService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @Test
    void authenticatedMotherCannotCallRemovedJourneyLinkPutRoute() throws Exception {
        MvcResult result = mockMvc.perform(put("/api/v1/babies/{babyId}/journey-link", BABY_ID)
                        .with(user(MOTHER_ID.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        assertFrameworkGenericMissingRoute(result);
        verifyNoInteractions(babyService);
    }

    @Test
    void authenticatedMotherCannotCallRemovedJourneyScopedBabiesGetRoute() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/journeys/{journeyId}/babies", JOURNEY_ID)
                        .with(user(MOTHER_ID.toString()).roles("MOTHER")))
                .andReturn();

        assertFrameworkGenericMissingRoute(result);
        verifyNoInteractions(babyService);
    }

    @Test
    void legacyRelatedJourneyIdIsNeutralValidationFailure() throws Exception {
        assertLegacyCreateFieldRejected("relatedJourneyId");
    }

    @Test
    void legacySubmissionIdIsNeutralValidationFailure() throws Exception {
        assertLegacyCreateFieldRejected("submissionId");
    }

    private void assertLegacyCreateFieldRejected(String legacyField) throws Exception {
        String request = """
                {
                  "nickname": "Baby Bean",
                  "birthDate": "2026-07-01",
                  "%s": "00000000-0000-0000-0000-000000000040"
                }
                """.formatted(legacyField);

        MvcResult result = mockMvc.perform(post("/api/v1/babies")
                        .with(user(MOTHER_ID.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid request body"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(legacyField);
        verifyNoInteractions(babyService);
    }

    private static void assertFrameworkGenericMissingRoute(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).isIn(404, 405);
        assertThat(result.getResponse().getContentAsString())
                .containsAnyOf(
                        "\"error\":\"RESOURCE_NOT_FOUND\"",
                        "\"error\":\"METHOD_NOT_ALLOWED\"")
                .doesNotContain("BABY-", "JOURNEY-");
    }
}

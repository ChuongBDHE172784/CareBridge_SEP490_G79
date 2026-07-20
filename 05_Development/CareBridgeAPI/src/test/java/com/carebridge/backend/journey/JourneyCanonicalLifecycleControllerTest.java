package com.carebridge.backend.journey;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.journey.controller.JourneyController;
import com.carebridge.backend.journey.dto.JourneyTransitionResponse;
import com.carebridge.backend.journey.dto.JourneyTransitionPageResponse;
import com.carebridge.backend.journey.dto.PregnancyOutcomeResponse;
import com.carebridge.backend.journey.entity.*;
import com.carebridge.backend.journey.service.IJourneyService;
import com.carebridge.backend.journey.service.IJourneyTransitionService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = JourneyController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class JourneyCanonicalLifecycleControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired RequestMappingHandlerMapping handlerMapping;

    @MockitoBean IJourneyService journeyService;
    @MockitoBean IJourneyTransitionService journeyTransitionService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserRepository userRepository;

    @Test
    void story63_motherRecordsOutcomeThroughDedicatedOwnedCommand() throws Exception {
        UUID evidenceId = UUID.fromString("40000000-0000-0000-0000-000000000001");
        when(journeyTransitionService.recordPregnancyOutcome(
                eq(JourneyLifecycleTestFactory.MOTHER_ID),
                eq(JourneyLifecycleTestFactory.JOURNEY_ID),
                any())).thenReturn(PregnancyOutcomeResponse.builder()
                .evidenceId(evidenceId)
                .journeyId(JourneyLifecycleTestFactory.JOURNEY_ID)
                .outcomeType(PregnancyOutcomeType.LIVE_BIRTH)
                .outcomeDate(java.time.LocalDate.of(2026, 7, 18))
                .journeyType(JourneyType.POSTPARTUM)
                .journeyVersion(4L)
                .revisionNumber(1)
                .babyActionsEligible(true)
                .build());

        mockMvc.perform(post("/api/v1/journeys/" + JourneyLifecycleTestFactory.JOURNEY_ID
                        + "/pregnancy-outcomes")
                        .with(SecurityMockMvcRequestPostProcessors.user(
                                JourneyLifecycleTestFactory.MOTHER_ID.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissionId":"30000000-0000-0000-0000-000000000001",
                                  "expectedJourneyVersion":3,
                                  "outcomeType":"LIVE_BIRTH",
                                  "outcomeDate":"2026-07-18",
                                  "source":"SELF_REPORTED",
                                  "reason":"Outcome confirmed",
                                  "effectiveAt":"2026-07-19T08:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.evidenceId").value(evidenceId.toString()))
                .andExpect(jsonPath("$.data.journeyType").value("POSTPARTUM"))
                .andExpect(jsonPath("$.data.babyActionsEligible").value(true));
    }

    @Test
    void story63_requiresExpectedJourneyVersion() throws Exception {
        mockMvc.perform(post("/api/v1/journeys/" + JourneyLifecycleTestFactory.JOURNEY_ID
                        + "/pregnancy-outcomes")
                        .with(SecurityMockMvcRequestPostProcessors.user(
                                JourneyLifecycleTestFactory.MOTHER_ID.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submissionId":"30000000-0000-0000-0000-000000000001",
                                  "outcomeType":"ONGOING",
                                  "source":"SELF_REPORTED",
                                  "reason":"Outcome confirmed",
                                  "effectiveAt":"2026-07-19T08:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(journeyTransitionService);
    }

    @Test
    void jrnTcSec001_unauthenticatedCreateUpdateAndHistoryReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/journeys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/journeys/" + JourneyLifecycleTestFactory.JOURNEY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"updated\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(historyUrl()))
                .andExpect(status().isUnauthorized());
        assertThat(historyRouteExists()).isTrue();
        verifyNoInteractions(journeyService);
    }

    @Test
    void jrnTcSec002_expertCannotCreateUpdateOrReadHistory() throws Exception {
        when(journeyService.createJourney(any(), eq(JourneyLifecycleTestFactory.EXPERT_ID)))
                .thenThrow(new BusinessException(
                        HttpStatus.FORBIDDEN, "JOURNEY-003", "Mother role required"));
        var expert = SecurityMockMvcRequestPostProcessors.user(
                JourneyLifecycleTestFactory.EXPERT_ID.toString()).roles("EXPERT");

        mockMvc.perform(post("/api/v1/journeys")
                        .with(expert)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("JOURNEY-003"));
        mockMvc.perform(put("/api/v1/journeys/" + JourneyLifecycleTestFactory.JOURNEY_ID)
                        .with(expert)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"updated\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(historyUrl())
                        .with(expert))
                .andExpect(status().isForbidden());
        assertThat(historyRouteExists()).isTrue();
        verify(journeyService).createJourney(any(), eq(JourneyLifecycleTestFactory.EXPERT_ID));
        verifyNoMoreInteractions(journeyService);
    }

    @Test
    void jrnTcSec003_otherMotherCannotReadHistory() throws Exception {
        when(journeyService.getHistory(
                JourneyLifecycleTestFactory.OTHER_MOTHER_ID,
                JourneyLifecycleTestFactory.JOURNEY_ID,
                PageRequest.of(0, 20)))
                .thenThrow(new BusinessException(
                        HttpStatus.FORBIDDEN, "JOURNEY-011", "Access denied"));

        mockMvc.perform(get(historyUrl())
                .with(SecurityMockMvcRequestPostProcessors.user(
                                JourneyLifecycleTestFactory.OTHER_MOTHER_ID.toString()).roles("MOTHER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("JOURNEY-011"));
    }

    @Test
    void jrnTcSec004_historyResponseIsMinimumNecessary() throws Exception {
        JourneyTransitionResponse item = JourneyTransitionResponse.builder()
                .transitionId(UUID.randomUUID())
                .eventType(JourneyTransitionType.DATES_CHANGED)
                .fromStage(JourneyType.PREGNANCY)
                .toStage(JourneyType.PREGNANCY)
                .changedFields(List.of("lastMenstrualDate"))
                .source(JourneyDateSource.SELF_REPORTED)
                .confidence(JourneyDateConfidence.ESTIMATED)
                .reason("DATE_CORRECTION")
                .effectiveAt(JourneyLifecycleTestFactory.NOW)
                .recordedAt(JourneyLifecycleTestFactory.NOW)
                .journeyVersion(1L)
                .build();
        when(journeyService.getHistory(
                JourneyLifecycleTestFactory.MOTHER_ID,
                JourneyLifecycleTestFactory.JOURNEY_ID,
                PageRequest.of(0, 20)))
                .thenReturn(JourneyTransitionPageResponse.builder()
                        .items(List.of(item))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .totalPages(1)
                        .build());

        String body = mockMvc.perform(get(historyUrl())
                        .with(SecurityMockMvcRequestPostProcessors.user(
                                JourneyLifecycleTestFactory.MOTHER_ID.toString()).roles("MOTHER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .contains("lastMenstrualDate")
                .contains("totalElements")
                .doesNotContain("notes", "token", "contact", "ownerUserId");
    }

    private String historyUrl() {
        return "/api/v1/journeys/" + JourneyLifecycleTestFactory.JOURNEY_ID + "/history";
    }

    private boolean historyRouteExists() {
        return handlerMapping.getHandlerMethods().keySet().stream()
                .anyMatch(info -> info.getPatternValues().contains(
                        "/api/v1/journeys/{journeyId}/history"));
    }

    private String validCreateJson() {
        return """
                {
                  "journeyType": "PREGNANCY",
                  "startDate": "2026-07-18",
                  "lastMenstrualDate": "2026-06-01",
                  "dateSource": "SELF_REPORTED",
                  "dateConfidence": "ESTIMATED",
                  "changeReason": "INITIAL_SETUP",
                  "effectiveAt": "2026-07-18T03:00:00Z"
                }
                """;
    }
}

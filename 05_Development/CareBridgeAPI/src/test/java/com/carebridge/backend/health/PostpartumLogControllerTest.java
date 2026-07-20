package com.carebridge.backend.health;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.health.controller.PostpartumLogController;
import com.carebridge.backend.health.dto.PostpartumLogResponse;
import com.carebridge.backend.health.service.IPostpartumLogService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = PostpartumLogController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class PostpartumLogControllerTest {

    private static final String MOTHER_ID = "00000000-0000-4000-8000-000000000064";
    private static final UUID JOURNEY_ID =
            UUID.fromString("10000000-0000-4000-8000-000000000064");
    private static final UUID LOG_ID =
            UUID.fromString("20000000-0000-4000-8000-000000000064");
    private static final UUID SUBMISSION_ID =
            UUID.fromString("30000000-0000-4000-8000-000000000064");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private IPostpartumLogService service;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @Test
    @WithMockUser(username = MOTHER_ID, roles = "MOTHER")
    void listLogs_authenticatedMother_returnsSinglePaginatedEnvelope() throws Exception {
        var item = response();
        var page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);
        when(service.listLogs(JOURNEY_ID, UUID.fromString(MOTHER_ID), 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/postpartum-logs")
                        .param("journeyId", JOURNEY_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].postpartumLogId").value(LOG_ID.toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = MOTHER_ID, roles = "MOTHER")
    void crudHappyPaths_useAuthenticatedPrincipal() throws Exception {
        when(service.addLog(eq(UUID.fromString(MOTHER_ID)), eq(JOURNEY_ID), any()))
                .thenReturn(response());
        when(service.updateLog(eq(LOG_ID), eq(UUID.fromString(MOTHER_ID)), any()))
                .thenReturn(response());
        when(service.getLogDetail(LOG_ID, UUID.fromString(MOTHER_ID))).thenReturn(response());

        mockMvc.perform(post("/api/v1/journeys/{journeyId}/postpartum-logs", JOURNEY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.postpartumLogId").value(LOG_ID.toString()));
        mockMvc.perform(get("/api/v1/postpartum-logs/{logId}", LOG_ID))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/postpartum-logs/{logId}", LOG_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"painLevel\":4}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/postpartum-logs/{logId}", LOG_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void unauthenticatedRequests_failClosed() throws Exception {
        mockMvc.perform(get("/api/v1/postpartum-logs")
                        .param("journeyId", JOURNEY_ID.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/journeys/{journeyId}/postpartum-logs", JOURNEY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/postpartum-logs/{logId}", LOG_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"painLevel\":4}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/postpartum-logs/{logId}", LOG_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = MOTHER_ID, roles = "FAMILY")
    void nonMotherRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/postpartum-logs")
                        .param("journeyId", JOURNEY_ID.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/journeys/{journeyId}/postpartum-logs", JOURNEY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isForbidden());
        verify(service, never()).listLogs(any(), any(), any(Integer.class), any(Integer.class));
        verify(service, never()).addLog(any(), any(), any());
    }

    @Test
    @WithMockUser(username = MOTHER_ID, roles = "MOTHER")
    void listLogs_rejectsPagingBoundsBeforeService() throws Exception {
        mockMvc.perform(get("/api/v1/postpartum-logs")
                        .param("journeyId", JOURNEY_ID.toString())
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/postpartum-logs")
                        .param("journeyId", JOURNEY_ID.toString())
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        verify(service, never()).listLogs(any(), any(), any(Integer.class), any(Integer.class));
    }

    @Test
    @WithMockUser(username = MOTHER_ID, roles = "MOTHER")
    void detailNotFound_returnsNeutralBusinessError() throws Exception {
        when(service.getLogDetail(LOG_ID, UUID.fromString(MOTHER_ID)))
                .thenThrow(new BusinessException(
                        HttpStatus.NOT_FOUND, "PPLOG-001", "Postpartum log not found"));

        mockMvc.perform(get("/api/v1/postpartum-logs/{logId}", LOG_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PPLOG-001"));
    }

    @Test
    @WithMockUser(username = MOTHER_ID, roles = "MOTHER")
    void deletedSubmissionReplay_returnsStableConflict() throws Exception {
        when(service.addLog(eq(UUID.fromString(MOTHER_ID)), eq(JOURNEY_ID), any()))
                .thenThrow(new BusinessException(
                        HttpStatus.CONFLICT,
                        "POSTPARTUM_SUBMISSION_GONE",
                        "Submission id belongs to a deleted postpartum recovery log"));

        mockMvc.perform(post("/api/v1/journeys/{journeyId}/postpartum-logs", JOURNEY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("POSTPARTUM_SUBMISSION_GONE"));
    }

    private static PostpartumLogResponse response() {
        return PostpartumLogResponse.builder()
                .postpartumLogId(LOG_ID)
                .journeyId(JOURNEY_ID)
                .submissionId(SUBMISSION_ID)
                .logDate(LocalDate.of(2026, 7, 19))
                .build();
    }

    private static String validCreateBody() {
        return """
                {
                  "submissionId":"%s",
                  "logDate":"2026-07-19",
                  "painLevel":3
                }
                """.formatted(SUBMISSION_ID);
    }
}

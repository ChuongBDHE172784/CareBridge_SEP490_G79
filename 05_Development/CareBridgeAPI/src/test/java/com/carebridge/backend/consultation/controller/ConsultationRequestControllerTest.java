package com.carebridge.backend.consultation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.exception.GlobalExceptionHandler;
import com.carebridge.backend.consultation.entity.ConsultationRequestStatus;
import com.carebridge.backend.consultation.exception.ConsultationRequestException;
import com.carebridge.backend.consultation.dto.response.ConsultationRequestResponse;
import com.carebridge.backend.consultation.service.CreateConsultationRequestResult;
import com.carebridge.backend.consultation.service.IConsultationRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ConsultationRequestControllerTest {

    private static final UUID MOTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID EXPERT_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");

    @Mock private IConsultationRequestService service;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ConsultationRequestController controller = new ConsultationRequestController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void createReturns201ForNewResource() throws Exception {
        when(service.create(any(), eq(MOTHER_ID)))
                .thenReturn(new CreateConsultationRequestResult(response(), true));

        mockMvc.perform(post("/api/v1/consultation-requests")
                        .principal(() -> MOTHER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createReturns200ForIdempotentRetry() throws Exception {
        when(service.create(any(), eq(MOTHER_ID)))
                .thenReturn(new CreateConsultationRequestResult(response(), false));

        mockMvc.perform(post("/api/v1/consultation-requests")
                        .principal(() -> MOTHER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(REQUEST_ID.toString()));
    }

    @Test
    void validationUsesFlatValidationErrorContract() throws Exception {
        var body = validBody();
        body.put("topic", "");
        body.put("preferredWindowStart", "2026-07-17T01:00:00Z");
        body.put("preferredWindowEnd", null);

        mockMvc.perform(post("/api/v1/consultation-requests")
                        .principal(() -> MOTHER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void topicBoundaryAccepts200AndRejects201Characters() throws Exception {
        when(service.create(any(), eq(MOTHER_ID)))
                .thenReturn(new CreateConsultationRequestResult(response(), true));
        var accepted = validBody();
        accepted.put("topic", "a".repeat(200));
        mockMvc.perform(post("/api/v1/consultation-requests")
                        .principal(() -> MOTHER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(accepted)))
                .andExpect(status().isCreated());

        var rejected = validBody();
        rejected.put("topic", "a".repeat(201));
        mockMvc.perform(post("/api/v1/consultation-requests")
                        .principal(() -> MOTHER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejected)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void listAssignedBuildsDefaultTwentyItemPage() {
        when(service.listAssigned(eq(MOTHER_ID), eq(ConsultationRequestStatus.PENDING), any()))
                .thenReturn(Page.empty());

        new ConsultationRequestController(service).listAssigned(
                ConsultationRequestStatus.PENDING, 0, 20, () -> MOTHER_ID.toString());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).listAssigned(
                eq(MOTHER_ID), eq(ConsultationRequestStatus.PENDING), pageable.capture());
        org.assertj.core.api.Assertions.assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void unexpectedExceptionUsesInternalErrorWithoutRawMessage() throws Exception {
        when(service.accept(eq(REQUEST_ID), eq(MOTHER_ID)))
                .thenThrow(new RuntimeException("internal db detail"));

        mockMvc.perform(patch("/api/v1/consultation-requests/{id}/accept", REQUEST_ID)
                        .principal(() -> MOTHER_ID.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("internal db detail"))));
    }

    @Test
    void notFoundAndOutsiderResponsesAreIndistinguishable() throws Exception {
        UUID missingId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        when(service.getById(eq(missingId), eq(MOTHER_ID)))
                .thenThrow(ConsultationRequestException.notFound());
        when(service.getById(eq(outsiderId), eq(MOTHER_ID)))
                .thenThrow(ConsultationRequestException.notFound());

        String missing = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/v1/consultation-requests/{id}", missingId)
                                .principal(() -> MOTHER_ID.toString()))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String outsider = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/v1/consultation-requests/{id}", outsiderId)
                                .principal(() -> MOTHER_ID.toString()))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var missingBody = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(missing);
        var outsiderBody = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(outsider);
        missingBody.remove(java.util.List.of("path", "timestamp"));
        outsiderBody.remove(java.util.List.of("path", "timestamp"));
        org.assertj.core.api.Assertions.assertThat(outsiderBody).isEqualTo(missingBody);
    }

    @Test
    void rejectReasonIsOptionalAndLimitedToFiveHundredCharacters() throws Exception {
        when(service.reject(eq(REQUEST_ID), eq(MOTHER_ID), eq(null))).thenReturn(response());
        mockMvc.perform(patch("/api/v1/consultation-requests/{id}/reject", REQUEST_ID)
                        .principal(() -> MOTHER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/consultation-requests/{id}/reject", REQUEST_ID)
                        .principal(() -> MOTHER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("reason", "a".repeat(501)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    private static java.util.Map<String, Object> validBody() {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("clientRequestId", UUID.randomUUID());
        body.put("expertProfileId", EXPERT_PROFILE_ID);
        body.put("topic", "Nutrition");
        body.put("description", "Please advise on feeding.");
        return body;
    }

    private static ConsultationRequestResponse response() {
        return ConsultationRequestResponse.builder()
                .id(REQUEST_ID)
                .expertProfileId(EXPERT_PROFILE_ID)
                .topic("Nutrition")
                .description("Please advise on feeding.")
                .status("PENDING")
                .expiresAt(Instant.parse("2026-07-18T12:00:00Z"))
                .createdAt(Instant.parse("2026-07-16T12:00:00Z"))
                .build();
    }
}

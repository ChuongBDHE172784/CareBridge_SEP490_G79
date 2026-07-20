package com.carebridge.backend.baby;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.baby.controller.BabyController;
import com.carebridge.backend.baby.controller.JourneyBabiesController;
import com.carebridge.backend.baby.dto.LinkBabyJourneyRequest;
import com.carebridge.backend.baby.dto.LinkBabyJourneyResponse;
import com.carebridge.backend.baby.security.BabyLinkBoundaryAuditFilter;
import com.carebridge.backend.baby.service.BabyLinkRejectionAuditService;
import com.carebridge.backend.baby.service.IBabyService;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.common.exception.BusinessException;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = {BabyController.class, JourneyBabiesController.class},
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class, BabyLinkBoundaryAuditFilter.class})
class BabyJourneyLinkControllerTest {

    private static final UUID MOTHER = UUID.fromString("51000000-0000-0000-0000-000000000001");
    private static final UUID EXPERT = UUID.fromString("51000000-0000-0000-0000-000000000002");
    private static final UUID BABY = UUID.fromString("52000000-0000-0000-0000-000000000001");
    private static final UUID JOURNEY = UUID.fromString("53000000-0000-0000-0000-000000000001");
    private static final UUID SUBMISSION = UUID.fromString("54000000-0000-0000-0000-000000000001");

    @Autowired MockMvc mockMvc;
    @MockitoBean IBabyService babyService;
    @MockitoBean BabyLinkRejectionAuditService rejectionAuditService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserRepository userRepository;

    @Test
    void unauthenticatedLinkIsRejectedWithoutServiceExecution() throws Exception {
        mockMvc.perform(put(linkUrl()).contentType(MediaType.APPLICATION_JSON).content(validLinkJson()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(babyService);
    }

    @Test
    void invalidBearerTokenIsRejectedWithoutServiceExecution() throws Exception {
        mockMvc.perform(put(linkUrl())
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLinkJson()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(babyService);
    }

    @Test
    void nonMotherLinkIsForbiddenAndBoundaryAudited() throws Exception {
        mockMvc.perform(put(linkUrl())
                        .with(user(EXPERT.toString()).roles("EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLinkJson()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(babyService);
        verify(rejectionAuditService).record(EXPERT, BABY, "BOUNDARY_HTTP_403");
    }

    @Test
    void invalidLinkPayloadIsRejectedAndBoundaryAudited() throws Exception {
        mockMvc.perform(put(linkUrl())
                        .with(user(MOTHER.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relatedJourneyId\":\"" + JOURNEY + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        verifyNoInteractions(babyService);
        verify(rejectionAuditService).record(MOTHER, BABY, "BOUNDARY_HTTP_400");
    }

    @Test
    void invalidCreateWithLinkIsBoundaryAuditedBeforeServiceExecution() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/babies")
                        .with(user(MOTHER.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"Bean\",\"birthDate\":\"2026-07-20\",\"relatedJourneyId\":\"" + JOURNEY + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(babyService);
        verify(rejectionAuditService).record(MOTHER, null, "BOUNDARY_VALIDATION");
    }

    @Test
    void validMotherLinkDelegatesWithAuthenticatedCaller() throws Exception {
        org.mockito.Mockito.when(babyService.linkExistingBaby(any(), any(), any()))
                .thenReturn(LinkBabyJourneyResponse.builder().babyId(BABY).relatedJourneyId(JOURNEY).build());

        mockMvc.perform(put(linkUrl())
                        .with(user(MOTHER.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLinkJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.babyId").value(BABY.toString()))
                .andExpect(jsonPath("$.data.relatedJourneyId").value(JOURNEY.toString()));

        verify(babyService).linkExistingBaby(
                org.mockito.ArgumentMatchers.eq(BABY),
                any(LinkBabyJourneyRequest.class),
                org.mockito.ArgumentMatchers.eq(MOTHER));
    }

    @Test
    void malformedBabyIdentifierIsRejectedBeforeServiceAndAuditedWithoutTarget() throws Exception {
        mockMvc.perform(put("/api/v1/babies/not-a-uuid/journey-link")
                        .with(user(MOTHER.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLinkJson()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(babyService);
        verify(rejectionAuditService).record(MOTHER, null, "BOUNDARY_HTTP_400");
    }

    @Test
    void neutralForeignIdentifierResponseDoesNotDiscloseOwnership() throws Exception {
        org.mockito.Mockito.when(babyService.linkExistingBaby(any(), any(), any()))
                .thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "LINK_RESOURCE_NOT_FOUND", "Resource not found"));

        mockMvc.perform(put(linkUrl())
                        .with(user(MOTHER.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLinkJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("LINK_RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    void linkConflictPreservesTyped409Response() throws Exception {
        org.mockito.Mockito.when(babyService.linkExistingBaby(any(), any(), any()))
                .thenThrow(new BusinessException(HttpStatus.CONFLICT, "BABY_ALREADY_LINKED", "Baby is already linked"));

        mockMvc.perform(put(linkUrl())
                        .with(user(MOTHER.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLinkJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BABY_ALREADY_LINKED"));
    }

    @Test
    void invalidPaginationIsRejectedAndBoundaryAudited() throws Exception {
        mockMvc.perform(get("/api/v1/journeys/" + JOURNEY + "/babies?page=-1")
                        .with(user(MOTHER.toString()).roles("MOTHER")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(babyService);
        verify(rejectionAuditService).record(MOTHER, JOURNEY, "BOUNDARY_HTTP_400");
    }

    private String linkUrl() {
        return "/api/v1/babies/" + BABY + "/journey-link";
    }

    private String validLinkJson() {
        return "{\"relatedJourneyId\":\"" + JOURNEY + "\",\"submissionId\":\"" + SUBMISSION + "\"}";
    }
}

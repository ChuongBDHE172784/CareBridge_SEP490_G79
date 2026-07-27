package com.carebridge.backend.moderation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.ModerationController;
import com.carebridge.backend.content.dto.response.ModerationQueueItemResponse;
import com.carebridge.backend.content.dto.response.ModerationQueueResponse;
import com.carebridge.backend.content.dto.response.AccountViolationHistoryResponse;
import com.carebridge.backend.content.dto.response.AccountViolationSummaryResponse;
import com.carebridge.backend.content.dto.response.RelatedReportPageResponse;
import com.carebridge.backend.content.dto.response.PendingContentItemResponse;
import com.carebridge.backend.content.dto.response.PendingContentQueueResponse;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.service.ModerationService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// MOD-TC-007, MOD-TC-008
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ModerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String QUEUE_URL = "/api/v1/admin/moderation/queue";

    private ModerationQueueResponse makeEmptyResponse() {
        return new ModerationQueueResponse(List.of(), 0, 0, 20);
    }

    private ModerationQueueResponse makeResponseWithItem() {
        ModerationQueueItemResponse item = new ModerationQueueItemResponse(
                UUID.randomUUID(),
                ReportTargetType.QUESTION,
                "Preview text",
                3L,
                Instant.now(),
                "Inappropriate",
                ReportStatus.PENDING
        );
        return new ModerationQueueResponse(List.of(item), 1, 0, 20);
    }

    // MOD-TC-007: size > 50 returns 400 with MOD-002 (BR-MOD-003, C6)
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getQueue_pageSizeExceedsMax_shouldReturn400() throws Exception {
        mockMvc.perform(get(QUEUE_URL + "?size=51").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MOD-002"));
    }

    // MOD-TC-008: invalid contentType/targetType enum → 400 (MOD-001 from binding error)
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getQueue_invalidTargetType_shouldReturn400() throws Exception {
        mockMvc.perform(get(QUEUE_URL + "?targetType=INVALID_VALUE").with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // Happy path: MODERATOR with valid params → 200
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getQueue_validRequestAsModerator_shouldReturn200() throws Exception {
        when(moderationService.getModerationQueue(any(), any())).thenReturn(makeResponseWithItem());

        mockMvc.perform(get(QUEUE_URL + "?targetType=QUESTION&status=PENDING&page=0&size=20").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].targetType").value("QUESTION"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    // Size exactly 50 is valid (boundary value)
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getQueue_pageSizeExactly50_shouldReturn200() throws Exception {
        when(moderationService.getModerationQueue(any(), any())).thenReturn(makeEmptyResponse());

        mockMvc.perform(get(QUEUE_URL + "?size=50").with(csrf()))
                .andExpect(status().isOk());
    }

    private static final String PENDING_CONTENT_URL = "/api/v1/admin/moderation/pending-content";
    private static final String HISTORY_URL = "/api/v1/admin/moderation/history";
    private static final String ACCOUNT_HISTORY_URL = "/api/v1/admin/moderation/account-history";
    private static final String RELATED_REPORTS_URL = "/api/v1/admin/moderation/reports/"
            + "11111111-1111-1111-1111-111111111111/related";

    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getRelatedReports_validRequestAsModerator_shouldReturn200() throws Exception {
        when(moderationService.getRelatedReports(any(), any(Integer.class), any(Integer.class), any()))
                .thenReturn(new RelatedReportPageResponse(List.of(), 0, 0, 20));

        mockMvc.perform(get(RELATED_REPORTS_URL + "?page=0&size=20").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getRelatedReports_pageSizeExceedsMax_shouldReturn400() throws Exception {
        mockMvc.perform(get(RELATED_REPORTS_URL + "?size=51").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MOD-002"));
    }

    @Test
    @WithMockUser(username = "1", roles = "MOTHER")
    void getRelatedReports_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(get(RELATED_REPORTS_URL).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getAccountViolationHistory_pageSizeExceedsMax_shouldReturn400() throws Exception {
        mockMvc.perform(get(ACCOUNT_HISTORY_URL + "?size=51").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MOD-002"));
    }

    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getAccountViolationHistory_validRequestAsModerator_shouldReturn200() throws Exception {
        when(moderationService.getAccountViolationHistory(any(Integer.class), any(Integer.class), any()))
                .thenReturn(new AccountViolationSummaryResponse(List.of(), 0, 0, 20));

        mockMvc.perform(get(ACCOUNT_HISTORY_URL + "?page=0&size=20").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getAccountViolationDetail_validRequestAsModerator_shouldReturn200() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        when(moderationService.getAccountViolationHistory(eq(targetUserId), any(Integer.class), any(Integer.class), any()))
                .thenReturn(new AccountViolationHistoryResponse(List.of(), 0, 0, 20));

        mockMvc.perform(get(ACCOUNT_HISTORY_URL + "/" + targetUserId + "?page=0&size=20").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // PCQH-TC-005: size > 50 returns 400 with MOD-002 (same guard as /queue, /pending-content)
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getModerationHistory_pageSizeExceedsMax_shouldReturn400() throws Exception {
        mockMvc.perform(get(HISTORY_URL + "?size=51").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MOD-002"));
    }

    // PCQ-TC-005: size > 50 returns 400 with MOD-002 (same guard as /queue)
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getPendingContentQueue_pageSizeExceedsMax_shouldReturn400() throws Exception {
        mockMvc.perform(get(PENDING_CONTENT_URL + "?targetType=QUESTION&size=51").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MOD-002"));
    }

    // PCQ-TC-008: happy path — MODERATOR with valid params → 200, correct JSON shape
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getPendingContentQueue_validRequestAsModerator_shouldReturn200() throws Exception {
        PendingContentItemResponse item = new PendingContentItemResponse(
                UUID.randomUUID(), ReportTargetType.QUESTION, "Preview text", Instant.now());
        PendingContentQueueResponse response = new PendingContentQueueResponse(List.of(item), 1, 0, 20);
        when(moderationService.getPendingContentQueue(any(), any())).thenReturn(response);

        mockMvc.perform(get(PENDING_CONTENT_URL + "?targetType=QUESTION&page=0&size=20").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content[0].targetType").value("QUESTION"));
    }

    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void revertReport_removedEndpoint_shouldReturn404() throws Exception {
        mockMvc.perform(post("/api/v1/admin/moderation/reports/" + UUID.randomUUID() + "/revert").with(csrf()))
                .andExpect(status().isNotFound());
    }
}

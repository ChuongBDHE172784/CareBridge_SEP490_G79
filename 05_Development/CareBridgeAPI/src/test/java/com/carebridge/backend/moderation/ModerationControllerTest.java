package com.carebridge.backend.moderation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.ModerationController;
import com.carebridge.backend.content.dto.response.ModerationQueueItemResponse;
import com.carebridge.backend.content.dto.response.ModerationQueueResponse;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.service.ModerationService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
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
@Import(SecurityConfig.class)
class ModerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

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
}

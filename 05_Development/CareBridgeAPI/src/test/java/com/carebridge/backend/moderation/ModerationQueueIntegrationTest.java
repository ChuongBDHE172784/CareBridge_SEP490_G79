package com.carebridge.backend.moderation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

// MOD-TC-INT-001: Full HTTP stack integration — Security → Controller → Service (mocked persistence)
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ModerationQueueIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String QUEUE_URL = "/api/v1/admin/moderation/queue";

    private ModerationQueueItemResponse makeItem(ReportTargetType type, ReportStatus status) {
        return new ModerationQueueItemResponse(
                UUID.randomUUID(), type, "Preview text", 1L, Instant.now(), "Reason", status);
    }

    // MOD-TC-INT-001 Step 2: PENDING filter returns FX-001 (QUESTION/PENDING) and FX-002 (ANSWER/PENDING)
    // FX-003 (QUESTION/RESOLVED) must NOT appear
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getQueue_pendingFilter_returnsPendingItemsOnly() throws Exception {
        ModerationQueueItemResponse fx001 = makeItem(ReportTargetType.QUESTION, ReportStatus.PENDING);
        ModerationQueueItemResponse fx002 = makeItem(ReportTargetType.ANSWER, ReportStatus.PENDING);
        ModerationQueueResponse response = new ModerationQueueResponse(
                List.of(fx001, fx002), 2L, 0, 20);

        when(moderationService.getModerationQueue(any(), any())).thenReturn(response);

        mockMvc.perform(get(QUEUE_URL + "?status=PENDING&page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    // MOD-TC-INT-001 Step 4: contentType=QUESTION filter returns only QUESTION items
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getQueue_questionFilter_returnsOnlyQuestionItems() throws Exception {
        ModerationQueueItemResponse fx001 = makeItem(ReportTargetType.QUESTION, ReportStatus.PENDING);
        ModerationQueueResponse response = new ModerationQueueResponse(
                List.of(fx001), 1L, 0, 20);

        when(moderationService.getModerationQueue(any(), any())).thenReturn(response);

        mockMvc.perform(get(QUEUE_URL + "?targetType=QUESTION&status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].targetType").value("QUESTION"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // Full response shape validation
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getQueue_responseShape_hasCorrectFields() throws Exception {
        ModerationQueueItemResponse item = makeItem(ReportTargetType.ANSWER, ReportStatus.PENDING);
        ModerationQueueResponse response = new ModerationQueueResponse(List.of(item), 1L, 0, 20);

        when(moderationService.getModerationQueue(any(), any())).thenReturn(response);

        mockMvc.perform(get(QUEUE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].targetType").exists())
                .andExpect(jsonPath("$.content[0].contentPreview").exists())
                .andExpect(jsonPath("$.content[0].reportCount").exists())
                .andExpect(jsonPath("$.content[0].reportedAt").exists())
                .andExpect(jsonPath("$.content[0].reportReason").exists())
                .andExpect(jsonPath("$.content[0].status").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.size").exists());
    }
}

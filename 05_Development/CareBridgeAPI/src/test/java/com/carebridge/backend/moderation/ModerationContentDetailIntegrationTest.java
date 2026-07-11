package com.carebridge.backend.moderation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.ModerationController;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.content.dto.response.ModerationContentDetailResponse;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.service.ModerationService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
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

// DETAIL-TC-INT-001: full HTTP stack (Security -> Controller -> Service, mocked persistence) —
// follows this module's existing "integration test" convention (WebMvcTest + mocked service,
// see ModerationQueueIntegrationTest.java); this package does not use Testcontainers anywhere.
@WebMvcTest(
        value = ModerationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ModerationContentDetailIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModerationService moderationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID QUESTION_ID = UUID.fromString("22222222-0000-0000-0000-000000000001");

    // DETAIL-TC-INT-001: full body (>200 chars) survives the whole HTTP round-trip untruncated
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getContentDetail_hiddenQuestion_returnsFullBodyOverHttp() throws Exception {
        String fullBody = "z".repeat(300);
        ModerationContentDetailResponse response = new ModerationContentDetailResponse(
                QUESTION_ID, ReportTargetType.QUESTION, UUID.randomUUID(), "Nguyen Thi A",
                "Tieu de", fullBody, QuestionStatus.HIDDEN.name(), false, null, null,
                Instant.now(), Instant.now());
        when(moderationService.getContentDetail(any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/moderation/content/QUESTION/" + QUESTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body", org.hamcrest.Matchers.hasLength(300)))
                .andExpect(jsonPath("$.status").value("HIDDEN"))
                .andExpect(jsonPath("$.targetType").value("QUESTION"));
    }
}

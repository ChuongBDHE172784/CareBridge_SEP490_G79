package com.carebridge.backend.content;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.content.controller.AdminContentController;
import com.carebridge.backend.content.dto.response.CreateContentResponse;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.service.AdminContentService;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = AdminContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class AdminContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminContentService adminContentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/admin/content";

    private CreateContentResponse makeResponse(UUID id) {
        return CreateContentResponse.builder()
                .id(id)
                .type(ContentType.ARTICLE)
                .title("Dinh dưỡng thai kỳ tuần 12")
                .stage(ContentStage.PREGNANCY)
                .status("DRAFT")
                .version(1)
                .createdAt(Instant.now())
                .build();
    }

    // CNT-TC-005: MOTHER role bị từ chối 403 (TC-COND-005)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void createContent_asMotherRole_shouldReturn403() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"ARTICLE\",\"title\":\"Test\",\"stage\":\"PREGNANCY\"}"))
                .andExpect(status().isForbidden());

        verify(adminContentService, never()).createContent(any(), any());
    }

    // CNT-TC-006: title rỗng → 400 (TC-COND-006)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "CONTENT_ADMIN")
    void createContent_emptyTitle_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"ARTICLE\",\"title\":\"\",\"stage\":\"PREGNANCY\"}"))
                .andExpect(status().isBadRequest());

        verify(adminContentService, never()).createContent(any(), any());
    }

    // CNT-TC-007: type không hợp lệ → 400 (TC-COND-007)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "CONTENT_ADMIN")
    void createContent_invalidType_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"INVALID_TYPE\",\"title\":\"Test\",\"stage\":\"PREGNANCY\"}"))
                .andExpect(status().isBadRequest());

        verify(adminContentService, never()).createContent(any(), any());
    }

    // CNT-TC-009: Không có JWT → 401 (TC-COND-010)
    @Test
    void createContent_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"ARTICLE\",\"title\":\"Test\",\"stage\":\"PREGNANCY\"}"))
                .andExpect(status().isUnauthorized());
    }

    // Happy path: CONTENT_ADMIN tạo ARTICLE → 201 Created (TC-COND-001)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "CONTENT_ADMIN")
    void createContent_asContentAdmin_validRequest_shouldReturn201() throws Exception {
        UUID newId = UUID.randomUUID();
        when(adminContentService.createContent(any(), eq(UUID.fromString("00000000-0000-0000-0000-000000000001")))).thenReturn(makeResponse(newId));

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"ARTICLE\",\"title\":\"Dinh dưỡng thai kỳ tuần 12\","
                        + "\"body\":\"Nội dung chi tiết\",\"stage\":\"PREGNANCY\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.type").value("ARTICLE"));
    }

    // title thiếu (null) → 400
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "CONTENT_ADMIN")
    void createContent_missingTitle_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"ARTICLE\",\"stage\":\"PREGNANCY\"}"))
                .andExpect(status().isBadRequest());

        verify(adminContentService, never()).createContent(any(), any());
    }

    // stage thiếu → 400
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "CONTENT_ADMIN")
    void createContent_missingStage_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"ARTICLE\",\"title\":\"Test Title\"}"))
                .andExpect(status().isBadRequest());

        verify(adminContentService, never()).createContent(any(), any());
    }

    // SYSTEM_ADMIN cũng được phép → 201
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SYSTEM_ADMIN")
    void createContent_asSystemAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"ARTICLE\",\"title\":\"Test\",\"stage\":\"PREGNANCY\"}"))
                .andExpect(status().isForbidden());
    }
}

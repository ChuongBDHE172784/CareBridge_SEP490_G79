package com.carebridge.backend.community;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.community.controller.CommunityTopicController;
import com.carebridge.backend.community.dto.response.CommunityTopicResponse;
import com.carebridge.backend.community.service.CommunityTopicService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = CommunityTopicController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import(SecurityConfig.class)
class CommunityTopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommunityTopicService topicService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private static final String BASE_URL = "/api/v1/community/topics";

    private CommunityTopicResponse makeTopic(UUID id, String name, boolean hidden) {
        return CommunityTopicResponse.builder()
                .id(id)
                .name(name)
                .description("Mô tả " + name)
                .isHidden(hidden)
                .sortOrder(1)
                .createdAt(Instant.now())
                .build();
    }

    // COM-TC-008: GET topics — MOTHER thấy non-hidden topics
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void getTopics_asMotherUser_shouldReturn200WithNonHiddenTopics() throws Exception {
        UUID t1 = UUID.randomUUID();
        when(topicService.getTopics(false)).thenReturn(List.of(makeTopic(t1, "Thai kỳ", false)));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Thai kỳ"))
                .andExpect(jsonPath("$.data[0].isHidden").value(false));
    }

    // COM-TC-009: GET topics — MODERATOR với includeHidden=true thấy tất cả
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void getTopics_asModeratorWithIncludeHidden_shouldReturn200WithAllTopics() throws Exception {
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        when(topicService.getTopics(true)).thenReturn(List.of(
                makeTopic(t1, "Thai kỳ", false),
                makeTopic(t2, "Ẩn", true)));

        mockMvc.perform(get(BASE_URL).param("includeHidden", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    // GET topics — không có JWT → 401
    @Test
    void getTopics_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    // COM-TC-006: POST — MOTHER bị từ chối 403
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void createTopic_asMotherUser_shouldReturn403() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Topic\",\"sortOrder\":1}"))
                .andExpect(status().isForbidden());

        verify(topicService, never()).createTopic(any(), any());
    }

    // COM-TC-003: POST — name rỗng → 400
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void createTopic_emptyName_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"sortOrder\":0}"))
                .andExpect(status().isBadRequest());

        verify(topicService, never()).createTopic(any(), any());
    }

    // COM-TC-004: POST — name > 100 ký tự → 400
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void createTopic_nameTooLong_shouldReturn400() throws Exception {
        String longName = "A".repeat(101);
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + longName + "\",\"sortOrder\":0}"))
                .andExpect(status().isBadRequest());

        verify(topicService, never()).createTopic(any(), any());
    }

    // POST — MODERATOR tạo topic thành công → 201
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void createTopic_asModeratorValidRequest_shouldReturn201() throws Exception {
        UUID newId = UUID.randomUUID();
        CommunityTopicResponse created = makeTopic(newId, "Dinh dưỡng thai kỳ", false);
        when(topicService.createTopic(eq(1L), any())).thenReturn(created);

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Dinh dưỡng thai kỳ\",\"sortOrder\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Dinh dưỡng thai kỳ"));
    }

    // COM-TC-007: PATCH — MOTHER bị từ chối 403
    @Test
    @WithMockUser(username = "2", roles = "MOTHER")
    void updateTopic_asMotherUser_shouldReturn403() throws Exception {
        UUID topicId = UUID.randomUUID();
        mockMvc.perform(patch(BASE_URL + "/" + topicId).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isHidden\":true}"))
                .andExpect(status().isForbidden());

        verify(topicService, never()).updateTopic(any(), any());
    }

    // PATCH — MODERATOR ẩn topic → 200
    @Test
    @WithMockUser(username = "1", roles = "MODERATOR")
    void updateTopic_asModerator_shouldReturn200() throws Exception {
        UUID topicId = UUID.randomUUID();
        CommunityTopicResponse updated = makeTopic(topicId, "Thai kỳ", true);
        when(topicService.updateTopic(eq(topicId), any())).thenReturn(updated);

        mockMvc.perform(patch(BASE_URL + "/" + topicId).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isHidden\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isHidden").value(true));
    }
}

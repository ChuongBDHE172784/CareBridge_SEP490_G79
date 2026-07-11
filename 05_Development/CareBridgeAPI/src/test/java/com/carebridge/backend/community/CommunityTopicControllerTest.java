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
import com.carebridge.backend.community.dto.response.TopicFollowResponse;
import com.carebridge.backend.community.exception.CommunityTopicNotFoundException;
import com.carebridge.backend.community.exception.TopicHiddenException;
import com.carebridge.backend.community.service.CommunityTopicService;
import com.carebridge.backend.community.service.TopicFollowService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
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
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class CommunityTopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommunityTopicService topicService;

    @MockitoBean
    private TopicFollowService followService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

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
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void getTopics_asMotherUser_shouldReturn200WithNonHiddenTopics() throws Exception {
        UUID t1 = UUID.randomUUID();
        when(topicService.searchTopics(eq(null), eq(false), any())).thenReturn(List.of(makeTopic(t1, "Thai kỳ", false)));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Thai kỳ"))
                .andExpect(jsonPath("$.data[0].isHidden").value(false));
    }

    // COM-TC-009: GET topics — MODERATOR với includeHidden=true thấy tất cả
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MODERATOR")
    void getTopics_asModeratorWithIncludeHidden_shouldReturn200WithAllTopics() throws Exception {
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        when(topicService.searchTopics(eq(null), eq(true), any())).thenReturn(List.of(
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
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void createTopic_asMotherUser_shouldReturn403() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Topic\",\"sortOrder\":1}"))
                .andExpect(status().isForbidden());

        verify(topicService, never()).createTopic(any(), any());
    }

    // COM-TC-003: POST — name rỗng → 400
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MODERATOR")
    void createTopic_emptyName_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"sortOrder\":0}"))
                .andExpect(status().isBadRequest());

        verify(topicService, never()).createTopic(any(), any());
    }

    // COM-TC-004: POST — name > 100 ký tự → 400
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MODERATOR")
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
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MODERATOR")
    void createTopic_asModeratorValidRequest_shouldReturn201() throws Exception {
        UUID newId = UUID.randomUUID();
        CommunityTopicResponse created = makeTopic(newId, "Dinh dưỡng thai kỳ", false);
        when(topicService.createTopic(eq(UUID.fromString("00000000-0000-0000-0000-000000000001")), any())).thenReturn(created);

        mockMvc.perform(post(BASE_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Dinh dưỡng thai kỳ\",\"sortOrder\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Dinh dưỡng thai kỳ"));
    }

    // COM-TC-007: PATCH — MOTHER bị từ chối 403
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void updateTopic_asMotherUser_shouldReturn403() throws Exception {
        UUID topicId = UUID.randomUUID();
        mockMvc.perform(patch(BASE_URL + "/" + topicId).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isHidden\":true}"))
                .andExpect(status().isForbidden());

        verify(topicService, never()).updateTopic(any(), any(), any());
    }

    // PATCH — MODERATOR ẩn topic → 200
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MODERATOR")
    void updateTopic_asModerator_shouldReturn200() throws Exception {
        UUID topicId = UUID.randomUUID();
        CommunityTopicResponse updated = makeTopic(topicId, "Thai kỳ", true);
        when(topicService.updateTopic(eq(topicId), eq(UUID.fromString("00000000-0000-0000-0000-000000000001")), any()))
                .thenReturn(updated);

        mockMvc.perform(patch(BASE_URL + "/" + topicId).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isHidden\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isHidden").value(true));
    }

    // ===================== UC-171: Follow Topic =====================

    // TC-171-1/2: toggle follow — happy path
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void toggleFollow_validRequest_returns200() throws Exception {
        UUID topicId = UUID.randomUUID();
        when(followService.toggleFollow(eq(topicId), any()))
                .thenReturn(TopicFollowResponse.builder().topicId(topicId).followed(true).build());

        mockMvc.perform(post(BASE_URL + "/" + topicId + "/follow").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followed").value(true));
    }

    // TC-171-5: unauthenticated -> 401
    @Test
    void toggleFollow_unauthenticated_returns401() throws Exception {
        UUID topicId = UUID.randomUUID();
        mockMvc.perform(post(BASE_URL + "/" + topicId + "/follow").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // TC-171-3: topic not found -> 404
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void toggleFollow_topicNotFound_returns404() throws Exception {
        UUID topicId = UUID.randomUUID();
        when(followService.toggleFollow(eq(topicId), any()))
                .thenThrow(new CommunityTopicNotFoundException(topicId.toString()));

        mockMvc.perform(post(BASE_URL + "/" + topicId + "/follow").with(csrf()))
                .andExpect(status().isNotFound());
    }

    // TC-171-4: hidden topic -> 409
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void toggleFollow_hiddenTopic_returns409() throws Exception {
        UUID topicId = UUID.randomUUID();
        when(followService.toggleFollow(eq(topicId), any()))
                .thenThrow(new TopicHiddenException(topicId.toString()));

        mockMvc.perform(post(BASE_URL + "/" + topicId + "/follow").with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("COM-014"));
    }
}

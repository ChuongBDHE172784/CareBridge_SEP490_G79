package com.carebridge.backend.content.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.ContentController;
import com.carebridge.backend.content.dto.response.LifecycleContentEnvelope;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.service.ContentService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Real lifecycle HTTP/error contracts for TC-005/006/016/019/020/021. */
@WebMvcTest(
        value = ContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class LifecycleContentControllerTest {

    private static final String USER_ID = "69000000-0000-0000-0000-000000000001";
    private static final String CONTENT_ID = "69000000-0000-0000-0000-000000000099";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ContentService contentService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @Test
    void uc82_69_tc_005_019_motherReceivesCanonicalStageEnvelope() throws Exception {
        when(contentService.getLifecycleContents(any(), any(), any(), any()))
                .thenReturn(new LifecycleContentEnvelope<>(ContentStage.POSTPARTUM, Page.empty()));
        when(contentService.getLifecycleChecklists(any()))
                .thenReturn(new LifecycleContentEnvelope<>(ContentStage.POSTPARTUM, List.of()));

        mockMvc.perform(get("/api/v1/content/lifecycle")
                        .with(user(USER_ID).roles("MOTHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("POSTPARTUM"))
                .andExpect(jsonPath("$.data.payload.data").isArray());
        mockMvc.perform(get("/api/v1/content/lifecycle/checklists")
                        .with(user(USER_ID).roles("MOTHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("POSTPARTUM"))
                .andExpect(jsonPath("$.data.payload").isArray());
    }

    @Test
    void uc82_69_tc_005_conflictingClientStageCannotChangeCanonicalChecklistEnvelope()
            throws Exception {
        when(contentService.getLifecycleChecklists(any()))
                .thenReturn(new LifecycleContentEnvelope<>(ContentStage.POSTPARTUM, List.of()));

        mockMvc.perform(get("/api/v1/content/lifecycle/checklists")
                        .param("stage", "PRE_PREGNANCY")
                        .with(user(USER_ID).roles("MOTHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("POSTPARTUM"))
                .andExpect(jsonPath("$.data.payload").isEmpty());

        verify(contentService).getLifecycleChecklists(UUID.fromString(USER_ID));
    }

    @Test
    void uc82_69_tc_016_missingLifecycleReturnsNeutralCnt013WithoutContextFields() throws Exception {
        when(contentService.getLifecycleContents(any(), any(), any(), any()))
                .thenThrow(ContentException.lifecycleContextUnavailable());

        mockMvc.perform(get("/api/v1/content/lifecycle")
                        .with(user(USER_ID).roles("MOTHER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CNT-013"))
                .andExpect(jsonPath("$.message").value("Lifecycle content context unavailable"))
                .andExpect(content().string(not(containsString("journeyId"))))
                .andExpect(content().string(not(containsString("journeyType"))))
                .andExpect(content().string(not(containsString("journeyStatus"))));
    }

    @Test
    void uc82_69_tc_016_allThreeLifecycleRoutesReturnTheSameCnt013Contract() throws Exception {
        when(contentService.getLifecycleContents(any(), any(), any(), any()))
                .thenThrow(ContentException.lifecycleContextUnavailable());
        when(contentService.getLifecycleChecklists(any()))
                .thenThrow(ContentException.lifecycleContextUnavailable());
        when(contentService.getLifecycleContentById(any(), any()))
                .thenThrow(ContentException.lifecycleContextUnavailable());

        for (String path : List.of(
                "/api/v1/content/lifecycle",
                "/api/v1/content/lifecycle/checklists",
                "/api/v1/content/lifecycle/" + CONTENT_ID)) {
            mockMvc.perform(get(path).with(user(USER_ID).roles("MOTHER")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("CNT-013"))
                    .andExpect(jsonPath("$.message")
                            .value("Lifecycle content context unavailable"))
                    .andExpect(jsonPath("$.path").value(path))
                    .andExpect(jsonPath("$.details")
                            .value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Test
    void uc82_69_tc_006_020_directIdDenialUsesNeutralCnt003WithoutLifecycleLeak() throws Exception {
        when(contentService.getLifecycleContentById(any(), any()))
                .thenThrow(ContentException.contentNotFound());

        mockMvc.perform(get("/api/v1/content/lifecycle/{id}", CONTENT_ID)
                        .with(user(USER_ID).roles("MOTHER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("CNT-003"))
                .andExpect(jsonPath("$.message").value("Content not found or not available"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/content/lifecycle/" + CONTENT_ID))
                .andExpect(jsonPath("$.details")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(content().string(not(containsString("authorUserId"))))
                .andExpect(content().string(not(containsString("review"))))
                .andExpect(content().string(not(containsString("PREGNANCY"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1&size=20", "page=0&size=0", "page=0&size=51"})
    void uc82_69_tc_021_invalidPagingReturnsCnt001BeforeService(String query) throws Exception {
        mockMvc.perform(get("/api/v1/content/lifecycle?" + query)
                        .with(user(USER_ID).roles("MOTHER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CNT-001"));
        verifyNoInteractions(contentService);
    }

    @Test
    void uc82_69_tc_021_validTypesTopicsAndOneFiftyPageSizesReachService() throws Exception {
        UUID topicId = UUID.fromString("69000000-0000-0000-0000-000000000555");
        when(contentService.getLifecycleContents(any(), any(), any(), any()))
                .thenReturn(new LifecycleContentEnvelope<>(
                        ContentStage.PREGNANCY, Page.empty()));

        for (ContentType type : List.of(
                ContentType.ARTICLE, ContentType.FAQ, ContentType.CHECKLIST)) {
            for (int size : List.of(1, 50)) {
                mockMvc.perform(get("/api/v1/content/lifecycle")
                                .param("type", type.name())
                                .param("topicId", topicId.toString())
                                .param("page", "0")
                                .param("size", Integer.toString(size))
                                .with(user(USER_ID).roles("MOTHER")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.stage").value("PREGNANCY"))
                        .andExpect(jsonPath("$.data.payload.data").isEmpty())
                        .andExpect(jsonPath("$.data.payload.totalElements").value(0));
            }
        }

        ArgumentCaptor<ContentType> types = ArgumentCaptor.forClass(ContentType.class);
        ArgumentCaptor<UUID> topics = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<Pageable> pages = ArgumentCaptor.forClass(Pageable.class);
        verify(contentService, times(6)).getLifecycleContents(
                org.mockito.ArgumentMatchers.eq(UUID.fromString(USER_ID)),
                types.capture(), topics.capture(), pages.capture());
        assertThat(Set.copyOf(types.getAllValues()))
                .containsExactlyInAnyOrder(
                        ContentType.ARTICLE, ContentType.FAQ, ContentType.CHECKLIST);
        assertThat(topics.getAllValues()).containsOnly(topicId);
        assertThat(pages.getAllValues()).extracting(Pageable::getPageSize)
                .containsExactlyInAnyOrder(1, 50, 1, 50, 1, 50);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "type=NOT_A_TYPE", "topicId=not-a-uuid"
    })
    void uc82_69_tc_021_malformedTypeOrTopicUuidReturnsMod001BeforeService(String query)
            throws Exception {
        mockMvc.perform(get("/api/v1/content/lifecycle?" + query)
                        .with(user(USER_ID).roles("MOTHER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MOD-001"));
        verifyNoInteractions(contentService);
    }
}

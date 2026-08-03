package com.carebridge.backend.content.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.AdminContentController;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateResponse;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.service.AdminContentService;
import com.carebridge.backend.content.service.ContentService;
import io.swagger.v3.oas.annotations.media.Schema;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Real admin checklist HTTP/security contracts for TC-014/022. */
@WebMvcTest(
        value = AdminContentController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class AdminChecklistControllerTest {

    private static final String USER_ID = "69000000-0000-0000-0000-000000000001";
    private static final String URL = "/api/v1/admin/content/checklists";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ContentService contentService;
    @MockitoBean private AdminContentService adminContentService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @Test
    void r69_008_nullableLegacyColumnsAreExplicitInTheApiContract() throws Exception {
        for (String accessor : List.of("stage", "updatedAt")) {
            var method = AdminChecklistTemplateResponse.class.getDeclaredMethod(accessor);
            assertThat(method.isAnnotationPresent(Nullable.class))
                    .as("%s must be explicitly nullable for legacy rows", accessor)
                    .isTrue();
            assertThat(method.getAnnotation(Schema.class))
                    .as("%s must be explicitly nullable in OpenAPI", accessor)
                    .isNotNull()
                    .extracting(Schema::nullable)
                    .isEqualTo(true);
        }
    }

    @Test
    void uc82_69_tc_014_missingJwtIs401AndMotherIs403() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(URL).with(user(USER_ID).roles("MOTHER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(contentService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONTENT_ADMIN", "SYSTEM_ADMIN"})
    void uc82_69_tc_014_adminResponseHasRealStatusAndCountWithoutItemBodies(String role)
            throws Exception {
        var dto = new AdminChecklistTemplateResponse(
                UUID.fromString("69000000-0000-0000-0000-000000000099"),
                "Pregnancy checklist",
                ContentStage.PREGNANCY,
                ChecklistTemplateStatus.PENDING_REVIEW,
                "Review metadata only",
                7,
                Instant.parse("2026-07-23T00:00:00Z"),
                12L);
        when(contentService.getAdminChecklists(
                        eq(ContentStage.PREGNANCY), eq(ChecklistTemplateStatus.PENDING_REVIEW), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(URL)
                        .with(user(USER_ID).roles(role))
                        .param("stage", "PREGNANCY")
                        .param("status", "PENDING_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data[0].itemCount").value(12))
                .andExpect(jsonPath("$.data[0].versionNo").value(7))
                .andExpect(content().string(not(containsString("\"items\""))))
                .andExpect(content().string(not(containsString("\"itemText\""))))
                .andExpect(content().string(not(containsString("\"body\""))));

        verify(contentService).getAdminChecklists(
                eq(ContentStage.PREGNANCY), eq(ChecklistTemplateStatus.PENDING_REVIEW), eq(null), any());
    }

    @Test
    void adminResponseExposesSequencePositionAndRecipientRoles() throws Exception {
        var dto = new AdminChecklistTemplateResponse(
                UUID.fromString("69000000-0000-0000-0000-000000000100"),
                "Preconception sequence 2",
                ContentStage.PRE_PREGNANCY,
                ChecklistTemplateType.MANDATORY,
                ChecklistTemplateStatus.PENDING_REVIEW,
                "Sequence metadata",
                2,
                Instant.parse("2026-08-03T00:00:00Z"),
                1L,
                2,
                List.of(ChecklistRecipientRole.MOTHER),
                null);
        when(contentService.getAdminChecklists(
                        eq(ContentStage.PRE_PREGNANCY), eq(ChecklistTemplateStatus.PENDING_REVIEW), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(URL)
                        .with(user(USER_ID).roles("SYSTEM_ADMIN"))
                        .param("stage", "PRE_PREGNANCY")
                        .param("status", "PENDING_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].displayOrder").value(2))
                .andExpect(jsonPath("$.data[0].recipientRoles[0]").value("MOTHER"));
    }

    @Test
    void checklistKeywordIsForwardedToTheAdminQuery() throws Exception {
        when(contentService.getAdminChecklists(eq(null), eq(null), eq("pregnancy"), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(URL)
                        .with(user(USER_ID).roles("CONTENT_ADMIN"))
                        .param("keyword", "pregnancy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(contentService).getAdminChecklists(eq(null), eq(null), eq("pregnancy"), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1&size=20", "page=0&size=0", "page=0&size=51"})
    void uc82_69_tc_022_invalidPagingReturnsCnt001BeforeService(String query) throws Exception {
        mockMvc.perform(get(URL + "?" + query)
                        .with(user(USER_ID).roles("CONTENT_ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CNT-001"));
        verifyNoInteractions(contentService);
    }

    @Test
    void uc82_69_tc_022_invalidStatusEnumReturnsExact400ContractBeforeService() throws Exception {
        mockMvc.perform(get(URL)
                        .with(user(USER_ID).roles("CONTENT_ADMIN"))
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MOD-001"));
        verifyNoInteractions(contentService);
    }

    @Test
    void uc82_69_tc_022_sizeOneAndFiftySucceedWithStageAndStatusFilters() throws Exception {
        when(contentService.getAdminChecklists(
                        eq(ContentStage.PREGNANCY), eq(ChecklistTemplateStatus.APPROVED), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of()));

        for (int size : List.of(1, 50)) {
            mockMvc.perform(get(URL)
                            .with(user(USER_ID).roles("CONTENT_ADMIN"))
                            .param("stage", "PREGNANCY")
                            .param("status", "APPROVED")
                            .param("size", Integer.toString(size)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        verify(contentService, times(2)).getAdminChecklists(
                eq(ContentStage.PREGNANCY), eq(ChecklistTemplateStatus.APPROVED), eq(null), any());
    }

    @Test
    void uc82_69_tc_022_sizeFiftyOneFailsForEveryFilterCombination() throws Exception {
        for (String query : List.of(
                "size=51&stage=PREGNANCY",
                "size=51&status=APPROVED",
                "size=51&stage=PREGNANCY&status=APPROVED")) {
            mockMvc.perform(get(URL + "?" + query)
                            .with(user(USER_ID).roles("SYSTEM_ADMIN")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("CNT-001"));
        }
        verifyNoInteractions(contentService);
    }

    @Test
    void uc82_69_tc_022_invalidStageEnumReturnsMod001BeforeService() throws Exception {
        mockMvc.perform(get(URL)
                        .with(user(USER_ID).roles("CONTENT_ADMIN"))
                        .param("stage", "NOT_A_STAGE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MOD-001"));
        verifyNoInteractions(contentService);
    }
}

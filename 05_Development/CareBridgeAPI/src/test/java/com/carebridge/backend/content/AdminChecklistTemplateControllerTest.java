package com.carebridge.backend.content;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.content.controller.AdminChecklistTemplateController;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateDetailResponse;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import java.util.List;
import java.util.UUID;
import com.carebridge.backend.content.service.AdminChecklistTemplateService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// CHKTPL-TC-003, CHKTPL-TC-009, CHKTPL-TC-013 (partial — list access)
@WebMvcTest(
        value = AdminChecklistTemplateController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class AdminChecklistTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminChecklistTemplateService adminChecklistTemplateService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private static final String BASE_URL = "/api/v1/admin/checklist-templates";

    // CHKTPL-TC-003: name blank -> 400
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "CONTENT_ADMIN")
    void create_blankName_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"stage\":\"PREGNANCY\",\"items\":[]}"))
                .andExpect(status().isBadRequest());

        verify(adminChecklistTemplateService, never()).create(any(), any());
    }

    // CHKTPL-TC-009a: MOTHER cannot create
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void create_asMother_shouldReturn403() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"stage\":\"PREGNANCY\",\"items\":[]}"))
                .andExpect(status().isForbidden());

        verify(adminChecklistTemplateService, never()).create(any(), any());
    }

    // CHKTPL-TC-009b: SYSTEM_ADMIN cannot create directly (write is CONTENT_ADMIN-only, ADR-CHK-001)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000003", roles = "SYSTEM_ADMIN")
    void create_asSystemAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(post(BASE_URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"stage\":\"PREGNANCY\",\"items\":[]}"))
                .andExpect(status().isForbidden());
    }

    // CHKTPL-TC-009c: MOTHER cannot even list
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "MOTHER")
    void list_asMother_shouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isForbidden());
    }

    // CHKTPL-TC-013 (list part): SYSTEM_ADMIN CAN list (§14 addendum — read open to both roles)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000003", roles = "SYSTEM_ADMIN")
    void list_asSystemAdmin_shouldReturn200() throws Exception {
        Page<AdminChecklistTemplateDetailResponse> page = new PageImpl<>(java.util.List.of());
        when(adminChecklistTemplateService.list(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE_URL)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000003", roles = "SYSTEM_ADMIN")
    void list_adminContractRetainsTrueTemplateStatus() throws Exception {
        AdminChecklistTemplateDetailResponse template = AdminChecklistTemplateDetailResponse.builder()
                .id(UUID.fromString("69000000-0000-0000-0000-000000000701"))
                .name("Draft checklist")
                .stage(ContentStage.PREGNANCY)
                .status(ChecklistTemplateStatus.DRAFT)
                .items(List.of())
                .build();
        when(adminChecklistTemplateService.list(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(template)));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000003", roles = "SYSTEM_ADMIN")
    void list_passesKeywordToService() throws Exception {
        when(adminChecklistTemplateService.list(any(), any(), eq("thai"), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE_URL).param("keyword", "thai"))
                .andExpect(status().isOk());

        verify(adminChecklistTemplateService).list(any(), any(), eq("thai"), any());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "CONTENT_ADMIN")
    void getById_adminContractIncludesPersistedVersion() throws Exception {
        UUID templateId = UUID.fromString("69000000-0000-0000-0000-000000000702");
        AdminChecklistTemplateDetailResponse template = AdminChecklistTemplateDetailResponse.builder()
                .id(templateId)
                .name("Versioned checklist")
                .stage(ContentStage.PREGNANCY)
                .status(ChecklistTemplateStatus.DRAFT)
                .versionNo(3)
                .items(List.of())
                .build();
        when(adminChecklistTemplateService.getById(templateId)).thenReturn(template);

        mockMvc.perform(get(BASE_URL + "/" + templateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionNo").value(3));
    }
}

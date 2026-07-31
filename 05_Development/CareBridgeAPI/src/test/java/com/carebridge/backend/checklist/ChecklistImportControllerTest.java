package com.carebridge.backend.checklist;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.checklist.controller.UserChecklistItemController;
import com.carebridge.backend.checklist.dto.ChecklistItemResponse;
import com.carebridge.backend.checklist.service.IUserChecklistItemService;
import com.carebridge.backend.checklist.service.UserCreatedChecklistTaskService;
import com.carebridge.backend.checklist.service.ChecklistV2CompatibilityMutationService;
import com.carebridge.backend.checklist.service.OptionalChecklistTemplateImportService;
import com.carebridge.backend.checklist.distribution.ChecklistDistributionResult;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.security.config.SecurityConfig;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP cutover contract: canonical V2 creation is required and legacy import is gone. */
@WebMvcTest(value = UserChecklistItemController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ChecklistImportControllerTest {
    private static final UUID USER_ID = UUID.fromString("69000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private IUserChecklistItemService checklistService;
    @MockitoBean private UserCreatedChecklistTaskService userCreatedTaskService;
    @MockitoBean private ChecklistV2CompatibilityMutationService v2MutationService;
    @MockitoBean private OptionalChecklistTemplateImportService optionalTemplateImportService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @Test
    void validLegacyImportReturnsGoneWithoutCallingLegacyService() throws Exception {
        mockMvc.perform(post("/api/v1/user-checklist-items/import").with(csrf())
                        .with(user(USER_ID.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"journeyId\":\"%s\",\"templateItemIds\":[\"%s\"]}"
                                .formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value("CHECKLIST_LEGACY_ROUTE_RETIRED"));
        verifyNoInteractions(checklistService, userCreatedTaskService);
    }

    @Test
    void canonicalCreateDelegatesToRequiredV2Service() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID journeyId = UUID.randomUUID();
        UUID clientTaskId = UUID.randomUUID();
        org.mockito.Mockito.when(userCreatedTaskService.create(any(), eq(USER_ID)))
                .thenReturn(new ChecklistItemResponse(taskId, USER_ID, journeyId, null,
                        null, null, false, "Pack water", "GENERAL", false, null, 0,
                        Instant.parse("2026-07-30T00:00:00Z"), "MOTHER", "USER_CREATED"));

        mockMvc.perform(post("/api/v1/user-checklist-items").with(csrf())
                        .with(user(USER_ID.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"journeyId":"%s","itemText":"Pack water","targetSubject":"MOTHER",
                                 "clientTaskId":"%s"}
                                """.formatted(journeyId, clientTaskId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.itemId").value(taskId.toString()))
                .andExpect(jsonPath("$.data.origin").value("USER_CREATED"));
        verify(userCreatedTaskService).create(any(), eq(USER_ID));
        verifyNoInteractions(checklistService);
    }

    @Test
    void optionalTemplateSelfAssignmentDelegatesToCanonicalV2Service() throws Exception {
        UUID templateId = UUID.randomUUID();
        UUID journeyId = UUID.randomUUID();
        org.mockito.Mockito.when(optionalTemplateImportService.selfAssign(any(), eq(USER_ID)))
                .thenReturn(ChecklistDistributionResult.created(1, 2));

        mockMvc.perform(post("/api/v1/user-checklist-items/from-template").with(csrf())
                        .with(user(USER_ID.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":\"%s\",\"journeyId\":\"%s\"}"
                                .formatted(templateId, journeyId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.createdInstances").value(1))
                .andExpect(jsonPath("$.data.createdTasks").value(2));
        verify(optionalTemplateImportService).selfAssign(any(), eq(USER_ID));
    }

    @Test
    void v2SystemTaskUpdateAndDeleteReturnImmutableThroughCompatibilityAdapter() throws Exception {
        UUID taskId = UUID.randomUUID();
        BusinessException immutable = new BusinessException(
                HttpStatus.CONFLICT, "SYSTEM_TASK_IMMUTABLE", "System tasks cannot be edited or deleted");
        doThrow(immutable).when(v2MutationService).rejectUpdate(taskId, USER_ID);
        doThrow(immutable).when(v2MutationService).rejectDelete(taskId, USER_ID);

        mockMvc.perform(put("/api/v1/user-checklist-items/{id}", taskId).with(csrf())
                        .with(user(USER_ID.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"itemText\":\"Changed\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SYSTEM_TASK_IMMUTABLE"));
        mockMvc.perform(delete("/api/v1/user-checklist-items/{id}", taskId).with(csrf())
                        .with(user(USER_ID.toString()).roles("MOTHER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SYSTEM_TASK_IMMUTABLE"));
        verifyNoInteractions(checklistService);
    }
}

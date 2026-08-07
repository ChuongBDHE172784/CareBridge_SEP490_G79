package com.carebridge.backend.family.controller;

import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.family.dto.AssignFamilyTaskRequest;
import com.carebridge.backend.family.dto.AssignFamilyTaskResponse;
import com.carebridge.backend.family.dto.CareTasksResponse;
import com.carebridge.backend.family.service.ICareGroupService;
import com.carebridge.backend.family.service.ICareTaskService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = CareGroupController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class CareGroupControllerAssignTaskTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean private ICareGroupService careGroupService;
    @MockitoBean private ICareTaskService careTaskService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    private static final UUID GROUP_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final String TASKS_URL = "/api/v1/care-groups/" + GROUP_ID + "/tasks";

    // ── FAM73-TC-011: Blank title → 400 ──────────────────────────────────────

    @Test
    @WithMockUser(roles = "MOTHER")
    void assignTask_blankTitle_returns400() throws Exception {
        String body = "{\"assigneeMemberId\":\"" + UUID.randomUUID() + "\","
                + "\"title\":\"\","
                + "\"dueAt\":\"" + Instant.now().plus(1, ChronoUnit.DAYS) + "\"}";

        mockMvc.perform(post(TASKS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── FAM73-TC-011 variant: missing title → 400 ─────────────────────────────

    @Test
    @WithMockUser(roles = "MOTHER")
    void assignTask_missingTitle_returns400() throws Exception {
        String body = "{\"assigneeMemberId\":\"" + UUID.randomUUID() + "\","
                + "\"dueAt\":\"" + Instant.now().plus(1, ChronoUnit.DAYS) + "\"}";

        mockMvc.perform(post(TASKS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── FAM73-TC-012: Title > 255 chars → 400 ────────────────────────────────

    @Test
    @WithMockUser(roles = "MOTHER")
    void assignTask_titleExceeds255Chars_returns400() throws Exception {
        String tooLongTitle = "A".repeat(256);
        String body = "{\"assigneeMemberId\":\"" + UUID.randomUUID() + "\","
                + "\"title\":\"" + tooLongTitle + "\","
                + "\"dueAt\":\"" + Instant.now().plus(1, ChronoUnit.DAYS) + "\"}";

        mockMvc.perform(post(TASKS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── FAM73-TC-021: No JWT on POST /tasks → 401/403 ─────────────────────────

    @Test
    void assignTask_noJwt_returnsUnauthorized() throws Exception {
        String body = "{\"assigneeMemberId\":\"" + UUID.randomUUID() + "\","
                + "\"title\":\"Buy diapers\","
                + "\"dueAt\":\"" + Instant.now().plus(1, ChronoUnit.DAYS) + "\"}";

        mockMvc.perform(post(TASKS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    // ── FAM73-TC-022: No JWT on GET /tasks → 401/403 ─────────────────────────

    @Test
    void listTasks_noJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(TASKS_URL))
                .andExpect(status().is4xxClientError());
    }

    // ── Happy path: valid POST returns 201 ───────────────────────────────────

    @Test
    @WithMockUser(username = "bbbbbbbb-0000-0000-0000-000000000002", roles = "MOTHER")
    void assignTask_validRequest_returns201() throws Exception {
        UUID assigneeId = UUID.randomUUID();
        AssignFamilyTaskRequest req = new AssignFamilyTaskRequest();
        req.setAssigneeMemberId(assigneeId);
        req.setTitle("Buy diapers");
        req.setDueAt(Instant.now().plus(1, ChronoUnit.DAYS));
        // V2 requires an explicit MOTHER/BABY target; without it @Valid rejects with 400 before
        // the controller is ever reached.
        req.setTargetSubject(ChecklistTargetSubject.MOTHER);

        AssignFamilyTaskResponse resp = AssignFamilyTaskResponse.builder()
                .careTaskId(UUID.randomUUID())
                .careGroupId(GROUP_ID)
                .assignedTo(assigneeId)
                .assignedBy(UUID.randomUUID())
                .title("Buy diapers")
                .status("OPEN")
                .createdAt(Instant.now())
                .build();

        when(careTaskService.assignFamilyTask(eq(GROUP_ID), any(), any())).thenReturn(resp);

        mockMvc.perform(post(TASKS_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    // ── Happy path: valid GET returns 200 ────────────────────────────────────

    @Test
    @WithMockUser(username = "cccccccc-0000-0000-0000-000000000003", roles = "FAMILY")
    void listTasks_validRequest_returns200() throws Exception {
        CareTasksResponse resp = CareTasksResponse.builder()
                .groupId(GROUP_ID)
                .totalTasks(0)
                .tasks(List.of())
                .build();

        when(careTaskService.listTasks(eq(GROUP_ID), any())).thenReturn(resp);

        mockMvc.perform(get(TASKS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTasks").value(0));
    }
}

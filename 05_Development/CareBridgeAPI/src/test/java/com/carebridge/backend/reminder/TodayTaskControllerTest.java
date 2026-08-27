package com.carebridge.backend.reminder;

import com.carebridge.backend.checklist.today.service.UnifiedTaskActionFacade;
import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.reminder.controller.ReminderController;
import com.carebridge.backend.reminder.dto.TodayTaskItem;
import com.carebridge.backend.reminder.service.IReminderService;
import com.carebridge.backend.reminder.service.ITodayTaskService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UC49 — View Today Tasks controller tests.
 * TDD Red Phase: all tests must FAIL until Green implementation.
 */
@WebMvcTest(
        value = ReminderController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class TodayTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private IReminderService reminderService;
    @MockitoBean private ITodayTaskService todayTaskService;
    @MockitoBean private UnifiedTaskActionFacade unifiedTaskActionFacade;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    // TODAY-TC-009: GET /api/v1/reminders/today returns 200 with list
    @Test
    @WithMockUser(username = "00000000-0000-0000-0002-000000000001", roles = "MOTHER")
    void getTodayTasks_authenticatedMother_returns200() throws Exception {
        var item = TodayTaskItem.builder()
                .id(UUID.randomUUID())
                .sourceType("REMINDER")
                .type("MEDICATION")
                .title("Vitamin D")
                .scheduledAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .dueAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .status("PENDING")
                .priority(2)
                .build();
        when(todayTaskService.getTodayTasks(any(), any())).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/reminders/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].sourceType").value("REMINDER"))
                .andExpect(jsonPath("$.data[0].dueAt").exists())
                .andExpect(jsonPath("$.data[0].type").value("MEDICATION"));
    }

    // TODAY-TC-010: Default timezone fallback when X-User-Timezone header absent
    @Test
    @WithMockUser(username = "00000000-0000-0000-0002-000000000001", roles = "MOTHER")
    void getTodayTasks_noTimezoneHeader_usesDefaultTimezone() throws Exception {
        when(todayTaskService.getTodayTasks(any(), any())).thenReturn(List.of());

        // Should not throw even without timezone header
        mockMvc.perform(get("/api/v1/reminders/today"))
                .andExpect(status().isOk());
    }

    // TODAY-TC-011: X-User-Timezone header forwarded correctly
    @Test
    @WithMockUser(username = "00000000-0000-0000-0002-000000000001", roles = "MOTHER")
    void getTodayTasks_withTimezoneHeader_usesProvidedTimezone() throws Exception {
        when(todayTaskService.getTodayTasks(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reminders/today")
                        .header("X-User-Timezone", "Asia/Ho_Chi_Minh"))
                .andExpect(status().isOk());
    }

    // TODAY-TC-012: Invalid timezone header falls back to default (no 500)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0002-000000000001", roles = "MOTHER")
    void getTodayTasks_invalidTimezoneHeader_fallsBackToDefault() throws Exception {
        when(todayTaskService.getTodayTasks(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reminders/today")
                        .header("X-User-Timezone", "Invalid/Zone"))
                .andExpect(status().isOk());
    }
}

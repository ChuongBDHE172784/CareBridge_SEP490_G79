package com.carebridge.backend.reminder;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.reminder.controller.ReminderController;
import com.carebridge.backend.reminder.service.IReminderService;
import com.carebridge.backend.reminder.service.ITodayTaskService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC46/UC47/UC48/UC49 — Security tests (RBAC).
 * Verifies: unauthenticated → 401, non-MOTHER role → 403.
 */
@WebMvcTest(
        value = ReminderController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class ReminderSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private IReminderService reminderService;
    @MockitoBean private ITodayTaskService todayTaskService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    // TC-SEC-001: All reminder endpoints → 401 when unauthenticated
    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/reminders/medication",
            "/api/v1/reminders/vaccination",
            "/api/v1/reminders/today",
            "/api/v1/reminders/vaccination/suggestions?babyId=00000000-0000-0000-0000-000000000001"
    })
    void allReminderEndpoints_unauthenticated_returns401(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/reminders/00000000-0000-0000-0000-000000000001/snooze",
            "/api/v1/reminders/00000000-0000-0000-0000-000000000001/complete",
            "/api/v1/reminders/00000000-0000-0000-0000-000000000001/skip",
            "/api/v1/reminders/00000000-0000-0000-0000-000000000001/enable"
    })
    void patchReminderEndpoints_unauthenticated_returns401(String path) throws Exception {
        mockMvc.perform(patch(path))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/reminders/00000000-0000-0000-0000-000000000001",
            "/api/v1/reminders/00000000-0000-0000-0000-000000000001/permanent"
    })
    void deleteReminderEndpoint_unauthenticated_returns401(String path) throws Exception {
        mockMvc.perform(delete(path))
                .andExpect(status().isUnauthorized());
    }

    // TC-SEC-002: Non-MOTHER role → 403 on MOTHER-only endpoints
    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/reminders/today"})
    @WithMockUser(roles = "EXPERT")
    void todayTasks_expertRole_returns403(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/reminders/today"})
    @WithMockUser(roles = "FAMILY")
    void todayTasks_familyRole_returns403(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isForbidden());
    }
}

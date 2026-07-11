package com.carebridge.backend.family;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.family.dto.SharedCareCalendarResponse;
import com.carebridge.backend.family.service.ICareCalendarService;
import com.carebridge.backend.family.service.ICareGroupService;
import com.carebridge.backend.family.service.ICareTaskService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.carebridge.backend.family.CareCalendarTestFactory.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E / controller-layer tests for GET /api/v1/care-groups/{groupId}/calendar.
 *
 * Tests: FAM-UC74-TC-012 (valid JWT → 200), FAM-UC74-TC-013 (no JWT → 401).
 * Uses @WebMvcTest with mocked ICareCalendarService.
 */
@WebMvcTest(
        value = com.carebridge.backend.family.controller.CareGroupController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class CareCalendarE2ETest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ICareGroupService careGroupService;
    @MockitoBean private ICareTaskService careTaskService;
    @MockitoBean private ICareCalendarService careCalendarService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    private static final String CALENDAR_URL =
            "/api/v1/care-groups/" + GROUP_CG_001 + "/calendar"
            + "?rangeStart=2026-07-01T00:00:00Z&rangeEnd=2026-07-31T23:59:59Z";

    // ── FAM-UC74-TC-012: Valid JWT → 200 ─────────────────────────────────────

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000a01", roles = "MOTHER")
    void getCalendar_validJwt_returns200() throws Exception {
        SharedCareCalendarResponse resp = SharedCareCalendarResponse.builder()
                .groupId(GROUP_CG_001)
                .groupName("Test Care Group")
                .rangeStart(RANGE_START)
                .rangeEnd(RANGE_END)
                .totalItems(1)
                .items(List.of())
                .build();

        when(careCalendarService.getCalendar(eq(GROUP_CG_001), any(), any(), any()))
                .thenReturn(resp);

        mockMvc.perform(get(CALENDAR_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value(GROUP_CG_001.toString()))
                .andExpect(jsonPath("$.data.totalItems").value(1));
    }

    // ── FAM-UC74-TC-013: No JWT → 401 ────────────────────────────────────────

    @Test
    void getCalendar_noJwt_returns401() throws Exception {
        mockMvc.perform(get(CALENDAR_URL))
                .andExpect(status().isUnauthorized());
    }
}

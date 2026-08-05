package com.carebridge.backend.checklist.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.checklist.today.dto.TodayTaskCounts;
import com.carebridge.backend.checklist.today.dto.TodayTaskSections;
import com.carebridge.backend.checklist.today.dto.TodayTasksResponse;
import com.carebridge.backend.checklist.today.model.TaskKind;
import com.carebridge.backend.checklist.today.service.CurrentChecklistServiceImpl;
import com.carebridge.backend.checklist.today.service.UnifiedTodayTaskService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentChecklistServiceImplTest {

    private static final UUID ACTOR = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CORRELATION = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final String ZONE = "Asia/Ho_Chi_Minh";

    @Test
    void currentChecklistReadRequestsReconciliationBeforeProjection() {
        UnifiedTodayTaskService unifiedTodayTaskService = mock(UnifiedTodayTaskService.class);
        TodayTasksResponse response = new TodayTasksResponse(
                Instant.parse("2026-08-05T01:00:00Z"),
                ZONE,
                7,
                new TodayTaskSections(List.of(), List.of(), List.of(), List.of()),
                new TodayTaskCounts(0, 0, 0, 0),
                CORRELATION,
                null);
        when(unifiedTodayTaskService.getTodayTasks(
                ACTOR, DATE, ZONE, Set.of(TaskKind.CHECKLIST), true)).thenReturn(response);

        var result = new CurrentChecklistServiceImpl(unifiedTodayTaskService)
                .getCurrentTasks(ACTOR, DATE, ZONE);

        verify(unifiedTodayTaskService).getTodayTasks(
                ACTOR, DATE, ZONE, Set.of(TaskKind.CHECKLIST), true);
        assertThat(result.correlationId()).isEqualTo(CORRELATION);
        assertThat(result.counts()).isEqualTo(response.counts());
    }

    @Test
    void familyCurrentChecklistReadDoesNotRequestReconciliation() {
        UnifiedTodayTaskService unifiedTodayTaskService = mock(UnifiedTodayTaskService.class);
        TodayTasksResponse response = new TodayTasksResponse(
                Instant.parse("2026-08-05T01:00:00Z"),
                ZONE,
                7,
                new TodayTaskSections(List.of(), List.of(), List.of(), List.of()),
                new TodayTaskCounts(0, 0, 0, 0),
                CORRELATION,
                null);
        when(unifiedTodayTaskService.getTodayTasks(
                ACTOR, DATE, ZONE, Set.of(TaskKind.CHECKLIST), false)).thenReturn(response);

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                ACTOR.toString(), "n/a", List.of(new SimpleGrantedAuthority("ROLE_FAMILY"))));
        SecurityContextHolder.setContext(context);
        try {
            var result = new CurrentChecklistServiceImpl(unifiedTodayTaskService)
                    .getCurrentTasks(ACTOR, DATE, ZONE);

            verify(unifiedTodayTaskService).getTodayTasks(
                    ACTOR, DATE, ZONE, Set.of(TaskKind.CHECKLIST), false);
            assertThat(result.correlationId()).isEqualTo(CORRELATION);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

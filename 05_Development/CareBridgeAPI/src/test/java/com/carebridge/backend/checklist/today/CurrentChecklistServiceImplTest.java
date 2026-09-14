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

    @Test
    void mapsReviewerToCurrentChecklistTaskResponse() {
        UnifiedTodayTaskService unifiedTodayTaskService = mock(UnifiedTodayTaskService.class);
        var reviewer = com.carebridge.backend.content.dto.response.ExpertReviewerResponse.builder()
                .expertId(UUID.randomUUID())
                .name("BS Đỗ Hải Long")
                .professionalTitle("BS.CKII")
                .specialty("Sản khoa")
                .workplace("Bệnh viện Từ Dũ")
                .build();
        var taskItem = new com.carebridge.backend.checklist.today.dto.TodayTaskItemResponse(
                TaskKind.CHECKLIST,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                null,
                "Khám tiền sản",
                null,
                com.carebridge.backend.checklist.model.ChecklistOrigin.SYSTEM_TEMPLATE,
                "PENDING",
                com.carebridge.backend.checklist.today.model.TaskTimeBucket.TODAY,
                Set.of(),
                Instant.now(),
                null,
                "Mô tả khám",
                null,
                null,
                null,
                "https://example.com/source.pdf",
                reviewer);

        TodayTasksResponse response = new TodayTasksResponse(
                Instant.parse("2026-08-05T01:00:00Z"),
                ZONE,
                7,
                new TodayTaskSections(List.of(), List.of(taskItem), List.of(), List.of()),
                new TodayTaskCounts(0, 1, 0, 0),
                CORRELATION,
                null);
        when(unifiedTodayTaskService.getTodayTasks(
                ACTOR, DATE, ZONE, Set.of(TaskKind.CHECKLIST), true)).thenReturn(response);

        var result = new CurrentChecklistServiceImpl(unifiedTodayTaskService)
                .getCurrentTasks(ACTOR, DATE, ZONE);

        assertThat(result.sections().today()).hasSize(1);
        var mappedTask = result.sections().today().get(0);
        assertThat(mappedTask.reviewer()).isEqualTo(reviewer);
        assertThat(mappedTask.sourceUrl()).isEqualTo("https://example.com/source.pdf");
    }
}

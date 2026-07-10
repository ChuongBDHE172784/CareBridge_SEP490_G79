package com.carebridge.backend.family;

import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.carebridge.backend.family.CareCalendarTestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CareTaskRepository — UC-74 date-range query correctness.
 *
 * Tests: FAM-UC74-TC-010, FAM-UC74-TC-011
 * Uses Testcontainers PostgreSQL via AbstractPostgresIntegrationTest.
 */
@Transactional
class CareTaskRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private CareGroupRepository groupRepository;
    @Autowired private CareTaskRepository careTaskRepository;

    private UUID group1Id;
    private UUID group2Id;
    private UUID taskInRangeId;

    @BeforeEach
    void setUp() {
        // Create two groups
        CareGroup group1 = groupRepository.save(CareGroup.builder()
                .ownerUserId(ACC_001)
                .groupName("Group A")
                .status(CareGroupStatus.ACTIVE)
                .build());
        group1Id = group1.getId();

        CareGroup group2 = groupRepository.save(CareGroup.builder()
                .ownerUserId(ACC_002)
                .groupName("Group B")
                .status(CareGroupStatus.ACTIVE)
                .build());
        group2Id = group2.getId();

        // FX-006: task in group1, dueAt = JULY_TASK_DUE_AT (in range)
        CareTask taskInRange = careTaskRepository.save(makeTask(group1Id, JULY_TASK_DUE_AT));
        taskInRangeId = taskInRange.getId();

        // FX-007: task in group1, dueAt = AUGUST_TASK_DUE_AT (out of July range)
        careTaskRepository.save(makeTask(group1Id, AUGUST_TASK_DUE_AT));

        // FX-008: task in group2, dueAt = JULY_TASK_DUE_AT (in range but wrong group)
        careTaskRepository.save(makeTask(group2Id, JULY_TASK_DUE_AT));
    }

    // ── FAM-UC74-TC-010: Query respects date-range boundaries ────────────────

    @Test
    void findByCareGroupIdAndDueAtBetween_onlyReturnsTasksInRange() {
        List<CareTask> results = careTaskRepository
                .findByCareGroupIdAndDueAtBetween(group1Id, RANGE_START, RANGE_END);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(taskInRangeId);
        // FX-007 (August task) must NOT appear
        assertThat(results).noneMatch(t -> t.getDueAt().equals(AUGUST_TASK_DUE_AT));
    }

    // ── FAM-UC74-TC-011: Query excludes tasks from other groups ───────────────

    @Test
    void findByCareGroupIdAndDueAtBetween_excludesOtherGroupTasks() {
        List<CareTask> results = careTaskRepository
                .findByCareGroupIdAndDueAtBetween(group1Id, RANGE_START, RANGE_END);

        // FX-008 (group2 task with same dueAt) must NOT appear
        assertThat(results).allMatch(t -> t.getCareGroupId().equals(group1Id));
        assertThat(results).noneMatch(t -> t.getCareGroupId().equals(group2Id));
    }
}

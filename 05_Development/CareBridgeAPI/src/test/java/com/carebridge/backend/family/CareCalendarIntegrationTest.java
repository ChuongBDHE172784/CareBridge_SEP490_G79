package com.carebridge.backend.family;

import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.family.service.ICareCalendarService;
import com.carebridge.backend.family.dto.SharedCareCalendarResponse;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.carebridge.backend.family.CareCalendarTestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full integration test for CareCalendarServiceImpl.
 * Covers: FAM-UC74-TC-INT-001 — Service + Repository + PostgreSQL coordination.
 *
 * Tests: in-range task visible, out-of-range task excluded, cross-group task excluded.
 */
@Transactional
class CareCalendarIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private CareGroupRepository groupRepository;
    @Autowired private CareGroupMemberRepository memberRepository;
    @Autowired private CareTaskRepository careTaskRepository;
    @Autowired private ICareCalendarService careCalendarService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID group1Id;
    private UUID group2Id;
    private UUID callerId;
    private UUID inRangeTaskId;

    @BeforeEach
    void setUp() {
        callerId = UUID.randomUUID();
        UUID group2OwnerId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(jdbcTemplate, callerId, "Calendar caller", null, "MOTHER");
        CanonicalUserFixture.insertUser(jdbcTemplate, group2OwnerId, "Other owner", null, "MOTHER");
        CanonicalUserFixture.insertUser(jdbcTemplate, ACC_001, "Task creator", null, "MOTHER");
        CanonicalUserFixture.insertUser(jdbcTemplate, ACC_002, "Task assignee", null, "FAMILY");

        // Create two groups
        CareGroup group1 = groupRepository.save(CareGroup.builder()
                .ownerUserId(callerId)
                .groupName("Integration Group A")
                .status(CareGroupStatus.ACTIVE)
                .build());
        group1Id = group1.getId();

        CareGroup group2 = groupRepository.save(CareGroup.builder()
                .ownerUserId(group2OwnerId)
                .groupName("Integration Group B")
                .status(CareGroupStatus.ACTIVE)
                .build());
        group2Id = group2.getId();

        // Create caller as OWNER/ACCEPTED member of group1 with calendar=true permission
        memberRepository.save(CareGroupMember.builder()
                .careGroupId(group1Id)
                .userId(callerId)
                .memberRole(GroupMemberRole.OWNER)
                .inviteStatus(InviteStatus.ACCEPTED)
                .permissionJson("{\"calendar\":true}")
                .build());

        // FX-006: task in group1 — in range (July 2026)
        CareTask inRangeTask = careTaskRepository.save(makeTask(group1Id, JULY_TASK_DUE_AT));
        inRangeTaskId = inRangeTask.getId();

        // FX-007: task in group1 — out of range (August 2026)
        careTaskRepository.save(makeTask(group1Id, AUGUST_TASK_DUE_AT));

        // FX-008: task in group2 — in range but different group
        careTaskRepository.save(makeTask(group2Id, JULY_TASK_DUE_AT));
    }

    // ── FAM-UC74-TC-INT-001 ───────────────────────────────────────────────────

    @Test
    void getCalendar_fullFlow_returnsOnlyInRangeOwnGroupTask() {
        SharedCareCalendarResponse response =
                careCalendarService.getCalendar(group1Id, callerId, RANGE_START, RANGE_END);

        // Only 1 item: FX-006 — in-range, own group
        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getTaskId()).isEqualTo(inRangeTaskId);

        // DB assertion
        List<CareTask> tasks = careTaskRepository
                .findByCareGroupIdAndDueAtBetween(group1Id, RANGE_START, RANGE_END);
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getCareGroupId()).isEqualTo(group1Id);
    }
}

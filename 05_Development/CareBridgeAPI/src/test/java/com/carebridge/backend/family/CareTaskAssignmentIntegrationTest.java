package com.carebridge.backend.family;

import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.dto.AssignFamilyTaskRequest;
import com.carebridge.backend.family.dto.AssignFamilyTaskResponse;
import com.carebridge.backend.family.dto.CareTasksResponse;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.family.service.ICareTaskService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for UC-73 Assign Family Task.
 * Tests FAM73-TC-INT-001: full assign + list flow with real PostgreSQL.
 */
@Transactional
class CareTaskAssignmentIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private CareGroupRepository groupRepository;
    @Autowired private CareGroupMemberRepository memberRepository;
    @Autowired private CareTaskRepository careTaskRepository;
    @Autowired private ICareTaskService careTaskService;

    private UUID groupId;
    private UUID ownerId;
    private UUID assigneeId;

    @BeforeEach
    void setUp() {
        ownerId   = UUID.randomUUID();
        assigneeId = UUID.randomUUID();

        // Seed care group
        CareGroup group = groupRepository.save(CareGroup.builder()
                .ownerUserId(ownerId)
                .groupName("Integration Task Group")
                .status(CareGroupStatus.ACTIVE)
                .build());
        groupId = group.getId();

        // Seed OWNER membership (FX-002)
        memberRepository.save(CareGroupMember.builder()
                .careGroupId(groupId)
                .userId(ownerId)
                .memberRole(GroupMemberRole.OWNER)
                .inviteStatus(InviteStatus.ACCEPTED)
                .build());

        // Seed ASSIGNEE membership (FX-003)
        memberRepository.save(CareGroupMember.builder()
                .careGroupId(groupId)
                .userId(assigneeId)
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.ACCEPTED)
                .build());
    }

    // ── FAM73-TC-INT-001: assign then list, DB state assertion ───────────────

    @Test
    void assignThenList_fullFlow_persistsAndReturnsTask() {
        // Step 1: Assign a task (POST /tasks equivalent)
        AssignFamilyTaskRequest request = new AssignFamilyTaskRequest();
        request.setAssigneeMemberId(assigneeId);
        request.setTitle("Buy diapers");
        request.setDescription("Size M, at least 2 packs");
        request.setDueAt(Instant.now().plus(3, ChronoUnit.DAYS));

        AssignFamilyTaskResponse assignResponse = careTaskService.assignFamilyTask(groupId, request, ownerId);

        // Assert POST response
        assertThat(assignResponse).isNotNull();
        assertThat(assignResponse.getStatus()).isEqualTo("OPEN");
        assertThat(assignResponse.getAssignedTo()).isEqualTo(assigneeId);
        assertThat(assignResponse.getAssignedBy()).isEqualTo(ownerId);
        assertThat(assignResponse.getCareGroupId()).isEqualTo(groupId);

        // Step 2: List tasks (GET /tasks equivalent)
        CareTasksResponse listResponse = careTaskService.listTasks(groupId, assigneeId);

        assertThat(listResponse.getTotalTasks()).isEqualTo(1);
        assertThat(listResponse.getTasks()).hasSize(1);
        assertThat(listResponse.getTasks().get(0).getTitle()).isEqualTo("Buy diapers");

        // Step 3: DB assertion
        List<CareTask> dbTasks = careTaskRepository.findByCareGroupId(groupId);
        assertThat(dbTasks).hasSize(1);
        CareTask record = dbTasks.get(0);
        assertThat(record.getStatus()).isEqualTo(CareTaskStatus.OPEN);
        assertThat(record.getAssignedTo()).isEqualTo(assigneeId);
        assertThat(record.getAssignedBy()).isEqualTo(ownerId);
        assertThat(record.getCareGroupId()).isEqualTo(groupId);
    }
}

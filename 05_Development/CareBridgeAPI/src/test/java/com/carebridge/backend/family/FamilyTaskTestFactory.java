package com.carebridge.backend.family;

import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import java.time.Instant;
import java.util.UUID;

final class FamilyTaskTestFactory {

    static final UUID GROUP_CG_001 = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    static final UUID ACC_001 = UUID.fromString("00000000-0000-0000-0000-000000000a01");
    static final UUID ACC_002 = UUID.fromString("00000000-0000-0000-0000-000000000a02");
    static final UUID ACC_006 = UUID.fromString("00000000-0000-0000-0000-000000000a06");
    static final UUID ACC_007 = UUID.fromString("00000000-0000-0000-0000-000000000a07");

    static final Instant JULY_TASK_DUE_AT = Instant.parse("2026-07-05T09:00:00Z");
    static final Instant AUGUST_TASK_DUE_AT = Instant.parse("2026-08-15T00:00:00Z");
    static final Instant RANGE_START = Instant.parse("2026-07-01T00:00:00Z");
    static final Instant RANGE_END = Instant.parse("2026-07-31T23:59:59Z");

    private FamilyTaskTestFactory() {
    }

    static CareGroupMember makeMember(
            UUID groupId,
            UUID userId,
            InviteStatus status,
            String permissionJson) {
        return CareGroupMember.builder()
                .id(UUID.randomUUID())
                .careGroupId(groupId)
                .userId(userId)
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(status)
                .permissionJson(permissionJson)
                .build();
    }

    static CareTask makeTask(UUID groupId, Instant dueAt) {
        return CareTask.builder()
                .careGroupId(groupId)
                .assignedBy(ACC_001)
                .assignedTo(ACC_002)
                .title("Test Task")
                .description("Test task description")
                .dueAt(dueAt)
                .status(CareTaskStatus.OPEN)
                .build();
    }
}

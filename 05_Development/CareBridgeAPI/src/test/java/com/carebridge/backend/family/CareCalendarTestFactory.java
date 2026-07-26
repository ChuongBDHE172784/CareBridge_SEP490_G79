package com.carebridge.backend.family;

import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Props Isolation Factory for UC-74 View Shared Care Calendar tests.
 * All test data is SYNTHETIC — no production PII.
 */
public class CareCalendarTestFactory {

    public static final UUID GROUP_CG_001 = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    public static final UUID GROUP_CG_002 = UUID.fromString("00000000-0000-0000-0000-0000000000c2");

    // Account IDs (fixture names match UC74 Test-Spec §3 TDS-05)
    public static final UUID ACC_001 = UUID.fromString("00000000-0000-0000-0000-000000000a01"); // ACCEPTED + calendar=true
    public static final UUID ACC_002 = UUID.fromString("00000000-0000-0000-0000-000000000a02"); // ACCEPTED + calendar=false
    public static final UUID ACC_003 = UUID.fromString("00000000-0000-0000-0000-000000000a03"); // PENDING
    public static final UUID ACC_004 = UUID.fromString("00000000-0000-0000-0000-000000000a04"); // REVOKED
    public static final UUID ACC_005 = UUID.fromString("00000000-0000-0000-0000-000000000a05"); // non-member
    public static final UUID ACC_006 = UUID.fromString("00000000-0000-0000-0000-000000000a06"); // ACCEPTED + permission_json=NULL
    public static final UUID ACC_007 = UUID.fromString("00000000-0000-0000-0000-000000000a07"); // ACCEPTED + missing "calendar" key

    // Well-known instant within the July 2026 test range
    public static final Instant JULY_TASK_DUE_AT = Instant.parse("2026-07-05T09:00:00Z");
    // Out-of-range (August)
    public static final Instant AUGUST_TASK_DUE_AT = Instant.parse("2026-08-15T00:00:00Z");

    public static final Instant RANGE_START = Instant.parse("2026-07-01T00:00:00Z");
    public static final Instant RANGE_END   = Instant.parse("2026-07-31T23:59:59Z");

    // ── FX-001/002/003: Care groups ───────────────────────────────────────────

    /** FX-001 equivalent: returns an ACTIVE group CG-001. */
    public static CareGroup makeGroup(UUID groupId) {
        return CareGroup.builder()
                .id(groupId)
                .groupName("Test Care Group " + groupId.toString().substring(0, 4))
                .ownerUserId(ACC_001) // ACC_001 is OWNER of CG-001 in test fixtures
                .status(CareGroupStatus.ACTIVE)
                .build();
    }

    // ── Member factories ──────────────────────────────────────────────────────

    /** Builds a CareGroupMember with given status and permissionJson. */
    public static CareGroupMember makeMember(UUID groupId, UUID userId,
                                             InviteStatus status, String permissionJson) {
        return CareGroupMember.builder()
                .id(UUID.randomUUID())
                .careGroupId(groupId)
                .userId(userId)
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(status)
                .permissionJson(permissionJson)
                .build();
    }

    public static CareGroupMember makeMember(UUID groupId, UUID userId,
                                             InviteStatus status, String permissionJson,
                                             Consumer<CareGroupMember> overrides) {
        CareGroupMember m = makeMember(groupId, userId, status, permissionJson);
        overrides.accept(m);
        return m;
    }

    // ── CareTask factories ────────────────────────────────────────────────────

    /** Builds a minimal valid CareTask seeded into groupId with the given dueAt. */
    public static CareTask makeTask(UUID groupId, Instant dueAt) {
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

    public static CareTask makeTask(UUID groupId, Instant dueAt, Consumer<CareTask> overrides) {
        CareTask task = makeTask(groupId, dueAt);
        overrides.accept(task);
        return task;
    }
}

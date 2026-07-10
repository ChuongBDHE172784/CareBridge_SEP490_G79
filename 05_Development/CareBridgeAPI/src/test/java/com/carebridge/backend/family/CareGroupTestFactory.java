package com.carebridge.backend.family;

import com.carebridge.backend.family.dto.AssignFamilyTaskRequest;
import com.carebridge.backend.family.dto.UpdateFamilyPermissionRequest;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.notification.entity.DeviceToken;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Consumer;

public class CareGroupTestFactory {

    // Stable UUIDs for deterministic multi-class tests
    public static final UUID GROUP_ID    = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    public static final UUID OWNER_ID    = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    public static final UUID MEMBER_ID   = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    public static final UUID ASSIGNEE_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000004");

    // ── UC73 helpers ──────────────────────────────────────────────────────────

    /** Builds a minimal valid CareTask (OPEN, due tomorrow). */
    public static CareTask makeCareTask() {
        return CareTask.builder()
                .id(UUID.randomUUID())
                .careGroupId(GROUP_ID)
                .assignedBy(OWNER_ID)
                .assignedTo(ASSIGNEE_ID)
                .title("Check medication")
                .description("Verify medication was taken at 8 AM")
                .dueAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .status(CareTaskStatus.OPEN)
                .build();
    }

    public static CareTask makeCareTask(Consumer<CareTask> overrides) {
        CareTask task = makeCareTask();
        overrides.accept(task);
        return task;
    }

    /** Builds a valid AssignFamilyTaskRequest (due tomorrow, assignee = ASSIGNEE_ID). */
    public static AssignFamilyTaskRequest makeAssignFamilyTaskRequest() {
        AssignFamilyTaskRequest req = new AssignFamilyTaskRequest();
        req.setAssigneeMemberId(ASSIGNEE_ID);
        req.setTitle("Check medication");
        req.setDescription("Verify medication was taken at 8 AM");
        req.setDueAt(Instant.now().plus(1, ChronoUnit.DAYS));
        return req;
    }

    public static AssignFamilyTaskRequest makeAssignFamilyTaskRequest(Consumer<AssignFamilyTaskRequest> overrides) {
        AssignFamilyTaskRequest req = makeAssignFamilyTaskRequest();
        overrides.accept(req);
        return req;
    }

    // ── CareGroup / CareGroupMember helpers ───────────────────────────────────

    public static CareGroup makeCareGroup() {
        CareGroup group = new CareGroup();
        group.setId(UUID.randomUUID());
        group.setOwnerUserId(UUID.randomUUID());
        group.setGroupName("Test Care Group");
        group.setStatus(CareGroupStatus.ACTIVE);
        return group;
    }

    public static CareGroup makeCareGroup(Consumer<CareGroup> overrides) {
        CareGroup group = makeCareGroup();
        overrides.accept(group);
        return group;
    }

    public static CareGroupMember makeCareGroupMember() {
        CareGroupMember member = new CareGroupMember();
        member.setId(UUID.randomUUID());
        member.setCareGroupId(UUID.randomUUID());
        member.setUserId(UUID.randomUUID());
        member.setMemberRole(GroupMemberRole.MEMBER);
        member.setInviteStatus(InviteStatus.ACCEPTED);
        member.setPermissionJson(null);
        return member;
    }

    public static CareGroupMember makeCareGroupMember(Consumer<CareGroupMember> overrides) {
        CareGroupMember member = makeCareGroupMember();
        overrides.accept(member);
        return member;
    }

    /** UC72: builds a member with a pre-set permission_json value. */
    public static CareGroupMember makeCareGroupMemberWithPermission(String permissionJson) {
        return makeCareGroupMember(m -> m.setPermissionJson(permissionJson));
    }

    /** UC72: builds a valid UpdateFamilyPermissionRequest. */
    public static UpdateFamilyPermissionRequest makePermissionUpdateRequest(
            Consumer<UpdateFamilyPermissionRequest> overrides) {
        UpdateFamilyPermissionRequest request = new UpdateFamilyPermissionRequest();
        request.setCalendar(true);
        request.setLogs(false);
        request.setAlerts(true);
        request.setRecords(false);
        overrides.accept(request);
        return request;
    }

    /** UC72: builds an active device token for FCM. */
    public static DeviceToken makeDeviceToken(UUID userId) {
        DeviceToken token = new DeviceToken();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setToken("fcm-token-synthetic-" + UUID.randomUUID());
        token.setActive(true);
        return token;
    }
}

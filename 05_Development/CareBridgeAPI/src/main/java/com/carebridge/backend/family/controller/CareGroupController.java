package com.carebridge.backend.family.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.family.dto.CareGroupMemberDto;
import com.carebridge.backend.family.dto.CareGroupMembersResponse;
import com.carebridge.backend.family.dto.CreateCareGroupRequest;
import com.carebridge.backend.family.dto.CreateCareGroupResponse;
import com.carebridge.backend.family.dto.FamilyPermissionResponse;
import com.carebridge.backend.family.dto.InviteFamilyMemberRequest;
import com.carebridge.backend.family.dto.InviteFamilyMemberResponse;
import com.carebridge.backend.family.dto.JoinCareGroupRequest;
import com.carebridge.backend.family.dto.CareGroupSummaryDto;
import com.carebridge.backend.family.dto.PendingInvitationDto;
import com.carebridge.backend.family.dto.UpdateFamilyPermissionRequest;
import com.carebridge.backend.family.dto.AcceptInvitationByTokenResponse;
import com.carebridge.backend.family.dto.AssignFamilyTaskRequest;
import com.carebridge.backend.family.dto.AssignFamilyTaskResponse;
import com.carebridge.backend.family.dto.CancelFamilyTaskResponse;
import com.carebridge.backend.family.dto.CareTaskDetailResponse;
import com.carebridge.backend.family.dto.CareTasksResponse;
import com.carebridge.backend.family.dto.LeaveCareGroupResponse;
import com.carebridge.backend.family.dto.RemoveMemberResponse;
import com.carebridge.backend.family.dto.RevokeInvitationResponse;
import com.carebridge.backend.family.dto.RelinkCareGroupJourneyRequest;
import com.carebridge.backend.family.dto.RelinkCareGroupJourneyResponse;
import com.carebridge.backend.family.dto.UpdateFamilyTaskRequest;
import com.carebridge.backend.family.dto.UpdateFamilyTaskResponse;
import com.carebridge.backend.family.dto.UpdateTaskStatusRequest;
import com.carebridge.backend.family.dto.UpdateTaskStatusResponse;
import com.carebridge.backend.family.service.ICareGroupService;
import com.carebridge.backend.family.service.ICareTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/care-groups")
@RequiredArgsConstructor
public class CareGroupController {

    private final ICareGroupService careGroupService;
    private final ICareTaskService careTaskService;

    // UC70: Create care group
    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<CreateCareGroupResponse>> createCareGroup(
            @Valid @RequestBody CreateCareGroupRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.createCareGroup(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Care group created successfully"));
    }

    // UC70-DEL: Delete care group (owner only, lifecycle-safe archive)
    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<Void>> deleteCareGroup(
            @PathVariable UUID groupId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        careGroupService.deleteCareGroup(groupId, callerId);
        return ResponseEntity.ok(ApiResponse.success(null, "Care group deleted successfully"));
    }

    // UC-71/83: List care groups the caller belongs to (owner or accepted member)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CareGroupSummaryDto>>> listMyGroups(Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(careGroupService.listMyGroups(callerId)));
    }

    // UC216: View care group members
    @GetMapping("/{groupId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CareGroupMembersResponse>> listMembers(
            @PathVariable UUID groupId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.listMembers(groupId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC71: Invite family member via LINK, QR, or PHONE channel
    @PostMapping("/{groupId}/invitations")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<InviteFamilyMemberResponse>> inviteFamilyMember(
            @PathVariable UUID groupId,
            @Valid @RequestBody InviteFamilyMemberRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.inviteFamilyMember(groupId, request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Invitation sent"));
    }

    // UC83: List the caller's own pending invitations
    @GetMapping("/invitations/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PendingInvitationDto>>> listMyInvitations(Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(careGroupService.listMyInvitations(callerId)));
    }

    // UC83: Accept a pending invitation
    @PostMapping("/{groupId}/invitations/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CareGroupMemberDto>> acceptInvite(
            @PathVariable UUID groupId,
            @Valid @RequestBody com.carebridge.backend.family.dto.FamilyRelationshipRoleRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.acceptInvite(groupId, request.getFamilyRelationshipRole(),
                request.getCustomFamilyRelationshipRole(), callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Invitation accepted"));
    }

    // UC83: Decline a pending invitation
    @PostMapping("/{groupId}/invitations/decline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> declineInvite(
            @PathVariable UUID groupId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        careGroupService.declineInvite(groupId, callerId);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation declined"));
    }

    // UC217: Owner revokes a still-pending invitation
    @PostMapping("/{groupId}/invitations/{targetUserId}/revoke")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<RevokeInvitationResponse>> revokeInvitation(
            @PathVariable UUID groupId,
            @PathVariable UUID targetUserId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.revokeInvitation(groupId, targetUserId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Invitation revoked"));
    }

    // UC219: Owner removes an accepted non-owner member
    @DeleteMapping("/{groupId}/members/{targetUserId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<RemoveMemberResponse>> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID targetUserId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.removeMember(groupId, targetUserId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Member removed"));
    }

    // UC220: Accepted non-owner member leaves the group
    @PostMapping("/{groupId}/leave")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LeaveCareGroupResponse>> leaveCareGroup(
            @PathVariable UUID groupId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.leaveCareGroup(groupId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "You have left the care group"));
    }

    @PatchMapping("/{groupId}/journey")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<RelinkCareGroupJourneyResponse>> relinkJourney(
            @PathVariable UUID groupId,
            @Valid @RequestBody RelinkCareGroupJourneyRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.relinkJourney(groupId, request.getJourneyId(), callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Care group journey relinked successfully"));
    }

    // UC83: Accept a pending invitation by invite token (token-based deep-link / QR / PHONE accept)
    @PostMapping("/invitations/{token}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AcceptInvitationByTokenResponse>> acceptInvitationByToken(
            @PathVariable("token") String token,
            @Valid @RequestBody com.carebridge.backend.family.dto.FamilyRelationshipRoleRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.acceptInvitationByToken(token, request.getFamilyRelationshipRole(),
                request.getCustomFamilyRelationshipRole(), callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Invitation accepted"));
    }

    // Join care group by invite code or groupId
    @PostMapping("/join")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CareGroupSummaryDto>> joinGroupByCode(
            @Valid @RequestBody JoinCareGroupRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.joinGroupByCode(request.getCode(), request.getFamilyRelationshipRole(),
                request.getCustomFamilyRelationshipRole(), callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Yêu cầu tham gia đã được gửi, vui lòng chờ Mother duyệt."));
    }

    // List self-initiated join requests for this group (OWNER only)
    @GetMapping("/{groupId}/join-requests")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<List<com.carebridge.backend.family.dto.JoinRequestDto>>> listJoinRequests(
            @PathVariable UUID groupId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(careGroupService.listJoinRequests(groupId, callerId)));
    }

    // Mother approves or rejects a join request
    @PostMapping("/{groupId}/join-requests/{memberId}/respond")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<CareGroupMemberDto>> respondJoinRequest(
            @PathVariable UUID groupId,
            @PathVariable UUID memberId,
            @RequestParam boolean approve,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.respondJoinRequest(groupId, memberId, approve, callerId);
        return ResponseEntity.ok(ApiResponse.success(response,
                approve ? "Đã chấp nhận yêu cầu tham gia" : "Đã từ chối yêu cầu tham gia"));
    }

    // UC72: Update a member's permission flags (OWNER only)
    @PatchMapping("/{groupId}/members/{memberId}/permissions")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<FamilyPermissionResponse>> updateFamilyPermission(
            @PathVariable UUID groupId,
            @PathVariable UUID memberId,
            @RequestBody UpdateFamilyPermissionRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.updateFamilyPermission(groupId, memberId, request, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Family permission updated successfully"));
    }

    // UC72: View current permission grant for a member (target or OWNER)
    @GetMapping("/{groupId}/members/{memberId}/permissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FamilyPermissionResponse>> getFamilyPermission(
            @PathVariable UUID groupId,
            @PathVariable UUID memberId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.getFamilyPermission(groupId, memberId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC73: Assign a care task to an ACCEPTED member (OWNER only)
    @PostMapping("/{groupId}/tasks")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<AssignFamilyTaskResponse>> assignTask(
            @PathVariable UUID groupId,
            @Valid @RequestBody AssignFamilyTaskRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careTaskService.assignFamilyTask(groupId, request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Task assigned successfully"));
    }

    // UC73: List all care tasks in a group (any ACCEPTED member)
    @GetMapping("/{groupId}/tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CareTasksResponse>> listTasks(
            @PathVariable UUID groupId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careTaskService.listTasks(groupId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC221: View assigned task detail
    @GetMapping("/{groupId}/tasks/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CareTaskDetailResponse>> getTaskDetail(
            @PathVariable UUID groupId,
            @PathVariable UUID taskId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careTaskService.getTaskDetail(groupId, taskId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // UC222: Update task content only
    @PatchMapping("/{groupId}/tasks/{taskId}")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<UpdateFamilyTaskResponse>> updateTask(
            @PathVariable UUID groupId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateFamilyTaskRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careTaskService.updateFamilyTask(groupId, taskId, request, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Care task updated successfully"));
    }

    // UC223: Cancel an incomplete task
    @PostMapping("/{groupId}/tasks/{taskId}/cancel")
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<CancelFamilyTaskResponse>> cancelTask(
            @PathVariable UUID groupId,
            @PathVariable UUID taskId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careTaskService.cancelFamilyTask(groupId, taskId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Care task cancelled successfully"));
    }

    // UC85: Update assigned task status (assignee only — FSM validated)
    @PatchMapping("/{groupId}/tasks/{taskId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UpdateTaskStatusResponse>> updateTaskStatus(
            @PathVariable UUID groupId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careTaskService.updateTaskStatus(groupId, taskId, request, callerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Task status updated"));
    }

}

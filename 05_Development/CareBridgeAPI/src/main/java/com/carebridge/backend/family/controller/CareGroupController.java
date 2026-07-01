package com.carebridge.backend.family.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.family.dto.CareGroupMemberDto;
import com.carebridge.backend.family.dto.CareGroupMembersResponse;
import com.carebridge.backend.family.dto.CreateCareGroupRequest;
import com.carebridge.backend.family.dto.CreateCareGroupResponse;
import com.carebridge.backend.family.dto.InviteCareGroupMemberRequest;
import com.carebridge.backend.family.dto.CareGroupSummaryDto;
import com.carebridge.backend.family.dto.PendingInvitationDto;
import com.carebridge.backend.family.service.ICareGroupService;
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

    // UC83: Owner invites a member by email
    @PostMapping("/{groupId}/invitations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CareGroupMemberDto>> inviteMember(
            @PathVariable UUID groupId,
            @Valid @RequestBody InviteCareGroupMemberRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.inviteMember(groupId, request, callerId);
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
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.acceptInvite(groupId, callerId);
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
}

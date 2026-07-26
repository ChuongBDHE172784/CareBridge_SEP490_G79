package com.carebridge.backend.family.service;

import com.carebridge.backend.family.dto.AcceptInvitationByTokenResponse;
import com.carebridge.backend.family.dto.CareGroupMemberDto;
import com.carebridge.backend.family.dto.CareGroupMembersResponse;
import com.carebridge.backend.family.dto.CareGroupSummaryDto;
import com.carebridge.backend.family.dto.CreateCareGroupRequest;
import com.carebridge.backend.family.dto.CreateCareGroupResponse;
import com.carebridge.backend.family.dto.FamilyPermissionResponse;
import com.carebridge.backend.family.dto.InviteCareGroupMemberRequest;
import com.carebridge.backend.family.dto.InviteFamilyMemberRequest;
import com.carebridge.backend.family.dto.InviteFamilyMemberResponse;
import com.carebridge.backend.family.dto.JoinRequestDto;
import com.carebridge.backend.family.dto.LeaveCareGroupResponse;
import com.carebridge.backend.family.dto.PendingInvitationDto;
import com.carebridge.backend.family.dto.RemoveMemberResponse;
import com.carebridge.backend.family.dto.RevokeInvitationResponse;
import com.carebridge.backend.family.dto.UpdateFamilyPermissionRequest;

import java.util.List;
import java.util.UUID;

public interface ICareGroupService {

    /** @throws com.carebridge.backend.common.exception.BusinessException (FAM-002/409) if >= 5 active groups */
    CreateCareGroupResponse createCareGroup(CreateCareGroupRequest request, UUID callerId);

    /**
     * Deletes a care group (hard delete from DB) and all its members/tasks.
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-005/404) group not found
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-008/403) caller is not the group owner
     */
    void deleteCareGroup(UUID groupId, UUID callerId);

    /** @throws com.carebridge.backend.common.exception.BusinessException (FAM-003/403) if not ACCEPTED member */
    CareGroupMembersResponse listMembers(UUID groupId, UUID callerId);

    /** UC-71/83: Lists care groups the caller is an ACCEPTED member of (owner or invited). */
    List<CareGroupSummaryDto> listMyGroups(UUID callerId);

    /**
     * UC-83: Invite a member by email. Only the ACCEPTED OWNER may invite.
     * @throws com.carebridge.backend.common.exception.BusinessException FAM-005/404 group not found,
     *         FAM-008/403 caller not owner, FAM-006/404 no account for that email,
     *         FAM-007/409 already invited or a member
     */
    CareGroupMemberDto inviteMember(UUID groupId, InviteCareGroupMemberRequest request, UUID callerId);

    /** UC-83: Lists the caller's own PENDING invitations across all care groups. */
    List<PendingInvitationDto> listMyInvitations(UUID callerId);

    /** @throws com.carebridge.backend.common.exception.BusinessException (FAM-009/404) if no pending invite for caller */
    CareGroupMemberDto acceptInvite(UUID groupId, UUID callerId);

    /** @throws com.carebridge.backend.common.exception.BusinessException (FAM-009/404) if no pending invite for caller */
    void declineInvite(UUID groupId, UUID callerId);

    RevokeInvitationResponse revokeInvitation(UUID groupId, UUID targetUserId, UUID callerId);

    RemoveMemberResponse removeMember(UUID groupId, UUID targetUserId, UUID callerId);

    LeaveCareGroupResponse leaveCareGroup(UUID groupId, UUID callerId);

    /**
     * UC71: Invites a family member into an existing care group via LINK, QR, or PHONE channel.
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-005/404) group not found or not ACTIVE
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-012/403) caller is not group OWNER
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-010/400) invalid channel or phone missing for PHONE channel
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-013/409) group has 20 PENDING invites already
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-014/404) PHONE channel and no user account for that phone
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-011/409) PENDING invite already exists for the same person
     */
    InviteFamilyMemberResponse inviteFamilyMember(UUID groupId, InviteFamilyMemberRequest request, UUID callerId);

    /**
     * UC72: Grants or updates a family member's permission flags (calendar/logs/alerts/records).
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-005/404) care group not found
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-021/403) caller is not the group OWNER
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-020/404) target member not found or not ACCEPTED
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-022/400) payload has no recognized flag keys
     */
    FamilyPermissionResponse updateFamilyPermission(
            UUID careGroupId, UUID memberId, UpdateFamilyPermissionRequest request, UUID callerId);

    /**
     * UC72: Returns the current permission grant for a given member.
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-005/404) care group not found
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-020/404) target member not found or not ACCEPTED
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-003/403) caller is neither target member nor OWNER
     */
    FamilyPermissionResponse getFamilyPermission(UUID careGroupId, UUID memberId, UUID callerId);

    /**
     * UC-83: Accepts a PENDING care group invitation identified by invite token (token-based, for LINK/QR/PHONE channels).
     * Performs lazy expiry (ADR-FAM-006), phone-match for PHONE channel (ADR-FAM-007),
     * and single-use conditional update (ADR-FAM-008).
     *
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-040/404) token not found
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-041/410) token expired (lazy-transitioned to EXPIRED)
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-042/409) already non-PENDING or lost concurrency race
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-043/403) PHONE channel — phone mismatch
     */
    AcceptInvitationByTokenResponse acceptInvitationByToken(String inviteToken, UUID callerId);

    /**
     * Joins a care group using an invite code (groupId UUID or invite token).
     * Creates a PENDING join request; the Mother must approve before the member can access the group.
     */
    CareGroupSummaryDto joinGroupByCode(String code, UUID callerId);

    /**
     * Lists all self-initiated join requests (PENDING, no invite token) for a care group.
     * Only the group OWNER (Mother) may call this.
     */
    List<JoinRequestDto> listJoinRequests(UUID groupId, UUID callerId);

    /**
     * Mother approves or rejects a join request.
     * @param approve true = accept (ACCEPTED), false = reject (REJECTED)
     */
    CareGroupMemberDto respondJoinRequest(UUID groupId, UUID memberId, boolean approve, UUID callerId);
}

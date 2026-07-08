package com.carebridge.backend.family.service;

import com.carebridge.backend.family.dto.CareGroupMemberDto;
import com.carebridge.backend.family.dto.CareGroupMembersResponse;
import com.carebridge.backend.family.dto.CareGroupSummaryDto;
import com.carebridge.backend.family.dto.CreateCareGroupRequest;
import com.carebridge.backend.family.dto.CreateCareGroupResponse;
import com.carebridge.backend.family.dto.InviteCareGroupMemberRequest;
import com.carebridge.backend.family.dto.PendingInvitationDto;

import java.util.List;
import java.util.UUID;

public interface ICareGroupService {

    /** @throws com.carebridge.backend.common.exception.BusinessException (FAM-002/409) if >= 5 active groups */
    CreateCareGroupResponse createCareGroup(CreateCareGroupRequest request, UUID callerId);

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
}

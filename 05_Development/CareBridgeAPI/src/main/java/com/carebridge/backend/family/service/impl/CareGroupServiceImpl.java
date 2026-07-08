package com.carebridge.backend.family.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.CareGroupMemberDto;
import com.carebridge.backend.family.dto.CareGroupMembersResponse;
import com.carebridge.backend.family.dto.CreateCareGroupRequest;
import com.carebridge.backend.family.dto.CreateCareGroupResponse;
import com.carebridge.backend.family.dto.InviteCareGroupMemberRequest;
import com.carebridge.backend.family.dto.CareGroupSummaryDto;
import com.carebridge.backend.family.dto.PendingInvitationDto;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.service.ICareGroupService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CareGroupServiceImpl implements ICareGroupService {

    private final CareGroupRepository groupRepository;
    private final CareGroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Override
    public CreateCareGroupResponse createCareGroup(CreateCareGroupRequest request, UUID callerId) {
        // C1: max 5 active groups
        long activeCount = groupRepository.countByOwnerUserIdAndStatus(callerId, CareGroupStatus.ACTIVE);
        if (activeCount >= 5) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-002",
                    "Maximum of 5 active care groups reached");
        }

        // C4: accountId from JWT
        CareGroup group = CareGroup.builder()
                .ownerUserId(callerId)
                .groupName(request.getGroupName())
                .description(request.getDescription())
                .linkedJourneyId(request.getLinkedJourneyId())
                .linkedBabyProfileId(request.getLinkedBabyProfileId())
                .status(CareGroupStatus.ACTIVE)
                .build();

        CareGroup saved = groupRepository.save(group);

        // C2: creator auto-added as OWNER with ACCEPTED status
        CareGroupMember ownerMember = CareGroupMember.builder()
                .careGroupId(saved.getId())
                .userId(callerId)
                .memberRole(GroupMemberRole.OWNER)
                .inviteStatus(InviteStatus.ACCEPTED)
                .joinedAt(Instant.now())
                .build();
        memberRepository.save(ownerMember);

        long memberCount = memberRepository.countByCareGroupId(saved.getId());

        // C3: emit audit event
        auditService.log(AuditAction.CARE_GROUP_CREATED, callerId,
                "CareGroup", saved.getId().toString(), "created");

        return CreateCareGroupResponse.builder()
                .id(saved.getId())
                .groupName(saved.getGroupName())
                .description(saved.getDescription())
                .status(saved.getStatus().name())
                .memberCount((int) memberCount)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CareGroupMembersResponse listMembers(UUID groupId, UUID callerId) {
        CareGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found: " + groupId));

        // C1: isMember() MUST use ACCEPTED status — PENDING is NOT sufficient (ADR-FAM-002)
        boolean isMember = memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                groupId, callerId, InviteStatus.ACCEPTED);
        if (!isMember) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-003",
                    "You are not an accepted member of this group");
        }

        // C3: filter ACCEPTED + PENDING; exclude REVOKED
        List<CareGroupMember> members = memberRepository.findByCareGroupIdAndInviteStatusIn(
                groupId, List.of(InviteStatus.ACCEPTED, InviteStatus.PENDING));

        // C2: displayName only — NO email/phone/raw accountId (BR-PRIVACY-002)
        List<CareGroupMemberDto> memberDtos = members.stream()
                .map(m -> CareGroupMemberDto.builder()
                        .memberId(m.getId())
                        .displayName("Member")  // display name resolved from user profile in controller layer
                        .memberRole(m.getMemberRole() != null ? m.getMemberRole().name() : null)
                        .inviteStatus(m.getInviteStatus().name())
                        .joinedAt(m.getJoinedAt())
                        .build())
                .collect(Collectors.toList());

        return CareGroupMembersResponse.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .totalMembers(memberDtos.size())
                .members(memberDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CareGroupSummaryDto> listMyGroups(UUID callerId) {
        List<CareGroupMember> memberships = memberRepository.findByUserIdAndInviteStatus(callerId, InviteStatus.ACCEPTED);
        return memberships.stream()
                .map(m -> {
                    CareGroup group = groupRepository.findById(m.getCareGroupId()).orElse(null);
                    if (group == null) return null;
                    long count = memberRepository.countByCareGroupId(group.getId());
                    return CareGroupSummaryDto.builder()
                            .groupId(group.getId())
                            .groupName(group.getGroupName())
                            .isActive(group.getStatus() == CareGroupStatus.ACTIVE)
                            .totalMembers((int) count)
                            .myRole(m.getMemberRole() != null ? m.getMemberRole().name() : null)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public CareGroupMemberDto inviteMember(UUID groupId, InviteCareGroupMemberRequest request, UUID callerId) {
        CareGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found: " + groupId));

        CareGroupMember callerMembership = memberRepository.findByCareGroupIdAndUserId(groupId, callerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "FAM-008",
                        "Only the group owner can invite members"));
        if (callerMembership.getMemberRole() != GroupMemberRole.OWNER
                || callerMembership.getInviteStatus() != InviteStatus.ACCEPTED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-008",
                    "Only the group owner can invite members");
        }

        User invitee = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-006",
                        "No CareBridge account found for that email"));

        GroupMemberRole role = request.getMemberRole() != null ? request.getMemberRole() : GroupMemberRole.MEMBER;
        CareGroupMember member = memberRepository.findByCareGroupIdAndUserId(groupId, invitee.getId())
                .orElse(null);

        if (member != null && member.getInviteStatus() != InviteStatus.REVOKED) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-007",
                    "User is already invited or a member of this group");
        }

        if (member == null) {
            member = CareGroupMember.builder()
                    .careGroupId(groupId)
                    .userId(invitee.getId())
                    .memberRole(role)
                    .inviteStatus(InviteStatus.PENDING)
                    .build();
        } else {
            member.setMemberRole(role);
            member.setInviteStatus(InviteStatus.PENDING);
            member.setJoinedAt(null);
        }
        CareGroupMember saved = memberRepository.save(member);

        auditService.log(AuditAction.CARE_GROUP_MEMBER_INVITED, callerId,
                "CareGroup", group.getId().toString(), "member invited");

        return toMemberDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingInvitationDto> listMyInvitations(UUID callerId) {
        return memberRepository.findByUserIdAndInviteStatus(callerId, InviteStatus.PENDING).stream()
                .map(member -> {
                    CareGroup group = groupRepository.findById(member.getCareGroupId()).orElse(null);
                    return PendingInvitationDto.builder()
                            .groupId(member.getCareGroupId())
                            .groupName(group != null ? group.getGroupName() : null)
                            .memberRole(member.getMemberRole() != null ? member.getMemberRole().name() : null)
                            .invitedAt(member.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public CareGroupMemberDto acceptInvite(UUID groupId, UUID callerId) {
        CareGroupMember member = pendingInviteOrThrow(groupId, callerId);
        member.setInviteStatus(InviteStatus.ACCEPTED);
        member.setJoinedAt(Instant.now());
        CareGroupMember saved = memberRepository.save(member);

        auditService.log(AuditAction.CARE_GROUP_INVITE_ACCEPTED, callerId,
                "CareGroup", groupId.toString(), "invite accepted");

        return toMemberDto(saved);
    }

    @Override
    public void declineInvite(UUID groupId, UUID callerId) {
        CareGroupMember member = pendingInviteOrThrow(groupId, callerId);
        member.setInviteStatus(InviteStatus.REVOKED);
        memberRepository.save(member);

        auditService.log(AuditAction.CARE_GROUP_INVITE_DECLINED, callerId,
                "CareGroup", groupId.toString(), "invite declined");
    }

    private CareGroupMember pendingInviteOrThrow(UUID groupId, UUID callerId) {
        CareGroupMember member = memberRepository.findByCareGroupIdAndUserId(groupId, callerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-009",
                        "No pending invitation found for this group"));
        if (member.getInviteStatus() != InviteStatus.PENDING) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "FAM-009",
                    "No pending invitation found for this group");
        }
        return member;
    }

    // C2: displayName only — NO email/phone/raw accountId (BR-PRIVACY-002)
    private CareGroupMemberDto toMemberDto(CareGroupMember member) {
        return CareGroupMemberDto.builder()
                .memberId(member.getId())
                .displayName("Member")
                .memberRole(member.getMemberRole() != null ? member.getMemberRole().name() : null)
                .inviteStatus(member.getInviteStatus().name())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}

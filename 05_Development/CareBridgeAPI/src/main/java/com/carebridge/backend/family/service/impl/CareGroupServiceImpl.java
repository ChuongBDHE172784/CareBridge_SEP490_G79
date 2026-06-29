package com.carebridge.backend.family.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.CareGroupMemberDto;
import com.carebridge.backend.family.dto.CareGroupMembersResponse;
import com.carebridge.backend.family.dto.CreateCareGroupRequest;
import com.carebridge.backend.family.dto.CreateCareGroupResponse;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.service.ICareGroupService;
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
}

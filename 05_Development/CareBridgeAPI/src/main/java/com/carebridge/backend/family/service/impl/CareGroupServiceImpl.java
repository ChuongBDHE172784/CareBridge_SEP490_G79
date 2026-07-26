package com.carebridge.backend.family.service.impl;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.family.dto.AcceptInvitationByTokenResponse;
import com.carebridge.backend.family.dto.CareGroupMemberDto;
import com.carebridge.backend.family.dto.CareGroupMembersResponse;
import com.carebridge.backend.family.dto.CreateCareGroupRequest;
import com.carebridge.backend.family.dto.CreateCareGroupResponse;
import com.carebridge.backend.family.dto.FamilyPermission;
import com.carebridge.backend.family.dto.FamilyPermissionResponse;
import com.carebridge.backend.family.dto.InviteCareGroupMemberRequest;
import com.carebridge.backend.family.dto.InviteFamilyMemberRequest;
import com.carebridge.backend.family.dto.InviteFamilyMemberResponse;
import com.carebridge.backend.family.dto.LeaveCareGroupResponse;
import com.carebridge.backend.family.dto.RemoveMemberResponse;
import com.carebridge.backend.family.dto.RevokeInvitationResponse;
import com.carebridge.backend.family.dto.CareGroupSummaryDto;
import com.carebridge.backend.family.dto.JoinRequestDto;
import com.carebridge.backend.family.dto.PendingInvitationDto;
import com.carebridge.backend.family.dto.UpdateFamilyPermissionRequest;
import com.carebridge.backend.family.event.CareGroupInvitationRevoked;
import com.carebridge.backend.family.event.CareGroupMemberLeft;
import com.carebridge.backend.family.event.CareGroupMemberRemoved;
import com.carebridge.backend.family.event.CareTaskReassigned;
import com.carebridge.backend.family.event.FamilyPermissionUpdated;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.service.FcmService;
import lombok.extern.slf4j.Slf4j;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.InviteChannel;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.family.repository.CareTaskRepository;
import com.carebridge.backend.family.service.ICareGroupService;
import com.carebridge.backend.family.service.InviteTokenGenerator;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CareGroupServiceImpl implements ICareGroupService {

    private final CareGroupRepository groupRepository;
    private final CareGroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final CareGroupAuthorizationPolicy authorizationPolicy;
    private final InviteTokenGenerator tokenGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final FcmService fcmService;
    private final DeviceTokenRepository deviceTokenRepository;
    private final CareTaskRepository taskRepository;

    @Override
    public CreateCareGroupResponse createCareGroup(CreateCareGroupRequest request, UUID callerId) {
        // C1: max 5 active groups
        long activeCount = groupRepository.countByOwnerUserIdAndStatus(callerId, CareGroupStatus.ACTIVE);
        if (activeCount >= 5) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-002",
                    "Maximum of 5 active care groups reached");
        }

        // C5: group name must be unique for this owner (case-insensitive)
        if (groupRepository.existsByOwnerUserIdAndGroupNameIgnoreCase(callerId, request.getGroupName().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-015",
                    "A care group with this name already exists");
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
    public void deleteCareGroup(UUID groupId, UUID callerId) {
        CareGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found: " + groupId));

        // Only OWNER can delete
        if (!group.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-008",
                    "Only the group owner can delete this group");
        }

        // Hard delete: members, tasks, then the group itself
        memberRepository.deleteByCareGroupId(groupId);
        taskRepository.deleteByCareGroupId(groupId);
        groupRepository.deleteById(groupId);

        auditService.log(AuditAction.CARE_GROUP_DELETED, callerId,
                "CareGroup", groupId.toString(), "deleted");
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

        // C2: resolve displayName from user profile — full_name only (BR-PRIVACY-002)
        List<CareGroupMemberDto> memberDtos = members.stream()
                .map(m -> {
                    String displayName = userRepository.findById(m.getUserId())
                            .map(u -> {
                                String name = u.getName();
                                if (name != null && !name.isBlank()) return name;
                                // fallback: show phone (masked) if name is empty
                                String phone = u.getPhone();
                                if (phone != null && !phone.isBlank()) return phone;
                                return "Thành viên";
                            })
                            .orElse("Thành viên");
                    return CareGroupMemberDto.builder()
                            .memberId(m.getId())
                            .userId(m.getUserId())
                            .displayName(displayName)
                            .memberRole(m.getMemberRole() != null ? m.getMemberRole().name() : null)
                            .inviteStatus(m.getInviteStatus().name())
                            .joinedAt(m.getJoinedAt())
                            .build();
                })
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

    @Override
    public RevokeInvitationResponse revokeInvitation(UUID groupId, UUID targetUserId, UUID callerId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found"));

        if (targetUserId.equals(callerId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FAM-053",
                    "You cannot revoke your own membership via this action");
        }

        requireOwner(groupId, callerId, "FAM-050", "Only the care group owner can revoke invitations");

        CareGroupMember target = memberRepository.findByCareGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-051",
                        "No invitation found for this user in this group"));
        if (target.getInviteStatus() != InviteStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-052",
                    "Invitation is not pending and cannot be revoked");
        }

        target.setInviteStatus(InviteStatus.REVOKED);
        CareGroupMember saved = memberRepository.save(target);
        Instant revokedAt = Instant.now();

        auditService.log(AuditAction.CARE_GROUP_INVITE_REVOKED, callerId,
                "CareGroup", groupId.toString(), "invite revoked for user " + targetUserId);
        eventPublisher.publishEvent(new CareGroupInvitationRevoked(
                UUID.randomUUID(), "CareGroupInvitationRevoked", revokedAt, "1.0",
                new CareGroupInvitationRevoked.Payload(groupId, saved.getId(), targetUserId, callerId),
                new CareGroupInvitationRevoked.Metadata(UUID.randomUUID(), "CareGroupServiceImpl")));

        return RevokeInvitationResponse.builder()
                .careGroupMemberId(saved.getId())
                .groupId(groupId)
                .targetUserId(targetUserId)
                .inviteStatus(saved.getInviteStatus().name())
                .revokedAt(revokedAt)
                .build();
    }

    @Override
    public RemoveMemberResponse removeMember(UUID groupId, UUID targetUserId, UUID callerId) {
        try {
            CareGroup group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                            "Care group not found"));

            if (!group.getOwnerUserId().equals(callerId)) {
                requireOwner(groupId, callerId, "FAM-058", "Only the care group owner can remove members");
            }

            CareGroupMember target = memberRepository.findByCareGroupIdAndUserId(groupId, targetUserId)
                    .or(() -> memberRepository.findByIdAndCareGroupId(targetUserId, groupId))
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-059",
                            "No membership found for this user in this group"));
            if (target.getMemberRole() == GroupMemberRole.OWNER || (target.getUserId() != null && target.getUserId().equals(group.getOwnerUserId()))) {
                throw new BusinessException(HttpStatus.CONFLICT, "FAM-061",
                        "The group owner cannot be removed");
            }

            UUID memberId = target.getId();
            UUID targetUserUuid = target.getUserId();

            if (targetUserUuid != null) {
                taskRepository.reassignIncompleteTasks(groupId, targetUserUuid, group.getOwnerUserId());
                List<CareGroupMember> duplicates = memberRepository.findAllByCareGroupIdAndUserId(groupId, targetUserUuid);
                if (!duplicates.isEmpty()) {
                    memberRepository.deleteAll(duplicates);
                } else {
                    memberRepository.delete(target);
                }
            } else {
                memberRepository.delete(target);
            }
            Instant removedAt = Instant.now();

            auditService.log(AuditAction.CARE_GROUP_MEMBER_REMOVED, callerId,
                    "CareGroup", groupId.toString(), "member removed: " + targetUserUuid);
            try {
                eventPublisher.publishEvent(new CareGroupMemberRemoved(
                        UUID.randomUUID(), "CareGroupMemberRemoved", removedAt, "1.0",
                        new CareGroupMemberRemoved.Payload(groupId, memberId, targetUserUuid, callerId),
                        new CareGroupMemberRemoved.Metadata(UUID.randomUUID(), "CareGroupServiceImpl")));
            } catch (Exception e) {
                log.warn("Failed to publish CareGroupMemberRemoved event", e);
            }

            return RemoveMemberResponse.builder()
                    .careGroupMemberId(memberId)
                    .groupId(groupId)
                    .targetUserId(targetUserUuid)
                    .inviteStatus("REMOVED")
                    .removedAt(removedAt)
                    .build();
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Error removing member from care group groupId={} targetUserId={}", groupId, targetUserId, e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "FAM-500",
                    "Error removing member: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @Override
    public LeaveCareGroupResponse leaveCareGroup(UUID groupId, UUID callerId) {
        CareGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found"));
        CareGroupMember own = memberRepository.findByCareGroupIdAndUserId(groupId, callerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, "FAM-064",
                        "You are not an active member of this care group"));
        if (own.getMemberRole() == GroupMemberRole.OWNER) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-063",
                    "The group owner cannot leave the care group");
        }
        if (own.getInviteStatus() != InviteStatus.ACCEPTED) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-064",
                    "You are not an active member of this care group");
        }

        int reassigned = taskRepository.reassignIncompleteTasks(groupId, callerId, group.getOwnerUserId());
        own.setInviteStatus(InviteStatus.REVOKED);
        memberRepository.save(own);
        Instant leftAt = Instant.now();

        auditService.log(AuditAction.CARE_GROUP_MEMBER_LEFT, callerId,
                "CareGroup", groupId.toString(), "member left");
        eventPublisher.publishEvent(new CareGroupMemberLeft(
                UUID.randomUUID(), "CareGroupMemberLeft", leftAt, "1.0",
                new CareGroupMemberLeft.Payload(groupId, own.getId(), callerId, reassigned),
                new CareGroupMemberLeft.Metadata(UUID.randomUUID(), "CareGroupServiceImpl")));
        if (reassigned > 0) {
            eventPublisher.publishEvent(new CareTaskReassigned(
                    UUID.randomUUID(), "CareTaskReassigned", leftAt, "1.0",
                    new CareTaskReassigned.Payload(groupId, callerId, group.getOwnerUserId(), reassigned),
                    new CareTaskReassigned.Metadata(UUID.randomUUID(), "CareGroupServiceImpl")));
        }

        return LeaveCareGroupResponse.builder()
                .groupId(groupId)
                .leftAt(leftAt)
                .reassignedTaskCount(reassigned)
                .build();
    }

    private CareGroupMember requireOwner(UUID groupId, UUID callerId, String code, String message) {
        CareGroupMember caller = memberRepository.findByCareGroupIdAndUserId(groupId, callerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, code, message));
        if (caller.getMemberRole() != GroupMemberRole.OWNER
                || caller.getInviteStatus() != InviteStatus.ACCEPTED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, code, message);
        }
        return caller;
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

    @Override
    public InviteFamilyMemberResponse inviteFamilyMember(UUID groupId, InviteFamilyMemberRequest request, UUID callerId) {
        // Guard: group must exist and be ACTIVE
        groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found: " + groupId));

        // C3: OWNER-only (ADR-FAM-011)
        if (!authorizationPolicy.isOwner(groupId, callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-012",
                    "Only the care group owner may invite new members.");
        }

        // C5: max 20 PENDING invites per group (ADR-FAM-013)
        long pendingCount = memberRepository.countByCareGroupIdAndInviteStatus(groupId, InviteStatus.PENDING);
        if (pendingCount >= 20) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-013",
                    "This care group already has the maximum number of pending invites (20).");
        }

        // C1: generate secure token (ADR-FAM-010)
        String rawToken = tokenGenerator.generate();

        InviteChannel channel = request.getChannel();
        Instant expiresAt = Instant.now().plus(7, java.time.temporal.ChronoUnit.DAYS);

        if (channel == InviteChannel.PHONE) {
            // C4: PHONE channel — resolve to existing account (ADR-FAM-012)
            String phone = request.getPhone();
            if (phone == null || phone.isBlank()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "FAM-010",
                        "Phone number or email is required for invitation.");
            }
            User invitee = userRepository.findByPhone(phone)
                    .or(() -> userRepository.findByEmailIgnoreCase(phone))
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-014",
                            "No CareBridge account was found for this phone number or email."));

            // Duplicate check: PENDING invite already exists
            if (memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(groupId, invitee.getId(), InviteStatus.PENDING)) {
                throw new BusinessException(HttpStatus.CONFLICT, "FAM-011",
                        "A pending invite already exists for this person in this care group.");
            }
            // Already-accepted check
            memberRepository.findByCareGroupIdAndUserId(groupId, invitee.getId())
                    .filter(m -> m.getInviteStatus() == InviteStatus.ACCEPTED)
                    .ifPresent(m -> { throw new BusinessException(HttpStatus.CONFLICT, "FAM-011",
                            "This person is already an accepted member of the group."); });

            String targetPhone = (invitee.getPhone() != null && !invitee.getPhone().isBlank())
                    ? invitee.getPhone()
                    : (phone.length() <= 20 ? phone : null);

            CareGroupMember member = CareGroupMember.builder()
                    .careGroupId(groupId)
                    .userId(invitee.getId())
                    .memberRole(GroupMemberRole.MEMBER)
                    .inviteStatus(InviteStatus.PENDING)
                    .inviteChannel(InviteChannel.PHONE)
                    .inviteToken(rawToken)
                    .inviteExpiresAt(expiresAt)
                    .invitedPhone(targetPhone)
                    .build();
            CareGroupMember saved = memberRepository.save(member);

            auditService.log(AuditAction.CARE_GROUP_MEMBER_INVITED, callerId,
                    "CareGroupMember", saved.getId().toString(), "phone/email invite created");

            // C2: publish event with hashed token only — NEVER log raw token
            publishInviteEvent(groupId, saved.getId(), callerId, channel, rawToken, targetPhone, expiresAt);

            return InviteFamilyMemberResponse.builder()
                    .careGroupMemberId(saved.getId())
                    .channel(InviteChannel.PHONE)
                    .inviteToken(rawToken)
                    .inviteExpiresAt(expiresAt)
                    .invitedPhone(targetPhone)
                    .build();
        }

        // LINK or QR: return token only, no DB row (user_id NOT NULL constraint — ADR-FAM-012)
        auditService.log(AuditAction.CARE_GROUP_MEMBER_INVITED, callerId,
                "CareGroupInviteToken", rawToken.substring(0, 8) + "...", channel.name() + " invite issued");

        publishInviteEvent(groupId, null, callerId, channel, rawToken, null, expiresAt);

        return InviteFamilyMemberResponse.builder()
                .careGroupMemberId(null)
                .channel(channel)
                .inviteToken(rawToken)
                .inviteExpiresAt(expiresAt)
                .invitedPhone(null)
                .build();
    }

    private void publishInviteEvent(UUID groupId, UUID memberId, UUID inviterId,
                                    InviteChannel channel, String rawToken,
                                    String invitedPhone, Instant expiresAt) {
        // C2: SHA-256 hash of raw token — raw token MUST NOT appear in event payload
        String tokenHash;
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            tokenHash = sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            tokenHash = "hash-unavailable";
        }

        eventPublisher.publishEvent(new com.carebridge.backend.family.event.FamilyMemberInvited(
                UUID.randomUUID(),
                "FamilyMemberInvited",
                Instant.now(),
                "1.0",
                new com.carebridge.backend.family.event.FamilyMemberInvited.Payload(
                        groupId, memberId, inviterId, channel, tokenHash, invitedPhone, expiresAt),
                new com.carebridge.backend.family.event.FamilyMemberInvited.Metadata(
                        UUID.randomUUID(), inviterId.toString())
        ));
    }

    @Override
    @Transactional
    public AcceptInvitationByTokenResponse acceptInvitationByToken(String inviteToken, UUID callerId) {
        // Step 1: look up member row by token (only PHONE-channel invites have a DB row)
        CareGroupMember member = memberRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-040",
                        "Invite token is invalid or does not exist."));

        Instant now = Instant.now();

        // Step 2: lazy expiry check (ADR-FAM-006)
        if (member.getInviteStatus() == InviteStatus.PENDING
                && member.getInviteExpiresAt() != null
                && member.getInviteExpiresAt().isBefore(now)) {
            memberRepository.markExpiredIfPending(member.getId(), now);
            throw new BusinessException(HttpStatus.GONE, "FAM-041",
                    "This invitation has expired.");
        }

        // Step 3: must still be PENDING
        if (member.getInviteStatus() != InviteStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-042",
                    "This invitation is no longer pending and cannot be accepted.");
        }

        // Step 4: PHONE channel — verified phone must match (ADR-FAM-007)
        if (member.getInviteChannel() == InviteChannel.PHONE) {
            if (!authorizationPolicy.isPhoneMatchForInvite(member, callerId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-043",
                        "Your verified phone number does not match this invitation.");
            }
        }

        // Step 5: atomic conditional accept (ADR-FAM-008) — prevents concurrent double-accept
        int rows = memberRepository.acceptIfPending(member.getId(), now);
        if (rows == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-042",
                    "This invitation is no longer pending and cannot be accepted.");
        }

        // Step 6: bind the accepting user's identity to the member row
        member.setUserId(callerId);
        member.setInviteStatus(InviteStatus.ACCEPTED);
        member.setJoinedAt(now);

        // Step 7: audit log
        auditService.log(AuditAction.CARE_GROUP_INVITATION_ACCEPTED, callerId,
                "CareGroupMember", member.getId().toString(), "Invitation accepted via token");

        // Step 8: FCM to owner — best-effort, must NOT roll back on failure
        try {
            memberRepository.findByCareGroupIdAndUserId(member.getCareGroupId(), member.getCareGroupId())
                    .ifPresent(ownerMember -> {
                        List<String> tokens = deviceTokenRepository
                                .findByUserIdAndActiveTrue(ownerMember.getUserId())
                                .stream().map(t -> t.getToken()).collect(Collectors.toList());
                        if (!tokens.isEmpty()) {
                            fcmService.sendToTokens(tokens, "Lời mời đã được chấp nhận",
                                    "Một thành viên đã tham gia nhóm của bạn.");
                        }
                    });
        } catch (Exception e) {
            log.warn("FCM notification failed after token-based invite accept (non-blocking): {}", e.getMessage());
        }

        return AcceptInvitationByTokenResponse.builder()
                .careGroupId(member.getCareGroupId())
                .careGroupMemberId(member.getId())
                .inviteStatus(InviteStatus.ACCEPTED.name())
                .joinedAt(now)
                .build();
    }

    @Override
    @Transactional
    public CareGroupSummaryDto joinGroupByCode(String code, UUID callerId) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FAM-060", "Mã mời không được để trống.");
        }
        String cleanCode = code.trim();
        UUID groupId;

        try {
            groupId = UUID.fromString(cleanCode);
        } catch (IllegalArgumentException e) {
            // Not a UUID — try accepting via invite token (old LINK/QR/PHONE flow)
            try {
                var res = acceptInvitationByToken(cleanCode, callerId);
                groupId = res.getCareGroupId();
                CareGroup tokenGroup = groupRepository.findById(groupId).orElseThrow();
                return toGroupSummaryDto(tokenGroup, GroupMemberRole.MEMBER.name());
            } catch (Exception ex) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "FAM-005", "Mã mời không hợp lệ hoặc không tồn tại.");
            }
        }

        CareGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005", "Không tìm thấy nhóm chăm sóc với mã này."));

        if (group.getStatus() != CareGroupStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FAM-061", "Nhóm chăm sóc này hiện không hoạt động.");
        }

        // If caller is the group owner, they are already a member
        if (group.getOwnerUserId().equals(callerId)) {
            return toGroupSummaryDto(group, GroupMemberRole.OWNER.name());
        }

        var existingOpt = memberRepository.findByCareGroupIdAndUserId(groupId, callerId);
        if (existingOpt.isPresent()) {
            CareGroupMember existing = existingOpt.get();
            if (existing.getInviteStatus() == InviteStatus.ACCEPTED) {
                throw new BusinessException(HttpStatus.CONFLICT, "FAM-007", "Bạn đã là thành viên của nhóm này.");
            }
            if (existing.getInviteStatus() == InviteStatus.PENDING) {
                throw new BusinessException(HttpStatus.CONFLICT, "FAM-007", "Yêu cầu tham gia đã được gửi, vui lòng chờ Mother duyệt.");
            }
            // REJECTED / REVOKED / EXPIRED — allow re-requesting
            existing.setInviteStatus(InviteStatus.PENDING);
            existing.setJoinedAt(null);
            memberRepository.save(existing);
            auditService.log(AuditAction.CARE_GROUP_MEMBER_INVITED, callerId,
                    "CareGroup", group.getId().toString(), "Re-submitted join request via code");
            return toGroupSummaryDto(group, GroupMemberRole.MEMBER.name());
        }

        // New join request — PENDING until Mother approves
        CareGroupMember joinRequest = CareGroupMember.builder()
                .careGroupId(groupId)
                .userId(callerId)
                .memberRole(GroupMemberRole.MEMBER)
                .inviteStatus(InviteStatus.PENDING)
                .build();
        memberRepository.save(joinRequest);

        auditService.log(AuditAction.CARE_GROUP_MEMBER_INVITED, callerId,
                "CareGroup", group.getId().toString(), "Join request submitted via invite code");

        return toGroupSummaryDto(group, GroupMemberRole.MEMBER.name());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JoinRequestDto> listJoinRequests(UUID groupId, UUID callerId) {
        CareGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found: " + groupId));
        if (!group.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-008",
                    "Only the group owner can view join requests");
        }
        return memberRepository
                .findByCareGroupIdAndInviteStatusAndInviteTokenIsNull(groupId, InviteStatus.PENDING)
                .stream()
                .map(m -> {
                    var userOpt = userRepository.findById(m.getUserId());
                    String displayName = userOpt.map(u -> u.getName() != null ? u.getName() : u.getEmail()).orElse("Unknown");
                    String email = userOpt.map(com.carebridge.backend.security.entity.User::getEmail).orElse(null);
                    String phone = userOpt.map(com.carebridge.backend.security.entity.User::getPhone).orElse(null);
                    return JoinRequestDto.builder()
                            .memberId(m.getId())
                            .userId(m.getUserId())
                            .displayName(displayName)
                            .email(email)
                            .phone(phone)
                            .requestedAt(m.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CareGroupMemberDto respondJoinRequest(UUID groupId, UUID memberId, boolean approve, UUID callerId) {
        CareGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found: " + groupId));
        if (!group.getOwnerUserId().equals(callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-008",
                    "Only the group owner can approve or reject join requests");
        }
        CareGroupMember member = memberRepository.findByIdAndCareGroupId(memberId, groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-009",
                        "Join request not found"));
        if (member.getInviteStatus() != InviteStatus.PENDING || member.getInviteToken() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "FAM-042",
                    "This is not a pending join request");
        }
        if (approve) {
            member.setInviteStatus(InviteStatus.ACCEPTED);
            member.setJoinedAt(Instant.now());
            auditService.log(AuditAction.CARE_GROUP_INVITE_ACCEPTED, callerId,
                    "CareGroup", groupId.toString(), "Join request approved for member: " + memberId);
        } else {
            member.setInviteStatus(InviteStatus.REJECTED);
            auditService.log(AuditAction.CARE_GROUP_INVITE_DECLINED, callerId,
                    "CareGroup", groupId.toString(), "Join request rejected for member: " + memberId);
        }
        CareGroupMember saved = memberRepository.save(member);
        return toMemberDto(saved);
    }

    private CareGroupSummaryDto toGroupSummaryDto(CareGroup group, String roleName) {
        long count = memberRepository.countByCareGroupId(group.getId());
        return CareGroupSummaryDto.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .isActive(group.getStatus() == CareGroupStatus.ACTIVE)
                .totalMembers((int) count)
                .myRole(roleName)
                .build();
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

    @Override
    public FamilyPermissionResponse updateFamilyPermission(
            UUID careGroupId, UUID memberId, UpdateFamilyPermissionRequest request, UUID callerId) {
        // C7: no new migration — permission_json already exists in V1
        groupRepository.findById(careGroupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found: " + careGroupId));

        // C1: OWNER-only via policy (ADR-FAM-021)
        if (!authorizationPolicy.canManagePermissions(careGroupId, callerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-021",
                    "Only the group owner can manage member permissions");
        }

        // C2: target must be ACCEPTED (ADR-FAM-023)
        CareGroupMember member = memberRepository.findByIdAndCareGroupId(memberId, careGroupId)
                .filter(m -> m.getInviteStatus() == InviteStatus.ACCEPTED)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-020",
                        "Member not found or not an accepted member of this group"));

        // C3: validate payload — at least one recognized flag required (ADR-FAM-020)
        if (!request.hasAtLeastOneField()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FAM-022",
                    "Invalid permission payload: no recognized flag provided");
        }

        // Merge previous + requested (null = unchanged)
        FamilyPermission previous = FamilyPermission.fromJson(member.getPermissionJson());
        FamilyPermission updated = new FamilyPermission(
                request.getCalendar()  != null ? request.getCalendar()  : previous.isCalendar(),
                request.getLogs()      != null ? request.getLogs()      : previous.isLogs(),
                request.getAlerts()    != null ? request.getAlerts()    : previous.isAlerts(),
                request.getRecords()   != null ? request.getRecords()   : previous.isRecords()
        );

        member.setPermissionJson(updated.toJson());
        CareGroupMember saved = memberRepository.save(member);

        // Audit log — POST-3 (SRS)
        auditService.log(AuditAction.CARE_GROUP_PERMISSION_UPDATED, callerId,
                "CareGroupMember", memberId.toString(), "permission updated");

        // Domain event — C5: publish after DB write
        eventPublisher.publishEvent(new FamilyPermissionUpdated(
                UUID.randomUUID(),
                "FamilyPermissionUpdated",
                Instant.now(),
                "1.0",
                new FamilyPermissionUpdated.Payload(careGroupId, memberId, callerId, previous, updated),
                new FamilyPermissionUpdated.Metadata(UUID.randomUUID(), callerId.toString())
        ));

        // FCM — C6: fire-and-forget; failure must NOT roll back DB write (ADR-FAM-022)
        try {
            List<String> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(member.getUserId())
                    .stream()
                    .map(t -> t.getToken())
                    .collect(Collectors.toList());
            if (!tokens.isEmpty()) {
                fcmService.sendToTokens(tokens, "Quyền truy cập đã thay đổi",
                        "Chủ nhóm đã cập nhật quyền truy cập dữ liệu của bạn.");
            }
        } catch (Exception e) {
            log.warn("FCM send failed for member {} after permission update (non-blocking): {}",
                    memberId, e.getMessage());
        }

        return FamilyPermissionResponse.builder()
                .memberId(memberId)
                .careGroupId(careGroupId)
                .calendar(updated.isCalendar())
                .logs(updated.isLogs())
                .alerts(updated.isAlerts())
                .records(updated.isRecords())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FamilyPermissionResponse getFamilyPermission(UUID careGroupId, UUID memberId, UUID callerId) {
        groupRepository.findById(careGroupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-005",
                        "Care group not found: " + careGroupId));

        // C2: target must be ACCEPTED (ADR-FAM-023)
        CareGroupMember member = memberRepository.findByIdAndCareGroupId(memberId, careGroupId)
                .filter(m -> m.getInviteStatus() == InviteStatus.ACCEPTED)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "FAM-020",
                        "Member not found or not an accepted member of this group"));

        // Authorization: caller must be the target member OR the group OWNER (§16)
        boolean isSelf = member.getUserId().equals(callerId);
        boolean isOwner = authorizationPolicy.isOwner(careGroupId, callerId);
        if (!isSelf && !isOwner) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FAM-003",
                    "You are not authorized to view this member's permissions");
        }

        FamilyPermission perm = FamilyPermission.fromJson(member.getPermissionJson());

        return FamilyPermissionResponse.builder()
                .memberId(memberId)
                .careGroupId(careGroupId)
                .calendar(perm.isCalendar())
                .logs(perm.isLogs())
                .alerts(perm.isAlerts())
                .records(perm.isRecords())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}

package com.carebridge.backend.family.policy;

import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.entity.GroupMemberRole;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.security.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CareGroupAuthorizationPolicy {

    private final CareGroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public boolean isOwner(UUID groupId, UUID userId) {
        return memberRepository.findByCareGroupIdAndUserId(groupId, userId)
                .map(m -> m.getMemberRole() == GroupMemberRole.OWNER && isActiveMembership(m))
                .orElse(false);
    }

    public boolean canManagePermissions(UUID groupId, UUID callerId) {
        return isOwner(groupId, callerId);
    }

    /** UC-73 (ADR-FAM-032): only ACCEPTED OWNER may assign tasks. */
    public boolean canAssignTasks(UUID groupId, UUID callerId) {
        return isOwner(groupId, callerId);
    }

    public boolean canUpdateTask(UUID groupId, UUID callerId) {
        return isOwner(groupId, callerId);
    }

    public boolean canCancelTask(UUID groupId, UUID callerId) {
        return isOwner(groupId, callerId);
    }

    /** UC-74 (ADR-FAM-002): ACCEPTED membership check for read access. */
    public boolean isMember(UUID groupId, UUID userId) {
        return memberRepository.findByCareGroupIdAndUserId(groupId, userId)
                .map(this::isActiveMembership)
                .orElse(false);
    }

    /**
     * UC-83 (ADR-FAM-007): for PHONE-channel invites, the caller's verified phone must match invited_phone.
     */
    public boolean isPhoneMatchForInvite(CareGroupMember member, UUID currentUserId) {
        String invitedPhone = member.getInvitedPhone();
        if (invitedPhone == null || invitedPhone.isBlank()) return false;
        return userRepository.findById(currentUserId)
                .map(u -> Boolean.TRUE.equals(u.getPhoneVerified()) && invitedPhone.equals(u.getPhone()))
                .orElse(false);
    }

    /**
     * UC-74 (ADR-FAM-003): reads permission_json flag for a data category.
     * Open — permission_json shape owned by UC72. Default-deny when null or key missing.
     */
    @SuppressWarnings("unchecked")
    public boolean hasPermission(UUID groupId, UUID userId, PermissionFlag flag) {
        return memberRepository.findByCareGroupIdAndUserId(groupId, userId)
                .map(m -> {
                    if (!isActiveMembership(m)) {
                        return false;
                    }
                        String json = m.getPermissionJson();
                        if (json == null || json.isBlank()) return false; // Open — owned by UC72: default deny
                        try {
                            Map<String, Object> map = objectMapper.readValue(json, Map.class);
                            Object val = map.get(flag.jsonKey());
                            return Boolean.TRUE.equals(val);
                    } catch (Exception e) {
                        log.warn("Failed to parse permission_json for group={} user={}: {}", groupId, userId, e.getMessage());
                        return false; // malformed JSON → deny
                    }
                })
                .orElse(false);
    }

    private boolean isActiveMembership(CareGroupMember member) {
        if (member.getInviteStatus() == InviteStatus.ACCEPTED) {
            // Invitation expiry governs the pending invitation window. Once the
            // member has accepted, access is controlled by membership status and
            // permission/epoch state, not by the original invite deadline.
            return true;
        }
        return member.getInviteStatus() == InviteStatus.PENDING
                && (member.getInviteExpiresAt() == null
                || !member.getInviteExpiresAt().isBefore(Instant.now()));
    }
}

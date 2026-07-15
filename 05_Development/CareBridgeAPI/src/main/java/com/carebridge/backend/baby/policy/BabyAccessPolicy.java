package com.carebridge.backend.baby.policy;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.PermissionFlag;
import com.carebridge.backend.family.policy.CareGroupAuthorizationPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import com.carebridge.backend.family.entity.CareGroupStatus;

@Component
@RequiredArgsConstructor
public class BabyAccessPolicy {

    private final CareGroupRepository careGroupRepository;
    private final CareGroupAuthorizationPolicy authorizationPolicy;

    /**
     * Owner access is unconditional. Delegated access requires an active care
     * group linked to this baby, ACCEPTED membership, and explicit permission.
     */
    public boolean canView(BabyProfile profile, UUID callerId) {
        if (isOwner(profile, callerId)) {
            return true;
        }
        return hasPermission(profile, callerId, PermissionFlag.BABY_VIEW);
    }

    public boolean isOwner(BabyProfile profile, UUID callerId) {
        return profile.getOwnerUserId().equals(callerId);
    }

    public boolean canManage(BabyProfile profile, UUID callerId) {
        return canManageJournal(profile, callerId);
    }

    public boolean canManageJournal(BabyProfile profile, UUID callerId) {
        return isOwner(profile, callerId)
                || hasPermission(profile, callerId, PermissionFlag.BABY_JOURNAL_WRITE);
    }

    public boolean canManageGrowth(BabyProfile profile, UUID callerId) {
        return isOwner(profile, callerId)
                || hasPermission(profile, callerId, PermissionFlag.BABY_GROWTH_WRITE);
    }

    private boolean hasPermission(BabyProfile profile, UUID callerId, PermissionFlag permission) {
        return linkedGroups(profile.getId()).stream()
                .anyMatch(group -> authorizationPolicy.isMember(group.getId(), callerId)
                        && authorizationPolicy.hasPermission(group.getId(), callerId, permission));
    }

    private List<CareGroup> linkedGroups(UUID babyId) {
        return careGroupRepository.findByLinkedBabyProfileId(babyId).stream()
                .filter(group -> group.getStatus() == CareGroupStatus.ACTIVE)
                .toList();
    }
}

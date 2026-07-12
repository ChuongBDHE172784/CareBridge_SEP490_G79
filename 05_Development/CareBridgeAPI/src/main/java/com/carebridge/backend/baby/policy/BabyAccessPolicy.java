package com.carebridge.backend.baby.policy;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BabyAccessPolicy {

    private final CareGroupMemberRepository memberRepository;

    /**
     * Returns true if caller is the owner OR is an ACCEPTED member of any care group
     * that includes this baby's owner.
     * ADR-BABY-001: ACCEPTED status is required — PENDING is NOT sufficient.
     */
    public boolean canView(BabyProfile profile, UUID callerId) {
        if (profile.getOwnerUserId().equals(callerId)) {
            return true;
        }
        return memberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                profile.getId(), callerId, InviteStatus.ACCEPTED);
    }

    public boolean isOwner(BabyProfile profile, UUID callerId) {
        return profile.getOwnerUserId().equals(callerId);
    }

    public boolean canManage(BabyProfile profile, UUID callerId) {
        return isOwner(profile, callerId);
    }
}

package com.carebridge.backend.emergency.adapter;

import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FamilyMemberPortAdapter implements FamilyMemberPort {

    private final CareGroupRepository careGroupRepository;
    private final CareGroupMemberRepository careGroupMemberRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    public List<String> getFamilyFcmTokens(UUID userId) {
        List<CareGroup> groups = careGroupRepository.findByOwnerUserIdAndStatus(userId, CareGroupStatus.ACTIVE);

        return groups.stream()
                .flatMap(group -> careGroupMemberRepository
                        .findByCareGroupIdAndInviteStatusIn(group.getId(), List.of(InviteStatus.ACCEPTED))
                        .stream())
                .map(CareGroupMember::getUserId)
                .distinct()
                .flatMap(memberId -> deviceTokenRepository.findByUserIdAndActiveTrue(memberId).stream())
                .map(com.carebridge.backend.notification.entity.DeviceToken::getToken)
                .distinct()
                .toList();
    }

    @Override
    public boolean isFamilyMember(UUID ownerUserId, UUID candidateUserId) {
        List<CareGroup> groups = careGroupRepository.findByOwnerUserIdAndStatus(ownerUserId, CareGroupStatus.ACTIVE);
        return groups.stream().anyMatch(group -> careGroupMemberRepository
                .existsByCareGroupIdAndUserIdAndInviteStatus(group.getId(), candidateUserId, InviteStatus.ACCEPTED));
    }
}

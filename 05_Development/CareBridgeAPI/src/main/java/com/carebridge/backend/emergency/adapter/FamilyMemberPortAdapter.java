package com.carebridge.backend.emergency.adapter;

import com.carebridge.backend.emergency.service.AlertRecipientEndpoint;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class FamilyMemberPortAdapter implements FamilyMemberPort {

    private final CareGroupRepository careGroupRepository;
    private final CareGroupMemberRepository careGroupMemberRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    public List<String> getFamilyFcmTokens(UUID userId) {
        return getFamilyAlertRecipients(userId).stream()
                .filter(AlertRecipientEndpoint::hasPushEndpoint)
                .map(AlertRecipientEndpoint::token)
                .toList();
    }

    @Override
    public List<AlertRecipientEndpoint> getFamilyAlertRecipients(UUID userId) {
        return careGroupMemberRepository.findAcceptedFamilyMembersForEmergencyAlerts(userId).stream()
                .flatMap(member -> {
                    var activeTokens = deviceTokenRepository.findByUserIdAndActiveTrue(member.getUserId());
                    if (activeTokens.isEmpty()) {
                        return Stream.of(AlertRecipientEndpoint.inAppOnly(
                                member.getUserId(), member.getCareGroupId()));
                    }
                    return activeTokens.stream().map(token -> new AlertRecipientEndpoint(
                            member.getUserId(), token.getId(), member.getCareGroupId(), token.getToken()));
                })
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

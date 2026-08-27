package com.carebridge.backend.notification.service.impl;

import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.entity.InviteChannel;
import com.carebridge.backend.family.event.FamilyMemberInvited;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.notification.dto.SendNotificationRequest;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyNotificationService {

    private final NotificationService notificationService;
    private final CareGroupRepository groupRepository;
    private final CareGroupMemberRepository memberRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFamilyMemberInvited(FamilyMemberInvited event) {
        try {
            FamilyMemberInvited.Payload payload = event.payload();
            
            // Only PHONE channel creates a direct in-app notification because LINK/QR don't have a target user yet
            if (payload.channel() != InviteChannel.PHONE || payload.careGroupMemberId() == null) {
                return;
            }

            Optional<CareGroupMember> memberOpt = memberRepository.findById(payload.careGroupMemberId());
            if (memberOpt.isEmpty()) {
                log.warn("CareGroupMember not found for id {}", payload.careGroupMemberId());
                return;
            }

            CareGroupMember member = memberOpt.get();
            if (member.getUserId() == null) {
                log.warn("CareGroupMember has no target userId for notification {}", payload.careGroupMemberId());
                return;
            }

            Optional<CareGroup> groupOpt = groupRepository.findById(payload.careGroupId());
            String groupName = groupOpt.map(CareGroup::getGroupName).orElse("một nhóm");

            SendNotificationRequest request = new SendNotificationRequest(
                    member.getUserId(),
                    NotificationType.GROUP_INVITE,
                    "Lời mời tham gia nhóm",
                    "Bạn vừa nhận được lời mời tham gia nhóm gia đình: " + groupName,
                    payload.careGroupId(),
                    "CARE_GROUP"
            );

            notificationService.send(request);
            log.info("Sent family invite notification to user {}", member.getUserId());

        } catch (Exception e) {
            log.error("Failed to send family invite notification: {}", e.getMessage(), e);
        }
    }
}

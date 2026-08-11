package com.carebridge.backend.emergency.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.emergency.service.AlertRecipientEndpoint;
import com.carebridge.backend.family.entity.CareGroup;
import com.carebridge.backend.family.entity.CareGroupStatus;
import com.carebridge.backend.family.entity.InviteStatus;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FamilyMemberPortAdapterTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FIRST_CONTACT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SECOND_CONTACT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CARE_GROUP_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock private CareGroupRepository careGroupRepository;
    @Mock private CareGroupMemberRepository careGroupMemberRepository;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @InjectMocks private FamilyMemberPortAdapter adapter;

    @Test
    void recipientsUseAllAcceptedFamilyAccountsAndOnlyActiveDeviceTokens() {
        DeviceToken firstDevice = token(FIRST_CONTACT_ID, "first-device");
        DeviceToken secondDevice = token(SECOND_CONTACT_ID, "second-device");
        when(careGroupMemberRepository.findAcceptedFamilyMembersForEmergencyAlerts(OWNER_ID))
                .thenReturn(List.of(member(FIRST_CONTACT_ID), member(SECOND_CONTACT_ID)));
        when(deviceTokenRepository.findByUserIdAndActiveTrue(FIRST_CONTACT_ID))
                .thenReturn(List.of(firstDevice));
        when(deviceTokenRepository.findByUserIdAndActiveTrue(SECOND_CONTACT_ID))
                .thenReturn(List.of(secondDevice));

        List<AlertRecipientEndpoint> recipients = adapter.getFamilyAlertRecipients(OWNER_ID);

        assertThat(recipients).containsExactly(
                new AlertRecipientEndpoint(FIRST_CONTACT_ID, firstDevice.getId(), CARE_GROUP_ID, "first-device"),
                new AlertRecipientEndpoint(SECOND_CONTACT_ID, secondDevice.getId(), CARE_GROUP_ID, "second-device"));
        verify(careGroupMemberRepository).findAcceptedFamilyMembersForEmergencyAlerts(OWNER_ID);
        verifyNoInteractions(careGroupRepository);
    }

    @Test
    void acceptedFamilyAccountWithoutDeviceTokenStillReceivesInAppAlert() {
        when(careGroupMemberRepository.findAcceptedFamilyMembersForEmergencyAlerts(OWNER_ID))
                .thenReturn(List.of(member(FIRST_CONTACT_ID)));
        when(deviceTokenRepository.findByUserIdAndActiveTrue(FIRST_CONTACT_ID))
                .thenReturn(List.of());

        List<AlertRecipientEndpoint> recipients = adapter.getFamilyAlertRecipients(OWNER_ID);

        assertThat(recipients).containsExactly(
                AlertRecipientEndpoint.inAppOnly(FIRST_CONTACT_ID, CARE_GROUP_ID));
        assertThat(recipients.get(0).hasPushEndpoint()).isFalse();
    }

    @Test
    void isFamilyMemberKeepsAcceptedMembershipBehavior() {
        UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        CareGroup activeGroup = CareGroup.builder()
                .id(groupId)
                .ownerUserId(OWNER_ID)
                .status(CareGroupStatus.ACTIVE)
                .build();
        when(careGroupRepository.findByOwnerUserIdAndStatus(OWNER_ID, CareGroupStatus.ACTIVE))
                .thenReturn(List.of(activeGroup));
        when(careGroupMemberRepository.existsByCareGroupIdAndUserIdAndInviteStatus(
                groupId, FIRST_CONTACT_ID, InviteStatus.ACCEPTED)).thenReturn(true);

        assertThat(adapter.isFamilyMember(OWNER_ID, FIRST_CONTACT_ID)).isTrue();
    }

    private static DeviceToken token(UUID userId, String value) {
        return DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .token(value)
                .active(true)
                .build();
    }

    private static com.carebridge.backend.family.entity.CareGroupMember member(UUID userId) {
        return com.carebridge.backend.family.entity.CareGroupMember.builder()
                .careGroupId(CARE_GROUP_ID)
                .userId(userId)
                .inviteStatus(InviteStatus.ACCEPTED)
                .build();
    }
}

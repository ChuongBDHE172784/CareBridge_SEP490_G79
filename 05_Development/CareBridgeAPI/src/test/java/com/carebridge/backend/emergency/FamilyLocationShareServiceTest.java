package com.carebridge.backend.emergency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.emergency.dto.request.ShareLocationRequest;
import com.carebridge.backend.emergency.exception.EmergencyException;
import com.carebridge.backend.emergency.service.FamilyLocationShareService;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import com.carebridge.backend.family.entity.CareGroupMember;
import com.carebridge.backend.family.repository.CareGroupMemberRepository;
import com.carebridge.backend.notification.dto.FcmDeliveryResult;
import com.carebridge.backend.notification.entity.DeviceToken;
import com.carebridge.backend.notification.entity.NotificationRecord;
import com.carebridge.backend.notification.entity.NotificationType;
import com.carebridge.backend.notification.repository.DeviceTokenRepository;
import com.carebridge.backend.notification.repository.NotificationRecordRepository;
import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FamilyLocationShareServiceTest {

    @Mock private LocationConsentPort locationConsentPort;
    @Mock private CareGroupMemberRepository careGroupMemberRepository;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private NotificationRecordRepository notificationRecordRepository;
    @Mock private UserRepository userRepository;
    @Mock private FcmService fcmService;
    @Mock private AuditService auditService;
    @InjectMocks private FamilyLocationShareService service;

    private final UUID motherId = UUID.randomUUID();
    private final UUID familyId = UUID.randomUUID();
    private final UUID careGroupId = UUID.randomUUID();

    @Test
    void sharePersistsTypedLocationNotificationAndPushesToFamily() {
        when(locationConsentPort.hasLocationConsent(motherId)).thenReturn(true);
        when(careGroupMemberRepository.findAcceptedFamilyMembersForEmergencyAlerts(motherId))
                .thenReturn(List.of(CareGroupMember.builder()
                        .userId(familyId)
                        .careGroupId(careGroupId)
                        .build()));
        when(userRepository.findById(motherId)).thenReturn(Optional.of(User.builder()
                .id(motherId)
                .name("Mẹ Linh")
                .build()));
        when(deviceTokenRepository.findByUserIdAndActiveTrue(familyId))
                .thenReturn(List.of(DeviceToken.builder()
                        .id(UUID.randomUUID())
                        .userId(familyId)
                        .token("family-token")
                        .active(true)
                        .build()));
        when(notificationRecordRepository.saveAndFlush(any(NotificationRecord.class)))
                .thenAnswer(invocation -> {
                    NotificationRecord record = invocation.getArgument(0);
                    record.setId(UUID.randomUUID());
                    return record;
                });
        when(notificationRecordRepository.save(any(NotificationRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(fcmService.sendWithRetry(eq("family-token"), any(), any(), any(), eq(3)))
                .thenReturn(FcmDeliveryResult.success("message-id", 1));

        var response = service.share(motherId, request());

        assertThat(response.recipientCount()).isEqualTo(1);
        assertThat(response.pushDeliveredCount()).isEqualTo(1);
        ArgumentCaptor<NotificationRecord> recordCaptor =
                ArgumentCaptor.forClass(NotificationRecord.class);
        verify(notificationRecordRepository).saveAndFlush(recordCaptor.capture());
        NotificationRecord record = recordCaptor.getValue();
        assertThat(record.getType()).isEqualTo(NotificationType.LOCATION_SHARE);
        assertThat(record.getReferenceType()).isEqualTo("LOCATION_SHARE");
        assertThat(record.getMetadata())
                .containsEntry("motherName", "Mẹ Linh")
                .containsEntry("latitude", "10.762622")
                .containsEntry("longitude", "106.660172");
    }

    @Test
    void shareRejectsMissingLocationConsent() {
        when(locationConsentPort.hasLocationConsent(motherId)).thenReturn(false);

        assertThatThrownBy(() -> service.share(motherId, request()))
                .isInstanceOf(EmergencyException.class);
    }

    private ShareLocationRequest request() {
        return new ShareLocationRequest(
                new BigDecimal("10.762622"),
                new BigDecimal("106.660172"));
    }
}

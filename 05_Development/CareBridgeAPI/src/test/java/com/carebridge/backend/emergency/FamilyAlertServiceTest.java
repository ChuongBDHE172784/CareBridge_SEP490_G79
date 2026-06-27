package com.carebridge.backend.emergency;

import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.repository.IFamilyAlertLogRepository;
import com.carebridge.backend.emergency.service.FamilyMemberPort;
import com.carebridge.backend.emergency.service.FcmNotificationPort;
import com.carebridge.backend.emergency.service.LocationConsentPort;
import com.carebridge.backend.emergency.service.impl.FamilyAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamilyAlertServiceTest {

    @Mock
    private IFamilyAlertLogRepository familyAlertLogRepository;

    @Mock
    private FamilyMemberPort familyMemberPort;

    @Mock
    private FcmNotificationPort fcmNotificationPort;

    @Mock
    private LocationConsentPort locationConsentPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FamilyAlertService familyAlertService;

    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Test
    void sendAlert_alreadySent_shouldBeIdempotentNoOp() {
        // UC65 — idempotent: skip if alert already sent for session
        when(familyAlertLogRepository.existsBySessionId(SESSION_ID)).thenReturn(true);

        familyAlertService.sendAlert(EmergencyTestFactory.makeEmergencySessionOpenedEvent());

        verify(fcmNotificationPort, never()).sendBatch(anyList(), any());
    }

    @Test
    void sendAlert_noConsent_shouldNotIncludeLocation() {
        // UC65 — PDPA: no location when consent=false
        when(familyAlertLogRepository.existsBySessionId(SESSION_ID)).thenReturn(false);
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(false);
        when(familyMemberPort.getFamilyFcmTokens(USER_ID)).thenReturn(List.of("token-001"));

        EmergencySessionOpened eventWithLocation = new EmergencySessionOpened(
                UUID.randomUUID(), SESSION_ID, USER_ID, "MANUAL",
                new BigDecimal("10.123"), new BigDecimal("106.456"),
                Instant.now());

        familyAlertService.sendAlert(eventWithLocation);

        verify(fcmNotificationPort).sendBatch(anyList(),
                argThat(payload -> !payload.containsKey("latitude") && !payload.containsKey("longitude")));
    }

    @Test
    void sendAlert_withConsent_shouldIncludeLocation() {
        // UC65 — location included when consent=true
        when(familyAlertLogRepository.existsBySessionId(SESSION_ID)).thenReturn(false);
        when(locationConsentPort.hasLocationConsent(USER_ID)).thenReturn(true);
        when(familyMemberPort.getFamilyFcmTokens(USER_ID)).thenReturn(List.of("token-001", "token-002"));

        EmergencySessionOpened eventWithLocation = new EmergencySessionOpened(
                UUID.randomUUID(), SESSION_ID, USER_ID, "MANUAL",
                new BigDecimal("10.123"), new BigDecimal("106.456"),
                Instant.now());

        familyAlertService.sendAlert(eventWithLocation);

        verify(fcmNotificationPort).sendBatch(anyList(),
                argThat(payload -> payload.containsKey("latitude") && payload.containsKey("longitude")));
    }
}

package com.carebridge.backend.emergency;

import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.event.EmergencySessionRealertRequested;
import com.carebridge.backend.emergency.service.EmergencySessionOpenedHandler;
import com.carebridge.backend.emergency.service.IFamilyAlertService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class EmergencySessionOpenedHandlerTest {

    @Test
    void afterCommitDispatchUsesCanonicalFamilyAlertService() {
        IFamilyAlertService alerts = mock(IFamilyAlertService.class);
        EmergencySessionOpenedHandler handler = new EmergencySessionOpenedHandler(alerts);
        EmergencySessionOpened event = EmergencyTestFactory.makeEmergencySessionOpenedEvent();

        handler.onEmergencySessionOpened(event);

        verify(alerts).sendAlert(event);
    }

    @Test
    void deliveryFailureDoesNotSurfaceAfterEmergencyCommit() {
        IFamilyAlertService alerts = mock(IFamilyAlertService.class);
        EmergencySessionOpened event = EmergencyTestFactory.makeEmergencySessionOpenedEvent();
        doThrow(new IllegalStateException("delivery unavailable")).when(alerts).sendAlert(event);
        EmergencySessionOpenedHandler handler = new EmergencySessionOpenedHandler(alerts);

        assertThatCode(() -> handler.onEmergencySessionOpened(event)).doesNotThrowAnyException();
    }

    @Test
    void afterCommitRealertUsesTheSeparateThrottledDeliveryPath() {
        IFamilyAlertService alerts = mock(IFamilyAlertService.class);
        EmergencySessionRealertRequested event = new EmergencySessionRealertRequested(
                java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                "FALL_DETECTION", null, null, java.time.Instant.now());
        EmergencySessionOpenedHandler handler = new EmergencySessionOpenedHandler(alerts);

        handler.onEmergencySessionRealertRequested(event);

        verify(alerts).sendRealert(event);
    }
}

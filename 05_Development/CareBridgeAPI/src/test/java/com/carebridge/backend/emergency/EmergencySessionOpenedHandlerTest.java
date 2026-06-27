package com.carebridge.backend.emergency;

import com.carebridge.backend.emergency.event.EmergencySessionOpened;
import com.carebridge.backend.emergency.service.EmergencySessionOpenedHandler;
import com.carebridge.backend.emergency.service.IFamilyAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencySessionOpenedHandlerTest {

    @Mock
    private IFamilyAlertService familyAlertService;

    @InjectMocks
    private EmergencySessionOpenedHandler handler;

    @Test
    void onEmergencySessionOpened_shouldCallSendAlert() {
        EmergencySessionOpened event = EmergencyTestFactory.makeEmergencySessionOpenedEvent();
        handler.onEmergencySessionOpened(event);
        verify(familyAlertService).sendAlert(event);
    }
}

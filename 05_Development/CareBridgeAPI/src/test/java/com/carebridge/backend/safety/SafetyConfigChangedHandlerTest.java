package com.carebridge.backend.safety;

import com.carebridge.backend.safety.event.SafetyConfigChanged;
import com.carebridge.backend.safety.service.IFallDetectionService;
import com.carebridge.backend.safety.service.SafetyConfigChangedHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SafetyConfigChangedHandlerTest {

    @Mock
    private IFallDetectionService fallDetectionService;

    @InjectMocks
    private SafetyConfigChangedHandler handler;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Test
    void onConfigChanged_enabledTrue_shouldCallEnable() {
        // FD-TC-003
        SafetyConfigChanged event = SafetyConfigTestFactory.makeConfigChangedEvent(true);
        handler.onSafetyConfigChanged(event);
        verify(fallDetectionService).enable(USER_ID, "MEDIUM");
        verify(fallDetectionService, never()).disable(any());
    }

    @Test
    void onConfigChanged_enabledFalse_shouldCallDisable() {
        // DIS-TC-003
        SafetyConfigChanged event = SafetyConfigTestFactory.makeConfigChangedEvent(false);
        handler.onSafetyConfigChanged(event);
        verify(fallDetectionService).disable(USER_ID);
        verify(fallDetectionService, never()).enable(any(), any());
    }
}

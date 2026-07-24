package com.carebridge.backend.emergency;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.carebridge.backend.emergency.adapter.FcmNotificationPortAdapter;
import com.carebridge.backend.emergency.adapter.SmsFallbackPortAdapter;
import com.carebridge.backend.notification.service.FcmService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmergencyNotificationDeliveryAdaptersTest {

    @Mock private FcmService fcmService;

    @Test
    void fcmZeroSuccessfulDeliveries_shouldFailForSmsFallbackAndOutboxRetry() {
        FcmNotificationPortAdapter adapter = new FcmNotificationPortAdapter(fcmService);
        when(fcmService.sendToTokens(eq(List.of("token-1")), anyString(), anyString()))
                .thenReturn(0);

        assertThatThrownBy(() -> adapter.sendBatch(
                        List.of("token-1"), Map.of("triggerSource", "UNKNOWN")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no successful emergency deliveries");
    }

    @Test
    void fcmPositiveSuccessfulDeliveryCount_shouldComplete() {
        FcmNotificationPortAdapter adapter = new FcmNotificationPortAdapter(fcmService);
        when(fcmService.sendToTokens(eq(List.of("token-1")), anyString(), anyString()))
                .thenReturn(1);

        assertThatCode(() -> adapter.sendBatch(
                        List.of("token-1"), Map.of("triggerSource", "UNKNOWN")))
                .doesNotThrowAnyException();
    }

    @Test
    void unconfiguredSmsPlaceholder_shouldFailInsteadOfReportingDelivery() {
        SmsFallbackPortAdapter adapter = new SmsFallbackPortAdapter();

        assertThatThrownBy(() -> adapter.sendFallback(
                        UUID.randomUUID(), UUID.randomUUID(), "Emergency fallback"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }
}

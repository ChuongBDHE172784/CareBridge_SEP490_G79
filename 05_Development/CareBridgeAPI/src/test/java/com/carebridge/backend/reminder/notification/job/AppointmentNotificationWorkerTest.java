package com.carebridge.backend.reminder.notification.job;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.reminder.notification.service.AppointmentNotificationProcessingService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentNotificationWorkerTest {

    @Mock
    private AppointmentNotificationProcessingService processingService;

    @Mock
    private FcmService fcmService;

    @Test
    void disabledWorkerDoesNotClaimJobs() {
        AppointmentNotificationWorker worker =
                new AppointmentNotificationWorker(processingService, fcmService, false, 25);

        worker.poll();

        verify(processingService, never()).claimDueJobs(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void enabledWorkerClaimsAndProcessesEachDueJob() {
        when(fcmService.isReady()).thenReturn(true);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(processingService.claimDueJobs(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(10)))
                .thenReturn(List.of(first, second));
        AppointmentNotificationWorker worker =
                new AppointmentNotificationWorker(processingService, fcmService, true, 10);

        worker.poll();

        verify(processingService).claimDueJobs(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(10));
        verify(processingService).processAsync(org.mockito.ArgumentMatchers.eq(first),
                org.mockito.ArgumentMatchers.anyString());
        verify(processingService).processAsync(org.mockito.ArgumentMatchers.eq(second),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void enabledWorkerClampsInvalidBatchSizeBeforeClaiming() {
        when(fcmService.isReady()).thenReturn(true);
        when(processingService.claimDueJobs(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(List.of());
        AppointmentNotificationWorker worker =
                new AppointmentNotificationWorker(processingService, fcmService, true, 0);

        worker.poll();

        verify(processingService).claimDueJobs(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(1));
    }

    @Test
    void enabledWorkerDoesNotClaimWhenFcmIsNotReady() {
        when(fcmService.isReady()).thenReturn(false);
        AppointmentNotificationWorker worker =
                new AppointmentNotificationWorker(processingService, fcmService, true, 25);

        worker.poll();

        verify(processingService, never()).claimDueJobs(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
    }
}

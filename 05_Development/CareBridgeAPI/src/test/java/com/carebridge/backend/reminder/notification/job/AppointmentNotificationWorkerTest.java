package com.carebridge.backend.reminder.notification.job;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void disabledWorkerDoesNotClaimJobs() {
        AppointmentNotificationWorker worker =
                new AppointmentNotificationWorker(processingService, false, 25);

        worker.poll();

        verify(processingService, never()).claimDueJobs(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void enabledWorkerClaimsAndProcessesEachDueJob() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(processingService.claimDueJobs(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(10)))
                .thenReturn(List.of(first, second));
        AppointmentNotificationWorker worker =
                new AppointmentNotificationWorker(processingService, true, 10);

        worker.poll();

        verify(processingService).claimDueJobs(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(10));
        verify(processingService).processAsync(first);
        verify(processingService).processAsync(second);
    }
}

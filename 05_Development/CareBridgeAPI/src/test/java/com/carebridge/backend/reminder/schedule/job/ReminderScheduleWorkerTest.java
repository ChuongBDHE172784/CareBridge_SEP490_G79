package com.carebridge.backend.reminder.schedule.job;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.notification.service.FcmService;
import com.carebridge.backend.reminder.schedule.service.ReminderScheduleProcessingService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderScheduleWorkerTest {

    @Mock
    private ReminderScheduleProcessingService processingService;

    @Mock
    private FcmService fcmService;

    @Test
    void disabledWorkerDoesNotClaimJobs() {
        ReminderScheduleWorker worker =
                new ReminderScheduleWorker(processingService, fcmService, false, 25);

        worker.poll();

        verify(processingService, never()).claimDueJobs(anyString(), eq(25));
    }

    @Test
    void enabledWorkerPassesTheFencingWorkerIdToEachJob() {
        when(fcmService.isReady()).thenReturn(true);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(processingService.claimDueJobs(anyString(), eq(10)))
                .thenReturn(List.of(first, second));
        ReminderScheduleWorker worker =
                new ReminderScheduleWorker(processingService, fcmService, true, 10);

        worker.poll();

        ArgumentCaptor<String> claimWorker = ArgumentCaptor.forClass(String.class);
        verify(processingService).claimDueJobs(claimWorker.capture(), eq(10));
        verify(processingService).processAsync(eq(first), eq(claimWorker.getValue()));
        verify(processingService).processAsync(eq(second), eq(claimWorker.getValue()));
    }

    @Test
    void enabledWorkerClampsInvalidBatchSizeBeforeClaiming() {
        when(fcmService.isReady()).thenReturn(true);
        when(processingService.claimDueJobs(anyString(), eq(1))).thenReturn(List.of());
        ReminderScheduleWorker worker =
                new ReminderScheduleWorker(processingService, fcmService, true, 0);

        worker.poll();

        verify(processingService).claimDueJobs(anyString(), eq(1));
    }

    @Test
    void enabledWorkerDoesNotClaimWhenFcmIsNotReady() {
        when(fcmService.isReady()).thenReturn(false);
        ReminderScheduleWorker worker =
                new ReminderScheduleWorker(processingService, fcmService, true, 25);

        worker.poll();

        verify(processingService, never()).claimDueJobs(anyString(), org.mockito.ArgumentMatchers.anyInt());
    }
}

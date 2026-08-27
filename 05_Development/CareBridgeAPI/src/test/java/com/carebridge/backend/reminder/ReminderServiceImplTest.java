package com.carebridge.backend.reminder;


import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.reminder.dto.CreateReminderRequest;
import com.carebridge.backend.reminder.dto.CreateReminderResponse;
import com.carebridge.backend.reminder.dto.ReminderDetailResponse;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.notification.service.AppointmentNotificationScheduleService;
import com.carebridge.backend.reminder.service.INotificationService;
import com.carebridge.backend.reminder.service.impl.ReminderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderServiceImplTest {

    @Mock private ReminderRepository reminderRepository;
    @Mock private INotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private AppointmentNotificationScheduleService appointmentNotificationScheduleService;
    @Mock private MotherJourneyRepository motherJourneyRepository;
    @InjectMocks private ReminderServiceImpl reminderService;

    private static final UUID CALLER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID REMINDER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID JOURNEY_ID   = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private CreateReminderRequest makeRequest(Instant scheduledAt) {
        CreateReminderRequest req = new CreateReminderRequest();
        req.setReminderType(ReminderType.APPOINTMENT);
        req.setTitle("Doctor visit");
        req.setScheduledAt(scheduledAt);
        return req;
    }

    private Reminder savedReminder(UUID id) {
        return Reminder.builder()
                .id(id)
                .ownerUserId(CALLER_ID)
                .reminderType(ReminderType.APPOINTMENT)
                .title("Doctor visit")
                .scheduledAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .status(ReminderStatus.PENDING)
                .build();
    }

    // REM-TC-001: Happy path — valid future scheduledAt
    @Test
    void createReminder_validFutureDate_returnsPendingReminder() {
        Instant future = Instant.now().plus(10, ChronoUnit.MINUTES);
        when(reminderRepository.save(any())).thenReturn(savedReminder(REMINDER_ID));

        CreateReminderResponse resp = reminderService.createReminder(makeRequest(future), CALLER_ID);

        assertThat(resp.getId()).isEqualTo(REMINDER_ID);
        assertThat(resp.getStatus()).isEqualTo("PENDING");
    }

    // REM-TC-002: C1 — scheduledAt < now+5min → REM-001 / 400
    @Test
    void createReminder_scheduledAtTooSoon_throwsBusinessException400() {
        Instant tooSoon = Instant.now().plus(1, ChronoUnit.MINUTES);

        assertThatThrownBy(() -> reminderService.createReminder(makeRequest(tooSoon), CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("REM-001");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(reminderRepository, never()).save(any());
    }

    // REM-TC-003: C2 — durable appointment milestones are snapshotted synchronously after save
    @Test
    void createReminder_appointmentScheduleSnapshottedSynchronously() {
        Instant future = Instant.now().plus(10, ChronoUnit.MINUTES);
        Reminder saved = savedReminder(REMINDER_ID);
        when(reminderRepository.save(any())).thenReturn(saved);

        reminderService.createReminder(makeRequest(future), CALLER_ID);

        verify(appointmentNotificationScheduleService).createSnapshot(saved, null, null);
        verify(notificationService, never()).scheduleFcmPush(any(), any(), any(), any());
    }

    @Test
    void createReminder_contextlessAppointmentInheritsLatestActiveJourney() {
        Instant future = Instant.now().plus(10, ChronoUnit.MINUTES);
        when(motherJourneyRepository.findFirstByOwnerUserIdAndStatusOrderByCreatedAtDesc(
                CALLER_ID, JourneyStatus.ACTIVE))
                .thenReturn(Optional.of(MotherJourney.builder().id(JOURNEY_ID).build()));
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> {
            Reminder reminder = invocation.getArgument(0);
            reminder.setId(REMINDER_ID);
            return reminder;
        });

        reminderService.createReminder(makeRequest(future), CALLER_ID);

        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().getFirst().getJourneyId()).isEqualTo(JOURNEY_ID);
    }

    @Test
    void createReminder_explicitContextIsNotOverwrittenByFallback() {
        Instant future = Instant.now().plus(10, ChronoUnit.MINUTES);
        CreateReminderRequest request = makeRequest(future);
        request.setJourneyId(JOURNEY_ID);
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> {
            Reminder reminder = invocation.getArgument(0);
            reminder.setId(REMINDER_ID);
            return reminder;
        });

        reminderService.createReminder(request, CALLER_ID);

        verify(motherJourneyRepository, never())
                .findFirstByOwnerUserIdAndStatusOrderByCreatedAtDesc(any(), any());
        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().getFirst().getJourneyId()).isEqualTo(JOURNEY_ID);
    }

    // REM-VIEW-TC-001: Owner views PENDING reminder → 200 with correct status and type
    @Test
    void getReminderDetail_pendingReminder_returnsCorrectStatusAndType() {
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(savedReminder(REMINDER_ID)));

        ReminderDetailResponse resp = reminderService.getReminderDetail(REMINDER_ID, CALLER_ID);

        assertThat(resp.getStatus()).isEqualTo("PENDING");
        assertThat(resp.getReminderType()).isEqualTo("APPOINTMENT");
        assertThat(resp.getTitle()).isEqualTo("Doctor visit");
    }

    // REM-TC-004: View — owner accesses reminder (including CANCELLED — ADR-REM-002)
    @Test
    void getReminderDetail_cancelledReminder_returns200() {
        Reminder cancelled = savedReminder(REMINDER_ID);
        cancelled.setStatus(ReminderStatus.CANCELLED);
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(cancelled));

        ReminderDetailResponse resp = reminderService.getReminderDetail(REMINDER_ID, CALLER_ID);

        assertThat(resp.getStatus()).isEqualTo("CANCELLED");
    }

    // REM-TC-005: View — non-owner → REM-004 / 403
    @Test
    void getReminderDetail_notOwner_throwsBusinessException403() {
        Reminder reminder = Reminder.builder()
                .id(REMINDER_ID).ownerUserId(UUID.randomUUID())
                .status(ReminderStatus.PENDING).build();
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(reminder));

        assertThatThrownBy(() -> reminderService.getReminderDetail(REMINDER_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("REM-004");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    // REM-TC-006: View — not found → REM-006 / 404
    @Test
    void getReminderDetail_notFound_throwsBusinessException404() {
        when(reminderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.getReminderDetail(REMINDER_ID, CALLER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getHttpStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // REM-TC-007: No dosage/prescription fields in response (BR-SAFETY)
    @Test
    void getReminderDetail_noDosageOrPrescriptionInResponse() {
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(savedReminder(REMINDER_ID)));

        ReminderDetailResponse resp = reminderService.getReminderDetail(REMINDER_ID, CALLER_ID);

        assertThat(resp.toString()).doesNotContainIgnoringCase("dosage");
        assertThat(resp.toString()).doesNotContainIgnoringCase("prescription");
    }
}

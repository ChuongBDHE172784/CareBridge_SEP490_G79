package com.carebridge.backend.reminder;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.reminder.dto.CreateReminderResponse;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.INotificationService;
import com.carebridge.backend.reminder.service.impl.ReminderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeastOnce;

/**
 * UC46 — Create Medication Reminder service tests.
 * TDD Red Phase: all tests must FAIL until Green implementation.
 */
@ExtendWith(MockitoExtension.class)
class MedicationReminderServiceTest {

    @Mock private ReminderRepository reminderRepository;
    @Mock private INotificationService notificationService;
    @Mock private AuditService auditService;
    @InjectMocks private ReminderServiceImpl reminderService;

    // REMINDER-TC-001: Happy path — valid medication reminder created
    @Test
    void createMedicationReminder_validRequest_returnsPendingMedicationReminder() {
        var request = ReminderTestFactory.validMedicationRequest();
        var saved = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.save(any())).thenReturn(saved);
        when(notificationService.scheduleFcmPush(any(), any(), any(), any())).thenReturn("fcm-med-1");

        CreateReminderResponse resp = reminderService.createMedicationReminder(request, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getReminderType()).isEqualTo("MEDICATION");
        assertThat(resp.getStatus()).isEqualTo("PENDING");
        assertThat(resp.getId()).isNotNull();
    }

    // REMINDER-TC-002: C1 — scheduledAt < now+5min → REM-001 / 400
    @Test
    void createMedicationReminder_scheduledAtTooSoon_throwsBusinessException400() {
        var request = ReminderTestFactory.medicationRequestTooSoon();

        assertThatThrownBy(() ->
                reminderService.createMedicationReminder(request, ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("REM-001");
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(reminderRepository, never()).save(any());
    }

    // REMINDER-TC-003: C1 — scheduledAt in past → REM-001 / 400
    @Test
    void createMedicationReminder_scheduledAtInPast_throwsBusinessException400() {
        var request = ReminderTestFactory.medicationRequestInPast();

        assertThatThrownBy(() ->
                reminderService.createMedicationReminder(request, ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                        assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // REMINDER-TC-004: C2 — reminderType MUST be MEDICATION (hardcoded in service)
    @Test
    void createMedicationReminder_reminderTypeHardcodedToMedication() {
        var request = ReminderTestFactory.validMedicationRequest();
        var saved = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.save(any())).thenReturn(saved);
        when(notificationService.scheduleFcmPush(any(), any(), any(), any())).thenReturn("job");

        var resp = reminderService.createMedicationReminder(request, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getReminderType()).isEqualTo("MEDICATION");
    }

    // REMINDER-TC-005: C3 — FCM push scheduled after save
    @Test
    void createMedicationReminder_fcmScheduledAfterSave() {
        var request = ReminderTestFactory.validMedicationRequest();
        var saved = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.save(any())).thenReturn(saved);
        when(notificationService.scheduleFcmPush(any(), any(), any(), any())).thenReturn("fcm-job");

        reminderService.createMedicationReminder(request, ReminderTestFactory.OWNER_ID);

        verify(notificationService).scheduleFcmPush(
                eq(ReminderTestFactory.OWNER_ID), any(), any(), any());
    }

    // REMINDER-TC-006: BR-SAFETY — no dosage/prescription fields in response
    @Test
    void createMedicationReminder_noDosageOrPrescriptionInResponse() {
        var request = ReminderTestFactory.validMedicationRequest();
        var saved = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.save(any())).thenReturn(saved);
        when(notificationService.scheduleFcmPush(any(), any(), any(), any())).thenReturn("job");

        var resp = reminderService.createMedicationReminder(request, ReminderTestFactory.OWNER_ID);

        assertThat(resp.toString()).doesNotContainIgnoringCase("dosage");
        assertThat(resp.toString()).doesNotContainIgnoringCase("prescription");
    }

    // REMINDER-TC-007: C4 — ownerUserId from JWT callerId (not from request)
    @Test
    void createMedicationReminder_ownerUserIdFromCallerId() {
        var request = ReminderTestFactory.validMedicationRequest();
        var saved = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.save(any())).thenReturn(saved);
        when(notificationService.scheduleFcmPush(any(), any(), any(), any())).thenReturn("job");

        reminderService.createMedicationReminder(request, ReminderTestFactory.OWNER_ID);

        verify(reminderRepository, atLeastOnce()).save(argThat(r ->
                ReminderTestFactory.OWNER_ID.equals(r.getOwnerUserId())));
    }
}

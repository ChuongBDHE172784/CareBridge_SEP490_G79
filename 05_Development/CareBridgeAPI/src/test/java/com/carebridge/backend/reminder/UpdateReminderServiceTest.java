package com.carebridge.backend.reminder;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.reminder.dto.ReminderDetailResponse;
import com.carebridge.backend.reminder.entity.ReminderStatus;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UC48 — Update / Snooze / Complete / Skip Reminder service tests.
 * TDD Red Phase: all tests must FAIL until Green implementation.
 */
@ExtendWith(MockitoExtension.class)
class UpdateReminderServiceTest {

    @Mock private ReminderRepository reminderRepository;
    @Mock private INotificationService notificationService;
    @Mock private AuditService auditService;
    @InjectMocks private ReminderServiceImpl reminderService;

    // UPD-TC-001: Happy path — update title and scheduledAt
    @Test
    void updateReminder_validRequest_returnsUpdatedReminder() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));
        when(reminderRepository.save(any())).thenReturn(pending);
        var request = ReminderTestFactory.validUpdateRequest();

        ReminderDetailResponse resp = reminderService.updateReminder(
                ReminderTestFactory.REMINDER_ID, request, ReminderTestFactory.OWNER_ID);

        assertThat(resp).isNotNull();
    }

    // UPD-TC-002: Not found → REM-006 / 404
    @Test
    void updateReminder_notFound_throwsBusinessException404() {
        when(reminderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.updateReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.validUpdateRequest(),
                ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                        assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // UPD-TC-003: Ownership check — non-owner → REM-004 / 403
    @Test
    void updateReminder_notOwner_throwsBusinessException403() {
        var other = ReminderTestFactory.reminderOwnedByOther(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() -> reminderService.updateReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.validUpdateRequest(),
                ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                        assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // UPD-TC-004: ADR-REM-STATE-001 — COMPLETED is terminal; update blocked → 409
    @Test
    void updateReminder_completedReminder_throwsBusinessException409() {
        var completed = ReminderTestFactory.completedReminder();
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> reminderService.updateReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.validUpdateRequest(),
                ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("REM-007");
                });
    }

    // UPD-TC-005: ADR-REM-STATE-001 — SKIPPED is terminal; update blocked → 409
    @Test
    void updateReminder_skippedReminder_throwsBusinessException409() {
        var skipped = ReminderTestFactory.skippedReminder();
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(skipped));

        assertThatThrownBy(() -> reminderService.updateReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.validUpdateRequest(),
                ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                        assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // UPD-TC-006: Snooze happy path — PENDING → SNOOZED
    @Test
    void snoozeReminder_pendingReminder_returnsSnoozedStatus() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));
        when(reminderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.snoozeReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.validSnoozeRequest(),
                ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.SNOOZED.name());
        assertThat(resp.getSnoozedUntil()).isNotNull();
    }

    // UPD-TC-007: Snooze — snoozedUntil > 24h in future → REM-008 / 400
    @Test
    void snoozeReminder_snoozedUntilTooFar_throwsBusinessException400() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> reminderService.snoozeReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.snoozeTooFarRequest(),
                ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getCode()).isEqualTo("REM-008");
                });
    }

    // UPD-TC-008: Snooze — snoozedUntil in past → REM-005 / 400
    @Test
    void snoozeReminder_snoozedUntilInPast_throwsBusinessException400() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> reminderService.snoozeReminder(
                ReminderTestFactory.REMINDER_ID,
                ReminderTestFactory.snoozeInPastRequest(),
                ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                        assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // UPD-TC-009: Complete reminder happy path → status = COMPLETED
    @Test
    void completeReminder_pendingReminder_returnsCompletedStatus() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));
        when(reminderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.completeReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.COMPLETED.name());
    }

    // UPD-TC-010: Skip reminder happy path → status = SKIPPED
    @Test
    void skipReminder_pendingReminder_returnsSkippedStatus() {
        var pending = ReminderTestFactory.pendingReminder(ReminderType.MEDICATION);
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(pending));
        when(reminderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReminderDetailResponse resp = reminderService.skipReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getStatus()).isEqualTo(ReminderStatus.SKIPPED.name());
    }

    // UPD-TC-011: Complete — COMPLETED is terminal; double-complete → 409
    @Test
    void completeReminder_alreadyCompleted_throwsBusinessException409() {
        var completed = ReminderTestFactory.completedReminder();
        when(reminderRepository.findById(ReminderTestFactory.REMINDER_ID))
                .thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> reminderService.completeReminder(
                ReminderTestFactory.REMINDER_ID, ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                        assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));
    }
}

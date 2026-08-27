package com.carebridge.backend.vaccination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.dto.VaccinationReminderCommand;
import com.carebridge.backend.notification.service.IVaccinationNotificationService;
import com.carebridge.backend.vaccination.config.VaccinationProperties;
import com.carebridge.backend.vaccination.entity.VaccinationRecord;
import com.carebridge.backend.vaccination.entity.VaccinationRecordStatus;
import com.carebridge.backend.vaccination.repository.VaccinationRecordRepository;
import com.carebridge.backend.vaccination.service.impl.VaccinationReminderService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * MF-03 step 2 — the mother is reminded from the materialised book as a dose approaches.
 */
@ExtendWith(MockitoExtension.class)
class VaccinationReminderDispatchTest {

    private static final UUID BABY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 7);

    @Mock private VaccinationRecordRepository recordRepository;
    @Mock private BabyProfileRepository babyRepository;
    @Mock private IVaccinationNotificationService notificationService;
    @Spy private VaccinationProperties properties = new VaccinationProperties();
    @InjectMocks private VaccinationReminderService reminderService;

    private BabyProfile baby(BabyProfileStatus status) {
        return BabyProfile.builder()
                .id(BABY_ID)
                .ownerUserId(OWNER_ID)
                .nickname("Bean")
                .birthDate(LocalDate.of(2026, 1, 15))
                .status(status)
                .build();
    }

    private VaccinationRecord record(String vaccine, int dose, LocalDate scheduledDate,
                                     VaccinationRecordStatus status) {
        return VaccinationRecord.builder()
                .id(UUID.randomUUID())
                .babyId(BABY_ID)
                .vaccineName(vaccine)
                .doseNumber((short) dose)
                .scheduledDate(scheduledDate)
                .status(status)
                .build();
    }

    private void arrangeDue(VaccinationRecord... records) {
        when(recordRepository.findByStatusInAndScheduledDateBetween(
                List.of(VaccinationRecordStatus.SCHEDULED, VaccinationRecordStatus.POSTPONED),
                TODAY, TODAY.plusDays(7)))
                .thenReturn(List.of(records));
    }

    private void arrangeBaby(BabyProfileStatus status) {
        when(babyRepository.findAllById(List.of(BABY_ID))).thenReturn(List.of(baby(status)));
    }

    private void arrangeDelivery() {
        when(notificationService.sendVaccinationReminder(any()))
                .thenReturn(new NotificationRecordResponse(
                        UUID.randomUUID(), OWNER_ID, "REMINDER", "t", "b", UUID.randomUUID(),
                        "VACCINATION", "SENT", null, null, false, null, "PUSH", "m", 1, null, null, null));
    }

    private List<VaccinationReminderCommand> capturedCommands() {
        ArgumentCaptor<VaccinationReminderCommand> captor =
                ArgumentCaptor.forClass(VaccinationReminderCommand.class);
        verify(notificationService, org.mockito.Mockito.atLeastOnce())
                .sendVaccinationReminder(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void dispatch_doseSevenDaysOut_remindsTheMotherAtTheSevenDayLead() {
        arrangeDue(record("Vắc-xin 6 trong 1", 1, TODAY.plusDays(7), VaccinationRecordStatus.SCHEDULED));
        arrangeBaby(BabyProfileStatus.ACTIVE);
        arrangeDelivery();

        assertThat(reminderService.dispatchDueReminders(TODAY)).isEqualTo(1);

        VaccinationReminderCommand command = capturedCommands().getFirst();
        assertThat(command.daysBefore()).isEqualTo(7);
        assertThat(command.userId()).isEqualTo(OWNER_ID);
        assertThat(command.babyNickname()).isEqualTo("Bean");
        assertThat(command.vaccineName()).isEqualTo("Vắc-xin 6 trong 1");
        assertThat(command.scheduledDate()).isEqualTo(TODAY.plusDays(7));
    }

    @Test
    void dispatch_doseDueToday_remindsAtTheZeroDayLead() {
        arrangeDue(record("Lao (BCG)", 1, TODAY, VaccinationRecordStatus.SCHEDULED));
        arrangeBaby(BabyProfileStatus.ACTIVE);
        arrangeDelivery();

        assertThat(reminderService.dispatchDueReminders(TODAY)).isEqualTo(1);
        assertThat(capturedCommands().getFirst().daysBefore()).isZero();
    }

    @Test
    void dispatch_doseBetweenLeads_staysSilentUntilItsNextMilestone() {
        // Five days out matches none of 7/3/1/0 — the mother is not nagged every single day.
        arrangeDue(record("Thủy đậu", 1, TODAY.plusDays(5), VaccinationRecordStatus.SCHEDULED));

        assertThat(reminderService.dispatchDueReminders(TODAY)).isZero();
        verify(notificationService, never()).sendVaccinationReminder(any());
    }

    @Test
    void dispatch_postponedDose_stillRemindsOnTheNewDate() {
        arrangeDue(record("Thủy đậu", 1, TODAY.plusDays(1), VaccinationRecordStatus.POSTPONED));
        arrangeBaby(BabyProfileStatus.ACTIVE);
        arrangeDelivery();

        assertThat(reminderService.dispatchDueReminders(TODAY)).isEqualTo(1);
        assertThat(capturedCommands().getFirst().daysBefore()).isEqualTo(1);
    }

    @Test
    void dispatch_archivedBaby_producesNoReminders() {
        arrangeDue(record("Lao (BCG)", 1, TODAY, VaccinationRecordStatus.SCHEDULED));
        arrangeBaby(BabyProfileStatus.ARCHIVED);

        assertThat(reminderService.dispatchDueReminders(TODAY)).isZero();
        verify(notificationService, never()).sendVaccinationReminder(any());
    }

    @Test
    void dispatch_alreadyDeliveredMilestone_isNotCountedAgain() {
        arrangeDue(record("Lao (BCG)", 1, TODAY, VaccinationRecordStatus.SCHEDULED));
        arrangeBaby(BabyProfileStatus.ACTIVE);
        // A null response is how the sender reports "already delivered / push disabled".
        when(notificationService.sendVaccinationReminder(any())).thenReturn(null);

        assertThat(reminderService.dispatchDueReminders(TODAY)).isZero();
    }

    @Test
    void dispatch_oneFailingDose_doesNotAbortTheRest() {
        arrangeDue(
                record("Lao (BCG)", 1, TODAY, VaccinationRecordStatus.SCHEDULED),
                record("Vắc-xin 6 trong 1", 1, TODAY, VaccinationRecordStatus.SCHEDULED));
        arrangeBaby(BabyProfileStatus.ACTIVE);
        when(notificationService.sendVaccinationReminder(any()))
                .thenThrow(new IllegalStateException("FCM unavailable"))
                .thenReturn(new NotificationRecordResponse(
                        UUID.randomUUID(), OWNER_ID, "REMINDER", "t", "b", UUID.randomUUID(),
                        "VACCINATION", "SENT", null, null, false, null, "PUSH", "m", 1, null, null, null));

        assertThat(reminderService.dispatchDueReminders(TODAY)).isEqualTo(1);
    }

    @Test
    void dispatch_disabledByConfiguration_doesNotEvenQueryTheBook() {
        properties.getReminder().setEnabled(false);

        assertThat(reminderService.dispatchDueReminders(TODAY)).isZero();
        verify(recordRepository, never()).findByStatusInAndScheduledDateBetween(any(), any(), any());
    }
}

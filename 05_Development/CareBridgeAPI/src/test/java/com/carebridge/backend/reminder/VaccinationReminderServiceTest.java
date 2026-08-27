package com.carebridge.backend.reminder;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.reminder.dto.CreateReminderResponse;
import com.carebridge.backend.reminder.dto.VaccinationSuggestionDto;
import com.carebridge.backend.reminder.entity.ReminderType;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.INotificationService;
import com.carebridge.backend.reminder.service.impl.ReminderServiceImpl;
import com.carebridge.backend.vaccination.entity.VaccinationRecord;
import com.carebridge.backend.vaccination.entity.VaccinationRecordStatus;
import com.carebridge.backend.vaccination.repository.VaccinationRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeastOnce;

/**
 * UC47 — Create Vaccination Reminder service tests.
 * TDD Red Phase: all tests must FAIL until Green implementation.
 */
@ExtendWith(MockitoExtension.class)
class VaccinationReminderServiceTest {

    @Mock private ReminderRepository reminderRepository;
    @Mock private INotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private BabyProfileRepository babyProfileRepository;
    @Mock private VaccinationRecordRepository vaccinationRecordRepository;
    @InjectMocks private ReminderServiceImpl reminderService;

    // VAC-TC-001: Happy path — valid vaccination reminder created
    @Test
    void createVaccinationReminder_validRequest_returnsPendingVaccinationReminder() {
        var request = ReminderTestFactory.validVaccinationRequest();
        var baby = BabyProfile.builder().id(ReminderTestFactory.BABY_ID)
                .ownerUserId(ReminderTestFactory.OWNER_ID).build();
        var saved = ReminderTestFactory.pendingReminder(ReminderType.VACCINATION);
        when(babyProfileRepository.findByIdAndOwnerUserId(
                ReminderTestFactory.BABY_ID, ReminderTestFactory.OWNER_ID))
                .thenReturn(Optional.of(baby));
        when(reminderRepository.save(any())).thenReturn(saved);
        when(notificationService.scheduleFcmPush(any(), any(), any(), any())).thenReturn("fcm-vac-1");

        CreateReminderResponse resp = reminderService.createVaccinationReminder(
                request, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getReminderType()).isEqualTo("VACCINATION");
        assertThat(resp.getStatus()).isEqualTo("PENDING");
    }

    // VAC-TC-002: babyId not provided → validation error (handled at controller layer)
    // VAC-TC-003: babyId does not belong to caller → REM-003 / 403
    @Test
    void createVaccinationReminder_babyNotOwnedByCaller_throwsBusinessException403() {
        var request = ReminderTestFactory.validVaccinationRequest();
        when(babyProfileRepository.findByIdAndOwnerUserId(
                ReminderTestFactory.BABY_ID, ReminderTestFactory.OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reminderService.createVaccinationReminder(request, ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("REM-003");
                });

        verify(reminderRepository, never()).save(any());
    }

    // VAC-TC-004: C1 — scheduledAt < now+5min → REM-001 / 400
    @Test
    void createVaccinationReminder_scheduledAtTooSoon_throwsBusinessException400() {
        var request = ReminderTestFactory.vaccinationRequestTooSoon();
        var baby = BabyProfile.builder().id(ReminderTestFactory.BABY_ID)
                .ownerUserId(ReminderTestFactory.OWNER_ID).build();
        when(babyProfileRepository.findByIdAndOwnerUserId(any(), any()))
                .thenReturn(Optional.of(baby));

        assertThatThrownBy(() ->
                reminderService.createVaccinationReminder(request, ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                        assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // VAC-TC-005: reminderType hardcoded VACCINATION
    @Test
    void createVaccinationReminder_reminderTypeHardcodedToVaccination() {
        var request = ReminderTestFactory.validVaccinationRequest();
        var baby = BabyProfile.builder().id(ReminderTestFactory.BABY_ID)
                .ownerUserId(ReminderTestFactory.OWNER_ID).build();
        var saved = ReminderTestFactory.pendingReminder(ReminderType.VACCINATION);
        when(babyProfileRepository.findByIdAndOwnerUserId(any(), any())).thenReturn(Optional.of(baby));
        when(reminderRepository.save(any())).thenReturn(saved);
        when(notificationService.scheduleFcmPush(any(), any(), any(), any())).thenReturn("job");

        var resp = reminderService.createVaccinationReminder(request, ReminderTestFactory.OWNER_ID);

        assertThat(resp.getReminderType()).isEqualTo("VACCINATION");
        verify(reminderRepository, atLeastOnce()).save(argThat(r ->
                ReminderType.VACCINATION.equals(r.getReminderType())));
    }

    // VAC-TC-006: Vaccination suggestions — returns SCHEDULED records for baby
    @Test
    void getVaccinationSuggestions_withScheduledRecords_returnsSuggestions() {
        var baby = BabyProfile.builder().id(ReminderTestFactory.BABY_ID)
                .ownerUserId(ReminderTestFactory.OWNER_ID).build();
        var record = VaccinationRecord.builder()
                .babyId(ReminderTestFactory.BABY_ID)
                .vaccineName("BCG")
                .doseNumber((short) 1)
                .scheduledDate(LocalDate.now().plusDays(7))
                .status(VaccinationRecordStatus.SCHEDULED)
                .build();
        when(babyProfileRepository.findByIdAndOwnerUserId(
                ReminderTestFactory.BABY_ID, ReminderTestFactory.OWNER_ID))
                .thenReturn(Optional.of(baby));
        when(vaccinationRecordRepository.findByBabyIdAndStatus(
                ReminderTestFactory.BABY_ID, VaccinationRecordStatus.SCHEDULED))
                .thenReturn(List.of(record));

        List<VaccinationSuggestionDto> suggestions = reminderService.getVaccinationSuggestions(
                ReminderTestFactory.BABY_ID, ReminderTestFactory.OWNER_ID);

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).getVaccineName()).isEqualTo("BCG");
        assertThat(suggestions.get(0).getBabyId()).isEqualTo(ReminderTestFactory.BABY_ID);
    }

    // VAC-TC-007: Suggestions — baby not owned by caller → REM-003 / 403
    @Test
    void getVaccinationSuggestions_babyNotOwned_throwsBusinessException403() {
        when(babyProfileRepository.findByIdAndOwnerUserId(
                ReminderTestFactory.BABY_ID, ReminderTestFactory.OWNER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.getVaccinationSuggestions(
                ReminderTestFactory.BABY_ID, ReminderTestFactory.OWNER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex ->
                        assertThat(((BusinessException) ex).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // VAC-TC-008: Suggestions — empty when no SCHEDULED records exist
    @Test
    void getVaccinationSuggestions_noScheduledRecords_returnsEmptyList() {
        var baby = BabyProfile.builder().id(ReminderTestFactory.BABY_ID)
                .ownerUserId(ReminderTestFactory.OWNER_ID).build();
        when(babyProfileRepository.findByIdAndOwnerUserId(any(), any()))
                .thenReturn(Optional.of(baby));
        when(vaccinationRecordRepository.findByBabyIdAndStatus(any(), any()))
                .thenReturn(List.of());

        var suggestions = reminderService.getVaccinationSuggestions(
                ReminderTestFactory.BABY_ID, ReminderTestFactory.OWNER_ID);

        assertThat(suggestions).isEmpty();
    }
}

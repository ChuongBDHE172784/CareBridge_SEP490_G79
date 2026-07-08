package com.carebridge.backend.reminder.service;

import com.carebridge.backend.reminder.dto.CreateMedicationReminderRequest;
import com.carebridge.backend.reminder.dto.CreateReminderRequest;
import com.carebridge.backend.reminder.dto.CreateReminderResponse;
import com.carebridge.backend.reminder.dto.CreateVaccinationReminderRequest;
import com.carebridge.backend.reminder.dto.ReminderDetailResponse;
import com.carebridge.backend.reminder.dto.SnoozeReminderRequest;
import com.carebridge.backend.reminder.dto.UpdateReminderRequest;
import com.carebridge.backend.reminder.dto.VaccinationSuggestionDto;

import java.util.List;
import java.util.UUID;

public interface IReminderService {

    /** @throws com.carebridge.backend.common.exception.BusinessException (REM-001/400) if scheduledAt < now+5min */
    CreateReminderResponse createReminder(CreateReminderRequest request, UUID callerId);

    /** @throws com.carebridge.backend.common.exception.BusinessException (REM-004/403) if not owner */
    ReminderDetailResponse getReminderDetail(UUID reminderId, UUID callerId);

    /** UC46 — reminderType hardcoded MEDICATION */
    CreateReminderResponse createMedicationReminder(CreateMedicationReminderRequest request, UUID callerId);

    /** UC47 — reminderType hardcoded VACCINATION; babyId validated against owner */
    CreateReminderResponse createVaccinationReminder(CreateVaccinationReminderRequest request, UUID callerId);

    /** UC47 — return upcoming SCHEDULED vaccination records as reminder suggestions */
    List<VaccinationSuggestionDto> getVaccinationSuggestions(UUID babyId, UUID callerId);

    /** UC48 — partial update; scheduledAt must be >= now+5min if provided */
    ReminderDetailResponse updateReminder(UUID reminderId, UpdateReminderRequest request, UUID callerId);

    /** UC48 — snooze PENDING/SNOOZED reminder; snoozedUntil must be 1–24h in future */
    ReminderDetailResponse snoozeReminder(UUID reminderId, SnoozeReminderRequest request, UUID callerId);

    /** UC48 — mark reminder COMPLETED; COMPLETED/SKIPPED are terminal and immutable */
    ReminderDetailResponse completeReminder(UUID reminderId, UUID callerId);

    /** UC48 — mark reminder SKIPPED; COMPLETED/SKIPPED are terminal and immutable */
    ReminderDetailResponse skipReminder(UUID reminderId, UUID callerId);
}

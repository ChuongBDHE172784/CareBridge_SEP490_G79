package com.carebridge.backend.reminder.service;

import com.carebridge.backend.reminder.dto.CreateReminderRequest;
import com.carebridge.backend.reminder.dto.CreateReminderResponse;
import com.carebridge.backend.reminder.dto.ReminderDetailResponse;

import java.util.UUID;

public interface IReminderService {

    /** @throws com.carebridge.backend.common.exception.BusinessException (REM-001/400) if scheduledAt < now+5min */
    CreateReminderResponse createReminder(CreateReminderRequest request, UUID callerId);

    /** @throws com.carebridge.backend.common.exception.BusinessException (REM-004/403) if not owner */
    ReminderDetailResponse getReminderDetail(UUID reminderId, UUID callerId);
}

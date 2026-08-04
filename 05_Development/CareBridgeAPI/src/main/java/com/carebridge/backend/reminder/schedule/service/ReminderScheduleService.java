package com.carebridge.backend.reminder.schedule.service;

import com.carebridge.backend.reminder.schedule.dto.CreateReminderScheduleRequest;
import com.carebridge.backend.reminder.schedule.dto.ReminderScheduleResponse;
import com.carebridge.backend.reminder.schedule.dto.UpdateReminderScheduleRequest;
import java.util.List;
import java.util.UUID;

public interface ReminderScheduleService {
    ReminderScheduleResponse create(UUID ownerUserId, CreateReminderScheduleRequest request);

    List<ReminderScheduleResponse> list(UUID ownerUserId);

    ReminderScheduleResponse get(UUID ownerUserId, UUID scheduleId);

    ReminderScheduleResponse update(UUID ownerUserId, UUID scheduleId,
                                    UpdateReminderScheduleRequest request);

    void delete(UUID ownerUserId, UUID scheduleId);
}

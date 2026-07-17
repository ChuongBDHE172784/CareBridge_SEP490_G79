package com.carebridge.backend.reminder.service.impl;

import com.carebridge.backend.reminder.dto.TodayTaskItem;
import com.carebridge.backend.reminder.entity.CareTask;
import com.carebridge.backend.reminder.entity.CareTaskStatus;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.repository.CareTaskRepository;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.ITodayTaskService;
import com.carebridge.backend.reminder.service.ReminderRecurrenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TodayTaskServiceImpl implements ITodayTaskService {

    private final ReminderRepository reminderRepository;
    private final CareTaskRepository careTaskRepository;
    private final ReminderRecurrenceService reminderRecurrenceService;

    @Override
    public List<TodayTaskItem> getTodayTasks(UUID callerId, ZoneId timezone) {
        // ADR-TODAY-002: compute day boundaries in caller's timezone
        LocalDate today = LocalDate.now(timezone);
        Instant startOfDay = today.atStartOfDay(timezone).toInstant();
        Instant endOfDay   = today.plusDays(1).atStartOfDay(timezone).toInstant();

        // ADR-TODAY-001: two separate DB queries, merged in service
        List<Reminder> reminders = reminderRepository
                .findByOwnerUserIdAndStatusNot(callerId, ReminderStatus.CANCELLED);

        List<CareTask> careTasks = careTaskRepository
                .findByAssignedToAndStatusInAndDueAtBetween(
                        callerId, List.of(CareTaskStatus.OPEN, CareTaskStatus.COMPLETED), startOfDay, endOfDay);

        List<TodayTaskItem> items = new ArrayList<>();

        for (Reminder r : reminders) {
            var occurrence = reminderRecurrenceService.occurrenceForDate(r, today, timezone);
            if (occurrence.isEmpty()) {
                continue;
            }
            var generated = occurrence.get();
            String type = r.getReminderType().name();
            items.add(TodayTaskItem.builder()
                    .id(r.getId())
                    .sourceType("REMINDER")
                    .type(type)
                    .title(r.getTitle())
                    .scheduledAt(generated.scheduledAt())
                    .dueAt(generated.dueAt())
                    .snoozedUntil(generated.snoozedUntil())
                    .status(generated.status().name())
                    .priority(reminderPriority(type))
                    .build());
        }

        for (CareTask t : careTasks) {
            items.add(TodayTaskItem.builder()
                    .id(t.getId())
                    .sourceType("CARE_TASK")
                    .type("CARE_TASK")
                    .title(t.getTitle())
                    .scheduledAt(t.getDueAt())
                    .dueAt(t.getDueAt())
                    .status(t.getStatus().name())
                    .priority(4)
                    .build());
        }

        items.sort(Comparator
                .comparingInt(TodayTaskItem::getPriority)
                .thenComparing(TodayTaskItem::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }

    /** Sort priority: VACCINATION=1 > MEDICATION=2 > APPOINTMENT=3 > CARE_TASK=4 */
    private static int reminderPriority(String reminderType) {
        return switch (reminderType) {
            case "VACCINATION"  -> 1;
            case "MEDICATION"   -> 2;
            case "APPOINTMENT"  -> 3;
            default             -> 4;
        };
    }
}

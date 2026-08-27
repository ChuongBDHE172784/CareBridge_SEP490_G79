package com.carebridge.backend.reminder.service.impl;

import com.carebridge.backend.checklist.today.policy.ReminderAccessPolicy;
import com.carebridge.backend.family.repository.CareGroupRepository;
import com.carebridge.backend.reminder.dto.TodayTaskItem;
import com.carebridge.backend.reminder.entity.CareTask;
import com.carebridge.backend.reminder.entity.CareTaskStatus;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.repository.CareTaskRepository;
import com.carebridge.backend.reminder.repository.ReminderRepository;
import com.carebridge.backend.reminder.service.ITodayTaskService;
import com.carebridge.backend.reminder.service.ReminderRecurrenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TodayTaskServiceImpl implements ITodayTaskService {

    private final ReminderRepository reminderRepository;
    private final CareTaskRepository careTaskRepository;
    private final ReminderRecurrenceService reminderRecurrenceService;
    private final CareGroupRepository careGroupRepository;
    private final ReminderAccessPolicy accessPolicy;

    @Autowired
    public TodayTaskServiceImpl(
            ReminderRepository reminderRepository,
            CareTaskRepository careTaskRepository,
            ReminderRecurrenceService reminderRecurrenceService,
            CareGroupRepository careGroupRepository,
            ReminderAccessPolicy accessPolicy) {
        this.reminderRepository = reminderRepository;
        this.careTaskRepository = careTaskRepository;
        this.reminderRecurrenceService = reminderRecurrenceService;
        this.careGroupRepository = careGroupRepository;
        this.accessPolicy = accessPolicy;
    }

    @Override
    public List<TodayTaskItem> getTodayTasks(UUID callerId, ZoneId timezone) {
        // ADR-TODAY-002: compute day boundaries in caller's timezone
        LocalDate today = LocalDate.now(timezone);
        Instant startOfDay = today.atStartOfDay(timezone).toInstant();
        Instant endOfDay   = today.plusDays(1).atStartOfDay(timezone).toInstant();

        Set<UUID> processedReminderIds = new HashSet<>();
        List<Reminder> reminders = new ArrayList<>();

        // Fetch caller's own reminders
        List<Reminder> ownReminders = reminderRepository
                .findByOwnerUserIdAndStatusNot(callerId, ReminderStatus.CANCELLED);
        for (Reminder r : ownReminders) {
            if (r.getId() == null || processedReminderIds.add(r.getId())) {
                reminders.add(r);
            }
        }

        // Family discovery is actor-scoped in SQL, then each reminder is checked against its
        // exact, unique current care context before it can enter the legacy response.
        List<UUID> authorizedOwnerIds = careGroupRepository
                .findActiveOwnerUserIdsForChecklistViewer(callerId);
        if (authorizedOwnerIds != null) {
            for (UUID ownerId : new HashSet<>(authorizedOwnerIds)) {
                if (ownerId == null || ownerId.equals(callerId)) {
                    continue;
                }
                reminderRepository.findByOwnerUserIdAndStatusNot(
                                ownerId, ReminderStatus.CANCELLED).stream()
                        .filter(reminder -> accessPolicy.canView(reminder, callerId))
                        .filter(reminder -> reminder.getId() == null
                                || processedReminderIds.add(reminder.getId()))
                        .forEach(reminders::add);
            }
        }

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

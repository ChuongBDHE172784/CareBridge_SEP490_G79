package com.carebridge.backend.reminder;

import com.carebridge.backend.reminder.entity.CareTask;
import com.carebridge.backend.reminder.entity.CareTaskStatus;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Props Isolation factory for UC49 (View Today Tasks) — SYNTHETIC data only.
 */
public final class TodayTaskTestFactory {

    public static final UUID OWNER_ID      = UUID.fromString("00000000-0000-0000-0002-000000000001");
    public static final UUID CARE_GROUP_ID = UUID.fromString("00000000-0000-0000-0002-000000000002");

    private TodayTaskTestFactory() {}

    public static Reminder pendingReminderToday(ReminderType type) {
        return pendingReminderToday(type, Instant.now().plus(1, ChronoUnit.HOURS));
    }

    public static Reminder pendingReminderToday(ReminderType type, Instant scheduledAt) {
        return Reminder.builder()
                .id(UUID.randomUUID())
                .ownerUserId(OWNER_ID)
                .reminderType(type)
                .title(type.name() + " today")
                .scheduledAt(scheduledAt)
                .status(ReminderStatus.PENDING)
                .build();
    }

    public static Reminder snoozedReminderToday(ReminderType type) {
        return Reminder.builder()
                .id(UUID.randomUUID())
                .ownerUserId(OWNER_ID)
                .reminderType(type)
                .title(type.name() + " snoozed today")
                .scheduledAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .status(ReminderStatus.SNOOZED)
                .snoozedUntil(Instant.now().plus(2, ChronoUnit.HOURS))
                .build();
    }

    public static Reminder snoozedReminderToday(ReminderType type, Instant scheduledAt, Instant snoozedUntil) {
        return Reminder.builder()
                .id(UUID.randomUUID())
                .ownerUserId(OWNER_ID)
                .reminderType(type)
                .title(type.name() + " snoozed today")
                .scheduledAt(scheduledAt)
                .status(ReminderStatus.SNOOZED)
                .snoozedUntil(snoozedUntil)
                .build();
    }

    public static Reminder completedReminderToday(ReminderType type) {
        return Reminder.builder()
                .id(UUID.randomUUID())
                .ownerUserId(OWNER_ID)
                .reminderType(type)
                .title(type.name() + " completed today")
                .scheduledAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .status(ReminderStatus.COMPLETED)
                .build();
    }

    public static CareTask openCareTaskToday() {
        return openCareTaskToday(Instant.now().plus(2, ChronoUnit.HOURS));
    }

    public static CareTask openCareTaskToday(Instant dueAt) {
        return CareTask.builder()
                .id(UUID.randomUUID())
                .careGroupId(CARE_GROUP_ID)
                .assignedTo(OWNER_ID)
                .title("Open care task today")
                .status(CareTaskStatus.OPEN)
                .dueAt(dueAt)
                .build();
    }

    public static CareTask completedCareTaskToday() {
        return CareTask.builder()
                .id(UUID.randomUUID())
                .careGroupId(CARE_GROUP_ID)
                .assignedTo(OWNER_ID)
                .title("Completed care task today")
                .status(CareTaskStatus.COMPLETED)
                .dueAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .build();
    }
}

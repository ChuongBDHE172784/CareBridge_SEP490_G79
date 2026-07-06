package com.carebridge.backend.reminder;

import com.carebridge.backend.reminder.dto.CreateMedicationReminderRequest;
import com.carebridge.backend.reminder.dto.CreateVaccinationReminderRequest;
import com.carebridge.backend.reminder.dto.SnoozeReminderRequest;
import com.carebridge.backend.reminder.dto.UpdateReminderRequest;
import com.carebridge.backend.reminder.entity.Reminder;
import com.carebridge.backend.reminder.entity.ReminderStatus;
import com.carebridge.backend.reminder.entity.ReminderType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Props Isolation factory — all data is SYNTHETIC, never PII.
 * UC46 / UC47 / UC48 shared factory.
 */
public final class ReminderTestFactory {

    // Stable synthetic IDs
    public static final UUID OWNER_ID       = UUID.fromString("00000000-0000-0000-0001-000000000001");
    public static final UUID OTHER_USER_ID  = UUID.fromString("00000000-0000-0000-0001-000000000002");
    public static final UUID REMINDER_ID    = UUID.fromString("00000000-0000-0000-0001-000000000003");
    public static final UUID BABY_ID        = UUID.fromString("00000000-0000-0000-0001-000000000004");
    public static final UUID JOURNEY_ID     = UUID.fromString("00000000-0000-0000-0001-000000000005");

    private ReminderTestFactory() {}

    // ── UC46: Medication reminder requests ─────────────────────────────────────

    public static CreateMedicationReminderRequest validMedicationRequest() {
        CreateMedicationReminderRequest req = new CreateMedicationReminderRequest();
        req.setTitle("Vitamin D supplement");
        req.setScheduledAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        req.setJourneyId(JOURNEY_ID);
        return req;
    }

    public static CreateMedicationReminderRequest medicationRequestTooSoon() {
        CreateMedicationReminderRequest req = new CreateMedicationReminderRequest();
        req.setTitle("Vitamin D supplement");
        req.setScheduledAt(Instant.now().plus(2, ChronoUnit.MINUTES));
        return req;
    }

    public static CreateMedicationReminderRequest medicationRequestInPast() {
        CreateMedicationReminderRequest req = new CreateMedicationReminderRequest();
        req.setTitle("Vitamin D supplement");
        req.setScheduledAt(Instant.now().minus(1, ChronoUnit.HOURS));
        return req;
    }

    // ── UC47: Vaccination reminder requests ────────────────────────────────────

    public static CreateVaccinationReminderRequest validVaccinationRequest() {
        CreateVaccinationReminderRequest req = new CreateVaccinationReminderRequest();
        req.setBabyId(BABY_ID);
        req.setTitle("BCG Dose 1");
        req.setScheduledAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        req.setJourneyId(JOURNEY_ID);
        return req;
    }

    public static CreateVaccinationReminderRequest vaccinationRequestNoBabyId() {
        CreateVaccinationReminderRequest req = new CreateVaccinationReminderRequest();
        req.setBabyId(null);
        req.setTitle("BCG Dose 1");
        req.setScheduledAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        return req;
    }

    public static CreateVaccinationReminderRequest vaccinationRequestTooSoon() {
        CreateVaccinationReminderRequest req = new CreateVaccinationReminderRequest();
        req.setBabyId(BABY_ID);
        req.setTitle("BCG Dose 1");
        req.setScheduledAt(Instant.now().plus(2, ChronoUnit.MINUTES));
        return req;
    }

    // ── UC48: Update / Snooze requests ─────────────────────────────────────────

    public static UpdateReminderRequest validUpdateRequest() {
        UpdateReminderRequest req = new UpdateReminderRequest();
        req.setTitle("Updated reminder title");
        req.setScheduledAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        return req;
    }

    public static UpdateReminderRequest updateRequestTooSoon() {
        UpdateReminderRequest req = new UpdateReminderRequest();
        req.setScheduledAt(Instant.now().plus(2, ChronoUnit.MINUTES));
        return req;
    }

    public static SnoozeReminderRequest validSnoozeRequest() {
        SnoozeReminderRequest req = new SnoozeReminderRequest();
        req.setSnoozedUntil(Instant.now().plus(30, ChronoUnit.MINUTES));
        return req;
    }

    public static SnoozeReminderRequest snoozeTooFarRequest() {
        SnoozeReminderRequest req = new SnoozeReminderRequest();
        req.setSnoozedUntil(Instant.now().plus(25, ChronoUnit.HOURS));
        return req;
    }

    public static SnoozeReminderRequest snoozeInPastRequest() {
        SnoozeReminderRequest req = new SnoozeReminderRequest();
        req.setSnoozedUntil(Instant.now().minus(1, ChronoUnit.HOURS));
        return req;
    }

    // ── Reminder entity builders ─────────────────────────────────────────────

    public static Reminder pendingReminder(ReminderType type) {
        return Reminder.builder()
                .id(REMINDER_ID)
                .ownerUserId(OWNER_ID)
                .reminderType(type)
                .title("Test reminder")
                .scheduledAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .status(ReminderStatus.PENDING)
                .build();
    }

    public static Reminder snoozedReminder() {
        return Reminder.builder()
                .id(REMINDER_ID)
                .ownerUserId(OWNER_ID)
                .reminderType(ReminderType.MEDICATION)
                .title("Snoozed reminder")
                .scheduledAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .status(ReminderStatus.SNOOZED)
                .snoozedUntil(Instant.now().plus(30, ChronoUnit.MINUTES))
                .build();
    }

    public static Reminder completedReminder() {
        return Reminder.builder()
                .id(REMINDER_ID)
                .ownerUserId(OWNER_ID)
                .reminderType(ReminderType.MEDICATION)
                .title("Completed reminder")
                .scheduledAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .status(ReminderStatus.COMPLETED)
                .build();
    }

    public static Reminder skippedReminder() {
        return Reminder.builder()
                .id(REMINDER_ID)
                .ownerUserId(OWNER_ID)
                .reminderType(ReminderType.MEDICATION)
                .title("Skipped reminder")
                .scheduledAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .status(ReminderStatus.SKIPPED)
                .build();
    }

    public static Reminder reminderOwnedByOther(ReminderType type) {
        return Reminder.builder()
                .id(REMINDER_ID)
                .ownerUserId(OTHER_USER_ID)
                .reminderType(type)
                .title("Other user's reminder")
                .scheduledAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .status(ReminderStatus.PENDING)
                .build();
    }
}

package com.carebridge.backend.notification.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One vaccination reminder to deliver to one mother.
 *
 * <p>{@code (vaccinationRecordId, daysBefore)} is the idempotency key: each lead milestone of
 * each dose is delivered at most once, however many times the dispatch job runs.
 */
public record VaccinationReminderCommand(
        UUID vaccinationRecordId,
        UUID babyId,
        UUID userId,
        String babyNickname,
        String vaccineName,
        Short doseNumber,
        LocalDate scheduledDate,
        int daysBefore) {
}

package com.carebridge.backend.checklist.distribution;

import java.time.LocalDate;

public record ChecklistLifecycleDates(
        LocalDate lastMenstrualDate,
        LocalDate estimatedDueDate,
        LocalDate deliveryDate,
        LocalDate birthDate) {
}

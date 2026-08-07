package com.carebridge.backend.vaccination.service;

import java.time.LocalDate;

/**
 * Scans the materialised vaccination book and pushes reminders to the mother as a dose's
 * scheduled date approaches (MF-03 step 2).
 */
public interface IVaccinationReminderService {

    /** Dispatches every reminder milestone that falls due today, in the configured zone. */
    int dispatchDueReminders();

    /** Same as {@link #dispatchDueReminders()} but for an explicit "today" — used by tests. */
    int dispatchDueReminders(LocalDate today);
}

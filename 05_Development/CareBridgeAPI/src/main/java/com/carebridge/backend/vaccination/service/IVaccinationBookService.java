package com.carebridge.backend.vaccination.service;

import com.carebridge.backend.baby.entity.BabyProfile;

/**
 * Materialises the expected vaccination book ("Sổ tiêm chủng dự kiến") for a baby by
 * scanning the active {@code vaccination_schedules} catalogue and projecting every dose
 * onto the baby's birth date.
 */
public interface IVaccinationBookService {

    /**
     * Creates one SCHEDULED {@code vaccination_records} row per catalogue dose that the baby
     * does not already have, with {@code scheduled_date = birthDate + offsetDays}.
     *
     * <p>Idempotent: doses already present in any status — including COMPLETED, POSTPONED and
     * DELETED — are left untouched, so re-running never resurrects a dose the mother removed
     * nor duplicates one she already recorded. A baby with no birth date yields no rows,
     * because no expected date can be derived.
     *
     * @return number of records created
     */
    int initializeBook(BabyProfile baby);

    /**
     * Re-projects the still-SCHEDULED part of the book onto a corrected birth date, and
     * materialises any dose that is still missing. Doses the mother has already completed,
     * postponed or deleted keep the dates she chose.
     *
     * @return number of records whose scheduled date changed, plus records newly created
     */
    int realignBook(BabyProfile baby);
}

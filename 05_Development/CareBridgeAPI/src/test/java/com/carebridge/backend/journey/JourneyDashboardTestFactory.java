package com.carebridge.backend.journey;

import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.GestationalDatingBasis;
import com.carebridge.backend.journey.entity.MotherJourney;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/** Props-isolation factory for UC24 ViewMotherJourneyDashboard unit tests. */
public final class JourneyDashboardTestFactory {

    /** Fixed test clock date (must match fixedClock in test setUp): 2026-06-26 UTC */
    public static final LocalDate TODAY    = LocalDate.of(2026, 6, 26);
    public static final UUID MOTHER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000024");
    private static final UUID JOURNEY_ID   = UUID.fromString("dddddddd-0000-0000-0000-000000000024");

    private JourneyDashboardTestFactory() {}

    /**
     * Creates a PREGNANCY journey where LMP corresponds to the given 1-based week.
     * Week 1 means LMP is TODAY.
     * Ensures getDashboard returns pregnancyWeek == weeksPregnant.
     */
    public static MotherJourney makePregnancyJourney(int weeksPregnant) {
        LocalDate lmp = TODAY.minusDays((long) (weeksPregnant - 1) * 7);
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .startDate(lmp)
                .lastMenstrualDate(lmp)
                .estimatedDueDate(lmp.plusDays(280)) // standard 40 weeks (280 days)
                .gestationalDatingBasis(GestationalDatingBasis.LMP)
                .gestationalDatingRevision(1L)
                .gestationalDatingEffectiveAt(
                        TODAY.atStartOfDay().toInstant(ZoneOffset.UTC))
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    /** Creates a PREGNANCY journey with exact daysAgo days since LMP (for fractional-week tests). */
    public static MotherJourney makePregnancyJourneyByDays(int daysAgo) {
        LocalDate lmp = TODAY.minusDays(daysAgo);
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .startDate(lmp)
                .lastMenstrualDate(lmp)
                .estimatedDueDate(lmp.plusDays(280))
                .gestationalDatingBasis(GestationalDatingBasis.LMP)
                .gestationalDatingRevision(1L)
                .gestationalDatingEffectiveAt(
                        TODAY.atStartOfDay().toInstant(ZoneOffset.UTC))
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    public static MotherJourney makePostpartumJourney() {
        LocalDate deliveryDate = TODAY.minusWeeks(4);
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.POSTPARTUM)
                .startDate(deliveryDate)
                .deliveryDate(deliveryDate)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    public static MotherJourney makePrePregnancyJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PRE_PREGNANCY)
                .startDate(TODAY.minusMonths(1))
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    public static MotherJourney makeBabyCareJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.BABY_CARE)
                .startDate(TODAY.minusWeeks(8))
                .status(JourneyStatus.ACTIVE)
                .build();
    }
}

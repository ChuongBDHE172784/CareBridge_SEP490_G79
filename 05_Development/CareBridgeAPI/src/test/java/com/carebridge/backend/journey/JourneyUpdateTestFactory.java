package com.carebridge.backend.journey;

import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;

import java.time.LocalDate;
import java.util.UUID;

/** Props-isolation factory for UC23 UpdateJourney unit tests. */
public final class JourneyUpdateTestFactory {

    public static final UUID MOTHER_ID         = UUID.fromString("00000000-0000-0000-0000-000000000023");
    public static final UUID OTHER_USER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000099");
    public static final UUID JOURNEY_ID        = UUID.fromString("cccccccc-0000-0000-0000-000000000023");
    public static final UUID UNKNOWN_JOURNEY_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private JourneyUpdateTestFactory() {}

    public static MotherJourney makeActiveJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .startDate(LocalDate.of(2026, 1, 1))
                .lastMenstrualDate(LocalDate.of(2025, 12, 1))
                .estimatedDueDate(LocalDate.of(2026, 9, 7))
                .status(JourneyStatus.ACTIVE)
                .notes("Initial notes")
                .build();
    }

    public static MotherJourney makeCompletedJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .startDate(LocalDate.of(2026, 1, 1))
                .status(JourneyStatus.COMPLETED)
                .deliveryDate(LocalDate.of(2026, 9, 1))
                .build();
    }

    public static MotherJourney makePrePregnancyJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PRE_PREGNANCY)
                .startDate(LocalDate.of(2026, 1, 1))
                .status(JourneyStatus.ACTIVE)
                .notes("Preparing")
                .build();
    }

    public static MotherJourney makeArchivedJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .startDate(LocalDate.of(2025, 1, 1))
                .status(JourneyStatus.ARCHIVED)
                .build();
    }

    /** Update notes + estimatedDueDate; status stays ACTIVE. */
    public static UpdateJourneyRequest makeUpdateRequest() {
        UpdateJourneyRequest req = new UpdateJourneyRequest();
        req.setNotes("Updated notes");
        req.setEstimatedDueDate(LocalDate.of(2026, 9, 14));
        return req;
    }

    /** Transition to COMPLETED with required deliveryDate. */
    public static UpdateJourneyRequest makeCompleteRequest() {
        UpdateJourneyRequest req = new UpdateJourneyRequest();
        req.setStatus("COMPLETED");
        req.setDeliveryDate(LocalDate.of(2026, 9, 1));
        return req;
    }

    /** Transition to COMPLETED WITHOUT deliveryDate — must be rejected (JOURNEY-013). */
    public static UpdateJourneyRequest makeCompleteRequestWithoutDeliveryDate() {
        UpdateJourneyRequest req = new UpdateJourneyRequest();
        req.setStatus("COMPLETED");
        return req;
    }

    /** Attempt to manually set status to ARCHIVED — must be rejected (JOURNEY-014). */
    public static UpdateJourneyRequest makeArchiveRequest() {
        UpdateJourneyRequest req = new UpdateJourneyRequest();
        req.setStatus("ARCHIVED");
        return req;
    }
}

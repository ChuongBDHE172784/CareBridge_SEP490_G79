package com.carebridge.backend.journey;

import com.carebridge.backend.journey.dto.CreateJourneyRequest;
import com.carebridge.backend.journey.dto.UpdateJourneyRequest;
import com.carebridge.backend.journey.entity.*;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

final class JourneyLifecycleTestFactory {

    static final UUID MOTHER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID OTHER_MOTHER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000102");
    static final UUID EXPERT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000201");
    static final UUID JOURNEY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000001001");
    static final Instant NOW = Instant.parse("2026-07-18T03:00:00Z");

    private JourneyLifecycleTestFactory() {
    }

    static User mother() {
        return User.builder().id(MOTHER_ID).role(Role.MOTHER).build();
    }

    static User expert() {
        return User.builder().id(EXPERT_ID).role(Role.EXPERT).build();
    }

    static CreateJourneyRequest pregnancyCreate() {
        CreateJourneyRequest request = new CreateJourneyRequest();
        request.setJourneyType(JourneyType.PREGNANCY);
        request.setStartDate(LocalDate.of(2026, 7, 18));
        request.setLastMenstrualDate(LocalDate.of(2026, 6, 1));
        request.setDateSource(JourneyDateSource.SELF_REPORTED);
        request.setDateConfidence(JourneyDateConfidence.ESTIMATED);
        request.setChangeReason("INITIAL_SETUP");
        request.setEffectiveAt(NOW);
        return request;
    }

    static UpdateJourneyRequest dateCorrection() {
        UpdateJourneyRequest request = new UpdateJourneyRequest();
        request.setLastMenstrualDate(LocalDate.of(2026, 6, 2));
        request.setDateSource(JourneyDateSource.SELF_REPORTED);
        request.setDateConfidence(JourneyDateConfidence.ESTIMATED);
        request.setChangeReason("DATE_CORRECTION");
        request.setEffectiveAt(NOW);
        return request;
    }

    static UpdateJourneyRequest dateCorrectionWithoutProvenance() {
        UpdateJourneyRequest request = new UpdateJourneyRequest();
        request.setLastMenstrualDate(LocalDate.of(2026, 6, 2));
        return request;
    }

    static UpdateJourneyRequest invalidPostpartumTransition() {
        UpdateJourneyRequest request = new UpdateJourneyRequest();
        request.setJourneyType(JourneyType.POSTPARTUM);
        request.setDateSource(JourneyDateSource.SELF_REPORTED);
        request.setDateConfidence(JourneyDateConfidence.CONFIRMED);
        request.setChangeReason("UNAPPROVED_OUTCOME_TRANSITION");
        request.setEffectiveAt(NOW);
        return request;
    }

    static MotherJourney activePrePregnancy() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PRE_PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1))
                .version(0L)
                .build();
    }

    static MotherJourney activePregnancy() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1))
                .lastMenstrualDate(LocalDate.of(2026, 6, 1))
                .estimatedDueDate(LocalDate.of(2027, 3, 8))
                .dateSource(JourneyDateSource.SELF_REPORTED)
                .dateConfidence(JourneyDateConfidence.ESTIMATED)
                .version(0L)
                .build();
    }

    static MotherJourney completedJourney() {
        MotherJourney journey = activePregnancy();
        journey.setStatus(JourneyStatus.COMPLETED);
        return journey;
    }

    static MotherJourney babyCareJourney() {
        MotherJourney journey = activePregnancy();
        journey.setJourneyType(JourneyType.BABY_CARE);
        return journey;
    }
}

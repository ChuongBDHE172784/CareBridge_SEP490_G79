package com.carebridge.backend.health;

import com.carebridge.backend.health.dto.AddPostpartumLogRequest;
import com.carebridge.backend.health.dto.UpdatePostpartumLogRequest;
import com.carebridge.backend.health.entity.BleedingLevel;
import com.carebridge.backend.health.entity.PostpartumLog;
import com.carebridge.backend.health.entity.PostpartumLogStatus;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Props-isolation factory for UC28 AddPostpartumLog unit tests. */
public final class PostpartumLogTestFactory {

    public static final UUID MOTHER_ID      = UUID.fromString("00000000-0000-0000-0000-000000000028");
    public static final UUID JOURNEY_ID     = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000028");
    public static final UUID LOG_ID         = UUID.fromString("cccccccc-0000-0000-0000-000000000028");
    public static final UUID SUBMISSION_ID  = UUID.fromString("dddddddd-0000-0000-0000-000000000028");
    public static final UUID OTHER_USER     = UUID.fromString("99999999-0000-0000-0000-000000000028");
    public static final UUID UNKNOWN_JOURNEY = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private PostpartumLogTestFactory() {}

    public static MotherJourney makeActivePostpartumJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.POSTPARTUM)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    public static MotherJourney makePregnancyJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    public static MotherJourney makeCompletedJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(MOTHER_ID)
                .journeyType(JourneyType.POSTPARTUM)
                .status(JourneyStatus.COMPLETED)
                .build();
    }

    public static MotherJourney makeOtherUsersJourney() {
        return MotherJourney.builder()
                .id(JOURNEY_ID)
                .ownerUserId(OTHER_USER)
                .journeyType(JourneyType.POSTPARTUM)
                .status(JourneyStatus.ACTIVE)
                .build();
    }

    public static AddPostpartumLogRequest makeValidRequest() {
        AddPostpartumLogRequest req = new AddPostpartumLogRequest();
        req.setSubmissionId(SUBMISSION_ID);
        req.setLogDate(LocalDate.now());
        req.setPainLevel((short) 3);
        req.setBleedingLevel(BleedingLevel.LIGHT);
        req.setMoodLevel((short) 7);
        req.setSleepHours(new BigDecimal("6.5"));
        req.setBreastfeedingNote("Fed 4 times, baby latched well");
        req.setSymptomNote("Mild cramps, improving");
        return req;
    }

    public static PostpartumLog makeActiveLog() {
        return PostpartumLog.builder()
                .id(LOG_ID)
                .journeyId(JOURNEY_ID)
                .submissionId(SUBMISSION_ID)
                .logDate(LocalDate.now())
                .painLevel((short) 3)
                .bleedingLevel(BleedingLevel.LIGHT)
                .moodLevel((short) 7)
                .sleepHours(new BigDecimal("6.5"))
                .breastfeedingNote("Fed 4 times, baby latched well")
                .symptomNote("Mild cramps, improving")
                .status(PostpartumLogStatus.ACTIVE)
                .build();
    }

    public static UpdatePostpartumLogRequest makeUpdateRequest() {
        UpdatePostpartumLogRequest req = new UpdatePostpartumLogRequest();
        req.setLogDate(LocalDate.now().minusDays(1));
        req.setPainLevel((short) 4);
        req.setSymptomNote("Pain improved");
        return req;
    }
}

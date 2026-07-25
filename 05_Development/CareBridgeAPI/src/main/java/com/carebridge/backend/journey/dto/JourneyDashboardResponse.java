package com.carebridge.backend.journey.dto;

import com.carebridge.backend.journey.entity.DashboardStatus;
import com.carebridge.backend.journey.entity.JourneyDateConfidence;
import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class JourneyDashboardResponse {

    /** null if no active journey */
    private UUID journeyId;

    /** PREGNANCY, POSTPARTUM, BABY_CARE, PRE_PREGNANCY — null if no active journey */
    private String journeyType;

    /** Dashboard status: ACTIVE_PREGNANCY, ACTIVE_POSTPARTUM, BABY_CARE, PRE_PREGNANCY, NO_JOURNEY */
    private DashboardStatus status;

    /** Pregnancy week (floor of days / 7). null if journey type is not PREGNANCY or LMP is null */
    private Integer pregnancyWeek;

    /** 1, 2, or 3. null if not a PREGNANCY journey */
    private Integer trimester;

    /** Days from today to estimatedDueDate. Negative if past due. null if no estimatedDueDate */
    private Long daysUntilDue;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate estimatedDueDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastMenstrualDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    private Long version;

    private JourneyDateSource dateSource;

    private JourneyDateConfidence dateConfidence;

    private PregnancyOutcomeType pregnancyOutcome;

    /** True only when a canonical postpartum LIVE_BIRTH has append-only evidence. */
    private boolean babyActionsEligible;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate pregnancyOutcomeDate;
}

package com.carebridge.backend.journey.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import com.carebridge.backend.journey.entity.JourneyDateConfidence;
import com.carebridge.backend.journey.entity.JourneyDateSource;
import com.carebridge.backend.journey.entity.PregnancyOutcomeType;
import com.carebridge.backend.journey.entity.GestationalDatingBasis;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class JourneyResponse {

    private UUID journeyId;
    private UUID ownerUserId;
    private String journeyType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastMenstrualDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate estimatedDueDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deliveryDate;

    private PregnancyOutcomeType pregnancyOutcome;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate pregnancyOutcomeDate;

    private String status;
    private String notes;
    private long version;
    private JourneyDateSource dateSource;
    private JourneyDateConfidence dateConfidence;
    private GestationalDatingBasis gestationalDatingBasis;
    private Long gestationalDatingRevision;
    private Instant gestationalDatingEffectiveAt;
    private String gestationalDatingQuarantineReasonCode;
    private LocalDate canonicalLmp;
    private Integer completedGestationalWeek;
    private Integer sourceWeekNumber;
    private Integer plan;
    private Instant createdAt;
    private Instant updatedAt;
}

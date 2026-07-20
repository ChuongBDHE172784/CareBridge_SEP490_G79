package com.carebridge.backend.baby.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class LinkBabyJourneyRequest {
    @NotNull private UUID relatedJourneyId;
    @NotNull private UUID submissionId;
}

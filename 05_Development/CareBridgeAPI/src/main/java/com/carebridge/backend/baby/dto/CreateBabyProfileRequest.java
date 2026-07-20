package com.carebridge.backend.baby.dto;

import com.carebridge.backend.baby.entity.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateBabyProfileRequest {

    @NotBlank
    @Size(max = 100)
    private String nickname;

    @NotNull
    @PastOrPresent
    private LocalDate birthDate;

    private Gender gender;

    @DecimalMin("0.5")
    @DecimalMax("10.0")
    private BigDecimal birthWeightKg;

    @DecimalMin("20.0")
    @DecimalMax("100.0")
    private BigDecimal birthLengthCm;

    private UUID relatedJourneyId;

    private UUID submissionId;

    @AssertTrue(message = "submissionId is required when relatedJourneyId is provided")
    public boolean isLinkSubmissionValid() {
        return relatedJourneyId == null || submissionId != null;
    }
}

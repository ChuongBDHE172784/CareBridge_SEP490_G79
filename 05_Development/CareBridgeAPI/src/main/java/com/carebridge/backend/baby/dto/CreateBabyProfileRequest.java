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
    private LocalDate birthDate;

    private Gender gender;

    @DecimalMin("0.5") @DecimalMax("8.0")
    private BigDecimal birthWeightKg;

    @DecimalMin("25.0") @DecimalMax("65.0")
    private BigDecimal birthLengthCm;

    private UUID relatedJourneyId;
}

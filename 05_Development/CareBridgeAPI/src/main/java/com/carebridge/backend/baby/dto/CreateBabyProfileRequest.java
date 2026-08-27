package com.carebridge.backend.baby.dto;

import com.carebridge.backend.baby.entity.Gender;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object ignoredValue) {
        throw new IllegalArgumentException("Unsupported baby profile field: " + fieldName);
    }
}

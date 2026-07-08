package com.carebridge.backend.baby.dto;

import com.carebridge.backend.baby.entity.Gender;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateBabyProfileRequest {

    @Size(max = 100)
    private String nickname;

    @PastOrPresent
    private LocalDate birthDate;

    private Gender gender;

    @DecimalMin("0.5")
    @DecimalMax("10.0")
    private BigDecimal birthWeightKg;

    @DecimalMin("20.0")
    @DecimalMax("100.0")
    private BigDecimal birthLengthCm;
}

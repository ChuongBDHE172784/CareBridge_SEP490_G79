package com.carebridge.backend.baby;

import com.carebridge.backend.baby.dto.CreateBabyProfileRequest;
import com.carebridge.backend.baby.entity.Gender;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CreateBabyProfileRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void createBabyProfile_allowsSameMeasurementRangeAsMobileForm() {
        CreateBabyProfileRequest request = new CreateBabyProfileRequest();
        request.setNickname("Baby Bean");
        request.setBirthDate(LocalDate.now());
        request.setGender(Gender.UNKNOWN);
        request.setBirthWeightKg(new BigDecimal("9.5"));
        request.setBirthLengthCm(new BigDecimal("70.0"));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void createBabyProfile_rejectsFutureBirthDate() {
        CreateBabyProfileRequest request = new CreateBabyProfileRequest();
        request.setNickname("Baby Bean");
        request.setBirthDate(LocalDate.now().plusDays(1));

        assertThat(validator.validate(request))
                .anyMatch(v -> "birthDate".equals(v.getPropertyPath().toString()));
    }
}

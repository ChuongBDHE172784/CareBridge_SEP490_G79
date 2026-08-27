package com.carebridge.backend.baby;

import com.carebridge.backend.baby.dto.CreateBabyProfileRequest;
import com.carebridge.backend.baby.entity.Gender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void createBabyProfile_rejectsLegacyLinkagePropertiesAtJsonBoundary() {
        ObjectMapper mapper = new ObjectMapper();

        String legacyField = "related" + "JourneyId";
        assertThatThrownBy(() -> mapper.readValue(
                "{\"nickname\":\"Baby Bean\","
                        + "\"" + legacyField + "\":\"00000000-0000-0000-0000-000000000001\"}",
                CreateBabyProfileRequest.class))
                .isInstanceOf(JsonMappingException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(legacyField);
    }
}

package com.carebridge.backend.expert.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.mapper.ExpertProfileMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExpertProfileRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void createAndUpdateAllowFeeAndExperienceBoundaryValues() {
        CreateExpertProfileRequest create = validCreate();
        create.setConsultationFeeVnd(0L);
        create.setExperienceYears(80);
        UpdateExpertProfileRequest update = UpdateExpertProfileRequest.builder()
                .consultationFeeVnd(0L)
                .experienceYears(0)
                .build();

        assertThat(validator.validate(create)).isEmpty();
        assertThat(validator.validate(update)).isEmpty();
    }

    @Test
    void createAndUpdateAcceptCanonicalFacilityUuidAndRejectLongerIdentifiers() {
        String facilityId = java.util.UUID.randomUUID().toString();
        CreateExpertProfileRequest create = validCreate();
        create.setHospitalId(facilityId);
        UpdateExpertProfileRequest update = UpdateExpertProfileRequest.builder()
                .hospitalId(facilityId)
                .build();

        assertThat(validator.validate(create)).isEmpty();
        assertThat(validator.validate(update)).isEmpty();

        create.setHospitalId("x".repeat(37));
        update.setHospitalId("x".repeat(37));
        assertViolationOn(validator.validate(create), "hospitalId");
        assertViolationOn(validator.validate(update), "hospitalId");
    }

    @Test
    void createAndUpdateRejectNegativeConsultationFee() {
        CreateExpertProfileRequest create = validCreate();
        create.setConsultationFeeVnd(-1L);
        UpdateExpertProfileRequest update = UpdateExpertProfileRequest.builder()
                .consultationFeeVnd(-1L)
                .build();

        assertViolationOn(validator.validate(create), "consultationFeeVnd");
        assertViolationOn(validator.validate(update), "consultationFeeVnd");
    }

    @Test
    void createAndUpdateRejectExperienceOutsideZeroToEightyYears() {
        CreateExpertProfileRequest create = validCreate();
        create.setExperienceYears(81);
        UpdateExpertProfileRequest update = UpdateExpertProfileRequest.builder()
                .experienceYears(-1)
                .build();

        assertViolationOn(validator.validate(create), "experienceYears");
        assertViolationOn(validator.validate(update), "experienceYears");
    }

    @Test
    void mapperPreservesConsultationFeeInCanonicalProfileAndResponses() {
        CreateExpertProfileRequest request = validCreate();
        request.setConsultationFeeVnd(350_000L);
        ExpertProfileMapper mapper = new ExpertProfileMapper();

        ExpertProfile profile = mapper.toEntity(request, java.util.UUID.randomUUID());

        assertThat(profile.getConsultationFeeVnd()).isEqualTo(350_000L);
        assertThat(mapper.toResponse(profile, "Expert", null).getConsultationFeeVnd())
                .isEqualByComparingTo(BigDecimal.valueOf(350_000L));
        assertThat(mapper.toDetailResponse(profile, "Expert", null).getConsultationFeeVnd())
                .isEqualByComparingTo(BigDecimal.valueOf(350_000L));
    }

    private static CreateExpertProfileRequest validCreate() {
        return CreateExpertProfileRequest.builder()
                .specialtyId("OBGYN")
                .hospitalId("HCM-001")
                .build();
    }

    private static void assertViolationOn(
            Set<? extends ConstraintViolation<?>> violations, String property) {
        assertThat(violations)
                .anyMatch(violation -> property.equals(violation.getPropertyPath().toString()));
    }
}

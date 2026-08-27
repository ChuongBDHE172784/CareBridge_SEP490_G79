package com.carebridge.backend.exercise;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.exercise.dto.CreatePostureConfigRequest;
import com.carebridge.backend.exercise.entity.AnalysisMode;
import com.carebridge.backend.exercise.entity.PostureFeedbackLevel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreatePostureConfigRequestValidationTest {

    private static final Validator VALIDATOR;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    private CreatePostureConfigRequest request(BigDecimal threshold) {
        CreatePostureConfigRequest request = new CreatePostureConfigRequest();
        request.setExerciseId(UUID.randomUUID());
        request.setAnalysisMode(AnalysisMode.MODEL_BASED);
        request.setRuleOrModelVersion("posenet-v2.1.0");
        request.setConfidenceThreshold(threshold);
        request.setFeedbackLevel(PostureFeedbackLevel.DETAILED);
        return request;
    }

    // PAC-TC-THRESH-001
    @Test
    @DisplayName("PAC-TC-THRESH-001: confidenceThreshold=0.0 lower boundary is valid")
    void confidenceThreshold_zero_isValid() {
        Set<ConstraintViolation<CreatePostureConfigRequest>> violations =
                VALIDATOR.validate(request(new BigDecimal("0.0")));
        assertThat(violations).isEmpty();
    }

    // PAC-TC-THRESH-002
    @Test
    @DisplayName("PAC-TC-THRESH-002: confidenceThreshold=1.0 upper boundary is valid")
    void confidenceThreshold_one_isValid() {
        Set<ConstraintViolation<CreatePostureConfigRequest>> violations =
                VALIDATOR.validate(request(new BigDecimal("1.0")));
        assertThat(violations).isEmpty();
    }

    // PAC-TC-THRESH-003
    @Test
    @DisplayName("PAC-TC-THRESH-003: confidenceThreshold=-0.01 just below lower boundary is invalid")
    void confidenceThreshold_belowZero_isInvalid() {
        Set<ConstraintViolation<CreatePostureConfigRequest>> violations =
                VALIDATOR.validate(request(new BigDecimal("-0.01")));
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString())
                .isEqualTo("confidenceThreshold");
    }

    // PAC-TC-THRESH-004
    @Test
    @DisplayName("PAC-TC-THRESH-004: confidenceThreshold=1.01 just above upper boundary is invalid")
    void confidenceThreshold_aboveOne_isInvalid() {
        Set<ConstraintViolation<CreatePostureConfigRequest>> violations =
                VALIDATOR.validate(request(new BigDecimal("1.01")));
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString())
                .isEqualTo("confidenceThreshold");
    }

    // PAC-TC-THRESH-005
    @Test
    @DisplayName("PAC-TC-THRESH-005: confidenceThreshold=null is invalid (required field)")
    void confidenceThreshold_null_isInvalid() {
        Set<ConstraintViolation<CreatePostureConfigRequest>> violations =
                VALIDATOR.validate(request(null));
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString())
                .isEqualTo("confidenceThreshold");
    }
}

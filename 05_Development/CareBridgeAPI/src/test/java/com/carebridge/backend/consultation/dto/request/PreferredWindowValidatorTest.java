package com.carebridge.backend.consultation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PreferredWindowValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsBothAbsent() {
        assertThat(validator.validate(validRequest(null, null))).isEmpty();
    }

    @Test
    void acceptsIncreasingWindow() {
        Instant start = Instant.parse("2026-07-17T01:00:00Z");
        assertThat(validator.validate(validRequest(start, start.plusSeconds(3600)))).isEmpty();
    }

    @Test
    void rejectsOneSidedAndNonIncreasingWindows() {
        Instant start = Instant.parse("2026-07-17T01:00:00Z");
        assertThat(validator.validate(validRequest(start, null))).isNotEmpty();
        assertThat(validator.validate(validRequest(null, start))).isNotEmpty();
        assertThat(validator.validate(validRequest(start, start))).isNotEmpty();
        assertThat(validator.validate(validRequest(start, start.minusSeconds(1)))).isNotEmpty();
    }

    private static CreateConsultationRequestRequest validRequest(Instant start, Instant end) {
        return CreateConsultationRequestRequest.builder()
                .clientRequestId(UUID.randomUUID())
                .expertProfileId(UUID.randomUUID())
                .topic("Nutrition consultation")
                .description("Please advise on a feeding schedule.")
                .preferredWindowStart(start)
                .preferredWindowEnd(end)
                .build();
    }
}

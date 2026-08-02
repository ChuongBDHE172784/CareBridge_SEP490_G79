package com.carebridge.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.recommendation.exception.RecommendationException;
import com.carebridge.backend.recommendation.service.RecommendationProfileValidator;
import com.carebridge.backend.recommendation.service.ValidatedRecommendationProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecommendationProfileValidatorTest {

    private RecommendationProfileValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new RecommendationProfileValidator(
                objectMapper,
                Clock.fixed(Instant.parse("2026-08-02T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void usesRawBmiBeforeRoundingAtThe18Point5Boundary() {
        ObjectNode profile = baseProfile();
        profile.set("bmi", objectMapper.createObjectNode()
                .put("state", "KNOWN")
                .put("heightCm", 200.0)
                .put("weightKg", 74.0)
                .put("weightContext", "PRE_PREGNANCY")
                .put("measuredOn", "2026-08-01"));

        ValidatedRecommendationProfile validated = validator.validateAccept(
                accepted(profile), JourneyType.PRE_PREGNANCY, null);

        assertThat(validated.derived()).containsEntry("bmiCategory", "HEALTHY_RANGE");
        assertThat(validated.signalSlugs()).contains("rec-bmi-healthy-range");
    }

    @Test
    void rejectsStiInfectionCodesForNonHistoryStatus() {
        ObjectNode profile = baseProfile();
        profile.set("sti", objectMapper.createObjectNode()
                .put("state", "KNOWN")
                .put("status", "NO_KNOWN_HISTORY")
                .set("infectionCodes", objectMapper.createArrayNode().add("HIV")));

        assertThatThrownBy(() -> validator.validateAccept(
                accepted(profile), JourneyType.PREGNANCY, null))
                .isInstanceOf(RecommendationException.class)
                .extracting("code")
                .isEqualTo("RECOMMENDATION_PROFILE_INVALID");
    }

    @Test
    void rejectedSensitiveValueIsNotReflectedInValidationError() {
        ObjectNode profile = baseProfile();
        profile.set("bmi", objectMapper.createObjectNode()
                .put("state", "KNOWN")
                .put("heightCm", "BMI_RAW_CANARY")
                .put("weightKg", 70.0)
                .put("weightContext", "PRE_PREGNANCY")
                .put("measuredOn", "2026-08-01"));

        assertThatThrownBy(() -> validator.validateAccept(
                accepted(profile), JourneyType.PRE_PREGNANCY, null))
                .isInstanceOf(RecommendationException.class)
                .hasMessageNotContaining("BMI_RAW_CANARY");
    }

    private ObjectNode accepted(ObjectNode profile) {
        return objectMapper.createObjectNode()
                .put("submissionId", UUID.randomUUID().toString())
                .put("schemaVersion", RecommendationConstants.SCHEMA_VERSION)
                .put("policyVersion", RecommendationConstants.POLICY_VERSION)
                .put("consentAccepted", true)
                .set("profile", profile);
    }

    private ObjectNode baseProfile() {
        ObjectNode profile = objectMapper.createObjectNode();
        profile.set("age", state("UNKNOWN"));
        profile.set("bmi", state("UNKNOWN"));
        profile.set("reproductiveHistory", state("UNKNOWN"));
        profile.set("underlyingConditions", state("UNKNOWN"));
        ObjectNode lifestyle = objectMapper.createObjectNode();
        lifestyle.set("smoking", state("UNKNOWN"));
        lifestyle.set("alcohol", state("UNKNOWN"));
        lifestyle.set("physicalActivity", state("UNKNOWN"));
        lifestyle.set("sleep", state("UNKNOWN"));
        profile.set("lifestyle", lifestyle);
        profile.set("nutrition", state("UNKNOWN"));
        ObjectNode vaccination = objectMapper.createObjectNode();
        var answers = objectMapper.createArrayNode();
        for (String code : RecommendationConstants.VACCINE_CODES.stream().sorted().toList()) {
            answers.add(state("UNKNOWN").put("code", code));
        }
        vaccination.set("answers", answers);
        profile.set("vaccination", vaccination);
        profile.set("currentMedications", state("UNKNOWN"));
        profile.set("sexualHealth", state("UNKNOWN"));
        profile.set("sti", state("UNKNOWN"));
        return profile;
    }

    private ObjectNode state(String value) {
        return objectMapper.createObjectNode().put("state", value);
    }
}

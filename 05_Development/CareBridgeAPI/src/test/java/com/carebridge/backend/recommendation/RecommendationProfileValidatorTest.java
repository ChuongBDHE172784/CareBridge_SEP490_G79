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
    void mapsNineGroupQuestionnaireAnswersToControlledSignals() {
        ObjectNode profile = baseProfile();
        profile.set("reproductiveHistory", withCodes("KNOWN", "PRIOR_PREECLAMPSIA"));
        profile.set("underlyingConditions", withCodes("KNOWN", "PCOS", "ANEMIA"));
        ObjectNode lifestyle = objectMapper.createObjectNode();
        lifestyle.set("smoking", answer("CURRENT"));
        lifestyle.set("alcohol", answer("ANY_USE"));
        lifestyle.set("physicalActivity", answer("LOW"));
        lifestyle.set("sleep", answer("CONCERN"));
        lifestyle.set("flags", objectMapper.createArrayNode().add("SUBSTANCE_USE").add("STRESS").add("UNHEALTHY_DIET"));
        profile.set("lifestyle", lifestyle);
        profile.set("nutrition", withCodes("KNOWN", "FOLIC_ACID_NOT_STARTED",
                "IODINE_UNASSESSED_OR_INSUFFICIENT", "VITAMIN_D_INSUFFICIENT_OR_SUPPLEMENT",
                "IRON_INSUFFICIENT_OR_SUPPLEMENT", "CALCIUM_INSUFFICIENT_OR_SUPPLEMENT"));
        ObjectNode vaccination = objectMapper.createObjectNode();
        vaccination.set("flags", objectMapper.createArrayNode().add("NOT_ASSESSED"));
        vaccination.set("answers", objectMapper.createArrayNode()
                .add(state("UNKNOWN").put("code", "COVID_19"))
                .add(state("UNKNOWN").put("code", "HEPATITIS_B"))
                .add(state("UNKNOWN").put("code", "INFLUENZA"))
                .add(state("UNKNOWN").put("code", "RUBELLA_IMMUNITY"))
                .add(state("UNKNOWN").put("code", "TDAP")));
        profile.set("vaccination", vaccination);
        profile.set("currentMedications", withCodes("KNOWN", "HIGH_RISK_OR_CONTRAINDICATED", "NEEDS_ADJUSTMENT"));
        profile.set("sexualHealth", withCodes("KNOWN", "SAFE_SEX_COUNSELING_NEEDED",
                "REPRODUCTIVE_TRACT_INFECTION", "STI_RISK", "STI_SUSPECTED_OR_KNOWN", "NO_PREGNANCY_PLAN"));
        profile.set("sti", state("KNOWN").put("status", "SUSPECTED_OR_KNOWN"));

        ValidatedRecommendationProfile validated = validator.validateAccept(
                accepted(profile), JourneyType.PRE_PREGNANCY, null);

        assertThat(validated.signalSlugs()).contains(
                "rec-reproductive-preeclampsia",
                "rec-condition-pcos",
                "rec-condition-anemia",
                "rec-alcohol-use",
                "rec-lifestyle-substance-use",
                "rec-lifestyle-stress",
                "rec-lifestyle-unhealthy-diet",
                "rec-nutrition-folic-acid-needed",
                "rec-nutrition-iodine-review",
                "rec-nutrition-vitamin-d-review",
                "rec-nutrition-iron-review",
                "rec-nutrition-calcium-review",
                "rec-vaccination-assessment-needed",
                "rec-medication-high-risk-or-contraindicated",
                "rec-medication-adjustment-needed",
                "rec-sexual-health-safe-sex-counseling",
                "rec-sexual-health-reproductive-tract-infection",
                "rec-sexual-health-sti-risk",
                "rec-sexual-health-sti-suspected-or-known",
                "rec-sexual-health-no-pregnancy-plan",
                "rec-sti-suspected-or-known");
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

    private ObjectNode answer(String value) {
        return state("KNOWN").put("value", value);
    }

    private ObjectNode withCodes(String state, String... codes) {
        ObjectNode value = state(state);
        var values = objectMapper.createArrayNode();
        for (String code : codes) values.add(code);
        value.set("codes", values);
        return value;
    }
}

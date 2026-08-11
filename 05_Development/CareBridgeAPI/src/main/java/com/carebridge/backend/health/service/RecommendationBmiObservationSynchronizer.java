package com.carebridge.backend.health.service;

import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.HealthObservation;
import com.carebridge.backend.health.entity.ObservationShape;
import com.carebridge.backend.health.repository.HealthObservationRepository;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.recommendation.RecommendationConstants;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Copies a validated recommendation BMI answer into the canonical maternal Journey history. */
@Component
@RequiredArgsConstructor
public class RecommendationBmiObservationSynchronizer {

    static final String LEGACY_ID_PREFIX = "recommendation-bmi:";
    private static final String METRIC_CODE = "BMI";
    private static final String UNIT = "kg/m²";
    private static final String PROVENANCE_SOURCE = "RECOMMENDATION_PROFILE";
    private static final MathContext BMI_CONTEXT = MathContext.DECIMAL128;

    private final HealthObservationRepository observationRepository;

    public void synchronize(
            MotherJourney journey,
            UUID submissionId,
            Map<String, Object> normalizedProfile) {
        Map<String, Object> bmi = nestedMap(normalizedProfile, "bmi");
        if (!"KNOWN".equals(bmi.get("state"))) {
            return;
        }
        requireJourneyContext(journey);
        if (submissionId == null) {
            throw new IllegalStateException("Recommendation BMI submission identity is unavailable");
        }
        String legacyId = LEGACY_ID_PREFIX + submissionId;
        var existing = observationRepository.findByLegacySourceAndLegacyId(
                HealthObservation.CANONICAL_SOURCE, legacyId);
        if (existing.isPresent()) {
            HealthObservation observation = existing.get();
            if (!journey.getCareSubjectId().equals(observation.getCareSubjectId())
                    || !METRIC_CODE.equals(observation.getMetricCode())
                    || !HealthObservation.CANONICAL_SOURCE.equals(observation.getLegacySource())
                    || !legacyId.equals(observation.getLegacyId())) {
                throw new IllegalStateException("Recommendation BMI identity is owned by another observation context");
            }
            return;
        }

        BigDecimal weightKg = decimal(bmi, "weightKg");
        BigDecimal heightCm = decimal(bmi, "heightCm");
        BigDecimal heightMeters = heightCm.divide(BigDecimal.valueOf(100), BMI_CONTEXT);
        BigDecimal value = weightKg.divide(
                heightMeters.pow(2, BMI_CONTEXT), BMI_CONTEXT).setScale(2, RoundingMode.HALF_UP);
        LocalDate measuredOn = LocalDate.parse(text(bmi, "measuredOn"));

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("weightKg", weightKg);
        context.put("heightCm", heightCm);
        context.put("weightContext", text(bmi, "weightContext"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("journeyId", journey.getId().toString());
        payload.put("recommendationSubmissionId", submissionId.toString());
        payload.put("source", PROVENANCE_SOURCE);

        observationRepository.save(HealthObservation.builder()
                .careSubjectId(journey.getCareSubjectId())
                .metricCode(METRIC_CODE)
                .valueNumeric(value)
                .unit(UNIT)
                .measuredAt(measuredOn.atStartOfDay(RecommendationConstants.BUSINESS_ZONE).toInstant())
                .sourceType(DataSource.MANUAL)
                .context(context)
                .payload(payload)
                .originalUnit(UNIT)
                .definitionVersion(1)
                .observationShape(ObservationShape.POINT)
                .qualityLabel("UNKNOWN")
                .legacySource(HealthObservation.CANONICAL_SOURCE)
                .legacyId(legacyId)
                .subjectType("MOTHER")
                .sourceRecordId(null)
                .build());
    }

    private void requireJourneyContext(MotherJourney journey) {
        if (journey == null || journey.getId() == null || journey.getCareSubjectId() == null) {
            throw new IllegalStateException("Canonical maternal journey context is unavailable");
        }
    }

    private Map<String, Object> nestedMap(Map<String, Object> parent, String key) {
        Object value = parent == null ? null : parent.get(key);
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Validated recommendation BMI is unavailable");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((nestedKey, nestedValue) -> result.put(String.valueOf(nestedKey), nestedValue));
        return result;
    }

    private BigDecimal decimal(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (raw instanceof BigDecimal decimal) {
            return decimal;
        }
        if (raw instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        throw new IllegalStateException("Validated recommendation BMI measurement is unavailable");
    }

    private String text(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (raw == null || raw.toString().isBlank()) {
            throw new IllegalStateException("Validated recommendation BMI context is unavailable");
        }
        return raw.toString();
    }
}

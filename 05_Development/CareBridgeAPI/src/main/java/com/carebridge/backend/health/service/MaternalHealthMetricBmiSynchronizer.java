package com.carebridge.backend.health.service;

import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.recommendation.RecommendationConstants;
import com.carebridge.backend.recommendation.entity.RecommendationProfileStatus;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Synchronizes newly recorded maternal BMI measurements into the mother's
 * canonical recommendation profile JSON when active.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaternalHealthMetricBmiSynchronizer {

    private static final MathContext BMI_CONTEXT = MathContext.DECIMAL128;
    private final MotherJourneyRepository journeyRepository;

    public void synchronize(MotherJourney journey, BigDecimal weightKg, BigDecimal heightCm, Instant measuredAt) {
        if (journey == null || weightKg == null || heightCm == null || heightCm.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        var status = journey.getRecommendationProfileStatus();
        if (status != RecommendationProfileStatus.ACTIVE && status != RecommendationProfileStatus.REVIEW_REQUIRED) {
            return;
        }

        Map<String, Object> envelope = journey.getRecommendationProfileJson();
        if (envelope == null) {
            envelope = new LinkedHashMap<>();
        } else {
            envelope = new LinkedHashMap<>(envelope);
        }

        Map<String, Object> profile = nestedMap(envelope, "profile");
        Map<String, Object> derived = nestedMap(envelope, "derived");

        LocalDate measuredOn = (measuredAt != null ? measuredAt : Instant.now())
                .atZone(RecommendationConstants.BUSINESS_ZONE)
                .toLocalDate();

        BigDecimal heightMeters = heightCm.divide(BigDecimal.valueOf(100), BMI_CONTEXT);
        BigDecimal bmi = weightKg.divide(heightMeters.pow(2, BMI_CONTEXT), BMI_CONTEXT);

        String category = bmi.compareTo(BigDecimal.valueOf(18.5)) < 0 ? "UNDERWEIGHT"
                : bmi.compareTo(BigDecimal.valueOf(25)) < 0 ? "HEALTHY_RANGE"
                : bmi.compareTo(BigDecimal.valueOf(30)) < 0 ? "OVERWEIGHT" : "OBESITY";

        String defaultContext = defaultWeightContext(journey.getJourneyType());

        Map<String, Object> bmiDomain = nestedMap(profile, "bmi");
        String currentContext = (String) bmiDomain.get("weightContext");
        String resolvedContext = (currentContext != null && !currentContext.isBlank()) ? currentContext : defaultContext;

        bmiDomain.put("state", "KNOWN");
        bmiDomain.put("heightCm", heightCm.setScale(1, RoundingMode.HALF_UP));
        bmiDomain.put("weightKg", weightKg.setScale(1, RoundingMode.HALF_UP));
        bmiDomain.put("weightContext", resolvedContext);
        bmiDomain.put("measuredOn", measuredOn.toString());

        profile.put("bmi", bmiDomain);

        derived.put("bmi", bmi.setScale(2, RoundingMode.HALF_UP));
        derived.put("bmiCategory", category);
        derived.put("bmiSignalEligible", true);

        envelope.put("profile", profile);
        envelope.put("derived", derived);
        envelope.put("updatedAt", Instant.now().toString());

        journey.setRecommendationProfileJson(envelope);
        journeyRepository.save(journey);
        log.info("Synchronized maternal BMI measurement to recommendation profile for journey: {}", journey.getId());
    }

    private String defaultWeightContext(JourneyType journeyType) {
        if (journeyType == null) return "CURRENT_PREGNANCY";
        return switch (journeyType) {
            case PRE_PREGNANCY -> "CURRENT_NON_PREGNANT";
            case POSTPARTUM -> "CURRENT_POSTPARTUM";
            default -> "CURRENT_PREGNANCY";
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        Map<String, Object> fresh = new LinkedHashMap<>();
        parent.put(key, fresh);
        return fresh;
    }
}

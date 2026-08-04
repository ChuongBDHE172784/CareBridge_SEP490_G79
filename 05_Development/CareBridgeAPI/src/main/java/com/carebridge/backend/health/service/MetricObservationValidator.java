package com.carebridge.backend.health.service;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.health.dto.AddMetricRequest;
import com.carebridge.backend.health.dto.UpdateMetricRequest;
import com.carebridge.backend.health.entity.DataSource;
import com.carebridge.backend.health.entity.MetricDefinition;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.entity.ObservationShape;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class MetricObservationValidator {

    private static final String[] GLUCOSE_CONTEXTS = {
            "FASTING", "PRE_MEAL", "POST_MEAL_1H", "POST_MEAL_2H", "RANDOM", "OTHER_APPROVED"
    };

    public NormalizedObservation normalize(AddMetricRequest request, MetricDefinition definition) {
        if (request == null || request.getMetricType() == null) {
            reject("METRIC-030", "Metric type is required");
        }
        if (request.getMeasuredAt() == null) {
            reject("METRIC-031", "measuredAt is required");
        }
        if (request.getMeasuredAt().isAfter(Instant.now().plusSeconds(300))) {
            reject("METRIC-004", "measuredAt cannot be more than 5 minutes in the future");
        }

        String metricCode = canonicalCode(request.getMetricType());
        if (metricCode == null) reject("METRIC-030", "Unsupported metric type");
        requireDefinition(definition, metricCode);

        String sourceUnit = normalizeUnit(request.getUnit(), definition);
        BigDecimal primary = normalizeNumber(request.getValueNumeric(), definition);
        BigDecimal secondary = request.getValueSecondary() == null
                ? null : normalizeNumber(request.getValueSecondary(), definition);
        if ("BLOOD_GLUCOSE".equals(metricCode) && "mmol/L".equalsIgnoreCase(sourceUnit)) {
            primary = primary == null ? null : primary.multiply(new BigDecimal("18.0182"));
        }
        if ("WEIGHT".equals(metricCode) && "lb".equalsIgnoreCase(request.getUnit())) {
            primary = primary == null ? null : primary.multiply(new BigDecimal("0.45359237"));
        }
        Map<String, Object> context = copyContext(request.getContext());

        if (definition.getObservationShape() == ObservationShape.PAIRED_POINT) {
            requirePositive(primary, "Systolic value is required");
            requirePositive(secondary, "Diastolic value is required");
            if (primary.compareTo(secondary) <= 0) {
                reject("METRIC-032", "Systolic value must be greater than diastolic value");
            }
        } else if (definition.getObservationShape() == ObservationShape.SESSION) {
            validateFetalMovement(request, primary);
        } else {
            requirePositive(primary, "Metric value must be positive");
        }

        if ("BLOOD_GLUCOSE".equals(metricCode)) {
            requireGlucoseContext(context);
        }

        Instant periodStart = request.getPeriodStart();
        Instant periodEnd = request.getPeriodEnd();
        validatePeriod(periodStart, periodEnd, definition.getObservationShape());
        context.put("metricCode", metricCode);
        context.put("definitionVersion", definition.getVersion());
        context.put("originalUnit", request.getUnit() == null ? sourceUnit : request.getUnit());
        if (request.getSourceType() != null) context.put("sourceType", request.getSourceType().name());

        return new NormalizedObservation(
                metricCode,
                primary,
                secondary,
                sourceUnit,
                request.getMeasuredAt(),
                request.getSourceType() == null ? DataSource.MANUAL : request.getSourceType(),
                request.getNote(),
                context,
                periodStart,
                periodEnd,
                definition.getVersion());
    }

    public NormalizedObservation mergeAndNormalize(
            MetricType existingType,
            BigDecimal existingPrimary,
            BigDecimal existingSecondary,
            String existingUnit,
            Instant existingMeasuredAt,
            DataSource existingSource,
            String existingNote,
            Map<String, Object> existingContext,
            Instant existingPeriodStart,
            Instant existingPeriodEnd,
            UpdateMetricRequest request,
            MetricDefinition definition) {
        AddMetricRequest merged = new AddMetricRequest();
        merged.setMetricType(existingType);
        merged.setValueNumeric(request.getValueNumeric() == null ? existingPrimary : request.getValueNumeric());
        merged.setValueSecondary(request.getValueSecondary() == null ? existingSecondary : request.getValueSecondary());
        merged.setUnit(request.getUnit() == null ? existingUnit : request.getUnit());
        merged.setMeasuredAt(request.getMeasuredAt() == null ? existingMeasuredAt : request.getMeasuredAt());
        merged.setSourceType(existingSource);
        merged.setNote(request.getNote() == null ? existingNote : request.getNote());
        merged.setContext(request.getContext() == null ? existingContext : request.getContext());
        merged.setPeriodStart(request.getPeriodStart() == null ? existingPeriodStart : request.getPeriodStart());
        merged.setPeriodEnd(request.getPeriodEnd() == null ? existingPeriodEnd : request.getPeriodEnd());
        return normalize(merged, definition);
    }

    public String canonicalCode(MetricType type) {
        if (type == null) return null;
        return switch (type) {
            case WEIGHT -> "WEIGHT";
            case BLOOD_PRESSURE, BLOOD_PRESSURE_SYSTOLIC, BLOOD_PRESSURE_DIASTOLIC -> "BLOOD_PRESSURE";
            case BLOOD_GLUCOSE -> "BLOOD_GLUCOSE";
            case FETAL_MOVEMENT_SESSION, FETAL_MOVEMENT_COUNT -> "FETAL_MOVEMENT_SESSION";
            case HYDRATION -> "HYDRATION";
            case EPDS_SCORE -> "EPDS_SCORE";
            default -> null;
        };
    }

    private String normalizeUnit(String requested, MetricDefinition definition) {
        String unit = requested == null || requested.isBlank() ? definition.getCanonicalUnit() : requested.trim();
        if (definition.getAcceptedInputUnits() != null && definition.getAcceptedInputUnits().stream()
                .noneMatch(candidate -> candidate.equalsIgnoreCase(unit))) {
            reject("METRIC-033", "Unsupported unit for metric: " + unit);
        }
        if ("BLOOD_GLUCOSE".equals(definition.getMetricCode()) && "mmol/L".equalsIgnoreCase(unit)) {
            return "mg/dL";
        }
        if ("WEIGHT".equals(definition.getMetricCode()) && "lb".equalsIgnoreCase(unit)) {
            return "kg";
        }
        return definition.getCanonicalUnit();
    }

    private BigDecimal normalizeNumber(BigDecimal value, MetricDefinition definition) {
        if (value == null) return null;
        Short scale = definition.getPrecisionScale();
        return scale == null ? value : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private void validateFetalMovement(AddMetricRequest request, BigDecimal count) {
        requirePositiveOrZero(count, "Movement count is required");
        if (request.getPeriodStart() == null || request.getPeriodEnd() == null) {
            reject("METRIC-034", "Fetal movement period is required");
        }
        String protocolCode = text(request.getContext(), "protocolCode");
        String completionStatus = text(request.getContext(), "completionStatus");
        String gestationalAge = text(request.getContext(), "gestationalAgeSnapshot");
        if (protocolCode == null || completionStatus == null || gestationalAge == null) {
            reject("METRIC-035", "Fetal movement session context is incomplete");
        }
    }

    private void requireGlucoseContext(Map<String, Object> context) {
        String value = text(context, "measurementContext");
        for (String accepted : GLUCOSE_CONTEXTS) {
            if (accepted.equals(value)) return;
        }
        reject("METRIC-036", "Glucose measurementContext is required and unsupported values are rejected");
    }

    private void validatePeriod(Instant start, Instant end, ObservationShape shape) {
        if (shape != ObservationShape.SESSION) return;
        if (start == null || end == null || !end.isAfter(start)) {
            reject("METRIC-037", "Session periodEnd must be after periodStart");
        }
    }

    private Map<String, Object> copyContext(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private String text(Map<String, Object> context, String key) {
        Object value = context == null ? null : context.get(key);
        if (value == null || value.toString().isBlank()) return null;
        return value.toString().trim().toUpperCase(Locale.ROOT);
    }

    private void requireDefinition(MetricDefinition definition, String metricCode) {
        if (definition == null || !metricCode.equals(definition.getMetricCode()) || !definition.isActive()) {
            reject("METRIC-030", "Metric is not supported for this P0 flow");
        }
    }

    private void requirePositive(BigDecimal value, String message) {
        if (value == null || value.signum() <= 0) reject("METRIC-038", message);
    }

    private void requirePositiveOrZero(BigDecimal value, String message) {
        if (value == null || value.signum() < 0) reject("METRIC-038", message);
    }

    private void reject(String code, String message) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, code, message);
    }

    public record NormalizedObservation(
            String metricCode,
            BigDecimal valueNumeric,
            BigDecimal valueSecondary,
            String unit,
            Instant measuredAt,
            DataSource sourceType,
            String note,
            Map<String, Object> context,
            Instant periodStart,
            Instant periodEnd,
            int definitionVersion) {
    }
}

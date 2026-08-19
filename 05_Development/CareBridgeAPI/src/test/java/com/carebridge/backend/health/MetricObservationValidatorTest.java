package com.carebridge.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.health.dto.AddMetricRequest;
import com.carebridge.backend.health.entity.MetricDefinition;
import com.carebridge.backend.health.entity.MetricType;
import com.carebridge.backend.health.entity.ObservationShape;
import com.carebridge.backend.health.service.MetricObservationValidator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class MetricObservationValidatorTest {

    private final MetricObservationValidator validator = new MetricObservationValidator();

    @Test
    void bloodPressureIsOnePairedObservation() {
        AddMetricRequest request = base(MetricType.BLOOD_PRESSURE, "120", "mmHg");
        request.setValueSecondary(new BigDecimal("80"));

        var result = validator.normalize(request,
                definition("BLOOD_PRESSURE", ObservationShape.PAIRED_POINT, "mmHg", List.of("mmHg")));

        assertThat(result.metricCode()).isEqualTo("BLOOD_PRESSURE");
        assertThat(result.valueNumeric()).isEqualByComparingTo("120");
        assertThat(result.valueSecondary()).isEqualByComparingTo("80");
    }

    @Test
    void glucoseRequiresStructuredContext() {
        AddMetricRequest request = base(MetricType.BLOOD_GLUCOSE, "95", "mg/dL");

        assertThatThrownBy(() -> validator.normalize(request,
                definition("BLOOD_GLUCOSE", ObservationShape.POINT, "mg/dL", List.of("mg/dL", "mmol/L"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo("METRIC-036"));
    }

    @Test
    void fetalMovementRequiresSessionPeriodAndContext() {
        AddMetricRequest request = base(MetricType.FETAL_MOVEMENT_SESSION, "7", "count");
        request.setPeriodStart(Instant.parse("2026-07-31T08:00:00Z"));
        request.setPeriodEnd(Instant.parse("2026-07-31T09:00:00Z"));
        request.setContext(new LinkedHashMap<>());
        request.getContext().put("protocolCode", "TEN_MOVEMENTS");
        request.getContext().put("completionStatus", "COMPLETED");
        request.getContext().put("gestationalAgeSnapshot", "32w3d");

        var result = validator.normalize(request,
                definition("FETAL_MOVEMENT_SESSION", ObservationShape.SESSION, "count", List.of("count")));

        assertThat(result.metricCode()).isEqualTo("FETAL_MOVEMENT_SESSION");
        assertThat(result.periodEnd()).isAfter(result.periodStart());
    }

    @Test
    void unsupportedTypeNeverFallsBackToWeight() {
        AddMetricRequest request = base(MetricType.OTHER, "65", "kg");

        assertThatThrownBy(() -> validator.normalize(request,
                definition("WEIGHT", ObservationShape.POINT, "kg", List.of("kg"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo("METRIC-030"));
    }

    @Test
    void bmiIsCalculatedFromWeightAndHeightContext() {
        AddMetricRequest request = base(MetricType.BMI, "1", "kg/m²");
        request.setContext(new LinkedHashMap<>());
        request.getContext().put("weightKg", "55");
        request.getContext().put("heightCm", "160");

        var result = validator.normalize(request,
                definition("BMI", ObservationShape.POINT, "kg/m²", List.of("kg/m²")));

        assertThat(result.metricCode()).isEqualTo("BMI");
        assertThat(result.valueNumeric()).isEqualByComparingTo("21.48");
        assertThat(result.context()).containsEntry("weightKg", new BigDecimal("55.00"));
        assertThat(result.context()).containsEntry("heightCm", new BigDecimal("160.0"));
    }

    @Test
    void bmiRejectsMissingSourceMeasurements() {
        AddMetricRequest request = base(MetricType.BMI, "21.5", "kg/m²");

        assertThatThrownBy(() -> validator.normalize(request,
                definition("BMI", ObservationShape.POINT, "kg/m²", List.of("kg/m²"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo("METRIC-039"));
    }

    @Test
    void bmiAcceptsTheSharedTwoHundredFiftyCentimeterBoundary() {
        AddMetricRequest request = base(MetricType.BMI, "1", "kg/m²");
        request.setContext(new LinkedHashMap<>());
        request.getContext().put("weightKg", "70.0");
        request.getContext().put("heightCm", "250.0");

        var result = validator.normalize(request,
                definition("BMI", ObservationShape.POINT, "kg/m²", List.of("kg/m²")));

        assertThat(result.valueNumeric()).isEqualByComparingTo("11.20");
        assertThat(result.context()).containsEntry("heightCm", new BigDecimal("250.0"));
    }

    @Test
    void bmiRejectsHeightAboveSharedBoundaryOrMoreThanOneInputDecimal() {
        AddMetricRequest tooTall = base(MetricType.BMI, "1", "kg/m²");
        tooTall.setContext(new LinkedHashMap<>());
        tooTall.getContext().put("weightKg", "70.0");
        tooTall.getContext().put("heightCm", "250.1");
        AddMetricRequest tooPrecise = base(MetricType.BMI, "1", "kg/m²");
        tooPrecise.setContext(new LinkedHashMap<>());
        tooPrecise.getContext().put("weightKg", "70.00");
        tooPrecise.getContext().put("heightCm", "160.0");

        assertThatThrownBy(() -> validator.normalize(tooTall,
                definition("BMI", ObservationShape.POINT, "kg/m²", List.of("kg/m²"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo("METRIC-039"));
        assertThatThrownBy(() -> validator.normalize(tooPrecise,
                definition("BMI", ObservationShape.POINT, "kg/m²", List.of("kg/m²"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo("METRIC-039"));
    }

    @Test
    void epdsAcceptsZeroAndThirtyScoreBoundariesWithVietnameseUnit() {
        AddMetricRequest request = base(MetricType.EPDS_SCORE, "0", "điểm");
        request.setValueSecondary(BigDecimal.ZERO);

        var result = validator.normalize(request,
                definition("EPDS_SCORE", ObservationShape.POINT, "score", List.of("score", "điểm")));

        assertThat(result.metricCode()).isEqualTo("EPDS_SCORE");
        assertThat(result.valueNumeric()).isEqualByComparingTo("0");
        assertThat(result.valueSecondary()).isEqualByComparingTo("0");
        assertThat(result.unit()).isEqualTo("score");

        AddMetricRequest maxScore = base(MetricType.EPDS_SCORE, "30", "score");
        maxScore.setValueSecondary(new BigDecimal("3"));
        var maxResult = validator.normalize(maxScore,
                definition("EPDS_SCORE", ObservationShape.POINT, "score", List.of("score", "điểm")));
        assertThat(maxResult.valueNumeric()).isEqualByComparingTo("30");
        assertThat(maxResult.valueSecondary()).isEqualByComparingTo("3");
    }

    @Test
    void epdsRejectsOutOfBoundScores() {
        AddMetricRequest scoreTooHigh = base(MetricType.EPDS_SCORE, "31", "điểm");
        assertThatThrownBy(() -> validator.normalize(scoreTooHigh,
                definition("EPDS_SCORE", ObservationShape.POINT, "score", List.of("score", "điểm"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo("METRIC-039"));

        AddMetricRequest question10TooHigh = base(MetricType.EPDS_SCORE, "15", "điểm");
        question10TooHigh.setValueSecondary(new BigDecimal("4"));
        assertThatThrownBy(() -> validator.normalize(question10TooHigh,
                definition("EPDS_SCORE", ObservationShape.POINT, "score", List.of("score", "điểm"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo("METRIC-039"));
    }

    @Test
    void temperatureNormalizesCorrectly() {
        AddMetricRequest request = base(MetricType.TEMPERATURE, "37.2", "°C");
        request.setContext(new LinkedHashMap<>());
        request.getContext().put("measurementSite", "ARMPIT");

        var result = validator.normalize(request,
                definition("TEMPERATURE", ObservationShape.POINT, "Cel", List.of("Cel", "°C", "°F")));

        assertThat(result.metricCode()).isEqualTo("TEMPERATURE");
        assertThat(result.valueNumeric()).isEqualByComparingTo("37.2");
        assertThat(result.unit()).isEqualTo("Cel");
    }

    @Test
    void bloodPressureRejectsPhysiologicallyImpossibleValues() {
        AddMetricRequest request = base(MetricType.BLOOD_PRESSURE, "600", "mmHg");
        request.setValueSecondary(new BigDecimal("500"));

        assertThatThrownBy(() -> validator.normalize(request,
                definition("BLOOD_PRESSURE", ObservationShape.PAIRED_POINT, "mmHg", List.of("mmHg"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo("METRIC-039"));
    }

    @Test
    void heartRateRejectsOutOfRangeValues() {
        AddMetricRequest request = base(MetricType.MATERNAL_HEART_RATE, "29", "bpm");

        assertThatThrownBy(() -> validator.normalize(request,
                definition("MATERNAL_HEART_RATE", ObservationShape.POINT, "bpm", List.of("bpm"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo("METRIC-039"));
    }

    private AddMetricRequest base(MetricType type, String value, String unit) {
        AddMetricRequest request = new AddMetricRequest();
        request.setMetricType(type);
        request.setValueNumeric(new BigDecimal(value));
        request.setUnit(unit);
        request.setMeasuredAt(Instant.now().minusSeconds(30));
        return request;
    }

    private MetricDefinition definition(String code, ObservationShape shape, String unit, List<String> units) {
        return MetricDefinition.builder()
                .metricCode(code)
                .version(1)
                .displayName(code)
                .observationShape(shape)
                .subjectType("MOTHER")
                .manualEntrySupported(true)
                .canonicalUnit(unit)
                .acceptedInputUnits(units)
                .precisionScale((short) 2)
                .active(true)
                .effectiveFrom(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}

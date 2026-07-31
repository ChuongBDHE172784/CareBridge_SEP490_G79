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

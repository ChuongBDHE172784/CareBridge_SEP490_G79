import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/healthRecords/models/health_metric_model.dart';

void main() {
  group('MetricTypeExtension', () {
    test('maps quick-note observation types from the API', () {
      expect(MetricTypeExtension.fromApi('BMI'), MetricType.bmi);
      expect(MetricTypeExtension.fromApi('WEIGHT'), MetricType.other);
      expect(MetricTypeExtension.fromApi('HYDRATION'), MetricType.hydration);
      expect(MetricTypeExtension.fromApi('MOOD'), MetricType.mood);
      expect(
        MetricTypeExtension.fromApi('FETAL_MOVEMENT_COUNT'),
        MetricType.fetalMovement,
      );
    });

    test('uses Vietnamese quick-note labels', () {
      expect(MetricType.bmi.displayLabel, 'Chỉ số BMI');
      expect(MetricType.hydration.displayLabel, 'Nước');
      expect(MetricType.mood.displayLabel, 'Tâm trạng');
    });
  });

  group('Health metric canonical model', () {
    test('maps legacy blood pressure and fetal movement codes', () {
      expect(
        MetricTypeExtension.fromApi('BLOOD_PRESSURE_SYSTOLIC'),
        MetricType.bloodPressure,
      );
      expect(
        MetricTypeExtension.fromApi('BLOOD_PRESSURE'),
        MetricType.bloodPressure,
      );
      expect(
        MetricTypeExtension.fromApi('FETAL_MOVEMENT_SESSION'),
        MetricType.fetalMovement,
      );
    });

    test('serializes glucose context without dropping it', () {
      final request = AddMetricRequest(
        metricType: 'BLOOD_GLUCOSE',
        valueNumeric: 95,
        unit: 'mg/dL',
        measuredAt: DateTime.utc(2026, 7, 30, 10),
        context: const {'measurementContext': 'FASTING'},
        definitionVersion: 1,
      );

      final json = request.toJson();

      expect(json['metricType'], 'BLOOD_GLUCOSE');
      expect(json['context'], {'measurementContext': 'FASTING'});
      expect(json['definitionVersion'], 1);
    });

    test('does not calculate an undifferentiated blood pressure average', () {
      final trend = MetricTrend(
        metricType: 'BLOOD_PRESSURE',
        unit: 'mmHg',
        dataPoints: [
          _point(valueNumeric: 120, valueSecondary: 80),
          _point(valueNumeric: 130, valueSecondary: 85),
        ],
      );

      expect(trend.average, isNull);
      expect(trend.trend, isNull);
    });

    test('does not mix glucose contexts in a generic average', () {
      final trend = MetricTrend(
        metricType: 'BLOOD_GLUCOSE',
        unit: 'mg/dL',
        dataPoints: [
          _point(
            valueNumeric: 95,
            context: const {'measurementContext': 'FASTING'},
          ),
          _point(
            valueNumeric: 135,
            context: const {'measurementContext': 'POST_MEAL_1H'},
          ),
        ],
      );

      expect(trend.average, isNull);
    });
  });
}

MetricDataPoint _point({
  required double valueNumeric,
  double? valueSecondary,
  Map<String, dynamic> context = const {},
}) {
  return MetricDataPoint(
    measuredAt: DateTime.utc(2026, 7, 30, 10),
    valueNumeric: valueNumeric,
    valueSecondary: valueSecondary,
    context: context,
  );
}

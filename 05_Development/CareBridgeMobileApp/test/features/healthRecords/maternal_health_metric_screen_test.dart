import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/healthRecords/models/health_metric_model.dart';
import 'package:untitled/features/healthRecords/screens/maternal_health_metric_screen.dart';

void main() {
  testWidgets('BMI detail shows the source weight and height', (tester) async {
    final metric = HealthMetricDetail(
      id: 'bmi-1',
      journeyId: 'journey-1',
      metricType: MetricType.bmi,
      metricCode: 'BMI',
      valueNumeric: 21.48,
      unit: 'kg/m²',
      measuredAt: DateTime(2026, 8, 4, 9, 30),
      sourceType: SourceType.manual,
      createdAt: DateTime(2026, 8, 4, 9, 30),
      context: const {'weightKg': 55, 'heightCm': 160},
    );

    await tester.pumpWidget(
      MaterialApp(
        home: MaternalHealthMetricScreen(
          metricId: metric.id,
          initialMetric: metric,
        ),
      ),
    );

    expect(find.text('Chỉ số BMI'), findsOneWidget);
    expect(find.text('Cân nặng'), findsOneWidget);
    expect(find.text('55.0 kg'), findsOneWidget);
    expect(find.text('Chiều cao'), findsOneWidget);
    expect(find.text('160.0 cm'), findsOneWidget);
  });
}

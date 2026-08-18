import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/healthRecords/models/health_metric_model.dart';
import 'package:untitled/features/healthRecords/screens/add_maternal_health_metric_screen.dart';
import 'package:untitled/features/healthRecords/services/health_metric_service.dart';

class _FakeHealthMetricService extends HealthMetricService {
  AddMetricRequest? submitted;

  @override
  Future<List<MetricCapability>> getCapabilities(String journeyId) async =>
      const [
        MetricCapability(
          metricCode: 'BMI',
          version: 1,
          displayName: 'Chỉ số BMI',
          observationShape: 'POINT',
          manualEntrySupported: true,
          deviceImportSupported: false,
          canonicalUnit: 'kg/m²',
          acceptedInputUnits: ['kg/m²'],
          requiredContextSchema: {
            'required': ['weightKg', 'heightCm'],
          },
        ),
        MetricCapability(
          metricCode: 'BLOOD_PRESSURE',
          version: 1,
          displayName: 'Huyết áp',
          observationShape: 'COMPOUND',
          manualEntrySupported: true,
          deviceImportSupported: false,
          canonicalUnit: 'mmHg',
          acceptedInputUnits: ['mmHg'],
          requiredContextSchema: const {},
        ),
        MetricCapability(
          metricCode: 'TEMPERATURE',
          version: 1,
          displayName: 'Nhiệt độ cơ thể',
          observationShape: 'POINT',
          manualEntrySupported: true,
          deviceImportSupported: false,
          canonicalUnit: '°C',
          acceptedInputUnits: ['°C', 'Cel'],
          requiredContextSchema: const {
            'required': ['measurementSite'],
          },
        ),
      ];

  @override
  Future<HealthMetricDetail> addMetric(
    String journeyId,
    AddMetricRequest request,
  ) async {
    submitted = request;
    return HealthMetricDetail(
      id: 'metric-1',
      journeyId: journeyId,
      metricType: MetricType.bmi,
      metricCode: request.metricType,
      valueNumeric: request.valueNumeric,
      unit: request.unit,
      measuredAt: request.measuredAt,
      sourceType: SourceType.manual,
      createdAt: request.measuredAt,
      context: request.context,
      definitionVersion: request.definitionVersion,
    );
  }
}

void main() {
  testWidgets('direct Journey BMI accepts 250 cm and submits source values', (
    tester,
  ) async {
    await tester.binding.setSurfaceSize(const Size(900, 1400));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final service = _FakeHealthMetricService();
    await tester.pumpWidget(
      MaterialApp(
        home: AddMaternalHealthMetricScreen(
          journeyId: 'journey-1',
          initialMetricType: 'BMI',
          service: service,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.enterText(
      find.byKey(const Key('maternal-metric-primary')),
      '70.0',
    );
    await tester.enterText(
      find.byKey(const Key('maternal-metric-secondary')),
      '250.0',
    );
    await tester.tap(find.byKey(const Key('maternal-metric-save')));
    await tester.pump();

    expect(service.submitted, isNotNull);
    expect(service.submitted!.context['weightKg'], 70.0);
    expect(service.submitted!.context['heightCm'], 250.0);
    expect(service.submitted!.valueNumeric, closeTo(11.2, 0.001));
  });

  testWidgets('direct Journey BMI rejects height above 250 cm', (tester) async {
    await tester.binding.setSurfaceSize(const Size(900, 1400));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final service = _FakeHealthMetricService();
    await tester.pumpWidget(
      MaterialApp(
        home: AddMaternalHealthMetricScreen(
          journeyId: 'journey-1',
          initialMetricType: 'BMI',
          service: service,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.enterText(
      find.byKey(const Key('maternal-metric-primary')),
      '70.0',
    );
    await tester.enterText(
      find.byKey(const Key('maternal-metric-secondary')),
      '250.1',
    );
    await tester.tap(find.byKey(const Key('maternal-metric-save')));
    await tester.pump();

    expect(service.submitted, isNull);
    expect(find.textContaining('chiều cao 100–250 cm'), findsOneWidget);
  });

  testWidgets(
    'severe blood pressure 165/110 triggers Critical Emergency Alert Dialog',
    (tester) async {
      await tester.binding.setSurfaceSize(const Size(900, 1400));
      addTearDown(() => tester.binding.setSurfaceSize(null));
      final service = _FakeHealthMetricService();
      await tester.pumpWidget(
        MaterialApp(
          home: AddMaternalHealthMetricScreen(
            journeyId: 'journey-1',
            initialMetricType: 'BLOOD_PRESSURE',
            service: service,
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.enterText(
        find.byKey(const Key('maternal-metric-primary')),
        '165',
      );
      await tester.enterText(
        find.byKey(const Key('maternal-metric-secondary')),
        '110',
      );
      await tester.tap(find.byKey(const Key('maternal-metric-save')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 500));

      expect(service.submitted, isNotNull);
      expect(find.text('CẢNH BÁO NGUY CẤP Y TẾ'), findsOneWidget);
      expect(find.text('MỞ BẢN ĐỒ BỆNH VIỆN & CẤP CỨU'), findsOneWidget);
      expect(find.text('GỌI CẤP CỨU 115 NGAY'), findsOneWidget);
    },
  );

  testWidgets(
    'mild blood pressure 135/88 triggers Anomaly Warning Dialog to AI Nurse',
    (tester) async {
      await tester.binding.setSurfaceSize(const Size(900, 1400));
      addTearDown(() => tester.binding.setSurfaceSize(null));
      final service = _FakeHealthMetricService();
      await tester.pumpWidget(
        MaterialApp(
          home: AddMaternalHealthMetricScreen(
            journeyId: 'journey-1',
            initialMetricType: 'BLOOD_PRESSURE',
            service: service,
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.enterText(
        find.byKey(const Key('maternal-metric-primary')),
        '135',
      );
      await tester.enterText(
        find.byKey(const Key('maternal-metric-secondary')),
        '88',
      );
      await tester.tap(find.byKey(const Key('maternal-metric-save')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 500));

      expect(service.submitted, isNotNull);
      expect(find.text('LƯU Ý THEO DÕI SỨC KHỎE'), findsOneWidget);
      expect(find.text('HỎI TRỢ LÝ AI NURSE'), findsOneWidget);
    },
  );

  testWidgets('high fever 39.0 C triggers Critical Emergency Alert Dialog', (
    tester,
  ) async {
    await tester.binding.setSurfaceSize(const Size(900, 1400));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final service = _FakeHealthMetricService();
    await tester.pumpWidget(
      MaterialApp(
        home: AddMaternalHealthMetricScreen(
          journeyId: 'journey-1',
          initialMetricType: 'TEMPERATURE',
          service: service,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.enterText(
      find.byKey(const Key('maternal-metric-primary')),
      '39.0',
    );
    await tester.tap(find.byKey(const Key('maternal-metric-save')));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));

    expect(service.submitted, isNotNull);
    expect(service.submitted!.context['measurementSite'], 'ARMPIT');
    expect(service.submitted!.valueNumeric, 39.0);
    expect(find.text('CẢNH BÁO NGUY CẤP Y TẾ'), findsOneWidget);
    expect(find.text('MỞ BẢN ĐỒ BỆNH VIỆN & CẤP CỨU'), findsOneWidget);
  });

  testWidgets('mild fever 37.8 C triggers Anomaly Warning Dialog to AI Nurse', (
    tester,
  ) async {
    await tester.binding.setSurfaceSize(const Size(900, 1400));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    final service = _FakeHealthMetricService();
    await tester.pumpWidget(
      MaterialApp(
        home: AddMaternalHealthMetricScreen(
          journeyId: 'journey-1',
          initialMetricType: 'TEMPERATURE',
          service: service,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.enterText(
      find.byKey(const Key('maternal-metric-primary')),
      '37.8',
    );
    await tester.tap(find.byKey(const Key('maternal-metric-save')));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));

    expect(service.submitted, isNotNull);
    expect(service.submitted!.context['measurementSite'], 'ARMPIT');
    expect(service.submitted!.valueNumeric, 37.8);
    expect(find.text('LƯU Ý THEO DÕI SỨC KHỎE'), findsOneWidget);
    expect(find.text('HỎI TRỢ LÝ AI NURSE'), findsOneWidget);
  });
}

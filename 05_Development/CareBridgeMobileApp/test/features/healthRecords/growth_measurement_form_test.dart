import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/healthRecords/models/growth_measurement_model.dart';
import 'package:untitled/features/healthRecords/screens/growth_measurement_form_screen.dart';
import 'package:untitled/features/healthRecords/services/growth_measurement_service.dart';

class _FakeGrowthMeasurementService extends GrowthMeasurementService {
  Map<String, dynamic>? addedPayload;
  Map<String, dynamic>? updatedPayload;
  String? updatedId;
  bool fail = false;

  @override
  Future<void> addGrowthMeasurement(
    String babyId,
    Map<String, dynamic> payload,
  ) async {
    if (fail) throw StateError('offline');
    addedPayload = {'babyId': babyId, ...payload};
  }

  @override
  Future<void> updateGrowthMeasurement(
    String babyId,
    String measurementId,
    Map<String, dynamic> payload,
  ) async {
    if (fail) throw StateError('offline');
    updatedId = measurementId;
    updatedPayload = {'babyId': babyId, ...payload};
  }
}

GrowthMeasurement _measurement() => GrowthMeasurement(
  id: 'growth-1',
  measuredAt: DateTime(2026, 7, 14),
  weightKg: 6.1,
  heightCm: 61.5,
  recordedBy: 'mother-1',
  sourceType: 'CLINIC',
  note: 'Before edit',
);

GrowthMeasurement _measurementWithoutNote() => GrowthMeasurement(
  id: 'growth-2',
  measuredAt: DateTime(2026, 7, 14),
  weightKg: 6.1,
  recordedBy: 'mother-1',
  sourceType: 'CLINIC',
);

Future<void> _pumpForm(
  WidgetTester tester, {
  GrowthMeasurementService? service,
  Future<void> Function(String babyId, Map<String, dynamic> payload)? onAdd,
  Future<void> Function(
    String babyId,
    String measurementId,
    Map<String, dynamic> payload,
  )?
  onUpdate,
  GrowthMeasurement? measurement,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: GrowthMeasurementFormScreen(
        babyId: 'baby-1',
        measurement: measurement,
        service: service,
        onAdd: onAdd,
        onUpdate: onUpdate,
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('valid add submits the selected baby and measurement fields', (
    tester,
  ) async {
    String? babyId;
    Map<String, dynamic>? payload;
    await _pumpForm(
      tester,
      onAdd: (id, body) async {
        babyId = id;
        payload = body;
      },
    );

    await tester.enterText(find.byKey(const Key('growth-form-weight')), '6.2');
    await tester.tap(find.byKey(const Key('growth-form-save')));
    await tester.pumpAndSettle();

    expect(babyId, 'baby-1');
    expect(payload?['weightKg'], 6.2);
    expect(payload?['sourceType'], 'HOME_SCALE');
    expect(payload?['measuredDate'], matches(RegExp(r'^\d{4}-\d{2}-\d{2}$')));
  });

  testWidgets('edit submits a PATCH payload for the existing measurement', (
    tester,
  ) async {
    String? babyId;
    String? measurementId;
    Map<String, dynamic>? payload;
    await _pumpForm(
      tester,
      measurement: _measurement(),
      onUpdate: (id, recordId, body) async {
        babyId = id;
        measurementId = recordId;
        payload = body;
      },
    );

    await tester.enterText(find.byKey(const Key('growth-form-weight')), '6.3');
    await tester.tap(find.byKey(const Key('growth-form-save')));
    await tester.pumpAndSettle();

    expect(measurementId, 'growth-1');
    expect(babyId, 'baby-1');
    expect(payload?['weightKg'], 6.3);
    expect(payload?['heightCm'], 61.5);
    expect(payload?['sourceType'], 'CLINIC');
  });

  testWidgets('empty metrics are rejected without a request', (tester) async {
    final service = _FakeGrowthMeasurementService();
    await _pumpForm(tester, service: service);

    await tester.tap(find.byKey(const Key('growth-form-save')));
    await tester.pump();

    expect(find.byKey(const Key('growth-form-error')), findsOneWidget);
    expect(service.addedPayload, isNull);
  });

  testWidgets(
    'negative and non-finite metrics are rejected without a request',
    (tester) async {
      final service = _FakeGrowthMeasurementService();
      await _pumpForm(tester, service: service);

      await tester.enterText(find.byKey(const Key('growth-form-weight')), '-1');
      await tester.tap(find.byKey(const Key('growth-form-save')));
      await tester.pump();
      expect(find.byKey(const Key('growth-form-error')), findsOneWidget);
      expect(service.addedPayload, isNull);

      await tester.enterText(
        find.byKey(const Key('growth-form-weight')),
        'NaN',
      );
      await tester.tap(find.byKey(const Key('growth-form-save')));
      await tester.pump();
      expect(find.byKey(const Key('growth-form-error')), findsOneWidget);
      expect(service.addedPayload, isNull);
    },
  );

  testWidgets('whitespace-only source is rejected for a new record', (
    tester,
  ) async {
    final service = _FakeGrowthMeasurementService();
    await _pumpForm(tester, service: service);

    await tester.enterText(find.byKey(const Key('growth-form-weight')), '6.2');
    await tester.enterText(find.byKey(const Key('growth-form-source')), '   ');
    await tester.tap(find.byKey(const Key('growth-form-save')));
    await tester.pump();

    expect(find.byKey(const Key('growth-form-error')), findsOneWidget);
    expect(service.addedPayload, isNull);
  });

  testWidgets('editing cannot silently clear an existing metric', (
    tester,
  ) async {
    final service = _FakeGrowthMeasurementService();
    await _pumpForm(tester, service: service, measurement: _measurement());

    await tester.enterText(find.byKey(const Key('growth-form-weight')), '');
    await tester.tap(find.byKey(const Key('growth-form-save')));
    await tester.pump();

    expect(find.byKey(const Key('growth-form-error')), findsOneWidget);
    expect(service.updatedPayload, isNull);
  });

  testWidgets('edit preserves a null note when it is unchanged', (
    tester,
  ) async {
    final service = _FakeGrowthMeasurementService();
    await _pumpForm(
      tester,
      service: service,
      measurement: _measurementWithoutNote(),
    );

    await tester.tap(find.byKey(const Key('growth-form-save')));
    await tester.pumpAndSettle();

    expect(service.updatedPayload, isNotNull);
    expect(service.updatedPayload!.containsKey('note'), isFalse);
  });

  testWidgets('failed save keeps the form open and preserves entered values', (
    tester,
  ) async {
    final service = _FakeGrowthMeasurementService()..fail = true;
    await _pumpForm(tester, service: service);

    await tester.enterText(find.byKey(const Key('growth-form-weight')), '6.2');
    await tester.tap(find.byKey(const Key('growth-form-save')));
    await tester.pumpAndSettle();

    expect(
      find.byKey(const Key('growth-measurement-form-screen')),
      findsOneWidget,
    );
    expect(find.byKey(const Key('growth-form-error')), findsOneWidget);
    expect(find.text('6.2'), findsOneWidget);
  });

  testWidgets('cancel closes the form without submitting', (tester) async {
    final service = _FakeGrowthMeasurementService();
    await _pumpForm(tester, service: service);

    await tester.tap(find.byKey(const Key('growth-form-cancel')));
    await tester.pumpAndSettle();

    expect(
      find.byKey(const Key('growth-measurement-form-screen')),
      findsNothing,
    );
    expect(service.addedPayload, isNull);
  });
}
